package ca.uqac.c8INF865.test.repository

import ca.uqac.c8INF865.test.data.Reveil
import ca.uqac.c8INF865.test.network.Supabase
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
private data class ReveilInfosUpdate(val nom: String, val heure: String)

@Serializable
private data class ReveilActifUpdate(val actif: Boolean)

@Serializable
private data class ReveilMembreRelation(
    val reveil_id: String,
    val membre_id: String,
    val son_id: String? = null // Empêche le crash de sérialisation
)
@Serializable
private data class MembreSonUpdate(val son_id: String)

data class MembreReveilInfo(val membreId: String, val sonId: String?)

class ReveilRepository {
    private val db = Supabase.client.postgrest

    suspend fun recupererReveils(userId: String): List<Reveil> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Récupérer les réveils que l'utilisateur a créés
                val mesReveils = db.from("reveils")
                    .select { filter { eq("createur_id", userId) } }
                    .decodeList<Reveil>()

                // 2. Récupérer les ID des réveils où l'utilisateur est invité
                val relationsInvitation = db.from("reveil_membres")
                    .select { filter { eq("membre_id", userId) } }
                    .decodeList<ReveilMembreRelation>()
                val reveilsInvitesIds = relationsInvitation.map { it.reveil_id }

                // 3. Récupérer les données des réveils invités
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

    suspend fun recupererMembresParReveil(reveilIds: List<String>): Map<String, List<MembreReveilInfo>> {
        return withContext(Dispatchers.IO) {
            if (reveilIds.isEmpty()) return@withContext emptyMap()
            try {
                val relations = db.from("reveil_membres")
                    .select { filter { isIn("reveil_id", reveilIds) } }
                    .decodeList<ReveilMembreRelation>()

                // On groupe par réveil, mais on garde l'ID du membre ET l'ID du son
                relations.groupBy(
                    keySelector = { it.reveil_id },
                    valueTransform = { MembreReveilInfo(it.membre_id, it.son_id) }
                )
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

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
}