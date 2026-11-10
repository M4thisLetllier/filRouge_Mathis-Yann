package ca.uqac.c8INF865.test.repository

import ca.uqac.c8INF865.test.data.Reveil
import ca.uqac.c8INF865.test.data.Son
import ca.uqac.c8INF865.test.data.Utilisateur
import ca.uqac.c8INF865.test.network.Supabase
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// --------------------------------------------------------
// MODÈLES DE DONNÉES
// --------------------------------------------------------

// Modèle pour mettre à jour les informations d'un réveil
@Serializable
private data class ReveilInfosUpdate(val nom: String, val heure: String)

// Modèle pour activer ou désactiver un réveil
@Serializable
private data class ReveilActifUpdate(val actif: Boolean)

// Modèle représentant la relation entre un réveil, un membre et son son
@Serializable
private data class ReveilMembreRelation(
    val reveil_id: String,
    val membre_id: String,
    val son_id: String? = null // Empêche le crash de sérialisation
)

// Modèle pour associer un son à un membre
@Serializable
private data class MembreSonUpdate(val son_id: String)

// Modèle complet sauvegardé dans SharedPreferences pour le BootReceiver et le Worker
@Serializable
data class ReveilCacheLocal(
    val reveil: Reveil,
    val monSonId: String? = null,
    val monSonUrl: String? = null
)
// Informations d'un membre associé à un réveil et du son qui lui est attribué
data class MembreReveilInfo(val membreId: String, val sonId: String?, val sonUrl: String? = null)

// --------------------------------------------------------
// REPOSITORY
// --------------------------------------------------------

class ReveilRepository {
    private val db = Supabase.client.postgrest

    // ==========================================
    // GESTION DES RÉVEILS
    // ==========================================

    // Récupérer les réveils créés ou partagés avec l'utilisateur
    suspend fun recupererReveils(userId: String): List<Reveil> {
        return withContext(Dispatchers.IO) {
            try {
                // Récupérer les réveils que l'utilisateur a créés
                val mesReveils = db.from("reveils")
                    .select { filter { eq("createur_id", userId) } }
                    .decodeList<Reveil>()

                // Récupérer les ID des réveils où l'utilisateur est invité
                val relationsInvitation = db.from("reveil_membres")
                    .select { filter { eq("membre_id", userId) } }
                    .decodeList<ReveilMembreRelation>()
                val reveilsInvitesIds = relationsInvitation.map { it.reveil_id }

                // Récupérer les données des réveils invités
                val reveilsInvites = if (reveilsInvitesIds.isNotEmpty()) {
                    db.from("reveils")
                        .select { filter { isIn("id", reveilsInvitesIds) } }
                        .decodeList<Reveil>()
                } else emptyList()

                // Retourner la combinaison des deux (sans doublons)
                (mesReveils + reveilsInvites).distinctBy { it.id }.sortedBy { it.heure }
            } catch (e: Exception) {
                android.util.Log.e("SupabaseReveil", "Erreur récupération réveils", e)
                emptyList()
            }
        }
    }

    // Récupérer les membres et leurs sons pour plusieurs réveils
    suspend fun recupererMembresParReveil(reveilIds: List<String>): Map<String, List<MembreReveilInfo>> {
        return withContext(Dispatchers.IO) {
            if (reveilIds.isEmpty()) return@withContext emptyMap()
            try {
                val relations = db.from("reveil_membres")
                    .select { filter { isIn("reveil_id", reveilIds) } }
                    .decodeList<ReveilMembreRelation>()
                val sons = relations.mapNotNull { it.son_id }.distinct().let { sonIds ->
                    if (sonIds.isEmpty()) {
                        emptyList()
                    } else {
                        db.from("sons").select { filter { isIn("id", sonIds) } }.decodeList<Son>()
                    }
                }.associateBy { it.id }

                // On groupe par réveil, mais on garde l'ID du membre et l'ID du son
                relations.groupBy(
                    keySelector = { it.reveil_id },
                    valueTransform = {
                        MembreReveilInfo(it.membre_id, it.son_id, sons[it.son_id]?.url)
                    }
                )
            } catch (e: Exception) {
                emptyMap()
            }
        }

    }

    // Récupérer le son attribué à un membre pour un réveil
    suspend fun recupererSonPourReveil(reveilId: String, membreId: String): MembreReveilInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val relation = db.from("reveil_membres")
                    .select {
                        filter {
                            eq("reveil_id", reveilId)
                            eq("membre_id", membreId)
                        }
                    }
                    .decodeList<ReveilMembreRelation>()
                    .firstOrNull()
                    ?: return@withContext null

