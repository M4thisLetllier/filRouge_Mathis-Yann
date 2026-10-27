package ca.uqac.c8INF865.test.repository

import ca.uqac.c8INF865.test.data.Utilisateur
import ca.uqac.c8INF865.test.network.Supabase
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

// Petite classe privée pour lire la table de liaison
@Serializable
private data class AmiRelation(val utilisateur_id: String, val ami_id: String)

class ContactRepository {
    private val db = Supabase.client.postgrest

    suspend fun recupererContacts(userId: String): List<Utilisateur> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Récupérer les liaisons d'amitié de l'utilisateur courant
                val relations = db.from("amis")
                    .select { filter { eq("utilisateur_id", userId) } }
                    .decodeList<AmiRelation>()

                if (relations.isEmpty()) return@withContext emptyList()

                // 2. Extraire les ID des amis
                val amiIds = relations.map { it.ami_id }

                // 3. Récupérer les profils complets de ces amis dans la table utilisateurs
                db.from("utilisateurs")
                    .select { filter { isIn("id", amiIds) } }
                    .decodeList<Utilisateur>()
            } catch (e: Exception) {
                android.util.Log.e("SupabaseContact", "Erreur récupération contacts", e)
                emptyList()
            }
        }
    }

    suspend fun ajouterAmiParTelephone(userId: String, telephone: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Chercher l'utilisateur avec ce numéro exact
                val profilTrouve = db.from("utilisateurs")
                    .select { filter { eq("telephone", telephone) } }
                    .decodeSingleOrNull<Utilisateur>()

                if (profilTrouve != null && profilTrouve.id != userId) {
                    // 2. Créer la liaison d'amitié
                    val liaison = AmiRelation(utilisateur_id = userId, ami_id = profilTrouve.id)
                    db.from("amis").insert(liaison)
                    true
                } else {
                    false // Utilisateur introuvable ou c'est nous-mêmes
                }
            } catch (e: Exception) {
                android.util.Log.e("SupabaseContact", "Erreur ajout ami", e)
                false
            }
        }
    }

    suspend fun supprimerAmi(userId: String, amiId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                db.from("amis").delete {
                    filter {
                        eq("utilisateur_id", userId)
                        eq("ami_id", amiId)
                    }
                }
                true
            } catch (e: Exception) {
                android.util.Log.e("SupabaseContact", "Erreur suppression ami", e)
                false
            }
        }
    }
}