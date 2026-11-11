package com.iatualcance.alfred.ui

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.iatualcance.alfred.AlfredApp
import com.iatualcance.alfred.R
import com.iatualcance.alfred.data.ChatMsg
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.iatualcance.alfred.google.ConsentRequired

class PendingConfirm(val title: String, val detail: String, val answer: CompletableDeferred<Boolean>)

class ChatViewModel(app: Application) : AndroidViewModel(app) {
    private val sv = (app as AlfredApp).sv
    var busy by mutableStateOf(false); private set
    var pending by mutableStateOf<PendingConfirm?>(null)

    fun send(text: String, image: Uri?) {
        if (busy || (text.isBlank() && image == null)) return
        val history = sv.store.chat.value.filter { !it.error }.map { it.fromUser to it.text }
        sv.store.addChat(ChatMsg(fromUser = true, text = text.ifBlank { "(imagen)" } + if (image != null && text.isNotBlank()) "  🖼️" else ""))
        busy = true
        viewModelScope.launch {
            try {
                val img = image?.let { uri ->
                    withContext(Dispatchers.IO) {
                        val cr = getApplication<Application>().contentResolver
                        cr.openInputStream(uri)!!.use { it.readBytes() } to (cr.getType(uri) ?: "image/jpeg")
                    }
                }
                val reply = sv.assistant.ask(history, text, img) { title, detail ->
                    val d = CompletableDeferred<Boolean>()
                    pending = PendingConfirm(title, detail, d)
                    try { d.await() } finally { pending = null }
                }
                sv.store.addChat(ChatMsg(fromUser = false, text = reply.text, actions = reply.actions))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val msg = if (e is ConsentRequired) "Para esto necesito tu cuenta de Google. Conéctala en Más → Ajustes." else (e.message ?: e.toString())
                sv.store.addChat(ChatMsg(fromUser = false, text = msg, error = true))
            } finally {
                busy = false
            }
        }
    }
}

private val suggestions = listOf(
    "¿Qué tengo en la agenda esta semana?", "Resume mis correos sin leer", "Recuérdame llamar al dentista mañana",
    "Crea una reunión el viernes a las 10", "Guarda una nota con la lista de la compra",
)

@Composable
fun ChatScreen(vm: ChatViewModel = viewModel()) {
    val sv = LocalServices.current
    val ui = LocalUi.current
    val ctx = LocalContext.current
    val msgs by sv.store.chat.collectAsStateCompat()
    val key by sv.settings.apiKey.collectAsStateCompat()
    var input by rememberSaveable { mutableStateOf("") }
    var image by rememberSaveable { mutableStateOf<Uri?>(null) }
    var askClear by rememberSaveable { mutableStateOf(false) }
    val list = rememberLazyListState()

    LaunchedEffect(msgs.size, vm.busy) { if (msgs.isNotEmpty()) list.animateScrollToItem(msgs.size - 1 + if (vm.busy) 1 else 0) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { if (it != null) image = it }
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val said = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!said.isNullOrBlank()) input = (input.trim() + " " + said).trim()
        }
    }

    fun send() { vm.send(input.trim(), image); input = ""; image = null }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.logo), null, Modifier.size(36.dp).clip(CircleShape))
                    Spacer(Modifier.width(10.dp)); Text("Alfred", fontWeight = FontWeight.SemiBold)
                }
            },
            actions = { if (msgs.isNotEmpty()) IconButton(onClick = { askClear = true }) { Icon(Icons.Default.DeleteSweep, "Borrar conversación") } },
        )

        if (key.isBlank()) Surface(
            color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(12.dp).fillMaxWidth().clickable { ui.moreScreen = "settings"; ui.tab = Tab.MORE },
        ) {
            Text("Para hablar con Alfred falta la clave de Gemini. Toca aquí para añadirla en Ajustes.", Modifier.padding(14.dp))
        }

        if (msgs.isEmpty()) {
            Column(Modifier.weight(1f).fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(R.drawable.logo), null, Modifier.size(120.dp).clip(CircleShape))
                VSpace(16)
                Text("¿En qué puedo ayudarle?", style = MaterialTheme.typography.titleLarge)
                VSpace(8)
                Hint("Agenda, correo, contactos, notas y tareas. Escriba o use el micrófono.")
                VSpace(16)
                suggestions.forEach { s -> AssistChip(onClick = { input = s }, label = { Text(s) }) }
            }
        } else LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list, contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(msgs, key = { it.id }) { Bubble(it) }
            if (vm.busy) item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)); Hint("Alfred está en ello…")
                }
            }
        }

        image?.let { uri ->
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(uri, null, Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                IconButton(onClick = { image = null }) { Icon(Icons.Default.Close, "Quitar imagen") }
            }
        }
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.Bottom) {
            IconButton(onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                Icon(Icons.Default.Image, "Adjuntar imagen")
            }
            OutlinedTextField(
                value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f),
                placeholder = { Text("Escriba a Alfred…") }, maxLines = 5, shape = RoundedCornerShape(24.dp),
            )
            if (input.isBlank() && image == null) IconButton(onClick = {
                try {
                    speech.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                        .putExtra(RecognizerIntent.EXTRA_PROMPT, "Dígame"))
                } catch (_: Exception) { ui.say("Este móvil no tiene reconocimiento de voz de Google instalado") }
            }) { Icon(Icons.Default.Mic, "Hablar") }
            else FilledIconButton(onClick = ::send, enabled = !vm.busy) { Icon(Icons.AutoMirrored.Filled.Send, "Enviar") }
        }
    }

    vm.pending?.let { p ->
        AlertDialog(
            onDismissRequest = { p.answer.complete(false) }, title = { Text(p.title) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text(p.detail.take(1500)) } },
            confirmButton = { TextButton(onClick = { p.answer.complete(true) }) { Text("Sí") } },
            dismissButton = { TextButton(onClick = { p.answer.complete(false) }) { Text("Cancelar") } },
        )
    }
    if (askClear) Confirm("Borrar conversación", "Se borrará el historial del chat. Tus notas y tareas no se tocan.", "Borrar", { askClear = false }) { sv.store.clearChat() }
}

@Composable
private fun Bubble(m: ChatMsg) {
    val cs = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth(), contentAlignment = if (m.fromUser) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(
            Modifier.widthIn(max = 320.dp)
                .clip(RoundedCornerShape(18.dp, 18.dp, if (m.fromUser) 4.dp else 18.dp, if (m.fromUser) 18.dp else 4.dp))
                .background(when { m.error -> cs.errorContainer; m.fromUser -> cs.secondaryContainer; else -> cs.surfaceContainerHigh })
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            m.actions.forEach { Text(it, style = MaterialTheme.typography.labelLarge, color = cs.primary, modifier = Modifier.padding(bottom = 4.dp)) }
            SelectionContainer { Text(m.text, color = if (m.error) cs.onErrorContainer else cs.onSurface) }
        }
    }
}
