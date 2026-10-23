package ca.uqac.c8INF865.test.viewModel

import androidx.lifecycle.ViewModel
import ca.uqac.c8INF865.test.screens.ModeleReveil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AccueilUiState(
    val listeReveils: List<ModeleReveil> = emptyList()
)

class ReveilViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AccueilUiState())
    val uiState: StateFlow<AccueilUiState> = _uiState.asStateFlow()

    // On garde un StateFlow pour les membres (UID) liés à un ID de réveil
    private val _contactsParReveil = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val contactsParReveil: StateFlow<Map<String, Set<String>>> = _contactsParReveil.asStateFlow()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        ecouterReveils()
    }

    private fun ecouterReveils() {
        val uid = auth.currentUser?.uid ?: return

        // On écoute les réveils où je suis le créateur OU membre
        db.collection("reveils")
            .whereEqualTo("createur_id", uid)
            .addSnapshotListener { snapshot, erreur ->
                if (erreur != null || snapshot == null) return@addSnapshotListener

                val liste = mutableListOf<ModeleReveil>()
                val mapMembres = mutableMapOf<String, Set<String>>()

                for (doc in snapshot) {
                    val membres = doc.get("membres_ids") as? List<String> ?: emptyList()
                    mapMembres[doc.id] = membres.toSet()

                    liste.add(
                        ModeleReveil(
                            id = doc.id, // Mettre ModeleReveil(id: String)
                            nom = doc.getString("nom") ?: "Sans nom",
                            heure = doc.getString("heure") ?: "00:00",
                            dateProchaine = doc.getString("dateProchaine") ?: "À venir",
                            nbAmis = membres.size,
                            actifParDefaut = doc.getBoolean("actif") ?: true
                        )
                    )
                }
                _uiState.value = AccueilUiState(liste)
                _contactsParReveil.value = mapMembres
            }
    }

    fun basculerEtatReveil(idReveil: String, estActif: Boolean) {
        db.collection("reveils").document(idReveil).update("actif", estActif)
    }

    fun ajouterReveil(nom: String, heure: String) {
        val uid = auth.currentUser?.uid ?: return
        val nouveauReveil = hashMapOf(
            "nom" to nom.trim(),
            "heure" to heure,
            "dateProchaine" to "À venir",
            "createur_id" to uid,
            "membres_ids" to emptyList<String>(),
            "actif" to true
        )
        db.collection("reveils").add(nouveauReveil)
    }

    fun modifierReveil(id: String, nom: String, heure: String) {
        db.collection("reveils").document(id).update(
            mapOf(
                "nom" to nom.trim(),
                "heure" to heure
            )
        )
    }

    fun supprimerReveil(id: String) {
        db.collection("reveils").document(id).delete()
    }

    // Associer un ID Firebase (String) au lieu d'un Int
    fun basculerContact(idReveil: String, idContact: String, associe: Boolean) {
        val ref = db.collection("reveils").document(idReveil)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            val membres = (snapshot.get("membres_ids") as? List<String>)?.toMutableList() ?: mutableListOf()

            if (associe && !membres.contains(idContact)) {
                membres.add(idContact)
            } else if (!associe) {
                membres.remove(idContact)
            }
            transaction.update(ref, "membres_ids", membres)
        }
    }
}