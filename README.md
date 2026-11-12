# Alfred — asistente personal para Android

App nativa (Kotlin + Jetpack Compose). Sin servidor: todo ocurre entre el móvil, la cuenta de Google del usuario y Gemini.

## Interfaz

Tema Pokémon sobre fondo claro: cabecera roja de Pokédex con degradado y lente brillante, tarjetas blancas con sombra suave, barra inferior flotante con la pestaña activa subrayada, tipografía jerarquizada y una textura de iconos de juego casi invisible (`tools/make_bg.py`). Barra inferior: **Correo (Dragonite) · Agenda (Blastoise) · ASISTENTE (Pikachu, centro, grande) · Notas (Psyduck) · Drive**. Los contactos (Lapras) están dentro de Correo. Buscar y Ajustes (los engranajes) están en la cabecera de todas las pestañas. El icono de la app es la Pokéball con Pikachu.

Las ilustraciones se procesan con `python3 tools/process_art.py <7 imágenes>` (recorta, quita el fondo y genera los recursos).

**Aviso**: los personajes son de Nintendo / Game Freak / The Pokémon Company. Vale para uso personal y APK propio; **no se pueden publicar en Google Play ni distribuir públicamente** sin permiso, porque infringirían marca y derechos de autor.

**Alfred controla el resto de la app.** Además de actuar sobre Calendar, Gmail, Contactos, notas, tareas, plantillas y Drive (crear, editar y borrar), puede cambiar de pantalla («abre mis tareas»), abrir una nota, dejar un correo redactado para que lo revises, abrir el escáner o la grabadora y situar la agenda en un mes. Envíos y borrados piden confirmación.

## Actualizaciones automáticas

Al abrirse, la app busca una versión nueva en GitHub Releases, la descarga y abre el instalador. Cada `git push` compila, firma y publica la versión; ver [`docs/ACTUALIZACIONES.md`](docs/ACTUALIZACIONES.md) (incluye lo que hay que preparar una vez).

## Modelo de IA gratuito

Alfred usa la API de Gemini con tu propia clave de Google AI Studio. Los modelos **Flash** y **Flash-Lite** tienen capa gratuita con límites por minuto y por día (Flash-Lite, más cupo; Flash, mejor calidad); los **Pro** son de pago. Si el modelo elegido agota su cupo, Alfred prueba solo con Flash-Lite. Los límites exactos se ven en Google AI Studio. Con la capa gratuita, Google puede usar lo que envíes para mejorar sus productos; con clave de pago, no.

## Notificaciones

- **Tareas**: avisan a la hora que indiques; si solo tienen fecha, a las 9:00. La notificación tiene botón «Hecho».
- **Citas de Google Calendar**: aviso 0–120 min antes (por defecto 15); las de todo el día, a las 8:00.
- Funcionan con la app cerrada (AlarmManager) y se reprograman al reiniciar el móvil; cada 3 h se vuelve a leer el calendario de los próximos 14 días. Ajustes → Notificaciones.
- Alfred también las crea: «recuérdame mañana a las 18:00 llamar a Ana».

## Qué hace

- **Chat con Alfred**: asistente conversacional completo, como Gemini (cultura general, explicaciones, traducciones, textos, ideas, decisiones, charla), con búsqueda en internet para datos actuales y modo voz. Además, texto, voz e imágenes. Gemini con herramientas reales: crea, modifica y borra eventos, busca y lee correos, redacta borradores, envía correos (con confirmación), busca y crea contactos, y gestiona notas, tareas y plantillas.
- **Agenda**: Google Calendar por meses; crear, editar y eliminar eventos con invitados.
- **Notas**: con fotos (galería o cámara), documentos escaneados, audios y etiquetas. Buscar, fijar, compartir.
  - **Escáner**: detecta bordes y recorta; guarda PDF + imagen y la IA extrae emisor, fecha, total y texto del ticket o factura.
  - **Transcripción**: graba una nota de voz o elige/comparte un audio (p. ej. de WhatsApp) y lo transcribe.
  - **Compartir con Alfred** desde cualquier app: texto, imágenes, audios y PDF.
- **Tareas** con fecha límite y **plantillas** de texto reutilizables en correos.
- **Correo**: Gmail (recibidos, sin leer, enviados, borradores, búsqueda), leer, responder, redactar, papelera, guardar un correo como nota.
- **Contactos** de Google: buscar, crear, eliminar, llamar o escribir.
- **Drive** (pestaña propia): explora tu Drive, busca, sube archivos, crea carpetas, documentos y hojas de Google, y **comparte proyectos con otra persona** (editor o lector) para trabajar a la vez. «Compartido conmigo» y «Proyectos». Además, la carpeta `Alfred` con `Notas`, `Escaneos`, `Facturas` y `Adjuntos`, subida automática de escaneos y copia de seguridad. Necesita el permiso de Drive completo (`docs/PROMPT_CHROME_ACTUALIZAR_DRIVE.md`).
- **Agenda**: pestaña propia (Google Calendar) con vistas **Día, Semana y Mes** y navegación por fechas bajo la cabecera.
- **Contactos**: dentro de la pestaña Correo (Correo · Contactos).
- **Búsqueda global** (lupa del asistente): notas, tareas, plantillas, agenda, correo, contactos y Drive a la vez.
- **Correo con adjuntos**: ver, abrir, guardar en nota o en Drive los adjuntos recibidos, y adjuntar archivos al enviar.
- **Voz**: modo «Conversar» (escucha, responde hablando y vuelve a escuchar) y lectura de respuestas en voz alta.
- **Compartir con el asistente** desde otra app: resumir, crear tareas, preguntar o guardar como nota.
- **Resumen del día** cada mañana (citas, tareas y correos sin leer) y **widget** de inicio con atajos a nota de voz y asistente.

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
