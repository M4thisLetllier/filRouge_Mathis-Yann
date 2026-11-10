package ca.uqac.c8INF865.test.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ca.uqac.c8INF865.test.data.Reveil
import ca.uqac.c8INF865.test.data.Son
import ca.uqac.c8INF865.test.data.Utilisateur
import ca.uqac.c8INF865.test.network.Supabase
import ca.uqac.c8INF865.test.repository.MembreReveilInfo
import ca.uqac.c8INF865.test.repository.ReveilCacheLocal
import ca.uqac.c8INF865.test.repository.ReveilRepository
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AccueilUiState(
    val listeReveils: List< Reveil > = emptyList()
)
//androidViewModel donne accès au contexte de l'application sans jamais créer de fuite de mémoire
//besoin pour le worker et bootreceiver
class ReveilViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ReveilRepository()
    private val appContext = application.applicationContext

    private val _currentUserId = MutableStateFlow(Supabase.client.auth.currentUserOrNull()?.id)
    val currentUserId: StateFlow< String? > = _currentUserId.asStateFlow()

    private val _uiState = MutableStateFlow(AccueilUiState())
    val uiState: StateFlow< AccueilUiState > = _uiState.asStateFlow()

    private val _contactsParReveil = MutableStateFlow< Map< String, List< MembreReveilInfo > > >(emptyMap())
    val contactsParReveil: StateFlow< Map< String, List< MembreReveilInfo > > > = _contactsParReveil.asStateFlow()

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
            _currentUserId.value = userId

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

            // 4. Sauvegarder dans SharedPreferences pour le BootReceiver et le Worker
            val cacheLocal = reveils.map { reveil ->
                val monSonId = if (reveil.createur_id == userId) {
                    reveil.son_id
                } else {
                    membresMap[reveil.id]?.find { it.membreId == userId }?.sonId
                }
                val monSonUrl = monSonId?.let { sonsMap[it]?.url }
                ReveilCacheLocal(reveil = reveil, monSonId = monSonId, monSonUrl = monSonUrl)
            }
            repository.sauvegarderReveilsDansPrefs(appContext, cacheLocal)

            // 5. Récupérer les profils des utilisateurs
            val userIds = mutableSetOf< String >()
            userIds.add(userId)
            reveils.forEach { userIds.add(it.createur_id) }
            membresMap.values.flatten().forEach { userIds.add(it.membreId) }
            sonsMap.values.forEach { userIds.add(it.utilisateur_id) }

            _utilisateursConnus.value = repository.recupererUtilisateursParIds(userIds.toList())
        }
    }

    // Ajouter un réveil
    fun ajouterReveil(nom: String, heure: String) {
        viewModelScope.launch {
            val userId = Supabase.client.auth.currentUserOrNull()?.id ?: return@launch
            val succes = repository.ajouterReveil(userId, nom.trim(), heure)
            if (succes) chargerReveils()
        }
    }

    // Activer ou désactiver un réveil
    fun basculerEtatReveil(idReveil: String, estActif: Boolean) {
        viewModelScope.launch {
            val succes = repository.mettreAJourEtatReveil(idReveil, estActif)
            if (succes) chargerReveils()
        }
    }

    // Modifier les informations d'un réveil
    fun modifierReveil(id: String, nom: String, heure: String) {
        viewModelScope.launch {
            val succes = repository.mettreAJourInfosReveil(id, nom.trim(), heure)
            if (succes) chargerReveils()
        }
    }

    // Supprimer un réveil
    fun supprimerReveil(id: String) {
        viewModelScope.launch {
            val succes = repository.supprimerReveil(id)
            if (succes) chargerReveils()
        }
    }

    // Ajouter ou retirer un contact d'un réveil
    fun basculerContact(idReveil: String, idContact: String, associe: Boolean) {
        viewModelScope.launch {
            val succes = repository.basculerMembre(idReveil, idContact, associe)
            if (succes) chargerReveils()
        }
    }

    // Attribuer un son à un contact du réveil
    fun assignerSonContact(idReveil: String, idContact: String, idSon: String) {
        viewModelScope.launch {
            val succes = repository.assignerSonMembre(idReveil, idContact, idSon)
            if (succes) chargerReveils()
        }
    }

    // Attribuer un son au créateur du réveil
    fun assignerSonCreateur(idReveil: String, idSon: String) {
        viewModelScope.launch {
            val succes = repository.assignerSonCreateur(idReveil, idSon)
            if (succes) chargerReveils()
        }
    }
}