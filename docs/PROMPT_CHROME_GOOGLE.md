# Prompt para la extensión de Claude en Chrome — Configurar Google para Alfred

Copia **todo** lo que hay debajo de la línea y pégalo en la extensión de Claude en Chrome,
con la sesión iniciada en la cuenta de Google que vaya a ser dueña del proyecto.

---

Vas a configurar desde cero, en Google Cloud Console y en Google AI Studio, todo lo que necesita una app Android llamada **Alfred** para iniciar sesión con Google y usar Calendar, Gmail, Contactos y Drive. Hazlo de principio a fin tú solo, paso a paso, comprobando cada paso antes de pasar al siguiente. La app ya está compilada: **no hay que tocar código, ni descargar ningún JSON, ni crear ningún "ID de cliente web" ni secretos.**

## Datos exactos (cópialos tal cual, sin cambiar nada)

| Dato | Valor |
|---|---|
| Nombre del proyecto de Google Cloud | `Alfred Asistente` |
| Nombre de la app (pantalla de consentimiento) | `Alfred` |
| Nombre del paquete Android | `com.iatualcance.alfred` |
| Huella digital del certificado SHA-1 | `DD:56:97:2D:D5:CE:FC:E3:51:49:53:6A:18:F2:68:E6:03:DE:81:00` |
| Nombre del cliente OAuth | `Alfred Android` |
| Tipo de usuario / público | **Externo** |
| Estado de publicación | **En pruebas (Testing)** — no lo pases a producción |
| Correo de asistencia, de contacto y usuario de prueba | el correo de la cuenta de Google con la que está iniciada la sesión en este navegador |

Permisos (scopes) que hay que añadir, exactamente estos seis:

```
https://www.googleapis.com/auth/userinfo.email
https://www.googleapis.com/auth/userinfo.profile
https://www.googleapis.com/auth/calendar.events
https://www.googleapis.com/auth/gmail.modify
https://www.googleapis.com/auth/contacts
https://www.googleapis.com/auth/drive.file
```

## Reglas

- Si la consola está en otro idioma o los menús han cambiado de nombre o de sitio, busca el equivalente; lo que importa es el resultado de cada paso.
- Detente y pregúntame solo si: pide iniciar sesión, verificación en dos pasos, aceptar condiciones legales en mi nombre que no sean las condiciones estándar de Google Cloud / AI Studio, o datos de facturación. **Nada de esto requiere facturación ni tarjeta**; si algo la pide, no la actives y avísame.
- No crees claves de API de Google Cloud, cuentas de servicio, clientes "Aplicación web" ni secretos. No envíes la app a verificación. No borres ni modifiques otros proyectos existentes.
- Si ya existe un proyecto llamado `Alfred Asistente`, úsalo y completa lo que falte en vez de crear otro.

## Paso 1 — Crear el proyecto

1. Abre `https://console.cloud.google.com/projectcreate`.
2. Nombre del proyecto: `Alfred Asistente`. Organización/ubicación: deja lo que venga por defecto ("Sin organización" si aparece). Pulsa **Crear**.
3. Espera a que se cree y **selecciónalo** en el selector de proyectos de la barra superior. Comprueba que el nombre `Alfred Asistente` aparece arriba antes de seguir. Apunta el **ID del proyecto**.

## Paso 2 — Activar las 4 API

Con el proyecto `Alfred Asistente` seleccionado, abre cada enlace y pulsa **Habilitar** (si ya pone "Administrar" o "API habilitada", ya está):

1. Google Calendar API — `https://console.cloud.google.com/apis/library/calendar-json.googleapis.com`
2. Gmail API — `https://console.cloud.google.com/apis/library/gmail.googleapis.com`
3. People API (contactos) — `https://console.cloud.google.com/apis/library/people.googleapis.com`
4. Google Drive API — `https://console.cloud.google.com/apis/library/drive.googleapis.com`

Comprobación: en `https://console.cloud.google.com/apis/dashboard` deben aparecer las cuatro como habilitadas.

## Paso 3 — Pantalla de consentimiento (Google Auth Platform)

