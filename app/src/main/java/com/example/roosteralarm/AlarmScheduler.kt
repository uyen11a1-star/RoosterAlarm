package com.example.roosteralarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object AlarmScheduler {

    fun schedule(context: Context, alarm: Alarm) {
        if (!alarm.enabled) return
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val trigger = nextTriggerMillis(alarm)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
        }
        val pi = PendingIntent.getBroadcast(
            context, alarm.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // setAlarmClock: chinh xac nhat, hien icon bao thuc tren status bar
                val showIntent = PendingIntent.getActivity(
                    context, alarm.id,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val info = AlarmManager.AlarmClockInfo(trigger, showIntent)
                am.setAlarmClock(info, pi)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, trigger, pi)
            }
        } catch (e: SecurityException) {
            // Fallback neu khong co quyen exact alarm
            am.set(AlarmManager.RTC_WAKEUP, trigger, pi)
        }
    }

    fun cancel(context: Context, alarmId: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, alarmId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
    }

    /** Snooze 5 phut sau ke tu bay gio */
    fun scheduleSnooze(context: Context, alarm: Alarm, delayMs: Long = 5 * 60 * 1000L) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val trigger = System.currentTimeMillis() + delayMs
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("is_snooze", true)
        }
        val pi = PendingIntent.getBroadcast(
            context, alarm.id + 10000, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
    }

    /** Tinh thoi diem ke tiep alarm se reo */
    fun nextTriggerMillis(alarm: Alarm): Long {
        val now = Calendar.getInstance()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (alarm.days.isEmpty()) {
            // Bao thuc 1 lan: neu gio da qua thi doi sang ngay mai
            if (cal.timeInMillis <= now.timeInMillis) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return cal.timeInMillis
        }

        // Lap theo ngay trong tuan
        for (offset in 0..7) {
            val trial = cal.clone() as Calendar
            trial.add(Calendar.DAY_OF_YEAR, offset)
            val dow = trial.get(Calendar.DAY_OF_WEEK)  // 1=Sun..7=Sat
            // Chuyen sang T2=1..CN=7
            val myDay = if (dow == Calendar.SUNDAY) 7 else dow - 1
            if (alarm.days.contains(myDay) && trial.timeInMillis > now.timeInMillis) {
                return trial.timeInMillis
            }
        }
        return cal.timeInMillis
    }
}
