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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.iatualcance.alfred.AlfredApp
import com.iatualcance.alfred.QuotaExceeded
import com.iatualcance.alfred.billing.BillingConfig
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
    /** Orden de navegación que Alfred ha dado; la consume AppRoot. */
    var nav by mutableStateOf<com.iatualcance.alfred.ai.Nav?>(null)
    /** Se pone a true cuando se agota la cuota gratis; AppRoot abre la pantalla Premium. */
    var needsPremium by mutableStateOf(false)

    /** Texto de la petición que se ha parado, para volver a editarlo en la caja. La consume ChatScreen. */
    var restored by mutableStateOf<String?>(null)
    private var job: kotlinx.coroutines.Job? = null
    private var lastUser: ChatMsg? = null

    /** Resumen del día para la cabecera de las skins sobrias (título, líneas). Se refresca como mucho cada 10 minutos. */
    var home by mutableStateOf<Pair<String, String>?>(null)
    private var homeAt = 0L
    fun loadHome() {
        if (home != null && System.currentTimeMillis() - homeAt < 10 * 60_000) return
        homeAt = System.currentTimeMillis()
        viewModelScope.launch { home = runCatching { sv.buildSummary() }.getOrNull() ?: home }
    }

    init { sv.voice.onText = { send(it, null) } }

    /** Para lo que Alfred esté haciendo: cancela la petición, quita tu mensaje y deja el texto para reformularlo. */
    fun stop() {
        if (!busy) { sv.voice.interrupt(); return }
        job?.cancel(); job = null
        pending?.answer?.complete(false)
        lastUser?.let { m -> sv.store.removeChat(m.id); restored = m.text.removeSuffix("  🖼️").takeIf { it != "(imagen)" } }
        lastUser = null; busy = false
        sv.voice.interrupt()
    }

    fun send(text: String, image: Uri?) {
        if (busy || (text.isBlank() && image == null)) return
        val history = sv.store.chat.value.filter { !it.error }.map { it.fromUser to it.text }
        val um = ChatMsg(fromUser = true, text = text.ifBlank { "(imagen)" } + if (image != null && text.isNotBlank()) "  🖼️" else "")
        sv.store.addChat(um); lastUser = um
        busy = true
        job = viewModelScope.launch {
            try {
                val img = image?.let { uri ->
                    withContext(Dispatchers.IO) {
                        val cr = getApplication<Application>().contentResolver
                        val bytes = cr.openInputStream(uri)!!.use { it.readBytes() }
                        if (bytes.size > 14 * 1024 * 1024) throw Exception("El archivo pesa más de 14 MB y no puedo procesarlo. Prueba con uno más pequeño.")
                        bytes to (cr.getType(uri) ?: "image/jpeg")
                    }
                }
                val reply = sv.metered { sv.assistant.ask(history, text, img, voice = sv.voice.active) { title, detail ->
                    val d = CompletableDeferred<Boolean>()
                    pending = PendingConfirm(title, detail, d)
                    try { d.await() } finally { pending = null }
                } }
                sv.store.addChat(ChatMsg(fromUser = false, text = reply.text, actions = reply.actions))
                nav = reply.nav.lastOrNull()
                if (sv.voice.active) sv.voice.speak(reply.text) { sv.voice.relisten() }
                else if (sv.settings.speakReplies.value) sv.voice.speak(reply.text)
            } catch (e: CancellationException) {
                throw e
            } catch (e: QuotaExceeded) {
                sv.store.addChat(ChatMsg(fromUser = false, text = e.message.orEmpty(), error = true))
                sv.voice.stop()
                needsPremium = true
            } catch (e: Exception) {
                val msg = if (e is ConsentRequired) "Para esto necesito tu cuenta de Google. Conéctala en Ajustes (el engranaje de la pantalla de Alfred)." else (e.message ?: e.toString())
                sv.store.addChat(ChatMsg(fromUser = false, text = msg, error = true))
                sv.voice.stop()
            } finally {
                if (coroutineContext[kotlinx.coroutines.Job]?.isActive != false) { busy = false; lastUser = null }
                launch { runCatching { sv.refreshReminders(true) } }
            }
        }
    }
}

private val suggestions = listOf(
    "Cuéntame algo interesante que no sepa", "Ayúdame a decidir entre dos opciones", "Explícame algo como si tuviera 10 años",
    "¿Qué tengo en la agenda esta semana?", "Resume mis correos sin leer", "Recuérdame llamar al dentista mañana a las 10",
)

