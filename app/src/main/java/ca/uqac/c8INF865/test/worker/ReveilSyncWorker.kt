package ca.uqac.c8INF865.test.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ca.uqac.c8INF865.test.AlarmSoundCache
import ca.uqac.c8INF865.test.network.Supabase
import ca.uqac.c8INF865.test.repository.ContactRepository
import ca.uqac.c8INF865.test.repository.ReveilRepository
import ca.uqac.c8INF865.test.ui.screens.annulerAlarmeReveil
import ca.uqac.c8INF865.test.ui.screens.programmerAlarmeReveil
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReveilSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val userId = Supabase.client.auth.currentUserOrNull()?.id
                    ?: return@withContext Result.success()

                val reveilRepository = ReveilRepository()
                val contactRepository = ContactRepository()

                // Mémoire locale pour ne pas spammer les notifications
                val prefs = applicationContext.getSharedPreferences("SynchroMemoire", Context.MODE_PRIVATE)
                val reveilsConnus = prefs.getStringSet("reveils_connus", emptySet()) ?: emptySet()
                val demandesConnues = prefs.getStringSet("demandes_connues", emptySet()) ?: emptySet()

                // ==========================================
                // 1. GESTION DES DEMANDES D'AMIS
                // ==========================================
                val demandes = contactRepository.recupererDemandes(userId)
                val idsDemandesActuelles = demandes.map { it.id }.toSet()

                // On trouve les nouvelles demandes (qui ne sont pas dans la mémoire)
                val nouvellesDemandes = demandes.filter { it.id !in demandesConnues }

                nouvellesDemandes.forEach { demandeur ->
                    afficherNotification(
                        idNotif = demandeur.id.hashCode(),
                        titre = "Nouvelle demande d'ami",
                        message = "${demandeur.nom} souhaite vous ajouter en ami."
                    )
                }

                // ==========================================
                // 2. GESTION DES RÉVEILS ET ALARMES
                // ==========================================
                val reveils = reveilRepository.recupererReveils(userId)
                val membresMap = reveilRepository.recupererMembresParReveil(reveils.map { it.id })

                val sonIdsMembres = membresMap.values.flatten().mapNotNull { it.sonId }
                val sonIdsCreateurs = reveils.mapNotNull { it.son_id }
                val sonsMap = reveilRepository.recupererSonsParIds((sonIdsMembres + sonIdsCreateurs).distinct())

                val idsReveilsActuels = reveils.map { it.id }.toSet()
                val nouveauxReveils = reveils.filter { it.id !in reveilsConnus }

                // Programmation des alarmes et notifications pour les nouveaux ajouts
                reveils.forEach { reveil ->
                    if (reveil.actif) {
                        val sonId = if (reveil.createur_id == userId) reveil.son_id else membresMap[reveil.id]?.find { it.membreId == userId }?.sonId
                        val sonUrl = sonId?.let { sonsMap[it]?.url }

                        if (sonId != null && sonUrl != null) {
                            AlarmSoundCache.cache(applicationContext, sonId, sonUrl)
                        }
                        programmerAlarmeReveil(applicationContext, reveil, sonId, sonUrl)
                    } else {
                        annulerAlarmeReveil(applicationContext, reveil.id)
                    }
                }

                // Notifier l'utilisateur s'il a été ajouté à un nouveau réveil (dont il n'est pas le créateur)
                nouveauxReveils.filter { it.createur_id != userId }.forEach { nouveauReveil ->
                    afficherNotification(
                        idNotif = nouveauReveil.id.hashCode(),
                        titre = "Nouveau réveil partagé",
                        message = "Vous avez été ajouté au réveil : ${nouveauReveil.nom}"
                    )
                }

                // ==========================================
                // 3. MISE À JOUR DE LA MÉMOIRE LOCALE
                // ==========================================
                prefs.edit()
                    .putStringSet("reveils_connus", idsReveilsActuels)
                    .putStringSet("demandes_connues", idsDemandesActuelles)
                    .apply()

                Result.success()
            } catch (e: Exception) {
                android.util.Log.e("ReveilSyncWorker", "Erreur de synchro en arrière-plan", e)
                Result.retry()
            }
        }
    }

    private fun afficherNotification(idNotif: Int, titre: String, message: String) {
        val channelId = "synchro_notifications"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Création du canal requis depuis Android 8 (Oreo)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Activité Sociale et Réveils",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(titre)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(idNotif, notification)
    }
}