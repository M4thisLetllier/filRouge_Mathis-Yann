package ca.uqac.c8INF865.test.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import ca.uqac.c8INF865.test.data.Utilisateur
import ca.uqac.c8INF865.test.viewModel.ContactViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranContact(
    contactViewModel: ContactViewModel = viewModel()
) {
    // États existants
    val contacts by contactViewModel.contacts.collectAsState()
    val erreur by contactViewModel.messageErreur.collectAsState()
    val texteRecherche = contactViewModel.recherche.trim()

    val demandes by contactViewModel.demandes.collectAsState()

    // Gestion des onglets (0 = Amis, 1 = Demandes)
    var onglet by remember { mutableIntStateOf(0) }
    var afficherAjoutContact by remember { mutableStateOf(false) }

    val contactsFiltres = contacts.filter { contact ->
        contact.nom.contains(texteRecherche, ignoreCase = true) ||
                contact.telephone.contains(texteRecherche, ignoreCase = true)
    }

    Scaffold(
        floatingActionButton = {
            // On affiche le bouton d'ajout uniquement sur l'onglet "Mes amis"
            if (onglet == 0) {
                FloatingActionButton(
                    onClick = {
                        contactViewModel.effacerErreur()
                        afficherAjoutContact = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Ajouter un contact"
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // NOUVEAU : Les onglets de navigation
            TabRow(selectedTabIndex = onglet) {
                listOf("Mes amis", "Demandes").forEachIndexed { index, titre ->
                    Tab(
                        selected = onglet == index,
                        onClick = { onglet = index },
                        text = { Text(titre) }
                    )
                }
            }

            if (onglet == 0) {
                // CONTENU DE L'ONGLET 0 : MES AMIS
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

                if (contactsFiltres.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Aucun ami trouvé", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
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
            } else {
                // CONTENU DE L'ONGLET 1 : DEMANDES D'AMIS
                Spacer(modifier = Modifier.height(16.dp))

                if (demandes.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Aucune demande en attente", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(demandes) { demandeur ->
                            DemandeCard(
                                contact = demandeur,
                                onAccepter = { contactViewModel.accepterDemande(demandeur) },
                                onRefuser = { contactViewModel.refuserDemande(demandeur) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (afficherAjoutContact) {
        FormulaireContact(
            erreur = erreur,
            onDismiss = { afficherAjoutContact = false },
            onConfirm = { telephone ->
                // Cette méthode devrait maintenant insérer dans "demandes_amis" et non "amis"
                contactViewModel.ajouterContact(telephone)
                if (erreur == null) {
                    afficherAjoutContact = false
                }
            }
        )
    }
}

// NOUVEAU COMPOSANT : Carte pour les demandes d'amis
@Composable
fun DemandeCard(contact: Utilisateur, onAccepter: () -> Unit, onRefuser: () -> Unit) {
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
                color = MaterialTheme.colorScheme.secondaryContainer
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
                Text(text = contact.nom, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = contact.telephone, style = MaterialTheme.typography.bodySmall)
            }

            Row {
                IconButton(onClick = onAccepter) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Accepter",
                        tint = Color(0xFF4CAF50) // Vert
                    )
                }
                IconButton(onClick = onRefuser) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Refuser",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// Carte de contact inchangée
@Composable
fun ContactCard(contact: Utilisateur, onSupprimer: () -> Unit = {}) {
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
                Text(text = contact.nom)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = contact.telephone)
            }

            IconButton(onClick = { afficherConfirmation = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Supprimer ${contact.nom}"
                )
            }
        }
    }

    if (afficherConfirmation) {
        AlertDialog(
            onDismissRequest = { afficherConfirmation = false },
            title = { Text("Retirer des amis ?") },
            text = { Text("Voulez-vous vraiment retirer ${contact.nom} de vos amis ?") },
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

// Formulaire légèrement adapté pour le texte
@Composable
private fun FormulaireContact(
    erreur: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var telephone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Envoyer une demande d'ami") },
        text = {
            Column {
                Text("Entrez le numéro de téléphone pour envoyer une demande de contact.")
                Spacer(modifier = Modifier.height(16.dp))

                if (erreur != null) {
                    Text(text = erreur, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = telephone,
                    onValueChange = { telephone = it },
                    label = { Text("Numéro de téléphone") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    isError = erreur != null
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(telephone) },
                enabled = telephone.isNotBlank()
            ) {
                Text("Envoyer la demande")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}