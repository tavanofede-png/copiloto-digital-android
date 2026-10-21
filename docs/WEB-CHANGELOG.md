# Registro de cambios de la página NIETO

## 21 de octubre de 2026

### Integración inicial

- Integrado el HTML original en `web/`, con imágenes locales y CSS/JavaScript separados.
- Añadida vista previa con `npm start`, sin dependencias.
- Conservados los colores, diseño, botones, secciones y comportamiento del HTML original.

### Pedidos durante la edición

- Sección «Qué es»: cambiar pretítulo «Fácil de entender» por «QUÉ ES». Aplicado.
- Sección «Qué es»: cambiar título por «ASISTENTE de SMARTPHONE para adultos mayores». Aplicado.
- Público del producto: personas de 65 a 99 años. Aplicado a la sección «Qué es», descripción de la web y documentación. Revisadas las fuentes Android: no contienen otro rango de edad.

### Corrección de alcance solicitada por el usuario

- Revertidos los cambios no solicitados de colores, diseño, textos, descargas, preguntas frecuentes y ayuda flotante.
- Restaurado el HTML original como base, conservando únicamente los cambios de texto explícitos indicados arriba.
- En adelante, aplicar solamente pedidos explícitos del usuario.
- Al terminar cada cambio solicitado, hacer commit y push a GitHub de inmediato, verificando el resultado.

### Texto debajo del título principal

- Reemplazada la oración por el texto exacto pedido: «Tunieto digital te explica paso a paso, con paciencia y sin seguro. para que uses tu celular con mas confianza a tu ritmo.»

### Alineación del pretítulo «QUÉ ES»

- Alineado su borde izquierdo con el pretítulo «Tu nieto, siempre cerca», respetando el margen interior del hero en escritorio y móvil.
- Conservados el título, el párrafo y el resto de la sección en sus posiciones.

### Nuevo título de la sección «Qué es»

- Reemplazado el título por «Un asistente personal para ayudarte a usar tu Smartphone.», según el texto solicitado.

### Espacio entre el primer bloque y «QUÉ ES»

- Eliminado el espacio doble entre secciones: la separación hasta el pretítulo pasa de 176 px a 32 px.

### Color de «Smartphone» en «Qué es»

- Aplicado a «Smartphone» el mismo azul claro del texto «TE ACOMPAÑA» del título principal, usando la variable de color existente.

### Tres cajas de la sección «Qué es»

- Aplicado el esquema pedido: título, globo de diálogo con una pregunta de ejemplo y texto explicativo.
- Caja 1: pedir ayuda y recibir el paso a paso, con el ejemplo de crear un grupo en WhatsApp.
- Caja 2: ver la misma pantalla y ayudar a destrabarse cuando la persona se pierde.
- Caja 3: usar a Nieto como profesor de tecnología y repetir hasta poder hacerlo por cuenta propia.
- Conservadas la paleta y la estructura general de la página.

### Ajustes de UI/UX pedidos para las cajas

- Añadidos iconos oficiales de Tabler (ayuda, pantalla y aprendizaje), con su licencia en `web/assets/icons/`.
- Añadidos círculos de perfil en los diálogos, preparados para incorporar la foto de la abuela cuando el usuario la entregue.
- Resaltadas las frases clave de los diálogos y explicaciones en negrita.
- Mejoradas las jerarquías, el espaciado y las respuestas visuales al pasar el puntero o usar el teclado, conservando la paleta.
- Añadido «Ver ejemplo de respuesta» desplegable en cada caja. Son ejemplos preparados, identificados como tales.

### Avatares entregados por el usuario

- Integrada la imagen original de los dos perfiles en `web/assets/nieto-avatares.png`.
- Reemplazados los círculos de las preguntas por el perfil de la abuela.
- Añadido el perfil del nieto a las respuestas de ejemplo desplegables.
- Seleccionado cada perfil mediante CSS, conservando el archivo de imagen recibido.

### Respuesta de Nieto dentro del diálogo

- Eliminados los desplegables y el control «Ver ejemplo de respuesta» de las tres cajas.
- Integrada la respuesta de Nieto como un segundo globo siempre visible, junto a la pregunta de la abuela.
- Conservados los avatares y colocado el texto explicativo debajo de la conversación.

