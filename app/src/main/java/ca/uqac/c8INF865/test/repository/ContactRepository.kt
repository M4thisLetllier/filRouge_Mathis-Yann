package ca.uqac.c8INF865.test.repository

import ca.uqac.c8INF865.test.data.Utilisateur
import ca.uqac.c8INF865.test.network.Supabase
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable

// Modèle pour la table de liaison "amis" (ajustez les noms si besoin)
@Serializable
data class RelationAmi(
    val id: String = "",
    val utilisateur_id: String,
    val ami_id: String
)

class ContactRepository {
    private val db = Supabase.client.postgrest

    // Récupérer la liste des amis de l'utilisateur connecté
    suspend fun recupererContacts(userId: String): List<Utilisateur> {
        return try {
            // 1. Récupérer les relations où l'utilisateur est impliqué depuis la table "amis"
            // (En supposant que la table s'appelle "amis" avec "utilisateur_id" et "ami_id")
            val relations = db.from("amis")
                .select {
                    filter {
                        eq("utilisateur_id", userId)
                    }
                }
                .decodeList<RelationAmi>()

            if (relations.isEmpty()) return emptyList()

            // 2. Extraire les ID des amis
            val idsAmis = relations.map { it.ami_id }

            // 3. Récupérer les profils complets de ces amis dans la table "utilisateurs"
            db.from("utilisateurs")
                .select {
                    filter {
                        isIn("id", idsAmis)
                    }
                }
                .decodeList<Utilisateur>()

        } catch (e: Exception) {
            android.util.Log.e("SupabaseContact", "Erreur récupération contacts", e)
            emptyList()
        }
    }

    // Ajouter un ami via son numéro de téléphone
    suspend fun ajouterAmiParTelephone(userId: String, telephone: String): Boolean {
        return try {
            // 1. Chercher l'utilisateur cible dans la table "utilisateurs" par son téléphone
            val utilisateurCible = db.from("utilisateurs")
                .select {
                    filter {
                        eq("telephone", telephone.trim())
                    }
                }
                .decodeSingleOrNull<Utilisateur>() ?: return false

            // Empêcher de s'ajouter soi-même
            if (utilisateurCible.id == userId) return false

            // 2. Insérer la relation dans la table "amis"
            val nouvelleRelation = RelationAmi(
                utilisateur_id = userId,
                ami_id = utilisateurCible.id
            )

            db.from("amis").insert(nouvelleRelation)
            true
        } catch (e: Exception) {
            android.util.Log.e("SupabaseContact", "Erreur ajout ami", e)
            false
        }
    }

    // Supprimer un ami
    suspend fun supprimerAmi(userId: String, amiId: String) {
        try {
            db.from("amis").delete {
                filter {
                    eq("utilisateur_id", userId)
                    eq("ami_id", amiId)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseContact", "Erreur suppression ami", e)
        }
    }
}