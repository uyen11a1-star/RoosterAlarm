package com.example.roosteralarm

import android.content.Context
import org.json.JSONArray

class AlarmStore(context: Context) {
    private val p = context.getSharedPreferences("alarms", Context.MODE_PRIVATE)

    fun list(): MutableList<Alarm> {
        val raw = p.getString("list", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val out = mutableListOf<Alarm>()
        for (i in 0 until arr.length()) out.add(Alarm.fromJson(arr.getJSONObject(i)))
        return out
    }

    fun save(items: List<Alarm>) {
        val arr = JSONArray()
        items.forEach { arr.put(it.toJson()) }
        p.edit().putString("list", arr.toString()).apply()
    }

    fun add(a: Alarm) { val l = list(); l.add(a); save(l) }
    fun update(a: Alarm) {
        val l = list()
        val idx = l.indexOfFirst { it.id == a.id }
        if (idx >= 0) { l[idx] = a; save(l) }
    }
    fun delete(id: Int) { val l = list(); l.removeAll { it.id == id }; save(l) }
    fun nextId(): Int = (list().maxOfOrNull { it.id } ?: 0) + 1
}
