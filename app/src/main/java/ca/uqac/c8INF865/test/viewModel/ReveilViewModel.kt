package ca.uqac.c8INF865.test.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
data class ModeleReveil(
    val id: Int,
    val nom: String,
    val heure: String,
    val dateProchaine: String,
    val nbAmis: Int,
    var actifParDefaut: Boolean
)
val listeDeReveilsStatistiques = listOf(
    ModeleReveil(1, "Réveil 1", "07:00", "Mercredi 30 Sept.", 2, true),
    ModeleReveil(2, "Réveil 2", "08:30", "Mercredi 30 Sept.", 5, true),
    ModeleReveil(3, "Réveil 3", "09:00", "Jeudi 1 Oct.", 0, false),
    ModeleReveil(4, "Réveil 4", "10:15", "Vendredi 2 Oct.", 3, true),
    ModeleReveil(5, "Réveil 5", "14:00", "Lundi 5 Oct.", 1, false)
)

class ReveilViewModel : ViewModel() {

    // 1. L'état mutable (Privé) : initialisé avec ta liste par défaut
    private val _listeReveils = MutableStateFlow<List<ModeleReveil>>(listeDeReveilsStatistiques)

    // 2. L'état immuable (Public) : exposé à l'interface graphique
    val listeReveils: StateFlow<List<ModeleReveil>> = _listeReveils.asStateFlow()

    // 3. La fonction pour modifier une donnée
    fun basculerEtatReveil(idReveil: Int, estActif: Boolean) {
        // .update permet de lire la valeur actuelle et d'en renvoyer une nouvelle
        _listeReveils.update { listeActuelle ->
            // On recrée la liste en modifiant uniquement le réveil concerné
            listeActuelle.map { reveil ->
                if (reveil.id == idReveil) {
                    reveil.copy(actifParDefaut = estActif) // copy() crée un nouvel objet avec la modif
                } else {
                    reveil
                }
            }
        }
    }
}