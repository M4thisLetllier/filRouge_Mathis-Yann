package ca.uqac.c8INF865.test.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.uqac.c8INF865.test.data.Reveil
import ca.uqac.c8INF865.test.data.Son
import ca.uqac.c8INF865.test.data.Utilisateur
import ca.uqac.c8INF865.test.viewModel.ContactViewModel
import ca.uqac.c8INF865.test.viewModel.ReveilViewModel
import ca.uqac.c8INF865.test.viewModel.SonViewModel
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranModificationReveil(
    reveil: Reveil,
    onRetour: () -> Unit,
    reveilViewModel: ReveilViewModel = viewModel(),
    contactViewModel: ContactViewModel = viewModel(),
    sonViewModel: SonViewModel = viewModel() // Ajout du ViewModel des sons
) {
    val contactsParReveil by reveilViewModel.contactsParReveil.collectAsState()
    val contacts by contactViewModel.contacts.collectAsState()

    // Récupération de la liste des sons de l'utilisateur
    val sons by sonViewModel.sons.collectAsState()

    val estNouveau = reveil.id.isEmpty()
    var nom by remember(reveil.nom) { mutableStateOf(reveil.nom) }
    var heure by remember(reveil.heure) { mutableStateOf(reveil.heure) }
    val contactsAssocies = contactsParReveil[reveil.id].orEmpty()

    val idsAssocies = contactsAssocies.map { it.membreId }
    val contactsAssociesDonnees = contactsParReveil[reveil.id].orEmpty()
    val contactsAssociesIds = contactsAssociesDonnees.map { it.membreId }.toSet()

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (estNouveau) "Créer un réveil" else "Modifier le réveil") },
                navigationIcon = {
                    IconButton(onClick = onRetour) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Détails du réveil", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = nom,
                onValueChange = { nom = it },
                label = { Text("Nom") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            var afficherSelectionHeure by remember { mutableStateOf(false) }
            OutlinedButton(
                onClick = { afficherSelectionHeure = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sélectionner l'heure : $heure")
            }

            if (afficherSelectionHeure) {
                SelecteurHeure(
                    heureInitiale = heure,
                    onDismiss = { afficherSelectionHeure = false },
                    onConfirm = {
                        heure = it
                        afficherSelectionHeure = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (estNouveau) {
                        reveilViewModel.ajouterReveil(nom, heure)
                    } else {
                        reveilViewModel.modifierReveil(reveil.id, nom, heure)

                        //On reprogramme l'alarme du téléphone avec les nouvelles données
                        if (reveil.actif) {
                            programmerAlarmeReveil(context, reveil.copy(nom = nom, heure = heure))
                        }
                    }
                    onRetour()
                },
                enabled = nom.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (estNouveau) "Créer le réveil" else "Enregistrer les changements")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Personnes liées (Amis)", fontWeight = FontWeight.Bold)

            if (estNouveau) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Veuillez d'abord créer et enregistrer ce réveil pour pouvoir y inviter des amis.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            } else {
                val contactsAssociesDetails = contacts.filter { it.id in contactsAssociesIds }
                if (contactsAssociesDetails.isEmpty()) {
                    Text("Aucun contact lié.", color = Color.Gray)
                } else {
                    contactsAssociesDetails.forEach { contact ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(contact.nom, fontWeight = FontWeight.SemiBold)

                                var menuDeroulantOuvert by remember { mutableStateOf(false) }

                                //On cherche le son enregistré dans la BDD pour ce contact précis
                                val donneeRelation = contactsAssociesDonnees.find { it.membreId == contact.id }
                                val sonEnregistre = sons.find { it.id == donneeRelation?.sonId }

                                // remember(sonEnregistre) permet de forcer la mise à jour si la BDD change
                                var sonSelectionne by remember(sonEnregistre) { mutableStateOf(sonEnregistre) }

                                Box {
                                    TextButton(
                                        onClick = { menuDeroulantOuvert = true },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = sonSelectionne?.nom ?: "Attribuer un son...",
                                            fontSize = 12.sp
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = menuDeroulantOuvert,
                                        onDismissRequest = { menuDeroulantOuvert = false }
                                    ) {
                                        if (sons.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("Aucun son disponible") },
                                                onClick = { menuDeroulantOuvert = false }
                                            )
                                        }
                                        sons.forEach { son ->
                                            DropdownMenuItem(
                                                text = { Text(son.nom) },
                                                onClick = {
                                                    sonSelectionne = son
                                                    menuDeroulantOuvert = false
                                                    reveilViewModel.assignerSonContact(reveil.id, contact.id, son.id)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            TextButton(onClick = { reveilViewModel.basculerContact(reveil.id, contact.id, false) }) {
                                Text("Retirer", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                var afficherAjoutContact by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { afficherAjoutContact = true },
                    enabled = contacts.any { it.id !in idsAssocies },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ajouter un contact")
                }

                if (afficherAjoutContact) {
                    SelectionContacts(
                        contacts = contacts.filter { it.id !in idsAssocies },
                        onDismiss = { afficherAjoutContact = false },
                        onConfirm = { ids ->
                            ids.forEach { reveilViewModel.basculerContact(reveil.id, it, true) }
                            afficherAjoutContact = false
                        }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                var afficherSuppression by remember { mutableStateOf(false) }
                TextButton(
                    onClick = { afficherSuppression = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Supprimer ce réveil")
                }

                if (afficherSuppression) {
                    AlertDialog(
                        onDismissRequest = { afficherSuppression = false },
                        title = { Text("Supprimer le réveil ?") },
                        text = { Text("Cette action supprimera ce réveil et arrêtera l'alarme associée.") },
                        confirmButton = {
                            TextButton(onClick = {
                                afficherSuppression = false
                                reveilViewModel.supprimerReveil(reveil.id)
                                onRetour()
                            }) {
                                Text("Supprimer", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { afficherSuppression = false }) {
                                Text("Annuler")
                            }
                        }
                    )
                }
            }
        }
    }
}