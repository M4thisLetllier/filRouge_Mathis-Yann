package ca.uqac.c8INF865.test

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Récupération de l'ID du réveil passé dans l'intent
        val reveilId = intent.getStringExtra("REVEIL_ID") ?: "default_id"

        val notificationManager = context.getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(CHANNEL_ID, "Alarm", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
            )
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)

        // Intent pour ouvrir l'application lorsque l'utilisateur clique sur la notification
        val openApp = PendingIntent.getActivity(
            context,
            reveilId.hashCode(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Alarme")
            .setContentText("C'est l'heure de votre réveil !")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        notification.flags = notification.flags or Notification.FLAG_INSISTENT

        // Utilisation du hashCode de l'ID pour avoir un ID de notification unique par réveil
        notificationManager.notify(reveilId.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID = "alarm_channel"
    }
}