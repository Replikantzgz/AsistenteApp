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
