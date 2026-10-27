package ca.uqac.c8INF865.test.data

import kotlinx.serialization.Serializable

@Serializable
data class Son(
    val id: String = "",           // Généré par Supabase (UUID)
    val utilisateur_id: String = "", // Clé étrangère vers le créateur
    val nom: String,
    val url: String
)