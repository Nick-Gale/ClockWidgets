package dev.ngale.thirds

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fired by the quarter-hour alarm; redraws every placed widget. */
class ThirdsTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TICK_ACTION) return
        val manager = context.getSystemService(AppWidgetManager::class.java)
        for (id in ThirdsWidgetReceiver.widgetIds(context)) {
            ThirdsWidgetReceiver.updateWidget(context, manager, id)
        }
    }
}
