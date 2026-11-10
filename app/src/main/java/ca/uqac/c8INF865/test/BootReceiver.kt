package ca.uqac.c8INF865.test

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ca.uqac.c8INF865.test.network.Supabase
import ca.uqac.c8INF865.test.repository.ReveilRepository
import ca.uqac.c8INF865.test.ui.screens.programmerAlarmeReveil
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // Le traitement réseau doit continuer après le retour de onReceive.
        val result = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                reprogrammerReveils(appContext)
            } catch (e: Exception) {
                android.util.Log.e("BootReceiver", "Erreur restauration des réveils", e)
            } finally {
                result.finish()
            }
        }
    }

    private suspend fun reprogrammerReveils(context: Context) {
        val userId = Supabase.client.auth.currentUserOrNull()?.id
        if (userId == null) {
            android.util.Log.w("BootReceiver", "Aucun utilisateur connecté, réveils non restaurés")
            return
        }
        val repository = ReveilRepository()
        val reveils = repository.recupererReveils(userId)
        if (reveils.isEmpty()) return

        val membresParReveil = repository.recupererMembresParReveil(reveils.map { it.id })
        val sonIds = reveils.mapNotNull { reveil ->
            if (reveil.createur_id == userId) {
                reveil.son_id
            } else {
                membresParReveil[reveil.id]
                    ?.firstOrNull { it.membreId == userId }
                    ?.sonId
            }
        }.distinct()
        val sons = repository.recupererSonsParIds(sonIds)

        reveils.forEach { reveil ->
            if (reveil.actif) {
                val sonId = if (reveil.createur_id == userId) {
                    reveil.son_id
                } else {
                    membresParReveil[reveil.id]
                        ?.firstOrNull { it.membreId == userId }
                        ?.sonId
                }
                val son = sonId?.let { sons[it] }
                programmerAlarmeReveil(context, reveil, son?.id, son?.url)
            }
        }
    }
}