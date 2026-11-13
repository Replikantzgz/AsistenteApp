package com.iatualcance.alfred.ui

import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iatualcance.alfred.R
import com.iatualcance.alfred.google.ConsentRequired
import com.iatualcance.alfred.google.DriveFile
import com.iatualcance.alfred.google.DrivePerm
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private data class Crumb(val id: String, val name: String)

private fun iconFor(f: DriveFile): Pair<ImageVector, Color> = when {
    f.isFolder -> (if (f.shared) Icons.Default.FolderShared else Icons.Default.Folder) to Color(0xFFF2B100)
    f.isGoogleDoc -> Icons.Default.Description to Blue
    f.isSheet -> Icons.Default.TableChart to Color(0xFF1FA463)
    f.mime == DriveFile.SLIDES -> Icons.Default.Slideshow to Color(0xFFF2A100)
    f.mime == "application/pdf" -> Icons.Default.PictureAsPdf to Color(0xFFE63B2E)
    f.mime.startsWith("image/") -> Icons.Default.Image to Color(0xFF8E5BD6)
    else -> Icons.AutoMirrored.Filled.InsertDriveFile to Color(0xFF6B6F85)
}

/** Pestaña Drive: explorar tu nube, buscar, subir, crear y compartir proyectos con otra persona. */
@Composable
fun DriveTab() {
    var reload by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        PixelBar(title = { Text("DRIVE") }, actions = { IconButton(onClick = { reload++ }) { Icon(Icons.Default.Refresh, "Actualizar") } })
        GoogleGate("tu Drive", R.drawable.art_drive) { DriveBrowser(reload) { reload++ } }
    }
}

