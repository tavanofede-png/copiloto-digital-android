# Validación — Android y Realtime

Este registro separa implementación, compilación y evidencia física. Un build exitoso no demuestra el flujo en un teléfono.

La primera entrega se conserva como registro histórico. La validación de la versión 0.2.0 figura al final; el usuario pidió continuar con Realtime y distribuir un APK que funcione por internet, sin USB.

## Estado inicial — 21 de octubre de 2026

- Directorio inicial vacío. Sin instrucciones AGENTS.md aplicables.
- No había Java, Gradle, SDK ni ADB en PATH.
- Herramientas portables instaladas dentro del proyecto: Microsoft OpenJDK 21.0.12.1, SDK 36 (rev. 2), Build Tools 35.0.0 y ADB.
- SHA-256 del JDK y command-line tools contrastados con los proveedores; distribución Gradle 8.13 verificada contra su checksum oficial.
- Scripts PowerShell analizados sin errores de sintaxis.
- Usuario indicó que conectará un teléfono Android con depuración USB.
- Fases 1–3 implementadas. Primera compilación `assembleDebug` exitosa: 37 tareas, 10m 8s. Validación física pendiente.
- No se avanzó a las integraciones de voz e IA, siguiendo la condición del plan.
- La descarga inicial del JDK se interrumpió; se reanudó y se verificó su integridad. Durante el bootstrap del entorno se avanzó en las fases 2–3 para aprovechar ese bloqueo, sin superar la puerta de validación física.

## Puerta de validación

| Prueba | Resultado |
| --- | --- |
| `assembleDebug` | Correcto; APK debug generado |
| `lintDebug` | Correcto: 0 errores, 11 advertencias no bloqueantes |
| Firma APK (`apksigner verify --verbose`) | Correcta, esquema v2 |
| Manifiesto del APK (`aapt dump badging`) | API 26–36; sin INTERNET, RECORD_AUDIO ni AccessibilityService |
| App abre en Android | Pendiente de teléfono |
| Permiso overlay concedido y denegado | Pendiente |
| Consentimiento de pantalla concedido y cancelado | Pendiente |
| Burbuja sobre Chrome | Pendiente |
| Arrastrar y pegar a un lateral | Pendiente |
| Captura de Chrome sin panel ni burbuja | Pendiente |
| Segunda captura actualizada | Pendiente |
| Rotación y nueva captura | Pendiente |
| Finalizar desde app, panel y notificación | Pendiente |
| Detener desde indicador del sistema / bloqueo | Pendiente |
| Nueva sesión con consentimiento nuevo | Pendiente |
| Pantalla protegida o vacía | Pendiente |
| Escalado de fuente grande | Pendiente |

## Recorrido de prueba

1. Instalar el APK debug y abrir la app.
2. Iniciar, denegar overlay una vez y verificar la explicación al volver.
3. Conceder overlay; cancelar compartir pantalla y volver a iniciar.
4. Aceptar compartir pantalla completa y abrir Chrome en `https://example.com`.
5. Comprobar que la burbuja sigue visible y puede moverse a ambos lados.
6. Tocar la burbuja, pedir captura y comprobar la vista previa de Chrome.
7. Cerrar el panel, cambiar de página y capturar de nuevo: debe mostrar el contenido nuevo.
8. Rotar, repetir captura y verificar proporción y dimensiones.
9. Finalizar por cada vía; no deben quedar burbuja, notificación ni indicador de captura.
10. Iniciar otra sesión: Android debe pedir consentimiento nuevamente.
11. Detener desde el indicador de pantalla compartida o bloquear; verificar limpieza.

No guardar imágenes personales como evidencia. Las capturas de validación, si se usan, deben mostrar exclusivamente contenido de prueba y borrarse después.

## Artefacto generado

- Ruta: `app/build/outputs/apk/debug/app-debug.apk`.
- Versión: `0.1.0` (`ar.com.copiloto`).
- Tamaño: 11.156.450 bytes.
- SHA-256: `f5b00fee1883f1c436f0a178ffe27374dfa8035a11d0c2afddf0d092d92b73af`.
- La inspección del APK y su firma no sustituyen la prueba física del flujo.

## Cierre de la primera entrega

- `assembleDebug lintDebug`: exitoso después de corregir el tema para API 26; último build combinado de 6 segundos.
- `lintDebug --rerun-tasks`: exitoso, 28 tareas ejecutadas en 12 segundos; informe regenerado, 0 errores y 11 advertencias.
- Corregida la única incompatibilidad detectada: `windowLightNavigationBar` quedó en `values-v27`.
- Agregadas reglas explícitas que excluyen datos de backups y transferencias (incluido `fullBackupContent=false` para Android 8–11), y reutilización del trazado de la burbuja.
- Advertencias restantes: versiones de dependencias fijadas, sugerencias KTX y uso deliberado del origen físico izquierdo para las coordenadas del overlay.
- ADB iniciado correctamente, sin dispositivos conectados/autorizados al último control. No se instaló ni se ejecutó la app en un teléfono.
- No se ejecutaron pruebas instrumentadas ni se afirma que el recorrido físico haya pasado. Conectar y autorizar el teléfono es el próximo paso; las fases 4–7 permanecen pendientes.

