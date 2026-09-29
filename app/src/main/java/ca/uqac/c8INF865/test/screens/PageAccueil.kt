package ca.uqac.c8INF865.test.screens

import androidx.compose.runtime.Composable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import ca.uqac.c8INF865.test.viewModel.ReveilViewModel



// Écran Principal
@Composable
fun EcranAccueilReveil() {
    val state by viewModel.uiState.collectAsState()

    // Scaffold permet de placer facilement la NavBar et le bouton flottant (FAB)
    Scaffold(
        bottomBar = { BarreDeNavigationBas() },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Action pour ajouter un réveil */ },
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
            items(reveils   ) { reveil ->
                reveil ->
                CarteReveil(
                    reveil = reveil,
                    // On fait remonter l'événement de clic vers le ViewModel
                    onActifChange = { nouvelEtat ->
                        viewModel.basculerEtatReveil(reveil.id, nouvelEtat)
                    }
                )
            }
        }
    }
}

//Composant graphique pour un réveil
@Composable
fun CarteReveil(reveil: ModeleReveil) {
    // État local pour le bouton switch
    reveil: ModeleReveil ,
    onActifChange: (Boolean) -> Unit

    // Couleur plus sombre si le réveil est désactivé
    val couleurFond = if (isActif) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant

    Card(
        shape = RoundedCornerShape(16.dp), // Bords arrondis
        colors = CardDefaults.cardColors(containerColor = couleurFond),
        border = BorderStroke(1.dp, Color.LightGray), // Contour rectangulaire léger
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
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
                    if (reveil.nbAmis == 0) {
                        Text(text = "Personnel", fontSize = 12.sp, color = Color.Gray)
                    } else {
                        //on affiche seulement 3 amis, au dela on ajoute (...)
                        val maxAffiches = 3
                        val nbAffiches = if (reveil.nbAmis > maxAffiches) maxAffiches else reveil.nbAmis

                        // Dessine les ronds pour les amis
                        for (i in 0 until nbAffiches) {
                            CercleAvatar()
                        }
                        // Si dépasse la capacité (3), affiche le bouton "..."
                        if (reveil.nbAmis > maxAffiches) {
                            CercleAvatar(texte = "...")
                        }
                    }
                }

                // Bouton Activer/Désactiver
                Switch(
                    checked = reveil.actifParDefaut,
                    onCheckedChange = { nouvelEtat -> onActifChange(nouvelEtat) }
                )
            }
        }
    }
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

// 5. Barre de navigation en bas
@Composable
fun BarreDeNavigationBas() {
    NavigationBar {
        // Icône 1 : Réveil (Actif par défaut pour cette page)
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Notifications, contentDescription = "Réveils") },
            label = { Text("Réveils") },
            selected = true,
            onClick = { /* Action statique */ }
        )
        // Icône 2 : Social
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Group, contentDescription = "Social") },
            label = { Text("Social") },
            selected = false,
            onClick = { /* Action statique */ }
        )
        // Icône 3 : Microphone (Sons)
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Mic, contentDescription = "Sons") },
            label = { Text("Sons") },
            selected = false,
            onClick = { /* Action statique */ }
        )
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