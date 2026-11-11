package com.iatualcance.alfred.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.iatualcance.alfred.Services
import com.iatualcance.alfred.google.ConsentRequired
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

val Gold = Color(0xFFD9B54A)
val Navy = Color(0xFF0D0A25)
val NavySurface = Color(0xFF17143A)
val NavyHigh = Color(0xFF221E4D)
val Blue = Color(0xFF5B9BFF)

private val Colors = darkColorScheme(
    primary = Gold, onPrimary = Color(0xFF231A00), primaryContainer = Color(0xFF4A3C0C), onPrimaryContainer = Color(0xFFFFE9A6),
    secondary = Blue, onSecondary = Color(0xFF001B3F), secondaryContainer = Color(0xFF1D3466), onSecondaryContainer = Color(0xFFD7E3FF),
    tertiary = Blue, background = Navy, onBackground = Color(0xFFE9E7F5), surface = Navy, onSurface = Color(0xFFE9E7F5),
    surfaceVariant = NavyHigh, onSurfaceVariant = Color(0xFFB9B6D3), surfaceContainer = NavySurface, surfaceContainerLow = NavySurface,
    surfaceContainerHigh = NavyHigh, surfaceContainerHighest = Color(0xFF2B2760), surfaceContainerLowest = Color(0xFF090720),
    outline = Color(0xFF6E6A94), outlineVariant = Color(0xFF39356A), error = Color(0xFFFF8A80), onError = Color(0xFF3B0907),
    errorContainer = Color(0xFF5C1512), onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun AlfredTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = Colors, content = content)

enum class Tab { CHAT, AGENDA, NOTES, MAIL, MORE }

data class MailDraft(val to: String = "", val subject: String = "", val body: String = "")

/** Estado de interfaz compartido entre pestañas. */
class AppUi(val snack: SnackbarHostState, private val scope: CoroutineScope) {
    var tab by mutableStateOf(Tab.CHAT)
    var busy by mutableStateOf<String?>(null)
    var openNoteId by mutableStateOf<String?>(null)
    var compose by mutableStateOf<MailDraft?>(null)
    var moreScreen by mutableStateOf("menu")

    fun say(msg: String) { scope.launch { snack.currentSnackbarData?.dismiss(); snack.showSnackbar(msg) } }

    fun fail(e: Throwable) = say(if (e is ConsentRequired) "Conecta tu cuenta de Google en Más → Ajustes" else (e.message ?: e.toString()))

    /** Ejecuta una operación mostrando "Trabajando…" y enseñando el error si falla. */
    fun run(label: String, block: suspend () -> Unit) {
        scope.launch {
            busy = label
            try { block() } catch (e: CancellationException) { throw e } catch (e: Exception) { fail(e) } finally { busy = null }
        }
    }

    fun openNote(id: String) { openNoteId = id; tab = Tab.NOTES }
    fun composeMail(d: MailDraft) { compose = d; tab = Tab.MAIL }
}

val LocalUi = compositionLocalOf<AppUi> { error("sin AppUi") }
val LocalServices = staticCompositionLocalOf<Services> { error("sin Services") }

/** Devuelve una función que conecta la cuenta de Google (mostrando la pantalla de permisos si hace falta). */
@Composable
fun rememberGoogleConnect(onDone: (Throwable?) -> Unit = {}): () -> Unit {
    val sv = LocalServices.current
    val ui = LocalUi.current
    val scope = rememberCoroutineScope()
    fun finish(e: Throwable?) { if (e != null) ui.fail(e) else ui.say("Cuenta de Google conectada"); onDone(e) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        scope.launch {
            val r = runCatching {
                if (res.resultCode != Activity.RESULT_OK) throw Exception("Inicio de sesión cancelado")
                sv.auth.fromIntent(res.data)
                sv.loadProfile()
            }
            finish(r.exceptionOrNull())
        }
    }
    return {
        scope.launch {
            try {
                sv.auth.token(); sv.loadProfile(); finish(null)
            } catch (e: ConsentRequired) {
                launcher.launch(IntentSenderRequest.Builder(e.pendingIntent.intentSender).build())
            } catch (e: Exception) {
                finish(e)
            }
        }
    }
}

@Composable
fun Centered(content: @Composable () -> Unit) =
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
    }

@Composable
fun Loading() = Centered { CircularProgressIndicator() }

@Composable
fun Hint(text: String) = Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

@Composable
fun ErrorBox(msg: String, onRetry: () -> Unit) = Centered {
    Hint(msg)
    Button(onClick = onRetry) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Reintentar") }
}

/** Muestra el contenido solo si hay cuenta de Google conectada; si no, el botón para conectarla. */
@Composable
fun GoogleGate(what: String, content: @Composable () -> Unit) {
    val sv = LocalServices.current
    val account by sv.settings.account.collectAsStateCompat()
    val connect = rememberGoogleConnect()
    if (account != null) content() else Centered {
        Text("Conecta tu cuenta de Google", style = MaterialTheme.typography.titleMedium)
        Hint("Para ver y gestionar $what necesito acceso a tu cuenta de Google.")
        Button(onClick = connect) { Text("Conectar con Google") }
    }
}

@Composable
fun Confirm(title: String, text: String, ok: String = "Aceptar", onDismiss: () -> Unit, onOk: () -> Unit) = AlertDialog(
    onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(text) },
    confirmButton = { TextButton(onClick = { onDismiss(); onOk() }) { Text(ok) } },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
)

@Composable
fun BusyDialog(label: String) = AlertDialog(
    onDismissRequest = {}, confirmButton = {},
    text = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(28.dp)); Spacer(Modifier.width(16.dp)); Text(label)
        }
    },
)

@Composable
fun VSpace(h: Int = 8) = Spacer(Modifier.height(h.dp))

@Composable
fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsStateCompat() = this.collectAsState()

fun Context.openUrl(url: String) = try {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
} catch (_: ActivityNotFoundException) {}

fun Context.fileUri(f: File): Uri = FileProvider.getUriForFile(this, "$packageName.files", f)

fun Context.viewFile(f: File, mime: String): Boolean = try {
    startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(fileUri(f), mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (_: ActivityNotFoundException) { false }

fun Context.shareText(text: String) = try {
    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Compartir").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
} catch (_: ActivityNotFoundException) {}

