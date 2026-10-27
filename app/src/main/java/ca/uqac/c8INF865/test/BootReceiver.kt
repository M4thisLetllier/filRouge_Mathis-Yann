package ca.uqac.c8INF865.test

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ca.uqac.c8INF865.test.screens.programmerAlarmeReveil
// N'oubliez pas d'importer votre modèle Reveil depuis Supabase si nécessaire
// import ca.uqac.c8INF865.test.data.Reveil

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // Note : Au redémarrage du téléphone, les alarmes programmées via AlarmManager sont effacées par Android.
        // Idéalement, vous récupérez ici vos réveils actifs depuis votre base locale ou Supabase,
        // puis vous appelez programmerAlarmeReveil(context, reveil) pour chacun d'eux.

        // Exemple de structure si vous stockez l'état dans des SharedPreferences ou une base locale :
        /*
        val reveilsActifs = ... // Récupérer vos réveils actifs
        for (reveil in reveilsActifs) {
            programmerAlarmeReveil(context, reveil)
        }
        */
    }
}