private data class Quick(val label: String, val prompt: String)
private val quick = listOf(
    Quick("Hoy", "¿Qué tengo hoy en la agenda?"), Quick("Sin leer", "Resume mis correos sin leer"),
    Quick("Tareas", "Abre mis tareas"), Quick("Escanear", "Abre el escáner de documentos"),
    Quick("Nota", "Quiero crear una nota"), Quick("Drive", "Abre Drive"),
)

@Composable
fun ChatScreen(vm: ChatViewModel) {
    val sv = LocalServices.current
    val ui = LocalUi.current
    val msgs by sv.store.chat.collectAsStateCompat()
    val key by sv.settings.apiKey.collectAsStateCompat()
    var input by rememberSaveable { mutableStateOf("") }
    var image by rememberSaveable { mutableStateOf<Uri?>(null) }
    var askClear by rememberSaveable { mutableStateOf(false) }
    val list = rememberLazyListState()
    val ctx = LocalContext.current
    var pendingOneShot by remember { mutableStateOf(false) }
    val voiceState by sv.voice.state.collectAsStateCompat()
    val heard by sv.voice.heard.collectAsStateCompat()
    val speak by sv.settings.speakReplies.collectAsStateCompat()
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) { if (sv.voice.available()) sv.voice.startConversation(pendingOneShot) else ui.say("Este móvil no tiene reconocimiento de voz de Google instalado") }
        else ui.say("Sin permiso de micrófono no puedo escucharte. Actívalo en los ajustes del móvil.")
    }
    fun startConversation(oneShot: Boolean = false) {
        pendingOneShot = oneShot
        if (androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            if (sv.voice.available()) sv.voice.startConversation(oneShot) else ui.say("Este móvil no tiene reconocimiento de voz de Google instalado")
        } else micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
    }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { sv.voice.stop() } }

    LaunchedEffect(msgs.size, vm.busy) { if (msgs.isNotEmpty()) list.animateScrollToItem(msgs.size - 1 + if (vm.busy) 1 else 0) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { if (it != null) image = it }
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val said = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!said.isNullOrBlank()) vm.send(said.trim(), null)
        }
    }

    LaunchedEffect(vm.restored) { vm.restored?.let { input = if (input.isBlank()) it else "$it $input"; vm.restored = null } }
    LaunchedEffect(ui.chatDraft) { ui.chatDraft?.let { image = it.uri; input = it.text; ui.chatDraft = null } }

    fun send() { vm.send(input.trim(), image); input = ""; image = null }

    Column(Modifier.fillMaxSize()) {
        PixelBar(
            title = { Text("ALFRED") },
            actions = {
                if (msgs.isNotEmpty()) IconButton(onClick = { askClear = true }) { Icon(Icons.Default.DeleteSweep, "Borrar conversación") }
            },
        )

        if (BillingConfig.ENABLED) {
            val lic by sv.settings.license.collectAsStateCompat()
            val used by sv.settings.aiToday.collectAsStateCompat()
            if (!sv.isPremium(lic)) Text(
                "Te quedan ${(BillingConfig.FREE_AI_PER_DAY - used).coerceAtLeast(0)} mensajes gratis hoy · Ver Premium",
                Modifier.fillMaxWidth().clickable { ui.overlay = "premium" }.padding(horizontal = 16.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center,
            )
        }
        if (key.isBlank()) Surface(
            color = MaterialTheme.colorScheme.primaryContainer, shape = PxShape,
            modifier = Modifier.padding(12.dp).fillMaxWidth().pxBorder(Ink).clickable { ui.overlay = "settings" },
        ) {
            Text("Falta la clave de Gemini para hablar con Alfred. Toca aquí y añádela en Ajustes.", Modifier.padding(14.dp))
        }

        if (msgs.isEmpty()) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = if (Skins.current.pokemon) Arrangement.Center else Arrangement.Top, horizontalAlignment = Alignment.CenterHorizontally) {
                if (Skins.current.pokemon) Art(R.drawable.art_pikachu, 150.dp, "Asistente") else { HomeHeader(vm); VSpace(8) }
                if (Skins.current.pokemon) VSpace(16)
                Text("¿En qué te ayudo?", style = MaterialTheme.typography.headlineSmall, color = Ink, textAlign = TextAlign.Center)
                VSpace(8)
                Hint("Pregúntame lo que quieras o simplemente charla conmigo. Y si quieres, también manejo tu correo, agenda, notas, tareas, contactos y Drive. Escribe, usa el micrófono o pulsa «Conversar».")
                VSpace(12)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    suggestions.forEach { s -> PillChip(s, { input = s }) }
                }
            }
        } else LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list, contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (!Skins.current.pokemon) item(key = "home") { HomeHeader(vm) }
            items(msgs, key = { it.id }) { Bubble(it) }
            if (vm.busy) item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)); Hint("Alfred está en ello…")
                }
            }
        }

        // Atajos
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillChip("Conversar", { if (voiceState == com.iatualcance.alfred.voice.VoiceController.State.IDLE) startConversation() else sv.voice.stop() })
            PillChip(if (speak) "Voz: sí" else "Voz: no", { sv.settings.setSpeakReplies(!speak); if (speak) sv.voice.stop() })
            quick.forEach { q -> PillChip(q.label, { vm.send(q.prompt, null) }, enabled = !vm.busy) }
        }
        if (voiceState != com.iatualcance.alfred.voice.VoiceController.State.IDLE) Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).card(borderColor = MaterialTheme.colorScheme.primary).padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
            Text(when (voiceState) { com.iatualcance.alfred.voice.VoiceController.State.LISTENING -> if (heard.isBlank()) "Escuchando… habla ahora" else "«$heard»"; com.iatualcance.alfred.voice.VoiceController.State.THINKING -> "Pensando…"; else -> "Hablando…" },
                style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 2)
            if (voiceState != com.iatualcance.alfred.voice.VoiceController.State.LISTENING || heard.isNotBlank()) TextButton(onClick = { vm.stop() }) { Text("PARAR", fontWeight = FontWeight.Bold) }
            TextButton(onClick = { sv.voice.stop() }) { Text("TERMINAR", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
        }
        image?.let { uri ->
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                val isImage = ctx.contentResolver.getType(uri)?.startsWith("image/") != false
                if (isImage) AsyncImage(uri, null, Modifier.size(56.dp).clip(PxShape), contentScale = ContentScale.Crop)
                else Row(Modifier.card(borderColor = SoftLine).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AttachFile, null); Spacer(Modifier.width(6.dp)); Text("Archivo adjunto", style = MaterialTheme.typography.labelLarge)
                }
                IconButton(onClick = { image = null }) { Icon(Icons.Default.Close, "Quitar imagen") }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, bottom = 8.dp, top = 2.dp), verticalAlignment = Alignment.Bottom) {
            IconButton(onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                Icon(Icons.Default.Image, "Adjuntar imagen")
            }
            OutlinedTextField(
                value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f).then(if (Skins.current.pokemon) Modifier.shadow(3.dp, PxShape, clip = false, ambientColor = Color(0x221B2A5E), spotColor = Color(0x331B2A5E)) else Modifier),
                placeholder = { Text("Escribe a Alfred…") }, maxLines = 5, shape = Skins.current.inputShape,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Surf, unfocusedContainerColor = Surf,
                    focusedBorderColor = if (Skins.current.look == Look.CYBER || Skins.current.look == Look.RETRO) Skins.current.accent2 else MaterialTheme.colorScheme.primary, unfocusedBorderColor = Skins.current.cardBorder,
                ),
            )
            Spacer(Modifier.width(8.dp))
            if (vm.busy) AppIconButton(onClick = { vm.stop() }) { Icon(Icons.Default.Stop, "Parar") }
            else if (input.isBlank() && image == null) AppIconButton(onClick = {
                if (voiceState != com.iatualcance.alfred.voice.VoiceController.State.IDLE) sv.voice.stop() else startConversation(oneShot = true)
            }) { Icon(Icons.Default.Mic, "Hablar") }
            else AppIconButton(onClick = ::send) { Icon(Icons.AutoMirrored.Filled.Send, "Enviar") }
        }
    }

    if (askClear) Confirm("Borrar conversación", "Se borrará el historial del chat. Tus notas y tareas no se tocan.", "Borrar", { askClear = false }) { sv.store.clearChat() }
}

