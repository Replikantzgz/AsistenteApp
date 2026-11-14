package com.iatualcance.alfred.ai

import com.iatualcance.alfred.Util
import com.iatualcance.alfred.data.Note
import com.iatualcance.alfred.data.Settings
import com.iatualcance.alfred.data.Store
import com.iatualcance.alfred.data.Task
import com.iatualcance.alfred.data.Template
import com.iatualcance.alfred.google.GoogleApi
import com.iatualcance.alfred.google.arr
import com.iatualcance.alfred.google.obj
import com.iatualcance.alfred.google.str
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Órdenes que el asistente da a la interfaz (abrir pantallas, abrir una nota, redactar…). */
sealed interface Nav {
    data class Screen(val name: String) : Nav
    data class Note(val id: String) : Nav
    data class Compose(val to: String, val subject: String, val body: String) : Nav
    data class Calendar(val date: LocalDate) : Nav
    data object Scan : Nav
    data object Record : Nav
}

data class Reply(val text: String, val actions: List<String>, val nav: List<Nav> = emptyList())

/**
 * El "cerebro" de Alfred: le pasa a Gemini las herramientas disponibles, ejecuta las que pida
 * (calendario, correo, contactos, notas, tareas, plantillas) y repite hasta tener la respuesta final.
 * [confirm] se llama antes de acciones delicadas (enviar un correo, borrar un evento).
 */
