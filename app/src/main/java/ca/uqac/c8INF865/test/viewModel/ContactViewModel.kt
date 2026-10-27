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

    private val _messageErreur = MutableStateFlow<String?>(null)
    val messageErreur: StateFlow<String?> = _messageErreur.asStateFlow()

    init {
        chargerContacts()
    }

    private fun chargerContacts() {
        viewModelScope.launch {
            val userId = Supabase.client.auth.currentUserOrNull()?.id ?: return@launch
            _contacts.value = repository.recupererContacts(userId)
        }
    }

    fun modifierRecherche(nouvelleRecherche: String) {
        recherche = nouvelleRecherche
    }

    fun ajouterContact(telephone: String) {
        viewModelScope.launch {
            val userId = Supabase.client.auth.currentUserOrNull()?.id ?: return@launch
            val succes = repository.ajouterAmiParTelephone(userId, telephone)

            if (succes) {
                chargerContacts()
                _messageErreur.value = null
            } else {
                _messageErreur.value = "Aucun utilisateur trouvé avec ce numéro."
            }
        }
    }

    fun supprimer(contact: Utilisateur) {
        viewModelScope.launch {
            val userId = Supabase.client.auth.currentUserOrNull()?.id ?: return@launch
            repository.supprimerAmi(userId, contact.id)
            chargerContacts()
        }
    }

    fun effacerErreur() {
        _messageErreur.value = null
    }
}