### Grupo familiar: próxima función y demo interactiva

- Acordado con el usuario presentarlo como próxima función, con datos ficticios.
- Reemplazada la sección de familias por la propuesta de Grupo familiar, manteniendo la paleta y añadiendo acceso en navegación.
- Añadidas vistas de resumen, sitios/actividad, tiempos de pantalla, ubicación y alertas de seguridad.
- Añadidos casos de prueba, controles de datos compartidos y revisión de alertas; la demo distingue una ubicación anterior de una sesión activa.
- Documentadas las decisiones de producto y los límites de la implementación en `docs/GRUPO-FAMILIAR.md`.
- Esta entrega es una demostración web; no monitorea dispositivos ni modifica el APK o backend.

### Grupo familiar: ping y aviso de voz

- Añadidas las acciones «Enviar ping» y «Aviso de voz» dentro del perfil de Mamá.
- Incorporado el ejemplo pedido: «Nieto, avisale a mamá que estoy yendo» → «Hola Martha, tu hijo está por llegar».
- Añadido editor con vista previa, escucha opcional mediante voz local en español y controles de teclado.
- Simulados los estados recibido/pendiente y un permiso independiente para recibir estos avisos.
- Conservado el alcance acordado: próxima función con demo de ejemplo, sin mensajes reales ni cambios en Android/backend.

### Unificación del UI/UX con el diseño aprobado de «Qué es»

- Conservadas la paleta azul, las ilustraciones, los textos y la navegación. Se tomó como referencia la iconografía en recuadros de 44 px, las tarjetas redondeadas y la jerarquía tipográfica aprobada.
- Reducido el padding habitual entre secciones de 88 a 36 px por lado en escritorio y a 28 px en celular; compactados también encabezados, cierre y footer.
- Alineados los pretítulos con el margen interior del hero y añadidos acentos azules y negritas en las frases principales.
- Actualizadas las tres tarjetas de «Cómo te ayuda» con iconos oficiales de Tabler, pasos identificados y el mismo tratamiento de bordes y hover de «Qué es».
- Reorganizada la presentación de los gestos de Nieto con un encabezado común, iconos en sus beneficios y cuatro tarjetas visuales.
- Alineado Grupo familiar al mismo sistema de encabezado y espaciado, conservando sus interacciones y la indicación de próxima función.
- Unificados los iconos de botones, estados de interacción y ayuda flotante. Corregido el ajuste de títulos en pantallas de 320 px.

### Indicador verde animado en todos los pretítulos

- Cambiado el punto de los seis pretítulos a verde, incluido «Tu nieto, siempre cerca».
- Añadido un pulso titilante suave de 1,8 segundos, compartido por todas las secciones y sin desplazar el contenido.
- Conservado el indicador verde estático cuando el dispositivo solicita movimiento reducido.

### «Cómo te ayuda»: aprendizaje y autonomía

- Reorientada la sección para explicar que Nieto enseña a realizar las tareas y no las hace en lugar de la persona.
- Nuevo título: «Aprendé con Nieto, hacelo por tu cuenta».
- Organizado el recorrido en seguir la guía, practicar y repetir, e incorporar el proceso para aplicarlo por cuenta propia.
- Conservados el diseño, las negritas y el indicador verde animado; ajustados los iconos al contenido de los pasos.

### Mockup navegable de la interfaz de Grupo familiar

- Añadido «Explorar el mockup de la app» para ampliar la misma demo sin duplicar controles ni estados.
- Incorporada la vista «Actividad de Mamá» con desglose de uso por aplicación, cronología, filtros y detalle de cada evento.
- Añadido un evento de sitio sospechoso en el caso de alerta, con acceso a su revisión.
- Conservados ubicación, permisos, ping y aviso de voz dentro del mockup. El diseño se adapta a escritorio y celular.
- Mantenido el alcance acordado: datos ficticios y próxima función, sin monitorización real ni cambios en Android/backend.

### Retirado el pretítulo «QUÉ ES»

