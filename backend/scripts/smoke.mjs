import { readFile } from 'node:fs/promises';
import { parseAssistRequest } from '../src/request.mjs';
import { getRealtimeGuidance } from '../src/realtime.mjs';

const imagePath = process.argv[2];
const input = parseAssistRequest({ question: 'Quiero ver mi factura. ¿Dónde toco?',
  imageBase64: imagePath ? (await readFile(imagePath)).toString('base64') : undefined,
  screen: { width: 1080, height: 1920 }, context: [] });
try {
  const response = await getRealtimeGuidance(input, { apiKey: process.env.OPENAI_API_KEY,
    model: process.env.OPENAI_REALTIME_MODEL });
  console.log(JSON.stringify({ model: response.model, message: response.message,
    target: response.target, risk: response.risk, elapsedMs: response.elapsedMs,
    audioBytes: response.audioBase64 ? Buffer.from(response.audioBase64, 'base64').length : 0 }, null, 2));
} catch (error) {
  console.error(`Smoke failed: ${error.message}`);
  process.exitCode = 1;
}
