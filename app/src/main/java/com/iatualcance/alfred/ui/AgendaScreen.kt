package com.iatualcance.alfred.ui

import androidx.compose.foundation.background
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
private val monthShort = DateTimeFormatter.ofPattern("MMM yyyy", Util.ES)
private val hm = DateTimeFormatter.ofPattern("HH:mm")
private val shortDate = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Util.ES)

@Composable
fun AgendaScreen() = GoogleGate("tu calendario", com.iatualcance.alfred.R.drawable.art_agenda) {
    val sv = LocalServices.current
    val ui = LocalUi.current
    var month by remember { mutableStateOf(YearMonth.now()) }
    var events by remember { mutableStateOf<List<CalEvent>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<CalEvent?>(null) }
    var creating by remember { mutableStateOf(false) }

    LaunchedEffect(ui.calendarDate) { ui.calendarDate?.let { month = YearMonth.from(it); ui.calendarDate = null } }

    LaunchedEffect(month, reload) {
        events = null; error = null
        val zone = ZoneId.systemDefault()
        try {
            events = sv.google.listEvents(month.atDay(1).atStartOfDay(zone), month.plusMonths(1).atDay(1).atStartOfDay(zone))
        } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { error = e.message ?: e.toString() }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            PixelBar(
                title = { Text(month.format(monthShort).uppercase(), maxLines = 1) },
                actions = {
                    IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.Default.ChevronLeft, "Mes anterior") }
                    IconButton(onClick = { month = YearMonth.now() }) { Icon(Icons.Default.Today, "Hoy") }
                    IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.Default.ChevronRight, "Mes siguiente") }
                },
            )
            val evs = events
            when {
                error != null -> ErrorBox(error!!) { reload++ }
                evs == null -> Loading()
                evs.isEmpty() -> ArtHint(com.iatualcance.alfred.R.drawable.art_agenda, "No hay eventos este mes.\nPulsa + para crear uno o pídeselo al asistente.")
                else -> {
                    val today = LocalDate.now()
                    // En el mes actual se ocultan los días ya pasados.
                    val shown = if (month == YearMonth.now()) evs.filter { !it.end.toLocalDate().isBefore(today) }.ifEmpty { evs } else evs
                    val byDay = shown.groupBy { it.start.toLocalDate() }.toSortedMap()
                    LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 88.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        byDay.forEach { (day, list) ->
                            item(key = day.toString()) {
                                Text(
                                    (if (day == today) "Hoy · " else "") + day.format(dayFmt).replaceFirstChar { it.uppercase() },
                                    color = if (day == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                                )
                            }
                            list.forEach { e -> item(key = e.id + e.start) { EventRow(e) { editing = e } } }
                        }
                    }
                }
            }
        }
        FloatingActionButton(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White, shape = PxShape, onClick = { creating = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Default.Add, "Nuevo evento") }
    }

    if (creating || editing != null) EventDialog(
        event = editing, defaultDay = if (month == YearMonth.now()) LocalDate.now() else month.atDay(1),
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
