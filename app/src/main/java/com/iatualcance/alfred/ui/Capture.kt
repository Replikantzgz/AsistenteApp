package com.iatualcance.alfred.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.delay
import java.io.File

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }
    return null
}

/** Escáner de documentos de Google (ML Kit): detecta bordes, recorta y devuelve PDF + imagen. */
@Composable
fun rememberScanner(onResult: (List<Pair<Uri, String>>) -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val ui = LocalUi.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val r = GmsDocumentScanningResult.fromActivityResultIntent(res.data)
            val files = buildList {
                r?.pdf?.let { add(it.uri to "application/pdf") }
                r?.pages?.firstOrNull()?.let { add(it.imageUri to "image/jpeg") }
            }
            if (files.isNotEmpty()) onResult(files)
        }
    }
    return {
        val act = ctx.findActivity()
        if (act == null) ui.say("No se pudo abrir el escáner") else {
            val opts = GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true).setPageLimit(20)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG, GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL).build()
            GmsDocumentScanning.getClient(opts).getStartScanIntent(act)
                .addOnSuccessListener { launcher.launch(IntentSenderRequest.Builder(it).build()) }
                .addOnFailureListener { ui.say("No se pudo abrir el escáner: ${it.message}") }
        }
    }
}

/** Pide permiso de micrófono y graba. Devuelve una función que abre la grabadora. */
@Composable
fun rememberRecorder(newFile: () -> File, onDone: (File) -> Unit): () -> Unit {
    val ui = LocalUi.current
    var recording by remember { mutableStateOf(false) }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) recording = true else ui.say("Sin permiso de micrófono no puedo grabar. Actívalo en los ajustes del móvil.")
    }
    if (recording) RecordDialog(newFile, onCancel = { recording = false }) { recording = false; onDone(it) }
    return { perm.launch(Manifest.permission.RECORD_AUDIO) }
}

@Composable
private fun RecordDialog(newFile: () -> File, onCancel: () -> Unit, onDone: (File) -> Unit) {
    val ctx = LocalContext.current
    val file = remember { newFile() }
    var seconds by remember { mutableIntStateOf(0) }
    var failed by remember { mutableStateOf<String?>(null) }
    val rec = remember {
        @Suppress("DEPRECATION")
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(ctx) else MediaRecorder()
        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioSamplingRate(16000); r.setAudioChannels(1); r.setAudioEncodingBitRate(32000)
            r.setOutputFile(file.absolutePath)
            r.prepare(); r.start()
        } catch (e: Exception) { failed = e.message ?: "No se pudo iniciar la grabación" }
        r
    }
    var stopped by remember { mutableStateOf(false) }
    fun stop(): Boolean {
        if (stopped) return false
        stopped = true
        val ok = runCatching { rec.stop() }.isSuccess
        runCatching { rec.release() }
        return ok && failed == null && file.length() > 0
    }
    DisposableEffect(Unit) { onDispose { if (!stopped) { stop(); file.delete() } } }
    LaunchedEffect(Unit) { while (true) { delay(1000); seconds++ } }

    AlertDialog(
        onDismissRequest = {},
        title = { Text("Grabando…") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("%02d:%02d".format(seconds / 60, seconds % 60), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                Text(failed ?: "Hable con normalidad. Al terminar, Alfred lo transcribe.")
            }
        },
        confirmButton = { TextButton(onClick = { if (stop()) onDone(file) else { file.delete(); onCancel() } }) { Text("Terminar") } },
        dismissButton = { TextButton(onClick = { stop(); file.delete(); onCancel() }) { Text("Cancelar") } },
    )
}

/** Botón de reproducir/pausar un audio guardado. */
@Composable
fun PlayButton(file: File, modifier: Modifier = Modifier) {
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember { mutableStateOf(false) }
    DisposableEffect(file) { onDispose { runCatching { player?.release() }; player = null } }
    IconButton(modifier = modifier, onClick = {
        val p = player
        if (p != null && playing) { p.pause(); playing = false }
        else if (p != null) { p.start(); playing = true }
        else runCatching {
            MediaPlayer().apply {
                setDataSource(file.absolutePath); prepare()
                setOnCompletionListener { playing = false; it.seekTo(0) }
                start()
            }
        }.onSuccess { player = it; playing = true }
    }) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (playing) "Pausar" else "Reproducir") }
}
