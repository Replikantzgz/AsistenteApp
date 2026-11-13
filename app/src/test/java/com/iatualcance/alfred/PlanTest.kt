package com.iatualcance.alfred

import com.iatualcance.alfred.data.Task
import com.iatualcance.alfred.google.CalEvent
import com.iatualcance.alfred.notify.Plan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class PlanTest {
    private val z = ZoneId.of("Europe/Madrid")
    private fun ms(s: String) = ZonedDateTime.parse(s).toInstant().toEpochMilli()
    private val now = ms("2026-11-12T08:00:00+01:00[Europe/Madrid]")

    private fun ev(start: String, allDay: Boolean = false, loc: String = "") =
        ZonedDateTime.parse(start).let { CalEvent("e1", "Dentista", it, it.plusHours(1), allDay, location = loc) }

    @Test fun taskWithDueDateRemindsAt9() {
        val r = Plan.taskReminder(Task(id = "a", title = "Pagar luz", due = "2026-11-13"), now, z)!!
        assertEquals(ms("2026-11-13T09:00:00+01:00[Europe/Madrid]"), r.at)
        assertEquals("tareas", r.route)
    }

    @Test fun taskWithExactTimeUsesIt() {
        val at = ms("2026-11-12T18:30:00+01:00[Europe/Madrid]")
        assertEquals(at, Plan.taskReminder(Task(id = "a", title = "Llamar", due = "2026-11-12", remindAt = at), now, z)!!.at)
    }

    @Test fun doneOrPastOrUndatedTasksDoNotRemind() {
        assertNull(Plan.taskReminder(Task(id = "a", title = "x", due = "2026-11-13", done = true), now, z))
        assertNull(Plan.taskReminder(Task(id = "a", title = "x"), now, z))
        assertNull(Plan.taskReminder(Task(id = "a", title = "x", due = "2026-11-01"), now, z))
    }

    @Test fun taskDueTodayStillRemindsIfNineHasNotPassed() {
        assertEquals(ms("2026-11-12T09:00:00+01:00[Europe/Madrid]"), Plan.taskReminder(Task(id = "a", title = "x", due = "2026-11-12"), now, z)!!.at)
        assertNull(Plan.taskReminder(Task(id = "a", title = "x", due = "2026-11-12"), ms("2026-11-12T09:30:00+01:00[Europe/Madrid]"), z))
    }

    @Test fun eventRemindsBeforeStart() {
        val r = Plan.eventReminder(ev("2026-11-12T10:00:00+01:00[Europe/Madrid]", loc = "Clínica"), 15, now)!!
        assertEquals(ms("2026-11-12T09:45:00+01:00[Europe/Madrid]"), r.at)
        assertEquals("En 15 min · 10:00 · Clínica", r.text)
        assertEquals("calendario", r.route)
    }

    @Test fun eventAtExactTime() {
        val r = Plan.eventReminder(ev("2026-11-12T10:00:00+01:00[Europe/Madrid]"), 0, now)!!
        assertEquals(ms("2026-11-12T10:00:00+01:00[Europe/Madrid]"), r.at)
        assertEquals("Ahora · 10:00", r.text)
    }

    @Test fun allDayEventRemindsAt8() {
        val r = Plan.eventReminder(ev("2026-11-13T00:00:00+01:00[Europe/Madrid]", allDay = true), 15, now)!!
        assertEquals(ms("2026-11-13T08:00:00+01:00[Europe/Madrid]"), r.at)
    }

    @Test fun pastEventsAreSkipped() {
        assertNull(Plan.eventReminder(ev("2026-11-12T08:10:00+01:00[Europe/Madrid]"), 15, now))
        assertNotNull(Plan.eventReminder(ev("2026-11-12T08:20:00+01:00[Europe/Madrid]"), 15, now))
    }
}

class SummaryPlanTest {
    private val z = java.time.ZoneId.of("Europe/Madrid")
    private fun ms(s: String) = java.time.ZonedDateTime.parse(s).toInstant().toEpochMilli()

