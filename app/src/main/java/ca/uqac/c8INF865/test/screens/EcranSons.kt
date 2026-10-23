package ca.uqac.c8INF865.test.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.uqac.c8INF865.test.data.AppDatabase
import ca.uqac.c8INF865.test.data.SonEntity
import ca.uqac.c8INF865.test.viewModel.AppViewModelFactory
import ca.uqac.c8INF865.test.viewModel.SonViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EcranSons(
    sonViewModel: SonViewModel = viewModel(
        factory = AppViewModelFactory(AppDatabase.getInstance(LocalContext.current))
    )
) {
    val context = LocalContext.current
    val sons by sonViewModel.sons.collectAsState()
    var onglet by remember { mutableIntStateOf(0) }
    var enregistrement by remember { mutableStateOf(false) }
    var fichierTemporaire by remember { mutableStateOf<File?>(null) }
    var permissionAccordee by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionAccordee = it }
    var secondes by remember { mutableIntStateOf(0) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }

    LaunchedEffect(enregistrement) {
        if (enregistrement) {
            secondes = 0
            while (enregistrement) {
                kotlinx.coroutines.delay(1_000)
                secondes++
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recorder?.runCatching { stop() }
            recorder?.release()
        }
    }

    fun commencer() {
        if (!permissionAccordee) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        val dossier = File(context.filesDir, "sons").apply { mkdirs() }
        val fichier = File(dossier, "son_${System.currentTimeMillis()}.3gp")
        val nouveauRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(fichier.absolutePath)
            prepare()
            start()
        }
        fichierTemporaire = fichier
        recorder = nouveauRecorder
        enregistrement = true
    }

    fun arreter() {
        recorder?.runCatching { stop() }
        recorder?.release()
        recorder = null
        enregistrement = false
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = onglet) {
                listOf("Enregistrer", "Mes sons").forEachIndexed { index, titre ->
                    Tab(
                        selected = onglet == index,
                        onClick = { if (!enregistrement) onglet = index },
                        text = { Text(titre) }
                    )
                }
            }
            if (onglet == 0) {
                Enregistreur(
                    enregistrement = enregistrement,
                    secondes = secondes,
                    fichierPret = fichierTemporaire != null && !enregistrement,
                    onCommencer = ::commencer,
                    onArreter = ::arreter,
                    onRefaire = {
                        fichierTemporaire?.delete()
                        fichierTemporaire = null
                    },
                    onSauvegarder = {
                        fichierTemporaire?.let { fichier ->
                            sonViewModel.ajouter(
                                nom = "Enregistrement " + SimpleDateFormat(
                                    "dd/MM/yyyy HH:mm",
                                    Locale.getDefault()
                                ).format(Date()),
                                chemin = fichier.absolutePath
                            )
                        }
                        fichierTemporaire = null
                        onglet = 1
                    }
                )
            } else {
                ListeSons(sons, sonViewModel::supprimer)
            }
        }
    }
}

@Composable
private fun Enregistreur(
    enregistrement: Boolean,
    secondes: Int,
    fichierPret: Boolean,
    onCommencer: () -> Unit,
    onArreter: () -> Unit,
    onRefaire: () -> Unit,
    onSauvegarder: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            if (enregistrement) "Enregistrement en cours" else if (fichierPret) "Votre vocal est prêt" else "Enregistrer un vocal",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        if (enregistrement) {
            Text("%02d:%02d".format(Locale.getDefault(), secondes / 60, secondes % 60))
        }
        Spacer(Modifier.height(28.dp))
        IconButton(
            onClick = if (enregistrement) onArreter else onCommencer,
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = if (enregistrement) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (enregistrement) "Arrêter l'enregistrement" else "Commencer l'enregistrement",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        if (fichierPret) {
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onRefaire) {
                    Text("Refaire")
                }
                Button(onClick = onSauvegarder) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Enregistrer")
                }
            }
        }
    }
}

@Composable
private fun ListeSons(sons: List<SonEntity>, onSupprimer: (SonEntity) -> Unit) {
    if (sons.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Aucun enregistrement", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(sons, key = { it.id }) { son ->
            CarteSon(son, onSupprimer)
        }
    }
}

@Composable
private fun CarteSon(son: SonEntity, onSupprimer: (SonEntity) -> Unit) {
    var lecture by remember(son.chemin) { mutableStateOf(false) }
    var afficherConfirmation by remember { mutableStateOf(false) }
    val lecteur = remember(son.chemin) {
        MediaPlayer().apply {
            setDataSource(son.chemin)
            prepare()
            setOnCompletionListener { lecture = false }
        }
    }
    DisposableEffect(lecteur) {
        onDispose { lecteur.release() }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (lecture) lecteur.pause() else lecteur.start()
                lecture = !lecture
            }) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = if (lecture) "Mettre en pause" else "Écouter"
                )
            }
            Text(son.nom, Modifier.weight(1f))
            IconButton(onClick = { afficherConfirmation = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Supprimer")
            }
        }
    }

    if (afficherConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { afficherConfirmation = false },
            title = { Text("Supprimer la note vocale ?") },
            text = {
                Text("Voulez-vous vraiment supprimer ${son.nom} ? Cette action est irréversible.")
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        if (lecture) {
                            lecteur.stop()
                            lecture = false
                        }
                        afficherConfirmation = false
                        onSupprimer(son)
                        File(son.chemin).delete()
                    }
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { afficherConfirmation = false }
                ) {
                    Text("Annuler")
                }
            }
        )
    }
}
