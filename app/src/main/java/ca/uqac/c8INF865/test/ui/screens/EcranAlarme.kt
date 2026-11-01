package ca.uqac.c8INF865.test.ui.screens

import android.content.Intent
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ca.uqac.c8INF865.test.AlarmReceiver

@Composable
fun EcranAlarme(
    reveilId: String,
    onDismissed: () -> Unit // Signal pour dire au MainActivity de fermer l'alarme
) {
    val context = LocalContext.current

    // Bloque le bouton retour physique
    BackHandler(enabled = true) {}

    // Active l'écran allumé / gardé éveillé pendant que l'alarme sonne
    DisposableEffect(Unit) {
        val activity = context as? android.app.Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Alarm,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = "C'est l'heure de votre réveil !",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 32.dp)
        )
        Button(
            onClick = {
                // Envoi du broadcast de désactivation
                context.sendBroadcast(
                    Intent(context, AlarmReceiver::class.java).apply {
                        action = AlarmReceiver.ACTION_DISMISS
                        putExtra("REVEIL_ID", reveilId)
                    }
                )
                // Notifie le MainActivity de couper l'écran d'alarme
                onDismissed()
            },
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text("Désactiver")
        }
    }
}