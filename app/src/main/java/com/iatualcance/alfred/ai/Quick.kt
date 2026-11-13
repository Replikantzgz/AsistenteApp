package com.iatualcance.alfred.ai

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Atajos y comandos rápidos: frases directas que se entienden sin pasar por la IA (más rápido, no gasta cuota y funciona
 * igual por voz que escribiendo). Si la frase no encaja con ninguno, el asistente la trata como siempre.
 * Es Kotlin puro, sin Android, para poder probarlo.
 */
object Quick {
    sealed interface Cmd
    data class NewTask(val title: String, val due: LocalDate?, val time: LocalTime?) : Cmd
    data class NewNote(val title: String, val content: String) : Cmd
    data class NewEvent(val title: String, val date: LocalDate, val start: LocalTime?, val durationMin: Int) : Cmd
    data class Done(val isNote: Boolean, val query: String) : Cmd
    data class Mail(val to: String, val subject: String, val body: String) : Cmd
    data class Open(val screen: String) : Cmd
    data object Scan : Cmd
    data object Record : Cmd

    /** Quita tildes y pasa a minúsculas conservando la longitud (así los índices valen también para el texto original). */
    fun fold(s: String): String = buildString(s.length) {
        for (c in s) append(when (c) {
            'á', 'Á', 'à', 'ä' -> 'a'; 'é', 'É', 'è', 'ë' -> 'e'; 'í', 'Í', 'ì', 'ï' -> 'i'; 'ó', 'Ó', 'ò', 'ö' -> 'o'; 'ú', 'Ú', 'ù', 'ü' -> 'u'; 'ñ', 'Ñ' -> 'n'
            else -> c.lowercaseChar()
        })
    }

    private const val CREATE = "(?:crea|crear|creame|anade|anadir|anademe|agrega|agregar|agregame|apunta|apuntar|apuntame|pon|poner|ponme|programa|programar|nueva|nuevo|guarda|guardar|anota|anotar|anotame|mete|metele)"
    private val DAYS = mapOf("lunes" to DayOfWeek.MONDAY, "martes" to DayOfWeek.TUESDAY, "miercoles" to DayOfWeek.WEDNESDAY, "jueves" to DayOfWeek.THURSDAY, "viernes" to DayOfWeek.FRIDAY, "sabado" to DayOfWeek.SATURDAY, "domingo" to DayOfWeek.SUNDAY)
    private val MONTHS = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
    private val DONE_WORDS = "(?:hecha|hecho|completada|completado|terminada|terminado|lista|listo|resuelta|resuelto|finalizada|finalizado|cumplida)"

    // ------------------------------------------------------------------------------------------------ fecha y hora
    private class Slot { var date: LocalDate? = null; var time: LocalTime? = null; var durationMin: Int? = null }

