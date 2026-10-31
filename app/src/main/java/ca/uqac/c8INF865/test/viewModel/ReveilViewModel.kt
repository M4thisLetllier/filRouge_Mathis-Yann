package ca.uqac.c8INF865.test.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.uqac.c8INF865.test.data.Reveil
import ca.uqac.c8INF865.test.data.Son
import ca.uqac.c8INF865.test.data.Utilisateur
import ca.uqac.c8INF865.test.network.Supabase
import ca.uqac.c8INF865.test.repository.MembreReveilInfo
import ca.uqac.c8INF865.test.repository.ReveilRepository
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AccueilUiState(
    val listeReveils: List< Reveil > = emptyList()
)

class ReveilViewModel : ViewModel() {
    private val repository = ReveilRepository()

    private val _uiState = MutableStateFlow(AccueilUiState())
    val uiState: StateFlow< AccueilUiState > = _uiState.asStateFlow()

    private val _contactsParReveil = MutableStateFlow< Map< String, List< MembreReveilInfo > > >(emptyMap())
    val contactsParReveil: StateFlow< Map< String, List< MembreReveilInfo > > > = _contactsParReveil.asStateFlow()

    // NOUVEAU : Stocke les sons assignés et les utilisateurs liés (créateurs, amis, auteurs de sons)
    private val _sonsAssignes = MutableStateFlow< Map< String, Son > >(emptyMap())
    val sonsAssignes: StateFlow< Map< String, Son > > = _sonsAssignes.asStateFlow()

    private val _utilisateursConnus = MutableStateFlow< Map< String, Utilisateur > >(emptyMap())
    val utilisateursConnus: StateFlow< Map< String, Utilisateur > > = _utilisateursConnus.asStateFlow()

    init {
        chargerReveils()
    }

    fun chargerReveils() {
        viewModelScope.launch {
            val userId = Supabase.client.auth.currentUserOrNull()?.id ?: return@launch

            // 1. Charger les réveils
            val reveils = repository.recupererReveils(userId)
            _uiState.value = AccueilUiState(reveils)

            // 2. Charger les membres de ces réveils
            val ids = reveils.map { it.id }
            val membresMap = repository.recupererMembresParReveil(ids)
            _contactsParReveil.value = membresMap

            // 3. Récupérer tous les sons assignés (aux membres ET aux créateurs)
            val sonIdsMembres = membresMap.values.flatten().mapNotNull { it.sonId }
            val sonIdsCreateurs = reveils.mapNotNull { it.son_id }
            val tousLesSonIds = (sonIdsMembres + sonIdsCreateurs).distinct()
            val sonsMap = repository.recupererSonsParIds(tousLesSonIds)
            _sonsAssignes.value = sonsMap

            // 4. Récupérer les profils de tous les utilisateurs impliqués (pour afficher les pseudos)
            val userIds = mutableSetOf< String >()
            userIds.add(userId)
            reveils.forEach { userIds.add(it.createur_id) }
            membresMap.values.flatten().forEach { userIds.add(it.membreId) }
            sonsMap.values.forEach { userIds.add(it.utilisateur_id) }

            _utilisateursConnus.value = repository.recupererUtilisateursParIds(userIds.toList())
        }
    }

    fun ajouterReveil(nom: String, heure: String) {
        viewModelScope.launch {
            val userId = Supabase.client.auth.currentUserOrNull()?.id ?: return@launch
            val succes = repository.ajouterReveil(userId, nom.trim(), heure)
            if (succes) chargerReveils()
        }
    }

    fun basculerEtatReveil(idReveil: String, estActif: Boolean) {
        viewModelScope.launch {
            val succes = repository.mettreAJourEtatReveil(idReveil, estActif)
            if (succes) chargerReveils()
        }
    }

    fun modifierReveil(id: String, nom: String, heure: String) {
        viewModelScope.launch {
            val succes = repository.mettreAJourInfosReveil(id, nom.trim(), heure)
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

    fun assignerSonContact(idReveil: String, idContact: String, idSon: String) {
        viewModelScope.launch {
            val succes = repository.assignerSonMembre(idReveil, idContact, idSon)
            if (succes) chargerReveils()
        }
    }

    //Assigner un son au créateur du réveil
    fun assignerSonCreateur(idReveil: String, idSon: String) {
        viewModelScope.launch {
            val succes = repository.assignerSonCreateur(idReveil, idSon)
            if (succes) chargerReveils()
        }
    }
}