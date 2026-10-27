package ca.uqac.c8INF865.test.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.Locale
import ca.uqac.c8INF865.test.AlarmReceiver
import ca.uqac.c8INF865.test.MainActivity
import ca.uqac.c8INF865.test.data.Reveil
import ca.uqac.c8INF865.test.data.Utilisateur
import ca.uqac.c8INF865.test.viewModel.ContactViewModel
import ca.uqac.c8INF865.test.viewModel.ReveilViewModel

// --- Fonctions utilitaires pour l'AlarmManager natif ---

private fun calculerProchaineOccurrence(heureStr: String): Long {
    val parties = heureStr.split(":")
    val heure = parties.getOrNull(0)?.toIntOrNull() ?: 7
    val minute = parties.getOrNull(1)?.toIntOrNull() ?: 0

    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, heure)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= System.currentTimeMillis()) {
            add(Calendar.DAY_OF_YEAR, 1)
        }
    }.timeInMillis
}

private fun alarmPendingIntent(context: Context, reveilId: String): PendingIntent {
    val intent = Intent(context, AlarmReceiver::class.java).apply {
        putExtra("REVEIL_ID", reveilId)
    }
    return PendingIntent.getBroadcast(
        context,
        reveilId.hashCode(),
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

fun programmerAlarmeReveil(context: Context, reveil: Reveil) {
    if (!reveil.actif) return
    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
    val triggerAtMillis = calculerProchaineOccurrence(reveil.heure)

    val showIntent = PendingIntent.getActivity(
        context,
        reveil.id.hashCode(),
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE
    )

    try {
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
            alarmPendingIntent(context, reveil.id)
        )
    } catch (e: SecurityException) {
        // Gérer l'absence de permission SCHEDULE_EXACT_ALARM
    }
}

fun annulerAlarmeReveil(context: Context, reveilId: String) {
    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
    alarmManager.cancel(alarmPendingIntent(context, reveilId))
}

// --- Écran Principal ---

@Composable
fun EcranAccueilReveil(
    reveilViewModel: ReveilViewModel = viewModel(),
    contactViewModel: ContactViewModel = viewModel()
) {
    val context = LocalContext.current
    val reveils by reveilViewModel.uiState.collectAsState()
    val contactsParReveil by reveilViewModel.contactsParReveil.collectAsState()
    val contacts by contactViewModel.contacts.collectAsState()
    var afficherAjout by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (Build.VERSION.SDK_INT >= 33) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    afficherAjout = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter un réveil")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
            items(reveils.listeReveils) { reveil ->
                CarteReveil(
                    reveil = reveil,
                    contacts = contacts,
                    contactsAssocies = contactsParReveil[reveil.id].orEmpty(),
                    onEtatChange = { actif ->
                        reveilViewModel.basculerEtatReveil(reveil.id, actif)
                        if (actif) {
                            programmerAlarmeReveil(context, reveil.copy(actif = true))
                        } else {
                            annulerAlarmeReveil(context, reveil.id)
                        }
                    },
                    onModifier = { nom, heure ->
                        reveilViewModel.modifierReveil(reveil.id, nom, heure)
                        if (reveil.actif) {
                            programmerAlarmeReveil(context, reveil.copy(nom = nom, heure = heure))
                        }
                    },
                    onSupprimer = {
                        annulerAlarmeReveil(context, reveil.id)
                        reveilViewModel.supprimerReveil(reveil.id)
                    },
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

// --- Composant graphique pour un réveil ---

@Composable
fun CarteReveil(
    reveil: Reveil,
    contacts: List<Utilisateur> = emptyList(), // Utilisation de Utilisateur
    contactsAssocies: Set<String> = emptySet(),
    onEtatChange: (Boolean) -> Unit = {},
    onModifier: (String, String) -> Unit = { _, _ -> },
    onSupprimer: () -> Unit = {},
    onContactChange: (String, Boolean) -> Unit = { _, _ -> }
) {
    val isActif = reveil.actif
    val nombreAmis = contactsAssocies.size
    var estOuvert by remember { mutableStateOf(false) }
    var afficherSuppression by remember { mutableStateOf(false) }
    var nom by remember(reveil.nom) { mutableStateOf(reveil.nom) }
    var heure by remember(reveil.heure) { mutableStateOf(reveil.heure) }

    val couleurFond = if (isActif) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = couleurFond),
        border = BorderStroke(1.dp, Color.LightGray),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { estOuvert = !estOuvert }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = reveil.nom,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActif) MaterialTheme.colorScheme.onSurface else Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

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
                    text = "À venir", // Adapté puisque dateProchaine a été retiré de Reveil
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((-8).dp)
                ) {
                    if (nombreAmis == 0) {
                        Text(text = "Personnel", fontSize = 12.sp, color = Color.Gray)
                    } else {
                        val maxAffiches = 3
                        val nbAffiches = if (nombreAmis > maxAffiches) maxAffiches else nombreAmis

                        for (i in 0 until nbAffiches) {
                            CercleAvatar()
                        }
                        if (nombreAmis > maxAffiches) {
                            CercleAvatar(texte = "...")
                        }
                    }
                }

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
                ) {
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
                Text("Personnes liées (Amis)", fontWeight = FontWeight.Bold)

                val contactsAssociesDetails = contacts.filter { it.id in contactsAssocies }
                if (contactsAssociesDetails.isEmpty()) {
                    Text("Aucun contact lié.", color = Color.Gray)
                } else {
                    contactsAssociesDetails.forEach { contact ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(contact.nom, modifier = Modifier.weight(1f)) // Adaptation pour Utilisateur.nom
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
            text = { Text("Cette action supprimera ce réveil et arrêtera l'alarme associée.") },
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
    contacts: List<Utilisateur>, // Utilisation de Utilisateur
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    var selection by remember { mutableStateOf(emptySet<String>()) }

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
                            Text(contact.nom) // Adaptation pour Utilisateur.nom
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

@Composable
fun CercleAvatar(texte: String? = null) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.LightGray)
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (texte != null) {
            Text(text = texte, fontWeight = FontWeight.Bold, color = Color.DarkGray)
        } else {
            Icon(Icons.Filled.Person, contentDescription = "Ami", tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}