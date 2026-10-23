package ca.uqac.c8INF865.test

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import ca.uqac.c8INF865.test.screens.EcranAccueilReveil
import ca.uqac.c8INF865.test.screens.EcranContact
import ca.uqac.c8INF865.test.screens.EcranSons
import ca.uqac.c8INF865.test.screens.EcranConnexion
import com.google.firebase.auth.FirebaseAuth

@Composable
fun MonApp() {
    val navController = rememberNavController()

    // 1. Vérifier si un utilisateur est déjà connecté
    val auth = FirebaseAuth.getInstance()
    val destinationDepart = if (auth.currentUser != null) "accueil" else "connexion"

    val items = listOf(
        BottomBarItem("accueil", "Réveils", Icons.Filled.Notifications),
        BottomBarItem("social", "Social", Icons.Filled.Group),
        BottomBarItem("sons", "Sons", Icons.Filled.Mic)
    )

    // 2. Observer la route actuelle pour masquer la barre sur l'écran de connexion
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val afficherBarreNavigation = currentDestination?.route != "connexion"

    Scaffold(
        bottomBar = {
            // 3. Condition pour afficher ou non la barre
            if (afficherBarreNavigation) {
                NavigationBar {
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == item.route
                            } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = destinationDepart, // 4. Utiliser la variable dynamique
            modifier = Modifier.padding(innerPadding)
        ) {
            // 5. Ajouter la route pour la connexion
            composable("connexion") {
                EcranConnexion(
                    onVoirDetail = {
                        navController.navigate("accueil") {
                            popUpTo("connexion") { inclusive = true }
                        }
                    }
                )
            }
            composable("accueil") {
                EcranAccueilReveil()
            }
            composable("social") {
                EcranContact()
            }
            composable("sons") {
                EcranSons()
            }
        }
    }
}

private data class BottomBarItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)