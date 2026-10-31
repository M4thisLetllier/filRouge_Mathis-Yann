package ca.uqac.c8INF865.test.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.uqac.c8INF865.test.repository.AuthRepository
import ca.uqac.c8INF865.test.data.Utilisateur
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

    private val _profil = MutableStateFlow<Utilisateur?>(null)
    val profil: StateFlow<Utilisateur?> = _profil.asStateFlow()

    private val _chargementProfil = MutableStateFlow(false)
    val chargementProfil: StateFlow<Boolean> = _chargementProfil.asStateFlow()

    private val _sauvegardeProfil = MutableStateFlow(false)
    val sauvegardeProfil: StateFlow<Boolean> = _sauvegardeProfil.asStateFlow()

    private val _erreurProfil = MutableStateFlow<String?>(null)
    val erreurProfil: StateFlow<String?> = _erreurProfil.asStateFlow()

    init {
        if (_connecte.value) {
            recupererProfil()
        }
    }

    fun inscription(email: String, motDePasse: String, nom: String, telephone: String) {
        viewModelScope.launch {
            _chargement.value = true
            _erreur.value = null
            val succes = repository.inscription(email, motDePasse, nom, telephone)
            if (succes) {
                _connecte.value = true
                recupererProfil()
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
                recupererProfil()
            } else {
                _erreur.value = "Identifiants incorrects."
            }
            _chargement.value = false
        }
    }

    fun recupererProfil() {
        viewModelScope.launch {
            _chargementProfil.value = true
            _erreurProfil.value = null
            _profil.value = repository.recupererProfil()
            if (_profil.value == null) {
                _erreurProfil.value = "Impossible de charger le profil."
            }
            _chargementProfil.value = false
        }
    }

    fun mettreAJourProfil(nom: String, telephone: String, onSucces: () -> Unit) {
        viewModelScope.launch {
            _sauvegardeProfil.value = true
            _erreurProfil.value = null
            val succes = repository.mettreAJourProfil(nom.trim(), telephone.trim())
            if (succes) {
                _profil.value = _profil.value?.copy(nom = nom.trim(), telephone = telephone.trim())
                onSucces()
            } else {
                _erreurProfil.value = "Impossible d'enregistrer les modifications."
            }
            _sauvegardeProfil.value = false
        }
    }

    fun deconnexion(onTerminee: () -> Unit) {
        viewModelScope.launch {
            repository.deconnexion()
            _profil.value = null
            _connecte.value = false
            onTerminee()
        }
    }
}