@Composable
private fun DriveBrowser(reloadKey: Int, requestReload: () -> Unit) {
    val sv = LocalServices.current
    val ui = LocalUi.current
    val ctx = LocalContext.current
    val connect = rememberGoogleConnect(full = true) { requestReload() }

    var mode by rememberSaveable { mutableIntStateOf(0) }              // 0 Mi unidad · 1 Compartido conmigo · 2 Proyectos
    var stack by remember { mutableStateOf(listOf<Crumb>()) }
    var query by rememberSaveable { mutableStateOf("") }
    var files by remember { mutableStateOf<List<DriveFile>?>(null) }
    var members by remember { mutableStateOf<Map<String, List<DrivePerm>>>(emptyMap()) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    var local by remember { mutableIntStateOf(0) }
    var projectsRoot by remember { mutableStateOf<String?>(null) }

    var shareFor by remember { mutableStateOf<DriveFile?>(null) }
    var newFolder by remember { mutableStateOf(false) }
    var newProject by remember { mutableStateOf(false) }
    var newItem by remember { mutableStateOf<String?>(null) }            // mime del documento a crear
    var rename by remember { mutableStateOf<DriveFile?>(null) }
    var delete by remember { mutableStateOf<DriveFile?>(null) }
    var fabMenu by remember { mutableStateOf(false) }

    fun reloadNow() { local++ }
    BackHandler(enabled = stack.isNotEmpty()) { stack = stack.dropLast(1) }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) ui.run("Subiendo a Drive…") {
            val parent = stack.lastOrNull()?.id ?: if (mode == 2) (projectsRoot ?: sv.google.folder("Proyectos")) else "root"
            uris.forEach { uri ->
                val cr = ctx.contentResolver
                val name = cr.query(uri, null, null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(c.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null } ?: "archivo"
                val bytes = withContext(Dispatchers.IO) { cr.openInputStream(uri)!!.use { it.readBytes() } }
                if (bytes.size > 25 * 1024 * 1024) throw Exception("«$name» pesa más de 25 MB. Súbelo desde la app de Drive.")
                sv.google.upload(bytes, name, cr.getType(uri) ?: "application/octet-stream", parent, full = true)
            }
            ui.say(if (uris.size == 1) "Archivo subido" else "${uris.size} archivos subidos"); reloadNow()
        }
    }

    LaunchedEffect(mode, stack, query, reloadKey, local) {
        files = null; error = null
        if (query.isNotBlank()) delay(350)
        try {
            val list = when {
                query.isNotBlank() -> sv.google.listDrive(parent = null, query = query)
                mode == 1 && stack.isEmpty() -> sv.google.listDrive(sharedWithMe = true)
                mode == 2 && stack.isEmpty() -> sv.google.listDrive(parent = (projectsRoot ?: sv.google.folder("Proyectos").also { projectsRoot = it }))
                else -> sv.google.listDrive(parent = stack.lastOrNull()?.id ?: "root")
            }
            files = list
            if (mode == 2 && stack.isEmpty() && query.isBlank()) {
                members = coroutineScope { list.filter { it.isFolder }.take(20).map { f -> async { f.id to runCatching { sv.google.permissions(f.id) }.getOrDefault(emptyList()) } }.map { it.await() } }.toMap()
            }
        } catch (e: CancellationException) { throw e } catch (e: Exception) { error = e }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Mi unidad", "Compartido conmigo", "Proyectos").forEachIndexed { i, t ->
                FilterChip(mode == i, { mode = i; stack = emptyList(); query = "" }, { Text(t, maxLines = 1) })
            }
        }
        OutlinedTextField(
            query, { query = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), singleLine = true, shape = PxShape,
            placeholder = { Text("Buscar en todo tu Drive") }, leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Borrar búsqueda") } },
        )
        if (stack.isNotEmpty() && query.isBlank()) Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { stack = stack.dropLast(1) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Subir un nivel") }
            Text(listOf(listOf("Mi unidad", "Compartido", "Proyectos")[mode]).plus(stack.map { it.name }).joinToString("  ›  "), style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            val list = files
            val err = error
            when {
                err != null -> if (err is ConsentRequired || "permiso de drive" in (err.message ?: "").lowercase() || "permisos" in (err.message ?: "").lowercase()) Centered {
                    Art(R.drawable.art_drive, 110.dp)
                    Text("Falta un permiso de Google", style = MaterialTheme.typography.titleSmall)
                    Hint("Para ver y compartir todo tu Drive, Alfred necesita el permiso de Drive completo. Concédelo una vez.")
                    AppButton(onClick = connect) { Text("Conceder permiso") }
                } else ErrorBox(err.message ?: err.toString()) { reloadNow() }
                list == null -> Loading()
                list.isEmpty() -> ArtHint(R.drawable.art_drive, when {
                    query.isNotBlank() -> "Sin resultados para «$query»."
                    mode == 1 -> "Nadie ha compartido nada contigo todavía."
                    mode == 2 -> "Aún no tienes proyectos.\nCrea uno con + y compártelo con otra persona para trabajar juntos."
                    else -> "Carpeta vacía.\nSube archivos o crea un documento con +."
                })
                else -> LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(list, key = { it.id }) { f ->
                        DriveRow(f, members[f.id], onOpen = { if (f.isFolder) { query = ""; stack = stack + Crumb(f.id, f.name) } else f.link?.let { ctx.openUrl(it) } },
                            onShare = { shareFor = f }, onRename = { rename = f }, onDelete = { delete = f }, onWeb = { f.link?.let { ctx.openUrl(it) } })
                    }
                }
            }
            if (err == null) Box(Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
                FloatingActionButton(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, shape = PxShape, onClick = { fabMenu = true }) { Icon(Icons.Default.Add, "Nuevo") }
                DropdownMenu(fabMenu, { fabMenu = false }) {
                    if (mode == 2 && stack.isEmpty()) DropdownMenuItem({ Text("Nuevo proyecto compartido") }, { fabMenu = false; newProject = true }, leadingIcon = { Icon(Icons.Default.Group, null) })
                    DropdownMenuItem({ Text("Subir archivo") }, { fabMenu = false; pick.launch("*/*") }, leadingIcon = { Icon(Icons.Default.Add, null) })
                    DropdownMenuItem({ Text("Nueva carpeta") }, { fabMenu = false; newFolder = true }, leadingIcon = { Icon(Icons.Default.Folder, null) })
                    DropdownMenuItem({ Text("Documento de Google") }, { fabMenu = false; newItem = DriveFile.DOC }, leadingIcon = { Icon(Icons.Default.Description, null) })
                    DropdownMenuItem({ Text("Hoja de cálculo de Google") }, { fabMenu = false; newItem = DriveFile.SHEET }, leadingIcon = { Icon(Icons.Default.TableChart, null) })
                }
            }
        }
    }

    // ---- Diálogos --------------------------------------------------------------------------------------------
    val parentForNew = stack.lastOrNull()?.id ?: if (mode == 2) projectsRoot else null
    if (newFolder) NameDialog("Nueva carpeta", "Nombre", "", "Crear", { newFolder = false }) { name ->
        ui.run("Creando carpeta…") { sv.google.createFolder(name, parentForNew); reloadNow() }
    }
    newItem?.let { mime ->
        NameDialog(if (mime == DriveFile.DOC) "Nuevo documento" else "Nueva hoja de cálculo", "Nombre", "", "Crear", { newItem = null }) { name ->
            ui.run("Creando…") { val f = sv.google.createDriveItem(name, mime, parentForNew); reloadNow(); f.link?.let { ctx.openUrl(it) } }
        }
    }
    rename?.let { f -> NameDialog("Renombrar", "Nombre", f.name, "Guardar", { rename = null }) { name -> ui.run("Renombrando…") { sv.google.rename(f.id, name); reloadNow() } } }
    delete?.let { f -> Confirm("Mover a la papelera", "«${f.name}» irá a la papelera de Drive. Si lo compartiste, las demás personas dejarán de verlo.", "Mover", { delete = null }) { ui.run("Moviendo a la papelera…") { sv.google.trash(f.id); reloadNow() } } }
    shareFor?.let { f -> ShareDialog(f) { shareFor = null; reloadNow() } }
    if (newProject) NewProjectDialog({ newProject = false }) { name, invites ->
        ui.run("Creando el proyecto…") {
            val root = projectsRoot ?: sv.google.folder("Proyectos").also { projectsRoot = it }
            val project = sv.google.createFolder(name, root)
            sv.google.createDocWithText("Notas del proyecto", "$name\n\nObjetivo:\n\nTareas:\n- \n\nNotas:\n", project.id)
            val failed = mutableListOf<String>()
            invites.forEach { mail -> runCatching { sv.google.share(project.id, mail, "writer", "Te invito al proyecto «$name» en Drive.") }.onFailure { failed += mail } }
            mode = 2; stack = listOf(Crumb(project.id, name)); reloadNow()
            ui.say(if (failed.isEmpty()) "Proyecto creado" + if (invites.isNotEmpty()) " y compartido con ${invites.size}" else "" else "Proyecto creado, pero no se pudo compartir con: ${failed.joinToString()}")
        }
    }
}

