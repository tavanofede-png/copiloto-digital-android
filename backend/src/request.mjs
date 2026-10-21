const base64 = /^[A-Za-z0-9+/]*={0,2}$/;
function decode(value, maxBytes, code) {
  if (typeof value !== 'string' || value.length % 4 || !base64.test(value) || value.length > Math.ceil(maxBytes / 3) * 4) {
    throw new Error(code);
  }
  const buffer = Buffer.from(value, 'base64');
  if (buffer.length > maxBytes || buffer.toString('base64') !== value) throw new Error(code);
  return buffer;
}

export function parseAssistRequest(body) {
  if (!body || typeof body !== 'object') throw new Error('INVALID_REQUEST');
  const question = typeof body.question === 'string' ? body.question.trim().slice(0, 800) : '';
  const audio = body.audioBase64 ? decode(body.audioBase64, 24_000 * 2 * 20, 'INVALID_AUDIO') : null;
  if ((!question && !audio) || (audio && (audio.length < 4_800 || audio.length % 2))) throw new Error('INVALID_QUESTION');
  const image = body.imageBase64 ? decode(body.imageBase64, 1_500_000, 'INVALID_IMAGE') : null;
  if (image && !((image[0] === 0xff && image[1] === 0xd8) || image.subarray(0, 8).equals(Buffer.from([137,80,78,71,13,10,26,10])))) {
    throw new Error('INVALID_IMAGE');
  }
  const width = Number(body.screen?.width);
  const height = Number(body.screen?.height);
  if (!Number.isInteger(width) || !Number.isInteger(height) || width < 1 || height < 1 || width > 10_000 || height > 10_000) {
    throw new Error('INVALID_SCREEN');
  }
  const context = (Array.isArray(body.context) ? body.context : []).slice(-6)
    .filter(t => (t?.role === 'user' || t?.role === 'assistant') && typeof t.text === 'string')
    .map(t => ({ role: t.role, text: t.text.slice(0, 800) }));
  return { question, audio, image, width, height, context };
}
