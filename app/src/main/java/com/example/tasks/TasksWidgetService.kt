package com.example.tasks

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class TasksWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(i: Intent): RemoteViewsFactory =
        Factory(applicationContext, i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0))

    class Factory(private val c: Context, private val wid: Int) : RemoteViewsFactory {
        private var items = listOf<Task>()
        private var sp = 14f
        private var color = Color.BLACK

        override fun onCreate() {}
        override fun onDestroy() {}

        override fun onDataSetChanged() {
            items = TaskStore.all(c)
            color = TaskStore.cfg(c).textColor
            val h = AppWidgetManager.getInstance(c).getAppWidgetOptions(wid)
                .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 150)
            sp = when {
                h < 130 -> 12.5f
                h < 260 -> 14f
                else -> 15.5f
            }
        }

        override fun getCount() = items.size

        override fun getViewAt(p: Int): RemoteViews {
            val t = items[p]
            val v = RemoteViews(c.packageName, R.layout.widget_item)
            val s = SpannableString(t.text)
            if (t.done) s.setSpan(StrikethroughSpan(), 0, s.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            v.setTextViewText(R.id.label, s)
            v.setTextViewTextSize(R.id.label, TypedValue.COMPLEX_UNIT_SP, sp)
            v.setTextColor(R.id.label, if (t.done) (color and 0x00FFFFFF) or 0x80000000.toInt() else color)
            v.setInt(R.id.outline, "setColorFilter", color)
            v.setViewVisibility(R.id.ok, if (t.done) View.VISIBLE else View.INVISIBLE)
            v.setOnClickFillInIntent(R.id.box, Intent().putExtra(TasksWidget.EXTRA_ID, t.id))
            return v
        }

        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount() = 1
        override fun getItemId(p: Int) = items[p].id
        override fun hasStableIds() = true
    }
}
