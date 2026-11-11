package com.iatualcance.alfred.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
fun AppRoot(shared: MutableStateFlow<Intent?>) {
    val sv = LocalServices.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    val ui = remember { AppUi(snack, scope) }
    val incoming by shared.collectAsState()

    LaunchedEffect(incoming) {
        val i = incoming ?: return@LaunchedEffect
        shared.value = null
        ui.run("Guardando en Alfred…") { handleShared(i, sv, ui) }
    }

    CompositionLocalProvider(LocalUi provides ui) {
        Scaffold(
            snackbarHost = { SnackbarHost(snack) },
            bottomBar = {
                NavigationBar {
                    listOf(
                        Triple(Tab.CHAT, "Alfred", Icons.AutoMirrored.Filled.Chat),
                        Triple(Tab.AGENDA, "Agenda", Icons.Default.CalendarMonth),
                        Triple(Tab.NOTES, "Notas", Icons.AutoMirrored.Filled.StickyNote2),
                        Triple(Tab.MAIL, "Correo", Icons.Default.Mail),
                        Triple(Tab.MORE, "Más", Icons.Default.MoreHoriz),
                    ).forEach { (t, label, icon) ->
                        NavigationBarItem(
                            selected = ui.tab == t, onClick = { ui.tab = t },
                            icon = { Icon(icon, label) }, label = { Text(label, maxLines = 1) },
                        )
                    }
                }
            },
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad).consumeWindowInsets(pad).imePadding()) {
                when (ui.tab) {
                    Tab.CHAT -> ChatScreen()
                    Tab.AGENDA -> AgendaScreen()
                    Tab.NOTES -> NotesScreen()
                    Tab.MAIL -> MailScreen()
                    Tab.MORE -> MoreScreen()
                }
            }
        }
        ui.busy?.let { BusyDialog(it) }
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
