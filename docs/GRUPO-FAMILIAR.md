# Grupo familiar: propuesta de producto y UI/UX

Alcance técnico: **prototipo web interactivo**. Por pedido posterior del usuario, la interfaz adopta una presentación de producto y omite etiquetas de demostración y aclaraciones de implementación. Esta edición modifica textos y estados locales de la web; no incorpora monitorización real al código Android ni al backend.

## Dónde se integra

La sección de familias existente pasa a presentar **Grupo familiar**, con un acceso en la navegación. Se conserva la paleta azul de NIETO. El contenido primero explica el valor para la familia y después permite explorar una vista funcional con datos ficticios.

## Organización de la vista

- **Resumen:** última actividad, tiempos de pantalla, sitios recientes, estado de ubicación y acceso prioritario a una alerta pendiente.
- **Actividad:** cronología de eventos y sitios compartidos. La propuesta interpreta actividad de pantalla como eventos y tiempos de uso; no presenta una transmisión de pantalla o mensajes privados.
- **Ubicación:** ubicación compartida y antigüedad del registro. Cuando Nieto está desactivado, se señala que el último dato ya no se actualiza. Si no se comparte ubicación, se oculta el mapa ilustrativo.
- **Alertas:** explicación de una posible actividad insegura, contexto para acompañar al familiar y acción «Marcar como revisada». Una alerta de posible riesgo no se presenta como prueba de fraude.
- **Contacto:** acciones visibles «Enviar ping» y «Aviso de voz». El aviso abre un editor con el ejemplo «Hola Martha, tu hijo está por llegar» y una vista previa de cómo lo recibiría Mamá.

## Recorrido propuesto en la app

1. La persona que acompaña invita a su familiar a un grupo privado.
2. El familiar acepta desde su propio celular y elige por separado compartir actividad/sitios, tiempos de pantalla, ubicación y avisos de seguridad, además de recibir pings y avisos de voz.
3. El grupo ve únicamente los datos autorizados. La sesión activa y la fecha del último registro permanecen visibles.
4. El familiar puede retirar permisos o pausar el acceso. Desactivar Nieto detiene las nuevas actualizaciones; un registro anterior se identifica como tal.
5. Ante una alerta, quien acompaña puede revisar el contexto y comunicarse con su familiar. Revisar la alerta no ejecuta acciones en el otro teléfono.

## Qué permite probar la demo

- **Abrir Grupo familiar:** amplía la tablet que contiene la interfaz, con navegación lateral en escritorio y distribución compacta en celular. Al cerrarla, el dispositivo vuelve a su lugar y conserva sus permisos y estados. La pantalla tiene desplazamiento propio y los diálogos de actividad y voz se ajustan a sus límites.
- **Actividad de Mamá:** desglose ilustrativo de 24 minutos de pantalla (14 de navegador y 10 de WhatsApp), con 12 minutos acompañada por Nieto. Los tiempos son ficticios.
- **Cronología y filtros:** permite ver todos los eventos, los sitios web o las actividades con Nieto. Una actividad puede pertenecer a ambas categorías.
- **Detalle de cada evento:** hora, aplicación, sitio de ejemplo y tiempo compartido, más una explicación de cómo la acompañó Nieto. No se muestran conversaciones ni pantallas privadas.
- **Caso inseguro:** incorpora un evento de alerta a la cronología y permite pasar de su detalle a la pestaña Alertas.
- Pestañas accesibles con teclado: flechas, Inicio y Fin.
- Estados internos «Uso normal», «Sitio inseguro» y «Nieto desactivado». Los selectores de escenarios se ocultan en la presentación de producto; el estado inicial incluye una alerta pendiente para recorrer su revisión.
- Aviso dentro de la página cuando se simula un enlace inseguro.
- Revisión de una alerta y preferencias de notificación del ejemplo.
- Permisos simulados que ocultan los datos correspondientes.
- El permiso de pantalla también controla las duraciones en la cronología y en el detalle; el de actividad oculta los eventos y los filtros.
- Ping y aviso de voz editable de hasta 180 caracteres, con vista previa de recepción.
- Avisos pendientes cuando Nieto está desactivado; al reactivarlo, la interfaz los muestra como preparados. No confirma una recepción externa.
- Permiso separado para recibir pings/voz. Al retirarlo, se deshabilitan ambas acciones y se limpia la vista previa.
- Escucha opcional en el mismo navegador, solo si dispone de una voz local en español. No se reproduce automáticamente; se puede detener. Si no está disponible, el mensaje permanece legible y el envío simulado sigue funcionando.

Los datos, horarios, Martha y el mapa son ficticios. Los dominios mostrados son contenido ilustrativo; no representan historial real de ninguna persona. La demo no pide permisos del navegador, consulta el GPS, lee dispositivos ni envía notificaciones o mensajes a teléfonos. La escucha usa síntesis local del navegador cuando está disponible. Los cambios viven en memoria y se reinician al recargar.

## Qué queda para desarrollar en Android y el backend

El repositorio actual no contiene grupos familiares, cuentas con roles, ubicación compartida, historial de navegación o notificaciones al grupo. Para convertir la propuesta en producto se necesitan autenticación y pertenencia al grupo, invitaciones, permisos revocables, recogida de eventos autorizados, reglas de acceso en servidor, antigüedad/retención de registros, un mecanismo de detección de riesgos y entrega de notificaciones. La cobertura real de sitios y actividad debe definirse durante la implementación; no se presupone acceso a todos los navegadores o aplicaciones.

Esos cambios no forman parte de esta edición de la página y requieren una implementación y validación separadas.

Para pings y avisos de voz también quedan pendientes la entrega autenticada al dispositivo autorizado, la cola y caducidad de mensajes, los estados de recepción y la lectura por voz en Android. Los estados de entrega actuales son únicamente una demostración visual.
