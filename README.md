# Alfred — asistente personal para Android

App nativa (Kotlin + Jetpack Compose). Sin servidor: todo ocurre entre el móvil, la cuenta de Google del usuario y Gemini.

## Interfaz

Estilo retro de 16 bits pero legible: iconos pixel-art con etiqueta, marcos duros y títulos en fuente pixel. Barra inferior con cinco botones: **Correo · Agenda · ALFRED (centro, grande) · Notas · Contactos**. Drive y Ajustes están arriba, en la pantalla de Alfred.

**Alfred controla el resto de la app.** Además de actuar sobre Calendar, Gmail, Contactos, notas, tareas, plantillas y Drive (crear, editar y borrar), puede cambiar de pantalla («abre mis tareas»), abrir una nota, dejar un correo redactado para que lo revises, abrir el escáner o la grabadora y situar la agenda en un mes. Envíos y borrados piden confirmación.

## Qué hace

- **Chat con Alfred** (texto, voz e imágenes). Gemini con herramientas reales: crea, modifica y borra eventos, busca y lee correos, redacta borradores, envía correos (con confirmación), busca y crea contactos, y gestiona notas, tareas y plantillas.
- **Agenda**: Google Calendar por meses; crear, editar y eliminar eventos con invitados.
- **Notas**: con fotos (galería o cámara), documentos escaneados, audios y etiquetas. Buscar, fijar, compartir.
  - **Escáner**: detecta bordes y recorta; guarda PDF + imagen y la IA extrae emisor, fecha, total y texto del ticket o factura.
  - **Transcripción**: graba una nota de voz o elige/comparte un audio (p. ej. de WhatsApp) y lo transcribe.
  - **Compartir con Alfred** desde cualquier app: texto, imágenes, audios y PDF.
- **Tareas** con fecha límite y **plantillas** de texto reutilizables en correos.
- **Correo**: Gmail (recibidos, sin leer, enviados, borradores, búsqueda), leer, responder, redactar, papelera, guardar un correo como nota.
- **Contactos** de Google: buscar, crear, eliminar, llamar o escribir.
- **Drive**: carpeta `Alfred` con subcarpetas `Notas`, `Escaneos` y `Facturas`; subida automática de escaneos y copia de seguridad.

## Configuración (una sola vez)

1. **Google Cloud**: sigue [`docs/PROMPT_CHROME_GOOGLE.md`](docs/PROMPT_CHROME_GOOGLE.md) (pensado para pegarlo en la extensión de Claude en Chrome).
2. **En la app**: Más → Ajustes → pegar la clave de Gemini y conectar la cuenta de Google.

Identidad de la app (lo que Google necesita):

| | |
|---|---|
| Paquete | `com.iatualcance.alfred` |
| SHA-1 | `DD:56:97:2D:D5:CE:FC:E3:51:49:53:6A:18:F2:68:E6:03:DE:81:00` |

No hay IDs de cliente ni claves en el código. La clave de Gemini la introduce cada usuario y se guarda cifrada en su móvil.

## Compilar

```bash
./gradlew assembleRelease
```

El APK queda en `app/build/outputs/apk/release/app-release.apk`. Cada push compila también en GitHub Actions (artefacto `Alfred-apk`).

### Firma

Todos los APK deben ir firmados con la misma clave (`alfred.jks`) para que la SHA-1 registrada en Google siga valiendo y las actualizaciones se instalen encima. La clave **no está en el repositorio**; guárdala en un sitio seguro.

- En local: copia `alfred.jks` a `keystore/alfred.jks` y crea `keystore.properties` en la raíz:

  ```
  file=keystore/alfred.jks
  password=LA_CONTRASEÑA
  alias=alfred
  ```

- En GitHub Actions: crea los secretos `ALFRED_KEYSTORE_BASE64` (el fichero en base64) y `ALFRED_PASSWORD`.

Sin la clave el APK se compila igual, pero firmado con una clave de depuración: se instala y funciona todo menos la conexión con Google.

## Estructura

```
app/src/main/java/com/iatualcance/alfred/
  data/     modelos, almacenamiento local (JSON) y ajustes
  google/   inicio de sesión + REST de Calendar, Gmail, Contactos y Drive
  ai/       cliente de Gemini y asistente con herramientas
  ui/       pantallas (Compose)
```

## Pendiente / abierto

- Pasar de 100 usuarios exige la verificación de Google (Gmail es un permiso restringido) y, para cobrar, un pequeño backend que guarde la clave de IA y valide la suscripción. La arquitectura lo permite sin rehacer la app: bastaría con cambiar `ai/Gemini.kt` para que llame a ese backend.
