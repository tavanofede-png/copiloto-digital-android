# NIETO · Página web

Landing integrada desde el HTML original para adultos mayores de **65 a 99 años**. Conserva las ilustraciones, la paleta azul y el tono de NIETO. Android y backend mantienen sus proyectos independientes.

## Ver y editar

Desde la raíz del repositorio, con Node.js 18 o posterior:

```sh
npm start
```

Abrir `http://127.0.0.1:4173`. No hace falta instalar dependencias ni compilar. La variable `PORT` permite elegir otro puerto. El servidor solo expone `web/` y escucha en la computadora local.

- `index.html`: página original con los cambios de texto pedidos por el usuario.
- `assets/site.css`: estilos originales, sin cambios de colores ni diseño.
- `assets/site.js`: comportamiento original de la ayuda flotante.
- `assets/family.css` y `assets/family.js`: propuesta visual y demo interactiva de Grupo familiar.
- `assets/*.png`: imágenes extraídas del HTML original, con dimensiones declaradas.

## Alcance de la edición

Se conserva la identidad del HTML original. Las ediciones aprobadas incluyen los textos para el público de 65 a 99 años, las cajas con diálogos y avatares de «Qué es», y la sección Grupo familiar como próxima función con datos ficticios.

La demo de Grupo familiar permite recorrer Resumen, Actividad, Ubicación y Alertas, cambiar situaciones y simular permisos. También incluye un ping y un aviso de voz editable, con vista previa y estados de entrega simulados. La escucha opcional utiliza una voz local en español del navegador, cuando está disponible. No monitorea dispositivos, solicita acceso al GPS ni envía mensajes reales. Las decisiones de producto y el trabajo pendiente para Android/backend están en [`docs/GRUPO-FAMILIAR.md`](../docs/GRUPO-FAMILIAR.md).

No realizar cambios de diseño, colores, contenido, secciones ni funciones sin un pedido explícito del usuario.

## Publicar

La carpeta `web/` es el sitio estático completo y se puede publicar sin build. Configurar esa carpeta como directorio público, manteniendo `index.html` y `assets/` juntos. No publicar la raíz del repositorio, que también contiene Android y el backend. Esta integración no cambia el despliegue del backend existente.
