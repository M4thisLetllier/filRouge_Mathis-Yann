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

class SonViewModel : ViewModel() {
    private val repository = SonRepository()

    private val _sons = MutableStateFlow<List<Son>>(emptyList())
    val sons: StateFlow<List<Son>> = _sons.asStateFlow()

    init {
        chargerSons()
    }

    private fun chargerSons() {
        viewModelScope.launch {
            // On récupère l'ID du vrai utilisateur connecté
            val userId = Supabase.client.auth.currentUserOrNull()?.id ?: return@launch

            val liste = repository.recupererSons(userId)
            _sons.value = liste
        }
    }

    fun uploaderSon(nom: String, cheminLocal: String) {
        viewModelScope.launch {
            // On récupère l'ID du vrai utilisateur connecté
            val userId = Supabase.client.auth.currentUserOrNull()?.id ?: return@launch

            val succes = repository.uploaderSon(userId, nom, cheminLocal)
            if (succes) {
                chargerSons() // Rafraîchit la liste si l'upload a fonctionné
            }
        }
    }

    fun supprimer(son: Son) {
        viewModelScope.launch {
            // Appel au repository pour supprimer le fichier et la donnée
            val succes = repository.supprimerSon(son)

            if (succes) {
                // Rafraîchir la liste après la suppression
                // (Remplacez chargerSons() par le nom exact de votre fonction de rechargement)
                chargerSons()
            }
        }
    }

    fun renommerSon(son: Son, nouveauNom: String) {
        viewModelScope.launch {
            val succes = repository.modifierNomSon(son.id, nouveauNom)
            if (succes) {
                // Remplacez 'chargerSons()' par le nom de votre fonction qui récupère la liste
                chargerSons()
            }
        }
    }
}