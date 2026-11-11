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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.runtime.CompositionLocalProvider
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
val Paper = Color(0xFFF4F5F9)
val Surf = Color(0xFFFFFFFF)
val SurfHigh = Color(0xFFEEF0F6)
val Ink = Color(0xFF1F1D2B)
val Red = Color(0xFFE63B2E)
val Yellow = Color(0xFFFFCB05)
val Blue = Color(0xFF3B6FE0)
val Gold = Color(0xFFB07A00)

private val Colors = lightColorScheme(
    primary = Color(0xFFD7301F), onPrimary = Color.White, primaryContainer = Color(0xFFFFF0A8), onPrimaryContainer = Color(0xFF3B2A00),
    secondary = Blue, onSecondary = Color.White, secondaryContainer = Color(0xFFE4ECFF), onSecondaryContainer = Color(0xFF0E1F66),
    tertiary = Color(0xFF2F9E57), background = Paper, onBackground = Ink, surface = Paper, onSurface = Ink,
    surfaceVariant = SurfHigh, onSurfaceVariant = Color(0xFF5A5870), surfaceTint = Color.Transparent,
    surfaceContainer = Surf, surfaceContainerLow = Surf, surfaceContainerHigh = Surf, surfaceContainerHighest = SurfHigh, surfaceContainerLowest = Surf,
    outline = Color(0xFF7A7890), outlineVariant = Color(0xFFD7DBE8), error = Color(0xFFB3261E), onError = Color.White,
    errorContainer = Color(0xFFFBDAD6), onErrorContainer = Color(0xFF5C1512),
)

/** Jerarquía tipográfica: sans-serif limpia, con pesos y tamaños escalonados. */
private val Type = Typography().let { t ->
    fun st(base: androidx.compose.ui.text.TextStyle, size: Int, lh: Int, w: FontWeight, ls: Double = 0.0) =
        base.copy(fontFamily = FontFamily.SansSerif, fontSize = size.sp, lineHeight = lh.sp, fontWeight = w, letterSpacing = ls.sp)
    t.copy(
        headlineSmall = st(t.headlineSmall, 22, 28, FontWeight.Bold),
        titleLarge = st(t.titleLarge, 20, 26, FontWeight.ExtraBold, 0.6),
        titleMedium = st(t.titleMedium, 16, 22, FontWeight.Bold),
        titleSmall = st(t.titleSmall, 14, 20, FontWeight.Bold, 0.2),
        bodyLarge = st(t.bodyLarge, 16, 23, FontWeight.Normal),
        bodyMedium = st(t.bodyMedium, 14, 21, FontWeight.Normal),
        bodySmall = st(t.bodySmall, 12, 17, FontWeight.Normal),
        labelLarge = st(t.labelLarge, 14, 20, FontWeight.SemiBold, 0.1),
        labelMedium = st(t.labelMedium, 12, 16, FontWeight.SemiBold, 0.3),
        labelSmall = st(t.labelSmall, 11, 15, FontWeight.Medium, 0.3),
    )
}

private val Pixel = Shapes(
    extraSmall = RoundedCornerShape(6.dp), small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp), extraLarge = RoundedCornerShape(20.dp),
)

@Composable
fun AlfredTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = Colors, typography = Type, shapes = Pixel, content = content)

/** Orden de la barra inferior: Correo · Agenda · ALFRED · Notas · Contactos. */
enum class Tab { MAIL, AGENDA, CHAT, NOTES, DRIVE }

data class MailDraft(val to: String = "", val subject: String = "", val body: String = "")

/** Contenido que otra app ha compartido con Alfred, a la espera de decidir qué hacer con él. */
data class SharedDraft(val intent: android.content.Intent, val uri: Uri?, val mime: String, val text: String?)

/** Adjunto o texto que se deja preparado en el cuadro de escritura del chat. */
data class ChatDraft(val uri: Uri?, val text: String)