    /** Busca fecha, hora y duración dentro de [orig], las quita del texto y devuelve lo que sobra. */
    private fun extractWhen(orig: String, today: LocalDate, keepLead: Boolean = false): Pair<String, Slot> {
        val chars = orig.toCharArray(); var f = fold(orig).toCharArray(); val slot = Slot()
        fun find(re: Regex): MatchResult? {
            val m = re.find(String(f)) ?: return null
            for (i in m.range) { chars[i] = ' '; f[i] = ' ' }
            return m
        }
        fun hour(h: Int, part: String?, mer: String?): Int {
            var x = h
            if ((part == "tarde" || part == "noche" || mer == "pm") && x < 12) x += 12
            if ((part == "manana" || mer == "am") && x == 12) x = 0
            return x % 24
        }
        fun lt(h: Int, m: Int) = runCatching { LocalTime.of(h, m) }.getOrNull()

        // duración y rangos
        find(Regex("""\bde (?:las )?(\d{1,2})(?:[:.h](\d{2}))? a (?:las )?(\d{1,2})(?:[:.h](\d{2}))?\b"""))?.let { m ->
            val g = m.groupValues; val s = lt(g[1].toInt(), g[2].ifBlank { "0" }.toInt()); val e = lt(g[3].toInt(), g[4].ifBlank { "0" }.toInt())
            if (s != null && e != null) { slot.time = s; val d = java.time.Duration.between(s, e).toMinutes().toInt(); slot.durationMin = if (d > 0) d else 60 }
        }
        find(Regex("""\bdurante (\d+|una|un|media) ?(hora|horas|minutos|min)\b"""))?.let { m ->
            val n = m.groupValues[1]; val unit = m.groupValues[2]
            slot.durationMin = when { n == "media" -> 30; unit.startsWith("min") -> n.toIntOrNull() ?: 60; else -> (if (n == "una" || n == "un") 1 else n.toIntOrNull() ?: 1) * 60 }
        }
        find(Regex("""\bmedia hora\b"""))?.let { slot.durationMin = 30 }

        // hora
        if (slot.time == null) {
            find(Regex("""\ba (?:las?|la) (\d{1,2})(?:[:.h ]?(\d{2}))?(?: y (media|cuarto)| menos (cuarto))?(?: de la (manana|tarde|noche)| ?(am|pm))?\b"""))?.let { m ->
                val g = m.groupValues; var h = hour(g[1].toInt(), g[5].ifBlank { null }, g[6].ifBlank { null }); var min = g[2].ifBlank { "0" }.toInt()
                if (g[3] == "media") min = 30 else if (g[3] == "cuarto") min = 15
                if (g[4] == "cuarto") { h = (h + 23) % 24; min = 45 }
                slot.time = lt(h, min)
            }
        }
        if (slot.time == null) find(Regex("""\b(\d{1,2})[:.](\d{2})\s*h?\b"""))?.let { m -> slot.time = lt(m.groupValues[1].toInt(), m.groupValues[2].toInt()) }
        if (slot.time == null) find(Regex("""\ba mediodia\b"""))?.let { slot.time = LocalTime.NOON }
        if (slot.time == null) find(Regex("""\ba medianoche\b"""))?.let { slot.time = LocalTime.MIDNIGHT }

        // fecha
        find(Regex("""\bpasado manana\b"""))?.let { slot.date = today.plusDays(2) }
        if (slot.date == null) find(Regex("""\bhoy\b"""))?.let { slot.date = today }
        if (slot.date == null) find(Regex("""(?<!por la )(?<!de la )(?<!en la )\bmanana\b"""))?.let { slot.date = today.plusDays(1) }
        if (slot.date == null) find(Regex("""\b(?:dentro de|en) (\d+) (dias?|semanas?)\b"""))?.let { m ->
            val n = m.groupValues[1].toLong(); slot.date = today.plusDays(if (m.groupValues[2].startsWith("sem")) n * 7 else n)
        }
        if (slot.date == null) find(Regex("""\b(?:la semana que viene|la proxima semana)\b"""))?.let { slot.date = today.plusDays(7) }
        if (slot.date == null) find(Regex("""\b(?:el |este |proximo |el proximo |el pasado )?(lunes|martes|miercoles|jueves|viernes|sabado|domingo)\b"""))?.let { m ->
            val target = DAYS.getValue(m.groupValues[1]); var d = (target.value - today.dayOfWeek.value + 7) % 7; if (d == 0) d = 7
            slot.date = today.plusDays(d.toLong())
        }
        if (slot.date == null) find(Regex("""\b(?:el )?(\d{1,2}) de (${MONTHS.joinToString("|")})(?: de (\d{4}))?\b"""))?.let { m ->
            val day = m.groupValues[1].toInt(); val month = MONTHS.indexOf(m.groupValues[2]) + 1
            var year = m.groupValues[3].ifBlank { null }?.toInt() ?: today.year
            var d = runCatching { LocalDate.of(year, month, day) }.getOrNull()
            if (d != null && m.groupValues[3].isBlank() && d.isBefore(today)) d = runCatching { LocalDate.of(year + 1, month, day) }.getOrNull()
            slot.date = d
        }
        if (slot.date == null) find(Regex("""\b(\d{1,2})/(\d{1,2})(?:/(\d{2,4}))?\b"""))?.let { m ->
            val day = m.groupValues[1].toInt(); val month = m.groupValues[2].toInt()
            var year = m.groupValues[3].ifBlank { null }?.toInt()?.let { if (it < 100) 2000 + it else it } ?: today.year
            var d = runCatching { LocalDate.of(year, month, day) }.getOrNull()
            if (d != null && m.groupValues[3].isBlank() && d.isBefore(today)) d = runCatching { LocalDate.of(year + 1, month, day) }.getOrNull()
            slot.date = d
        }
        if (slot.date == null) find(Regex("""\bel (\d{1,2})\b(?![:/.\d])"""))?.let { m ->
            val day = m.groupValues[1].toInt()
            var d = runCatching { today.withDayOfMonth(day) }.getOrNull()
            if (d != null && d.isBefore(today)) d = runCatching { today.plusMonths(1).withDayOfMonth(day) }.getOrNull()
            slot.date = d
        }
        return clean(String(chars), keepLead) to slot
    }

