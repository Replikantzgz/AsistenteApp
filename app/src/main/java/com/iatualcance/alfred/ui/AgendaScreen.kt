package com.iatualcance.alfred.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iatualcance.alfred.Util
import com.iatualcance.alfred.google.CalEvent
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dayFmt = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Util.ES)
private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Util.ES)
private val shortDay = DateTimeFormatter.ofPattern("d MMM", Util.ES)
private val shortDayYear = DateTimeFormatter.ofPattern("d MMM yyyy", Util.ES)
private val hm = DateTimeFormatter.ofPattern("HH:mm")
private val shortDate = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Util.ES)

@Composable
fun AgendaScreen() = GoogleGate("tu calendario", com.iatualcance.alfred.R.drawable.art_agenda) {
    val sv = LocalServices.current
    val ui = LocalUi.current
    var anchor by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }      // día de referencia
    var view by rememberSaveable { mutableIntStateOf(2) }                              // 0 día · 1 semana · 2 mes
    var events by remember { mutableStateOf<List<CalEvent>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<CalEvent?>(null) }
    var creating by remember { mutableStateOf(false) }

    val date = LocalDate.parse(anchor)
    val monday = date.with(java.time.DayOfWeek.MONDAY)
    val (from, to) = when (view) {
        0 -> date to date.plusDays(1)
        1 -> monday to monday.plusDays(7)
        else -> date.withDayOfMonth(1).let { it to it.plusMonths(1) }
    }
    fun step(n: Int) { anchor = when (view) { 0 -> date.plusDays(n.toLong()); 1 -> date.plusWeeks(n.toLong()); else -> date.withDayOfMonth(1).plusMonths(n.toLong()) }.toString() }
    val title = when (view) {
        0 -> date.format(dayFmt)
        1 -> monday.format(shortDay) + " – " + monday.plusDays(6).format(shortDayYear)
        else -> date.format(monthFmt)
    }.replaceFirstChar { it.uppercase() }

    LaunchedEffect(ui.calendarDate) { ui.calendarDate?.let { anchor = it.toString(); view = 0; ui.calendarDate = null } }

    LaunchedEffect(anchor, view, reload) {
        events = null; error = null
        val zone = ZoneId.systemDefault()
        try {
            events = sv.google.listEvents(from.atStartOfDay(zone), to.atStartOfDay(zone))
        } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { error = e.message ?: e.toString() }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            PixelBar(title = { Text("AGENDA") })

            // Navegación por fechas, justo bajo la cabecera
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { step(-1) }) { Icon(Icons.Default.ChevronLeft, "Anterior") }
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                IconButton(onClick = { step(1) }) { Icon(Icons.Default.ChevronRight, "Siguiente") }
                PillChip("Hoy", { anchor = LocalDate.now().toString() })
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Día", "Semana", "Mes").forEachIndexed { i, t ->
                    androidx.compose.material3.FilterChip(view == i, { view = i }, { Text(t, fontWeight = FontWeight.SemiBold) }, shape = androidx.compose.foundation.shape.RoundedCornerShape(50), modifier = Modifier.weight(1f))
                }
            }

            val evs = events
            when {
                error != null -> ErrorBox(error!!) { reload++ }
                evs == null -> Loading()
                view == 2 -> MonthGrid(date, evs.groupBy { it.start.toLocalDate() }) { d -> anchor = d.toString(); view = 0 }
                evs.isEmpty() && view != 1 -> ArtHint(com.iatualcance.alfred.R.drawable.art_agenda, if (view == 0) "No tienes nada este día.\nPulsa + para crear un evento o pídeselo al asistente." else "No hay eventos este mes.\nPulsa + para crear uno o pídeselo al asistente.")
                else -> {
                    val today = LocalDate.now()
                    val byDay = evs.groupBy { it.start.toLocalDate() }
                    val days = when (view) {
                        0 -> listOf(date)
                        1 -> (0L..6L).map { monday.plusDays(it) }
                        else -> {
                            // En el mes actual se ocultan los días ya pasados.
                            val all = byDay.keys.sorted()
                            if (date.year == today.year && date.month == today.month) all.filter { !it.isBefore(today) }.ifEmpty { all } else all
                        }
                    }
                    LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        days.forEach { day ->
                            if (view != 0) item(key = "h$day") {
                                Text(
                                    (if (day == today) "Hoy · " else "") + day.format(dayFmt).replaceFirstChar { it.uppercase() },
                                    color = if (day == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                                )
                            }
                            val list = byDay[day].orEmpty()
                            if (list.isEmpty() && view == 1) item(key = "e$day") { Text("Sin eventos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(start = 4.dp)) }
                            list.forEach { e -> item(key = e.id + e.start) { EventRow(e) { editing = e } } }
                        }
                    }
                }
            }
        }
        FloatingActionButton(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White, shape = PxShape, onClick = { creating = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Default.Add, "Nuevo evento") }
    }

    if (creating || editing != null) EventDialog(
        event = editing, defaultDay = if (view == 2 && date.month == LocalDate.now().month && date.year == LocalDate.now().year) LocalDate.now() else date,
        onDismiss = { creating = false; editing = null },
        onSave = { f ->
            val ev = editing
            creating = false; editing = null
            ui.run("Guardando evento…") {
                val zone = ZoneId.systemDefault()
                val s = f.date.atTime(if (f.allDay) LocalTime.MIDNIGHT else f.start).atZone(zone)
                var en = f.endDate.atTime(if (f.allDay) LocalTime.MIDNIGHT else f.end).atZone(zone)
                if (f.allDay) en = en.plusDays(1) else if (!en.isAfter(s)) en = s.plusHours(1)
                val guests = f.guests.split(',', ';', ' ').map { it.trim() }.filter { "@" in it }
                if (ev == null) sv.google.createEvent(f.title.ifBlank { "(Sin título)" }, s, en, f.allDay, f.description, f.location, guests)
                else sv.google.updateEvent(ev.id, f.title, s, en, f.allDay, f.description, f.location, guests)
                reload++
                runCatching { sv.refreshReminders(true) }
            }
        },
        onDelete = { ev -> editing = null; ui.run("Eliminando…") { sv.google.deleteEvent(ev.id); reload++; runCatching { sv.refreshReminders(true) } } },
    )
}

