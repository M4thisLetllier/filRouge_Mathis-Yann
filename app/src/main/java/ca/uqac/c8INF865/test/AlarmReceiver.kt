package ca.uqac.c8INF865.test

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// BroadcastReceiver capturant le signal de l'AlarmManager d'Android à l'heure programmée pour déclencher l'alarme.
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reveilId = intent.getStringExtra("REVEIL_ID") ?: "default_id"
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        if (intent.action == ACTION_DISMISS) {
            context.stopService(Intent(context, AlarmSoundService::class.java))
            notificationManager.cancel(AlarmSoundService.NOTIFICATION_ID(reveilId))
            context.startActivity(
                Intent(context, MainActivity::class.java).apply {
                    action = MainActivity.ACTION_DISMISS_ALARM
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            )
            return
        }

        val serviceIntent = Intent(context, AlarmSoundService::class.java).apply {
            putExtra("REVEIL_ID", reveilId)
            putExtra("SON_ID", intent.getStringExtra("SON_ID"))
        }
        context.startForegroundService(serviceIntent)
    }

    companion object {
        const val ACTION_DISMISS = "ca.uqac.c8INF865.test.ACTION_DISMISS_ALARM"
    }
}