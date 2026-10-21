# Copiloto

Un copiloto que acompaña a las personas mientras usan su celular. La IA no hace el trámite por vos: te enseña a hacerlo, un paso a la vez.

## Estado actual

Primera entrega implementada y compilada: **fases 1–3** del plan proporcionado, con validación física pendiente. Incluye el proyecto Android, inicio de sesión, permisos, notificación con acción de finalizar, burbuja arrastrable y captura de pantalla a pedido con vista previa en memoria.

La compilación y las pruebas en dispositivo se registran en [docs/VALIDATION.md](docs/VALIDATION.md). No se considera terminado el MVP hasta probar físicamente el recorrido completo con voz e IA. **Voz, backend, IA y highlights todavía no están implementados.** El plan requiere validar primero la burbuja sobre Chrome y la captura real.

Comprobaciones realizadas: `assembleDebug` y `lintDebug` correctos (0 errores; advertencias de versiones/estilo), manifiesto inspeccionado y firma del APK verificada. ADB funciona; todavía no detecta un dispositivo conectado/autorizado.

## Qué hace esta versión

1. Abrí Copiloto y tocá **Iniciar acompañamiento**.
2. Permití mostrar sobre otras apps y regresá a Copiloto.
3. Permití las notificaciones si Android las solicita. Denegarlas no impide la sesión.
4. Tocá **Permitir y comenzar** y aceptá compartir la pantalla completa.
5. Abrí Chrome u otra app. La burbuja se puede arrastrar y se acomoda a un lateral.
6. Tocá la burbuja y luego **Probar captura**. El panel y la burbuja se ocultan antes de capturar.
7. Comprobá la vista previa. Al cerrar el panel se descarta la imagen.
8. Finalizá desde el panel, la notificación o la pantalla de sesión.

No usa AccessibilityService ni ejecuta acciones en otras aplicaciones. No tiene acceso a internet en esta etapa. No pide micrófono hasta implementar voz.

## Arquitectura

```text
MainActivity + Compose
  ├── PermissionManager: overlay y consentimiento MediaProjection
  └── AssistanceSessionManager: estado efímero de la sesión
        └── MediaProjectionService (foreground, propietario de los recursos)
              ├── OverlayController + BubbleView
              └── ScreenCaptureManager (HandlerThread)
                    └── MediaProjection → un VirtualDisplay por consentimiento
                          → ImageReader solo durante la solicitud
                          → JPEG en memoria → vista previa temporal
```

Se usa un único servicio para que la captura, la notificación y los overlays tengan el mismo ciclo de vida. Cada inicio solicita consentimiento nuevo. Entre solicitudes el VirtualDisplay queda sin superficie; no se consumen frames ni se transmite video. Se redimensiona el display existente al rotar, sin reutilizar el token para crear otro.

La imagen incluye la pantalla completa y conserva su proporción al comprimir. Se conserva la relación entre las dimensiones reales y las dimensiones del JPEG para la futura conversión de coordenadas. Se usa JPEG 80, ancho máximo 1080 y lado mayor máximo 1920.

## Requisitos

- Windows, Linux o macOS con JDK 17 o 21.
- Android SDK: plataforma 36, Build Tools 35.0.0 y platform-tools.
- Teléfono Android 8+ (API 26); preferible Android 12+ para la demo.
- Depuración USB habilitada para instalar desde ADB.
- Internet para descargar dependencias de compilación.

Versiones fijadas: AGP 8.13.2, Gradle 8.13, Kotlin/Compose Compiler 2.2.20, Compose BOM 2025.08.01. `compileSdk` y `targetSdk`: 36.

## Cómo ejecutar Android

### Entorno portable de este proyecto (Windows)

```powershell
# Instala herramientas oficiales en .tools; no modifica el PATH global.
.\scripts\setup-android.ps1

# Compila usando ese entorno y cachés locales al proyecto.
.\scripts\build.ps1

# Comprobaciones de código.
.\scripts\build.ps1 -Tasks @('testDebugUnitTest', 'lintDebug')

# Conectá el teléfono y aceptá la autorización de depuración en su pantalla.
. .\scripts\android-env.ps1
adb devices -l
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n ar.com.copiloto/.MainActivity
```

También podés instalar y abrir en un solo paso con `.\scripts\install-debug.ps1` (o `-Serial ID` si hay varios dispositivos).

`setup-android.ps1 -AcceptLicenses` permite aceptar las licencias del SDK sin interacción. Solo usalo si ya leíste y aceptaste las condiciones oficiales.

### Android Studio / herramientas propias

Abrí la carpeta raíz en Android Studio, elegí JDK 17/21 y configurá la ubicación del SDK. También podés usar:

```text
gradlew.bat assembleDebug        # Windows
./gradlew assembleDebug          # Linux/macOS
```

