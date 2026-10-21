# Validación — fases 1–3

Este registro separa implementación, compilación y evidencia física. Un build exitoso no demuestra el flujo en un teléfono.

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
