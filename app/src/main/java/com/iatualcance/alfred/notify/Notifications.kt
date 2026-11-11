package com.iatualcance.alfred.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.iatualcance.alfred.AlfredApp
import com.iatualcance.alfred.MainActivity
import com.iatualcance.alfred.R
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

object Notif {
    const val CHANNEL = "recordatorios"
    const val EXTRA_ROUTE = "route"

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Recordatorios y citas", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Avisos de tareas y de citas de tu calendario"
                    enableVibration(true)
                }
            )
        }
    }

    fun canPost(ctx: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            androidx.core.app.NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    @android.annotation.SuppressLint("MissingPermission")   // canPost() comprueba el permiso justo debajo
    fun show(ctx: Context, r: Reminder) {
        if (!canPost(ctx)) return
        ensureChannel(ctx)
        val open = PendingIntent.getActivity(
            ctx, r.id.hashCode(),
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra(EXTRA_ROUTE, r.route),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_alfred).setContentTitle(r.title).setContentText(r.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(r.text))
            .setCategory(if (r.taskId != null) NotificationCompat.CATEGORY_REMINDER else NotificationCompat.CATEGORY_EVENT)
            .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).setContentIntent(open)
            .setColor(0xFFB07A00.toInt()).setWhen(r.at).setShowWhen(true)
        if (r.taskId != null) {
            val done = PendingIntent.getBroadcast(
                ctx, r.id.hashCode() + 1,
                Intent(ctx, TaskDoneReceiver::class.java).setAction("alfred.TASK_DONE.${r.id}").putExtra("taskId", r.taskId).putExtra("notifId", r.id.hashCode()),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            n.addAction(0, "Hecho", done)
        }
        runCatching { androidx.core.app.NotificationManagerCompat.from(ctx).notify(r.id.hashCode(), n.build()) }
    }
}

/** Guarda los avisos pendientes y los programa en el AlarmManager (suenan con la app cerrada). */
class ReminderCenter(private val ctx: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val file = File(File(ctx.filesDir, "datos").apply { mkdirs() }, "recordatorios.json")
    private val alarms = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun load(): List<Reminder> = runCatching { json.decodeFromString(ListSerializer(Reminder.serializer()), file.readText()) }.getOrDefault(emptyList())

    @Synchronized
    private fun save(list: List<Reminder>) { file.writeText(json.encodeToString(ListSerializer(Reminder.serializer()), list)) }

    private fun pi(r: Reminder) = PendingIntent.getBroadcast(
        ctx, r.id.hashCode(),
        Intent(ctx, ReminderReceiver::class.java).setAction("alfred.REMINDER.${r.id}")
            .putExtra("id", r.id).putExtra("at", r.at).putExtra("title", r.title).putExtra("text", r.text)
            .putExtra("route", r.route).putExtra("taskId", r.taskId).putExtra("kind", r.kind),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun schedule(r: Reminder) {
        if (r.at <= System.currentTimeMillis()) return
        val p = pi(r)
        try {
            if (Build.VERSION.SDK_INT >= 31 && !alarms.canScheduleExactAlarms()) alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r.at, p)
            else alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r.at, p)
        } catch (_: SecurityException) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r.at, p)
        }
    }

    /** Sustituye todos los avisos por [list]: cancela los que ya no existen y programa los nuevos o cambiados. */
    fun apply(list: List<Reminder>) {
        val old = load().associateBy { it.id }
        val new = list.associateBy { it.id }
        old.values.filter { new[it.id] != it }.forEach { alarms.cancel(pi(it)) }
        list.filter { old[it.id] != it }.forEach { schedule(it) }
        save(list)
    }

    fun remove(id: String) = save(load().filterNot { it.id == id })

    /** Tras reiniciar el móvil las alarmas se pierden: se vuelven a programar desde el fichero. */
    fun rescheduleAll() = load().filter { it.at > System.currentTimeMillis() }.also { save(it) }.forEach { schedule(it) }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        val r = Reminder(
            id = i.getStringExtra("id") ?: return, at = i.getLongExtra("at", System.currentTimeMillis()),
            title = i.getStringExtra("title").orEmpty(), text = i.getStringExtra("text").orEmpty(),
            route = i.getStringExtra("route") ?: "calendario", taskId = i.getStringExtra("taskId"), kind = i.getStringExtra("kind"),
        )
        // El resumen del día se prepara al sonar, con los datos de ese momento.
        if (r.kind == "summary") androidx.work.WorkManager.getInstance(ctx).enqueue(androidx.work.OneTimeWorkRequestBuilder<SummaryWorker>().build())
        else Notif.show(ctx, r)
        ReminderCenter(ctx).remove(r.id)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        if (i.action == Intent.ACTION_BOOT_COMPLETED || i.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            ReminderCenter(ctx).rescheduleAll()
            (ctx.applicationContext as? AlfredApp)?.scheduleRefresh()
        }
    }
}

/** Botón "Hecho" de la notificación de una tarea. */
class TaskDoneReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        val id = i.getStringExtra("taskId") ?: return
        val sv = (ctx.applicationContext as AlfredApp).sv
        sv.store.tasks.value.firstOrNull { it.id == id }?.let { sv.store.upsertTask(it.copy(done = true)) }
        androidx.core.app.NotificationManagerCompat.from(ctx).cancel(i.getIntExtra("notifId", 0))
    }
}

/** Cada pocas horas vuelve a leer el calendario para programar los avisos de los próximos días. */
class RefreshWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        (applicationContext as AlfredApp).sv.refreshReminders(true)
        return Result.success()
    }
}

/** Prepara y muestra el resumen de la mañana, y programa el de mañana. */
class SummaryWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val sv = (applicationContext as AlfredApp).sv
        val (title, text) = sv.buildSummary()
        Notif.show(applicationContext, Reminder("daily-show", System.currentTimeMillis(), title, text, "chat"))
        sv.refreshReminders(true)
        return Result.success()
    }
}
