package com.example.roosteralarm

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class AddAlarmActivity : AppCompatActivity() {

    private lateinit var picker: TimePicker
    private lateinit var chipGroup: ChipGroup
    private lateinit var labelEdit: EditText
    private lateinit var soundBtn: Button

    private var soundKind = 0
    private var editId = -1
    private var previewTrack: AudioTrack? = null
    private var previewThread: Thread? = null
    @Volatile private var previewRunning = false

    private val dayMap = listOf(
        1 to "T2", 2 to "T3", 3 to "T4", 4 to "T5",
        5 to "T6", 6 to "T7", 7 to "CN"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_alarm)

        picker = findViewById(R.id.timePicker)
        picker.setIs24HourView(true)
        chipGroup = findViewById(R.id.chipGroup)
        labelEdit = findViewById(R.id.editLabel)
        soundBtn = findViewById(R.id.btnSound)

        // Tao 7 chip ngay trong tuan
        dayMap.forEach { (day, name) ->
            val chip = Chip(this).apply {
                text = name
                isCheckable = true
                tag = day
            }
            chipGroup.addView(chip)
        }

        // Nut chon kieu tieng ga
        soundBtn.setOnClickListener { showSoundChooser() }

        // Load du lieu neu la edit
        editId = intent.getIntExtra("edit_id", -1)
        if (editId >= 0) {
            val alarm = AlarmStore(this).list().firstOrNull { it.id == editId }
            if (alarm != null) {
                picker.hour = alarm.hour
                picker.minute = alarm.minute
                labelEdit.setText(alarm.label)
                soundKind = alarm.soundKind
                updateSoundBtn()
                alarm.days.forEach { day ->
                    for (i in 0 until chipGroup.childCount) {
                        val c = chipGroup.getChildAt(i) as Chip
                        if (c.tag == day) { c.isChecked = true; break }
                    }
                }
            }
        } else {
            updateSoundBtn()
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener { save() }
        findViewById<Button>(R.id.btnCancel).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnDelete).setOnClickListener {
            if (editId >= 0) {
                AlarmScheduler.cancel(this, editId)
                AlarmStore(this).delete(editId)
                finish()
            }
        }
        findViewById<Button>(R.id.btnDelete).visibility =
            if (editId >= 0) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun updateSoundBtn() {
        soundBtn.text = when (soundKind) {
            1 -> "🔊 Tiếng: Gà mái (cục cục)"
            2 -> "🔊 Tiếng: Gà con (chíp chíp)"
            else -> "🔊 Tiếng: Gà trống (ò ó o o o)"
        }
    }

    private fun showSoundChooser() {
        val names = arrayOf(
            "🐓 Gà trống (ò ó o o o)",
            "🐔 Gà mái (cục cục)",
            "🐤 Gà con (chíp chíp)"
        )
        android.app.AlertDialog.Builder(this)
            .setTitle("Chọn tiếng gà")
            .setSingleChoiceItems(names, soundKind) { d, which ->
                soundKind = which
                updateSoundBtn()
                playPreview(soundKind)
                d.dismiss()
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    private fun playPreview(kind: Int) {
        stopPreview()
        previewRunning = true
        val pcm = RoosterSynth.synthesize(kind)
        previewThread = Thread {
            try {
                val minBuf = AudioTrack.getMinBufferSize(
                    RoosterSynth.SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val track = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                    .setAudioFormat(AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RoosterSynth.SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                    .setBufferSizeInBytes(maxOf(minBuf, pcm.size * 2))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                previewTrack = track
                track.play()
                var loops = 0
                while (previewRunning && loops < 3) {
                    track.write(pcm, 0, pcm.size)
                    loops++
                }
                try { track.stop(); track.release() } catch (_: Exception) {}
                previewTrack = null
            } catch (_: Exception) {}
        }.apply { start() }
    }

    private fun stopPreview() {
        previewRunning = false
        try { previewThread?.join(500) } catch (_: Exception) {}
        previewThread = null
    }

    private fun save() {
        val days = mutableSetOf<Int>()
        for (i in 0 until chipGroup.childCount) {
            val c = chipGroup.getChildAt(i) as Chip
            if (c.isChecked) days.add(c.tag as Int)
        }
        val hour = picker.hour
        val minute = picker.minute
        val label = labelEdit.text.toString().trim()

        val store = AlarmStore(this)
        if (editId >= 0) {
            val old = store.list().firstOrNull { it.id == editId } ?: return
            val updated = old.copy(hour = hour, minute = minute, days = days,
                label = label, soundKind = soundKind)
            store.update(updated)
            AlarmScheduler.cancel(this, updated.id)
            if (updated.enabled) AlarmScheduler.schedule(this, updated)
        } else {
            val a = Alarm(
                id = store.nextId(),
                hour = hour, minute = minute,
                days = days, enabled = true,
                label = label, soundKind = soundKind
            )
            store.add(a)
            AlarmScheduler.schedule(this, a)
        }
        Toast.makeText(this, "Đã lưu báo thức", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onPause() {
        super.onPause()
        stopPreview()
    }
}
