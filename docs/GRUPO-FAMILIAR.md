# Grupo familiar: propuesta de producto y UI/UX

Estado acordado con el usuario: **próxima función con una demostración de ejemplo**. La página no afirma que ya esté disponible en el APK. Esta entrega modifica la web; no incorpora monitorización real al código Android ni al backend.

## Dónde se integra

La sección de familias existente pasa a presentar **Grupo familiar**, con un acceso en la navegación. Se conserva la paleta azul de NIETO. El contenido primero explica el valor para la familia y después permite explorar una vista funcional con datos ficticios.

## Organización de la vista

- **Resumen:** última actividad, tiempos de pantalla, sitios recientes, estado de ubicación y acceso prioritario a una alerta pendiente.
- **Actividad:** cronología de eventos y sitios compartidos. La propuesta interpreta actividad de pantalla como eventos y tiempos de uso; no presenta una transmisión de pantalla o mensajes privados.
- **Ubicación:** ubicación compartida y antigüedad del registro. Cuando Nieto está desactivado, se señala que el último dato ya no se actualiza. Si no se comparte ubicación, se oculta el mapa ilustrativo.
- **Alertas:** explicación de una posible actividad insegura, contexto para acompañar al familiar y acción «Marcar como revisada». Una alerta de posible riesgo no se presenta como prueba de fraude.

## Recorrido propuesto en la app

1. La persona que acompaña invita a su familiar a un grupo privado.
2. El familiar acepta desde su propio celular y elige por separado compartir actividad/sitios, tiempos de pantalla, ubicación y avisos de seguridad.
3. El grupo ve únicamente los datos autorizados. La sesión activa y la fecha del último registro permanecen visibles.
4. El familiar puede retirar permisos o pausar el acceso. Desactivar Nieto detiene las nuevas actualizaciones; un registro anterior se identifica como tal.
5. Ante una alerta, quien acompaña puede revisar el contexto y comunicarse con su familiar. Revisar la alerta no ejecuta acciones en el otro teléfono.

## Qué permite probar la demo

- Pestañas accesibles con teclado: flechas, Inicio y Fin.
- Casos «Uso normal», «Sitio inseguro» y «Nieto desactivado».
- Aviso dentro de la página cuando se simula un enlace inseguro.
- Revisión de una alerta y preferencias de notificación del ejemplo.
- Permisos simulados que ocultan los datos correspondientes.

Los datos, horarios, sitios `.example` y el mapa son ficticios. La demo no pide permisos del navegador, consulta el GPS, lee dispositivos ni envía notificaciones. Los cambios viven en memoria y se reinician al recargar.

## Qué queda para desarrollar en Android y el backend

El repositorio actual no contiene grupos familiares, cuentas con roles, ubicación compartida, historial de navegación o notificaciones al grupo. Para convertir la propuesta en producto se necesitan autenticación y pertenencia al grupo, invitaciones, permisos revocables, recogida de eventos autorizados, reglas de acceso en servidor, antigüedad/retención de registros, un mecanismo de detección de riesgos y entrega de notificaciones. La cobertura real de sitios y actividad debe definirse durante la implementación; no se presupone acceso a todos los navegadores o aplicaciones.

Esos cambios no forman parte de esta edición de la página y requieren una implementación y validación separadas.
