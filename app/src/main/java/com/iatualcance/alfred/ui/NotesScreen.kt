package com.iatualcance.alfred.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import coil.compose.AsyncImage
import com.iatualcance.alfred.Util
import com.iatualcance.alfred.data.Attachment
import com.iatualcance.alfred.data.Note
import com.iatualcance.alfred.data.Task
import com.iatualcance.alfred.data.Template
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val noteDate = DateTimeFormatter.ofPattern("d MMM", Util.ES)

fun Context.copyText(text: String) {
    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Alfred", text))
}

@Composable
fun NotesScreen() {
    val ui = LocalUi.current
    val sub = ui.notesSub
    val open = ui.openNoteId
    if (open != null) { NoteEditor(open) { ui.openNoteId = null }; return }
    Column(Modifier.fillMaxSize()) {
        PixelBar(title = { Text("NOTAS") })
        TabRow(selectedTabIndex = sub, containerColor = MaterialTheme.colorScheme.background) {
            listOf("Notas", "Tareas", "Plantillas").forEachIndexed { i, t -> Tab(sub == i, { ui.notesSub = i }, text = { Text(t.uppercase(), fontWeight = FontWeight.Bold) }) }
        }
        when (sub) { 0 -> NotesList(); 1 -> TasksList(); else -> TemplatesList() }
    }
}

// =============================================================== Lista de notas
@Composable
private fun NotesList() {
    val sv = LocalServices.current
    val ui = LocalUi.current
    val notes by sv.store.notes.collectAsStateCompat()
    var q by rememberSaveable { mutableStateOf("") }

    val scan = rememberScanner { files -> ui.run("Analizando el documento…") { ui.openNote(sv.documentToNote(files).id) } }
    fun audio(att: Attachment) = ui.run("Transcribiendo el audio…") {
        val n = if (sv.hasAi) sv.audioToNote(att) else sv.store.upsertNote(Note(title = "Nota de voz", attachments = listOf(att), tags = listOf("audio")))
        if (!sv.hasAi) ui.say("Audio guardado. Añade la clave de Gemini en Ajustes para transcribirlo.")
        ui.openNote(n.id)
    }
    val record = rememberRecorder({ sv.store.newAttachmentFile("aac") }) { f -> audio(Attachment(fileName = f.name, mime = "audio/aac", label = "Grabación")) }
    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) ui.run("Importando el audio…") {
            val att = withContext(Dispatchers.IO) { sv.store.importUri(uri, "audio/ogg", "Audio") }
            audio(att)
        }
    }

    androidx.compose.runtime.LaunchedEffect(ui.pending) {
        when (ui.pending) { "scan" -> scan(); "record" -> record() }
        ui.pending = null
    }

    Box(Modifier.fillMaxSize()) {
        Column {
            Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(q, { q = it }, Modifier.weight(1f), placeholder = { Text("Buscar notas") }, singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) }, shape = PxShape)
                IconButton(onClick = scan) { Icon(Icons.Default.DocumentScanner, "Escanear documento") }
                IconButton(onClick = record) { Icon(Icons.Default.Mic, "Nota de voz") }
                IconButton(onClick = { pickAudio.launch("audio/*") }) { Icon(Icons.Default.AudioFile, "Transcribir un audio") }
            }
            val shown = (if (q.isBlank()) notes else sv.store.searchNotes(q)).sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.updatedAt })
            if (shown.isEmpty()) ArtHint(com.iatualcance.alfred.R.drawable.art_notas, if (q.isBlank()) "Aún no hay notas.\nCrea una con +, escanea un ticket, graba una nota de voz o comparte algo con la app desde otra." else "Sin resultados.") else LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 88.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown, key = { it.id }) { n -> NoteCard(n) { ui.openNoteId = n.id } }
            }
        }
        FloatingActionButton(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White, shape = PxShape, onClick = { ui.openNoteId = sv.store.upsertNote(Note()).id }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Default.Add, "Nueva nota")
        }
    }
}