    private val EDGE = Regex("""^(?:(?:para|el|la|a|de|del|que|por|en|y|con)\s+)+|(?:\s+(?:para|el|la|a|de|del|que|por|en|y|con|las|los))+$""", RegexOption.IGNORE_CASE)
    private val EDGE_TAIL = Regex("""(?:\s+(?:para|el|la|a|de|del|que|por|en|y|con|las|los))+$""", RegexOption.IGNORE_CASE)
    private val EDGE_LEAD_LIGHT = Regex("""^(?:(?:para|el|la|a|que|y)\s+)+""", RegexOption.IGNORE_CASE)
    private fun clean(s: String, keepLead: Boolean = false): String {
        var t = s.replace(Regex("""\s+"""), " ").trim().trim(',', '.', ';', ':', '-', ' ')
        repeat(2) { t = (if (keepLead) EDGE_TAIL.replace(EDGE_LEAD_LIGHT.replace(t, ""), "") else EDGE.replace(t, "")).trim().trim(',', '.', ';', ':', '-', ' ') }
        return t
    }

    private fun cap(s: String) = s.replaceFirstChar { it.uppercase() }

    // ------------------------------------------------------------------------------------------------ interpretación
    /** Devuelve el comando si [text] es un atajo reconocido, o null si debe resolverlo la IA. */
    fun parse(text: String, today: LocalDate = LocalDate.now()): Cmd? {
        val orig = text.trim().trimEnd('.', '!', '?')
        if (orig.isBlank() || orig.length > 600) return null
        val f = fold(orig)
        fun rest(m: MatchResult, group: Int): String = orig.substring(m.groups[group]!!.range.first.coerceAtMost(orig.length)).let { if (m.groups[group]!!.range.isEmpty()) "" else orig.substring(m.groups[group]!!.range) }

        // Abrir pantallas y herramientas
        Regex("""^(?:abre|abrir|ve a|ir a|muestra|muestrame|ensename|llevame a)\s+(?:el |la |los |las |mi |mis )?(correos?|bandeja de entrada|agenda|calendario|notas|tareas|drive|ajustes|configuracion|contactos|plantillas|buscador|busqueda)$""").find(f)?.let { m ->
            return Open(when (m.groupValues[1]) {
                "correo", "correos", "bandeja de entrada" -> "correo"; "agenda", "calendario" -> "calendario"; "ajustes", "configuracion" -> "ajustes"
                "buscador", "busqueda" -> "buscar"; else -> m.groupValues[1]
            })
        }
        if (Regex("""^(?:escanea|escanear|escaner|abre el escaner|escanea un documento|escanear documento)$""").matches(f)) return Scan
        if (Regex("""^(?:graba|grabar|empieza a grabar)\s+(?:una\s+)?nota de voz$""").matches(f)) return Record

        // Marcar como hecho
        Regex("""^(?:marca|marcar|pon|poner|da por|dar por|completa|completar|termina|terminar|finaliza|finalizar)\s+(?:la\s+|el\s+)?(tarea|nota)?\s*(.+?)(?:\s+como)?\s+${DONE_WORDS}$""").find(f)?.let { m ->
            val q = orig.substring(m.groups[2]!!.range).trim()
            if (q.isNotBlank()) return Done(m.groupValues[1] == "nota", q)
        }
        Regex("""^(?:completa|completar|termina|terminar|finaliza|finalizar)\s+(?:la\s+|el\s+)?(tarea|nota)\s+(.+)$""").find(f)?.let { m ->
            return Done(m.groupValues[1] == "nota", orig.substring(m.groups[2]!!.range).trim())
        }
        Regex("""^(?:ya (?:he )?(?:hecho|terminado|completado|acabado)|he (?:hecho|terminado|completado|acabado))\s+(?:la\s+|el\s+)?(tarea|nota)?\s*(.+)$""").find(f)?.let { m ->
            return Done(m.groupValues[1] == "nota", orig.substring(m.groups[2]!!.range).trim())
        }

        // Correo
        Regex("""^(?:envia|enviar|enviale|manda|mandar|mandale|escribe|escribir|escribele|redacta|redactar)\s+(?:un\s+|el\s+)?(?:correo|mail|email|e-mail)\s*(?:electronico\s*)?(?:a|para)\s+(.+)$""").find(f)?.let { m ->
            val r = orig.substring(m.groups[1]!!.range); val rf = f.substring(m.groups[1]!!.range)
            var body = ""; var subject = ""; var head = r
            Regex("""\s+(?:y\s+)?(?:diciendole que|diciendo que|diciendole|diciendo|que diga|que ponga|con el texto|con el mensaje|dile que|ponle que)\s+(.+)$""").find(rf)?.let { b ->
                body = r.substring(b.groups[1]!!.range).trim(); head = r.substring(0, b.range.first)
            }
            val hf = fold(head)
            Regex("""\s+(?:con\s+)?asunto\s+(.+)$""").find(hf)?.let { s -> subject = head.substring(s.groups[1]!!.range).trim(); head = head.substring(0, s.range.first) }
            val email = Regex("""[\w.+-]+@[\w-]+(?:\.[\w-]+)+""").find(head)?.value
            val to = (email ?: head.trim()).trim(',', '.')
            if (to.isNotBlank()) return Mail(to, cap(subject), body)
        }

        // Evento: "crea una cita…", "añade a la agenda…", "agéndame…"
        Regex("""^(?:$CREATE|programame|agendame|agendar|agenda)\s+(?:me\s+)?(?:una?\s+|la\s+|el\s+)?(cita|reunion|evento|quedada)\b(.*)$""").find(f)?.let { m ->
            val kind = when (m.groupValues[1]) { "reunion" -> "Reunión"; "quedada" -> "Quedada"; "evento" -> "Evento"; else -> "Cita" }
            return event(kind, orig.substring(m.groups[2]!!.range.first), today)
        }
        Regex("""^(?:anade|agrega|pon|apunta|mete|guarda|anademe|agregame|apuntame)\s+(?:en|a)\s+la\s+(?:agenda|calendario)\s*(?:que\s+)?(.*)$""").find(f)?.let { m ->
            return event(null, if (m.groups[1]!!.range.isEmpty()) "" else orig.substring(m.groups[1]!!.range.first), today)
        }
        Regex("""^(?:agendame|agendar)\s+(.+)$""").find(f)?.let { m -> return event(null, orig.substring(m.groups[1]!!.range.first), today) }

        // Tarea
        Regex("""^(?:$CREATE)\s+(?:me\s+)?(?:una?\s+|la\s+)?tarea\b\s*(?:que\s+(?:diga|ponga|sea)|de|para|:)?\s*(.*)$""").find(f)?.let { m ->
            val body = if (m.groups[1]!!.range.isEmpty()) "" else orig.substring(m.groups[1]!!.range.first)
            return task(body, today)
        }
        Regex("""^(?:recuerdame|recordame|recuerda)\s+(?:que\s+|de\s+)?(.+)$""").find(f)?.let { m -> return task(orig.substring(m.groups[1]!!.range.first), today) }

        // Nota
        Regex("""^(?:$CREATE)\s+(?:me\s+)?(?:una?\s+|la\s+)?nota\b\s*(?:que\s+(?:diga|ponga|sea)|de|sobre|:)?\s*(.*)$""").find(f)?.let { m ->
            val body = (if (m.groups[1]!!.range.isEmpty()) "" else orig.substring(m.groups[1]!!.range.first)).trim()
            if (body.isNotBlank()) return note(body)
        }
        Regex("""^(?:anota|anotame|apunta|apuntame)\s+(?:que\s+)?(.+)$""").find(f)?.let { m -> return note(orig.substring(m.groups[1]!!.range.first).trim()) }
        return null
    }