/** Estado de interfaz compartido entre pestañas. */
class AppUi(val snack: SnackbarHostState, private val scope: CoroutineScope) {
    var tab by mutableStateOf(Tab.CHAT)
    var busy by mutableStateOf<String?>(null)
    var openNoteId by mutableStateOf<String?>(null)
    var compose by mutableStateOf<MailDraft?>(null)
    /** Pantalla a pantalla completa por encima de las pestañas: "settings" o "drive". */
    var overlay by mutableStateOf<String?>(null)
    /** Subpestaña de Correo: 0 correo, 1 contactos. */
    var mailSub by mutableIntStateOf(0)
    /** Subpestaña de Notas: 0 notas, 1 tareas, 2 plantillas. */
    var notesSub by mutableIntStateOf(0)
    var calendarDate by mutableStateOf<java.time.LocalDate?>(null)
    /** Correo abierto en la pestaña Correo (lo usan la búsqueda y el resto de pantallas). */
    var mailOpenId by mutableStateOf<String?>(null)
    var contactQuery by mutableStateOf("")
    var shared by mutableStateOf<SharedDraft?>(null)
    var chatDraft by mutableStateOf<ChatDraft?>(null)
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
    fun composeMail(d: MailDraft) { overlay = null; mailSub = 0; compose = d; tab = Tab.MAIL }

    /** Lo que Alfred (el asistente) le pide a la interfaz. */
    fun navigate(n: com.iatualcance.alfred.ai.Nav) {
        when (n) {
            is com.iatualcance.alfred.ai.Nav.Screen -> {
                overlay = null
                when (n.name) {
                    "correo" -> { compose = null; mailSub = 0; tab = Tab.MAIL }
                    "calendario" -> tab = Tab.AGENDA
                    "chat" -> tab = Tab.CHAT
                    "contactos" -> { compose = null; mailOpenId = null; mailSub = 1; tab = Tab.MAIL }
                    "notas" -> { openNoteId = null; notesSub = 0; tab = Tab.NOTES }
                    "tareas" -> { openNoteId = null; notesSub = 1; tab = Tab.NOTES }
                    "plantillas" -> { openNoteId = null; notesSub = 2; tab = Tab.NOTES }
                    "drive" -> tab = Tab.DRIVE
                    "ajustes" -> overlay = "settings"
                    "buscar" -> overlay = "search"
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
fun Hint(text: String) = Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)

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



// ----- Componentes comunes: mismas proporciones en toda la app
private val BtnShape = RoundedCornerShape(14.dp)

@Composable
fun AppButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) =
    androidx.compose.material3.Button(
        onClick, modifier.heightIn(min = 46.dp), enabled, BtnShape,
        elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 1.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 10.dp), content = content,
    )

@Composable
fun AppOutlinedButton(
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) = androidx.compose.material3.OutlinedButton(
    onClick, modifier.heightIn(min = 46.dp), enabled, BtnShape,
    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
    border = androidx.compose.foundation.BorderStroke(1.5.dp, if (enabled) Color(0xFFB9BFD3) else SoftLine), contentPadding = contentPadding, content = content,
)

/** Botón cuadrado redondeado con degradado rojo (enviar, micrófono). */
@Composable
fun AppIconButton(onClick: () -> Unit, enabled: Boolean = true, content: @Composable () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Box(
        Modifier.size(52.dp).shadow(4.dp, BtnShape, clip = false, ambientColor = Color(0x33C92A1F), spotColor = Color(0x55C92A1F))
            .background(Brush.verticalGradient(if (enabled) listOf(Color(0xFFF2473B), Color(0xFFCB2B20)) else listOf(Color(0xFFBDBDC8), Color(0xFFA9A9B6))), BtnShape)
            .clickable(src, null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides Color.White) { content() } }
}

/** Chip en forma de pastilla, blanco con sombra suave. */
@Composable
fun PillChip(text: String, onClick: () -> Unit, enabled: Boolean = true) = androidx.compose.material3.AssistChip(
    onClick = onClick, enabled = enabled, label = { Text(text, fontWeight = FontWeight.SemiBold) }, shape = RoundedCornerShape(50),
    colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(containerColor = Color.White),
    border = androidx.compose.foundation.BorderStroke(1.5.dp, SoftLine),
    elevation = androidx.compose.material3.AssistChipDefaults.assistChipElevation(elevation = 2.dp),
)

/** Confirmación para acciones graves: hay que escribir la palabra indicada. */
@Composable
fun ConfirmTyped(title: String, text: String, word: String = "BORRAR", onDismiss: () -> Unit, onOk: () -> Unit) {
    var typed by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text)
                androidx.compose.material3.OutlinedTextField(typed, { typed = it }, singleLine = true, shape = PxShape, label = { Text("Escribe $word para confirmar") })
            }
        },
        confirmButton = { TextButton(enabled = typed.trim().equals(word, true), onClick = { onDismiss(); onOk() }) { Text(word, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** Comparte un fichero (JSON de copia de datos) con otra app. */
fun Context.shareFile(f: File, mime: String) = try {
    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, fileUri(f)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Exportar").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
} catch (_: ActivityNotFoundException) {}
