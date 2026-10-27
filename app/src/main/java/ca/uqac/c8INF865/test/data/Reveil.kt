package ca.uqac.c8INF865.test.data

import kotlinx.serialization.Serializable

@Serializable
data class Reveil(
    val id: String = "",
    val createur_id: String = "",
    val nom: String,
    val heure: String,
    val actif: Boolean = true
)