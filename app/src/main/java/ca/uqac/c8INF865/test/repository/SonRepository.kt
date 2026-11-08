package ca.uqac.c8INF865.test.repository

import ca.uqac.c8INF865.test.network.Supabase
import ca.uqac.c8INF865.test.data.Son
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// --------------------------------------------------------
// REPOSITORY
// --------------------------------------------------------

class SonRepository {
    private val db = Supabase.client.postgrest
    private val storage = Supabase.client.storage

    // ==========================================
    // GESTION DES SONS
    // ==========================================

    // Récupérer les sons appartenant à l'utilisateur
    suspend fun recupererSons(userId: String): List<Son> {
        return withContext(Dispatchers.IO) {
            try {
                db.from("sons").select {
                    filter { eq("utilisateur_id", userId) }
                }.decodeList<Son>()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    // Importer un fichier audio dans le Storage et l'enregistrer en base de données
    suspend fun uploaderSon(userId: String, nom: String, cheminLocal: String): Boolean {
        return try {
            val fichier = java.io.File(cheminLocal)
            val extension = fichier.extension
            val nomFichierUnique = "${userId}_${System.currentTimeMillis()}.$extension"

            // Upload dans le Storage
            val bucket = Supabase.client.storage.from("sons")
            bucket.upload(nomFichierUnique, fichier.readBytes())

            // Récupération de l'URL publique
            val urlPublique = bucket.publicUrl(nomFichierUnique)

            // Sauvegarde dans la table SQL 'sons'
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

    // Modifier le nom d'un son
    suspend fun modifierNomSon(sonId: String, nouveauNom: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                db.from("sons").update(
                    {
                        set("nom", nouveauNom)
                    }
                ) {
                    filter { eq("id", sonId) }
                }
                true
            } catch (e: Exception) {
                android.util.Log.e("SupabaseStorage", "Erreur modification nom", e)
                false
            }
        }
    }

    // Supprimer un son du Storage et de la base de données
    suspend fun supprimerSon(son: Son): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Extraire le nom du fichier à partir de l'URL publique
                val nomFichier = son.url.substringAfterLast("/")

                // Supprimer le fichier du Storage de Supabase
                val bucket = Supabase.client.storage.from("sons")
                bucket.delete(nomFichier) // delete prend une liste ou un vararg de noms de fichiers

                // Supprimer l'entrée dans la base de données SQL
                db.from("sons").delete {
                    filter { eq("id", son.id) }
                }

                true
            } catch (e: Exception) {
                android.util.Log.e("SupabaseStorage", "Erreur suppression son", e)
                false
            }
        }
    }
}