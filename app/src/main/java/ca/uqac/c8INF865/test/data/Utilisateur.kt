package ca.uqac.c8INF865.test.data

import kotlinx.serialization.Serializable

@Serializable
data class Utilisateur(
    val id: String = "",
    val nom: String,
    val telephone: String
)

// --------------------------------------------------------
// MODÈLES DE DONNÉES pour la bdd
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