@Composable
private fun Bubble(m: ChatMsg) {
    val cs = MaterialTheme.colorScheme
    val k = Skins.current
    val shape: androidx.compose.ui.graphics.Shape = when (k.look) {
        Look.CYBER -> androidx.compose.foundation.shape.CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp)
        Look.DC -> RoundedCornerShape(8.dp); Look.MARVEL -> RoundedCornerShape(14.dp); else -> RoundedCornerShape(18.dp)
    }
    val bg = when { m.error -> cs.errorContainer; m.fromUser -> if (k.pokemon) cs.secondaryContainer else k.userBubble; else -> Surf }
    val line = when {
        m.error -> cs.error.copy(alpha = 0.7f)
        m.fromUser -> if (k.pokemon) Blue.copy(alpha = 0.75f) else if (k.look == Look.MARVEL) k.cardBorder else k.userBubble
        else -> if (k.pokemon) SoftLine else k.cardBorder
    }
    val textColor = when { m.error -> cs.onErrorContainer; m.fromUser && !k.pokemon -> k.onUserBubble; else -> cs.onSurface }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.fromUser) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Top) {
        if (!m.fromUser) {
            if (k.pokemon) Box(
                Modifier.size(44.dp).shadow(3.dp, CircleShape, ambientColor = Color(0x221B2A5E), spotColor = Color(0x331B2A5E)).background(Surf, CircleShape).border(2.dp, Ink, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Art(R.drawable.art_pikachu_round, 40.dp) }
            else AlfredMark(34.dp)
            Spacer(Modifier.width(10.dp))
        }
        Column(
            Modifier.widthIn(max = 310.dp)
                .then(if (k.pokemon) Modifier.shadow(3.dp, shape, clip = false, ambientColor = Color(0x221B2A5E), spotColor = Color(0x331B2A5E)) else Modifier)
                .background(bg, shape).border(if (k.pokemon) 1.5.dp else k.cardBorderW.dp, line, shape).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            m.actions.forEach { Text(it, style = MaterialTheme.typography.labelLarge, color = if (m.fromUser && !k.pokemon) k.onUserBubble else cs.primary, fontWeight = FontWeight.Bold) }
            SelectionContainer { Text(m.text, style = MaterialTheme.typography.bodyLarge, color = textColor) }
        }
    }
}

