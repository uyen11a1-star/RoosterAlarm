package com.example.roosteralarm

import org.json.JSONArray
import org.json.JSONObject

data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val days: Set<Int>,           // 1=Mon ... 7=Sun (theo Calendar.DAY_OF_WEEK)
    val enabled: Boolean,
    val label: String,
    val soundKind: Int            // 0=rooster, 1=hen, 2=chick
) {
    fun daysShort(): String {
        if (days.isEmpty()) return "Một lần"
        if (days.size == 7) return "Hằng ngày"
        val names = mapOf(1 to "T2", 2 to "T3", 3 to "T4", 4 to "T5",
            5 to "T6", 6 to "T7", 7 to "CN")
        return days.sorted().joinToString(" ") { names[it] ?: "" }
    }

    fun timeString(): String = "%02d:%02d".format(hour, minute)

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("hour", hour); put("minute", minute)
        put("enabled", enabled); put("label", label); put("soundKind", soundKind)
        val arr = JSONArray(); days.sorted().forEach { arr.put(it) }
        put("days", arr)
    }

    companion object {
        fun fromJson(o: JSONObject): Alarm {
            val set = mutableSetOf<Int>()
            val arr = o.optJSONArray("days") ?: JSONArray()
            for (i in 0 until arr.length()) set.add(arr.getInt(i))
            return Alarm(
                id = o.getInt("id"),
                hour = o.getInt("hour"),
                minute = o.getInt("minute"),
                days = set,
                enabled = o.optBoolean("enabled", true),
                label = o.optString("label", ""),
                soundKind = o.optInt("soundKind", 0)
            )
        }
    }
}
