package com.iatualcance.alfred.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.iatualcance.alfred.BuildConfig
import com.iatualcance.alfred.billing.BillingConfig
import com.iatualcance.alfred.google.Contact
import com.iatualcance.alfred.google.DriveFile
import kotlinx.coroutines.launch

/** Drive y Ajustes, a pantalla completa por encima de las pestañas. */
@Composable
fun OverlayScreen(which: String) {
    val ui = LocalUi.current
    BackHandler { ui.overlay = null }
    Column(Modifier.fillMaxSize()) {
        PixelBar(
            title = { Text(when (which) { "drive" -> "DRIVE"; "premium" -> "PREMIUM"; else -> "AJUSTES" }) },
            navigationIcon = { IconButton(onClick = { ui.overlay = null }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White) } },
        )
        when (which) { "drive" -> DriveScreen(); "premium" -> PremiumScreen(); else -> SettingsScreen() }
    }
}

/** Pestaña de contactos. */
@Composable
fun ContactsTab() {
    Column(Modifier.fillMaxSize()) {
        PixelBar(title = { Text("CONTACTOS") })
        ContactsScreen()
    }
}

// =============================================================== Contactos
@Composable
fun ContactsScreen() = GoogleGate("tus contactos", com.iatualcance.alfred.R.drawable.art_contactos) {
    val sv = LocalServices.current
    val ui = LocalUi.current
    val ctx = LocalContext.current
    var all by remember { mutableStateOf<List<Contact>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var q by rememberSaveable { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Contact?>(null) }

    LaunchedEffect(reload) {
        error = null
        try { all = sv.google.listContacts() } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { error = e.message ?: e.toString() }
    }

    Box(Modifier.fillMaxSize()) {
        Column {
            OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth().padding(horizontal = 12.dp), placeholder = { Text("Buscar contactos") }, singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) }, shape = PxShape)
            val list = all
            when {
                error != null -> ErrorBox(error!!) { reload++ }
                list == null -> Loading()
                else -> {
                    val shown = list.filter { c -> q.isBlank() || q.lowercase().let { it in c.name.lowercase() || c.emails.any { e -> it in e.lowercase() } || c.phones.any { p -> it in p } } }
                    if (shown.isEmpty()) ArtHint(com.iatualcance.alfred.R.drawable.art_contactos, "Sin contactos.")
                    else LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                        items(shown, key = { it.resource }) { c ->
                            Column(Modifier.fillMaxWidth().clickable { selected = c }.padding(horizontal = 20.dp, vertical = 10.dp)) {
                                Text(c.name.ifBlank { c.emails.firstOrNull() ?: c.phones.firstOrNull().orEmpty() }, fontWeight = FontWeight.Medium)
                                Text((c.emails + c.phones).joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { adding = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Default.Add, "Nuevo contacto") }
    }

    selected?.let { c ->
        AlertDialog(
            onDismissRequest = { selected = null }, title = { Text(c.name.ifBlank { "Contacto" }) },
            text = {
                Column {
                    c.emails.forEach { e ->
                        TextButton(onClick = { selected = null; ui.composeMail(MailDraft(to = e)) }) { Icon(Icons.Default.Mail, null); Spacer(Modifier.width(8.dp)); Text(e) }
                    }
                    c.phones.forEach { p ->
                        TextButton(onClick = { runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(p)))) } }) { Icon(Icons.Default.Call, null); Spacer(Modifier.width(8.dp)); Text(p) }
                    }
                    TextButton(onClick = { selected = null; ui.run("Eliminando…") { sv.google.deleteContact(c.resource); reload++ } }) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error); Spacer(Modifier.width(8.dp)); Text("Eliminar contacto", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Cerrar") } },
        )
    }
    if (adding) {
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { adding = false }, title = { Text("Nuevo contacto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true)
                    OutlinedTextField(email, { email = it }, label = { Text("Correo") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                    OutlinedTextField(phone, { phone = it }, label = { Text("Teléfono") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                }
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    adding = false
                    ui.run("Guardando contacto…") { sv.google.createContact(name.trim(), email.trim(), phone.trim()); reload++ }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Cancelar") } },
        )
    }
}

// =============================================================== Drive
@Composable
private fun DriveScreen() = GoogleGate("tu carpeta de Drive", com.iatualcance.alfred.R.drawable.art_notas) {
    val sv = LocalServices.current
    val ui = LocalUi.current
    val ctx = LocalContext.current
    var path by remember { mutableStateOf(listOf<DriveFile>()) }   // subcarpetas abiertas
    var files by remember { mutableStateOf<List<DriveFile>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var toDelete by remember { mutableStateOf<DriveFile?>(null) }
    if (path.isNotEmpty()) BackHandler { path = path.dropLast(1) }

    LaunchedEffect(path, reload) {
        files = null; error = null
        try { files = sv.google.listFiles(path.lastOrNull()?.id ?: sv.google.folder(null)) }
        catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { error = e.message ?: e.toString() }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Alfred" + path.joinToString("") { " / " + it.name }, Modifier.weight(1f), color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = { ui.run("Haciendo copia de seguridad…") { sv.backupToDrive(); ui.say("Copia de notas, tareas y plantillas guardada en Drive"); reload++ } }) { Icon(Icons.Default.Backup, "Copia de seguridad") }
            IconButton(onClick = { reload++ }) { Icon(Icons.Default.Refresh, "Actualizar") }
        }
        val list = files
        when {
            error != null -> ErrorBox(error!!) { reload++ }
            list == null -> Loading()
            list.isEmpty() -> Centered { Hint("Carpeta vacía.\nAquí aparecen las notas, escaneos y facturas que Alfred guarda en su Drive.") }
            else -> LazyColumn {
                items(list, key = { it.id }) { f ->
                    Row(Modifier.fillMaxWidth().clickable { if (f.isFolder) path = path + f else f.link?.let { ctx.openUrl(it) } }.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (f.isFolder) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile, null,
                            tint = if (f.isFolder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(f.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            f.modified?.let { Text(it.take(10), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        if (!f.isFolder) IconButton(onClick = { toDelete = f }) { Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        else f.link?.let { l -> IconButton(onClick = { ctx.openUrl(l) }) { Icon(Icons.AutoMirrored.Filled.OpenInNew, "Abrir en Drive", tint = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    }
                }
            }
        }
    }
    toDelete?.let { f ->
        Confirm("Eliminar de Drive", "«${f.name}» se eliminará de su Google Drive.", "Eliminar", { toDelete = null }) {
            ui.run("Eliminando…") { sv.google.deleteFile(f.id); reload++ }
        }
    }
}

// =============================================================== Ajustes
@Composable
private fun SettingsScreen() {
    val sv = LocalServices.current
    val ui = LocalUi.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val account by sv.settings.account.collectAsStateCompat()
    val savedKey by sv.settings.apiKey.collectAsStateCompat()
    val model by sv.settings.model.collectAsStateCompat()
    val driveAuto by sv.settings.driveAuto.collectAsStateCompat()
    var key by remember { mutableStateOf(savedKey) }
    var show by remember { mutableStateOf(false) }
    var models by remember { mutableStateOf<List<String>>(emptyList()) }
    var modelMenu by remember { mutableStateOf(false) }
    var askWipe by remember { mutableStateOf(false) }
    val connect = rememberGoogleConnect()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Section("Cuenta de Google") {
            val a = account
            if (a == null) {
                Hint("Conecte su cuenta para usar Calendar, Gmail, Contactos y Drive.")
                Button(onClick = connect) { Text("Conectar con Google") }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (a.photo != null) { AsyncImage(a.photo, null, Modifier.size(44.dp).clip(PxShape)); Spacer(Modifier.width(12.dp)) }
                    Column(Modifier.weight(1f)) { Text(a.name, fontWeight = FontWeight.Medium); Text(a.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = connect) { Text("Reconectar") }
                    OutlinedButton(onClick = { scope.launch { sv.disconnect(); ui.say("Cuenta desconectada") } }) { Text("Desconectar") }
                }
                TextButton(onClick = { ctx.openUrl("https://myaccount.google.com/connections") }) { Text("Gestionar permisos en Google") }
            }
        }
        Section("Inteligencia artificial (Gemini)") {
            OutlinedTextField(
                key, { key = it }, label = { Text("Clave de API de Gemini") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { IconButton(onClick = { show = !show }) { Icon(if (show) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Mostrar") } },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    sv.settings.setApiKey(key)
                    if (key.isBlank()) ui.say("Clave borrada") else ui.run("Comprobando la clave…") {
                        models = sv.gemini.listModels()
                        ui.say("Clave correcta. ${models.size} modelos disponibles.")
                    }
                }) { Text("Guardar y probar") }
                TextButton(onClick = { ctx.openUrl("https://aistudio.google.com/apikey") }) { Text("Obtener clave gratis") }
            }
            Box {
                OutlinedButton(onClick = {
                    if (models.isNotEmpty()) modelMenu = true
                    else if (savedKey.isBlank()) ui.say("Guarde primero la clave")
                    else ui.run("Cargando modelos…") { models = sv.gemini.listModels(); modelMenu = true }
                }) { Text("Modelo: $model") }
                DropdownMenu(modelMenu, { modelMenu = false }) {
                    models.forEach { m -> DropdownMenuItem({ Text(m) }, { sv.settings.setModel(m); modelMenu = false }) }
                }
            }
            Hint("La clave se guarda cifrada solo en este móvil y se usa únicamente para hablar con Gemini.")
        }
        if (BillingConfig.ENABLED) Section("Plan") {
            val lic by sv.settings.license.collectAsStateCompat()
            val data = sv.licenseData(lic)
            Text(if (data != null) "Premium activo" + (data.x?.let { " hasta $it" } ?: " · de por vida") else "Plan gratuito", fontWeight = FontWeight.Medium)
            OutlinedButton(onClick = { ui.overlay = "premium" }) { Text(if (data != null) "Ver mi plan" else "Ver Premium") }
        }
        Section("Actualizaciones") {
            val autoUpdate by sv.settings.autoUpdate.collectAsStateCompat()
            val upd by sv.updater.state.collectAsStateCompat()
            Text("Versión instalada: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Buscar e instalar al abrir")
                    Text("Si hay una versión nueva, se descarga y se instala sola.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(autoUpdate, { sv.settings.setAutoUpdate(it) })
            }
            OutlinedButton(enabled = upd !is com.iatualcance.alfred.update.UpdateState.Checking && upd !is com.iatualcance.alfred.update.UpdateState.Downloading, onClick = {
                scope.launch {
                    val info = sv.updater.check(install = true)
                    if (info == null && sv.updater.state.value !is com.iatualcance.alfred.update.UpdateState.Failed) ui.say("Ya tienes la última versión")
                    else if (info != null && sv.updater.canInstall()) ui.say("Actualizando a la ${info.versionName}…")
                }
            }) { Text(when (upd) { is com.iatualcance.alfred.update.UpdateState.Checking -> "Buscando…"; is com.iatualcance.alfred.update.UpdateState.Downloading -> "Descargando…"; else -> "Buscar actualizaciones ahora" }) }
        }
        Section("Notificaciones") {
            val notifyEvents by sv.settings.notifyEvents.collectAsStateCompat()
            val notifyTasks by sv.settings.notifyTasks.collectAsStateCompat()
            val lead by sv.settings.leadMin.collectAsStateCompat()
            var leadMenu by remember { mutableStateOf(false) }
            var allowed by remember { mutableStateOf(com.iatualcance.alfred.notify.Notif.canPost(ctx)) }
            androidx.lifecycle.compose.LifecycleResumeEffect(Unit) { allowed = com.iatualcance.alfred.notify.Notif.canPost(ctx); onPauseOrDispose {} }
            if (!allowed) {
                Hint("Las notificaciones están desactivadas para Alfred, así que no sonará ningún aviso.")
                Button(onClick = {
                    ctx.startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, ctx.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }) { Text("Permitir notificaciones") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Avisar de las citas")
                    Text("Notificación antes de cada evento de Google Calendar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(notifyEvents, { sv.settings.setNotifyEvents(it) })
            }
            if (notifyEvents) Box {
                OutlinedButton(onClick = { leadMenu = true }) { Text(if (lead == 0) "Avisar a la hora exacta" else "Avisar $lead min antes") }
                DropdownMenu(leadMenu, { leadMenu = false }) {
                    listOf(0, 5, 10, 15, 30, 60, 120).forEach { m -> DropdownMenuItem({ Text(if (m == 0) "A la hora exacta" else "$m minutos antes") }, { sv.settings.setLeadMin(m); leadMenu = false }) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Avisar de las tareas")
                    Text("A la hora que indique la tarea; si solo tiene fecha, a las 9:00.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(notifyTasks, { sv.settings.setNotifyTasks(it) })
            }
            OutlinedButton(onClick = {
                com.iatualcance.alfred.notify.Notif.show(ctx, com.iatualcance.alfred.notify.Reminder("test", System.currentTimeMillis(), "Alfred", "Así sonarán tus avisos.", "chat"))
                if (!allowed) ui.say("Activa primero las notificaciones")
            }) { Text("Enviar notificación de prueba") }
        }
        Section("Drive") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Subir escaneos automáticamente")
                    Text("Cada documento escaneado se guarda también en Drive, carpeta Alfred (Facturas o Escaneos).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(driveAuto, { sv.settings.setDriveAuto(it) })
            }
        }
        Section("Datos y privacidad") {
            Hint("Sus notas, tareas y plantillas se guardan en este móvil. Alfred solo envía a Google lo necesario para cada acción (Calendar, Gmail, Contactos, Drive) y a Gemini el texto, audio o imagen que usted le pida procesar. No hay servidores propios ni se comparten datos con terceros.")
            OutlinedButton(onClick = { askWipe = true }) { Text("Borrar todos los datos de la app", color = MaterialTheme.colorScheme.error) }
        }
        Text("Alfred ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (askWipe) Confirm("Borrar todo", "Se borrarán del móvil las notas, tareas, plantillas, el chat, la clave de Gemini y la conexión con Google. Lo guardado en Drive, Calendar y Gmail no se toca.", "Borrar todo", { askWipe = false }) {
        scope.launch { runCatching { sv.disconnect() }; sv.wipe(); key = ""; ui.say("Datos borrados") }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) =
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = PxShape, modifier = Modifier.pxBorder().fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }


// =============================================================== Premium (solo accesible con BillingConfig.ENABLED)
@Composable
private fun PremiumScreen() {
    val sv = LocalServices.current
    val ui = LocalUi.current
    val ctx = LocalContext.current
    val lic by sv.settings.license.collectAsStateCompat()
    val used by sv.settings.aiToday.collectAsStateCompat()
    val data = sv.licenseData(lic)
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = PxShape, modifier = Modifier.fillMaxWidth().pxBorder(Ink)) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Art(com.iatualcance.alfred.R.drawable.art_pikachu, 84.dp)
                Text(if (data != null) "PREMIUM ACTIVO" else "ALFRED PREMIUM", style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
                if (data != null) Text(data.x?.let { "Válido hasta $it" } ?: "De por vida · ${data.e}", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (data == null) {
            Section("Qué incluye") {
                listOf("Mensajes ilimitados con Alfred", "Transcripción de audios y lectura de facturas sin límite", "Todas las funciones futuras").forEach { Text("✔  $it") }
                Text("Plan gratuito: ${BillingConfig.FREE_AI_PER_DAY} usos de IA al día · hoy llevas $used.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Section("Elige tu plan") {
                PlanButton("Mensual", BillingConfig.PRICE_MONTHLY, BillingConfig.CHECKOUT_MONTHLY_URL)
                PlanButton("Anual", BillingConfig.PRICE_YEARLY, BillingConfig.CHECKOUT_YEARLY_URL)
                Hint("Tras pagar recibirás un código por correo. Pégalo aquí abajo.")
            }
        }
        Section(if (data != null) "Cambiar de código" else "Ya tengo un código") {
            OutlinedTextField(code, { code = it; error = null }, label = { Text("Código ALFRED1.…") }, modifier = Modifier.fillMaxWidth(), isError = error != null, supportingText = { error?.let { Text(it) } })
            Button(enabled = code.isNotBlank(), onClick = {
                val err = sv.activateLicense(code)
                if (err == null) { code = ""; ui.say("¡Premium activado! Gracias.") } else error = err
            }) { Text("Activar") }
            if (BillingConfig.SUPPORT_EMAIL.isNotBlank()) TextButton(onClick = { ctx.openUrl("mailto:${BillingConfig.SUPPORT_EMAIL}") }) { Text("Escribir a soporte") }
        }
    }
}

@Composable
private fun PlanButton(name: String, price: String, url: String) {
    val ctx = LocalContext.current
    Row(Modifier.fillMaxWidth().pxBorder().clickable(enabled = url.isNotBlank()) { ctx.openUrl(url) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.Bold)
            Text(if (url.isBlank()) "Próximamente" else price, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (url.isNotBlank()) Text("COMPRAR", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}
