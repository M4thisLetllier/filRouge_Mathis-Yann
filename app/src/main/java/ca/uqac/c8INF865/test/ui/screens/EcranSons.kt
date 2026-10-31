package ca.uqac.c8INF865.test.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.uqac.c8INF865.test.data.Son
import ca.uqac.c8INF865.test.viewModel.SonViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EcranSons(
    sonViewModel: SonViewModel = viewModel() // Instanciation autonome
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
        val nouveauRecorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
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
                            // Envoi vers Firebase Storage
                            sonViewModel.uploaderSon(
                                nom = "Enregistrement " + SimpleDateFormat(
                                    "dd/MM/yyyy HH:mm",
                                    Locale.getDefault()
                                ).format(Date()),
                                cheminLocal = fichier.absolutePath
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
private fun ListeSons(sons: List<Son>, onSupprimer: (Son) -> Unit) {
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
private fun CarteSon(son: Son, onSupprimer: (Son) -> Unit) {
    var lecture by remember(son.url) { mutableStateOf(false) }
    var afficherConfirmation by remember { mutableStateOf(false) }

    // Le MediaPlayer lit désormais directement l'URL Firebase
    val lecteur = remember(son.url) {
        MediaPlayer().apply {
            setDataSource(son.url)
            prepareAsync() // Asynchrone car téléchargement réseau
            setOnPreparedListener {
                // Optionnel : Activer le bouton play seulement quand prêt
            }
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