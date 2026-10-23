package ca.uqac.c8INF865.test.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ca.uqac.c8INF865.test.data.AppDatabase

class AppViewModelFactory(
    private val database: AppDatabase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        when {
            modelClass.isAssignableFrom(ReveilViewModel::class.java) ->
                ReveilViewModel(database.reveilDao(), database.reveilContactDao()) as T
            modelClass.isAssignableFrom(ContactViewModel::class.java) ->
                ContactViewModel(database.contactDao(), database.reveilContactDao()) as T
            modelClass.isAssignableFrom(SonViewModel::class.java) ->
                SonViewModel(database.sonDao()) as T
            else -> throw IllegalArgumentException("ViewModel inconnu: ${modelClass.name}")
        }
}