1. Abre `https://console.cloud.google.com/auth/overview` (si redirige a "Pantalla de consentimiento de OAuth" es lo mismo). Pulsa **Comenzar / Get started** si aparece.
2. **Información de la app**: nombre `Alfred`; correo de asistencia = el correo de la cuenta con la sesión iniciada.
3. **Público / Audience**: **Externo**.
4. **Información de contacto**: el mismo correo.
5. Acepta la política de datos de usuario de los servicios de API de Google y pulsa **Crear**.
6. No hace falta logotipo, página de inicio, política de privacidad ni dominios autorizados: déjalos vacíos.

## Paso 4 — Usuarios de prueba

1. Ve a **Público / Audience** (`https://console.cloud.google.com/auth/audience`).
2. Comprueba que el estado de publicación es **En pruebas / Testing**. Si pone "En producción", pulsa "Volver a pruebas".
3. En **Usuarios de prueba** pulsa **Añadir usuarios**, escribe el correo de la cuenta con la sesión iniciada y guarda. (Si te he indicado más correos en este chat, añádelos también; el máximo es 100.)

## Paso 5 — Permisos (scopes)

1. Ve a **Acceso a los datos / Data access** (`https://console.cloud.google.com/auth/scopes`).
2. Pulsa **Añadir o quitar permisos**.
3. En el cuadro "Añadir permisos manualmente" (al final del panel) pega los seis permisos de la lista de arriba, uno por línea o separados por comas, y pulsa **Añadir a la tabla**. Asegúrate de que los seis quedan marcados y pulsa **Actualizar**.
4. Pulsa **Guardar** al final de la página.
5. Comprobación: `calendar.events` y `contacts` deben aparecer como permisos sensibles, `gmail.modify` como restringido y `drive.file`, `userinfo.email`, `userinfo.profile` como no sensibles. Los seis deben estar en la lista.

## Paso 6 — Cliente OAuth de Android

1. Ve a **Clientes / Clients** (`https://console.cloud.google.com/auth/clients`) y pulsa **Crear cliente**.
2. Tipo de aplicación: **Android**.
3. Nombre: `Alfred Android`.
4. Nombre del paquete: `com.iatualcance.alfred`
5. Huella digital del certificado SHA-1: `DD:56:97:2D:D5:CE:FC:E3:51:49:53:6A:18:F2:68:E6:03:DE:81:00`
6. Deja lo demás por defecto y pulsa **Crear**. Cierra el diálogo (no hace falta descargar nada).
7. Comprobación: abre el cliente recién creado y verifica, carácter a carácter, que el paquete y la SHA-1 coinciden con los de la tabla. Si Google dice que ese paquete + SHA-1 ya está en uso en otro proyecto, párate y dímelo.

## Paso 7 — Clave de Gemini (Google AI Studio)

1. Abre `https://aistudio.google.com/apikey`. Acepta las condiciones de AI Studio si es la primera vez.
2. Pulsa **Crear clave de API**. Si pregunta el proyecto, elige `Alfred Asistente`. Nombre de la clave: `Alfred`.
3. Cuando aparezca la clave (empieza por `AIza…`), **déjala visible en pantalla y avísame para que la copie yo**. No la escribas en el chat, ni en ninguna otra página, ni la guardes en ningún sitio.

## Paso 8 — Informe final

Dame un resumen con:

- ID del proyecto de Google Cloud.
- Las 4 API: habilitadas sí/no.
- Pantalla de consentimiento: nombre, público (Externo), estado (En pruebas), usuarios de prueba añadidos.
- Los 6 permisos añadidos sí/no.
- Cliente Android: nombre, paquete y SHA-1 tal como han quedado guardados.
- Clave de Gemini creada sí/no (sin mostrar la clave).
- Cualquier paso que no hayas podido completar y por qué.

## Para mí (el usuario), después

1. Instalar `Alfred.apk` en el móvil.
2. Abrir Alfred → **Más → Ajustes** → pegar la clave de Gemini → **Guardar y probar**.
3. En Ajustes → **Conectar con Google**, elegir la cuenta añadida como usuario de prueba. Saldrá el aviso "Google no ha verificado esta aplicación": pulsar **Continuar** y **marcar todas las casillas de permisos**.
4. Los cambios de Google Cloud pueden tardar unos minutos en aplicarse. Si sale "error 10", esperar 5–10 minutos y reintentar; si sigue, revisar paquete y SHA-1 del paso 6.
