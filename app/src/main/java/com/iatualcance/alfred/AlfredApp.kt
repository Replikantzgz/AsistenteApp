package com.iatualcance.alfred

import android.app.Application
import android.net.Uri
import com.iatualcance.alfred.ai.Assistant
import com.iatualcance.alfred.ai.Gemini
import com.iatualcance.alfred.data.Attachment
import com.iatualcance.alfred.data.Note
import com.iatualcance.alfred.data.Settings
import com.iatualcance.alfred.data.Store
import com.iatualcance.alfred.google.GoogleApi
import com.iatualcance.alfred.google.GoogleAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AlfredApp : Application() {
    lateinit var sv: Services
    override fun onCreate() {
        super.onCreate()
        sv = Services(this)
    }
}

/** Todo lo que comparten las pantallas: datos, Google, Gemini y las operaciones que los combinan. */
class Services(private val app: Application) {
    val store = Store(app)
    val settings = Settings(app)
    val auth = GoogleAuth(app)
    val google = GoogleApi(auth)
    val gemini = Gemini(settings)
    val assistant = Assistant(gemini, google, store, settings, uploadNote = { uploadNote(it) }, backup = { backupToDrive() })

    val hasAi get() = settings.apiKey.value.isNotBlank()
    val hasGoogle get() = settings.account.value != null

    suspend fun loadProfile() = settings.setAccount(google.profile())

    suspend fun disconnect() {
        settings.account.value?.let { auth.revoke(it.email) } ?: auth.invalidate()
        google.forgetCache()
        settings.setAccount(null)
    }

    private fun stamp() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Util.ES))

    private suspend fun bytes(a: Attachment) = withContext(Dispatchers.IO) { store.fileOf(a).readBytes() }

    /** Sube la nota (texto + adjuntos) a la carpeta Alfred de Drive. Las facturas y los escaneos van a su subcarpeta. */
    suspend fun uploadNote(note: Note): Note {
        val sub = when {
            note.tags.any { it.equals("factura", true) } -> "Facturas"
            note.tags.any { it.equals("escaneo", true) } -> "Escaneos"
            else -> "Notas"
        }
        val parent = google.folder(sub)
        val base = Util.safeFileName(note.title.ifBlank { "Nota ${stamp()}" })
        val atts = note.attachments.map { a ->
            if (a.driveLink != null) a
            else a.copy(driveLink = google.upload(bytes(a), "$base - ${a.label}", a.mime, parent).link ?: "")
        }
        var fileId = note.driveFileId
        if (note.content.isNotBlank() || atts.isEmpty()) {
            val text = buildString {
                append(note.title).append("\n\n").append(note.content)
                if (note.tags.isNotEmpty()) append("\n\nEtiquetas: ").append(note.tags.joinToString(", "))
            }
            fileId = google.upload(text.toByteArray(), "$base.txt", "text/plain", parent, note.driveFileId).id
        }
        return store.upsertNote(note.copy(attachments = atts, driveFileId = fileId))
    }

    private suspend fun autoUpload(note: Note): Note =
        if (hasGoogle && settings.driveAuto.value) runCatching { uploadNote(note) }.getOrDefault(note) else note

    /** Documento escaneado (o PDF/imagen compartida) → nota con el fichero adjunto y, si hay IA, sus datos extraídos. */
    suspend fun documentToNote(files: List<Pair<Uri, String>>, into: Note? = null): Note = withContext(Dispatchers.IO) {
        val atts = files.map { (uri, mime) ->
            store.importUri(uri, mime, if (mime == "application/pdf") "Documento.pdf" else "Imagen.jpg")
        }
        var note = (into ?: Note(title = "Escaneo ${stamp()}", tags = listOf("escaneo")))
        note = note.copy(attachments = note.attachments + atts)
        val forAi = atts.firstOrNull { it.mime.startsWith("image/") } ?: atts.firstOrNull()
        if (hasAi && forAi != null) runCatching {
            val info = assistant.analyzeDocument(bytes(forAi), forAi.mime)
            val tags = (note.tags + if (info.isInvoice) listOf("factura") else emptyList()).distinct()
            note = note.copy(
                title = if (into == null || note.title.isBlank()) info.title else note.title,
                content = listOf(note.content, info.body).filter { it.isNotBlank() }.joinToString("\n\n"),
                tags = tags,
            )
        }
        autoUpload(store.upsertNote(note))
    }

    /** Audio grabado, elegido o compartido → nota con la transcripción y el audio adjunto. */
    suspend fun audioToNote(att: Attachment, into: Note? = null): Note {
        val text = gemini.transcribe(bytes(att), att.mime)
        val note = into ?: Note(title = "Nota de voz ${stamp()}", tags = listOf("audio"))
        val has = note.attachments.any { it.id == att.id }
        return store.upsertNote(note.copy(
            content = listOf(note.content, text).filter { it.isNotBlank() }.joinToString("\n\n"),
            attachments = if (has) note.attachments else note.attachments + att,
        ))
    }

    suspend fun backupToDrive(): String {
        val f = google.upload(store.exportAll().toByteArray(), "Copia de seguridad Alfred.json", "application/json", google.folder(null), backupId())
        return f.name
    }

    private suspend fun backupId(): String? =
        google.listFiles(google.folder(null)).firstOrNull { it.name == "Copia de seguridad Alfred.json" }?.id

    fun wipe() { store.deleteEverything(); settings.clear() }
}
