package ca.uqac.c8INF865.test

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.IBinder
import androidx.core.app.NotificationCompat

class AlarmSoundService : Service() {
    private var mediaPlayer: MediaPlayer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val reveilId = intent?.getStringExtra("REVEIL_ID") ?: "default_id"
        val sonId = intent?.getStringExtra("SON_ID")
        val channelId = "$CHANNEL_ID-${sonId ?: "default"}"
        createChannel(channelId)
        startForeground(NOTIFICATION_ID(reveilId), buildNotification(reveilId, channelId))

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            val localFile = sonId?.let { AlarmSoundCache.localFile(this@AlarmSoundService, it) }
            if (localFile != null && localFile.exists() && localFile.length() > 0) {
                setDataSource(localFile.absolutePath)
            } else {
                setDataSource(this@AlarmSoundService, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            }
            isLooping = true
            prepare()
            start()
        }
        return START_NOT_STICKY
    }

    private fun createChannel(channelId: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(channelId, "Alarm", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(true)
            }
        )
    }

    private fun buildNotification(reveilId: String, channelId: String): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            reveilId.hashCode(),
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val dismiss = PendingIntent.getBroadcast(
            this,
            reveilId.hashCode(),
            Intent(this, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_DISMISS
                putExtra("REVEIL_ID", reveilId)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Alarme")
            .setContentText("C'est l'heure de votre réveil !")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Désactiver", dismiss)
            .build()
    }

    override fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "alarm_service_channel"
        const val NOTIFICATION_ID_PREFIX = 100000

        fun NOTIFICATION_ID(reveilId: String): Int =
            NOTIFICATION_ID_PREFIX + reveilId.hashCode()
    }
}
