package com.iatualcance.alfred.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.iatualcance.alfred.AlfredApp
import com.iatualcance.alfred.BuildConfig
import com.iatualcance.alfred.R
import com.iatualcance.alfred.notify.Notif
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class NeedsPermission(val info: UpdateInfo) : UpdateState
    data class Downloading(val info: UpdateInfo, val percent: Int) : UpdateState
    data class Installing(val info: UpdateInfo) : UpdateState
    data class Failed(val message: String) : UpdateState
}

/**
 * Actualizaciones automáticas. Lee update.json de la última versión publicada en GitHub Releases; si es más nueva,
 * descarga el APK, comprueba su huella y se lo entrega al instalador de Android. Android solo deja instalar si el APK
 * está firmado con la misma clave que la app instalada, así que una versión ajena nunca se instala.
 */
class Updater(private val ctx: Context, private val http: okhttp3.OkHttpClient) {
    val state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    private val lock = Mutex()
    private val dir get() = File(ctx.cacheDir, "updates").apply { mkdirs() }

    fun canInstall(): Boolean = ctx.packageManager.canRequestPackageInstalls()

    /** Abre la pantalla de Android donde se concede «instalar apps» a Alfred. */
    fun openInstallPermission() {
        ctx.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Busca una versión nueva. Con [install] la descarga y lanza el instalador. Devuelve la versión nueva, o null si ya está al día. */
    suspend fun check(install: Boolean, quiet: Boolean = false): UpdateInfo? = lock.withLock {
        try {
            state.value = UpdateState.Checking
            val info = fetch() ?: run { state.value = UpdateState.Idle; return null }
            if (!info.isNewerThan(BuildConfig.VERSION_CODE)) { state.value = UpdateState.Idle; return null }
            if (!install) { state.value = UpdateState.Available(info); return info }
            if (!canInstall()) { state.value = UpdateState.NeedsPermission(info); return info }
            val apk = download(info)
            state.value = UpdateState.Installing(info)
            install(apk)
            info
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            state.value = if (quiet) UpdateState.Idle else UpdateState.Failed(e.message ?: e.toString())
            null
        }
    }

    private suspend fun fetch(): UpdateInfo? = withContext(Dispatchers.IO) {
        http.newCall(Request.Builder().url(BuildConfig.UPDATE_URL).build()).execute().use { r ->
            if (r.code == 404) return@withContext null            // todavía no hay ninguna versión publicada
            if (!r.isSuccessful) throw Exception("No se pudo consultar las actualizaciones (${r.code})")
            UpdateInfo.parse(r.body?.string().orEmpty())
        }
    }

    private suspend fun download(info: UpdateInfo): File = withContext(Dispatchers.IO) {
        dir.listFiles()?.forEach { it.delete() }
        val out = File(dir, "Alfred-${info.versionCode}.apk")
        val md = MessageDigest.getInstance("SHA-256")
        state.value = UpdateState.Downloading(info, 0)
        http.newCall(Request.Builder().url(info.url).build()).execute().use { r ->
            if (!r.isSuccessful) throw Exception("No se pudo descargar la actualización (${r.code})")
            val body = r.body ?: throw Exception("Descarga vacía")
            val total = body.contentLength()
            var done = 0L; var last = -1
            body.byteStream().use { input ->
                out.outputStream().use { o ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf); if (n < 0) break
                        o.write(buf, 0, n); md.update(buf, 0, n); done += n
                        if (total > 0) { val p = (done * 100 / total).toInt(); if (p != last) { last = p; state.value = UpdateState.Downloading(info, p) } }
                    }
                }
            }
        }
        val sha = md.digest().joinToString("") { "%02x".format(it) }
        if (info.sha256 != null && !sha.equals(info.sha256, true)) { out.delete(); throw Exception("La descarga está dañada. Vuelve a intentarlo.") }
        out
    }

    private fun install(apk: File) {
        val installer = ctx.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(ctx.packageName)
            // Android 12+: si Alfred ya instaló la app antes por este mismo camino, la actualización puede ser silenciosa.
            if (Build.VERSION.SDK_INT >= 31) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val id = installer.createSession(params)
        installer.openSession(id).use { s ->
            s.openWrite("alfred.apk", 0, apk.length()).use { out -> apk.inputStream().use { it.copyTo(out) }; s.fsync(out) }
            val cb = PendingIntent.getBroadcast(
                ctx, id, Intent(ctx, UpdateInstallReceiver::class.java).setAction("alfred.UPDATE_RESULT"),
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            s.commit(cb.intentSender)
        }
    }

    /** Borra descargas antiguas. Se llama al arrancar. */
    fun cleanup() { dir.listFiles()?.filter { (it.name.substringAfter('-').substringBefore('.').toIntOrNull() ?: 0) <= BuildConfig.VERSION_CODE }?.forEach { it.delete() } }
}

/** Respuesta del instalador de Android: pide la confirmación al usuario si hace falta, o informa del error. */
class UpdateInstallReceiver : BroadcastReceiver() {
    @android.annotation.SuppressLint("MissingPermission")   // se comprueba con Notif.canPost
    override fun onReceive(ctx: Context, i: Intent) {
        val updater = (ctx.applicationContext as AlfredApp).sv.updater
        when (val status = i.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = (if (Build.VERSION.SDK_INT >= 33) i.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java) else i.getParcelableExtra(Intent.EXTRA_INTENT)) ?: return
                runCatching { ctx.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                // Si Android no deja abrir la pantalla desde segundo plano, queda un aviso para tocar.
                Notif.ensureChannel(ctx)
                val pi = PendingIntent.getActivity(ctx, 77, confirm, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                if (Notif.canPost(ctx)) runCatching {
                    androidx.core.app.NotificationManagerCompat.from(ctx).notify(
                        7701, NotificationCompat.Builder(ctx, Notif.CHANNEL).setSmallIcon(R.drawable.ic_stat_alfred)
                            .setContentTitle("Actualización de Alfred lista").setContentText("Toca para instalar la nueva versión.")
                            .setAutoCancel(true).setContentIntent(pi).setPriority(NotificationCompat.PRIORITY_HIGH).build(),
                    )
                }
            }
            PackageInstaller.STATUS_SUCCESS -> updater.state.value = UpdateState.Idle
            PackageInstaller.STATUS_FAILURE_ABORTED -> updater.state.value = UpdateState.Idle   // el usuario canceló
            else -> {
                val msg = i.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty()
                updater.state.value = UpdateState.Failed(
                    if (status == PackageInstaller.STATUS_FAILURE_INCOMPATIBLE || "signature" in msg.lowercase() || "INSTALL_FAILED_UPDATE_INCOMPATIBLE" in msg)
                        "La actualización está firmada con otra clave y no puede sustituir a esta app. Hay que instalarla a mano una vez."
                    else "No se pudo instalar la actualización: $msg",
                )
            }
        }
    }
}
