package ca.uqac.c8INF865.test.repository

import ca.uqac.c8INF865.test.network.Supabase
import ca.uqac.c8INF865.test.data.Son
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class SonRepository {
    private val db = Supabase.client.postgrest
    private val storage = Supabase.client.storage

    // Fonction suspendue : elle s'exécute en arrière-plan sans bloquer l'application
    suspend fun recupererSons(userId: String): List<Son> {
        return withContext(Dispatchers.IO) {
            try {
                // Requête SQL vers Supabase
                db.from("sons").select {
                    filter { eq("utilisateur_id", userId) }
                }.decodeList<Son>()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun uploaderSon(userId: String, nom: String, cheminLocal: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val fichier = File(cheminLocal)
                val cheminStorage = "$userId/${fichier.name}"

                // 1. Upload dans le bucket "audios"
                val bucket = storage.from("audios")
                bucket.upload(cheminStorage, fichier.readBytes())

                // 2. Récupération de l'URL publique
                val urlPublique = bucket.publicUrl(cheminStorage)

                // 3. Sauvegarde dans la table SQL "sons"
                val nouveauSon = mapOf(
                    "utilisateur_id" to userId,
                    "nom" to nom,
                    "url" to urlPublique
                )
                db.from("sons").insert(nouveauSon)
                true
            } catch (e: Exception) {
                android.util.Log.e("Supabase", "Erreur upload", e)
                false
            }
        }
    }
}