package com.iatualcance.alfred.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.TextButton
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.iatualcance.alfred.MailFile
import com.iatualcance.alfred.google.MailAttachment
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iatualcance.alfred.Util
import com.iatualcance.alfred.data.Note
import com.iatualcance.alfred.google.Mail
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val filters = listOf("Recibidos" to "in:inbox", "Sin leer" to "in:inbox is:unread", "Enviados" to "in:sent", "Borradores" to "in:drafts")

private fun mailDate(ms: Long): String {
    val z = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
    return z.format(DateTimeFormatter.ofPattern(if (z.toLocalDate() == LocalDate.now()) "HH:mm" else "d MMM", Util.ES))
}

@Composable
private fun MailInner() = GoogleGate("tu correo", com.iatualcance.alfred.R.drawable.art_correo) {
    val sv = LocalServices.current
    val ui = LocalUi.current
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var search by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var mails by remember { mutableStateOf<List<Mail>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(filter, query, reload) {
        mails = null; error = null
        try { mails = sv.google.listMail(listOf(filters[filter].second, query).filter { it.isNotBlank() }.joinToString(" "), 30) }
        catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { error = e.message ?: e.toString() }
    }

    val draft = ui.compose
    when {
        draft != null -> ComposeMail(draft, onClose = { ui.compose = null }, onSent = { ui.compose = null; reload++ })
        ui.mailOpenId != null -> ReadMail(ui.mailOpenId!!, onClose = { ui.mailOpenId = null }, onChanged = { reload++ })
        else -> Box(Modifier.fillMaxSize()) {
            Column {
                PixelBar(title = { Text("CORREO Y CONTACTOS") })
                MailTabs()
                Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(search, { search = it }, Modifier.weight(1f), placeholder = { Text("Buscar en el correo") }, singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null) }, shape = PxShape,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { query = search.trim() }))
                    IconButton(onClick = { query = search.trim(); reload++ }) { Icon(Icons.Default.Refresh, "Actualizar") }
                }
                Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    filters.forEachIndexed { i, f -> FilterChip(filter == i, { filter = i }, { Text(f.first, maxLines = 1) }) }
                }
                val list = mails
                when {
                    error != null -> ErrorBox(error!!) { reload++ }
                    list == null -> Loading()
                    list.isEmpty() -> ArtHint(com.iatualcance.alfred.R.drawable.art_correo, "No hay correos.")
                    else -> LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                        items(list, key = { it.id }) { m ->
                            Column(Modifier.fillMaxWidth().clickable { ui.mailOpenId = m.id }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                val w = if (m.unread) FontWeight.Bold else FontWeight.Normal
                                Row {
                                    Text(if (filter >= 2) "Para: " + Util.splitAddress(m.to.substringBefore(',')).first else Util.splitAddress(m.from).first,
                                        Modifier.weight(1f), fontWeight = w, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(mailDate(m.date), style = MaterialTheme.typography.labelSmall, color = if (m.unread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(m.subject, fontWeight = w, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                                Text(m.snippet, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
            FloatingActionButton(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White, shape = PxShape, onClick = { ui.compose = MailDraft() }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Default.Edit, "Redactar") }
        }
    }
}

@Composable
private fun ReadMail(id: String, onClose: () -> Unit, onChanged: () -> Unit) {
    val sv = LocalServices.current
    val ui = LocalUi.current
    var mail by remember(id) { mutableStateOf<Mail?>(null) }
    var error by remember(id) { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var askTrash by remember { mutableStateOf(false) }
    var chosen by remember { mutableStateOf<MailAttachment?>(null) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    BackHandler(onBack = onClose)

    LaunchedEffect(id, retry) {
        error = null
        try {
            val m = sv.google.readMail(id)
            mail = m
            if (m.unread) { runCatching { sv.google.markRead(id) }; onChanged() }
        } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { error = e.message ?: e.toString() }
    }

    Column(Modifier.fillMaxSize()) {
        val m = mail
        PixelBar(
            title = {}, navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
            actions = {
                if (m != null) {
                    IconButton(onClick = {
                        val n = sv.store.upsertNote(Note(title = m.subject, content = "De: ${m.from}\n\n${m.body}", tags = listOf("correo")))
                        ui.openNote(n.id)
                    }) { Icon(Icons.AutoMirrored.Filled.NoteAdd, "Guardar como nota") }
                    IconButton(onClick = { askTrash = true }) { Icon(Icons.Default.Delete, "Papelera") }
                    IconButton(onClick = {
                        val quoted = m.body.lines().take(60).joinToString("\n") { "> $it" }
                        ui.compose = MailDraft(Util.splitAddress(m.from).second, if (m.subject.startsWith("Re:", true)) m.subject else "Re: ${m.subject}", "\n\n$quoted")
                        replyTo = m
                    }) { Icon(Icons.AutoMirrored.Filled.Reply, "Responder") }
                }
            },
        )
        when {
            error != null -> ErrorBox(error!!) { retry++ }
            m == null -> Loading()
            else -> Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                SelectionContainer {
                    Column {
                        Text(m.subject, style = MaterialTheme.typography.headlineSmall)
                        VSpace(6)
                        Text("De: ${m.from}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Para: ${m.to}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        VSpace(12)
                        Text(m.body.ifBlank { m.snippet }, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                if (m.attachments.isNotEmpty()) {
                    VSpace(18)
                    Text("Adjuntos (${m.attachments.size})", style = MaterialTheme.typography.titleSmall)
                    VSpace(6)
                    m.attachments.forEach { a ->
                        androidx.compose.material3.Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer, shape = PxShape,
                            modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth().pxBorder().clickable { chosen = a },
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AttachFile, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(a.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(sizeText(a.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (askTrash) Confirm("Mover a la papelera", "El correo se moverá a la papelera de Gmail.", "Mover", { askTrash = false }) {
        ui.run("Moviendo a la papelera…") { sv.google.trashMail(id); onChanged(); onClose() }
    }
    val msg = mail
    chosen?.let { a ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { chosen = null }, title = { Text(a.name, style = MaterialTheme.typography.titleMedium, maxLines = 2) },
            text = {
                Column {
                    TextButton(onClick = { chosen = null; ui.run("Descargando…") { val f = sv.mailAttachmentFile(id, a); if (!ctx.viewFile(f, a.mime.ifBlank { "*/*" })) ctx.shareFile(f, a.mime.ifBlank { "*/*" }) } }) { Text("Abrir o compartir") }
                    TextButton(onClick = { chosen = null; ui.run("Guardando en una nota…") { ui.openNote(sv.saveAttachmentToNote(id, a, msg?.subject.orEmpty(), msg?.from.orEmpty()).id) } }) { Text("Guardar en una nota") }
                    TextButton(onClick = { chosen = null; ui.run("Subiendo a Drive…") { sv.saveAttachmentToDrive(id, a); ui.say("Guardado en Drive, carpeta Alfred › Adjuntos") } }) { Text("Guardar en Drive") }
                }
            },
            confirmButton = { TextButton(onClick = { chosen = null }) { Text("CERRAR") } },
        )
    }
}

private fun sizeText(b: Long) = when { b >= 1_048_576 -> "%.1f MB".format(b / 1_048_576.0); b >= 1024 -> "${b / 1024} KB"; else -> "$b B" }

/** Correo al que se está respondiendo (para mantener el hilo). */
private var replyTo: Mail? = null

@Composable
private fun ComposeMail(initial: MailDraft, onClose: () -> Unit, onSent: () -> Unit) {
    val sv = LocalServices.current
    val ui = LocalUi.current
    val templates by sv.store.templates.collectAsStateCompat()
    var to by rememberSaveable { mutableStateOf(initial.to) }
    var subject by rememberSaveable { mutableStateOf(initial.subject) }
    var body by rememberSaveable { mutableStateOf(initial.body) }
    var menu by remember { mutableStateOf(false) }
    var files by remember { mutableStateOf(listOf<MailFile>()) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val reply = remember { replyTo.also { replyTo = null } }
    BackHandler(onBack = onClose)
    val pick = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) ui.run("Adjuntando…") {
            val added = withContext(Dispatchers.IO) {
                uris.map { uri ->
                    val cr = ctx.contentResolver
                    val name = cr.query(uri, null, null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(c.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME)) else null } ?: "adjunto"
                    MailFile(name, cr.getType(uri) ?: "application/octet-stream", cr.openInputStream(uri)!!.use { it.readBytes() })
                }
            }
            if ((files + added).sumOf { it.bytes.size.toLong() } > 20L * 1024 * 1024) throw Exception("Los adjuntos no pueden pasar de 20 MB en total.")
            files = files + added
        }
    }

    Column(Modifier.fillMaxSize()) {
        PixelBar(
            title = { Text(if (reply != null) "Responder" else "Redactar") },
            navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
            actions = {
                if (templates.isNotEmpty()) Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.Description, "Insertar plantilla") }
                    DropdownMenu(menu, { menu = false }) {
                        templates.forEach { t -> DropdownMenuItem({ Text(t.name) }, { menu = false; body = if (body.isBlank()) t.content else t.content + "\n" + body }) }
                    }
                }
                IconButton(onClick = { pick.launch("*/*") }) { Icon(Icons.Default.AttachFile, "Adjuntar archivo") }
                IconButton(onClick = { ui.run("Guardando borrador…") { sv.google.createDraft(to, subject, body, files); ui.say("Borrador guardado en Gmail"); onSent() } }) { Icon(Icons.Default.Save, "Guardar borrador") }
                IconButton(onClick = {
                    if ("@" !in to) ui.say("Escriba la dirección del destinatario")
                    else ui.run("Enviando…") { sv.google.sendMail(to, subject, body, reply?.threadId, reply?.messageId, files); ui.say("Correo enviado"); onSent() }
                }) { Icon(Icons.AutoMirrored.Filled.Send, "Enviar") }
            },
        )
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(to, { to = it }, label = { Text("Para") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            OutlinedTextField(subject, { subject = it }, label = { Text("Asunto") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(body, { body = it }, label = { Text("Mensaje") }, minLines = 10, modifier = Modifier.fillMaxWidth())
            if (files.isNotEmpty()) androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                files.forEach { f ->
                    androidx.compose.material3.InputChip(selected = false, onClick = { files = files - f }, label = { Text(f.name + " · " + sizeText(f.bytes.size.toLong()), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        trailingIcon = { Icon(Icons.Default.Close, "Quitar", Modifier.size(16.dp)) })
                }
            }
            VSpace(12)
        }
    }
}


/** Correo y Contactos comparten pestaña: dos subpestañas. */
@Composable
fun MailScreen() {
    val ui = LocalUi.current
    if (ui.mailSub == 1 && ui.compose == null && ui.mailOpenId == null) Column(Modifier.fillMaxSize()) {
        PixelBar(title = { Text("CORREO Y CONTACTOS") })
        MailTabs()
        ContactsScreen()
    } else MailInner()
}

@Composable
private fun MailTabs() {
    val ui = LocalUi.current
    androidx.compose.material3.TabRow(selectedTabIndex = ui.mailSub, containerColor = MaterialTheme.colorScheme.background) {
        listOf("Correo", "Contactos").forEachIndexed { i, t ->
            androidx.compose.material3.Tab(ui.mailSub == i, { ui.mailSub = i }, text = { Text(t.uppercase(), fontWeight = FontWeight.Bold) })
        }
    }
}
