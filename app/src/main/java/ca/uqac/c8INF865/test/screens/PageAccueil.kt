package ca.uqac.c8INF865.test.screens

import androidx.compose.runtime.Composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import ca.uqac.c8INF865.test.data.AppDatabase
import ca.uqac.c8INF865.test.viewModel.AppViewModelFactory
import ca.uqac.c8INF865.test.viewModel.ContactViewModel
import ca.uqac.c8INF865.test.viewModel.ReveilViewModel
import androidx.compose.ui.platform.LocalContext

// Définition des données
data class ModeleReveil(
    val id: Int,
    val nom: String,
    val heure: String,
    val dateProchaine: String,
    val nbAmis: Int,
    val actifParDefaut: Boolean
)

val listeDeReveilsStatistiques = listOf(
    ModeleReveil(1, "Réveil 1", "07:00", "Mercredi 30 Sept.", 2, true),
    ModeleReveil(2, "Réveil 2", "08:30", "Mercredi 30 Sept.", 5, true),
    ModeleReveil(3, "Réveil 3", "09:00", "Jeudi 1 Oct.", 0, false),
    ModeleReveil(4, "Réveil 4", "10:15", "Vendredi 2 Oct.", 3, true),
    ModeleReveil(5, "Réveil 5", "14:00", "Lundi 5 Oct.", 1, false)
)

// Écran Principal
@Composable
fun EcranAccueilReveil(
    reveilViewModel: ReveilViewModel = viewModel(
        factory = AppViewModelFactory(AppDatabase.getInstance(LocalContext.current))
    )
) {
    val reveils by reveilViewModel.uiState.collectAsState()
    val contactsParReveil by reveilViewModel.contactsParReveil.collectAsState()
    val contactViewModel: ContactViewModel = viewModel(
        factory = AppViewModelFactory(AppDatabase.getInstance(LocalContext.current))
    )
    val contacts by contactViewModel.contacts.collectAsState()
    var afficherAjout by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { afficherAjout = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter un réveil")
            }
        }
    ) { paddingValues ->
        // LazyColumn crée la liste défilante verticalement
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues) // Respecte l'espace de la bottom bar
                .padding(horizontal = 16.dp), // Marge sur les côtés
            verticalArrangement = Arrangement.spacedBy(16.dp), // Espace entre chaque carte
            contentPadding = PaddingValues(vertical = 24.dp) // Espace en haut et en bas de la liste
        ) {
            items(reveils.listeReveils) { reveil ->
                CarteReveil(
                    reveil = reveil,
                    contacts = contacts,
                    contactsAssocies = contactsParReveil[reveil.id].orEmpty(),
                    onEtatChange = { reveilViewModel.basculerEtatReveil(reveil.id, it) },
                    onModifier = { nom, heure -> reveilViewModel.modifierReveil(reveil.id, nom, heure) },
                    onSupprimer = { reveilViewModel.supprimerReveil(reveil.id) },
                    onContactChange = { contactId, associe ->
                        reveilViewModel.basculerContact(reveil.id, contactId, associe)
                    }
                )
            }
        }
    }

    if (afficherAjout) {
        FormulaireReveil(
            onDismiss = { afficherAjout = false },
            onConfirm = { nom, heure ->
                reveilViewModel.ajouterReveil(nom, heure)
                afficherAjout = false
            }
        )
    }
}