@Composable
private fun EventRow(e: CalEvent, onClick: () -> Unit) = Surface(
    color = MaterialTheme.colorScheme.surfaceContainer, shape = PxShape,
    modifier = Modifier.pxBorder().fillMaxWidth().clickable(onClick = onClick),
) {
    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(38.dp).background(MaterialTheme.colorScheme.primary, PxShape))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(e.title, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val time = if (e.allDay) "Todo el día" else "${e.start.format(hm)} – ${e.end.format(hm)}"
            Text(listOf(time, e.location).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

class EventForm(
    val title: String, val date: LocalDate, val endDate: LocalDate, val start: LocalTime, val end: LocalTime,
    val allDay: Boolean, val location: String, val description: String, val guests: String,
)

@Composable
private fun EventDialog(event: CalEvent?, defaultDay: LocalDate, onDismiss: () -> Unit, onSave: (EventForm) -> Unit, onDelete: (CalEvent) -> Unit) {
    val nextHour = LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
    var title by remember { mutableStateOf(event?.title ?: "") }
    var allDay by remember { mutableStateOf(event?.allDay ?: false) }
    var date by remember { mutableStateOf(event?.start?.toLocalDate() ?: defaultDay) }
    var endDate by remember {
        mutableStateOf(event?.let { if (it.allDay) it.end.toLocalDate().minusDays(1).coerceAtLeast(it.start.toLocalDate()) else it.end.toLocalDate() } ?: defaultDay)
    }
    var start by remember { mutableStateOf(event?.takeIf { !it.allDay }?.start?.toLocalTime() ?: nextHour) }
    var end by remember { mutableStateOf(event?.takeIf { !it.allDay }?.end?.toLocalTime() ?: nextHour.plusHours(1)) }
    var location by remember { mutableStateOf(event?.location ?: "") }
    var description by remember { mutableStateOf(event?.description ?: "") }
    var guests by remember { mutableStateOf(event?.attendees?.joinToString(", ") ?: "") }
    var askDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (event == null) "Nuevo evento" else "Editar evento") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Todo el día", Modifier.weight(1f)); Switch(allDay, { allDay = it })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateButton(date, Modifier.weight(1f)) { date = it; if (endDate.isBefore(it)) endDate = it }
                    if (!allDay) TimeButton(start) { start = it; end = it.plusHours(1); if (end.isBefore(it)) endDate = date.plusDays(1) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateButton(endDate, Modifier.weight(1f)) { endDate = if (it.isBefore(date)) date else it }
                    if (!allDay) TimeButton(end) { end = it }
                }
                OutlinedTextField(location, { location = it }, label = { Text("Lugar") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(guests, { guests = it }, label = { Text("Invitados (correos separados por comas)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it }, label = { Text("Descripción") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                if (event != null) TextButton(onClick = { askDelete = true }) { Text("Eliminar evento", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(EventForm(title.trim(), date, endDate, start, end, allDay, location.trim(), description.trim(), guests)) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
    if (askDelete && event != null) Confirm("Eliminar evento", "«${event.title}» se eliminará de Google Calendar.", "Eliminar", { askDelete = false }) { onDelete(event) }
}

@Composable
fun DateButton(date: LocalDate, modifier: Modifier = Modifier, onPick: (LocalDate) -> Unit) {
    var open by remember { mutableStateOf(false) }
    AppOutlinedButton(onClick = { open = true }, modifier = modifier, contentPadding = PaddingValues(horizontal = 10.dp)) { Text(date.format(shortDate), maxLines = 1) }
    if (open) {
        val st = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }; open = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } },
        ) { DatePicker(st) }
    }
}

@Composable
fun TimeButton(time: LocalTime, onPick: (LocalTime) -> Unit) {
    var open by remember { mutableStateOf(false) }
    AppOutlinedButton(onClick = { open = true }, contentPadding = PaddingValues(horizontal = 14.dp)) { Text(time.format(hm)) }
    if (open) {
        val st = rememberTimePickerState(time.hour, time.minute, true)
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { onPick(LocalTime.of(st.hour, st.minute)); open = false }) { Text("Aceptar") } },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } },
            text = { TimePicker(st) },
        )
    }
}

