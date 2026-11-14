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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.iatualcance.alfred.BuildConfig
import com.iatualcance.alfred.ai.ModelInfo
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
            title = { Text(when (which) { "premium" -> "PREMIUM"; "search" -> "BUSCAR"; else -> "AJUSTES" }) },
            navigationIcon = { IconButton(onClick = { ui.overlay = null }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
        )
        when (which) { "premium" -> PremiumScreen(); "search" -> SearchScreen(); else -> SettingsScreen() }
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
    LaunchedEffect(ui.contactQuery) { if (ui.contactQuery.isNotBlank()) { q = ui.contactQuery; ui.contactQuery = "" } }
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
        FloatingActionButton(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, shape = PxShape, onClick = { adding = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Default.Add, "Nuevo contacto") }
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
    val noteCount by sv.store.notes.collectAsStateCompat()
    val taskCount by sv.store.tasks.collectAsStateCompat()
    val templateCount by sv.store.templates.collectAsStateCompat()
    var key by remember { mutableStateOf(savedKey) }
    var show by remember { mutableStateOf(false) }
    var models by remember { mutableStateOf(ModelInfo.CURATED) }
    var modelsLoaded by remember { mutableStateOf(false) }
    var modelMenu by remember { mutableStateOf(false) }
    var probeOpen by remember { mutableStateOf(false) }
    val probeRows = remember { androidx.compose.runtime.mutableStateListOf<Triple<String, Int, String>>() }   // modelo, estado (0 probando · 1 ok · 2 falla), detalle
    var probeJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var confirm by remember { mutableStateOf<String?>(null) }   // acción grave pendiente de confirmar
    val connect = rememberGoogleConnect()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // ------------------------------------------------------------------ Cuenta
        // ------------------------------------------------------------------ Apariencia
        Section("Apariencia") {
            val skinId by sv.settings.skin.collectAsStateCompat()
            var skinMenu by remember { mutableStateOf(false) }
            val cur = Skins.byId(skinId)
            Text("Skin", style = MaterialTheme.typography.titleSmall)
            Text("El aspecto de toda la app. Se aplica al instante.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box {
                AppOutlinedButton(onClick = { skinMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(cur.label, Modifier.weight(1f)); Icon(Icons.Default.ArrowDropDown, null)
                }
                DropdownMenu(skinMenu, { skinMenu = false }) {
                    Skins.all.forEach { k ->
                        DropdownMenuItem(
                            { Column { Text(k.label + if (k.id == skinId) "  ✔" else "", fontWeight = FontWeight.SemiBold); Text(k.sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                            { sv.settings.setSkin(k.id); skinMenu = false },
                        )
                    }
                }
            }
        }
        Section("Cuenta de Google") {
            val a = account
            if (a == null) {
                Hint("Conecta tu cuenta para usar Calendar, Gmail, Contactos y Drive.")
                AppButton(onClick = connect, modifier = Modifier.fillMaxWidth()) { Text("Conectar con Google") }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (a.photo != null) AsyncImage(a.photo, null, Modifier.size(52.dp).clip(CircleShape)) else Art(com.iatualcance.alfred.R.drawable.art_pikachu, 52.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(a.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(a.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppOutlinedButton(onClick = { scope.launch { sv.disconnect(); connect() } }, modifier = Modifier.weight(1f)) { Text("Cambiar de cuenta") }
                    AppOutlinedButton(onClick = { confirm = "signout" }, modifier = Modifier.weight(1f)) { Text("Cerrar sesión") }
                }
                TextButton(onClick = { ctx.openUrl("https://myaccount.google.com/connections") }) { Text("Gestionar permisos en Google") }
            }
        }

        // ------------------------------------------------------------------ IA
        Section("Inteligencia artificial (Gemini)") {
            OutlinedTextField(
                key, { key = it }, label = { Text("Clave de API de Gemini") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = PxShape,
                visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { IconButton(onClick = { show = !show }) { Icon(if (show) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Mostrar") } },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AppButton(onClick = {
                    sv.settings.setApiKey(key)
                    if (key.isBlank()) ui.say("Clave borrada") else ui.run("Comprobando la clave…") {
                        sv.gemini.listModels()
                        ui.say("Clave correcta")
                    }
                }) { Text("Guardar y probar") }
                TextButton(onClick = { ctx.openUrl("https://aistudio.google.com/apikey") }) { Text("Obtener clave gratis") }
            }
            // ---- Consumo del mes y límite
            val usage by sv.settings.usage.collectAsStateCompat()
            val limit by sv.settings.monthlyLimit.collectAsStateCompat()
            val cityNow by sv.settings.weatherCity.collectAsStateCompat()
            Text("Consumo de este mes", style = MaterialTheme.typography.titleSmall)
            Text(
                "${usage.msgs} mensajes con IA" + (if (limit > 0) " de $limit" else "") + " · ${usage.tokens / 1000} mil tokens · ≈ " + "%.2f".format(usage.costEur).replace('.', ',') + " € (estimado)" +
                    (if (usage.advanced > 0) " · ${usage.advanced} consultas avanzadas" else ""),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (limit > 0) androidx.compose.material3.LinearProgressIndicator(progress = { (usage.msgs.toFloat() / limit).coerceIn(0f, 1f) }, Modifier.fillMaxWidth(), color = if (usage.msgs >= limit * 0.8) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            var limitText by remember(limit) { mutableStateOf(limit.toString()) }
            androidx.compose.material3.OutlinedTextField(
                limitText, { t -> limitText = t.filter { it.isDigit() }.take(6); sv.settings.setMonthlyLimit(limitText.toIntOrNull() ?: 0) }, Modifier.fillMaxWidth(), singleLine = true, shape = PxShape,
                label = { Text("Límite mensual de mensajes (0 = sin límite)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            )
            Text("Los atajos no gastan IA. Se avisa al llegar al 80 % y al 100 % del límite.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            androidx.compose.material3.OutlinedTextField(cityNow, { sv.settings.setWeatherCity(it) }, Modifier.fillMaxWidth(), singleLine = true, shape = PxShape, label = { Text("Tu ciudad (tiempo y búsquedas «cerca de mí»)") }, placeholder = { Text("Ej.: Zaragoza") })
            Text("Modelo en uso: $model (${ModelInfo.label(model).ifBlank { "gratis" }})", style = MaterialTheme.typography.titleSmall)
            Box {
                AppOutlinedButton(onClick = {
                    if (modelsLoaded || savedKey.isBlank()) modelMenu = true
                    else ui.run("Cargando modelos…") { models = ModelInfo.freeList(sv.gemini.listModels()); modelsLoaded = true; modelMenu = true }
                }) { Text("Elegir modelo gratuito") }
                DropdownMenu(modelMenu, { modelMenu = false }) {
                    models.forEach { m -> DropdownMenuItem({ Text(m + if (m == model) "  ✔" else "") }, { sv.settings.setModel(m); modelMenu = false; ui.say("Modelo: $m") }) }
                }
            }
            AppOutlinedButton(onClick = {
                if (savedKey.isBlank()) { ui.say("Guarda primero la clave"); return@AppOutlinedButton }
                probeRows.clear(); probeOpen = true
                probeJob = scope.launch {
                    val list = if (modelsLoaded) models else runCatching { ModelInfo.freeList(sv.gemini.listModels()) }.getOrDefault(ModelInfo.CURATED).also { models = it; modelsLoaded = true }
                    list.take(8).forEach { m ->
                        probeRows.add(Triple(m, 0, "probando…"))
                        val i = probeRows.lastIndex
                        val (ok, detail) = sv.gemini.probe(m)
                        probeRows[i] = Triple(m, if (ok) 1 else 2, detail)
                    }
                }
            }) { Text("Probar los modelos gratuitos") }
            if (savedKey.isNotBlank()) TextButton(onClick = { sv.settings.setApiKey(""); key = ""; ui.say("Clave eliminada de este móvil") }) {
                Text("Quitar la clave de este móvil", color = MaterialTheme.colorScheme.error)
            }
            Hint("Solo se ofrecen modelos con capa gratuita (Flash y Flash-Lite; los Pro son de pago). Si uno no contesta o agota su cupo, Alfred prueba solo con otros. Los límites exactos se ven en Google AI Studio.")
            Hint("La clave se guarda cifrada solo en este móvil. Con la capa gratuita, Google puede usar lo que envíes a Gemini para mejorar sus productos; con una clave de pago, no.")
        }

        // ------------------------------------------------------------------ Voz
        Section("Voz") {
            val speak by sv.settings.speakReplies.collectAsStateCompat()
            SwitchRow("Leer las respuestas en voz alta", "El asistente te contesta hablando. También puedes usar «Conversar» en la pantalla del asistente para hablar sin tocar nada.", speak) { sv.settings.setSpeakReplies(it) }
        }

        // ------------------------------------------------------------------ Notificaciones
        Section("Notificaciones") {
            val notifyEvents by sv.settings.notifyEvents.collectAsStateCompat()
            val notifyTasks by sv.settings.notifyTasks.collectAsStateCompat()
            val lead by sv.settings.leadMin.collectAsStateCompat()
            var leadMenu by remember { mutableStateOf(false) }
            var allowed by remember { mutableStateOf(com.iatualcance.alfred.notify.Notif.canPost(ctx)) }
            androidx.lifecycle.compose.LifecycleResumeEffect(Unit) { allowed = com.iatualcance.alfred.notify.Notif.canPost(ctx); onPauseOrDispose {} }
            if (!allowed) {
                Hint("Las notificaciones están desactivadas para Alfred, así que no sonará ningún aviso.")
                AppButton(onClick = {
                    ctx.startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, ctx.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }) { Text("Permitir notificaciones") }
            }
            SwitchRow("Avisar de las citas", "Notificación antes de cada evento de Google Calendar.", notifyEvents) { sv.settings.setNotifyEvents(it) }
            if (notifyEvents) Box {
                AppOutlinedButton(onClick = { leadMenu = true }) { Text(if (lead == 0) "Avisar a la hora exacta" else "Avisar $lead min antes") }
                DropdownMenu(leadMenu, { leadMenu = false }) {
                    listOf(0, 5, 10, 15, 30, 60, 120).forEach { m -> DropdownMenuItem({ Text(if (m == 0) "A la hora exacta" else "$m minutos antes") }, { sv.settings.setLeadMin(m); leadMenu = false }) }
                }
            }
            val summary by sv.settings.dailySummary.collectAsStateCompat()
            val summaryMin by sv.settings.summaryMinutes.collectAsStateCompat()
            SwitchRow("Resumen del día", "Cada mañana: tus citas, tareas y correos sin leer.", summary) { sv.settings.setDailySummary(it) }
            if (summary) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Hora del resumen", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    TimeButton(java.time.LocalTime.of(summaryMin / 60, summaryMin % 60)) { sv.settings.setSummaryMinutes(it.hour * 60 + it.minute) }
                }
                val sEv by sv.settings.sumEvents.collectAsStateCompat(); val sTk by sv.settings.sumTasks.collectAsStateCompat()
                val sNo by sv.settings.sumNotes.collectAsStateCompat(); val sMa by sv.settings.sumMail.collectAsStateCompat()
                val sWe by sv.settings.sumWeather.collectAsStateCompat(); val city by sv.settings.weatherCity.collectAsStateCompat()
                Text("Qué incluye", style = MaterialTheme.typography.labelLarge)
                SwitchRow("Citas del día", "Las de tu calendario de hoy.", sEv) { sv.settings.setSumEvents(it) }
                SwitchRow("Tareas", "Las de hoy y las vencidas.", sTk) { sv.settings.setSumTasks(it) }
                SwitchRow("Notas", "Las notas con aviso hoy.", sNo) { sv.settings.setSumNotes(it) }
                SwitchRow("Correos importantes", "Los sin leer que Gmail marca como importantes.", sMa) { sv.settings.setSumMail(it) }
                SwitchRow("El tiempo", "Si va a llover o no, y la máxima y la mínima.", sWe) { sv.settings.setSumWeather(it) }
                if (sWe && city.isBlank()) Text("Pon tu ciudad más arriba, en la sección de inteligencia artificial, para ver el tiempo.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SwitchRow("Avisar de las tareas", "A la hora que indique la tarea; si solo tiene fecha, a las 9:00.", notifyTasks) { sv.settings.setNotifyTasks(it) }
            AppOutlinedButton(onClick = {
                com.iatualcance.alfred.notify.Notif.show(ctx, com.iatualcance.alfred.notify.Reminder("test", System.currentTimeMillis(), "Alfred", "Así sonarán tus avisos.", "chat"))
                if (!allowed) ui.say("Activa primero las notificaciones")
            }) { Text("Enviar notificación de prueba") }
        }

        // ------------------------------------------------------------------ Actualizaciones
        Section("Actualizaciones") {
            val autoUpdate by sv.settings.autoUpdate.collectAsStateCompat()
            val upd by sv.updater.state.collectAsStateCompat()
            Text("Versión instalada: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.titleSmall)
            SwitchRow("Buscar e instalar al abrir", "Si hay una versión nueva, se descarga y se instala sola.", autoUpdate) { sv.settings.setAutoUpdate(it) }
            AppOutlinedButton(enabled = upd !is com.iatualcance.alfred.update.UpdateState.Checking && upd !is com.iatualcance.alfred.update.UpdateState.Downloading, onClick = {
                scope.launch {
                    val info = sv.updater.check(install = true)
                    if (info == null && sv.updater.state.value !is com.iatualcance.alfred.update.UpdateState.Failed) ui.say("Ya tienes la última versión")
                    else if (info != null && sv.updater.canInstall()) ui.say("Actualizando a la ${info.versionName}…")
                }
            }) { Text(when (upd) { is com.iatualcance.alfred.update.UpdateState.Checking -> "Buscando…"; is com.iatualcance.alfred.update.UpdateState.Downloading -> "Descargando…"; else -> "Buscar actualizaciones ahora" }) }
        }

        // ------------------------------------------------------------------ Drive
        Section("Drive") {
            SwitchRow("Subir escaneos automáticamente", "Cada documento escaneado se guarda también en Drive, carpeta Alfred (Facturas o Escaneos).", driveAuto) { sv.settings.setDriveAuto(it) }
            AppOutlinedButton(enabled = account != null, onClick = { ui.run("Haciendo copia de seguridad…") { sv.backupToDrive(); ui.say("Copia de notas, tareas y plantillas guardada en Drive") } }) { Text("Hacer copia de seguridad ahora") }
            if (account == null) Hint("Conecta tu cuenta de Google para usar Drive.")
        }

        // ------------------------------------------------------------------ Plan
        if (BillingConfig.ENABLED) Section("Plan") {
            val lic by sv.settings.license.collectAsStateCompat()
            val data = sv.licenseData(lic)
            Text(if (data != null) "Premium activo" + (data.x?.let { " hasta $it" } ?: " · de por vida") else "Plan gratuito", style = MaterialTheme.typography.titleSmall)
            AppOutlinedButton(onClick = { ui.overlay = "premium" }) { Text(if (data != null) "Ver mi plan" else "Ver Premium") }
        }

        // ------------------------------------------------------------------ Datos
        Section("Tus datos") {
            Hint("Tus notas, tareas y plantillas se guardan solo en este móvil. Alfred envía a Google únicamente lo necesario para cada acción (Calendar, Gmail, Contactos, Drive) y a Gemini el texto, audio o imagen que le pidas procesar. No hay servidores propios ni se comparten datos con terceros.")
            AppOutlinedButton(onClick = { ui.run("Preparando la copia…") { ctx.shareFile(sv.exportFile(), "application/json") } }, modifier = Modifier.fillMaxWidth()) { Text("Exportar mis datos (JSON)") }
        }

        // ------------------------------------------------------------------ Zona de borrado
        Section("Zona de borrado", danger = true) {
            Hint("Estas acciones no se pueden deshacer. Lo que ya está en Drive, Calendar o Gmail no se toca (salvo que lo indique).")
            DangerRow("Borrar la conversación con el asistente") { confirm = "chat" }
            DangerRow("Borrar todas las notas y sus adjuntos") { confirm = "notes" }
            DangerRow("Borrar todas las tareas y recordatorios") { confirm = "tasks" }
            DangerRow("Borrar todas las plantillas") { confirm = "templates" }
            if (account != null) DangerRow("Borrar mi copia de seguridad en Drive") { confirm = "backup" }
            DangerRow("Restablecer los ajustes") { confirm = "reset" }
            AppOutlinedButton(onClick = { confirm = "all" }, modifier = Modifier.fillMaxWidth()) { Text("Borrar mi cuenta y todos los datos", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
        }

        Text("Alfred ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }

    if (probeOpen) androidx.compose.material3.AlertDialog(
        onDismissRequest = { probeJob?.cancel(); probeOpen = false },
        title = { Text("Probar modelos gratuitos", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Hint("Cada modelo recibe una pregunta de prueba. Toca uno que funcione para usarlo.")
                probeRows.forEach { (m, st, detail) ->
                    Row(
                        Modifier.fillMaxWidth().clickable(enabled = st == 1) { sv.settings.setModel(m); probeJob?.cancel(); probeOpen = false; ui.say("Modelo: $m") }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(when (st) { 0 -> "⏳"; 1 -> "✅"; else -> "❌" }, Modifier.width(30.dp))
                        Column(Modifier.weight(1f)) {
                            Text(m + if (m == model) "  (en uso)" else "", style = MaterialTheme.typography.titleSmall)
                            Text(detail, style = MaterialTheme.typography.bodySmall, color = if (st == 2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { probeJob?.cancel(); probeOpen = false }) { Text("CERRAR") } },
    )

    when (confirm) {
        "signout" -> Confirm("Cerrar sesión", "Se retiran los permisos de Google a Alfred. Tus notas, tareas y plantillas se quedan en el móvil.", "Cerrar sesión", { confirm = null }) { scope.launch { sv.disconnect(); ui.say("Sesión cerrada") } }
        "chat" -> Confirm("Borrar la conversación", "Se borra el historial del chat. Tus notas y tareas no se tocan.", "Borrar", { confirm = null }) { sv.store.clearChat(); ui.say("Conversación borrada") }
        "notes" -> Confirm("Borrar todas las notas", "Se borran del móvil ${noteCount.size} notas y todos sus adjuntos. Lo que ya esté en Drive no se toca.", "Borrar", { confirm = null }) { sv.store.clearNotes(); ui.say("Notas borradas") }
        "tasks" -> Confirm("Borrar todas las tareas", "Se borran ${taskCount.size} tareas y sus avisos.", "Borrar", { confirm = null }) { sv.store.clearTasks(); ui.say("Tareas borradas") }
        "templates" -> Confirm("Borrar todas las plantillas", "Se borran las ${templateCount.size} plantillas guardadas.", "Borrar", { confirm = null }) { sv.store.clearTemplates(); ui.say("Plantillas borradas") }
        "backup" -> Confirm("Borrar la copia de Drive", "Se elimina de tu Drive el archivo «Copia de seguridad Alfred.json». Las notas del móvil no se tocan.", "Borrar", { confirm = null }) {
            ui.run("Borrando la copia…") { ui.say(if (sv.deleteDriveBackup()) "Copia de seguridad borrada" else "No había ninguna copia en Drive") }
        }
        "reset" -> Confirm("Restablecer los ajustes", "Notificaciones, actualizaciones, Drive y modelo vuelven a su valor inicial. La clave de Gemini, la cuenta y tus datos no se tocan.", "Restablecer", { confirm = null }) { sv.settings.resetPreferences(); ui.say("Ajustes restablecidos") }
        "all" -> ConfirmTyped(
            "Borrar mi cuenta y todos los datos",
            "Se retiran los permisos de Google, se cancelan los avisos y se borran del móvil las notas, tareas, plantillas, adjuntos, el chat y la clave de Gemini. No se puede deshacer. Lo que está en Drive, Calendar y Gmail no se toca.",
            onDismiss = { confirm = null },
        ) { scope.launch { sv.deleteAccountAndData(); key = ""; ui.say("Todo borrado") } }
    }
}

@Composable
private fun SwitchRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) = Row(verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.width(8.dp))
    Switch(checked, onChange)
}

@Composable
private fun DangerRow(text: String, onClick: () -> Unit) = Row(
    Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
) {
    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
    Spacer(Modifier.width(12.dp))
    Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Section(title: String, danger: Boolean = false, content: @Composable () -> Unit) =
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer, shape = PxShape,
        modifier = Modifier.fillMaxWidth().pxBorder(if (danger) MaterialTheme.colorScheme.error.copy(alpha = 0.55f) else SoftLine),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
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
            AppButton(enabled = code.isNotBlank(), onClick = {
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
