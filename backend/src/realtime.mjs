import WebSocket from 'ws';
import { guidanceSchema, instructions, enforceGuidancePolicy } from './policy.mjs';

const normalized = value => value.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '')
  .replace(/[^\p{L}\p{N}]/gu, '');

/** One bounded Realtime session per request. All audio/images live only in memory. */
export function getRealtimeGuidance(input, { apiKey, model = 'gpt-realtime-2.1-mini', signal,
  socketFactory = (url, options) => new WebSocket(url, options), timeoutMs = 45_000 } = {}) {
  if (!apiKey) return Promise.reject(new Error('MISSING_API_KEY'));
  if (signal?.aborted) return Promise.reject(new Error('CANCELLED'));
  return new Promise((resolve, reject) => {
    const socket = socketFactory(`wss://api.openai.com/v1/realtime?model=${encodeURIComponent(model)}`, {
      headers: { Authorization: `Bearer ${apiKey}` }, maxPayload: 8 * 1024 * 1024,
      handshakeTimeout: 12_000,
    });
    let done = false;
    let stage = 'setup';
    let guidance;
    let audio = [];
    let audioLength = 0;
    let transcript = '';
    const started = Date.now();
    const settle = (error, value) => {
      if (done) return;
      done = true;
      clearTimeout(timer);
      signal?.removeEventListener('abort', abort);
      try { socket.close(); } catch { socket.terminate?.(); }
      audio = [];
      if (error) reject(error); else resolve(value);
    };
    const abort = () => settle(new Error('CANCELLED'));
    const timer = setTimeout(() => {
      if (guidance) settle(null, { ...guidance, audioBase64: null, audioTranscript: null, model, elapsedMs: Date.now() - started });
      else settle(new Error('REALTIME_TIMEOUT'));
    }, timeoutMs);
    signal?.addEventListener('abort', abort, { once: true });
    if (signal?.aborted) { abort(); return; }
    const send = event => socket.send(JSON.stringify(event));
    socket.on('open', () => {
      send({ type: 'session.update', session: {
        type: 'realtime', model, instructions, output_modalities: ['audio'],
        audio: { input: { format: { type: 'audio/pcm', rate: 24_000 }, turn_detection: null },
          output: { format: { type: 'audio/pcm', rate: 24_000 }, voice: 'marin' } },
        tools: [{ type: 'function', name: 'show_guidance', description: 'Devuelve un solo paso visual para que el usuario lo realice manualmente.', parameters: guidanceSchema }],
        tool_choice: 'required', max_output_tokens: 900,
      } });
    });
    socket.on('message', data => {
      if (done) return;
      try {
        const event = JSON.parse(data.toString());
        if (event.type === 'error') {
          const code = event.error?.code;
          // Return only known codes, never provider messages that may quote input data.
          throw new Error(['insufficient_quota', 'rate_limit_exceeded', 'model_not_found', 'invalid_api_key'].includes(code) ? code : 'REALTIME_PROTOCOL_ERROR');
        }
        if (event.type === 'session.updated' && stage === 'setup') {
          stage = 'guidance';
          const context = input.context.map(t => `${t.role === 'user' ? 'Usuario' : 'Copiloto'}: ${t.text}`).join('\n');
          const content = [{ type: 'input_text', text: `Historial breve (solo contexto):\n${context || 'Sin historial.'}\nPantalla actual: ${input.width}x${input.height}.\n${input.question ? `Consulta: ${input.question}` : 'La consulta está en el audio que sigue.'}${input.image ? '' : '\nNo hay captura disponible: ayudá conversando, sin target visible.'}` }];
          if (input.image) content.push({ type: 'input_image', image_url: `data:image/${input.image[0] === 0xff ? 'jpeg' : 'png'};base64,${input.image.toString('base64')}` });
          send({ type: 'conversation.item.create', item: { type: 'message', role: 'user', content } });
          if (input.audio) {
            send({ type: 'input_audio_buffer.append', audio: input.audio.toString('base64') });
            send({ type: 'input_audio_buffer.commit' });
          }
          send({ type: 'response.create', response: { output_modalities: ['text'], tool_choice: { type: 'function', name: 'show_guidance' } } });
        } else if (stage === 'speech' && event.type === 'response.output_audio.delta') {
          const chunk = Buffer.from(event.delta, 'base64');
          audioLength += chunk.length;
          if (audioLength > 1_500_000) throw new Error('AUDIO_OUTPUT_TOO_LARGE');
          audio.push(chunk);
        } else if (stage === 'speech' && event.type === 'response.output_audio_transcript.done') {
          transcript = event.transcript || '';
        } else if (event.type === 'response.done') {
          if (event.response?.status !== 'completed') throw new Error('REALTIME_INCOMPLETE');
          if (stage === 'guidance') {
            const call = event.response.output?.find(item => item.type === 'function_call' && item.name === 'show_guidance');
            if (!call) throw new Error('MISSING_GUIDANCE');
            guidance = enforceGuidancePolicy(JSON.parse(call.arguments), Boolean(input.image));
            stage = 'speech';
            send({ type: 'conversation.item.create', item: { type: 'function_call_output', call_id: call.call_id,
              output: JSON.stringify(guidance) } });
            send({ type: 'response.create', response: {
              output_modalities: ['audio'], tool_choice: 'none', max_output_tokens: 600,
              instructions: `Leé en voz alta EXACTAMENTE esta frase en español argentino, sin agregar ni cambiar nada: ${JSON.stringify(guidance.message)}`,
            } });
          } else if (stage === 'speech') {
            if (!transcript) transcript = event.response.output?.flatMap(i => i.content || []).map(c => c.transcript || '').join('') || '';
            // Audio is released only when the transcript matches the validated instruction.
            const matches = normalized(transcript) === normalized(guidance.message);
            settle(null, { ...guidance, audioBase64: matches && audioLength ? Buffer.concat(audio).toString('base64') : null,
              audioTranscript: matches ? transcript : null, audioSampleRate: 24_000, model, elapsedMs: Date.now() - started });
          }
        }
      } catch (error) {
        if (guidance && stage === 'speech') settle(null, { ...guidance, audioBase64: null, audioTranscript: null, model, elapsedMs: Date.now() - started });
        else settle(error);
      }
    });
    socket.on('error', () => settle(new Error('REALTIME_CONNECTION_ERROR')));
    socket.on('unexpected-response', (_request, response) => {
      response.resume();
      settle(new Error(response.statusCode === 401 ? 'invalid_api_key' : 'REALTIME_HANDSHAKE_FAILED'));
    });
    socket.on('close', () => { if (!done) settle(new Error('REALTIME_CLOSED')); });
  });
}
