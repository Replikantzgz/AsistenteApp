package com.iatualcance.alfred.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.iatualcance.alfred.MainActivity
import com.iatualcance.alfred.R
import com.iatualcance.alfred.notify.Notif

/** Barra horizontal 4×1: el asistente a la izquierda y Correo, Agenda y Notas, que abren una persiana encima de la pantalla de inicio. */
class BarWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, views(ctx)) }
    }

    private fun panel(ctx: Context, which: String, code: Int) = PendingIntent.getActivity(
        ctx, code, Intent(ctx, PanelActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra(PanelActivity.EXTRA_PANEL, which),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun views(ctx: Context) = RemoteViews(ctx.packageName, R.layout.widget_bar).apply {
        setOnClickPendingIntent(R.id.bar_agent, PendingIntent.getActivity(
            ctx, 20, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra(Notif.EXTRA_ROUTE, "chat"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        ))
        setOnClickPendingIntent(R.id.bar_mail, panel(ctx, "mail", 21))
        setOnClickPendingIntent(R.id.bar_agenda, panel(ctx, "agenda", 22))
        setOnClickPendingIntent(R.id.bar_notes, panel(ctx, "notes", 23))
    }
}
