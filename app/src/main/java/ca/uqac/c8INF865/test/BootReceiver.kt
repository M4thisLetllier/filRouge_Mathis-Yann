package ca.uqac.c8INF865.test

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        //Au redémmarrage du tel les réveils ne fonctionnent plus
        //Il faudrait regénérer les réveils ici !
    }
}