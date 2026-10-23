package ca.uqac.c8INF865.test.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.uqac.c8INF865.test.data.SonDao
import ca.uqac.c8INF865.test.data.SonEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SonViewModel(private val dao: SonDao) : ViewModel() {
    val sons: StateFlow<List<SonEntity>> = dao.observerTous().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun ajouter(nom: String, chemin: String) {
        viewModelScope.launch {
            dao.inserer(SonEntity(nom = nom, chemin = chemin, dateCreation = System.currentTimeMillis()))
        }
    }

    fun supprimer(son: SonEntity) {
        viewModelScope.launch {
            dao.supprimer(son)
        }
    }
}
