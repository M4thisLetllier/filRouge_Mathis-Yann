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

    // Exemple d'implémentation pour SonRepository
    suspend fun uploaderSon(userId: String, nom: String, cheminLocal: String): Boolean {
        return try {
            val fichier = java.io.File(cheminLocal)
            val extension = fichier.extension
            val nomFichierUnique = "${userId}_${System.currentTimeMillis()}.$extension"

            // 1. Upload dans le Storage
            val bucket = Supabase.client.storage.from("sons")
            bucket.upload(nomFichierUnique, fichier.readBytes())

            // 2. Récupération de l'URL publique
            val urlPublique = bucket.publicUrl(nomFichierUnique)

            // 3. Sauvegarde dans la table SQL 'sons'
            val nouveauSon = Son(
                id = java.util.UUID.randomUUID().toString(),
                utilisateur_id = userId,
                nom = nom,
                url = urlPublique
            )
            Supabase.client.postgrest.from("sons").insert(nouveauSon)

            true
        } catch (e: Exception) {
            android.util.Log.e("SupabaseStorage", "Erreur d'upload", e)
            false
        }
    }
}