En terminal necesitás `JAVA_HOME` y `ANDROID_HOME` (o `sdk.dir` en `local.properties`). El Wrapper verifica el SHA-256 de la distribución Gradle.

APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Colaborar desde GitHub

```sh
git clone https://github.com/tavanofede-png/copiloto-digital-android.git
cd copiloto-digital-android
```

Abrí esa carpeta en Android Studio o seguí las instrucciones de compilación anteriores. El repositorio contiene código y documentación; el SDK, JDK, cachés, APKs, archivos `.env` y claves de firma quedan excluidos.

La rama `main` contiene la primera entrega (fases 1–3). Antes de integrar voz e IA falta validar la burbuja y la captura sobre Chrome en un teléfono. Consultá [el registro de validación](docs/VALIDATION.md) para retomar desde el estado comprobado.

## Cómo ejecutar backend

Pendiente de la fase 5, después de la validación física de las fases 1–3. Será un proxy con `POST /assist`; las credenciales del proveedor permanecerán fuera del APK. No existe un endpoint simulado que se presente como IA real.

## Variables de entorno

Actualmente no se requiere una API key. El SDK y JDK portables se configuran por proceso en `scripts/android-env.ps1`.

En fase 5 se incorporarán `AI_API_KEY`, `AI_MODEL`, `PORT` y `.env.example`; en Android se configurará `ASSISTANT_API_BASE_URL`.

## Permisos y privacidad

- `SYSTEM_ALERT_WINDOW`: burbuja sobre otras apps, se revalida al volver de Ajustes.
- `FOREGROUND_SERVICE` y `FOREGROUND_SERVICE_MEDIA_PROJECTION`: sesión explícita con notificación y botón Finalizar.
- `POST_NOTIFICATIONS` en Android 13+: opcional, para la notificación visible.
- MediaProjection: consentimiento del sistema **en cada sesión**, después de conceder el overlay.
- El orden evita guardar o reutilizar tokens mientras el usuario sale a Ajustes.
- No se guardan capturas, grabaciones, preguntas o tokens en disco. No hay historial visual.
- Cerrar el panel libera su vista previa. Finalizar o retirar el consentimiento libera ImageReader, VirtualDisplay, MediaProjection y ventanas.
- Bloquear el dispositivo detiene la captura en versiones modernas de Android. Si se cierra la tarea desde Recientes se termina la sesión.
- Los logs solo contienen eventos, dimensiones, tamaño en bytes y tipo de error; nunca imágenes, credenciales o texto sensible.

## Modo demo

Pendiente de las fases 5–7. La vista previa actual usa capturas reales, no respuestas preparadas. Luego se añadirán dos flujos ficticios identificados como simulación, separados de la IA real.

## Limitaciones

- Android únicamente; sin voz, IA ni indicación visual en esta primera entrega.
- Las pantallas protegidas pueden aparecer negras. La detección actual es una heurística que también puede confundir una pantalla oscura; se permite reintentar.
- No se ha certificado el comportamiento en fabricantes específicos hasta probar en un teléfono.
- Los permisos de compartir pantalla y las restricciones sobre overlays varían por versión/fabricante.
- El futuro modelo visual puede equivocarse; los highlights serán aproximados.
- La IA cloud requerirá conexión. Las acciones siempre las realizará la persona.

## Roadmap

1. **Fases 1–3**: build, burbuja y captura; validar físicamente en Chrome.
2. **Fase 4**: SpeechRecognizer es-AR, transcripción, cancelación, texto alternativo y TextToSpeech.
3. **Fase 5**: proxy multimodal, JSON validado y guardrails.
4. **Fase 6**: captura + pregunta + contexto corto → un paso → voz + highlight.
5. **Fase 7**: dos recorridos, modo demo explícito, casos de error y estabilización.

Fuera de este MVP: cuentas, bases de datos, iOS, acciones automáticas, monitorización continua y SDK B2B. V2 podrá explorar asistencia local/híbrida, más idiomas y un SDK de guía contextual.

## Pitch técnico breve

Copiloto agrega una capa de orientación sobre las apps Android. La persona solicita ayuda; se toma una captura puntual y un modelo multimodal devuelve un único paso con coordenadas normalizadas y nivel de confianza. La app lo explica por voz y señala el elemento. La persona conserva el control y toca por sí misma. El primer hito valida los dos mecanismos nativos que hacen posible ese recorrido: overlay y MediaProjection.

## Referencias técnicas

- [MediaProjection: consentimiento, ciclo de vida y cambios de tamaño](https://developer.android.com/media/grow/media-projection)
- [Compatibilidad de AGP 8.13](https://developer.android.com/build/releases/agp-8-13-0-release-notes)
- [Herramientas oficiales de Android](https://developer.android.com/studio#command-line-tools-only)
