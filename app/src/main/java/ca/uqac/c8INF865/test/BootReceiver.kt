package ca.uqac.c8INF865.test

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ca.uqac.c8INF865.test.repository.ReveilRepository
import ca.uqac.c8INF865.test.ui.screens.programmerAlarmeReveil

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val appContext = context.applicationContext
        try {
            val repository = ReveilRepository()
            // Lecture depuis les SharedPreferences
            val reveilsSauvegardes = repository.lireReveilsDepuisPrefs(appContext)

            reveilsSauvegardes.forEach { item ->
                if (item.reveil.actif) {
                    programmerAlarmeReveil(
                        context = appContext,
                        reveil = item.reveil,
                        sonId = item.monSonId,
                        sonUrl = item.monSonUrl
                    )
                }
            }
            android.util.Log.d("BootReceiver", "${reveilsSauvegardes.size} réveils restaurés depuis SharedPreferences")
        } catch (e: Exception) {
            android.util.Log.e("BootReceiver", "Erreur restauration des réveils", e)
        }
    }
}