    @Test fun nextDailyIsAlwaysInTheFuture() {
        val before = ms("2026-11-12T07:00:00+01:00[Europe/Madrid]")
        val after = ms("2026-11-12T09:00:00+01:00[Europe/Madrid]")
        assertEquals(ms("2026-11-12T08:00:00+01:00[Europe/Madrid]"), Plan.nextDailyAt(before, 480, z))
        assertEquals(ms("2026-11-13T08:00:00+01:00[Europe/Madrid]"), Plan.nextDailyAt(after, 480, z))
        assertEquals(ms("2026-11-13T08:00:00+01:00[Europe/Madrid]"), Plan.nextDailyAt(ms("2026-11-12T08:00:00+01:00[Europe/Madrid]"), 480, z))
    }

    @Test fun summaryText() {
        val (t, x) = Plan.summary(listOf("10:00" to "Dentista", "17:00" to "C43"), listOf("Comprar leche"), 4, "Ana", 8)
        assertEquals("Buenos días, Ana", t)
        assertEquals("📅 2 citas: 10:00 Dentista · 17:00 C43\n✅ 1 tarea: Comprar leche\n✉️ 4 correos sin leer", x)
    }

    @Test fun emptyDayAndGreetings() {
        assertEquals("Hoy tienes el día libre: sin citas, tareas ni correos pendientes.", Plan.summary(emptyList(), emptyList(), 0, null, 8).second)
        assertEquals("Buenas tardes", Plan.summary(emptyList(), emptyList(), 0, null, 15).first)
        assertEquals("Buenas noches, Leo", Plan.summary(emptyList(), emptyList(), 0, "Leo", 22).first)
    }

    @Test fun widgetShowsUpcomingAndTasks() {
        val now = ms("2026-11-12T08:00:00+01:00[Europe/Madrid]")
        fun ev(id: String, start: String, title: String): CalEvent { val s = java.time.ZonedDateTime.parse(start); return CalEvent(id, title, s, s.plusHours(1), false) }
        val lines = Plan.widgetLines(
            listOf(ev("b", "2026-11-13T09:30:00+01:00[Europe/Madrid]", "Reunión"), ev("a", "2026-11-12T10:00:00+01:00[Europe/Madrid]", "Dentista"), ev("old", "2026-11-12T06:00:00+01:00[Europe/Madrid]", "Pasada")),
            listOf(Task(id = "1", title = "Comprar leche", due = "2026-11-12"), Task(id = "2", title = "Hecha", done = true)), now, z,
        )
        assertEquals(listOf("📅 Hoy 10:00 Dentista", "📅 Mañana 09:30 Reunión", "☐ Comprar leche · 2026-11-12"), lines)
        assertEquals(listOf("Nada pendiente. ¡Buen día!"), Plan.widgetLines(emptyList(), emptyList(), now, z))
    }
}

class SummaryOptionsTest {
    @Test fun weatherRain() {
        assertEquals("☔ Hoy va a llover (70% de probabilidad) · 8°/14°", Plan.weatherLine(61, 70, 14, 8))
        assertEquals("☀️ Hoy no llueve · 10°/22°", Plan.weatherLine(0, 5, 22, 10))
    }
    @Test fun optionsHideSections() {
        val (_, x) = Plan.summary(listOf("10:00" to "Dentista"), listOf("Leche"), 2, null, 8, notes = listOf("Llamar"), mails = listOf("Ana: Factura"), weather = "☀️ Hoy no llueve · 10°/22°",
            opts = Plan.SummaryOpts(events = false, tasks = false, notes = true, mail = true))
        assertEquals("☀️ Hoy no llueve · 10°/22°\n📝 1 nota: Llamar\n✉️ Importantes: Ana: Factura", x)
    }
    @Test fun noteReminderStopsWhenDone() {
        val n = com.iatualcance.alfred.data.Note(title = "X", remindAt = 5_000L)
        org.junit.Assert.assertNotNull(Plan.noteReminder(n, 1_000L))
        org.junit.Assert.assertNull(Plan.noteReminder(n.copy(done = true), 1_000L))
    }
}