/** Cabecera de las skins sobrias: saludo y tarjeta "Tu día" (citas, tareas, notas, correos y tiempo). */
@Composable
private fun HomeHeader(vm: ChatViewModel) {
    val k = Skins.current
    LaunchedEffect(Unit) { vm.loadHome() }
    val date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", com.iatualcance.alfred.Util.ES)).uppercase()
    val home = vm.home
    Column(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Text(date, style = MaterialTheme.typography.labelMedium, color = k.accent, letterSpacing = 1.sp)
        Text(home?.first ?: "Hola", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Normal), color = k.ink, modifier = Modifier.padding(top = 2.dp))
        Text(if (home == null) "Preparando tu día…" else "Esto es lo que tienes hoy.", color = k.muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp, bottom = 14.dp))
        if (home != null) Column(Modifier.fillMaxWidth().card().padding(horizontal = 16.dp, vertical = 6.dp)) {
            val lines = home.second.lines().filter { it.isNotBlank() }
            lines.forEachIndexed { i, raw ->
                val icon = when {
                    raw.startsWith("📅") -> Icons.Outlined.CalendarMonth
                    raw.startsWith("✅") -> Icons.Outlined.CheckCircle
                    raw.startsWith("📝") -> Icons.Outlined.StickyNote2
                    raw.startsWith("✉") -> Icons.Outlined.MailOutline
                    raw.startsWith("☀") -> Icons.Outlined.WbSunny
                    raw.startsWith("☔") || raw.startsWith("⛈") || raw.startsWith("❄") -> Icons.Outlined.WaterDrop
                    else -> Icons.Outlined.Cloud
                }
                val body = raw.dropWhile { !it.isLetterOrDigit() }.trim()
                val head = body.substringBefore(": "); val tail = if (": " in body) body.substringAfter(": ") else ""
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, Modifier.size(22.dp), tint = k.accent)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(head, style = MaterialTheme.typography.titleSmall, color = k.ink)
                        if (tail.isNotBlank()) Text(tail, style = MaterialTheme.typography.bodySmall, color = k.muted, maxLines = 2)
                    }
                }
                if (i < lines.lastIndex) Box(Modifier.fillMaxWidth().padding(start = 36.dp).height(1.dp).background(k.line))
            }
        }
    }
}
