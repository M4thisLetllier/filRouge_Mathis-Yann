package ca.uqac.c8INF865.test.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.uqac.c8INF865.test.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _chargement = MutableStateFlow(false)
    val chargement: StateFlow<Boolean> = _chargement.asStateFlow()

    private val _erreur = MutableStateFlow<String?>(null)
    val erreur: StateFlow<String?> = _erreur.asStateFlow()

    private val _connecte = MutableStateFlow(repository.recupererIdUtilisateurCourant() != null)
    val connecte: StateFlow<Boolean> = _connecte.asStateFlow()

    fun inscription(email: String, motDePasse: String, nom: String, telephone: String) {
        viewModelScope.launch {
            _chargement.value = true
            _erreur.value = null
            val succes = repository.inscription(email, motDePasse, nom, telephone)
            if (succes) {
                _connecte.value = true
            } else {
                _erreur.value = "L'inscription a échoué. Vérifiez vos informations."
            }
            _chargement.value = false
        }
    }

    fun connexion(email: String, motDePasse: String) {
        viewModelScope.launch {
            _chargement.value = true
            _erreur.value = null
            val succes = repository.connexion(email, motDePasse)
            if (succes) {
                _connecte.value = true
            } else {
                _erreur.value = "Identifiants incorrects."
            }
            _chargement.value = false
        }
    }
}