package com.iatualcance.alfred.notify

import com.iatualcance.alfred.data.Task
import com.iatualcance.alfred.google.CalEvent
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Un aviso programado: cuándo suena (milisegundos), qué dice y a qué pantalla lleva al tocarlo. */
@Serializable
data class Reminder(
    val id: String, val at: Long, val title: String, val text: String,
    val route: String, val taskId: String? = null,
    /** "summary" = resumen del día: el texto se prepara al sonar, con los datos de ese momento. */
    val kind: String? = null,
)

/** Cálculo de avisos (sin dependencias de Android, para poder probarlo). */
object Plan {
    const val TASK_HOUR = 9      // las tareas con fecha pero sin hora avisan a las 9:00
    const val ALL_DAY_HOUR = 8   // los eventos de todo el día avisan a las 8:00

    private val hm = DateTimeFormatter.ofPattern("HH:mm")
    private val es = java.util.Locale("es", "ES")

    /** Próxima vez que toca el resumen diario (siempre en el futuro). */
    fun nextDailyAt(now: Long, minutesOfDay: Int, zone: ZoneId): Long {
        val today = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        var at = today.atTime(minutesOfDay / 60, minutesOfDay % 60).atZone(zone)
        if (at.toInstant().toEpochMilli() <= now) at = at.plusDays(1)
        return at.toInstant().toEpochMilli()
    }

    fun dailyReminder(minutesOfDay: Int, now: Long, zone: ZoneId) =
        Reminder("daily", nextDailyAt(now, minutesOfDay, zone), "Resumen del día", "Preparando tu resumen…", "chat", kind = "summary")

    /** Título y texto del resumen de la mañana. [events]: (hora, título) de hoy; [tasks]: títulos de las tareas de hoy o vencidas. */
    fun summary(events: List<Pair<String, String>>, tasks: List<String>, unread: Int, name: String?, hour: Int): Pair<String, String> {
        val greet = when { hour < 13 -> "Buenos días"; hour < 21 -> "Buenas tardes"; else -> "Buenas noches" }
        val title = if (name.isNullOrBlank()) greet else "$greet, $name"
        if (events.isEmpty() && tasks.isEmpty() && unread == 0) return title to "Hoy tienes el día libre: sin citas, tareas ni correos pendientes."
        val lines = mutableListOf<String>()
        lines += if (events.isEmpty()) "📅 Sin citas hoy" else "📅 " + (if (events.size == 1) "1 cita: " else "${events.size} citas: ") + events.take(4).joinToString(" · ") { (t, n) -> "$t $n".trim() } + if (events.size > 4) " …" else ""
        lines += if (tasks.isEmpty()) "✅ Sin tareas para hoy" else "✅ " + (if (tasks.size == 1) "1 tarea: " else "${tasks.size} tareas: ") + tasks.take(4).joinToString(" · ") + if (tasks.size > 4) " …" else ""
        if (unread > 0) lines += "✉️ " + (if (unread == 1) "1 correo sin leer" else "$unread correos sin leer")
        return title to lines.joinToString("\n")
    }

    /** Líneas del widget: próximas citas y tareas pendientes. */
    fun widgetLines(events: List<CalEvent>, tasks: List<Task>, now: Long, zone: ZoneId): List<String> {
        val today = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val out = mutableListOf<String>()
        events.filter { it.end.toInstant().toEpochMilli() > now }.sortedBy { it.start }.take(3).forEach { e ->
            val d = e.start.toLocalDate()
            val day = when (d) { today -> "Hoy"; today.plusDays(1) -> "Mañana"; else -> d.format(DateTimeFormatter.ofPattern("EEE d", es)).replaceFirstChar { it.uppercase() } }
            out += if (e.allDay) "📅 $day · ${e.title}" else "📅 $day ${e.start.format(hm)} ${e.title}"
        }
        val pending = tasks.filter { !it.done }.sortedWith(compareBy<Task> { it.due ?: "9999" }.thenBy { it.createdAt })
        if (out.isEmpty() && pending.isEmpty()) return listOf("Nada pendiente. ¡Buen día!")
        pending.take(3).forEach { out += "☐ ${it.title}" + (it.due?.let { d -> " · $d" } ?: "") }
        return out
    }

    fun taskReminder(t: Task, now: Long, zone: ZoneId): Reminder? {
        if (t.done) return null
        val at = t.remindAt
            ?: t.due?.let { runCatching { LocalDate.parse(it).atTime(TASK_HOUR, 0).atZone(zone).toInstant().toEpochMilli() }.getOrNull() }
            ?: return null
        if (at <= now) return null
        return Reminder("task-${t.id}", at, "Recordatorio", t.title, "tareas", t.id)
    }

    fun eventReminder(e: CalEvent, leadMinutes: Int, now: Long): Reminder? {
        val start = e.start.toInstant().toEpochMilli()
        val at = if (e.allDay) e.start.toLocalDate().atTime(ALL_DAY_HOUR, 0).atZone(e.start.zone).toInstant().toEpochMilli()
        else start - leadMinutes * 60_000L
        if (at <= now) return null
        val text = buildString {
            if (e.allDay) append("Hoy, todo el día")
            else append(if (leadMinutes > 0) "En $leadMinutes min · " else "Ahora · ").append(e.start.format(hm))
            if (e.location.isNotBlank()) append(" · ").append(e.location)
        }
        return Reminder("ev-${e.id}-${e.start.toEpochSecond()}", at, e.title, text, "calendario")
    }
}
