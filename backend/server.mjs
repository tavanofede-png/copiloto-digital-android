import { createServer } from 'node:http';
import { assistHandler, healthHandler, sendJson } from './src/http.mjs';

const server = createServer((request, response) => {
  const path = new URL(request.url, 'http://localhost').pathname;
  if (path === '/assist' || path === '/api/assist') return assistHandler(request, response);
  if (path === '/health' || path === '/api/health') return healthHandler(request, response);
  sendJson(response, 404, { error: 'NOT_FOUND' });
});
server.listen(Number(process.env.PORT || 3000), process.env.HOST || '127.0.0.1', () => {
  console.log('Copiloto Realtime backend listo. No se registran datos de las consultas.');
});
