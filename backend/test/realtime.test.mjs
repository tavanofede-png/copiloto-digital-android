import test from 'node:test';
import assert from 'node:assert/strict';
import { EventEmitter } from 'node:events';
import { getRealtimeGuidance } from '../src/realtime.mjs';

const guidance = () => ({ canHelp: true, heardQuestion: 'Mi factura', message: 'Tocá Facturas.',
  target: { visible: true, label: 'Facturas', x: .5, y: .4, width: .7, height: .1, confidence: .9 },
  risk: 'LOW', requiresConfirmation: false, reason: 'Visible' });
class FakeSocket extends EventEmitter {
  constructor({ spoken = 'Tocá Facturas.', stall = false } = {}) {
    super(); this.sent = []; this.spoken = spoken; this.stall = stall; this.closed = false;
    queueMicrotask(() => this.emit('open'));
  }
  send(text) {
    const event = JSON.parse(text); this.sent.push(event);
    if (event.type === 'session.update') queueMicrotask(() => this.emit('message', JSON.stringify({ type: 'session.updated' })));
    if (event.type !== 'response.create') return;
    queueMicrotask(() => {
      if (event.response.tool_choice !== 'none') {
        this.emit('message', JSON.stringify({ type: 'response.done', response: { status: 'completed', output: [{
          type: 'function_call', name: 'show_guidance', call_id: 'test-call', arguments: JSON.stringify(guidance()),
        }] } }));
      } else if (!this.stall) {
        this.emit('message', JSON.stringify({ type: 'response.output_audio.delta', delta: Buffer.alloc(480).toString('base64') }));
        this.emit('message', JSON.stringify({ type: 'response.output_audio_transcript.done', transcript: this.spoken }));
        this.emit('message', JSON.stringify({ type: 'response.done', response: { status: 'completed', output: [] } }));
      }
    });
  }
  close() { this.closed = true; }
}
const input = { question: 'Mi factura', image: Buffer.from([255,216,255]), audio: null, width: 1080, height: 1920, context: [] };

test('real protocol order: no voice before validated guidance; session closes', async () => {
  let socket;
  const result = await getRealtimeGuidance(input, { apiKey: 'test', socketFactory: () => (socket = new FakeSocket()) });
  assert.equal(result.audioSampleRate, 24000); assert.ok(result.audioBase64);
  const types = socket.sent.map(e => e.type);
  assert.ok(types.indexOf('conversation.item.create') < types.indexOf('response.create'));
  const outputIndex = socket.sent.findIndex(e => e.item?.type === 'function_call_output');
  assert.equal(socket.sent[outputIndex + 1].response.tool_choice, 'none');
  assert.equal(socket.closed, true);
});
test('audio that changes the validated message is discarded for native TTS', async () => {
  const result = await getRealtimeGuidance(input, { apiKey: 'test', socketFactory: () => new FakeSocket({ spoken: 'Tocá Pagar ahora.' }) });
  assert.equal(result.message, 'Tocá Facturas.'); assert.equal(result.audioBase64, null);
});
test('slow speech does not discard an already validated text result', async () => {
  const result = await getRealtimeGuidance(input, { apiKey: 'test', socketFactory: () => new FakeSocket({ stall: true }), timeoutMs: 25 });
  assert.equal(result.message, 'Tocá Facturas.'); assert.equal(result.audioBase64, null);
});
test('cancelled requests do not start an upstream session', async () => {
  const controller = new AbortController(); controller.abort(); let opened = false;
  await assert.rejects(getRealtimeGuidance(input, { apiKey: 'test', signal: controller.signal,
    socketFactory: () => { opened = true; return new FakeSocket(); } }), /CANCELLED/);
  assert.equal(opened, false);
});