- Eliminada la etiqueta de pretítulo de esa sección, según el pedido del usuario.
- Conservados su título principal, el contenido, las tarjetas y el enlace de navegación.

### Nuevos títulos de las cajas de «Qué es»

- Caja 1: «Ayuda Paso a Paso».
- Caja 2: «Nieto Sabe Dónde Estás Parado».
- Caja 3: «De Asistente a Tutor».
- Conservados los diálogos, avatares, iconos y textos explicativos.

### CTA final de descarga de Nieto

- Reemplazada la sección «Próximamente» por «Descargá Nieto», con un CTA principal de descarga para Android.
- Enlazado el APK publicado `copiloto-0.2.0-debug.apk` de la release `v0.2.0`, verificado mediante la API de GitHub.
- Eliminados el correo de ejemplo y el CTA de novedades.
- Añadidas la indicación de versión de prueba, compatibilidad con Android 8+, requisito de código de conexión e instrucciones de instalación.

### Presentación visual de producto

- Retiradas de la interfaz las etiquetas de mockup, demo, datos ficticios, próxima función y prueba, según el pedido del usuario.
- Actualizados los textos de detalles, alertas, voz, estados y accesibilidad con lenguaje de producto.
- Cambiado el acceso principal a «Abrir Grupo familiar» y ocultados los selectores de escenarios.
- Conservadas las preferencias y los controles existentes. Los avisos preparados no confirman recepción en otro teléfono.
- Esta edición modifica la presentación web; no añade integraciones de Android, GPS ni mensajería.

### Corrección del texto debajo del título principal

- Corregido «Tunieto digital» por «Tu nieto digital».

### Texto de edad y salto de línea en «Qué es»

- Cambiado a «Pensado para adultos entre 65 a 100 años de edad.».
- Colocada en la línea siguiente la oración que comienza con «NIETO transforma».
- Actualizada la edad máxima de la descripción de la página a 100 años.

### Interfaz de Grupo familiar dentro de una tablet

- Enmarcada toda la interfaz en una tablet con bisel oscuro, cámara y pantalla desplazable.
- Conservado el dispositivo al ampliar Grupo familiar y al volver a la sección, compartiendo el mismo estado.
- Adaptada la navegación a la pantalla horizontal y a la orientación vertical en celular.
- Ajustados los diálogos de actividad y voz para que se abran dentro de los límites de la pantalla de la tablet.
- Conservados los textos, la paleta de la app y sus controles.

### Publicación de la página en Vercel

- Publicado el sitio estático en https://nieto-web.vercel.app, en un proyecto separado de la API.
- Añadida configuración para servir `web/` sin instalación ni compilación.
- Excluidos del despliegue los archivos locales, las variables de entorno y la documentación.
- Documentado el comando de publicación. El despliegue automático desde GitHub queda sin conectar.

### Despliegues automáticos desde GitHub

- Conectado el proyecto `nieto-web` con `tavanofede-png/copiloto-digital-android`, con autorización explícita del usuario.
- Configurado `web` como directorio raíz y `main` como rama de producción.
- Las actualizaciones publicadas en `main` se despliegan automáticamente en https://nieto-web.vercel.app.
- Actualizadas las instrucciones de publicación para utilizar la integración de GitHub.

### Video de presentación de 40 segundos

- Creado un video horizontal Full HD con narración en español argentino y subtítulos sincronizados.
- Incluye ayuda paso a paso, aprendizaje, la interfaz de Grupo familiar, ubicación compartida, alertas, aviso de voz y descarga.
- Conservados los colores, ilustraciones y avatares del producto.
- Añadidos el MP4, los subtítulos y la portada como archivos independientes para compartir desde la web.

### Nueva voz y video a 1,1×

- Reemplazada la narración por una nueva voz masculina argentina, con menos pausas entre frases.
- Acelerado el montaje completo a 1,1×, con duración final de aproximadamente 36,4 segundos.
- Ajustados los subtítulos a la nueva voz y velocidad; conservado el tono al acelerar el audio.
- Actualizado el archivo público conservando el mismo enlace.

Este registro se actualiza con los pedidos recibidos durante la sesión.