//Composant graphique pour un réveil
@Composable
fun CarteReveil(
    reveil: ModeleReveil,
    contacts: List<Contact> = emptyList(),
    contactsAssocies: Set<Int> = emptySet(),
    onEtatChange: (Boolean) -> Unit = {},
    onModifier: (String, String) -> Unit = { _, _ -> },
    onSupprimer: () -> Unit = {},
    onContactChange: (Int, Boolean) -> Unit = { _, _ -> }
) {
    val isActif = reveil.actifParDefaut
    val nombreAmis = contactsAssocies.size
    var estOuvert by remember { mutableStateOf(false) }
    var afficherSuppression by remember { mutableStateOf(false) }
    var nom by remember(reveil.nom) { mutableStateOf(reveil.nom) }
    var heure by remember(reveil.heure) { mutableStateOf(reveil.heure) }

    // Couleur plus sombre si le réveil est désactivé
    val couleurFond = if (isActif) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant

    Card(
        shape = RoundedCornerShape(16.dp), // Bords arrondis
        colors = CardDefaults.cardColors(containerColor = couleurFond),
        border = BorderStroke(1.dp, Color.LightGray), // Contour rectangulaire léger
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { estOuvert = !estOuvert }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Ligne 1 : Nom du réveil
            Text(
                text = reveil.nom,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActif) MaterialTheme.colorScheme.onSurface else Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Ligne 2 : Heure (gauche) et Date (droite)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = reveil.heure,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isActif) MaterialTheme.colorScheme.primary else Color.Gray
                )
                Text(
                    text = reveil.dateProchaine,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ligne 3 : Avatars des amis (gauche) et Switch (droite)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Section des amis partagés
                Row(
                    // Espacement négatif permet de chevaucher les icônes
                    horizontalArrangement = Arrangement.spacedBy((-8).dp)
                ) {
                    if (nombreAmis == 0) {
                        Text(text = "Personnel", fontSize = 12.sp, color = Color.Gray)
                    } else {
                        //on affiche seulement 3 amis, au dela on ajoute (...)
                        val maxAffiches = 3
                        val nbAffiches = if (nombreAmis > maxAffiches) maxAffiches else nombreAmis

                        // Dessine les ronds pour les amis
                        for (i in 0 until nbAffiches) {
                            CercleAvatar()
                        }
                        // Si dépasse la capacité (3), affiche le bouton "..."
                        if (nombreAmis > maxAffiches) {
                            CercleAvatar(texte = "...")
                        }
                    }
                }

                // Bouton Activer/Désactiver
                Switch(
                    checked = isActif,
                    onCheckedChange = onEtatChange
                )
            }

            if (estOuvert) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
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
                )
                {
                    Text("Modifier l'heure : $heure")
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
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onModifier(nom, heure) },
                    enabled = nom.isNotBlank()
                ) {
                    Text("Enregistrer les changements")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Personnes liées", fontWeight = FontWeight.Bold)
                val contactsAssociesDetails = contacts.filter { it.id in contactsAssocies }
                if (contactsAssociesDetails.isEmpty()) {
                    Text("Aucun contact lié.", color = Color.Gray)
                } else {
                    contactsAssociesDetails.forEach { contact ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(contact.name, modifier = Modifier.weight(1f))
                            TextButton(onClick = { onContactChange(contact.id, false) }) {
                                Text("Retirer")
                            }
                        }
                    }
                }
                var afficherAjoutContact by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { afficherAjoutContact = true },
                    enabled = contacts.any { it.id !in contactsAssocies },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ajouter un contact")
                }
                if (afficherAjoutContact) {
                    SelectionContacts(
                        contacts = contacts.filter { it.id !in contactsAssocies },
                        onDismiss = { afficherAjoutContact = false },
                        onConfirm = { ids ->
                            ids.forEach { onContactChange(it, true) }
                            afficherAjoutContact = false
                        }
                    )
                }
                TextButton(
                    onClick = { afficherSuppression = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Supprimer ce réveil")
                }
            } else {
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "Afficher les détails",
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }

    if (afficherSuppression) {
        AlertDialog(
            onDismissRequest = { afficherSuppression = false },
            title = { Text("Supprimer le réveil ?") },
            text = { Text("Cette action supprimera aussi les personnes liées à ce réveil.") },
            confirmButton = {
                TextButton(onClick = {
                    afficherSuppression = false
                    onSupprimer()
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

@Composable
private fun FormulaireReveil(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var nom by remember { mutableStateOf("") }
    var heure by remember { mutableStateOf("07:30") }
    var afficherSelectionHeure by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un réveil") },
        text = {
            Column {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text("Nom du réveil") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { afficherSelectionHeure = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Choisir l'heure : $heure")
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(nom, heure) },
                enabled = nom.isNotBlank()
            ) {
                Text("Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelecteurHeure(
    heureInitiale: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val morceaux = heureInitiale.split(":")
    val heure = morceaux.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 7
    val minute = morceaux.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 30
    val etat = rememberTimePickerState(
        initialHour = heure,
        initialMinute = minute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choisir une heure") },
        text = { TimePicker(state = etat) },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        String.format(
                            Locale.getDefault(),
                            "%02d:%02d",
                            etat.hour,
                            etat.minute
                        )
                    )
                }
            ) {
                Text("Valider")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun SelectionContacts(
    contacts: List<Contact>,
    onDismiss: () -> Unit,
    onConfirm: (Set<Int>) -> Unit
) {
    var selection by remember { mutableStateOf(emptySet<Int>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter des contacts") },
        text = {
            Column {
                if (contacts.isEmpty()) {
                    Text("Tous les contacts sont déjà liés à ce réveil.")
                } else {
                    contacts.forEach { contact ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = contact.id in selection,
                                onCheckedChange = { coche ->
                                    selection = if (coche) {
                                        selection + contact.id
                                    } else {
                                        selection - contact.id
                                    }
                                }
                            )
                            Text(contact.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selection) },
                enabled = selection.isNotEmpty()
            ) {
                Text("Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

// Petit composant pour dessiner les "bulles" d'amis
@Composable
fun CercleAvatar(texte: String? = null) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.LightGray)
            .border(2.dp, Color.White, CircleShape), // Bordure blanche pour séparer les cercles chevauchés
        contentAlignment = Alignment.Center
    ) {
        if (texte != null) {
            Text(text = texte, fontWeight = FontWeight.Bold, color = Color.DarkGray)
        } else {
            Icon(Icons.Filled.Person, contentDescription = "Ami", tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

// Permet de voir le résultat directement dans Android Studio sans lancer l'app
@Preview(showBackground = true)
@Composable
fun PreviewEcranAccueil() {
    MaterialTheme {
        EcranAccueilReveil()
    }
}