@Composable
private fun NoteCard(n: Note, onClick: () -> Unit) {
    val sv = LocalServices.current
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = PxShape, modifier = Modifier.pxBorder().fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (n.remindAt != null) Checkbox(n.done, { sv.store.upsertNote(n.copy(done = it)) })
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (n.pinned) { Icon(Icons.Default.PushPin, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(4.dp)) }
                    Text(n.title.ifBlank { "Sin título" }, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, textDecoration = if (n.done) TextDecoration.LineThrough else null)
                }
                if (n.content.isNotBlank()) Text(n.content, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val meta = buildList {
                    add(Instant.ofEpochMilli(n.updatedAt).atZone(ZoneId.systemDefault()).format(noteDate))
                    n.remindAt?.let { add((if (n.done) "✔ " else "⏰ ") + Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("d MMM HH:mm", com.iatualcance.alfred.Util.ES))) }
                    if (n.attachments.isNotEmpty()) add("📎 ${n.attachments.size}")
                    if (n.driveFileId != null || n.attachments.any { !it.driveLink.isNullOrBlank() }) add("☁️")
                    addAll(n.tags.map { "#$it" })
                }
                Text(meta.joinToString("  "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            n.attachments.firstOrNull { it.mime.startsWith("image/") }?.let {
                Spacer(Modifier.width(10.dp))
                AsyncImage(sv.store.fileOf(it), null, Modifier.size(56.dp).clip(PxShape), contentScale = ContentScale.Crop)
            }
        }
    }
}

