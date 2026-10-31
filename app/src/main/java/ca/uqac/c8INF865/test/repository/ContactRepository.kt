package ca.uqac.c8INF865.test.repository

import ca.uqac.c8INF865.test.data.Utilisateur
import ca.uqac.c8INF865.test.network.Supabase
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable

// --------------------------------------------------------
// MODÈLES DE DONNÉES
// --------------------------------------------------------

// Modèle pour LIRE une relation (l'id est optionnel pour éviter le crash MissingFieldException)
@Serializable
data class RelationAmi(
    val id: String? = null,
    val utilisateur_id: String,
    val ami_id: String
)

// Modèle pour INSÉRER une relation (sans id, Supabase le génère automatiquement)
@Serializable
data class InsertRelationAmi(
    val utilisateur_id: String,
    val ami_id: String
)

// Modèle pour LIRE une demande d'ami
@Serializable
data class DemandeAmi(
    val id: String? = null,
    val expediteur_id: String,
    val destinataire_id: String,
    val statut: String
)

// Modèle pour INSÉRER une demande d'ami
@Serializable
data class InsertDemandeAmi(
    val expediteur_id: String,
    val destinataire_id: String,
    val statut: String = "en_attente"
)

// --------------------------------------------------------
// REPOSITORY
// --------------------------------------------------------

class ContactRepository {
    private val db = Supabase.client.postgrest

    // ==========================================
    // GESTION DES AMIS
    // ==========================================

    // Récupérer la liste des amis de l'utilisateur connecté
    suspend fun recupererContacts(userId: String): List<Utilisateur> {
        return try {
            val relations = db.from("amis")
                .select {
                    filter {
                        eq("utilisateur_id", userId)
                    }
                }
                .decodeList<RelationAmi>()

            if (relations.isEmpty()) return emptyList()

            val idsAmis = relations.map { it.ami_id }

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

    // Supprimer un ami
    suspend fun supprimerAmi(userId: String, amiId: String) {
        try {
            // Supprime la relation où l'utilisateur connecté est l'initiateur
            db.from("amis").delete {
                filter {
                    eq("utilisateur_id", userId)
                    eq("ami_id", amiId)
                }
            }
            // Supprime la relation inverse
            db.from("amis").delete {
                filter {
                    eq("utilisateur_id", amiId)
                    eq("ami_id", userId)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseContact", "Erreur suppression ami", e)
        }
    }
    // ==========================================
    // GESTION DES DEMANDES D'AMIS
    // ==========================================

    // Envoyer une demande avec vérification des amitiés et demandes existantes
    suspend fun envoyerDemandeParTelephone(userId: String, telephone: String): Boolean {
        return try {
            // 1. Chercher l'utilisateur cible dans la table "utilisateurs"
            val utilisateurCible = db.from("utilisateurs")
                .select {
                    filter {
                        eq("telephone", telephone.trim())
                    }
                }
                .decodeSingleOrNull<Utilisateur>() ?: return false

            // Empêcher de s'ajouter soi-même
            if (utilisateurCible.id == userId) return false

            // 2. VÉRIFICATION : Sont-ils DÉJÀ amis ? (On vérifie dans les deux sens)
            val amitieSens1 = db.from("amis")
                .select { filter { eq("utilisateur_id", userId); eq("ami_id", utilisateurCible.id) } }
                .decodeList<RelationAmi>()

            val amitieSens2 = db.from("amis")
                .select { filter { eq("utilisateur_id", utilisateurCible.id); eq("ami_id", userId) } }
                .decodeList<RelationAmi>()

            if (amitieSens1.isNotEmpty() || amitieSens2.isNotEmpty()) {
                return false // Ils sont déjà amis, on bloque la demande
            }

            // 3. VÉRIFICATION : Y a-t-il DÉJÀ une demande en attente entre eux ?
            val demandeSens1 = db.from("demandes_amis")
                .select { filter { eq("expediteur_id", userId); eq("destinataire_id", utilisateurCible.id) } }
                .decodeList<DemandeAmi>()

            val demandeSens2 = db.from("demandes_amis")
                .select { filter { eq("expediteur_id", utilisateurCible.id); eq("destinataire_id", userId) } }
                .decodeList<DemandeAmi>()

            if (demandeSens1.isNotEmpty() || demandeSens2.isNotEmpty()) {
                return false // Une demande existe déjà, on bloque
            }

            // 4. Tout est bon, on insère la nouvelle demande (en utilisant InsertDemandeAmi)
            val nouvelleDemande = InsertDemandeAmi(
                expediteur_id = userId,
                destinataire_id = utilisateurCible.id
            )

            db.from("demandes_amis").insert(nouvelleDemande)
            true
        } catch (e: Exception) {
            android.util.Log.e("SupabaseContact", "Erreur envoi demande", e)
            false
        }
    }

    // Récupérer les demandes reçues en attente
    suspend fun recupererDemandes(userId: String): List<Utilisateur> {
        return try {
            val demandes = db.from("demandes_amis")
                .select {
                    filter {
                        eq("destinataire_id", userId)
                        eq("statut", "en_attente")
                    }
                }
                .decodeList<DemandeAmi>()

            if (demandes.isEmpty()) return emptyList()

            // On récupère les profils complets de ceux qui ont envoyé la demande
            val idsExpediteurs = demandes.map { it.expediteur_id }
            db.from("utilisateurs")
                .select {
                    filter {
                        isIn("id", idsExpediteurs)
                    }
                }
                .decodeList<Utilisateur>()
        } catch (e: Exception) {
            android.util.Log.e("SupabaseContact", "Erreur récupération demandes", e)
            emptyList()
        }
    }

    // Accepter la demande en insérant proprement dans la table amis
    suspend fun accepterDemande(userId: String, expediteurId: String) {
        try {
            // 1. On crée les deux relations pour que l'amitié soit bilatérale
            val relation1 = InsertRelationAmi(utilisateur_id = userId, ami_id = expediteurId)
            val relation2 = InsertRelationAmi(utilisateur_id = expediteurId, ami_id = userId)

            // On insère les deux d'un coup en passant une liste
            db.from("amis").insert(listOf(relation1, relation2))

            // 2. On supprime la demande en attente
            db.from("demandes_amis").delete {
                filter {
                    eq("destinataire_id", userId)
                    eq("expediteur_id", expediteurId)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseContact", "Erreur acceptation demande", e)
        }
    }

    // Refuser la demande (supprime simplement la ligne)
    suspend fun refuserDemande(userId: String, expediteurId: String) {
        try {
            db.from("demandes_amis").delete {
                filter {
                    eq("destinataire_id", userId)
                    eq("expediteur_id", expediteurId)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseContact", "Erreur refus demande", e)
        }
    }
}