    private fun task(body: String, today: LocalDate): Cmd? {
        val (title, slot) = extractWhen(body, today)
        if (title.isBlank()) return null
        val due = slot.date ?: if (slot.time != null) today else null
        return NewTask(cap(title), due, slot.time)
    }

    private fun event(kind: String?, body: String, today: LocalDate): Cmd? {
        val (rest, slot) = extractWhen(body, today, keepLead = true)
        val title = when {
            rest.isBlank() -> kind ?: return null
            kind != null && Regex("""^(?:con|de|del|para|en)\b""", RegexOption.IGNORE_CASE).containsMatchIn(rest) -> "$kind $rest"
            else -> cap(rest)
        }
        return NewEvent(title, slot.date ?: today, slot.time, slot.durationMin ?: 60)
    }

    private fun note(body: String): Cmd? {
        if (body.isBlank()) return null
        val first = body.split(Regex("""[.\n]""")).first().trim()
        val title = if (first.length <= 40) first else first.take(40).substringBeforeLast(' ', first.take(40)).trim() + "…"
        return NewNote(cap(title), cap(body))
    }

    /** Elige el elemento cuyo título mejor encaja con [query] (todas sus palabras dentro del título, el más corto). */
    fun <T> best(items: List<T>, query: String, title: (T) -> String): T? {
        val words = fold(query).split(Regex("""\s+""")).filter { it.length > 1 && it !in setOf("la", "el", "de", "del", "los", "las", "que", "mi", "mis") }
        if (words.isEmpty()) return null
        return items.filter { i -> val t = fold(title(i)); words.all { it in t } }.minByOrNull { title(it).length }
    }

