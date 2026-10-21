# Copiloto digital · Android

## Página web de NIETO

NIETO es un asistente de smartphone para adultos mayores de **65 a 99 años**. La página HTML original está integrada en [`web/`](web/README.md). Para verla, ejecutá `npm start` en la raíz y abrí `http://127.0.0.1:4173`. No requiere instalar dependencias ni compilar.

Asistencia por voz y pantalla, un paso a la vez. Copiloto explica y señala; la persona toca por su cuenta. No usa AccessibilityService ni ejecuta acciones en otras apps.

## Versión 0.2.0

Integra **OpenAI `gpt-realtime-2.1-mini`** mediante un backend HTTPS en Vercel. Recibe una pregunta hablada o escrita y una captura puntual; devuelve un paso, voz y un objetivo visual aproximado. Grok no está integrado en esta versión.

La compilación, las pruebas automáticas y una consulta real de voz + imagen al backend publicado pasaron. **El recorrido físico en Android todavía no fue probado.** El registro y los casos pendientes están en [docs/VALIDATION.md](docs/VALIDATION.md). Esta entrega es un APK de prueba, no una versión certificada para producción.

## Instalar en el teléfono, sin PC ni USB

1. Desde el teléfono, descargá `copiloto-0.2.0-debug.apk` en [Releases](https://github.com/tavanofede-png/copiloto-digital-android/releases/tag/v0.2.0).
2. Instalalo y abrí **Copiloto**. Android puede pedir autorización para instalar desde el navegador.
3. Abrí **Configurar conexión**, pegá el código de conexión recibido por privado y tocá **Guardar y probar**. El servidor ya viene configurado: `https://copiloto-realtime-api.vercel.app`.
4. Iniciá el acompañamiento, habilitá mostrar sobre otras apps y aceptá compartir **la pantalla completa**. Las notificaciones son opcionales.
5. Abrí Chrome u otra app, tocá la burbuja y elegí **Preguntar por voz** o escribí tu consulta. El micrófono se solicita al usar voz; tocá **Terminar y preguntar** al terminar.
6. Escuchá la indicación y realizá el paso. Podés pedir el siguiente, indicar que no lo encontrás, repetir o cerrar el panel para ver mejor la pantalla.
7. Finalizá desde el panel, la app o la notificación. Una nueva sesión vuelve a pedir consentimiento de pantalla.

Necesitás Android 8+ e internet. El código de conexión es distinto de la clave de OpenAI: **no pegues la API key en la app**. El código no se publica en el repositorio ni en el APK; compartilo por privado con quien vaya a probar.

## Qué está implementado

- Burbuja arrastrable, sesión explícita con notificación y captura bajo demanda mediante MediaProjection.
- Consulta por texto o grabación PCM mono de 24 kHz, con un máximo de 20 segundos. Una actividad visible controla el micrófono; no se escucha permanentemente.
- Conexión HTTP del teléfono al backend; una sesión WebSocket Realtime por consulta en el servidor. La respuesta llega al terminar la consulta: no es una conversación de audio continua.
- Función `show_guidance` con JSON validado, un paso breve en español, nivel de riesgo y coordenadas normalizadas del centro del elemento.
- Voz Realtime cuando su transcripción coincide con la indicación validada; síntesis de voz de Android como alternativa.
- Highlight temporal que permite tocar la app debajo, sin inyectar acciones. Se oculta con confianza menor de 0,65, riesgo alto o confirmación sensible.
- Contexto corto de hasta seis mensajes de texto en memoria y cancelación de solicitudes.
- La copia enviada al modelo lleva una cuadrícula de referencia para ayudar a ubicar los elementos; la pantalla del usuario no se modifica.
- Vista previa de captura para diagnóstico en el APK debug.

La precisión visual depende del modelo. Los recuadros son aproximados y la persona debe comprobar el contenido antes de actuar. No se han implementado cuentas, Grok, modo demo con recorridos preparados ni modelos locales.

## Arquitectura

```text
Android
  MainActivity / Compose → permisos y configuración
  MediaProjectionService → sesión, burbuja y captura puntual
    VoiceCaptureActivity → pregunta PCM / alternativa por texto
    HttpAssistantApi → POST /assist autenticado por HTTPS
        Backend Vercel (Node.js)
          → WebSocket OpenAI Realtime: audio + imagen + contexto
          → show_guidance → validación y política de riesgo
          → voz correspondiente al mensaje validado
    GuidanceSpeaker + HighlightOverlay → explicación y marca temporal
```

Cada consentimiento crea un VirtualDisplay, que queda sin superficie entre solicitudes. Los overlays se ocultan antes de capturar. La imagen se comprime a JPEG 80, ancho máximo 1080 y lado mayor máximo 1920. No se transmite video continuo.

## Desarrollar Android

Cloná [el repositorio](https://github.com/tavanofede-png/copiloto-digital-android) y abrí la raíz en Android Studio. Requiere JDK 17/21, SDK 36 y Build Tools 35.0.0.

En Windows podés usar el entorno portable, sin modificar el PATH global:

```powershell
.\scripts\setup-android.ps1
.\scripts\build.ps1 -Tasks @('assembleDebug', 'testDebugUnitTest', 'lintDebug')
```

O con herramientas propias:

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
# Windows: gradlew.bat
```

APK generado: `app/build/outputs/apk/debug/app-debug.apk`. La instalación por ADB es opcional mediante `scripts/install-debug.ps1`; el APK publicado se instala directamente desde el teléfono.

Versiones fijadas: Gradle 8.13, AGP 8.13.2, Kotlin 2.2.20 y Compose BOM 2025.08.01. `compileSdk`/`targetSdk` 36 y `minSdk` 26. Para otro servidor, compilá con `-PassistantApiBaseUrl=https://tu-servidor.example` o cambialo en Configurar conexión. Nunca uses propiedades Gradle para introducir la API key.

## Desarrollar y desplegar el backend

Ver [backend/README.md](backend/README.md) para la configuración, contrato y despliegue. Las variables `OPENAI_API_KEY`, `OPENAI_REALTIME_MODEL` y `BACKEND_TOKEN` viven en el servidor. El APK solo contiene la URL pública y solicita el código al primer uso.

```powershell
cd backend
npm ci
Copy-Item .env.example .env
# Completá .env localmente; no lo subas a GitHub.
npm test
npm start
```

El servidor local escucha en `127.0.0.1:3000`. La app exige HTTPS, por lo que para el teléfono se usa el despliegue online. Salud pública: [GET /health](https://copiloto-realtime-api.vercel.app/health); las consultas requieren autenticación.

## Privacidad y límites

- El servidor necesita la clave de OpenAI; el teléfono guarda únicamente URL y código de conexión en preferencias privadas, con backups deshabilitados.
- Capturas y audio se procesan en memoria en la app y nuestro backend; no se implementó almacenamiento de consultas ni historial visual. Se envían a OpenAI para responder y aplican las condiciones del proveedor.
- No se registran capturas, audio, preguntas ni credenciales en los logs de la aplicación o el backend.
- Finalizar libera captura, ventanas, micrófono, reproducción y contexto. Retirar el consentimiento o cerrar la tarea también termina la sesión.
- Las pantallas protegidas pueden aparecer negras. La detección es heurística y puede confundir una pantalla oscura; se puede conversar sin marcar elementos.
- El código compartido sirve para una prueba entre personas de confianza. No hay cuentas individuales. Los límites defensivos de 60 solicitudes por hora/IP y dos consultas simultáneas son **por instancia**, no una cuota global de gasto.
- No se ha comprobado todavía el comportamiento del micrófono, overlays, rotación, permisos y ahorro de batería en un teléfono real.

El SDK, JDK, cachés, `.env`, metadatos Vercel, claves y APKs quedan fuera del historial Git. El binario se distribuye como adjunto de la release.

## Referencias

- [OpenAI: modelo gpt-realtime-2.1-mini](https://developers.openai.com/api/docs/models/gpt-realtime-2.1-mini)
- [OpenAI: Realtime por WebSocket](https://developers.openai.com/api/docs/guides/voice-websockets)
- [Android: MediaProjection](https://developer.android.com/media/grow/media-projection)
