package com.iatualcance.alfred.widget

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iatualcance.alfred.AlfredApp
import com.iatualcance.alfred.MainActivity
import com.iatualcance.alfred.billing.Feature
import com.iatualcance.alfred.notify.Notif
import com.iatualcance.alfred.ui.AlfredTheme
import com.iatualcance.alfred.ui.Ink
import com.iatualcance.alfred.ui.Red
import com.iatualcance.alfred.ui.Surf
import com.iatualcance.alfred.ui.SoftLine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * La "persiana": una pantalla transparente que baja desde arriba sobre la pantalla de inicio con la bandeja de entrada,
 * la agenda de hoy o las notas, según el botón de la barra que se haya pulsado. Tocar fuera la sube.
 */
class PanelActivity : ComponentActivity() {
    companion object { const val EXTRA_PANEL = "panel" }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val which = intent.getStringExtra(EXTRA_PANEL) ?: "mail"
        val sv = (application as AlfredApp).sv
        setContent { AlfredTheme { Persiana(which, sv, onClose = { finish(); overridePendingTransition(0, 0) }, onOpen = { route -> openApp(route) }) } }
    }

    private fun openApp(route: String) {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra(Notif.EXTRA_ROUTE, route))
        finish(); overridePendingTransition(0, 0)
    }
}

private data class Row3(val title: String, val sub: String, val side: String = "", val strong: Boolean = false)

@Composable
private fun Persiana(which: String, sv: com.iatualcance.alfred.Services, onClose: () -> Unit, onOpen: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var shown by remember { mutableStateOf(false) }
    var rows by remember { mutableStateOf<List<Row3>?>(null) }
    var note by remember { mutableStateOf<String?>(null) }
    val title = when (which) { "agenda" -> "AGENDA · HOY"; "notes" -> "NOTAS Y TAREAS"; else -> "CORREO" }
    val route = when (which) { "agenda" -> "calendario"; "notes" -> "notas"; else -> "correo" }
    val hm = DateTimeFormatter.ofPattern("HH:mm")

    fun close() { scope.launch { shown = false; delay(260); onClose() } }

    LaunchedEffect(Unit) {
        shown = true
        try {
            rows = when (which) {
                "mail" -> if (!sv.hasGoogle) { note = "Conecta tu cuenta de Google en Alfred para ver aquí el correo."; emptyList() } else
                    sv.google.listMail("in:inbox", 8).map { m -> Row3(m.from.substringBefore('<').trim().trim('"').ifBlank { m.from }, m.subject.ifBlank { "(sin asunto)" }, "", m.unread) }
                "agenda" -> if (!sv.hasGoogle) { note = "Conecta tu cuenta de Google en Alfred para ver aquí la agenda."; emptyList() } else {
                    val zone = ZoneId.systemDefault(); val d = ZonedDateTime.now(zone).toLocalDate()
                    sv.google.listEvents(d.atStartOfDay(zone), d.plusDays(1).atStartOfDay(zone)).map { e -> Row3(e.title, e.location, if (e.allDay) "Todo el día" else e.start.format(hm)) }
                }
                else -> if (!sv.allows(Feature.NOTES)) { note = "Las notas y tareas son del plan Premium."; emptyList() } else {
                    val tasks = sv.store.tasks.value.filter { !it.done }.sortedBy { it.due ?: "9999" }.take(5).map { Row3("☐ " + it.title, it.due ?: "", "", false) }
                    val notes = sv.store.notes.value.sortedWith(compareByDescending<com.iatualcance.alfred.data.Note> { it.pinned }.thenByDescending { it.updatedAt }).take(5).map { Row3(it.title.ifBlank { "Sin título" }, it.content.take(60), "", it.pinned) }
                    tasks + notes
                }
            }
            if (rows.isNullOrEmpty() && note == null) note = when (which) { "mail" -> "La bandeja de entrada está vacía."; "agenda" -> "No tienes nada hoy."; else -> "Aún no hay notas ni tareas." }
        } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { rows = emptyList(); note = e.message ?: "No he podido cargarlo." }
    }

    Box(Modifier.fillMaxSize().background(Color(0x66000000)).clickable(remember { MutableInteractionSource() }, null) { close() }) {
        AnimatedVisibility(shown, enter = expandVertically(tween(260), expandFrom = Alignment.Top), exit = shrinkVertically(tween(220), shrinkTowards = Alignment.Top)) {
            Column(
                Modifier.fillMaxWidth().clickable(remember { MutableInteractionSource() }, null) {}
                    .background(Surf, RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp)).statusBarsPadding().padding(bottom = 10.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onOpen(route) }) { Text("Abrir en Alfred", color = Red, fontWeight = FontWeight.Bold) }
                }
                Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        rows == null -> Row(Modifier.padding(20.dp)) { CircularProgressIndicator(Modifier.width(22.dp), strokeWidth = 2.dp) }
                        else -> {
                            note?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp)) }
                            rows!!.forEach { r ->
                                Row(
                                    Modifier.fillMaxWidth().clickable { onOpen(route) }.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(r.title, fontWeight = if (r.strong) FontWeight.Bold else FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Ink)
                                        if (r.sub.isNotBlank()) Text(r.sub, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (r.side.isNotBlank()) { Spacer(Modifier.width(8.dp)); Text(r.side, style = MaterialTheme.typography.labelLarge, color = Red) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
