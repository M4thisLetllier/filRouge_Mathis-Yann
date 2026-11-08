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

// --------------------------------------------------------
// VIEWMODEL DES CONTACTS
// --------------------------------------------------------

class ContactViewModel : ViewModel() {
    private val repository = ContactRepository()

    // Texte saisi dans la recherche de contacts
    var recherche by mutableStateOf("")
        private set

    // Liste des contacts de l'utilisateur connecté
    private val _contacts = MutableStateFlow<List<Utilisateur>>(emptyList())
    val contacts: StateFlow<List<Utilisateur>> = _contacts.asStateFlow()

    // Demandes d'amitié reçues
    private val _demandes = MutableStateFlow<List<Utilisateur>>(emptyList())
    val demandes: StateFlow<List<Utilisateur>> = _demandes.asStateFlow()

    // Message d'erreur affiché lors d'une action sur les contacts
    private val _messageErreur = MutableStateFlow<String?>(null)
    val messageErreur: StateFlow<String?> = _messageErreur.asStateFlow()

    // Charger les contacts et les demandes au démarrage
    init {
        chargerDonnees()
    }

    // Récupérer les contacts et les demandes de l'utilisateur connecté
    private fun chargerDonnees() {
        viewModelScope.launch {
            val userId = Supabase.client.auth.currentUserOrNull()?.id ?: return@launch
            _contacts.value = repository.recupererContacts(userId)
            _demandes.value = repository.recupererDemandes(userId)
        }
    }

    // Mettre à jour le texte de recherche
    fun modifierRecherche(nouvelleRecherche: String) {
        recherche = nouvelleRecherche
    }

    // Envoyer une demande d'amitié à partir d'un numéro de téléphone
    fun ajouterContact(telephone: String) {
        viewModelScope.launch {
            val userId = Supabase.client.auth.currentUserOrNull()?.id ?: return@launch
            val succes = repository.envoyerDemandeParTelephone(userId, telephone)

            if (succes) {
                _messageErreur.value = null
            } else {
                _messageErreur.value = "Aucun utilisateur trouvé ou demande déjà envoyée."
            }
        }
    }

    // Accepter une demande d'amitié
    fun accepterDemande(expediteur: Utilisateur) {
        viewModelScope.launch {
            val userId = Supabase.client.auth.currentUserOrNull()?.id ?: return@launch
            repository.accepterDemande(userId, expediteur.id)
            chargerDonnees()
        }
    }

    // Refuser une demande d'amitié
    fun refuserDemande(expediteur: Utilisateur) {
        viewModelScope.launch {
            val userId = Supabase.client.auth.currentUserOrNull()?.id ?: return@launch
            repository.refuserDemande(userId, expediteur.id)
            chargerDonnees()
        }
    }

    // Supprimer un contact
    fun supprimer(contact: Utilisateur) {
        viewModelScope.launch {
            val userId = Supabase.client.auth.currentUserOrNull()?.id ?: return@launch
            repository.supprimerAmi(userId, contact.id)
            chargerDonnees()
        }
    }

    // Effacer le message d'erreur courant
    fun effacerErreur() {
        _messageErreur.value = null
    }
}