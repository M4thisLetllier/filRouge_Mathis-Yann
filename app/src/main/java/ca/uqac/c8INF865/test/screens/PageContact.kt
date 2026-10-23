package ca.uqac.c8INF865.test.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.uqac.c8INF865.test.viewModel.ContactViewModel

// Structure d'un contact
data class Contact(
    val id: Int,
    val name: String,
    val phone: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranContact(
    contactViewModel: ContactViewModel = viewModel() // Instanciation autonome sans Room
) {
    val contacts by contactViewModel.contacts.collectAsState()
    val texteRecherche = contactViewModel.recherche.trim()

    // Gérer l'affichage du formulaire d'ajout
    var afficherAjoutContact by remember { mutableStateOf(false) }

    val contactsFiltres = contacts.filter { contact ->
        contact.name.contains(texteRecherche, ignoreCase = true) ||
                contact.phone.contains(texteRecherche, ignoreCase = true)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { afficherAjoutContact = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Ajouter un contact"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = contactViewModel.recherche,
                onValueChange = { contactViewModel.modifierRecherche(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Rechercher un contact...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Icône de recherche"
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(contactsFiltres) { contact ->
                    ContactCard(
                        contact = contact,
                        onSupprimer = { contactViewModel.supprimer(contact) }
                    )
                }
            }
        }
    }

    // Affichage du formulaire d'ajout par-dessus l'écran
    if (afficherAjoutContact) {
        FormulaireContact(
            onDismiss = { afficherAjoutContact = false },
            onConfirm = { nom, telephone ->
                contactViewModel.ajouterContact(nom, telephone)
                afficherAjoutContact = false
            }
        )
    }
}

// Brique d'une personne dans la liste
@Composable
fun ContactCard(contact: Contact, onSupprimer: () -> Unit = {}) {
    var afficherConfirmation by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = contact.phone,
                )
            }

            IconButton(onClick = { afficherConfirmation = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Supprimer ${contact.name}"
                )
            }
        }
    }

    if (afficherConfirmation) {
        AlertDialog(
            onDismissRequest = { afficherConfirmation = false },
            title = { Text("Supprimer le contact ?") },
            text = {
                Text("Voulez-vous vraiment supprimer ${contact.name} ? Cette action est irréversible.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        afficherConfirmation = false
                        onSupprimer()
                    }
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { afficherConfirmation = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

// Boîte de dialogue pour ajouter un nouveau contact
@Composable
private fun FormulaireContact(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var nom by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un contact") },
        text = {
            Column {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text("Nom du contact") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = telephone,
                    onValueChange = { telephone = it },
                    label = { Text("Numéro de téléphone") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(nom, telephone) },
                enabled = nom.isNotBlank() && telephone.isNotBlank()
            ) {
                Text("Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}