    /** Tabla de atajos para mostrar al usuario: (grupo, lista de (qué hace, ejemplo)). */
    val TABLE: List<Pair<String, List<Pair<String, String>>>> = listOf(
        "Agenda" to listOf(
            "Crear una cita" to "Crea una cita mañana a las 10 con Ana",
            "Con duración" to "Crea una reunión el viernes de 17 a 18 con el equipo",
            "Añadir a la agenda" to "Añade a la agenda dentista el 20 de noviembre a las 16:30",
            "Agendar rápido" to "Agéndame revisión del coche pasado mañana a las 9",
        ),
        "Tareas" to listOf(
            "Crear una tarea" to "Crea una tarea llamar al banco mañana a las 11",
            "Recordatorio" to "Recuérdame comprar pan hoy a las 18:00",
            "Marcar como hecha" to "Marca la tarea llamar al banco como hecha",
            "Terminar" to "Completa la tarea comprar pan",
        ),
        "Notas" to listOf(
            "Crear una nota" to "Añade una nota ideas para el viaje a Lisboa",
            "Anotar" to "Anota que el código del garaje es 4821",
            "Marcar nota hecha" to "Marca la nota ideas para el viaje como hecha",
        ),
        "Correo" to listOf(
            "Redactar un correo" to "Envía un correo a ana@correo.com asunto Presupuesto diciendo que mañana se lo mando",
            "Con nombre" to "Escribe un correo a Leo diciendo que llego tarde",
        ),
        "Ir a…" to listOf(
            "Abrir una pantalla" to "Abre las tareas",
            "Escáner" to "Escanea un documento",
            "Nota de voz" to "Graba una nota de voz",
        ),
    )
}
