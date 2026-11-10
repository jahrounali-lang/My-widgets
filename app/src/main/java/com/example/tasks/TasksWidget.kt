package com.example.tasks

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews

class TasksWidget : AppWidgetProvider() {

    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(c, m, it) }
    }

    override fun onAppWidgetOptionsChanged(c: Context, m: AppWidgetManager, id: Int, o: Bundle) {
        m.notifyAppWidgetViewDataChanged(id, R.id.list) // re-read size -> text size
        update(c, m, id)
    }

    override fun onReceive(c: Context, i: Intent) {
        if (i.action == ACTION_TOGGLE) {
            TaskStore.toggle(c, i.getLongExtra(EXTRA_ID, -1))
            refresh(c)
        } else super.onReceive(c, i)
    }

    companion object {
        const val ACTION_TOGGLE = "com.example.tasks.TOGGLE"
        const val EXTRA_ID = "task_id"

        /** Call after any change to tasks or style. */
        fun refresh(c: Context) {
            val m = AppWidgetManager.getInstance(c)
            val ids = m.getAppWidgetIds(ComponentName(c, TasksWidget::class.java))
            if (ids.isEmpty()) return
            m.notifyAppWidgetViewDataChanged(ids, R.id.list)
            ids.forEach { update(c, m, it) }
        }

        fun update(c: Context, m: AppWidgetManager, id: Int) {
            val g = TaskStore.cfg(c)
            val v = RemoteViews(c.packageName, R.layout.widget)

            val svc = Intent(c, TasksWidgetService::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            svc.data = Uri.parse(svc.toUri(Intent.URI_INTENT_SCHEME))
            v.setRemoteAdapter(R.id.list, svc)
            v.setEmptyView(R.id.list, R.id.empty)

            // Background: opaque tint + image alpha => alpha 0 is truly transparent
            v.setInt(R.id.bg, "setColorFilter", 0xFF000000.toInt() or g.rgb)
            v.setInt(R.id.bg, "setImageAlpha", g.alpha * 255 / 100)
            v.setTextColor(R.id.add, g.textColor)
            v.setTextColor(R.id.empty, g.textColor)

            val toggle = PendingIntent.getBroadcast(
                c, 0, Intent(c, TasksWidget::class.java).setAction(ACTION_TOGGLE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            v.setPendingIntentTemplate(R.id.list, toggle)

            val open = PendingIntent.getActivity(
                c, 1,
                Intent(c, MainActivity::class.java).putExtra("add", true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            v.setOnClickPendingIntent(R.id.add, open)
            m.updateAppWidget(id, v)
        }
    }
}