@Composable
private fun DriveRow(
    f: DriveFile, people: List<DrivePerm>?, onOpen: () -> Unit, onShare: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit, onWeb: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val (icon, tint) = iconFor(f)
    androidx.compose.material3.Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = PxShape, modifier = Modifier.fillMaxWidth().pxBorder().clickable(onClick = onOpen)) {
        Row(Modifier.padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(f.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val others = people?.filter { it.role != "owner" }.orEmpty()
                val sub = buildList {
                    f.modified?.take(10)?.let { add(it) }
                    if (!f.ownedByMe && f.owner != null) add("de ${f.owner}")
                    if (others.isNotEmpty()) add("con " + others.joinToString { it.name.substringBefore('@') }) else if (f.shared && f.ownedByMe) add("compartido")
                }.joinToString(" · ")
                if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Más") }
                DropdownMenu(menu, { menu = false }) {
                    if (f.link != null) DropdownMenuItem({ Text(if (f.isFolder) "Abrir en Drive" else "Abrir") }, { menu = false; onWeb() })
                    DropdownMenuItem({ Text("Compartir…") }, { menu = false; onShare() })
                    DropdownMenuItem({ Text("Renombrar") }, { menu = false; onRename() })
                    DropdownMenuItem({ Text("Mover a la papelera", color = MaterialTheme.colorScheme.error) }, { menu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun NameDialog(title: String, label: String, initial: String, ok: String, onDismiss: () -> Unit, onOk: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = { OutlinedTextField(text, { text = it }, label = { Text(label) }, singleLine = true, shape = PxShape) },
        confirmButton = { TextButton(enabled = text.isNotBlank(), onClick = { onDismiss(); onOk(text.trim()) }) { Text(ok.uppercase()) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } },
    )
}

@Composable
private fun NewProjectDialog(onDismiss: () -> Unit, onOk: (String, List<String>) -> Unit) {
    var name by remember { mutableStateOf("") }
    var emails by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Nuevo proyecto compartido", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Hint("Se crea una carpeta en Drive con un documento de notas. Quien invites podrá editar todo lo que haya dentro, a la vez que tú.")
                OutlinedTextField(name, { name = it }, label = { Text("Nombre del proyecto") }, singleLine = true, shape = PxShape)
                OutlinedTextField(emails, { emails = it }, label = { Text("Invitar (correos, separados por comas)") }, shape = PxShape, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onDismiss(); onOk(name.trim(), emails.split(',', ';', ' ', '\n').map { it.trim() }.filter { "@" in it }) }) { Text("CREAR") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } },
    )
}

@Composable
private fun ShareDialog(f: DriveFile, onClose: () -> Unit) {
    val sv = LocalServices.current
    val ui = LocalUi.current
    var perms by remember { mutableStateOf<List<DrivePerm>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var editor by remember { mutableStateOf(true) }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(f.id, tick) {
        try { perms = sv.google.permissions(f.id) } catch (e: CancellationException) { throw e } catch (e: Exception) { error = e.message }
    }
    AlertDialog(
        onDismissRequest = onClose, title = { Text("Compartir «${f.name}»", style = MaterialTheme.typography.titleMedium, maxLines = 2) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(email, { email = it }, label = { Text("Correo de la persona") }, singleLine = true, shape = PxShape, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(editor, { editor = true }, { Text("Puede editar") }); FilterChip(!editor, { editor = false }, { Text("Solo ver") })
                }
                AppButton(onClick = {
                    val mail = email.trim(); email = ""
                    ui.run("Compartiendo…") { sv.google.share(f.id, mail, if (editor) "writer" else "reader"); tick++; ui.say("Compartido con $mail") }
                }, enabled = "@" in email, modifier = Modifier.fillMaxWidth()) { Text("Compartir") }
                Text("Personas con acceso", style = MaterialTheme.typography.titleSmall)
                when {
                    error != null -> Hint(error!!)
                    perms == null -> Hint("Cargando…")
                    else -> perms!!.forEach { p ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p.name.ifBlank { p.email }, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(when (p.role) { "owner" -> "Propietario"; "writer" -> "Editor"; "commenter" -> "Comentarista"; else -> "Lector" } + if (p.type == "anyone") " · cualquiera con el enlace" else "",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (p.role != "owner") IconButton(onClick = { ui.run("Quitando acceso…") { sv.google.unshare(f.id, p.id); tick++ } }) { Icon(Icons.Default.Close, "Quitar acceso") }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("LISTO") } },
    )
}
