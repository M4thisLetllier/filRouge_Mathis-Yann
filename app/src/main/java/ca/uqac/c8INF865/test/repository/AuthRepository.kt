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

    fun recupererIdUtilisateurCourant(): String? {
        return auth.currentUserOrNull()?.id
    }
}