/** Mes en cuadrícula: una raya roja por evento (hasta 3) y, al tocar un día, se abre su vista de día. */
@Composable
private fun MonthGrid(date: LocalDate, byDay: Map<LocalDate, List<CalEvent>>, onDay: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    val first = date.withDayOfMonth(1)
    val start = first.with(java.time.DayOfWeek.MONDAY)
    val weeks = ((first.dayOfWeek.value - 1) + first.lengthOfMonth() + 6) / 7
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).card().padding(6.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            listOf("L", "M", "X", "J", "V", "S", "D").forEach {
                Text(it, Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        for (w in 0 until weeks) Row(Modifier.fillMaxWidth()) {
            for (d in 0..6) {
                val day = start.plusDays((w * 7 + d).toLong())
                val inMonth = day.month == first.month
                val n = byDay[day].orEmpty().size
                val isToday = day == today
                Column(
                    Modifier.weight(1f).height(68.dp).padding(2.dp)
                        .background(if (isToday) Color(0xFFFFEDEA) else Color.Transparent, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                        .border(1.dp, if (isToday) Red else SoftLine.copy(alpha = if (inMonth) 1f else 0.4f), androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                        .clickable { onDay(day) }.padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        day.dayOfMonth.toString(), fontSize = 15.sp, fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (isToday) Red else if (inMonth) Ink else MaterialTheme.colorScheme.outline,
                    )
                    Spacer(Modifier.height(4.dp))
                    repeat(minOf(n, 3)) { Box(Modifier.padding(bottom = 3.dp).fillMaxWidth(0.7f).height(4.dp).background(Red, androidx.compose.foundation.shape.RoundedCornerShape(2.dp))) }
                }
            }
        }
    }
}
