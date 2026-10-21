# Backend Realtime

Proxy Node.js para Copiloto Android. La clave permanente de OpenAI permanece en el servidor; el teléfono usa un código `BACKEND_TOKEN` compartido para esta prueba. No se incorpora ninguna credencial al código fuente o al APK.

## Configuración

Requiere Node.js 22+ (verificado con 24) y acceso al modelo `gpt-realtime-2.1-mini`.

```powershell
npm ci
Copy-Item .env.example .env
# Editar los valores de .env en privado.
npm test
npm start
```

| Variable | Uso |
| --- | --- |
| `OPENAI_API_KEY` | Clave del proyecto OpenAI, solo servidor |
| `OPENAI_REALTIME_MODEL` | `gpt-realtime-2.1-mini` |
| `BACKEND_TOKEN` | Código aleatorio largo entregado por privado a los probadores |
| `HOST`, `PORT` | Servidor local; predeterminado `127.0.0.1:3000` |

En Vercel se usa el proyecto `copiloto-realtime-api`. Configurá las tres primeras variables como secretos de Production y desplegá desde esta carpeta con `vercel deploy --prod`. Los secretos nuevos requieren un despliegue posterior. `.vercelignore` excluye entornos locales y herramientas de prueba.

## HTTP

`GET /health` devuelve estado de configuración y nombre del modelo; no comprueba saldo ni consume una inferencia. `POST /assist` requiere `Content-Type: application/json` y `Authorization: Bearer <código de conexión>`. También se aceptan las rutas `/api/health` y `/api/assist` para el adaptador serverless.

Cuerpo de ejemplo sin credenciales:

```json
{
  "question": "Quiero ver mi factura. ¿Dónde toco?",
  "imageBase64": "<JPEG o PNG en base64, sin prefijo data:>",
  "screen": { "width": 1080, "height": 1920 },
  "context": []
}
```

Como alternativa a `question`, se puede enviar `audioBase64`: PCM crudo, 16 bits little endian, mono, 24 kHz, entre 0,1 y 20 segundos. No enviar cabecera WAV. La imagen es opcional, máximo 1,5 MB. El cuerpo completo tiene límite de 4 MB. El contexto admite los últimos seis mensajes `{role: "user" | "assistant", text: "..."}`, hasta 800 caracteres cada uno.

La respuesta incluye `canHelp`, `heardQuestion`, `message`, `target`, `risk`, `requiresConfirmation`, `reason`, `model` y, si está disponible, `audioBase64`/`audioSampleRate`. `target.x/y` son el **centro** normalizado sobre toda la imagen, y `width/height` su tamaño normalizado. Una respuesta sin objetivo usa `visible: false` y coordenadas nulas.

Enviar `{}` autenticado produce HTTP 400 sin consultar OpenAI. La app usa ese resultado para verificar el código. Un código incorrecto produce HTTP 401. Los errores no devuelven mensajes crudos del proveedor ni contenido del usuario.

## Realtime y validación

Cada consulta abre un WebSocket propio y espera `session.updated` antes de enviar texto, imagen y audio. Se fuerza la función `show_guidance`, se valida su JSON con Ajv y se aplican reglas de riesgo y confianza. Function calling no se considera una garantía de Structured Outputs estricto.

Después de validar se pide la voz. Solo se entrega el PCM si la transcripción coincide con el mensaje validado, ignorando diferencias de puntuación y acentos; si no coincide, Android lee el mensaje con su TTS. El tiempo límite es de 45 segundos. La cancelación o desconexión HTTP cierra la sesión upstream.

No se registran ni persisten preguntas, capturas ni grabaciones en este código. No se afirma retención cero por parte del proveedor. Los límites en memoria de 60 solicitudes/hora/IP y dos consultas simultáneas se aplican por instancia de Node, y no sustituyen cuotas globales ni autenticación individual.

## Verificación

```powershell
npm test
# Prueba opcional de pago con una imagen ficticia y las variables de .env:
node --env-file=.env scripts/smoke.mjs ruta-a-pantalla-ficticia.jpg
```

Las pruebas automáticas cubren validación, riesgo, confianza, autenticación y orden del protocolo con un WebSocket controlado. El registro de la consulta real contra Vercel está en [../docs/VALIDATION.md](../docs/VALIDATION.md).
