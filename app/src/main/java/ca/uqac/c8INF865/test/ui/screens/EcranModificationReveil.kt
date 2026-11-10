package ca.uqac.c8INF865.test.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.uqac.c8INF865.test.data.Reveil
import ca.uqac.c8INF865.test.viewModel.ContactViewModel
import ca.uqac.c8INF865.test.viewModel.ReveilViewModel
import ca.uqac.c8INF865.test.viewModel.SonViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranModificationReveil(
    reveil: Reveil,
    onRetour: () -> Unit,
    reveilViewModel: ReveilViewModel = viewModel(),
    contactViewModel: ContactViewModel = viewModel(),
    sonViewModel: SonViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentUserId by reveilViewModel.currentUserId.collectAsState()
    val currentUserIdValue = currentUserId ?: ""

    // États provenant des ViewModels
    val uiState by reveilViewModel.uiState.collectAsState()
    val contactsParReveil by reveilViewModel.contactsParReveil.collectAsState()
    val sonsAssignes by reveilViewModel.sonsAssignes.collectAsState()
    val utilisateursConnus by reveilViewModel.utilisateursConnus.collectAsState()
    val contacts by contactViewModel.contacts.collectAsState()
    val mesSons by sonViewModel.sons.collectAsState()

    val estNouveau = reveil.id.isEmpty()
    val reveilActuel = uiState.listeReveils.find { it.id == reveil.id } ?: reveil

    // Vérifie si l'utilisateur connecté est le créateur
    val estCreateur = estNouveau || reveilActuel.createur_id == currentUserIdValue

    // Couleur dominante : Primaire si créateur, Vert si invité
    val couleurDominante = if (estCreateur) MaterialTheme.colorScheme.primary else Color(0xFF2E7D32)

    var nom by remember(reveilActuel.nom) { mutableStateOf(reveilActuel.nom) }
    var heure by remember(reveilActuel.heure) { mutableStateOf(reveilActuel.heure) }

    val contactsAssociesDonnees = contactsParReveil[reveilActuel.id].orEmpty()
    val idsAssocies = contactsAssociesDonnees.map { it.membreId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (estNouveau) "Créer un réveil" else if (estCreateur) "Modifier le réveil" else "Réveil partagé",
                        color = if (!estCreateur) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onRetour) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = if (!estCreateur) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (!estCreateur) couleurDominante else MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ==========================================
            // SECTION : FORMULAIRE PRINCIPAL
            // ==========================================
            item {
                Text("Détails du réveil", fontWeight = FontWeight.Bold, color = couleurDominante)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text("Nom") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = couleurDominante,
                        focusedLabelColor = couleurDominante
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                var afficherSelectionHeure by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { afficherSelectionHeure = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = couleurDominante),
                    border = BorderStroke(1.dp, couleurDominante)
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
                            reveilViewModel.modifierReveil(reveilActuel.id, nom, heure)
                            if (reveilActuel.actif) {
                                val monSonId = if (estCreateur) {
                                    reveilActuel.son_id
                                } else {
                                    contactsAssociesDonnees.find { it.membreId == currentUserIdValue }?.sonId
                                }
                                val monSon = monSonId?.let { sonsAssignes[it] }

                                programmerAlarmeReveil(
                                    context,
                                    reveilActuel.copy(nom = nom, heure = heure),
                                    monSon?.id,
                                    monSon?.url
                                )
                            }
                        }
                        onRetour()
                    },
                    enabled = nom.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = couleurDominante)
                ) {
                    Text(if (estNouveau) "Créer le réveil" else "Enregistrer les changements")
                }
            }

            if (estNouveau) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Membres", fontWeight = FontWeight.Bold, color = couleurDominante)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Veuillez d'abord créer et enregistrer ce réveil pour pouvoir y inviter des membres.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                // ==========================================
                // SECTION : CRÉATEUR DU RÉVEIL
                // ==========================================
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Créateur du réveil", fontWeight = FontWeight.Bold, color = couleurDominante)
                    Spacer(modifier = Modifier.height(4.dp))

                    val createurUser = utilisateursConnus[reveilActuel.createur_id]
                    val nomCreateur = if (estCreateur) "${createurUser?.nom ?: "Moi"} (Vous)" else (createurUser?.nom ?: "Créateur")

                    val sonCreateur = reveilActuel.son_id?.let { sonsAssignes[it] }
                    val auteurSonCreateur = sonCreateur?.let { utilisateursConnus[it.utilisateur_id]?.nom ?: "Inconnu" }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(nomCreateur, fontWeight = FontWeight.SemiBold)

                            var menuCreateurOuvert by remember { mutableStateOf(false) }

                            if (!estCreateur) {
                                Box {
                                    TextButton(
                                        onClick = { menuCreateurOuvert = true },
                                        contentPadding = PaddingValues(0.dp),
                                        colors = ButtonDefaults.textButtonColors(contentColor = couleurDominante)
                                    ) {
                                        val texteSon = if (sonCreateur != null) {
                                            "Son : ${sonCreateur.nom} (ajouté par $auteurSonCreateur)"
                                        } else {
                                            "Attribuer un son..."
                                        }
                                        Text(text = texteSon, fontSize = 12.sp)
                                    }
                                    DropdownMenu(
                                        expanded = menuCreateurOuvert,
                                        onDismissRequest = { menuCreateurOuvert = false }
                                    ) {
                                        if (mesSons.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("Aucun son disponible") },
                                                onClick = { menuCreateurOuvert = false }
                                            )
                                        } else {
                                            mesSons.forEach { son ->
                                                DropdownMenuItem(
                                                    text = { Text(son.nom) },
                                                    onClick = {
                                                        menuCreateurOuvert = false
                                                        reveilViewModel.assignerSonCreateur(reveilActuel.id, son.id)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                val texteSon = if (sonCreateur != null) {
                                    "Son : ${sonCreateur.nom} (ajouté par $auteurSonCreateur)"
                                } else {
                                    "Aucun son attribué par les membres"
                                }
                                Text(text = texteSon, fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                // ==========================================
                // SECTION : ENTÊTE MEMBRES
                // ==========================================
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Membres", fontWeight = FontWeight.Bold, color = couleurDominante)
                    Spacer(modifier = Modifier.height(4.dp))

                    if (contactsAssociesDonnees.isEmpty()) {
                        Text("Aucun membre lié.", color = Color.Gray)
                    }
                }

                // ==========================================
                // LISTE DYNAMIQUE DES MEMBRES
                // ==========================================
                items(contactsAssociesDonnees, key = { it.membreId }) { donneeRelation ->
                    val membreId = donneeRelation.membreId
                    val utilisateurMembre = utilisateursConnus[membreId] ?: contacts.find { it.id == membreId }
                    val nomMembre = if (membreId == currentUserIdValue) {
                        "${utilisateurMembre?.nom ?: "Moi"} (Vous)"
                    } else {
                        utilisateurMembre?.nom ?: "Membre"
                    }

                    val sonMembre = donneeRelation.sonId?.let { sonsAssignes[it] }
                    val auteurSonMembre = sonMembre?.let { utilisateursConnus[it.utilisateur_id]?.nom ?: "Inconnu" }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(nomMembre, fontWeight = FontWeight.SemiBold)

                            var menuDeroulantOuvert by remember { mutableStateOf(false) }

                            Box {
                                TextButton(
                                    onClick = { menuDeroulantOuvert = true },
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.textButtonColors(contentColor = couleurDominante)
                                ) {
                                    val texteSon = if (sonMembre != null) {
                                        "Son : ${sonMembre.nom} (ajouté par $auteurSonMembre)"
                                    } else {
                                        "Attribuer un son..."
                                    }
                                    Text(text = texteSon, fontSize = 12.sp)
                                }
                                DropdownMenu(
                                    expanded = menuDeroulantOuvert,
                                    onDismissRequest = { menuDeroulantOuvert = false }
                                ) {
                                    if (mesSons.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("Aucun son disponible") },
                                            onClick = { menuDeroulantOuvert = false }
                                        )
                                    } else {
                                        mesSons.forEach { son ->
                                            DropdownMenuItem(
                                                text = { Text(son.nom) },
                                                onClick = {
                                                    menuDeroulantOuvert = false
                                                    reveilViewModel.assignerSonContact(reveilActuel.id, membreId, son.id)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (estCreateur || membreId == currentUserIdValue) {
                            TextButton(onClick = { reveilViewModel.basculerContact(reveilActuel.id, membreId, false) }) {
                                Text(if (membreId == currentUserIdValue) "Quitter" else "Retirer", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                // ==========================================
                // BOUTONS D'ACTION (AJOUTER / SUPPRIMER)
                // ==========================================
                item {
                    var afficherAjoutContact by remember { mutableStateOf(false) }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { afficherAjoutContact = true },
                        enabled = contacts.any { it.id !in idsAssocies && it.id != reveilActuel.createur_id },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = couleurDominante),
                        border = BorderStroke(1.dp, couleurDominante)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ajouter un membre")
                    }

                    if (afficherAjoutContact) {
                        SelectionContacts(
                            contacts = contacts.filter { it.id !in idsAssocies && it.id != reveilActuel.createur_id },
                            onDismiss = { afficherAjoutContact = false },
                            onConfirm = { ids ->
                                ids.forEach { reveilViewModel.basculerContact(reveilActuel.id, it, true) }
                                afficherAjoutContact = false
                            }
                        )
                    }
                }

                if (estCreateur) {
                    item {
                        var afficherSuppression by remember { mutableStateOf(false) }
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            TextButton(
                                onClick = { afficherSuppression = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Supprimer ce réveil")
                            }
                        }

                        if (afficherSuppression) {
                            AlertDialog(
                                onDismissRequest = { afficherSuppression = false },
                                title = { Text("Supprimer le réveil ?") },
                                text = { Text("Cette action supprimera ce réveil et arrêtera l'alarme associée.") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        afficherSuppression = false
                                        reveilViewModel.supprimerReveil(reveilActuel.id)
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
    }
}