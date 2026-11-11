# Prompt para la extensión de Chrome — Añadir el permiso de Drive completo

Para ver todo tu Drive, buscar, y compartir proyectos con otra persona, Alfred necesita el permiso **Drive completo**
(`drive`) en lugar del limitado (`drive.file`). Pega esto en la extensión de Claude en Chrome:

---

En Google Cloud Console, proyecto **Alfred Asistente**, añade un permiso (scope) al consentimiento de la app **Alfred**:

1. Abre https://console.cloud.google.com/auth/scopes con el proyecto `Alfred Asistente` seleccionado.
2. Pulsa **Añadir o quitar permisos**.
3. En «Añadir permisos manualmente» pega: `https://www.googleapis.com/auth/drive` y pulsa **Añadir a la tabla**.
4. Deja marcados los demás que ya estaban (userinfo.email, userinfo.profile, calendar.events, gmail.modify, contacts). El antiguo `drive.file` puede quedarse o quitarse.
5. Pulsa **Actualizar** y **Guardar**.
6. Comprueba que el estado de publicación sigue en **En pruebas (Testing)** y que mi correo sigue en Usuarios de prueba.
7. Confirma que Google Drive API sigue habilitada: https://console.cloud.google.com/apis/library/drive.googleapis.com

No cambies nada más. No crees claves ni clientes nuevos. Dime al terminar qué permisos quedan en la lista.

---

## Después, en el móvil

Abre Alfred. Te pedirá aceptar el permiso nuevo (una vez): marca todas las casillas. También puedes ir a Drive → «Conceder permiso»
o a Ajustes → Cambiar de cuenta.

> Aviso: `drive` es un permiso *restringido* de Google. Con la app **en pruebas** (hasta 100 usuarios que añadas tú) funciona sin más.
> Para abrirla al público habría que pasar la verificación de Google, que para permisos restringidos incluye una auditoría de seguridad.
