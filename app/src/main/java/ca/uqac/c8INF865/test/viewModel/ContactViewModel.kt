package ca.uqac.c8INF865.test.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import ca.uqac.c8INF865.test.screens.Contact
import ca.uqac.c8INF865.test.screens.testContacts

class ContactViewModel : ViewModel() {
    var recherche by mutableStateOf("")
        private set

    val contactsFiltres: List<Contact>
        get() {
            val texteRecherche = recherche.trim()

            return testContacts.filter { contact ->
                contact.name.contains(texteRecherche, ignoreCase = true) ||
                        contact.phone.contains(texteRecherche, ignoreCase = true)
            }
        }

    fun modifierRecherche(nouvelleRecherche: String) {
        recherche = nouvelleRecherche
    }
}