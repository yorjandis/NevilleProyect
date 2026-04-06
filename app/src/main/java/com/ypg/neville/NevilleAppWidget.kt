package com.ypg.neville

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class NevilleAppWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            appWidgetManager.updateAppWidget(widgetId, buildRemoteViews(context))
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        refreshAllWidgets(context)
    }

    companion object {
        fun refreshAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, NevilleAppWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(component)
            if (appWidgetIds.isEmpty()) return

            val views = buildRemoteViews(context)
            appWidgetIds.forEach { id ->
                appWidgetManager.updateAppWidget(id, views)
            }
        }

        private fun buildRemoteViews(context: Context): RemoteViews {
            val openAppIntent = Intent(context, MainActivity::class.java)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getActivity(context, 0, openAppIntent, flags)

            return RemoteViews(context.packageName, R.layout.neville_widget).apply {
                setTextViewText(R.id.widget_text_home, context.getString(R.string.neville))
                setTextViewText(R.id.widget_text, context.getString(R.string.widget_text_default))
                setOnClickPendingIntent(R.id.widget_text, pendingIntent)
                setOnClickPendingIntent(R.id.widget_text_home, pendingIntent)
            }
        }
    }
}

