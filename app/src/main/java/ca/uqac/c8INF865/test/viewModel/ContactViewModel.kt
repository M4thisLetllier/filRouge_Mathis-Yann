package ca.uqac.c8INF865.test.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.uqac.c8INF865.test.data.Utilisateur
import ca.uqac.c8INF865.test.network.Supabase
import ca.uqac.c8INF865.test.repository.ContactRepository
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContactViewModel : ViewModel() {
    private val repository = ContactRepository()

    var recherche by mutableStateOf("")
        private set

    private val _contacts = MutableStateFlow<List<Utilisateur>>(emptyList())
    val contacts: StateFlow<List<Utilisateur>> = _contacts.asStateFlow()

    // NOUVEAU : État pour les demandes
    private val _demandes = MutableStateFlow<List<Utilisateur>>(emptyList())
    val demandes: StateFlow<List<Utilisateur>> = _demandes.asStateFlow()

    private val _messageErreur = MutableStateFlow<String?>(null)
    val messageErreur: StateFlow<String?> = _messageErreur.asStateFlow()

    init {
        chargerDonnees()
    }

    private fun chargerDonnees() {
        viewModelScope.launch {
            val userId = Supabase.client.auth.currentUserOrNull()?.id ?: return@launch
            _contacts.value = repository.recupererContacts(userId)
            _demandes.value = repository.recupererDemandes(userId)
        }
    }

    fun modifierRecherche(nouvelleRecherche: String) {
        recherche = nouvelleRecherche
    }

    fun ajouterContact(telephone: String) {
        viewModelScope.launch {
            val userId = Supabase.client.auth.currentUserOrNull()?.id ?: return@launch
            // Appelle la nouvelle méthode qui envoie une demande
            val succes = repository.envoyerDemandeParTelephone(userId, telephone)

            if (succes) {
                _messageErreur.value = null
            } else {
                _messageErreur.value = "Aucun utilisateur trouvé ou demande déjà envoyée."
            }
        }
    }

    fun accepterDemande(expediteur: Utilisateur) {
        viewModelScope.launch {
            val userId = Supabase.client.auth.currentUserOrNull()?.id ?: return@launch
            repository.accepterDemande(userId, expediteur.id)
            chargerDonnees() // Rafraîchit les amis et les demandes
        }
    }

    fun refuserDemande(expediteur: Utilisateur) {
        viewModelScope.launch {
            val userId = Supabase.client.auth.currentUserOrNull()?.id ?: return@launch
            repository.refuserDemande(userId, expediteur.id)
            chargerDonnees() // Rafraîchit les demandes
        }
    }

    fun supprimer(contact: Utilisateur) {
        viewModelScope.launch {
            val userId = Supabase.client.auth.currentUserOrNull()?.id ?: return@launch
            repository.supprimerAmi(userId, contact.id)
            chargerDonnees()
        }
    }

    fun effacerErreur() {
        _messageErreur.value = null
    }
}