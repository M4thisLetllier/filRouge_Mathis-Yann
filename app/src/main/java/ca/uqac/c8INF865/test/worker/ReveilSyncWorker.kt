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
import ca.uqac.c8INF865.test.repository.ReveilCacheLocal
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

                // Mémoire locale pour les notifications
                val prefs = applicationContext.getSharedPreferences("SynchroMemoire", Context.MODE_PRIVATE)
                val reveilsConnus = prefs.getStringSet("reveils_connus", emptySet()) ?: emptySet()
                val demandesConnues = prefs.getStringSet("demandes_connues", emptySet()) ?: emptySet()

                // Lecture de l'ancien cache des réveils (pour comparer les changements faits par les autres)
                val anciensReveilsMap = reveilRepository.lireReveilsDepuisPrefs(applicationContext)
                    .associateBy { it.reveil.id }

                // ==========================================
                // 1. GESTION DES DEMANDES D'AMIS
                // ==========================================
                val demandes = contactRepository.recupererDemandes(userId)
                val idsDemandesActuelles = demandes.mapNotNull { it.id }.toSet()

                val nouvellesDemandes = demandes.filter { it.id != null && it.id !in demandesConnues }
                nouvellesDemandes.forEach { demandeur ->
                    afficherNotification(
                        idNotif = demandeur.id.hashCode(),
                        titre = "Nouvelle demande d'ami",
                        message = "${demandeur.nom} souhaite vous ajouter en ami."
                    )
                }

                // ==========================================
                // 2. GESTION DES RÉVEILS ET DES SONNERIES
                // ==========================================
                val reveils = reveilRepository.recupererReveils(userId)
                val membresMap = reveilRepository.recupererMembresParReveil(reveils.map { it.id })

                val sonIdsMembres = membresMap.values.flatten().mapNotNull { it.sonId }
                val sonIdsCreateurs = reveils.mapNotNull { it.son_id }
                val sonsMap = reveilRepository.recupererSonsParIds((sonIdsMembres + sonIdsCreateurs).distinct())

                val idsReveilsActuels = reveils.map { it.id }.toSet()
                val nouveauxReveils = reveils.filter { it.id !in reveilsConnus }

                // A. Annuler les alarmes des réveils qui ont été supprimés ou quittés
                val idsSupprimes = anciensReveilsMap.keys - idsReveilsActuels
                idsSupprimes.forEach { idSupprime ->
                    annulerAlarmeReveil(applicationContext, idSupprime)
                }

                // B. Construire le nouveau cache, mettre à jour les sonneries et détecter les modifications
                val nouveauCacheLocal = mutableListOf< ReveilCacheLocal >()

                reveils.forEach { reveil ->
                    // Récupère le son attribué à l'utilisateur courant (qu'il soit créateur ou membre !)
                    val monSonId = if (reveil.createur_id == userId) {
                        reveil.son_id // Son attribué par un ami au créateur
                    } else {
                        membresMap[reveil.id]?.find { it.membreId == userId }?.sonId
                    }
                    val monSonUrl = monSonId?.let { sonsMap[it]?.url }

                    // Ajout à la liste pour SharedPreferences
                    nouveauCacheLocal.add(
                        ReveilCacheLocal(
                            reveil = reveil,
                            monSonId = monSonId,
                            monSonUrl = monSonUrl
                        )
                    )

                    // Vérifier si un ami a modifié ce réveil (heure ou son attribué) même si on en est le créateur
                    val ancienEtat = anciensReveilsMap[reveil.id]
                    if (ancienEtat != null) {
                        val heureModifiee = ancienEtat.reveil.heure != reveil.heure
                        val sonModifie = ancienEtat.monSonId != monSonId

                        if (heureModifiee || (sonModifie && monSonId != null)) {
                            val detailsModif = when {
                                heureModifiee && sonModifie -> "nouvelle heure (${reveil.heure}) et nouveau son !"
                                heureModifiee -> "l'heure a été changée à ${reveil.heure}."
                                else -> "une nouvelle sonnerie vous a été attribuée !"
                            }
                            afficherNotification(
                                idNotif = (reveil.id + "_modif").hashCode(),
                                titre = "Réveil mis à jour : ${reveil.nom}",
                                message = detailsModif
                            )
                        }
                    }

                    // Mise à jour effective de l'alarme et du fichier audio en cache
                    if (reveil.actif) {
                        if (monSonId != null && monSonUrl != null) {
                            // Télécharge le nouveau son en cache local s'il a été changé par un ami
                            AlarmSoundCache.cache(applicationContext, monSonId, monSonUrl)
                        }
                        programmerAlarmeReveil(applicationContext, reveil, monSonId, monSonUrl)
                    } else {
                        annulerAlarmeReveil(applicationContext, reveil.id)
                    }
                }

                // C. Notifier l'utilisateur s'il a été invité à un tout nouveau réveil
                nouveauxReveils.filter { it.createur_id != userId }.forEach { nouveauReveil ->
                    afficherNotification(
                        idNotif = nouveauReveil.id.hashCode(),
                        titre = "Nouveau réveil partagé",
                        message = "Vous avez été ajouté au réveil : ${nouveauReveil.nom} (${nouveauReveil.heure})"
                    )
                }

                // ==========================================
                // 3. SAUVEGARDE DANS SHARED PREFERENCES
                // ==========================================
                // Sauvegarde la liste complète des réveils et des sons pour le BootReceiver
                reveilRepository.sauvegarderReveilsDansPrefs(applicationContext, nouveauCacheLocal)

                // Sauvegarde les IDs connus pour les notifications
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