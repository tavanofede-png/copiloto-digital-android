import { timingSafeEqual } from 'node:crypto';
import { parseAssistRequest } from './request.mjs';
import { getRealtimeGuidance } from './realtime.mjs';

export function authorized(request, token = process.env.BACKEND_TOKEN) {
  if (!token) return false;
  const supplied = request.headers.authorization || '';
  const expected = `Bearer ${token}`;
  if (supplied.length > 256) return false;
  const actualBytes = Buffer.from(supplied);
  const expectedBytes = Buffer.from(expected);
  return actualBytes.length === expectedBytes.length && timingSafeEqual(actualBytes, expectedBytes);
}

export function sendJson(response, status, body) {
  response.statusCode = status;
  response.setHeader('Content-Type', 'application/json; charset=utf-8');
  response.setHeader('Cache-Control', 'no-store');
  response.setHeader('X-Content-Type-Options', 'nosniff');
  response.end(JSON.stringify(body));
}

export async function readBody(request) {
  if (request.body) {
    const body = typeof request.body === 'string' ? request.body : JSON.stringify(request.body);
    if (Buffer.byteLength(body) > 4_000_000) throw new Error('BODY_TOO_LARGE');
    return JSON.parse(body);
  }
  const chunks = [];
  let size = 0;
  for await (const chunk of request) {
    size += chunk.length;
    if (size > 4_000_000) throw new Error('BODY_TOO_LARGE');
    chunks.push(chunk);
  }
  return JSON.parse(Buffer.concat(chunks).toString('utf8'));
}

// Defensive per-instance throttling in addition to authentication and provider limits.
const attempts = new Map();
let inFlight = 0;
export async function assistHandler(request, response) {
  if (request.method !== 'POST') { sendJson(response, 405, { error: 'METHOD_NOT_ALLOWED' }); return; }
  if (!authorized(request)) { sendJson(response, 401, { error: 'UNAUTHORIZED', message: 'Revisá el código de conexión en Copiloto.' }); return; }
  if (!process.env.OPENAI_API_KEY) { sendJson(response, 503, { error: 'NOT_CONFIGURED', message: 'El servidor todavía no tiene configurada la conexión con OpenAI.' }); return; }
  if (!(request.headers['content-type'] || '').startsWith('application/json')) { sendJson(response, 415, { error: 'JSON_REQUIRED' }); return; }
  const now = Date.now();
  const ip = String(request.headers['x-forwarded-for'] || request.socket?.remoteAddress || 'unknown').split(',')[0];
  if (attempts.size > 500) for (const [key, value] of attempts) if (now - value.start > 3_600_000) attempts.delete(key);
  const bucket = attempts.get(ip);
  if (bucket && now - bucket.start < 3_600_000 && bucket.count >= 60) { sendJson(response, 429, { error: 'RATE_LIMITED', message: 'Llegaste al límite de consultas de esta prueba. Intentá más tarde.' }); return; }
  attempts.set(ip, bucket && now - bucket.start < 3_600_000 ? { start: bucket.start, count: bucket.count + 1 } : { start: now, count: 1 });
  if (inFlight >= 2) { sendJson(response, 429, { error: 'BUSY', message: 'Estoy atendiendo otra consulta. Volvé a intentar en unos segundos.' }); return; }
  const abort = new AbortController();
  const disconnected = () => { if (!response.writableEnded) abort.abort(); };
  response.on('close', disconnected);
  inFlight++;
  try {
    const input = parseAssistRequest(await readBody(request));
    const guidance = await getRealtimeGuidance(input, { apiKey: process.env.OPENAI_API_KEY,
      model: process.env.OPENAI_REALTIME_MODEL || 'gpt-realtime-2.1-mini', signal: abort.signal });
    if (!response.destroyed) sendJson(response, 200, guidance);
  } catch (error) {
    if (response.destroyed) return;
    const invalid = error instanceof SyntaxError || /^(INVALID_|BODY_TOO_LARGE)/.test(error.message);
    const providerCode = ['invalid_api_key', 'insufficient_quota', 'rate_limit_exceeded', 'model_not_found'].includes(error.message) ? error.message : undefined;
    const message = invalid ? 'No pude recibir la consulta. Probá nuevamente.' :
      providerCode === 'insufficient_quota' ? 'La cuenta de OpenAI no tiene saldo disponible. Revisá la facturación del proyecto.' :
      providerCode === 'invalid_api_key' ? 'La clave de OpenAI del servidor no es válida. Hay que actualizarla.' :
      providerCode === 'model_not_found' ? 'Esta cuenta todavía no tiene acceso al modelo Realtime configurado.' :
      'No pude obtener la respuesta en este momento. Intentá nuevamente.';
    // No provider payload, prompt, audio, screenshot or key is logged or returned.
    sendJson(response, invalid ? 400 : 502, { error: invalid ? 'INVALID_REQUEST' : 'ASSISTANT_UNAVAILABLE', providerCode, message });
  } finally {
    inFlight--;
    response.off('close', disconnected);
  }
}

export function healthHandler(request, response) {
  const configured = Boolean(process.env.OPENAI_API_KEY && process.env.BACKEND_TOKEN);
  sendJson(response, configured ? 200 : 503, { ok: configured, service: 'copiloto-realtime',
    model: process.env.OPENAI_REALTIME_MODEL || 'gpt-realtime-2.1-mini' });
}