class Assistant(
    private val gemini: Gemini, private val google: GoogleApi, private val store: Store, private val settings: Settings,
    private val uploadNote: suspend (Note, String?) -> Note, private val backup: suspend () -> String,
) {
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    /** Se pone a true si en la petición en curso se ha saltado al modelo avanzado (para contarlo en el consumo). */
    @Volatile var advancedUsed = false

    private fun system(voice: Boolean): String {
        val now = ZonedDateTime.now()
        val acc = settings.account.value
        return """
Eres Alfred, un asistente de IA conversacional completo, como Gemini: puedes hablar de cualquier tema, responder preguntas de cultura general, explicar, resumir, traducir, escribir y corregir textos, dar ideas, ayudar a decidir, hacer cálculos, programar, planificar viajes o estudios, o simplemente charlar. Además tienes acceso a la agenda, el correo, los contactos, las notas, las tareas y el Drive del usuario, y puedes actuar sobre ellos con herramientas.
Tono: cercano, amable y natural, con un toque de mayordomo moderno (ingenioso, nunca empalagoso). Respondes en español salvo que el usuario escriba en otro idioma.
Fecha y hora actuales: ${now.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy, HH:mm", Util.ES))} (zona ${now.zone.id}). Hoy es ${now.toLocalDate()}.
Usuario: ${acc?.name ?: "sin cuenta de Google conectada"}${acc?.let { " <${it.email}>" } ?: ""}.

Cómo conversar:
- Si el usuario solo quiere charlar o preguntar algo, responde con normalidad, sin hablar de tareas ni de la app y sin empujarle hacia tus herramientas.
- Adapta la longitud: breve para acciones y datos sueltos; completa y bien explicada cuando pida explicar, comparar o aconsejar. Puedes usar listas y negritas con moderación.
- Haz preguntas de seguimiento cuando ayuden (aclarar qué quiere, ofrecer opciones, proponer el siguiente paso), una o dos como mucho, sin interrogatorios.
- Recuerda lo hablado antes en esta conversación y úsalo.
- Para cualquier cuenta, porcentaje o conversión usa SIEMPRE la herramienta calcular. Si la pregunta exige razonar o investigar a fondo (comparar, planear, buscar lo más cercano) usa consulta_avanzada; el resto lo resuelves tú sin ella.
- Si no sabes algo o puede haber cambiado (noticias, precios, resultados, datos recientes), usa buscar_en_internet y menciona de dónde sale. No inventes datos; si dudas, dilo.
- En opiniones o decisiones personales, da tu criterio razonado y deja la decisión al usuario.
${if (voice) "- Estás en una CONVERSACIÓN HABLADA: contesta con frases naturales y cortas, como hablarías, sin listas, tablas, símbolos, emojis ni enlaces, y termina con una pregunta breve si ayuda a seguir hablando." else ""}

Cuando el usuario pida hacer algo en su cuenta, eres el CENTRO DE MANDO de la app: la app tiene cinco pestañas (Correo —con Contactos—, Agenda, tú, Notas —con Tareas y Plantillas— y Drive) más Ajustes y la búsqueda global. Puedes hacer por herramientas todo lo que el usuario puede hacer tocando la pantalla.
- Si pide "abre", "ve a", "enséñame", "muéstrame" una sección o un elemento, usa las herramientas abrir_*: tras tu respuesta la app cambiará a esa pantalla. Si solo pide información (qué tengo mañana), respóndela tú sin cambiar de pantalla.
- Usa las herramientas para actuar de verdad; nunca digas que has hecho algo si no has llamado a la herramienta y ha respondido bien. Si una herramienta devuelve un error, explícalo con claridad.
- Puedes encadenar varias herramientas en una misma petición (por ejemplo, buscar un contacto y después enviarle un correo o invitarle a un evento).
- Las fechas van en hora local con formato AAAA-MM-DDTHH:MM. Calcula tú las fechas relativas ("mañana", "el viernes") a partir de la fecha actual. Si no dicen hora para una cita, pregunta o usa una razonable y dilo.
- Antes de modificar o eliminar un evento, o de responder a un correo, búscalo primero para obtener su id.
- Para enviar un correo necesitas una dirección: si solo te dan un nombre, búscalo en contactos. Si el usuario pide "redactar" o "preparar", crea un borrador; envía solo si pide enviar.
- Drive: puedes buscar archivos, crear documentos de Google, crear PROYECTOS compartidos (una carpeta en Drive que se comparte con otra persona para trabajar juntos) y compartir archivos o carpetas con alguien por su correo. Antes de compartir, el usuario confirma. Para trabajar dos personas en lo mismo, crea un proyecto y comparte la carpeta; los documentos de Google se editan a la vez desde la app de Docs.
- Búsqueda global en la app: abre la pantalla "buscar" si el usuario quiere buscar algo en sus notas, correo, agenda, contactos y Drive.
- Las tareas y recordatorios van como tarea (el móvil avisa con una notificación): si piden una hora, usa `recordatorio`; si solo dan el día, usa `fecha_limite` (avisa a las 9:00). Las citas del calendario avisan solas unos minutos antes. La información para guardar va como nota.
- No inventes datos del usuario (direcciones, teléfonos, importes). Si falta algo imprescindible para una acción, pregunta.
""".trim()
    }

    // ---------------------------------------------------------------- Declaración de herramientas
    private fun s(desc: String) = buildJsonObject { put("type", "STRING"); put("description", desc) }
    private fun b(desc: String) = buildJsonObject { put("type", "BOOLEAN"); put("description", desc) }
    private fun i(desc: String) = buildJsonObject { put("type", "INTEGER"); put("description", desc) }
    private fun list(desc: String) = buildJsonObject {
        put("type", "ARRAY"); put("description", desc); putJsonObject("items") { put("type", "STRING") }
    }

    private fun fn(name: String, desc: String, props: Map<String, JsonObject> = emptyMap(), required: List<String> = emptyList()) =
        buildJsonObject {
            put("name", name); put("description", desc)
            if (props.isNotEmpty()) putJsonObject("parameters") {
                put("type", "OBJECT")
                put("properties", JsonObject(props))
                if (required.isNotEmpty()) putJsonArray("required") { required.forEach { add(JsonPrimitive(it)) } }
            }
        }

    private val eventProps = mapOf(
        "titulo" to s("Título del evento"),
        "inicio" to s("Inicio en hora local, AAAA-MM-DDTHH:MM (o AAAA-MM-DD si es de todo el día)"),
        "fin" to s("Fin en hora local, AAAA-MM-DDTHH:MM. Por defecto una hora después del inicio"),
        "todo_el_dia" to b("true si el evento dura todo el día"),
        "descripcion" to s("Descripción o notas"),
        "lugar" to s("Lugar"),
        "invitados" to list("Correos electrónicos de los invitados"),
    )

    val tools: JsonArray = buildJsonArray {
        add(fn("crear_evento", "Crea un evento en Google Calendar.", eventProps, listOf("titulo", "inicio")))
        add(fn("listar_eventos", "Lista los eventos de Google Calendar entre dos fechas.", mapOf(
            "desde" to s("Fecha inicial AAAA-MM-DD"), "hasta" to s("Fecha final AAAA-MM-DD (incluida)"),
            "texto" to s("Texto a buscar (opcional)")), listOf("desde", "hasta")))
        add(fn("modificar_evento", "Modifica un evento existente. Indica solo los campos que cambian.",
            mapOf("id" to s("Id del evento, obtenido con listar_eventos")) + eventProps, listOf("id")))
        add(fn("eliminar_evento", "Elimina un evento del calendario.", mapOf("id" to s("Id del evento"), "titulo" to s("Título del evento, para confirmarlo con el usuario")), listOf("id")))
        add(fn("buscar_correos", "Busca correos en Gmail. Devuelve remitente, asunto, fecha y un extracto.", mapOf(
            "consulta" to s("Consulta con sintaxis de búsqueda de Gmail, p. ej. 'in:inbox is:unread', 'from:ana factura', 'newer_than:2d'"),
            "max" to i("Número máximo de resultados (por defecto 10)")), listOf("consulta")))
        add(fn("leer_correo", "Lee el texto completo de un correo.", mapOf("id" to s("Id del correo")), listOf("id")))
        add(fn("crear_borrador", "Guarda un borrador en Gmail sin enviarlo.", mapOf(
            "para" to s("Correo del destinatario"), "asunto" to s("Asunto"), "cuerpo" to s("Texto del correo")), listOf("asunto", "cuerpo")))
        add(fn("enviar_correo", "Envía un correo desde la cuenta de Gmail del usuario (se le pedirá confirmación).", mapOf(
            "para" to s("Correo del destinatario"), "asunto" to s("Asunto"), "cuerpo" to s("Texto del correo"),
            "responder_a_id" to s("Id del correo al que se responde (opcional)")), listOf("para", "asunto", "cuerpo")))
        add(fn("buscar_contactos", "Busca en los contactos de Google por nombre, correo o teléfono.", mapOf("texto" to s("Texto a buscar")), listOf("texto")))
        add(fn("crear_contacto", "Añade un contacto a Google Contactos.", mapOf(
            "nombre" to s("Nombre completo"), "email" to s("Correo electrónico"), "telefono" to s("Teléfono")), listOf("nombre")))
        add(fn("crear_nota", "Guarda una nota.", mapOf(
            "titulo" to s("Título"), "contenido" to s("Contenido"), "etiquetas" to list("Etiquetas")), listOf("titulo")))
        add(fn("buscar_notas", "Busca en las notas guardadas. Con texto vacío devuelve las más recientes.", mapOf("texto" to s("Texto a buscar"))))
        add(fn("crear_tarea", "Crea una tarea o recordatorio. Si tiene fecha, el móvil enviará una notificación (a las 9:00 si no se indica hora).", mapOf(
            "titulo" to s("Qué hay que hacer"), "fecha_limite" to s("Fecha límite AAAA-MM-DD (opcional)"),
            "recordatorio" to s("Momento exacto del aviso, AAAA-MM-DDTHH:MM (opcional). Úsalo cuando pidan una hora concreta: «recuérdame a las 18:00»"),
            "prioridad" to s("alta, media o baja")), listOf("titulo")))
        add(fn("listar_tareas", "Lista las tareas pendientes y hechas."))
        add(fn("completar_tarea", "Marca una tarea como hecha.", mapOf("id" to s("Id de la tarea")), listOf("id")))
        add(fn("guardar_plantilla", "Guarda una plantilla de texto reutilizable.", mapOf("nombre" to s("Nombre"), "contenido" to s("Texto")), listOf("nombre", "contenido")))
        add(fn("listar_plantillas", "Lista las plantillas de texto guardadas."))

        // --- editar y borrar
        add(fn("leer_nota", "Devuelve el texto completo de una nota.", mapOf("id" to s("Id de la nota")), listOf("id")))
        add(fn("modificar_nota", "Cambia una nota. Indica solo lo que cambia. Con 'anadir_al_contenido' se añade texto al final sin borrar lo existente.", mapOf(
            "id" to s("Id de la nota"), "titulo" to s("Nuevo título"), "contenido" to s("Nuevo contenido completo (sustituye al anterior)"),
            "anadir_al_contenido" to s("Texto que se añade al final"), "etiquetas" to list("Nuevas etiquetas (sustituyen a las anteriores)"),
            "fijada" to b("Fijar o desfijar")), listOf("id")))
        add(fn("eliminar_nota", "Elimina una nota del móvil (se pedirá confirmación).", mapOf("id" to s("Id de la nota"), "titulo" to s("Título, para confirmar")), listOf("id")))
        add(fn("modificar_tarea", "Cambia una tarea: título, fecha límite, prioridad o si está hecha.", mapOf(
            "id" to s("Id de la tarea"), "titulo" to s("Nuevo título"), "fecha_limite" to s("AAAA-MM-DD, o vacío para quitarla"),
            "recordatorio" to s("Momento exacto del aviso AAAA-MM-DDTHH:MM, o vacío para quitarlo"),
            "prioridad" to s("alta, media o baja"), "hecha" to b("true si está hecha, false si vuelve a estar pendiente")), listOf("id")))
        add(fn("eliminar_tarea", "Elimina una tarea.", mapOf("id" to s("Id de la tarea")), listOf("id")))
        add(fn("eliminar_plantilla", "Elimina una plantilla por su nombre.", mapOf("nombre" to s("Nombre de la plantilla")), listOf("nombre")))
        add(fn("eliminar_contacto", "Elimina un contacto de Google (se pedirá confirmación). El id sale de buscar_contactos.", mapOf(
            "id" to s("Id del contacto"), "nombre" to s("Nombre, para confirmar")), listOf("id")))
        add(fn("papelera_correo", "Mueve un correo a la papelera de Gmail (se pedirá confirmación).", mapOf("id" to s("Id del correo"), "asunto" to s("Asunto, para confirmar")), listOf("id")))

        // --- Drive
        add(fn("guardar_nota_en_drive", "Sube una nota (y sus adjuntos) a Google Drive: a la carpeta Alfred o, si se indica, a un proyecto compartido.", mapOf("id" to s("Id de la nota"), "proyecto" to s("Nombre del proyecto (opcional)")), listOf("id")))
        add(fn("buscar_en_drive", "Busca archivos y carpetas por nombre en todo el Drive del usuario.", mapOf("texto" to s("Texto a buscar")), listOf("texto")))
        add(fn("crear_proyecto", "Crea un proyecto: una carpeta en Drive con un documento de notas, y la comparte con las personas indicadas para que trabajen juntos (pedirá confirmación si hay invitados).", mapOf(
            "nombre" to s("Nombre del proyecto"), "invitados" to list("Correos de las personas con las que compartirlo (pueden editar)")), listOf("nombre")))
        add(fn("compartir_en_drive", "Comparte un archivo o carpeta de Drive con una persona (pedirá confirmación). El id sale de buscar_en_drive o listar_drive.", mapOf(
            "id" to s("Id del archivo o carpeta"), "nombre" to s("Nombre, para confirmar"), "correo" to s("Correo de la persona"), "rol" to s("editor o lector (por defecto editor)")), listOf("id", "correo")))
        add(fn("crear_documento_drive", "Crea un documento de Google con el texto indicado, en un proyecto o en la carpeta Alfred de Drive.", mapOf(
            "nombre" to s("Nombre del documento"), "contenido" to s("Texto del documento"), "proyecto" to s("Nombre del proyecto (opcional)")), listOf("nombre", "contenido")))
        add(fn("listar_drive", "Lista los archivos de la carpeta Alfred de Drive o de una subcarpeta.", mapOf("carpeta" to s("Notas, Escaneos o Facturas. Vacío para la carpeta principal"))))
        add(fn("copia_seguridad_drive", "Guarda en Drive una copia de seguridad de notas, tareas y plantillas."))

        // --- interfaz
        add(fn("buscar_en_internet", "Busca información actual en internet: noticias, precios, resultados, datos recientes o cualquier cosa que no sepas o que pueda haber cambiado. Devuelve un resumen y las fuentes.", mapOf("consulta" to s("Qué buscar")), listOf("consulta")))
        add(fn("calcular", "Calculadora exacta. Úsala SIEMPRE para cuentas, porcentajes, conversiones o raíces en lugar de calcular de cabeza. Admite + - * / ^ ( ) %, decimales y sqrt()/raiz().", mapOf("expresion" to s("Operación, p. ej. 5+7*25-48*raiz(3)")), listOf("expresion")))
        add(fn("consulta_avanzada", "Solo para preguntas que de verdad exigen razonar o investigar a fondo: comparar opciones con datos actuales, planificar, analizar, o buscar algo cercano a la ubicación del usuario (por ejemplo la tienda más cercana). Usa un modelo más potente y busca en internet. Para búsquedas simples usa buscar_en_internet; para cuentas usa calcular.", mapOf("pregunta" to s("La pregunta completa, con todo el contexto necesario")), listOf("pregunta")))
        add(fn("abrir_pantalla", "Cambia la app a una pantalla. Úsala cuando el usuario pida ver o abrir una sección.", mapOf(
            "pantalla" to s("correo, calendario, notas, tareas, plantillas, contactos, drive, buscar, ajustes o chat")), listOf("pantalla")))
        add(fn("abrir_nota", "Abre una nota en el editor.", mapOf("id" to s("Id de la nota")), listOf("id")))
        add(fn("abrir_redactar_correo", "Abre la pantalla de redactar con el correo ya escrito para que el usuario lo revise y lo envíe él.", mapOf(
            "para" to s("Correo del destinatario"), "asunto" to s("Asunto"), "cuerpo" to s("Texto")), listOf("asunto", "cuerpo")))
        add(fn("abrir_calendario", "Abre la agenda en el mes de una fecha.", mapOf("fecha" to s("AAAA-MM-DD")), listOf("fecha")))
        add(fn("abrir_escaner", "Abre el escáner de documentos (tickets, facturas, papeles)."))
        add(fn("grabar_nota_voz", "Abre la grabadora: graba y transcribe una nota de voz."))
    }

    // ---------------------------------------------------------------- Ejecución
    private fun JsonObject.strings(k: String) = (this[k] as? JsonArray)?.map { it.jsonPrimitive.content }
    private fun JsonObject.bool(k: String) = str(k) == "true"
    private fun when_(s: String?) = s?.takeIf { it.isNotBlank() }?.let { Util.parseDateTime(it) }
    private fun ok(msg: String, extra: JsonObject? = null): JsonElement = buildJsonObject { put("ok", true); put("mensaje", msg); extra?.let { put("datos", it) } }

    private suspend fun run(name: String, a: JsonObject, actions: MutableList<String>, nav: MutableList<Nav>, confirm: suspend (String, String) -> Boolean): JsonElement = when (name) {
        "crear_evento" -> {
            val start = when_(a.str("inicio")) ?: throw Exception("Falta la fecha de inicio")
            val allDay = a.bool("todo_el_dia") || (a.str("inicio")?.trim()?.length ?: 0) <= 10
            val e = google.createEvent(a.str("titulo") ?: "Evento", start, when_(a.str("fin")), allDay, a.str("descripcion"), a.str("lugar"), a.strings("invitados"))
            actions += "📅 Evento creado: ${e.title} · ${describe(e.start, e.allDay)}"
            ok("Evento creado", buildJsonObject { put("id", e.id); put("inicio", e.start.format(fmt)); put("fin", e.end.format(fmt)) })
        }
        "listar_eventos" -> {
            val zone = ZoneId.systemDefault()
            val from = LocalDate.parse(a.str("desde")!!.take(10)).atStartOfDay(zone)
            val to = LocalDate.parse((a.str("hasta") ?: a.str("desde"))!!.take(10)).plusDays(1).atStartOfDay(zone)
            val evs = google.listEvents(from, to, a.str("texto"))
            buildJsonObject {
                put("total", evs.size)
                putJsonArray("eventos") {
                    evs.take(60).forEach { e ->
                        add(buildJsonObject {
                            put("id", e.id); put("titulo", e.title); put("todo_el_dia", e.allDay)
                            put("inicio", if (e.allDay) e.start.toLocalDate().toString() else e.start.format(fmt))
                            put("fin", if (e.allDay) e.end.toLocalDate().toString() else e.end.format(fmt))
                            if (e.location.isNotBlank()) put("lugar", e.location)
                            if (e.description.isNotBlank()) put("descripcion", e.description.take(300))
                            if (e.attendees.isNotEmpty()) put("invitados", e.attendees.joinToString(", "))
                        })
                    }
                }
            }
        }
        "modificar_evento" -> {
            val start = when_(a.str("inicio"))
            val allDay = a.bool("todo_el_dia") || (start != null && a.str("inicio")!!.trim().length <= 10)
            val e = google.updateEvent(a.str("id")!!, a.str("titulo"), start, when_(a.str("fin")), allDay, a.str("descripcion"), a.str("lugar"), a.strings("invitados"))
            actions += "📅 Evento actualizado: ${e.title} · ${describe(e.start, e.allDay)}"
            ok("Evento actualizado")
        }
        "eliminar_evento" -> {
            if (!confirm("¿Eliminar este evento?", a.str("titulo") ?: "Evento del calendario")) throw Exception("El usuario ha cancelado la eliminación")
            google.deleteEvent(a.str("id")!!)
            actions += "🗑️ Evento eliminado: ${a.str("titulo") ?: ""}"
            ok("Evento eliminado")
        }
        "buscar_correos" -> {
            val mails = google.listMail(a.str("consulta").orEmpty().ifBlank { "in:inbox" }, a.str("max")?.toDoubleOrNull()?.toInt() ?: 10)
            buildJsonObject {
                put("total", mails.size)
                putJsonArray("correos") {
                    mails.forEach { m ->
                        add(buildJsonObject {
                            put("id", m.id); put("de", m.from); put("asunto", m.subject); put("extracto", m.snippet)
                            put("fecha", java.time.Instant.ofEpochMilli(m.date).atZone(ZoneId.systemDefault()).format(fmt)); put("sin_leer", m.unread)
                        })
                    }
                }
            }
        }
        "leer_correo" -> {
            val m = google.readMail(a.str("id")!!)
            buildJsonObject {
                put("de", m.from); put("para", m.to); put("asunto", m.subject); put("texto", m.body.take(8000))
                if (m.attachments.isNotEmpty()) put("adjuntos", m.attachments.joinToString(", ") { it.name })
            }
        }
        "crear_borrador" -> {
            google.createDraft(a.str("para").orEmpty(), a.str("asunto").orEmpty(), a.str("cuerpo").orEmpty())
            actions += "✉️ Borrador guardado en Gmail: ${a.str("asunto")}"
            ok("Borrador guardado en Gmail")
        }
        "enviar_correo" -> {
            val to = a.str("para").orEmpty()
            if ("@" !in to) throw Exception("Falta una dirección de correo válida para el destinatario")
            val subject = a.str("asunto").orEmpty()
            val body = a.str("cuerpo").orEmpty()
            if (!confirm("¿Enviar este correo?", "Para: $to\nAsunto: $subject\n\n$body")) throw Exception("El usuario ha cancelado el envío")
            val orig = a.str("responder_a_id")?.takeIf { it.isNotBlank() }?.let { runCatching { google.readMail(it) }.getOrNull() }
            google.sendMail(to, subject, body, orig?.threadId, orig?.messageId)
            actions += "✉️ Correo enviado a $to: $subject"
            ok("Correo enviado")
        }
        "buscar_contactos" -> {
            val q = a.str("texto").orEmpty().lowercase()
            val found = google.listContacts().filter { c ->
                q.split(" ").filter { it.isNotBlank() }.all { w -> w in c.name.lowercase() || c.emails.any { w in it.lowercase() } || c.phones.any { w in it } }
            }
            buildJsonObject {
                put("total", found.size)
                putJsonArray("contactos") {
                    found.take(20).forEach { c ->
                        add(buildJsonObject { put("nombre", c.name); put("emails", c.emails.joinToString(", ")); put("telefonos", c.phones.joinToString(", ")) })
                    }
                }
            }
        }
        "crear_contacto" -> {
            google.createContact(a.str("nombre")!!, a.str("email"), a.str("telefono"))
            actions += "👤 Contacto añadido: ${a.str("nombre")}"
            ok("Contacto creado")
        }
        "crear_nota" -> {
            val n = store.upsertNote(Note(title = a.str("titulo").orEmpty(), content = a.str("contenido").orEmpty(), tags = a.strings("etiquetas") ?: emptyList()))
            actions += "📝 Nota guardada: ${n.title}"
            ok("Nota guardada", buildJsonObject { put("id", n.id) })
        }
        "buscar_notas" -> {
            val q = a.str("texto").orEmpty()
            val notes = if (q.isBlank()) store.notes.value.sortedByDescending { it.updatedAt } else store.searchNotes(q)
            buildJsonObject {
                put("total", notes.size)
                putJsonArray("notas") {
                    notes.take(15).forEach { n ->
                        add(buildJsonObject {
                            put("id", n.id); put("titulo", n.title); put("contenido", n.content.take(1500))
                            put("etiquetas", n.tags.joinToString(", ")); put("adjuntos", n.attachments.size)
                        })
                    }
                }
            }
        }
        "crear_tarea" -> {
            val prio = a.str("prioridad")?.lowercase()?.takeIf { it in listOf("alta", "media", "baja") } ?: "media"
            val due = a.str("fecha_limite")?.take(10)?.takeIf { runCatching { LocalDate.parse(it) }.isSuccess }
            val remind = a.str("recordatorio")?.takeIf { it.isNotBlank() }?.let { Util.parseDateTime(it) }
            val t = Task(title = a.str("titulo")!!, due = due ?: remind?.toLocalDate()?.toString(), priority = prio, remindAt = remind?.toInstant()?.toEpochMilli())
            store.upsertTask(t)
            actions += "✅ Tarea creada: ${t.title}" + (remind?.let { " · aviso ${it.format(DateTimeFormatter.ofPattern("d MMM HH:mm", Util.ES))}" } ?: "")
            ok("Tarea creada", buildJsonObject { put("id", t.id) })
        }
        "listar_tareas" -> buildJsonObject {
            putJsonArray("tareas") {
                store.tasks.value.forEach { t ->
                    add(buildJsonObject { put("id", t.id); put("titulo", t.title); put("hecha", t.done); put("prioridad", t.priority); t.due?.let { put("fecha_limite", it) }; t.remindAt?.let { put("aviso", java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(fmt)) } })
                }
            }
        }
        "completar_tarea" -> {
            val t = store.tasks.value.firstOrNull { it.id == a.str("id") } ?: throw Exception("No existe ninguna tarea con ese id")
            store.upsertTask(t.copy(done = true))
            actions += "✅ Tarea completada: ${t.title}"
            ok("Tarea completada")
        }
        "guardar_plantilla" -> {
            store.upsertTemplate(Template(name = a.str("nombre")!!, content = a.str("contenido")!!))
            actions += "📄 Plantilla guardada: ${a.str("nombre")}"
            ok("Plantilla guardada")
        }
        "listar_plantillas" -> buildJsonObject {
            putJsonArray("plantillas") { store.templates.value.forEach { add(buildJsonObject { put("nombre", it.name); put("contenido", it.content) }) } }
        }
        "leer_nota" -> {
            val n = store.note(a.str("id").orEmpty()) ?: throw Exception("No existe ninguna nota con ese id")
            buildJsonObject { put("titulo", n.title); put("contenido", n.content); put("etiquetas", n.tags.joinToString(", ")); put("adjuntos", n.attachments.size) }
        }
        "modificar_nota" -> {
            val n = store.note(a.str("id").orEmpty()) ?: throw Exception("No existe ninguna nota con ese id")
            var content = a.str("contenido") ?: n.content
            a.str("anadir_al_contenido")?.takeIf { it.isNotBlank() }?.let { content = (content.trimEnd() + "\n" + it).trim() }
            val up = store.upsertNote(n.copy(
                title = a.str("titulo") ?: n.title, content = content, tags = a.strings("etiquetas") ?: n.tags,
                pinned = a.str("fijada")?.let { it == "true" } ?: n.pinned,
            ))
            actions += "📝 Nota actualizada: ${up.title}"
            ok("Nota actualizada")
        }
        "eliminar_nota" -> {
            val n = store.note(a.str("id").orEmpty()) ?: throw Exception("No existe ninguna nota con ese id")
            if (!confirm("¿Eliminar esta nota?", n.title.ifBlank { a.str("titulo") ?: "Nota sin título" })) throw Exception("El usuario ha cancelado la eliminación")
            store.deleteNote(n.id)
            actions += "🗑️ Nota eliminada: ${n.title}"
            ok("Nota eliminada")
        }
        "modificar_tarea" -> {
            val t = store.tasks.value.firstOrNull { it.id == a.str("id") } ?: throw Exception("No existe ninguna tarea con ese id")
            val due = if (a.containsKey("fecha_limite")) a.str("fecha_limite")?.take(10)?.takeIf { runCatching { LocalDate.parse(it) }.isSuccess } else t.due
            val remind = if (a.containsKey("recordatorio")) a.str("recordatorio")?.takeIf { it.isNotBlank() }?.let { Util.parseDateTime(it) } else null
            store.upsertTask(t.copy(
                title = a.str("titulo") ?: t.title, due = due ?: remind?.toLocalDate()?.toString(),
                remindAt = if (a.containsKey("recordatorio")) remind?.toInstant()?.toEpochMilli() else t.remindAt,
                priority = a.str("prioridad")?.lowercase()?.takeIf { it in listOf("alta", "media", "baja") } ?: t.priority,
                done = a.str("hecha")?.let { it == "true" } ?: t.done,
            ))
            actions += "✅ Tarea actualizada: ${a.str("titulo") ?: t.title}"
            ok("Tarea actualizada")
        }
        "eliminar_tarea" -> {
            val t = store.tasks.value.firstOrNull { it.id == a.str("id") } ?: throw Exception("No existe ninguna tarea con ese id")
            store.deleteTask(t.id)
            actions += "🗑️ Tarea eliminada: ${t.title}"
            ok("Tarea eliminada")
        }
        "eliminar_plantilla" -> {
            val t = store.templates.value.firstOrNull { it.name.equals(a.str("nombre"), true) } ?: throw Exception("No existe ninguna plantilla con ese nombre")
            store.deleteTemplate(t.id)
            actions += "🗑️ Plantilla eliminada: ${t.name}"
            ok("Plantilla eliminada")
        }
        "eliminar_contacto" -> {
            if (!confirm("¿Eliminar este contacto?", a.str("nombre") ?: "Contacto de Google")) throw Exception("El usuario ha cancelado la eliminación")
            google.deleteContact(a.str("id").orEmpty())
            actions += "🗑️ Contacto eliminado: ${a.str("nombre") ?: ""}"
            ok("Contacto eliminado")
        }
        "papelera_correo" -> {
            if (!confirm("¿Mover a la papelera?", a.str("asunto") ?: "Correo de Gmail")) throw Exception("El usuario ha cancelado la acción")
            google.trashMail(a.str("id").orEmpty())
            actions += "🗑️ Correo movido a la papelera"
            ok("Correo en la papelera")
        }
        "guardar_nota_en_drive" -> {
            val n = store.note(a.str("id").orEmpty()) ?: throw Exception("No existe ninguna nota con ese id")
            val proj = a.str("proyecto")?.takeIf { it.isNotBlank() }
            val parent = proj?.let { projectFolder(it) ?: throw Exception("No encuentro ningún proyecto llamado «$it»") }
            uploadNote(n, parent)
            actions += "☁️ Guardada en Drive: ${n.title}" + (proj?.let { " (proyecto $it)" } ?: "")
            ok("Nota guardada en Drive")
        }
        "buscar_en_drive" -> {
            val files = google.listDrive(parent = null, query = a.str("texto").orEmpty())
            buildJsonObject {
                put("total", files.size)
                putJsonArray("archivos") { files.take(12).forEach { f -> add(buildJsonObject { put("id", f.id); put("nombre", f.name); put("carpeta", f.isFolder); put("compartido", f.shared); f.link?.let { put("enlace", it) } }) } }
            }
        }
        "crear_proyecto" -> {
            val name = a.str("nombre").orEmpty().ifBlank { throw Exception("Falta el nombre del proyecto") }
            val invites = a.strings("invitados")?.filter { "@" in it } ?: emptyList()
            if (invites.isNotEmpty() && !confirm("¿Crear el proyecto y compartirlo?", "Proyecto «$name»\nSe invitará (con permiso de edición) a:\n" + invites.joinToString("\n"))) throw Exception("El usuario ha cancelado")
            val folder = google.createFolder(name, google.folder("Proyectos"))
            google.createDocWithText("Notas del proyecto", "$name\n\nObjetivo:\n\nTareas:\n- \n\nNotas:\n", folder.id)
            val failed = invites.filter { mail -> runCatching { google.share(folder.id, mail, "writer", "Te invito al proyecto «$name» en Drive.") }.isFailure }
            actions += "📁 Proyecto creado: $name" + if (invites.isNotEmpty()) " · compartido con ${invites.size - failed.size}" else ""
            ok("Proyecto creado" + if (failed.isNotEmpty()) ", pero no se pudo compartir con: ${failed.joinToString()}" else "", buildJsonObject { put("id", folder.id); folder.link?.let { put("enlace", it) } })
        }
        "compartir_en_drive" -> {
            val mail = a.str("correo").orEmpty()
            if ("@" !in mail) throw Exception("Falta un correo válido")
            val role = if (a.str("rol")?.lowercase()?.startsWith("l") == true) "reader" else "writer"
            if (!confirm("¿Compartir en Drive?", "«${a.str("nombre") ?: "Archivo"}» con $mail (${if (role == "writer") "puede editar" else "solo ver"})")) throw Exception("El usuario ha cancelado")
            google.share(a.str("id").orEmpty(), mail, role)
            actions += "🤝 Compartido con $mail: ${a.str("nombre") ?: ""}"
            ok("Compartido")
        }
        "crear_documento_drive" -> {
            val proj = a.str("proyecto")?.takeIf { it.isNotBlank() }
            val parent = proj?.let { projectFolder(it) ?: throw Exception("No encuentro ningún proyecto llamado «$it»") } ?: google.folder(null)
            val f = google.createDocWithText(a.str("nombre").orEmpty().ifBlank { "Documento" }, a.str("contenido").orEmpty(), parent)
            actions += "📄 Documento creado en Drive: ${f.name}"
            ok("Documento creado", buildJsonObject { put("id", f.id); f.link?.let { put("enlace", it) } })
        }
        "listar_drive" -> {
            val sub = a.str("carpeta")?.trim()?.takeIf { it.isNotBlank() }
            val files = google.listFiles(google.folder(sub))
            buildJsonObject {
                put("total", files.size)
                putJsonArray("archivos") { files.take(40).forEach { f -> add(buildJsonObject { put("nombre", f.name); put("carpeta", f.isFolder); f.link?.let { put("enlace", it) } }) } }
            }
        }
        "copia_seguridad_drive" -> { backup(); actions += "☁️ Copia de seguridad guardada en Drive"; ok("Copia de seguridad guardada") }

        "calcular" -> {
            val e = a.str("expresion").orEmpty().ifBlank { throw Exception("Falta la operación") }
            try { val v = Calc.eval(e); buildJsonObject { put("resultado", Calc.format(v)) } } catch (x: Calc.CalcError) { throw Exception("No he podido calcular «$e»: ${x.message}") }
        }
        "consulta_avanzada" -> {
            val q = a.str("pregunta").orEmpty().ifBlank { throw Exception("Falta la pregunta") }
            advancedUsed = true
            val (text, sources) = gemini.advanced(q, settings.weatherCity.value)
            actions += "🧠 Consulta avanzada"
            buildJsonObject { put("respuesta", text); put("fuentes", sources.joinToString("; ")) }
        }
        "buscar_en_internet" -> {
            var q = a.str("consulta").orEmpty().ifBlank { throw Exception("Falta qué buscar") }
            val place = settings.weatherCity.value
            if (place.isNotBlank() && Regex("""(?i)cerca|cercan|por aqu|mi zona|alrededor""").containsMatchIn(q)) q += " (zona: $place)"
            val (text, sources) = gemini.searchWeb(q)
            actions += "🔎 Buscado en internet: $q"
            buildJsonObject { put("respuesta", text); put("fuentes", sources.joinToString("; ")) }
        }
        "abrir_pantalla" -> {
            val p = a.str("pantalla").orEmpty().lowercase().trim()
            val name = mapOf("correo" to "correo", "gmail" to "correo", "email" to "correo", "calendario" to "calendario", "agenda" to "calendario",
                "notas" to "notas", "tareas" to "tareas", "plantillas" to "plantillas", "contactos" to "contactos", "drive" to "drive",
                "ajustes" to "ajustes", "configuracion" to "ajustes", "configuración" to "ajustes", "chat" to "chat")[p] ?: throw Exception("Pantalla desconocida: $p")
            nav += Nav.Screen(name); ok("Abriendo $name")
        }
        "abrir_nota" -> {
            if (store.note(a.str("id").orEmpty()) == null) throw Exception("No existe ninguna nota con ese id")
            nav += Nav.Note(a.str("id")!!); ok("Abriendo la nota")
        }
        "abrir_redactar_correo" -> { nav += Nav.Compose(a.str("para").orEmpty(), a.str("asunto").orEmpty(), a.str("cuerpo").orEmpty()); ok("Correo preparado en la pantalla de redactar") }
        "abrir_calendario" -> { nav += Nav.Calendar(LocalDate.parse((a.str("fecha") ?: throw Exception("Falta la fecha")).take(10))); ok("Abriendo la agenda") }
        "abrir_escaner" -> { nav += Nav.Scan; ok("Abriendo el escáner") }
        "grabar_nota_voz" -> { nav += Nav.Record; ok("Abriendo la grabadora") }
        else -> throw Exception("Herramienta desconocida: $name")
    }

    /** Id de la carpeta de un proyecto por su nombre (o null si no existe). */
    private suspend fun projectFolder(name: String): String? =
        google.listDrive(parent = google.folder("Proyectos")).firstOrNull { it.isFolder && it.name.equals(name.trim(), true) }?.id

    private fun describe(z: ZonedDateTime, allDay: Boolean) =
        z.format(DateTimeFormatter.ofPattern(if (allDay) "EEE d MMM" else "EEE d MMM, HH:mm", Util.ES))

    /**
     * Procesa un mensaje del usuario. [history] son los mensajes anteriores (solo texto);
     * [image] es una imagen opcional adjunta al mensaje.
     */
    suspend fun ask(
        history: List<Pair<Boolean, String>>, text: String, image: Pair<ByteArray, String>? = null, voice: Boolean = false,
        confirm: suspend (title: String, detail: String) -> Boolean,
    ): Reply {
        val contents = mutableListOf<JsonObject>()
        // El historial debe empezar por el usuario y alternar turnos.
        history.takeLast(30).dropWhile { !it.first }.forEach { (fromUser, t) ->
            if (t.isNotBlank()) contents += if (fromUser) Gemini.userContent(t) else Gemini.modelContent(t)
        }
        contents += Gemini.userContent(text.ifBlank { "Mira esta imagen." }, image?.first, image?.second)

        val actions = mutableListOf<String>()
        val nav = mutableListOf<Nav>()
        val sys = system(voice)
        repeat(8) {
            val content = gemini.generate(contents, sys, tools)
            val calls = Gemini.functionCalls(content)
            if (calls.isEmpty()) return Reply(Gemini.textOf(content).ifBlank { "Hecho." }, actions, nav.takeLast(1))
            contents += JsonObject(content + ("role" to JsonPrimitive("model")))
            val responses = calls.map { c ->
                val result = try {
                    run(c.str("name").orEmpty(), c.obj("args") ?: JsonObject(emptyMap()), actions, nav, confirm)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    buildJsonObject { put("ok", false); put("error", e.message ?: e.toString()) }
                }
                Gemini.functionResponse(c, result)
            }
            contents += buildJsonObject { put("role", "user"); put("parts", JsonArray(responses)) }
        }
        return Reply("He hecho varias acciones pero no he podido terminar la petición. Revisa el resultado, por favor.", actions, nav.takeLast(1))
    }

    /** Extrae los datos de un ticket o factura escaneado. Devuelve un texto listo para la nota. */
    suspend fun analyzeDocument(bytes: ByteArray, mime: String): DocInfo {
        val raw = gemini.withFile(
            """Analiza este documento escaneado (ticket, factura, recibo u otro). Devuelve SOLO un JSON con estas claves:
"tipo" (ticket, factura, recibo, contrato, carta, otro), "titulo" (título breve y descriptivo en español, máximo 60 caracteres),
"comercio" (emisor o comercio, o ""), "fecha" (AAAA-MM-DD o ""), "total" (importe total con moneda, o ""),
"resumen" (2-4 líneas con lo importante: conceptos, impuestos, forma de pago, número de factura...), "texto" (transcripción del texto legible).
No inventes datos que no aparezcan.""", bytes, mime, jsonOut = true,
        )
        val j = runCatching { com.iatualcance.alfred.google.Http.json.parseToJsonElement(raw.removePrefix("```json").removeSuffix("```").trim()) as JsonObject }
            .getOrElse { return DocInfo("otro", "Documento escaneado", raw) }
        val body = buildString {
            j.str("comercio")?.takeIf { it.isNotBlank() }?.let { append("Emisor: $it\n") }
            j.str("fecha")?.takeIf { it.isNotBlank() }?.let { append("Fecha: $it\n") }
            j.str("total")?.takeIf { it.isNotBlank() }?.let { append("Total: $it\n") }
            j.str("resumen")?.takeIf { it.isNotBlank() }?.let { append("\n$it\n") }
            j.str("texto")?.takeIf { it.isNotBlank() }?.let { append("\n— Texto —\n$it") }
        }.trim()
        return DocInfo(j.str("tipo")?.lowercase() ?: "otro", j.str("titulo")?.ifBlank { null } ?: "Documento escaneado", body)
    }
}

data class DocInfo(val type: String, val title: String, val body: String) {
    val isInvoice get() = type in listOf("ticket", "factura", "recibo")
}