                val son = relation.son_id?.let { sonId ->
                    db.from("sons").select { filter { eq("id", sonId) } }
                        .decodeList<Son>()
                        .firstOrNull()
                }
                MembreReveilInfo(relation.membre_id, relation.son_id, son?.url)
            } catch (exception: Exception) {
                android.util.Log.e("SupabaseReveil", "Erreur récupération son du réveil", exception)
                null
            }
        }
    }

    // Ajouter un nouveau réveil
    suspend fun ajouterReveil(userId: String, nom: String, heure: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val nouveauReveil = Reveil(
                    id = java.util.UUID.randomUUID().toString(), // Génère un vrai UUID valide
                    createur_id = userId,
                    nom = nom,
                    heure = heure,
                    actif = true
                )
                db.from("reveils").insert(nouveauReveil)
                true
            } catch (e: Exception) {
                android.util.Log.e("SupabaseReveil", "Erreur lors de l'ajout du réveil", e)
                false
            }
        }
    }

    // Mettre à jour le nom et l'heure d'un réveil
    suspend fun mettreAJourInfosReveil(id: String, nom: String, heure: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                db.from("reveils").update(ReveilInfosUpdate(nom, heure)) {
                    filter { eq("id", id) }
                }
                true
            } catch (e: Exception) {
                android.util.Log.e("SupabaseReveil", "Erreur update infos", e)
                false
            }
        }
    }

    // Activer ou désactiver un réveil
    suspend fun mettreAJourEtatReveil(id: String, actif: Boolean): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                db.from("reveils").update(ReveilActifUpdate(actif)) {
                    filter { eq("id", id) }
                }
                true
            } catch (e: Exception) {
                android.util.Log.e("SupabaseReveil", "Erreur update etat", e)
                false
            }
        }
    }

    // Supprimer un réveil
    suspend fun supprimerReveil(id: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                db.from("reveils").delete { filter { eq("id", id) } }
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    // ==========================================
    // GESTION DES MEMBRES ET DES SONS
    // ==========================================

    // Ajouter ou retirer un contact d'un réveil
    suspend fun basculerMembre(idReveil: String, idContact: String, associe: Boolean): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (associe) {
                    db.from("reveil_membres").insert(ReveilMembreRelation(idReveil, idContact))
                } else {
                    db.from("reveil_membres").delete {
                        filter {
                            eq("reveil_id", idReveil)
                            eq("membre_id", idContact)
                        }
                    }
                }
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    // Attribuer un son à un membre du réveil
    suspend fun assignerSonMembre(idReveil: String, idContact: String, idSon: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                db.from("reveil_membres").update(MembreSonUpdate(idSon)) {
                    filter {
                        eq("reveil_id", idReveil)
                        eq("membre_id", idContact)
                    }
                }
                true
            } catch (e: Exception) {
                android.util.Log.e("SupabaseReveil", "Erreur assignation son", e)
                false
            }
        }
    }

    // Attribuer un son au créateur du réveil
    suspend fun assignerSonCreateur(idReveil: String, idSon: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                db.from("reveils").update(MembreSonUpdate(idSon)) {
                    filter { eq("id", idReveil) }
                }
                true
            } catch (e: Exception) {
                android.util.Log.e("SupabaseReveil", "Erreur assignation son createur", e)
                false
            }
        }
    }

    // Récupérer plusieurs sons à partir de leurs identifiants
    suspend fun recupererSonsParIds(sonIds: List< String >): Map< String, Son > {
        return withContext(Dispatchers.IO) {
            if (sonIds.isEmpty()) return@withContext emptyMap()
            try {
                db.from("sons")
                    .select { filter { isIn("id", sonIds) } }
                    .decodeList< Son >()
                    .associateBy { it.id }
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    // Récupérer plusieurs utilisateurs à partir de leurs identifiants
    suspend fun recupererUtilisateursParIds(userIds: List< String >): Map< String, Utilisateur > {
        return withContext(Dispatchers.IO) {
            if (userIds.isEmpty()) return@withContext emptyMap()
            try {
                db.from("utilisateurs")
                    .select { filter { isIn("id", userIds) } }
                    .decodeList< Utilisateur >()
                    .associateBy { it.id }
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    //Local
    fun sauvegarderReveilsDansPrefs(context: Context, reveilsCache: List< ReveilCacheLocal >) {
        try {
            val prefs = context.getSharedPreferences("ReveilsPrefs", Context.MODE_PRIVATE)
            val jsonString = Json.encodeToString(reveilsCache)
            prefs.edit().putString("reveils_sauvegardes", jsonString).apply()
        } catch (e: Exception) {
            android.util.Log.e("ReveilRepository", "Erreur sauvegarde SharedPreferences", e)
        }
    }

    fun lireReveilsDepuisPrefs(context: Context): List< ReveilCacheLocal > {
        return try {
            val prefs = context.getSharedPreferences("ReveilsPrefs", Context.MODE_PRIVATE)
            val jsonString = prefs.getString("reveils_sauvegardes", null) ?: return emptyList()
            Json.decodeFromString< List< ReveilCacheLocal > >(jsonString)
        } catch (e: Exception) {
            android.util.Log.e("ReveilRepository", "Erreur lecture SharedPreferences", e)
            emptyList()
        }
    }
}
