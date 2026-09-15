package com.example.roosteralarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("alarm_id", -1)
        val isSnooze = intent.getBooleanExtra("is_snooze", false)
        if (id < 0) return

        val store = AlarmStore(context)
        val alarm = store.list().firstOrNull { it.id == id } ?: return

        val svcIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("alarm_id", id)
            putExtra("is_snooze", isSnooze)
        }
        ContextCompat.startForegroundService(context, svcIntent)

        if (!isSnooze) {
            // Schedule next occurrence neu la lap
            if (alarm.days.isNotEmpty()) {
                AlarmScheduler.schedule(context, alarm)
            }
        }
    }
}
