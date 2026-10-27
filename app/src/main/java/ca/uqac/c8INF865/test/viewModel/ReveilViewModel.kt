package ca.uqac.c8INF865.test.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.uqac.c8INF865.test.data.Reveil
import ca.uqac.c8INF865.test.network.Supabase
import ca.uqac.c8INF865.test.repository.ReveilRepository
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AccueilUiState(
    val listeReveils: List<Reveil> = emptyList()
)

class ReveilViewModel : ViewModel() {
    private val repository = ReveilRepository()

    private val _uiState = MutableStateFlow(AccueilUiState())
    val uiState: StateFlow<AccueilUiState> = _uiState.asStateFlow()

    private val _contactsParReveil = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val contactsParReveil: StateFlow<Map<String, Set<String>>> = _contactsParReveil.asStateFlow()

    init {
        chargerReveils()
    }

    private fun chargerReveils() {
        viewModelScope.launch {
            val userId = Supabase.client.auth.currentUserOrNull()?.id ?: return@launch

            // 1. Charger les réveils
            val reveils = repository.recupererReveils(userId)
            _uiState.value = AccueilUiState(reveils)

            // 2. Charger les membres de ces réveils
            val ids = reveils.map { it.id }
            _contactsParReveil.value = repository.recupererMembresParReveil(ids)
        }
    }

    fun basculerEtatReveil(idReveil: String, estActif: Boolean) {
        viewModelScope.launch {
            val succes = repository.mettreAJourReveil(idReveil, mapOf("actif" to estActif))
            if (succes) chargerReveils()
        }
    }

    fun ajouterReveil(nom: String, heure: String) {
        viewModelScope.launch {
            val userId = Supabase.client.auth.currentUserOrNull()?.id ?: return@launch
            val succes = repository.ajouterReveil(userId, nom.trim(), heure)
            if (succes) chargerReveils()
        }
    }

    fun modifierReveil(id: String, nom: String, heure: String) {
        viewModelScope.launch {
            val succes = repository.mettreAJourReveil(
                id,
                mapOf("nom" to nom.trim(), "heure" to heure)
            )
            if (succes) chargerReveils()
        }
    }

    fun supprimerReveil(id: String) {
        viewModelScope.launch {
            val succes = repository.supprimerReveil(id)
            if (succes) chargerReveils()
        }
    }

    fun basculerContact(idReveil: String, idContact: String, associe: Boolean) {
        viewModelScope.launch {
            val succes = repository.basculerMembre(idReveil, idContact, associe)
            if (succes) chargerReveils()
        }
    }
}