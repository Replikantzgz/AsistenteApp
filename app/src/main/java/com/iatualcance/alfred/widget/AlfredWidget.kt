package com.iatualcance.alfred.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.RemoteViews
import com.iatualcance.alfred.AlfredApp
import com.iatualcance.alfred.MainActivity
import com.iatualcance.alfred.R
import com.iatualcance.alfred.notify.Notif
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Widget de la pantalla de inicio: 3/4 calendario del mes (una raya roja por cita, toque en un día = abrir ese día,
 * flechas para cambiar de mes) y 1/4 notas y tareas, con botones para dictar o crear una nota.
 */
class AlfredWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, views(ctx)) }
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            ACTION_PREV, ACTION_NEXT, ACTION_TODAY -> {
                val prefs = ctx.getSharedPreferences("widget", Context.MODE_PRIVATE)
                val off = when (intent.action) { ACTION_PREV -> prefs.getInt("monthOffset", 0) - 1; ACTION_NEXT -> prefs.getInt("monthOffset", 0) + 1; else -> 0 }
                prefs.edit().putInt("monthOffset", off.coerceIn(-12, 12)).apply()
                refresh(ctx)                                   // al instante, con lo que haya guardado
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {        // y después con los datos de ese mes
                    try { (ctx.applicationContext as AlfredApp).sv.refreshWidgetMonth() } finally { pending.finish() }
                }
            }
            else -> super.onReceive(ctx, intent)
        }
    }

    companion object {
        const val ACTION_PREV = "com.iatualcance.alfred.widget.PREV"
        const val ACTION_NEXT = "com.iatualcance.alfred.widget.NEXT"
        const val ACTION_TODAY = "com.iatualcance.alfred.widget.TODAY"

        private val CELLS = intArrayOf(R.id.cell_0, R.id.cell_1, R.id.cell_2, R.id.cell_3, R.id.cell_4, R.id.cell_5, R.id.cell_6, R.id.cell_7, R.id.cell_8, R.id.cell_9, R.id.cell_10, R.id.cell_11, R.id.cell_12, R.id.cell_13, R.id.cell_14, R.id.cell_15, R.id.cell_16, R.id.cell_17, R.id.cell_18, R.id.cell_19, R.id.cell_20, R.id.cell_21, R.id.cell_22, R.id.cell_23, R.id.cell_24, R.id.cell_25, R.id.cell_26, R.id.cell_27, R.id.cell_28, R.id.cell_29, R.id.cell_30, R.id.cell_31, R.id.cell_32, R.id.cell_33, R.id.cell_34, R.id.cell_35, R.id.cell_36, R.id.cell_37, R.id.cell_38, R.id.cell_39, R.id.cell_40, R.id.cell_41)
        private val title = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "ES"))

        private fun open(ctx: Context, route: String, code: Int) = PendingIntent.getActivity(
            ctx, code, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra(Notif.EXTRA_ROUTE, route),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        private fun action(ctx: Context, a: String, code: Int) = PendingIntent.getBroadcast(
            ctx, code, Intent(ctx, AlfredWidget::class.java).setAction(a), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        /** Citas por día del mes guardadas por la app: "AAAA-MM-DD=n,…". */
        private fun counts(ctx: Context, ym: YearMonth): Map<String, Int> =
            ctx.getSharedPreferences("widget", Context.MODE_PRIVATE).getString("cal:$ym", "").orEmpty().split(',').mapNotNull { p ->
                val kv = p.split('='); if (kv.size == 2) kv[0] to (kv[1].toIntOrNull() ?: 0) else null
            }.toMap()

        private fun views(ctx: Context): RemoteViews = RemoteViews(ctx.packageName, R.layout.widget_alfred).apply {
            val prefs = ctx.getSharedPreferences("widget", Context.MODE_PRIVATE)
            val today = LocalDate.now()
            val ym = YearMonth.from(today).plusMonths(prefs.getInt("monthOffset", 0).toLong())
            val byDay = counts(ctx, ym)
            setTextViewText(R.id.cal_title, ym.format(title).replaceFirstChar { it.uppercase() })
            setOnClickPendingIntent(R.id.cal_title, action(ctx, ACTION_TODAY, 30))
            setOnClickPendingIntent(R.id.cal_prev, action(ctx, ACTION_PREV, 31))
            setOnClickPendingIntent(R.id.cal_next, action(ctx, ACTION_NEXT, 32))

            val first = ym.atDay(1)
            val lead = first.dayOfWeek.value - 1
            for (i in 0 until 42) {
                val day = first.plusDays((i - lead).toLong())
                val id = CELLS[i]
                if (day.month != ym.month) { setTextViewText(id, ""); setInt(id, "setBackgroundResource", 0); continue }
                val n = byDay[day.toString()] ?: 0
                val isToday = day == today
                val sb = SpannableStringBuilder(day.dayOfMonth.toString())
                if (isToday) { sb.setSpan(StyleSpan(android.graphics.Typeface.BOLD), 0, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); sb.setSpan(ForegroundColorSpan(Color.parseColor("#D7301F")), 0, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
                sb.append('\n')
                val bars = "▬".repeat(minOf(n, 3)).ifEmpty { " " }
                val st = sb.length; sb.append(bars)
                sb.setSpan(ForegroundColorSpan(Color.parseColor("#D7301F")), st, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                setTextViewText(id, sb)
                setInt(id, "setBackgroundResource", if (isToday) R.drawable.widget_today_bg else 0)
                setOnClickPendingIntent(id, open(ctx, "dia:$day", 100 + i))
            }

            setTextViewText(R.id.widget_text, prefs.getString("notes", null) ?: "Aún no hay notas ni tareas.")
            setOnClickPendingIntent(R.id.widget_text, open(ctx, "notas", 10))
            setOnClickPendingIntent(R.id.widget_mic, open(ctx, "grabar", 12))
            setOnClickPendingIntent(R.id.widget_add, open(ctx, "nota", 13))
        }

        /** Repinta todos los widgets colocados. */
        fun refresh(ctx: Context) {
            val m = AppWidgetManager.getInstance(ctx)
            val ids = m.getAppWidgetIds(ComponentName(ctx, AlfredWidget::class.java))
            ids.forEach { m.updateAppWidget(it, views(ctx)) }
        }
    }
}
