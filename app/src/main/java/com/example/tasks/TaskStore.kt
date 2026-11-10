package com.example.tasks

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Task(val id: Long, val text: String, val done: Boolean)

/** rgb: background color (no alpha), alpha: 0..100 (0 = fully transparent) */
data class Cfg(val rgb: Int, val alpha: Int, val darkText: Boolean) {
    val textColor: Int get() = if (darkText) 0xFF101114.toInt() else 0xFFFFFFFF.toInt()
}

object TaskStore {
    private fun p(c: Context) = c.getSharedPreferences("tasks", Context.MODE_PRIVATE)

    private fun raw(c: Context): MutableList<Task> {
        val a = JSONArray(p(c).getString("list", "[]"))
        return MutableList(a.length()) {
            val o = a.getJSONObject(it)
            Task(o.getLong("id"), o.getString("t"), o.getBoolean("d"))
        }
    }

    private fun write(c: Context, l: List<Task>) {
        val a = JSONArray()
        l.forEach { a.put(JSONObject().put("id", it.id).put("t", it.text).put("d", it.done)) }
        p(c).edit().putString("list", a.toString()).apply()
    }

    /** Undone first, done below. */
    fun all(c: Context): List<Task> = raw(c).let { l -> l.filter { !it.done } + l.filter { it.done } }

    fun add(c: Context, text: String) {
        if (text.isBlank()) return
        write(c, raw(c) + Task(System.currentTimeMillis(), text.trim(), false))
    }

    fun toggle(c: Context, id: Long) {
        val l = raw(c)
        val t = l.find { it.id == id } ?: return
        l.remove(t)
        l.add(t.copy(done = !t.done))
        write(c, l)
    }

    fun edit(c: Context, id: Long, text: String) {
        if (text.isBlank()) return
        write(c, raw(c).map { if (it.id == id) it.copy(text = text.trim()) else it })
    }

    fun delete(c: Context, id: Long) = write(c, raw(c).filter { it.id != id })

    fun cfg(c: Context) = p(c).let { Cfg(it.getInt("rgb", 0xFFFFFF), it.getInt("alpha", 30), it.getBoolean("dark", true)) }

    fun saveCfg(c: Context, g: Cfg) {
        p(c).edit().putInt("rgb", g.rgb).putInt("alpha", g.alpha).putBoolean("dark", g.darkText).apply()
    }
}
