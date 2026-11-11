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

data class Reply(val text: String, val actions: List<String>)

/**
 * El "cerebro" de Alfred: le pasa a Gemini las herramientas disponibles, ejecuta las que pida
 * (calendario, correo, contactos, notas, tareas, plantillas) y repite hasta tener la respuesta final.
 * [confirm] se llama antes de acciones delicadas (enviar un correo, borrar un evento).
 */
class Assistant(
    private val gemini: Gemini, private val google: GoogleApi, private val store: Store, private val settings: Settings,
) {
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

    private fun system(): String {
        val now = ZonedDateTime.now()
        val acc = settings.account.value
        return """
Eres Alfred, un asistente personal al estilo de un mayordomo moderno: educado, breve y eficaz. Respondes siempre en español salvo que el usuario escriba en otro idioma.
Fecha y hora actuales: ${now.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy, HH:mm", Util.ES))} (zona ${now.zone.id}). Hoy es ${now.toLocalDate()}.
Usuario: ${acc?.name ?: "sin cuenta de Google conectada"}${acc?.let { " <${it.email}>" } ?: ""}.

Normas:
- Usa las herramientas para actuar de verdad; nunca digas que has hecho algo si no has llamado a la herramienta y ha respondido bien. Si una herramienta devuelve un error, explícalo con claridad.
- Puedes encadenar varias herramientas en una misma petición (por ejemplo, buscar un contacto y después enviarle un correo o invitarle a un evento).
- Las fechas van en hora local con formato AAAA-MM-DDTHH:MM. Calcula tú las fechas relativas ("mañana", "el viernes") a partir de la fecha actual. Si no dicen hora para una cita, pregunta o usa una razonable y dilo.
- Antes de modificar o eliminar un evento, o de responder a un correo, búscalo primero para obtener su id.
- Para enviar un correo necesitas una dirección: si solo te dan un nombre, búscalo en contactos. Si el usuario pide "redactar" o "preparar", crea un borrador; envía solo si pide enviar.
- Las tareas y recordatorios sencillos van como tarea; la información para guardar va como nota.
- No inventes datos (direcciones, teléfonos, importes). Si falta algo imprescindible, pregunta.
- Respuestas cortas, sin markdown recargado.
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
        add(fn("crear_tarea", "Crea una tarea o recordatorio.", mapOf(
            "titulo" to s("Qué hay que hacer"), "fecha_limite" to s("Fecha límite AAAA-MM-DD (opcional)"),
            "prioridad" to s("alta, media o baja")), listOf("titulo")))
        add(fn("listar_tareas", "Lista las tareas pendientes y hechas."))
        add(fn("completar_tarea", "Marca una tarea como hecha.", mapOf("id" to s("Id de la tarea")), listOf("id")))
        add(fn("guardar_plantilla", "Guarda una plantilla de texto reutilizable.", mapOf("nombre" to s("Nombre"), "contenido" to s("Texto")), listOf("nombre", "contenido")))
        add(fn("listar_plantillas", "Lista las plantillas de texto guardadas."))
    }

    // ---------------------------------------------------------------- Ejecución
    private fun JsonObject.strings(k: String) = (this[k] as? JsonArray)?.map { it.jsonPrimitive.content }
    private fun JsonObject.bool(k: String) = str(k) == "true"
    private fun when_(s: String?) = s?.takeIf { it.isNotBlank() }?.let { Util.parseDateTime(it) }
    private fun ok(msg: String, extra: JsonObject? = null): JsonElement = buildJsonObject { put("ok", true); put("mensaje", msg); extra?.let { put("datos", it) } }

    private suspend fun run(name: String, a: JsonObject, actions: MutableList<String>, confirm: suspend (String, String) -> Boolean): JsonElement = when (name) {
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
            buildJsonObject { put("de", m.from); put("para", m.to); put("asunto", m.subject); put("texto", m.body.take(8000)) }
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
            val t = Task(title = a.str("titulo")!!, due = due, priority = prio)
            store.upsertTask(t)
            actions += "✅ Tarea creada: ${t.title}"
            ok("Tarea creada", buildJsonObject { put("id", t.id) })
        }
        "listar_tareas" -> buildJsonObject {
            putJsonArray("tareas") {
                store.tasks.value.forEach { t ->
                    add(buildJsonObject { put("id", t.id); put("titulo", t.title); put("hecha", t.done); put("prioridad", t.priority); t.due?.let { put("fecha_limite", it) } })
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
        else -> throw Exception("Herramienta desconocida: $name")
    }

    private fun describe(z: ZonedDateTime, allDay: Boolean) =
        z.format(DateTimeFormatter.ofPattern(if (allDay) "EEE d MMM" else "EEE d MMM, HH:mm", Util.ES))

    /**
     * Procesa un mensaje del usuario. [history] son los mensajes anteriores (solo texto);
     * [image] es una imagen opcional adjunta al mensaje.
     */
    suspend fun ask(
        history: List<Pair<Boolean, String>>, text: String, image: Pair<ByteArray, String>? = null,
        confirm: suspend (title: String, detail: String) -> Boolean,
    ): Reply {
        val contents = mutableListOf<JsonObject>()
        // El historial debe empezar por el usuario y alternar turnos.
        history.takeLast(16).dropWhile { !it.first }.forEach { (fromUser, t) ->
            if (t.isNotBlank()) contents += if (fromUser) Gemini.userContent(t) else Gemini.modelContent(t)
        }
        contents += Gemini.userContent(text.ifBlank { "Mira esta imagen." }, image?.first, image?.second)

        val actions = mutableListOf<String>()
        val sys = system()
        repeat(8) {
            val content = gemini.generate(contents, sys, tools)
            val calls = Gemini.functionCalls(content)
            if (calls.isEmpty()) return Reply(Gemini.textOf(content).ifBlank { "Hecho." }, actions)
            contents += JsonObject(content + ("role" to JsonPrimitive("model")))
            val responses = calls.map { c ->
                val result = try {
                    run(c.str("name").orEmpty(), c.obj("args") ?: JsonObject(emptyMap()), actions, confirm)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    buildJsonObject { put("ok", false); put("error", e.message ?: e.toString()) }
                }
                Gemini.functionResponse(c, result)
            }
            contents += buildJsonObject { put("role", "user"); put("parts", JsonArray(responses)) }
        }
        return Reply("He hecho varias acciones pero no he podido terminar la petición. Revisa el resultado, por favor.", actions)
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
