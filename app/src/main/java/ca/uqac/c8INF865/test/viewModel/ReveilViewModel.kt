package ca.uqac.c8INF865.test.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.uqac.c8INF865.test.data.ReveilDao
import ca.uqac.c8INF865.test.data.ReveilEntity
import ca.uqac.c8INF865.test.data.ReveilContactDao
import ca.uqac.c8INF865.test.data.ReveilContactEntity
import ca.uqac.c8INF865.test.screens.ModeleReveil
import ca.uqac.c8INF865.test.screens.listeDeReveilsStatistiques
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// État global de la page d'accueil
data class AccueilUiState(
    val listeReveils: List<ModeleReveil> = emptyList()
)

class ReveilViewModel(
    private val dao: ReveilDao,
    private val associationDao: ReveilContactDao
) : ViewModel() {
    private val _uiState = MutableStateFlow(AccueilUiState())
    val uiState: StateFlow<AccueilUiState> = _uiState.asStateFlow()
    private val _contactsParReveil = MutableStateFlow<Map<Int, Set<Int>>>(emptyMap())
    val contactsParReveil: StateFlow<Map<Int, Set<Int>>> = _contactsParReveil.asStateFlow()

    init {
        viewModelScope.launch {
            if (dao.compter() == 0) {
                dao.insererTous(listeDeReveilsStatistiques.map { it.toEntity() })
            }
        }
        viewModelScope.launch {
            dao.observerTous().collect { reveils ->
                _uiState.value = AccueilUiState(reveils.map { it.toModel() })
            }
        }
        viewModelScope.launch {
            associationDao.observerToutesLesAssociations().collect { associations ->
                _contactsParReveil.value = associations.groupBy { it.reveilId }
                    .mapValues { (_, values) -> values.map { it.contactId }.toSet() }
            }
        }
    }

    fun basculerEtatReveil(idReveil: Int, estActif: Boolean) {
        viewModelScope.launch {
            dao.modifierEtat(idReveil, estActif)
        }
    }

    fun ajouterReveil(nom: String, heure: String) {
        viewModelScope.launch {
            val id = dao.prochainId()
            dao.inserer(ReveilEntity(id, nom.trim(), heure, "À venir", 0, true))
        }
    }

    fun modifierReveil(id: Int, nom: String, heure: String) {
        viewModelScope.launch {
            dao.modifierDetails(id, nom.trim(), heure)
        }
    }

    fun supprimerReveil(id: Int) {
        viewModelScope.launch {
            associationDao.supprimerAssociations(id)
            dao.supprimer(id)
        }
    }

    fun basculerContact(idReveil: Int, idContact: Int, associe: Boolean) {
        viewModelScope.launch {
            if (associe) {
                associationDao.associer(ReveilContactEntity(idReveil, idContact))
            } else {
                associationDao.dissocier(idReveil, idContact)
            }
        }
    }
}

private fun ModeleReveil.toEntity() = ReveilEntity(id, nom, heure, dateProchaine, nbAmis, actifParDefaut)

private fun ReveilEntity.toModel() =
    ModeleReveil(id, nom, heure, dateProchaine, nbAmis, actif)