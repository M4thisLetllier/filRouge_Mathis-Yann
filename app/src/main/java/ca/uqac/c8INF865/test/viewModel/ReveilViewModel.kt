package ca.uqac.c8INF865.test.viewModel

import androidx.lifecycle.ViewModel
import ca.uqac.c8INF865.test.screens.ModeleReveil
import ca.uqac.c8INF865.test.screens.listeDeReveilsStatistiques
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// État global de la page d'accueil
data class AccueilUiState(
    val listeReveils: List<ModeleReveil> = emptyList()
)

class ReveilViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AccueilUiState(listeReveils = listeDeReveilsStatistiques))
    val uiState: StateFlow<AccueilUiState> = _uiState.asStateFlow()

    fun basculerEtatReveil(idReveil: Int, estActif: Boolean) {
        _uiState.update { etatActuel ->
            val listeMiseAJour = etatActuel.listeReveils.map { reveil ->
                if (reveil.id == idReveil) reveil.copy(actifParDefaut = estActif) else reveil
            }
            etatActuel.copy(listeReveils = listeMiseAJour)
        }
    }
}