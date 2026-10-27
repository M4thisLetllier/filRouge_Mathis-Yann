package ca.uqac.c8INF865.test.data

import kotlinx.serialization.Serializable

@Serializable
data class Utilisateur(
    val id: String = "",
    val nom: String,
    val telephone: String
)