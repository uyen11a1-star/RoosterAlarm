package com.example.roosteralarm

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat

class MainActivity : AppCompatActivity() {

    private lateinit var listContainer: LinearLayout
    private lateinit var emptyText: TextView
    private lateinit var store: AlarmStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        store = AlarmStore(this)

        listContainer = findViewById(R.id.listContainer)
        emptyText = findViewById(R.id.emptyText)
        val fab = findViewById<View>(R.id.fabAdd)
        fab.setOnClickListener {
            startActivity(Intent(this, AddAlarmActivity::class.java))
        }

        requestNeededPermissions()
    }

    override fun onResume() {
        super.onResume()
        renderList()
    }

    private fun requestNeededPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                } catch (_: Exception) {}
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                try {
                    startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
                } catch (_: Exception) {}
            }
        }
    }

    private fun renderList() {
        listContainer.removeAllViews()
        val items = store.list().sortedWith(compareBy({ it.hour }, { it.minute }))

        if (items.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            return
        }
        emptyText.visibility = View.GONE

        val inflater = LayoutInflater.from(this)
        items.forEach { alarm ->
            val row = inflater.inflate(R.layout.item_alarm, listContainer, false)
            row.findViewById<TextView>(R.id.tvTime).text = alarm.timeString()
            row.findViewById<TextView>(R.id.tvDays).text = alarm.daysShort()
            row.findViewById<TextView>(R.id.tvLabel).text = alarm.label
            val sw = row.findViewById<Switch>(R.id.swEnabled)
            sw.isChecked = alarm.enabled
            sw.setOnCheckedChangeListener { _, checked ->
                val updated = alarm.copy(enabled = checked)
                store.update(updated)
                if (checked) AlarmScheduler.schedule(this, updated)
                else AlarmScheduler.cancel(this, updated.id)
            }
            row.setOnClickListener {
                startActivity(Intent(this, AddAlarmActivity::class.java)
                    .putExtra("edit_id", alarm.id))
            }
            row.setOnLongClickListener {
                android.app.AlertDialog.Builder(this)
                    .setTitle("Xoá báo thức ${alarm.timeString()}?")
                    .setPositiveButton("Xoá") { _, _ ->
                        AlarmScheduler.cancel(this, alarm.id)
                        store.delete(alarm.id)
                        renderList()
                    }
                    .setNegativeButton("Huỷ", null)
                    .show()
                true
            }
            listContainer.addView(row)
        }
    }
}
