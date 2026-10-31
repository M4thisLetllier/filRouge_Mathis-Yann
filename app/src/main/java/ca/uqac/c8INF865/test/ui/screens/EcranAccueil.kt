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

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun EcranAccueilReveil(
    onModifierReveil: (Reveil) -> Unit,
    reveilViewModel: ReveilViewModel = viewModel(),
    contactViewModel: ContactViewModel = viewModel()
) {
    val context = LocalContext.current
    val reveils by reveilViewModel.uiState.collectAsState()
    val contactsParReveil by reveilViewModel.contactsParReveil.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    // --- MISE À JOUR AUTOMATIQUE AU RETOUR SUR LA PAGE ---
    LaunchedEffect(Unit) {
        reveilViewModel.chargerReveils()
    }

    // --- GESTION DU PULL-TO-REFRESH (Version Material 2) ---
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                reveilViewModel.chargerReveils()
                delay(800) // Petit délai pour laisser l'animation visible
                isRefreshing = false
            }
        }
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (Build.VERSION.SDK_INT >= 33) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    onModifierReveil(
                        Reveil(id = "", createur_id = "", nom = "", heure = "07:30", actif = true)
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter un réveil")
            }
        }
    ) { paddingValues ->
        // Application du pullRefresh sur la Box contenant la liste
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pullRefresh(pullRefreshState)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 24.dp)
            ) {
                items(reveils.listeReveils) { reveil ->
                    CarteReveil(
                        reveil = reveil,
                        contactsAssocies = contactsParReveil[reveil.id].orEmpty(),
                        onClick = { onModifierReveil(reveil) },
                        onEtatChange = { actif ->
                            reveilViewModel.basculerEtatReveil(reveil.id, actif)
                            if (actif) {
                                programmerAlarmeReveil(context, reveil.copy(actif = true))
                            } else {
                                annulerAlarmeReveil(context, reveil.id)
                            }
                        }
                    )
                }
            }

            // L'indicateur visuel de chargement
            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                contentColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// --- Composant graphique pour un réveil ---
@Composable
fun CarteReveil(
    reveil: Reveil,
    contactsAssocies: Set<String> = emptySet(),
    onClick: () -> Unit,
    onEtatChange: (Boolean) -> Unit = {}
) {
    val isActif = reveil.actif
    val nombreAmis = contactsAssocies.size
    val couleurFond = if (isActif) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = couleurFond),
        border = BorderStroke(1.dp, Color.LightGray),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                    text = "À venir",
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
        }
    }
}

// Composants publics utilisés par les autres écrans
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelecteurHeure(
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
fun SelectionContacts(
    contacts: List<Utilisateur>,
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
                            Text(contact.nom)
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