// =============================================================== Editor de nota
@Composable
private fun NoteEditor(id: String, onClose: () -> Unit) {
    val sv = LocalServices.current
    val ui = LocalUi.current
    val ctx = LocalContext.current
    val initial = remember(id) { sv.store.note(id) }
    if (initial == null) { androidx.compose.runtime.LaunchedEffect(id) { onClose() }; return }

    var note by remember(id) { mutableStateOf(initial) }
    var title by remember(id) { mutableStateOf(initial.title) }
    var content by remember(id) { mutableStateOf(initial.content) }
    var tags by remember(id) { mutableStateOf(initial.tags.joinToString(", ")) }
    var deleted by remember(id) { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }
    var cameraFile by rememberSaveable { mutableStateOf<String?>(null) }

    fun current() = note.copy(title = title.trim(), content = content, tags = tags.split(',', '#').map { it.trim() }.filter { it.isNotBlank() }.distinct())
    fun load(n: Note) { note = n; title = n.title; content = n.content; tags = n.tags.joinToString(", ") }

    /** Guarda si hay cambios. Una nota vacía se descarta al cerrar. */
    fun persist(closing: Boolean): Note {
        val c = current()
        if (deleted) return c
        val empty = c.title.isBlank() && c.content.isBlank() && c.attachments.isEmpty()
        if (empty) { if (closing) sv.store.deleteNote(c.id); return c }
        val stored = sv.store.note(c.id)
        if (stored == null || stored.copy(updatedAt = 0) != c.copy(updatedAt = 0)) note = sv.store.upsertNote(c)
        return note
    }
    fun close() { persist(true); onClose() }

    BackHandler { close() }
    DisposableEffect(id) { onDispose { persist(true) } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { persist(false) }

    fun addAttachments(list: List<Attachment>) { note = note.copy(attachments = note.attachments + list); persist(false) }

    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isNotEmpty()) ui.run("Añadiendo imágenes…") {
            addAttachments(withContext(Dispatchers.IO) { uris.map { sv.store.importUri(it, "image/jpeg", "Imagen") } })
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val name = cameraFile
        cameraFile = null
        if (name != null) {
            val f = java.io.File(sv.store.attachDir, name)
            if (ok && f.length() > 0) addAttachments(listOf(Attachment(fileName = name, mime = "image/jpeg", label = "Foto"))) else f.delete()
        }
    }
    val scan = rememberScanner { files -> ui.run("Analizando el documento…") { load(sv.documentToNote(files, into = persist(false))) } }
    fun transcribe(att: Attachment) {
        if (!sv.hasAi) { if (note.attachments.none { it.id == att.id }) addAttachments(listOf(att)); ui.say("Añade la clave de Gemini en Ajustes para transcribir."); return }
        ui.run("Transcribiendo el audio…") { load(sv.audioToNote(att, into = persist(false))) }
    }
    val record = rememberRecorder({ sv.store.newAttachmentFile("aac") }) { f -> transcribe(Attachment(fileName = f.name, mime = "audio/aac", label = "Grabación")) }
    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) ui.run("Importando el audio…") { transcribe(withContext(Dispatchers.IO) { sv.store.importUri(uri, "audio/ogg", "Audio") }) }
    }

    Column(Modifier.fillMaxSize()) {
        PixelBar(
            title = {},
            navigationIcon = { IconButton(onClick = ::close) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
            actions = {
                IconButton(onClick = { note = note.copy(pinned = !note.pinned) }) {
                    Icon(if (note.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, "Fijar", tint = if (note.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = {
                    if (!sv.hasGoogle) ui.say("Conecta tu cuenta de Google en Ajustes (engranaje de la pantalla de Alfred) para guardar en Drive")
                    else ui.run("Guardando en Drive…") { load(sv.uploadNote(persist(false))); ui.say("Guardado en Drive, carpeta Alfred") }
                }) { Icon(if (note.driveFileId != null) Icons.Default.CloudDone else Icons.Default.CloudUpload, "Guardar en Drive") }
                IconButton(onClick = { ctx.shareText(listOf(title, content).filter { it.isNotBlank() }.joinToString("\n\n")) }) { Icon(Icons.Default.Share, "Compartir") }
                IconButton(onClick = { askDelete = true }) { Icon(Icons.Default.Delete, "Eliminar") }
            },
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
            val bare = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            )
            // Aviso de la nota + check "hecho" (igual que en las tareas)
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val at = note.remindAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) }
                if (at == null) com.iatualcance.alfred.ui.PillChip("⏰ Añadir aviso", { note = note.copy(remindAt = LocalDate.now().atTime(java.time.LocalTime.now().plusHours(1).withSecond(0).withNano(0)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), done = false) })
                else {
                    Checkbox(note.done, { note = note.copy(done = it) })
                    DateButton(at.toLocalDate(), Modifier.weight(1f)) { d -> note = note.copy(remindAt = d.atTime(at.toLocalTime()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()) }
                    TimeButton(at.toLocalTime()) { t -> note = note.copy(remindAt = at.toLocalDate().atTime(t).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()) }
                    IconButton(onClick = { note = note.copy(remindAt = null, done = false) }) { Icon(Icons.Default.Close, "Quitar aviso") }
                }
            }
            TextField(title, { title = it }, Modifier.fillMaxWidth(), placeholder = { Text("Título", style = MaterialTheme.typography.headlineSmall) },
                textStyle = MaterialTheme.typography.headlineSmall, colors = bare)
            if (note.attachments.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                note.attachments.forEach { a ->
                    AttachmentTile(a,
                        onRemove = { sv.store.fileOf(a).delete(); note = note.copy(attachments = note.attachments.filterNot { it.id == a.id }); persist(false) },
                        onTranscribe = { transcribe(a) },
                        onAnalyze = {
                            if (!sv.hasAi) ui.say("Añade la clave de Gemini en Ajustes") else ui.run("Leyendo el documento…") {
                                val info = sv.assistant.analyzeDocument(withContext(Dispatchers.IO) { sv.store.fileOf(a).readBytes() }, a.mime)
                                content = listOf(content, info.body).filter { it.isNotBlank() }.joinToString("\n\n")
                                if (title.isBlank()) title = info.title
                                persist(false)
                            }
                        })
                }
            }
            TextField(content, { content = it }, Modifier.fillMaxWidth(), placeholder = { Text("Escriba aquí…") }, colors = bare, minLines = 8)
            TextField(tags, { tags = it }, Modifier.fillMaxWidth(), placeholder = { Text("Etiquetas, separadas por comas") }, singleLine = true, colors = bare,
                textStyle = MaterialTheme.typography.bodySmall)
        }
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton(onClick = { pickImages.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Icon(Icons.Default.Image, "Añadir imagen") }
                IconButton(onClick = {
                    val f = sv.store.newAttachmentFile("jpg")
                    cameraFile = f.name
                    runCatching { camera.launch(ctx.fileUri(f)) }.onFailure { ui.say("No se pudo abrir la cámara") }
                }) { Icon(Icons.Default.PhotoCamera, "Hacer foto") }
                IconButton(onClick = scan) { Icon(Icons.Default.DocumentScanner, "Escanear") }
                IconButton(onClick = record) { Icon(Icons.Default.Mic, "Grabar y transcribir") }
                IconButton(onClick = { pickAudio.launch("audio/*") }) { Icon(Icons.Default.AudioFile, "Transcribir un audio") }
            }
        }
    }
    if (askDelete) Confirm("Eliminar nota", "La nota y sus adjuntos se borrarán del móvil. Lo que ya esté en Drive no se toca.", "Eliminar", { askDelete = false }) {
        deleted = true; sv.store.deleteNote(id); onClose()
    }
}

