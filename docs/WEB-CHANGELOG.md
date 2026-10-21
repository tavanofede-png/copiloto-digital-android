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

Este registro se actualiza con los pedidos recibidos durante la sesión.
