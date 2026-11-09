package ca.uqac.c8INF865.test.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.uqac.c8INF865.test.data.Son
import ca.uqac.c8INF865.test.viewModel.SonViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EcranSons(
    sonViewModel: SonViewModel = viewModel()
) {
    val context = LocalContext.current
    val sons by sonViewModel.sons.collectAsState()

    // Gestion du Pager (Swipe)
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    // États d'enregistrement
    var enregistrement by remember { mutableStateOf(false) }
    var fichierTemporaire by remember { mutableStateOf<File?>(null) }
    var permissionAccordee by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
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

    // Instance unique de MediaPlayer pour toute la liste
    val lecteur = remember { MediaPlayer() }
    var sonEnCoursId by remember { mutableStateOf<String?>(null) }
    var estEnTrainDeJouer by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            recorder?.runCatching { stop() }
            recorder?.release()
            lecteur.release()
        }
    }

    LaunchedEffect(enregistrement) {
        if (enregistrement) {
            secondes = 0
            while (enregistrement) {
                delay(1_000)
                secondes++
            }
        }
    }

    fun commencerEnregistrement() {
        if (!permissionAccordee) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        // Arrêter la lecture en cours si un son est en train de jouer
        if (estEnTrainDeJouer) {
            lecteur.stop()
            lecteur.reset()
            sonEnCoursId = null
            estEnTrainDeJouer = false
        }

        val dossier = File(context.filesDir, "sons").apply { mkdirs() }
        val fichier = File(dossier, "son_${System.currentTimeMillis()}.3gp")
        val nouveauRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(fichier.absolutePath)
        }

        runCatching {
            nouveauRecorder.prepare()
            nouveauRecorder.start()
            fichierTemporaire = fichier
            recorder = nouveauRecorder
            enregistrement = true
        }.onFailure { e ->
            Log.e("EcranSons", "Erreur lors du démarrage de l'enregistrement", e)
            nouveauRecorder.release()
        }
    }

    fun arreterEnregistrement() {
        recorder?.runCatching { stop() }
        recorder?.release()
        recorder = null
        enregistrement = false
    }

    fun jouerOuPauser(son: Son) {
        if (sonEnCoursId == son.id) {
            if (lecteur.isPlaying) {
                lecteur.pause()
                estEnTrainDeJouer = false
            } else {
                lecteur.start()
                estEnTrainDeJouer = true
            }
        } else {
            lecteur.reset()
            runCatching {
                lecteur.setDataSource(son.url)
                lecteur.setOnCompletionListener {
                    sonEnCoursId = null
                    estEnTrainDeJouer = false
                }
                lecteur.setOnPreparedListener { mp ->
                    mp.start()
                    sonEnCoursId = son.id
                    estEnTrainDeJouer = true
                }
                lecteur.prepareAsync()
            }.onFailure { e ->
                Log.e("EcranSons", "Erreur lors de la lecture du son", e)
                lecteur.reset()
                sonEnCoursId = null
                estEnTrainDeJouer = false
            }
        }
    }

    fun supprimerSon(son: Son) {
        if (sonEnCoursId == son.id) {
            lecteur.stop()
            lecteur.reset()
            sonEnCoursId = null
            estEnTrainDeJouer = false
        }
        sonViewModel.supprimer(son)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                listOf("Enregistrer", "Mes sons").forEachIndexed { index, titre ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            if (!enregistrement) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        },
                        text = { Text(titre) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !enregistrement
            ) { page ->
                if (page == 0) {
                    Enregistreur(
                        enregistrement = enregistrement,
                        secondes = secondes,
                        fichierPret = fichierTemporaire != null && !enregistrement,
                        onCommencer = ::commencerEnregistrement,
                        onArreter = ::arreterEnregistrement,
                        onRefaire = {
                            fichierTemporaire?.delete()
                            fichierTemporaire = null
                        },
                        onSauvegarder = {
                            fichierTemporaire?.let { fichier ->
                                // Envoi vers Supabase Storage
                                sonViewModel.uploaderSon(
                                    nom = "Enregistrement " + SimpleDateFormat(
                                        "dd/MM/yyyy HH:mm",
                                        Locale.getDefault()
                                    ).format(Date()),
                                    cheminLocal = fichier.absolutePath
                                )
                            }
                            fichierTemporaire = null
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        }
                    )
                } else {
                    ListeSons(
                        sons = sons,
                        sonEnCoursId = sonEnCoursId,
                        estEnTrainDeJouer = estEnTrainDeJouer,
                        onToggleLecture = ::jouerOuPauser,
                        onSupprimer = ::supprimerSon,
                        onRenommer = sonViewModel::renommerSon
                    )
                }
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
private fun ListeSons(
    sons: List<Son>,
    sonEnCoursId: String?,
    estEnTrainDeJouer: Boolean,
    onToggleLecture: (Son) -> Unit,
    onSupprimer: (Son) -> Unit,
    onRenommer: (Son, String) -> Unit
) {
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
            CarteSon(
                son = son,
                estEnCoursDeLecture = son.id == sonEnCoursId && estEnTrainDeJouer,
                onToggleLecture = { onToggleLecture(son) },
                onSupprimer = onSupprimer,
                onRenommer = onRenommer
            )
        }
    }
}

@Composable
private fun CarteSon(
    son: Son,
    estEnCoursDeLecture: Boolean,
    onToggleLecture: () -> Unit,
    onSupprimer: (Son) -> Unit,
    onRenommer: (Son, String) -> Unit
) {
    var afficherConfirmationSuppression by remember { mutableStateOf(false) }
    var afficherBoiteRenommer by remember { mutableStateOf(false) }
    var nouveauNom by remember { mutableStateOf(son.nom) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleLecture) {
                Icon(
                    imageVector = if (estEnCoursDeLecture) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (estEnCoursDeLecture) "Mettre en pause" else "Écouter"
                )
            }

            Text(son.nom, Modifier.weight(1f))

            IconButton(onClick = {
                nouveauNom = son.nom
                afficherBoiteRenommer = true
            }) {
                Icon(Icons.Default.Edit, contentDescription = "Renommer")
            }

            IconButton(onClick = { afficherConfirmationSuppression = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Supprimer")
            }
        }
    }

    if (afficherBoiteRenommer) {
        AlertDialog(
            onDismissRequest = { afficherBoiteRenommer = false },
            title = { Text("Renommer le son") },
            text = {
                OutlinedTextField(
                    value = nouveauNom,
                    onValueChange = { nouveauNom = it },
                    label = { Text("Nom du son") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nouveauNom.isNotBlank() && nouveauNom != son.nom) {
                            onRenommer(son, nouveauNom.trim())
                        }
                        afficherBoiteRenommer = false
                    }
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { afficherBoiteRenommer = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    if (afficherConfirmationSuppression) {
        AlertDialog(
            onDismissRequest = { afficherConfirmationSuppression = false },
            title = { Text("Supprimer la note vocale ?") },
            text = { Text("Voulez-vous vraiment supprimer ${son.nom} ? Cette action est irréversible.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        afficherConfirmationSuppression = false
                        onSupprimer(son)
                    }
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                TextButton(onClick = { afficherConfirmationSuppression = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}