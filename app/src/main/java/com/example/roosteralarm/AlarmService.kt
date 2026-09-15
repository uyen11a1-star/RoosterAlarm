package com.example.roosteralarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    private var audioTrack: AudioTrack? = null
    private var playThread: Thread? = null
    @Volatile private var running = false

    private var alarmId: Int = -1

    companion object {
        const val CHANNEL_ID = "rooster_alarm_channel"
        const val NOTIF_ID = 9001
        var isRunning = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        alarmId = intent?.getIntExtra("alarm_id", -1) ?: -1
        if (alarmId < 0) { stopSelf(); return START_NOT_STICKY }

        val store = AlarmStore(this)
        val alarm = store.list().firstOrNull { it.id == alarmId }
        val kind = alarm?.soundKind ?: RoosterSynth.KIND_ROOSTER
        val label = alarm?.label?.takeIf { it.isNotEmpty() } ?: "Báo thức gà gáy"
        val time = alarm?.timeString() ?: ""

        // Foreground notification
        val fullIntent = Intent(this, AlarmRingActivity::class.java).apply {
            putExtra("alarm_id", alarmId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pi = PendingIntent.getActivity(
            this, alarmId, fullIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("🐓 $time  $label")
            .setContentText("Nhấn để mở và tắt báo thức")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notif,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIF_ID, notif)
        }

        isRunning = true
        startPlaying(kind)
        return START_STICKY
    }

    private fun startPlaying(kind: Int) {
        stopPlaying()
        running = true
        val pcm = RoosterSynth.synthesize(kind)
        playThread = Thread {
            try {
                val minBuf = AudioTrack.getMinBufferSize(
                    RoosterSynth.SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufSize = maxOf(minBuf, pcm.size * 2)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(RoosterSynth.SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                audioTrack = track

                // Tang am luong toi da
                track.setStereoVolume(1f, 1f)

                track.play()
                while (running) {
                    track.write(pcm, 0, pcm.size)
                }
                try { track.stop() } catch (_: Exception) {}
                try { track.release() } catch (_: Exception) {}
                audioTrack = null
            } catch (_: Exception) { }
        }.apply { priority = Thread.MAX_PRIORITY; start() }
    }

    private fun stopPlaying() {
        running = false
        try { playThread?.join(500) } catch (_: Exception) {}
        playThread = null
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID, "Báo thức gà gáy",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Kênh phát tiếng gà khi đến giờ"
                    setSound(null, null)         // tat sound mac dinh, ta tu phat
                    enableVibration(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                nm.createNotificationChannel(ch)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        stopPlaying()
        try { stopForeground(true) } catch (_: Exception) {}
    }
}
