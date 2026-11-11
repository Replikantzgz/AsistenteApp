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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class QuotaExceeded : Exception("Has agotado los mensajes gratis de hoy. Con Premium no hay límite.")

class AlfredApp : Application() {
    lateinit var sv: Services
    override fun onCreate() {
        super.onCreate()
        sv = Services(this)
        com.iatualcance.alfred.notify.Notif.ensureChannel(this)
        scheduleRefresh()
        sv.start()
    }

    /** Lectura periódica del calendario para programar avisos aunque la app no se abra. */
    fun scheduleRefresh() {
        val wm = androidx.work.WorkManager.getInstance(this)
        wm.enqueueUniquePeriodicWork(
            "refresh-reminders", androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            androidx.work.PeriodicWorkRequestBuilder<com.iatualcance.alfred.notify.RefreshWorker>(3, java.util.concurrent.TimeUnit.HOURS)
                .setConstraints(androidx.work.Constraints.Builder().setRequiredNetworkType(androidx.work.NetworkType.CONNECTED).build()).build(),
        )
        wm.enqueueUniqueWork(
            "refresh-reminders-now", androidx.work.ExistingWorkPolicy.REPLACE,
            androidx.work.OneTimeWorkRequestBuilder<com.iatualcance.alfred.notify.RefreshWorker>()
                .setConstraints(androidx.work.Constraints.Builder().setRequiredNetworkType(androidx.work.NetworkType.CONNECTED).build()).build(),
        )
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

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.Default)
    private val reminders = com.iatualcance.alfred.notify.ReminderCenter(app)
    private val remindersLock = kotlinx.coroutines.sync.Mutex()
    @Volatile private var lastFull = 0L

    /** Vigila las tareas y los ajustes de avisos para reprogramar al instante. */
    fun start() {
        scope.launch { store.tasks.collectLatest { delay(400); refreshReminders(false) } }
        scope.launch {
            combine(settings.notifyEvents, settings.notifyTasks, settings.leadMin, settings.account) { a, b, c, d -> listOf(a, b, c, d) }
                .drop(1).collectLatest { delay(400); refreshReminders(true) }
        }
    }

    /**
     * Recalcula y programa todos los avisos: tareas (siempre) y citas de Calendar de los próximos 14 días.
     * Si no se puede leer el calendario (sin conexión), se conservan los avisos de citas que ya había.
     */
    suspend fun refreshReminders(events: Boolean = true) = withContext(Dispatchers.Default) {
        remindersLock.withLock {
            val now = System.currentTimeMillis()
            val zone = java.time.ZoneId.systemDefault()
            val plan = mutableListOf<com.iatualcance.alfred.notify.Reminder>()
            if (settings.notifyTasks.value) store.tasks.value.mapNotNullTo(plan) { com.iatualcance.alfred.notify.Plan.taskReminder(it, now, zone) }
            val wantEvents = settings.notifyEvents.value && hasGoogle
            var fresh: List<com.iatualcance.alfred.notify.Reminder>? = null
            if (wantEvents && (events || now - lastFull > 60_000)) {
                fresh = runCatching {
                    val from = java.time.ZonedDateTime.now(zone)
                    google.listEvents(from, from.plusDays(14)).mapNotNull { com.iatualcance.alfred.notify.Plan.eventReminder(it, settings.leadMin.value, now) }
                }.getOrNull()
                if (fresh != null) lastFull = now
            }
            when {
                !wantEvents -> {}
                fresh != null -> plan += fresh
                else -> plan += reminders.load().filter { it.id.startsWith("ev-") && it.at > now }   // sin conexión: se conservan
            }
            reminders.apply(plan.sortedBy { it.at })
        }
    }

    // ---- Premium (apagado mientras BillingConfig.ENABLED sea false: todo ilimitado)
    /** Licencia válida y vigente, o null. */
    fun licenseData(code: String = settings.license.value): com.iatualcance.alfred.billing.LicenseData? =
        (com.iatualcance.alfred.billing.Licenses.verify(code) as? com.iatualcance.alfred.billing.LicenseResult.Valid)?.data

    fun isPremium(code: String = settings.license.value): Boolean = !com.iatualcance.alfred.billing.BillingConfig.ENABLED || licenseData(code) != null

    /** Devuelve null si el código se ha activado, o el motivo por el que no vale. */
    fun activateLicense(code: String): String? = when (val r = com.iatualcance.alfred.billing.Licenses.verify(code)) {
        is com.iatualcance.alfred.billing.LicenseResult.Valid -> { settings.setLicense(code.filterNot { it.isWhitespace() }); null }
        is com.iatualcance.alfred.billing.LicenseResult.Invalid -> r.reason
    }

    fun aiLeftToday(): Int = (com.iatualcance.alfred.billing.BillingConfig.FREE_AI_PER_DAY - settings.aiUsedToday()).coerceAtLeast(0)

    /** Ejecuta una operación con IA descontándola de la cuota diaria si el usuario no es Premium. */
    suspend fun <T> metered(block: suspend () -> T): T {
        if (!isPremium() && aiLeftToday() <= 0) throw QuotaExceeded()
        val r = block()
        if (com.iatualcance.alfred.billing.BillingConfig.ENABLED && !isPremium()) settings.addAiUse()
        return r
    }

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
            val info = metered { assistant.analyzeDocument(bytes(forAi), forAi.mime) }
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
        val text = metered { gemini.transcribe(bytes(att), att.mime) }
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
