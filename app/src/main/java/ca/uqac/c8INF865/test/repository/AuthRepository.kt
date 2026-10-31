package ca.uqac.c8INF865.test.repository

import ca.uqac.c8INF865.test.data.Utilisateur
import ca.uqac.c8INF865.test.network.Supabase
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository {
    private val auth = Supabase.client.auth
    private val db = Supabase.client.postgrest

    suspend fun inscription(email: String, motDePasse: String, nom: String, telephone: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Création du compte sécurisé Supabase
                auth.signUpWith(Email) {
                    this.email = email
                    this.password = motDePasse
                }

                // 2. Récupération de l'ID généré
                val utilisateurId = auth.currentUserOrNull()?.id ?: return@withContext false

                // 3. Sauvegarde du profil dans notre table SQL
                val profil = Utilisateur(
                    id = utilisateurId,
                    nom = nom,
                    telephone = telephone
                )
                db.from("utilisateurs").insert(profil)
                true
            } catch (e: Exception) {
                android.util.Log.e("SupabaseAuth", "Erreur d'inscription", e)
                false
            }
        }
    }

    suspend fun connexion(email: String, motDePasse: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                auth.signInWith(Email) {
                    this.email = email
                    this.password = motDePasse
                }
                true
            } catch (e: Exception) {
                android.util.Log.e("SupabaseAuth", "Erreur de connexion", e)
                false
            }
        }
    }

    suspend fun deconnexion() {
        withContext(Dispatchers.IO) {
            try {
                auth.signOut()
            } catch (e: Exception) {
                android.util.Log.e("SupabaseAuth", "Erreur de déconnexion", e)
            }
        }
    }

    suspend fun recupererProfil(): Utilisateur? {
        return withContext(Dispatchers.IO) {
            try {
                val utilisateurId = recupererIdUtilisateurCourant() ?: return@withContext null
                db.from("utilisateurs")
                    .select {
                        filter {
                            eq("id", utilisateurId)
                        }
                    }
                    .decodeSingleOrNull<Utilisateur>()
            } catch (e: Exception) {
                android.util.Log.e("SupabaseAuth", "Erreur de récupération du profil", e)
                null
            }
        }
    }

    suspend fun mettreAJourProfil(nom: String, telephone: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val utilisateurId = recupererIdUtilisateurCourant() ?: return@withContext false
                db.from("utilisateurs").update(
                    Utilisateur(
                        id = utilisateurId,
                        nom = nom,
                        telephone = telephone
                    )
                ) {
                    filter {
                        eq("id", utilisateurId)
                    }
                }
                true
            } catch (e: Exception) {
                android.util.Log.e("SupabaseAuth", "Erreur de mise à jour du profil", e)
                false
            }
        }
    }

    fun recupererIdUtilisateurCourant(): String? {
        return auth.currentUserOrNull()?.id
    }
}