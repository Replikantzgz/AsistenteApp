package com.iatualcance.alfred.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.iatualcance.alfred.MainActivity
import com.iatualcance.alfred.R
import com.iatualcance.alfred.notify.Notif

/** Widget de la pantalla de inicio: próximas citas y tareas, con botones para dictar una nota o abrir el asistente. */
class AlfredWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, views(ctx)) }
    }

    companion object {
        private fun open(ctx: Context, route: String, code: Int) = PendingIntent.getActivity(
            ctx, code, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra(Notif.EXTRA_ROUTE, route),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        private fun views(ctx: Context): RemoteViews = RemoteViews(ctx.packageName, R.layout.widget_alfred).apply {
            val text = ctx.getSharedPreferences("widget", Context.MODE_PRIVATE).getString("text", null)
            setTextViewText(R.id.widget_text, text ?: "Abre Alfred para ver aquí tu agenda y tus tareas.")
            setOnClickPendingIntent(R.id.widget_text, open(ctx, "chat", 10))
            setOnClickPendingIntent(R.id.widget_title, open(ctx, "chat", 11))
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
