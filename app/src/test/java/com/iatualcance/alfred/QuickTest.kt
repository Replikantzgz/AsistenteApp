package com.iatualcance.alfred

import com.iatualcance.alfred.ai.Quick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class QuickTest {
    private val today = LocalDate.of(2026, 11, 12)   // jueves

    @Test fun taskWithTomorrowAndTime() {
        val c = Quick.parse("Crea una tarea llamar al banco mañana a las 11", today) as Quick.NewTask
        assertEquals("Llamar al banco", c.title); assertEquals(today.plusDays(1), c.due); assertEquals(LocalTime.of(11, 0), c.time)
    }
    @Test fun reminderToday() {
        val c = Quick.parse("Recuérdame comprar pan hoy a las 18:00", today) as Quick.NewTask
        assertEquals("Comprar pan", c.title); assertEquals(today, c.due); assertEquals(LocalTime.of(18, 0), c.time)
    }
    @Test fun taskPlain() {
        val c = Quick.parse("añade una tarea que diga revisar facturas", today) as Quick.NewTask
        assertEquals("Revisar facturas", c.title); assertNull(c.due); assertNull(c.time)
    }
    @Test fun eventTimeRange() {
        val c = Quick.parse("Crea una reunión el viernes de 17 a 18 con el equipo", today) as Quick.NewEvent
        assertEquals("Reunión con el equipo", c.title); assertEquals(LocalDate.of(2026, 11, 13), c.date)
        assertEquals(LocalTime.of(17, 0), c.start); assertEquals(60, c.durationMin)
    }
    @Test fun eventDayMonthAfternoon() {
        val c = Quick.parse("Añade a la agenda dentista el 20 de noviembre a las 4 de la tarde", today) as Quick.NewEvent
        assertEquals("Dentista", c.title); assertEquals(LocalDate.of(2026, 11, 20), c.date); assertEquals(LocalTime.of(16, 0), c.start)
    }
    @Test fun eventHalfPastAndDuration() {
        val c = Quick.parse("crea una cita pasado mañana a las 5 y media durante 2 horas con Ana", today) as Quick.NewEvent
        assertEquals(today.plusDays(2), c.date); assertEquals(LocalTime.of(5, 30), c.start); assertEquals(120, c.durationMin); assertEquals("Cita con Ana", c.title)
    }
    @Test fun noteCreate() {
        val c = Quick.parse("Añade una nota ideas para el viaje a Lisboa", today) as Quick.NewNote
        assertEquals("Ideas para el viaje a Lisboa", c.title)
    }
    @Test fun noteAnota() {
        val c = Quick.parse("Anota que el código del garaje es 4821", today) as Quick.NewNote
        assertEquals("El código del garaje es 4821", c.content)
    }
    @Test fun doneTaskAndNote() {
        val t = Quick.parse("Marca la tarea llamar al banco como hecha", today) as Quick.Done
        assertEquals(false, t.isNote); assertEquals("llamar al banco", t.query)
        val n = Quick.parse("marca la nota ideas para el viaje como hecha", today) as Quick.Done
        assertEquals(true, n.isNote)
        val c = Quick.parse("Completa la tarea comprar pan", today) as Quick.Done
        assertEquals("comprar pan", c.query)
    }
    @Test fun mailWithSubjectAndBody() {
        val c = Quick.parse("Envía un correo a ana@correo.com asunto Presupuesto diciendo que mañana se lo mando", today) as Quick.Mail
        assertEquals("ana@correo.com", c.to); assertEquals("Presupuesto", c.subject); assertEquals("mañana se lo mando", c.body)
        val d = Quick.parse("Escribe un correo a Leo diciendo que llego tarde", today) as Quick.Mail
        assertEquals("Leo", d.to); assertEquals("que llego tarde".removePrefix("que "), d.body.removePrefix("que "))
    }
    @Test fun navigation() {
        assertEquals(Quick.Open("tareas"), Quick.parse("Abre las tareas", today))
        assertEquals(Quick.Open("calendario"), Quick.parse("abre la agenda", today))
        assertEquals(Quick.Scan, Quick.parse("Escanea un documento", today))
        assertEquals(Quick.Record, Quick.parse("graba una nota de voz", today))
    }
    @Test fun otherPhrasesGoToTheAssistant() {
        assertNull(Quick.parse("¿Qué tengo en la agenda esta semana?", today))
        assertNull(Quick.parse("Cuéntame algo interesante", today))
        assertNull(Quick.parse("Resume mis correos sin leer", today))
        assertNull(Quick.parse("pon música relajante", today))
    }
    @Test fun bestMatchPrefersShortest() {
        val l = listOf("Llamar al banco por la hipoteca", "Llamar al banco", "Comprar pan")
        assertEquals("Llamar al banco", Quick.best(l, "llamar al banco") { it })
        assertNull(Quick.best(l, "dentista") { it })
    }
    @Test fun tableHasExamplesThatParse() {
        Quick.TABLE.forEach { (group, rows) -> rows.forEach { (what, ex) -> assertTrue("$group/$what no se entiende: $ex", Quick.parse(ex, today) != null) } }
    }
}
