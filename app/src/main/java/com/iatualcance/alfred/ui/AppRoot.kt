package com.iatualcance.alfred.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iatualcance.alfred.update.UpdateState
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import kotlinx.coroutines.launch
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.content.IntentCompat
import com.iatualcance.alfred.Services
import com.iatualcance.alfred.data.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

@Composable
fun AppRoot(shared: MutableStateFlow<Intent?>, route: MutableStateFlow<String?>) {
    val sv = LocalServices.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    val ui = remember { AppUi(snack, scope) }
    val chat: ChatViewModel = viewModel()
    val incoming by shared.collectAsState()
    val routed by route.collectAsState()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val notifPermission = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }
    // Actualizaciones: al abrir la app busca una versión nueva y, si la hay, la descarga y lanza el instalador.
    val updateState by sv.updater.state.collectAsState()
    LaunchedEffect(Unit) {
        sv.updater.cleanup()
        if (sv.settings.autoUpdate.value) sv.updater.check(install = true, quiet = true)
    }
    // Al volver de la pantalla de permisos de Android, reintenta.
    androidx.lifecycle.compose.LifecycleResumeEffect(updateState) {
        if (updateState is UpdateState.NeedsPermission && sv.updater.canInstall()) scope.launch { sv.updater.check(install = true) }
        onPauseOrDispose {}
    }
    LaunchedEffect(updateState) { (updateState as? UpdateState.Failed)?.let { ui.say(it.message); sv.updater.state.value = UpdateState.Idle } }
    LaunchedEffect(routed) { routed?.let { ui.navigate(com.iatualcance.alfred.ai.Nav.Screen(it)); route.value = null } }

    LaunchedEffect(incoming) {
        val i = incoming ?: return@LaunchedEffect
        shared.value = null
        ui.run("Guardando en Alfred…") { handleShared(i, sv, ui) }
    }
    // Alfred (el asistente) manda a la interfaz: abrir pantallas, notas, redactar…
    LaunchedEffect(chat.needsPremium) { if (chat.needsPremium) { ui.overlay = "premium"; chat.needsPremium = false } }
    LaunchedEffect(chat.nav) { chat.nav?.let { ui.navigate(it); chat.nav = null } }

    CompositionLocalProvider(LocalUi provides ui) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snack, Modifier.padding(bottom = 4.dp)) },
            bottomBar = { PixelNavBar(ui.tab) { ui.overlay = null; ui.tab = it } },
        ) { pad ->
            Box(Modifier.fillMaxSize().pixelGrid().padding(pad).consumeWindowInsets(pad).imePadding()) {
                when {
                    ui.overlay != null -> OverlayScreen(ui.overlay!!)
                    else -> when (ui.tab) {
                        Tab.CHAT -> ChatScreen(chat)
                        Tab.AGENDA -> AgendaScreen()
                        Tab.NOTES -> NotesScreen()
                        Tab.MAIL -> MailScreen()
                        Tab.CONTACTS -> ContactsTab()
                    }
                }
            }
        }
        ui.busy?.let { BusyDialog(it) }
        (updateState as? UpdateState.NeedsPermission)?.let { st ->
            AlertDialog(
                onDismissRequest = { sv.updater.state.value = UpdateState.Idle },
                title = { Text("Hay una versión nueva", style = MaterialTheme.typography.titleSmall) },
                text = { Text("Alfred ${st.info.versionName} está disponible. Para instalarla solo, Alfred necesita permiso para instalar aplicaciones: activa «Permitir desde esta fuente» y vuelve.\n\n" + st.info.notes.take(300)) },
                confirmButton = { TextButton(onClick = { sv.updater.openInstallPermission() }) { Text("ABRIR AJUSTES") } },
                dismissButton = { TextButton(onClick = { sv.updater.state.value = UpdateState.Idle }) { Text("MÁS TARDE") } },
            )
        }
        // Banner de progreso, sin bloquear la app
        val msg = when (val st = updateState) {
            is UpdateState.Downloading -> "Descargando Alfred ${st.info.versionName}… ${st.percent}%"
            is UpdateState.Installing -> "Instalando Alfred ${st.info.versionName}…"
            else -> null
        }
        if (msg != null) Box(Modifier.fillMaxSize().padding(top = 70.dp).statusBarsPadding(), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
            Surface(color = Yellow, shape = PxShape, modifier = Modifier.pxBorder()) { Text(msg, Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
        }
        chat.pending?.let { p ->
            AlertDialog(
                onDismissRequest = { p.answer.complete(false) }, title = { Text(p.title, style = MaterialTheme.typography.titleSmall) },
                text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(p.detail.take(1500)) } },
                confirmButton = { TextButton(onClick = { p.answer.complete(true) }) { Text("SÍ") } },
                dismissButton = { TextButton(onClick = { p.answer.complete(false) }) { Text("CANCELAR") } },
            )
        }
    }
}

/** "Compartir con Alfred" desde otra app. */
private suspend fun handleShared(i: Intent, sv: Services, ui: AppUi) {
    val type = i.type.orEmpty()
    val uri = IntentCompat.getParcelableExtra(i, Intent.EXTRA_STREAM, Uri::class.java)
    val text = i.getStringExtra(Intent.EXTRA_TEXT)
    val note: Note? = when {
        uri != null && (type.startsWith("audio/") || type == "application/ogg") -> {
            val att = withContext(Dispatchers.IO) { sv.store.importUri(uri, "audio/ogg", "Audio") }
            if (sv.hasAi) sv.audioToNote(att)
            else sv.store.upsertNote(Note(title = "Audio compartido", attachments = listOf(att), tags = listOf("audio")))
        }
        uri != null && type == "application/pdf" -> sv.documentToNote(listOf(uri to "application/pdf"))
        uri != null && type.startsWith("image/") -> {
            val att = withContext(Dispatchers.IO) { sv.store.importUri(uri, "image/jpeg", "Imagen") }
            sv.store.upsertNote(Note(title = text?.take(60) ?: "Imagen compartida", content = text.orEmpty(), attachments = listOf(att)))
        }
        !text.isNullOrBlank() -> sv.store.upsertNote(
            Note(title = i.getStringExtra(Intent.EXTRA_SUBJECT) ?: text.lineSequence().first().take(60), content = text)
        )
        else -> null
    }
    if (note != null) ui.openNote(note.id) else ui.say("No he podido leer el contenido compartido")
}
