package ca.uqac.c8INF865.test.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.uqac.c8INF865.test.data.ContactDao
import ca.uqac.c8INF865.test.data.ContactEntity
import ca.uqac.c8INF865.test.screens.Contact
import ca.uqac.c8INF865.test.screens.testContacts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContactViewModel(private val dao: ContactDao) : ViewModel() {
    var recherche by mutableStateOf("")
        private set

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts = _contacts.asStateFlow()

    val contactsFiltres: List<Contact>
        get() {
            val texteRecherche = recherche.trim()
            return _contacts.value.filter { contact ->
                contact.name.contains(texteRecherche, ignoreCase = true) ||
                    contact.phone.contains(texteRecherche, ignoreCase = true)
            }
        }

    init {
        viewModelScope.launch {
            if (dao.compter() == 0) {
                dao.insererTous(testContacts.map { ContactEntity(it.id, it.name, it.phone) })
            }
        }
        viewModelScope.launch {
            dao.observerTous().collect { contacts ->
                _contacts.value = contacts.map { Contact(it.id, it.name, it.phone) }
            }
        }
    }

    fun modifierRecherche(nouvelleRecherche: String) {
        recherche = nouvelleRecherche
    }
}