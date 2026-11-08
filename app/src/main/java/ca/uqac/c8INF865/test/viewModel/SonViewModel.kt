package ca.uqac.c8INF865.test.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.uqac.c8INF865.test.data.Son
import ca.uqac.c8INF865.test.network.Supabase
import ca.uqac.c8INF865.test.repository.SonRepository
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// --------------------------------------------------------
// VIEWMODEL DES SONS
// --------------------------------------------------------

class SonViewModel : ViewModel() {
    private val repository = SonRepository()

    // Liste des sons appartenant à l'utilisateur connecté
    private val _sons = MutableStateFlow<List<Son>>(emptyList())
    val sons: StateFlow<List<Son>> = _sons.asStateFlow()

    // Charger les sons au démarrage
    init {
        chargerSons()
    }

    // Récupérer les sons de l'utilisateur connecté
    private fun chargerSons() {
        viewModelScope.launch {
            val userId = Supabase.client.auth.currentUserOrNull()?.id ?: return@launch

            val liste = repository.recupererSons(userId)
            _sons.value = liste
        }
    }

    // Importer un nouveau son
    fun uploaderSon(nom: String, cheminLocal: String) {
        viewModelScope.launch {
            val userId = Supabase.client.auth.currentUserOrNull()?.id ?: return@launch

            val succes = repository.uploaderSon(userId, nom, cheminLocal)
            if (succes) {
                chargerSons()
            }
        }
    }

    // Supprimer un son
    fun supprimer(son: Son) {
        viewModelScope.launch {
            val succes = repository.supprimerSon(son)

            if (succes) {
                chargerSons()
            }
        }
    }

    // Renommer un son
    fun renommerSon(son: Son, nouveauNom: String) {
        viewModelScope.launch {
            val succes = repository.modifierNomSon(son.id, nouveauNom)
            if (succes) {
                chargerSons()
            }
        }
    }
}