package com.example.roosteralarm

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AlarmRingActivity : AppCompatActivity() {

    private var alarmId = -1
    private var answer = 0
    private lateinit var questionText: TextView
    private lateinit var answerEdit: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hien tren man hinh khoa
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_ring)

        alarmId = intent.getIntExtra("alarm_id", -1)
        val store = AlarmStore(this)
        val alarm = store.list().firstOrNull { it.id == alarmId }

        findViewById<TextView>(R.id.tvTime).text = alarm?.timeString() ?: "--:--"
        findViewById<TextView>(R.id.tvLabel).text = alarm?.label?.takeIf { it.isNotEmpty() }
            ?: "Dậy thôi! Gà gáy rồi 🐓"

        questionText = findViewById(R.id.tvQuestion)
        answerEdit = findViewById(R.id.editAnswer)
        generateQuestion()

        findViewById<Button>(R.id.btnStop).setOnClickListener {
            val input = answerEdit.text.toString().trim().toIntOrNull()
            if (input == null) {
                Toast.makeText(this, "Nhập số", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (input == answer) {
                stopAlarm()
                finish()
            } else {
                Toast.makeText(this, "Sai rồi, thử lại!", Toast.LENGTH_SHORT).show()
                answerEdit.text.clear()
                generateQuestion()
            }
        }

        findViewById<Button>(R.id.btnSnooze).setOnClickListener {
            snooze()
            finish()
        }
    }

    private fun generateQuestion() {
        val a = (10..50).random()
        val b = (2..9).random()
        answer = a + b
        questionText.text = "$a + $b = ?"
    }

    private fun stopAlarm() {
        stopService(android.content.Intent(this, AlarmService::class.java))
        val store = AlarmStore(this)
        val alarm = store.list().firstOrNull { it.id == alarmId }
        // Neu la bao thuc 1 lan thi tu tat sau khi reo
        if (alarm != null && alarm.days.isEmpty()) {
            val updated = alarm.copy(enabled = false)
            store.update(updated)
        }
    }

    private fun snooze() {
        stopService(android.content.Intent(this, AlarmService::class.java))
        val alarm = AlarmStore(this).list().firstOrNull { it.id == alarmId } ?: return
        AlarmScheduler.scheduleSnooze(this, alarm, 5 * 60 * 1000L)
        Toast.makeText(this, "Snooze 5 phút 🐓", Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        // Chan nguoi dung back ra ngoai
    }
}
