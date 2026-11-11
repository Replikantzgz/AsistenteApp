package com.iatualcance.alfred.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iatualcance.alfred.R
import com.iatualcance.alfred.Util
import com.iatualcance.alfred.ai.Nav
import com.iatualcance.alfred.data.Note
import com.iatualcance.alfred.data.Task
import com.iatualcance.alfred.data.Template
import com.iatualcance.alfred.google.CalEvent
import com.iatualcance.alfred.google.Contact
import com.iatualcance.alfred.google.DriveFile
import com.iatualcance.alfred.google.Mail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private class Found(
    val notes: List<Note> = emptyList(), val tasks: List<Task> = emptyList(), val templates: List<Template> = emptyList(),
    val events: List<CalEvent> = emptyList(), val mails: List<Mail> = emptyList(), val contacts: List<Contact> = emptyList(), val files: List<DriveFile> = emptyList(),
) {
    val total get() = notes.size + tasks.size + templates.size + events.size + mails.size + contacts.size + files.size
}

private val evFmt = DateTimeFormatter.ofPattern("EEE d MMM yyyy · HH:mm", Util.ES)

/** Un solo cuadro que busca en notas, tareas, plantillas, agenda, correo, contactos y Drive. */
@Composable
fun SearchScreen() {
    val sv = LocalServices.current
    val ui = LocalUi.current
    val ctx = LocalContext.current
    var q by rememberSaveable { mutableStateOf("") }
    var found by remember { mutableStateOf<Found?>(null) }
    var busy by remember { mutableStateOf(false) }
    var contacts by remember { mutableStateOf<List<Contact>?>(null) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    LaunchedEffect(q) {
        val text = q.trim()
        if (text.length < 2) { found = null; busy = false; return@LaunchedEffect }
        delay(400); busy = true
        try {
            found = coroutineScope {
                val google = sv.hasGoogle
                val now = ZonedDateTime.now()
                val ev = async { if (google) runCatching { sv.google.listEvents(now.minusYears(1), now.plusYears(1), text) }.getOrDefault(emptyList()).take(8) else emptyList() }
                val ml = async { if (google) runCatching { sv.google.listMail(text, 8) }.getOrDefault(emptyList()) else emptyList() }
                val dr = async { if (google) runCatching { sv.google.listDrive(parent = null, query = text) }.getOrDefault(emptyList()).take(8) else emptyList() }
                val ct = async {
                    if (!google) emptyList() else runCatching {
                        val all = contacts ?: sv.google.listContacts().also { contacts = it }
                        val w = text.lowercase()
                        all.filter { c -> w in c.name.lowercase() || c.emails.any { w in it.lowercase() } || c.phones.any { w in it } }.take(8)
                    }.getOrDefault(emptyList())
                }
                val w = text.lowercase()
                Found(
                    notes = sv.store.searchNotes(text).take(8),
                    tasks = sv.store.tasks.value.filter { w in it.title.lowercase() }.take(8),
                    templates = sv.store.templates.value.filter { w in it.name.lowercase() || w in it.content.lowercase() }.take(5),
                    events = ev.await(), mails = ml.await(), contacts = ct.await(), files = dr.await(),
                )
            }
        } catch (e: CancellationException) { throw e } catch (_: Exception) { found = Found() }
        busy = false
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            q, { q = it }, modifier = Modifier.fillMaxWidth().padding(12.dp).focusRequester(focus), singleLine = true, shape = PxShape,
            placeholder = { Text("Busca en todo: notas, correo, agenda, contactos, Drive…") }, leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { if (q.isNotEmpty()) IconButton(onClick = { q = "" }) { Icon(Icons.Default.Close, "Borrar") } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = {}),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedContainerColor = androidx.compose.ui.graphics.Color.White, unfocusedContainerColor = androidx.compose.ui.graphics.Color.White),
        )
        val f = found
        when {
            busy && f == null -> Loading()
            f == null -> ArtHint(R.drawable.art_pikachu, if (!sv.hasGoogle) "Escribe al menos 2 letras.\nConecta tu cuenta de Google para buscar también en correo, agenda, contactos y Drive." else "Escribe al menos 2 letras.")
            f.total == 0 -> ArtHint(R.drawable.art_pikachu, "Sin resultados para «${q.trim()}».")
            else -> LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                fun section(title: String, n: Int) { if (n > 0) item(key = "h$title") { Text("$title · $n", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 10.dp)) } }
                if (busy) item { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)); Hint("Buscando…") } }

                section("Notas", f.notes.size)
                f.notes.forEach { n -> item(key = "n${n.id}") { Hit(Icons.AutoMirrored.Filled.StickyNote2, n.title.ifBlank { "Sin título" }, n.content.take(80)) { ui.overlay = null; ui.openNote(n.id) } } }
                section("Tareas", f.tasks.size)
                f.tasks.forEach { t -> item(key = "t${t.id}") { Hit(Icons.Default.CheckCircle, t.title, listOfNotNull(t.due, if (t.done) "hecha" else null).joinToString(" · ")) { ui.navigate(Nav.Screen("tareas")) } } }
                section("Plantillas", f.templates.size)
                f.templates.forEach { t -> item(key = "p${t.id}") { Hit(Icons.Default.Description, t.name, t.content.take(80)) { ui.navigate(Nav.Screen("plantillas")) } } }
                section("Agenda", f.events.size)
                f.events.forEach { e -> item(key = "e${e.id}${e.start}") { Hit(Icons.Default.CalendarMonth, e.title, if (e.allDay) e.start.toLocalDate().toString() else e.start.format(evFmt)) { ui.navigate(Nav.Calendar(e.start.toLocalDate())) } } }
                section("Correo", f.mails.size)
                f.mails.forEach { m -> item(key = "m${m.id}") { Hit(Icons.Default.Mail, m.subject, Util.splitAddress(m.from).first + " · " + m.snippet.take(60)) { ui.overlay = null; ui.compose = null; ui.mailOpenId = m.id; ui.tab = Tab.MAIL } } }
                section("Contactos", f.contacts.size)
                f.contacts.forEach { c -> item(key = "c${c.resource}") { Hit(Icons.Default.Contacts, c.name.ifBlank { c.emails.firstOrNull().orEmpty() }, (c.emails + c.phones).joinToString(" · ")) { ui.overlay = null; ui.contactQuery = c.name.ifBlank { c.emails.firstOrNull().orEmpty() }; ui.tab = Tab.CONTACTS } } }
                section("Drive", f.files.size)
                f.files.forEach { d -> item(key = "d${d.id}") { Hit(if (d.isFolder) Icons.Default.Folder else Icons.Default.Description, d.name, d.modified?.take(10).orEmpty()) { d.link?.let { ctx.openUrl(it) } } } }
            }
        }
    }
}

@Composable
private fun Hit(icon: ImageVector, title: String, sub: String, onClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = PxShape, modifier = Modifier.fillMaxWidth().pxBorder().clickable(onClick = onClick)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
