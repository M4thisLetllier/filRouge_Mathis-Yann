package ca.uqac.c8INF865.test.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.uqac.c8INF865.test.screens.Contact
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContactViewModel : ViewModel() {
    var recherche by mutableStateOf("")
        private set

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts = _contacts.asStateFlow()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        ecouterContacts()
    }

    private fun ecouterContacts() {
        val uid = auth.currentUser?.uid ?: return

        // On suppose que chaque utilisateur gère sa propre liste de contacts
        db.collection("utilisateurs").document(uid).collection("contacts")
            .addSnapshotListener { snapshot, erreur ->
                if (erreur != null || snapshot == null) return@addSnapshotListener

                val liste = snapshot.documents.mapNotNull { doc ->
                    Contact(
                        id = doc.getString("id")?.hashCode() ?: 0, // Firestore utilise des Strings pour les ID
                        name = doc.getString("name") ?: "",
                        phone = doc.getString("phone") ?: ""
                    )
                }
                _contacts.value = liste
            }
    }

    fun modifierRecherche(nouvelleRecherche: String) {
        recherche = nouvelleRecherche
    }

    fun ajouterContact(nom: String, telephone: String) {
        val uid = auth.currentUser?.uid ?: return
        val nouveauContact = hashMapOf(
            "name" to nom,
            "phone" to telephone
        )
        db.collection("utilisateurs").document(uid).collection("contacts").add(nouveauContact)
    }

    fun supprimer(contact: Contact) {
        val uid = auth.currentUser?.uid ?: return
        // Idéalement, il faut retrouver l'ID du document Firestore
        db.collection("utilisateurs").document(uid).collection("contacts")
            .whereEqualTo("name", contact.name)
            .get()
            .addOnSuccessListener { query ->
                for (doc in query) {
                    doc.reference.delete()
                }
            }
    }
}