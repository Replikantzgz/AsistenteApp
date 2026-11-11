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
)

/** Cálculo de avisos (sin dependencias de Android, para poder probarlo). */
object Plan {
    const val TASK_HOUR = 9      // las tareas con fecha pero sin hora avisan a las 9:00
    const val ALL_DAY_HOUR = 8   // los eventos de todo el día avisan a las 8:00

    private val hm = DateTimeFormatter.ofPattern("HH:mm")

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