@Composable
private fun AttachmentTile(a: Attachment, onRemove: () -> Unit, onTranscribe: () -> Unit, onAnalyze: () -> Unit) {
    val sv = LocalServices.current
    val ui = LocalUi.current
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    val file = sv.store.fileOf(a)
    val isImage = a.mime.startsWith("image/")
    val isAudio = a.mime.startsWith("audio/") || a.mime == "application/ogg"
    Box {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = PxShape, modifier = Modifier.pxBorder().size(104.dp).clickable { menu = true }) {
            if (isImage) AsyncImage(file, a.label, contentScale = ContentScale.Crop)
            else Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                if (isAudio) PlayButton(file) else Icon(if (a.mime == "application/pdf") Icons.Default.PictureAsPdf else Icons.Default.AttachFile, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                Text(a.label, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        DropdownMenu(menu, { menu = false }) {
            DropdownMenuItem({ Text("Abrir") }, { menu = false; if (!ctx.viewFile(file, a.mime)) ui.say("No hay ninguna app para abrir este archivo") })
            if (isAudio) DropdownMenuItem({ Text("Transcribir") }, { menu = false; onTranscribe() })
            if (isImage || a.mime == "application/pdf") DropdownMenuItem({ Text("Extraer datos con IA") }, { menu = false; onAnalyze() })
            if (!a.driveLink.isNullOrBlank()) DropdownMenuItem({ Text("Ver en Drive") }, { menu = false; ctx.openUrl(a.driveLink) })
            DropdownMenuItem({ Text("Quitar", color = MaterialTheme.colorScheme.error) }, { menu = false; onRemove() })
        }
    }
}

// =============================================================== Tareas
@Composable
private fun TasksList() {
    val sv = LocalServices.current
    val tasks by sv.store.tasks.collectAsStateCompat()
    var text by rememberSaveable { mutableStateOf("") }
    var due by rememberSaveable { mutableStateOf<String?>(null) }
    var time by rememberSaveable { mutableStateOf<String?>(null) }   // HH:mm
    var pickDue by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }
    val today = LocalDate.now().toString()

    fun add() {
        if (text.isBlank()) return
        val day = due ?: if (time != null) LocalDate.now().toString() else null
        val remind = time?.let { runCatching { LocalDate.parse(day).atTime(java.time.LocalTime.parse(it)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }.getOrNull() }
        sv.store.upsertTask(Task(title = text.trim(), due = day, remindAt = remind))
        text = ""; due = null; time = null
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(text, { text = it }, Modifier.weight(1f), singleLine = true, shape = PxShape,
                placeholder = { Text(if (due == null && time == null) "Nueva tarea" else "Aviso: " + listOfNotNull(due, time).joinToString(" ")) })
            IconButton(onClick = { pickDue = true }) { Icon(Icons.Default.Event, "Fecha límite", tint = if (due != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }
            IconButton(onClick = { pickTime = true }) { Icon(Icons.Default.Alarm, "Hora del aviso", tint = if (time != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }
            IconButton(onClick = ::add) { Icon(Icons.Default.Add, "Añadir tarea") }
        }
        if (tasks.isEmpty()) ArtHint(com.iatualcance.alfred.R.drawable.art_notas, "Sin tareas. Escribe una arriba o dile al asistente «recuérdame…».")
        else LazyColumn(contentPadding = PaddingValues(8.dp)) {
            items(tasks.sortedWith(compareBy<Task> { it.done }.thenBy { it.due ?: "9999" }.thenByDescending { it.createdAt }), key = { it.id }) { t ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(t.done, { sv.store.upsertTask(t.copy(done = it)) })
                    Column(Modifier.weight(1f)) {
                        Text(t.title, textDecoration = if (t.done) TextDecoration.LineThrough else null,
                            color = if (t.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                        val hour = t.remindAt?.let { java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime().toString().take(5) }
                        val meta = listOfNotNull(t.due?.let { (if (it == today) "Hoy" else it) + (hour?.let { h -> " $h" } ?: "") }, t.priority.takeIf { it != "media" }?.let { "Prioridad $it" })
                        if (meta.isNotEmpty()) Text(meta.joinToString(" · "), style = MaterialTheme.typography.labelSmall,
                            color = if (!t.done && t.due != null && t.due < today) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { sv.store.deleteTask(t.id) }) { Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
    if (pickTime) {
        val st = androidx.compose.material3.rememberTimePickerState(java.time.LocalTime.now().plusHours(1).hour, 0, true)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            confirmButton = { TextButton(onClick = { time = "%02d:%02d".format(st.hour, st.minute); pickTime = false }) { Text("Aceptar") } },
            dismissButton = { TextButton(onClick = { time = null; pickTime = false }) { Text("Sin hora") } },
            text = { androidx.compose.material3.TimePicker(st) },
        )
    }
    if (pickDue) {
        val st = rememberDatePickerState(initialSelectedDateMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickDue = false },
            confirmButton = { TextButton(onClick = { due = st.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }; pickDue = false }) { Text("Aceptar") } },
            dismissButton = { TextButton(onClick = { due = null; pickDue = false }) { Text("Sin fecha") } },
        ) { DatePicker(st) }
    }
}

// =============================================================== Plantillas
@Composable
private fun TemplatesList() {
    val sv = LocalServices.current
    val ui = LocalUi.current
    val ctx = LocalContext.current
    val templates by sv.store.templates.collectAsStateCompat()
    var editing by remember { mutableStateOf<Template?>(null) }

    Box(Modifier.fillMaxSize()) {
        if (templates.isEmpty()) ArtHint(com.iatualcance.alfred.R.drawable.art_notas, "Las plantillas son textos que repites a menudo (respuestas, presupuestos, firmas).\nCrea una con + y úsala en correos con un toque.")
        else LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 88.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(templates, key = { it.id }) { t ->
                Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = PxShape, modifier = Modifier.pxBorder().fillMaxWidth().clickable { editing = t }) {
                    Column(Modifier.padding(12.dp)) {
                        Text(t.name, fontWeight = FontWeight.SemiBold)
                        Text(t.content, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row {
                            TextButton(onClick = { ctx.copyText(t.content); ui.say("Copiada") }) { Text("Copiar") }
                            TextButton(onClick = { ui.composeMail(MailDraft(body = t.content)) }) { Text("Usar en un correo") }
                        }
                    }
                }
            }
        }
        FloatingActionButton(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White, shape = PxShape, onClick = { editing = Template(name = "", content = "") }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Default.Add, "Nueva plantilla") }
    }
    editing?.let { t ->
        var name by remember(t.id) { mutableStateOf(t.name) }
        var body by remember(t.id) { mutableStateOf(t.content) }
        val exists = templates.any { it.id == t.id }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (exists) "Editar plantilla" else "Nueva plantilla") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(body, { body = it }, label = { Text("Texto") }, minLines = 5, maxLines = 12, modifier = Modifier.fillMaxWidth())
                    if (exists) TextButton(onClick = { sv.store.deleteTemplate(t.id); editing = null }) { Text("Eliminar plantilla", color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { TextButton(enabled = name.isNotBlank() && body.isNotBlank(), onClick = { sv.store.upsertTemplate(t.copy(name = name.trim(), content = body)); editing = null }) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancelar") } },
        )
    }
}