## Versión 0.2.0 — Realtime online — 21 de octubre de 2026

Se implementaron voz, proxy HTTPS, validación, respuesta hablada y highlights por solicitud posterior del usuario. La prueba física que exigía el plan inicial sigue pendiente y no se considera cumplida por las pruebas de servidor.

### Evidencia comprobada

| Control | Resultado |
| --- | --- |
| `assembleDebug testDebugUnitTest lintDebug` | Correcto; último build combinado de 9 segundos |
| Pruebas Android JVM | 3 aprobadas: riesgo, coordenadas y contexto acotado |
| Pruebas backend `npm test` | 9 aprobadas: entradas, política, autenticación, protocolo, voz y cancelación |
| Lint | 0 errores y 14 advertencias no bloqueantes |
| Firma APK | Correcta, esquema v2 |
| Manifiesto APK | `ar.com.copiloto`, versión 0.2.0, API 26–36, INTERNET y RECORD_AUDIO presentes; sin AccessibilityService |
| Revisión de secretos | Clave del proveedor y código de conexión ausentes de los 62 archivos fuente candidatos y las 123 entradas descomprimidas del APK |
| Exclusiones Git | `.env`, `.env.local` y metadatos `.vercel` ignorados; `.env.example` vacío de credenciales incluido |
| Backend publicado | `https://copiloto-realtime-api.vercel.app` |
| Salud HTTPS | HTTP 200 con configuración activa |
| Consulta sin código | HTTP 401 |
| Consulta autenticada vacía | HTTP 400, sin consumir inferencia |
| OpenAI real desde Vercel | HTTP 200, `gpt-realtime-2.1-mini`, audio de entrada + imagen + voz de salida |

Las advertencias de lint corresponden a versiones fijadas, sugerencias de API/estilo y el origen físico de coordenadas. No se hicieron pruebas instrumentadas ni instalación en dispositivo.

### Prueba real de voz y visión

- Entrada: pantalla ficticia de 1080 × 1920, con botones «Facturas» y «Consumo», más audio sintético en español: «Quiero ver mi factura. ¿Dónde toco?». No se enviaron capturas personales.
- Audio de entrada PCM16 mono 24 kHz: 170.856 bytes.
- Consulta entendida: «El usuario quiere ver su factura y pregunta dónde tocar».
- Respuesta: «Tocá el botón “Facturas” (está arriba del botón “Consumo”) para abrir el listado de facturas».
- Objetivo: `Facturas`, centro normalizado `(0.50, 0.35)`, confianza 0.95, riesgo LOW. El centro cayó dentro del botón de prueba. El tamaño del recuadro sigue siendo aproximado.
- Voz Realtime recibida: 278.400 bytes PCM; la transcripción coincidió con la indicación validada.
- Duración del pedido HTTPS: 5.994 ms. Es una observación puntual, no una garantía de latencia.
- Una prueba previa sin cuadrícula dio una ubicación vertical imprecisa. Se agregó una cuadrícula únicamente a la copia enviada al modelo y se comprobó la ubicación anterior con ella. No demuestra precisión general en otras pantallas.

### APK distribuido

- Archivo: `copiloto-0.2.0-debug.apk`, generado desde `app/build/outputs/apk/debug/app-debug.apk`.
- Tamaño: 10.807.953 bytes.
- SHA-256: `48f66a8de18e00175deaf13f6b3b26b348f1edd7579b7c650c564593bae6e5ce`.
- Endpoint HTTPS preconfigurado. El usuario ingresa una vez el código de conexión recibido por privado. La clave de OpenAI permanece en secretos de producción de Vercel.
- Binario debug para pruebas; no constituye una versión de tienda ni una validación física aprobada.

### Pendiente en un teléfono real

1. Instalar desde la release, guardar y probar el código por internet móvil/Wi-Fi, sin PC.
2. Conceder y denegar los permisos de overlay, notificaciones, pantalla completa y micrófono.
3. Ver la burbuja sobre Chrome, arrastrarla y abrir/cerrar el panel con fuente grande.
4. Escribir una consulta y verificar que la captura excluya panel, burbuja y teclado.
5. Hablar, finalizar y cancelar la grabación; comprobar que la pantalla capturada sea la app anterior y que el micrófono se cierre al salir.
6. Escuchar voz Realtime, repetir y cancelar; comprobar TTS alternativo cuando no llega voz válida.
7. Comprobar el highlight, tocar la app debajo y confirmar que se borre a los seis segundos. Verificar baja confianza y riesgo alto con pantallas ficticias.
8. Rotar, probar una pantalla protegida, interrumpir internet y reintentar.
9. Finalizar desde panel, app, notificación e indicador del sistema; verificar que no queden captura, audio ni ventanas.
10. Volver a iniciar y confirmar consentimiento nuevo. Revisar al menos dos recorridos ficticios completos antes de presentar la demo.

No se afirma funcionamiento físico del micrófono, overlay, reproducción, MediaProjection ni ubicación de coordenadas hasta completar estos controles.
