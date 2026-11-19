package dev.ngale.thirds

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Home-screen widget. The host updates at most every 30 min via
 * updatePeriodMillis, so we own a wall-clock alarm aligned to
 * :00/:15/:30/:45 (docs/spec.md) and redraw through ThirdsTickReceiver.
 */
class ThirdsWidgetReceiver : android.appwidget.AppWidgetProvider() {

    override fun onUpdate(
        context: Context, manager: AppWidgetManager, ids: IntArray,
    ) {
        for (id in ids) updateWidget(context, manager, id)
        scheduleQuarterAlarm(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context, manager: AppWidgetManager, id: Int,
        newOptions: android.os.Bundle,
    ) {
        updateWidget(context, manager, id) // size changed → re-bitmap
    }

    override fun onEnabled(context: Context) = scheduleQuarterAlarm(context)

    override fun onDisabled(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(tickIntent(context))
    }

    companion object {
        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val opts = manager.getAppWidgetOptions(id)
            val minWidth = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val minHeight = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            if (minWidth <= 0 || minHeight <= 0) return
            val density = context.resources.displayMetrics.density
            val views = RemoteViews(context.packageName, R.layout.thirds_bar)
            val now = java.time.LocalTime.now()
            val bmp = BarRenderer.render(
                context,
                thirdState(now.hour, now.minute),
                (minWidth * density).toInt(),
                (minHeight * density).toInt(),
            )
            views.setImageViewBitmap(R.id.bar, bmp)
            manager.updateAppWidget(id, views)
        }

        fun widgetIds(context: Context): IntArray =
            context.getSystemService(AppWidgetManager::class.java)
                .getAppWidgetIds(ComponentName(context, ThirdsWidgetReceiver::class.java))
    }
}

const val TICK_ACTION = "dev.ngale.thirds.action.TICK"

private fun tickIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
    context, 0,
    Intent(context, ThirdsTickReceiver::class.java).setAction(TICK_ACTION),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)

private fun scheduleQuarterAlarm(context: Context) {
    val now = LocalDateTime.now()
    val next = now.withMinute(now.minute / 15 * 15).withSecond(0).withNano(0)
        .plusMinutes(15) // next :00/:15/:30/:45 boundary (rolls over hours/days)
    val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    context.getSystemService(AlarmManager::class.java)
        .setRepeating(AlarmManager.RTC, millis, 15 * 60_000L, tickIntent(context))
}
