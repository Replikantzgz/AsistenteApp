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
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.iatualcance.alfred.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

// Tema claro de inspiración Pokémon GBC: blanco, rojo Pokédex, azul y amarillo.
val Paper = Color(0xFFF6F3EA)
val Surf = Color(0xFFFFFFFF)
val SurfHigh = Color(0xFFEDE9DC)
val Ink = Color(0xFF1F1D2B)
val Red = Color(0xFFE63B2E)
val Yellow = Color(0xFFFFCB05)
val Blue = Color(0xFF3B5BDB)
val Gold = Color(0xFFB07A00)

private val Colors = lightColorScheme(
    primary = Color(0xFFD7301F), onPrimary = Color.White, primaryContainer = Color(0xFFFFF0A8), onPrimaryContainer = Color(0xFF3B2A00),
    secondary = Blue, onSecondary = Color.White, secondaryContainer = Color(0xFFDCE4FF), onSecondaryContainer = Color(0xFF0E1F66),
    tertiary = Color(0xFF2F9E57), background = Paper, onBackground = Ink, surface = Paper, onSurface = Ink,
    surfaceVariant = SurfHigh, onSurfaceVariant = Color(0xFF5A5870), surfaceTint = Color.Transparent,
    surfaceContainer = Surf, surfaceContainerLow = Surf, surfaceContainerHigh = Surf, surfaceContainerHighest = SurfHigh, surfaceContainerLowest = Surf,
    outline = Color(0xFF7A7890), outlineVariant = Color(0xFFCFCABB), error = Color(0xFFB3261E), onError = Color.White,
    errorContainer = Color(0xFFFBDAD6), onErrorContainer = Color(0xFF5C1512),
)

private val PixelFont = FontFamily(Font(R.font.pressstart2p))

private val Type = Typography().let { t ->
    t.copy(
        titleLarge = t.titleLarge.copy(fontFamily = PixelFont, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
        titleMedium = t.titleMedium.copy(fontFamily = PixelFont, fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal),
        titleSmall = t.titleSmall.copy(fontFamily = PixelFont, fontSize = 10.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal),
    )
}

private val Pixel = Shapes(
    extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(6.dp), medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp), extraLarge = RoundedCornerShape(12.dp),
)

@Composable
fun AlfredTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = Colors, typography = Type, shapes = Pixel, content = content)

/** Orden de la barra inferior: Correo · Agenda · ALFRED · Notas · Contactos. */
enum class Tab { MAIL, AGENDA, CHAT, NOTES, CONTACTS }

data class MailDraft(val to: String = "", val subject: String = "", val body: String = "")

/** Estado de interfaz compartido entre pestañas. */
class AppUi(val snack: SnackbarHostState, private val scope: CoroutineScope) {
    var tab by mutableStateOf(Tab.CHAT)
    var busy by mutableStateOf<String?>(null)
    var openNoteId by mutableStateOf<String?>(null)
    var compose by mutableStateOf<MailDraft?>(null)
    /** Pantalla a pantalla completa por encima de las pestañas: "settings" o "drive". */
    var overlay by mutableStateOf<String?>(null)
    /** Subpestaña de Notas: 0 notas, 1 tareas, 2 plantillas. */
    var notesSub by mutableIntStateOf(0)
    var calendarDate by mutableStateOf<java.time.LocalDate?>(null)
    /** Acción pendiente que ejecuta la pantalla de Notas al abrirse: "scan" o "record". */
    var pending by mutableStateOf<String?>(null)

    fun say(msg: String) { scope.launch { snack.currentSnackbarData?.dismiss(); snack.showSnackbar(msg) } }

    fun fail(e: Throwable) { if (e is com.iatualcance.alfred.QuotaExceeded) overlay = "premium" else failMsg(e) }

    private fun failMsg(e: Throwable) = say(if (e is ConsentRequired) "Conecta tu cuenta de Google en Ajustes (engranaje de la pantalla de Alfred)" else (e.message ?: e.toString()))

    /** Ejecuta una operación mostrando "Trabajando…" y enseñando el error si falla. */
    fun run(label: String, block: suspend () -> Unit) {
        scope.launch {
            busy = label
            try { block() } catch (e: CancellationException) { throw e } catch (e: Exception) { fail(e) } finally { busy = null }
        }
    }

    fun openNote(id: String) { overlay = null; notesSub = 0; openNoteId = id; tab = Tab.NOTES }
    fun composeMail(d: MailDraft) { overlay = null; compose = d; tab = Tab.MAIL }

    /** Lo que Alfred (el asistente) le pide a la interfaz. */
    fun navigate(n: com.iatualcance.alfred.ai.Nav) {
        when (n) {
            is com.iatualcance.alfred.ai.Nav.Screen -> {
                overlay = null
                when (n.name) {
                    "correo" -> { compose = null; tab = Tab.MAIL }
                    "calendario" -> tab = Tab.AGENDA
                    "chat" -> tab = Tab.CHAT
                    "contactos" -> tab = Tab.CONTACTS
                    "notas" -> { openNoteId = null; notesSub = 0; tab = Tab.NOTES }
                    "tareas" -> { openNoteId = null; notesSub = 1; tab = Tab.NOTES }
                    "plantillas" -> { openNoteId = null; notesSub = 2; tab = Tab.NOTES }
                    "drive" -> overlay = "drive"
                    "ajustes" -> overlay = "settings"
                }
            }
            is com.iatualcance.alfred.ai.Nav.Note -> openNote(n.id)
            is com.iatualcance.alfred.ai.Nav.Compose -> composeMail(MailDraft(n.to, n.subject, n.body))
            is com.iatualcance.alfred.ai.Nav.Calendar -> { overlay = null; calendarDate = n.date; tab = Tab.AGENDA }
            com.iatualcance.alfred.ai.Nav.Scan -> { overlay = null; openNoteId = null; notesSub = 0; pending = "scan"; tab = Tab.NOTES }
            com.iatualcance.alfred.ai.Nav.Record -> { overlay = null; openNoteId = null; notesSub = 0; pending = "record"; tab = Tab.NOTES }
        }
    }
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

/** Pantalla vacía con la ilustración de la sección. */
@Composable
fun ArtHint(@androidx.annotation.DrawableRes art: Int, text: String) = Centered { Art(art, 132.dp); Hint(text) }

/** Muestra el contenido solo si hay cuenta de Google conectada; si no, el botón para conectarla. */
@Composable
fun GoogleGate(what: String, @androidx.annotation.DrawableRes art: Int = com.iatualcance.alfred.R.drawable.art_pikachu, content: @Composable () -> Unit) {
    val sv = LocalServices.current
    val account by sv.settings.account.collectAsStateCompat()
    val connect = rememberGoogleConnect()
    if (account != null) content() else Centered {
        Art(art, 132.dp)
        Text("Conecta tu cuenta de Google", style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
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

