package ca.uqac.c8INF865.test

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

import ca.uqac.c8INF865.test.ui.screens.EcranAccueilReveil
import ca.uqac.c8INF865.test.ui.screens.EcranContact
import ca.uqac.c8INF865.test.ui.screens.EcranSons
import ca.uqac.c8INF865.test.ui.screens.PageConnexion
import ca.uqac.c8INF865.test.ui.screens.EcranModificationReveil
import ca.uqac.c8INF865.test.ui.screens.EcranProfil
import ca.uqac.c8INF865.test.data.Reveil
import ca.uqac.c8INF865.test.viewModel.AuthViewModel


import ca.uqac.c8INF865.test.network.Supabase
import io.github.jan.supabase.gotrue.auth

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MonApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    // 1. Vérifier si un utilisateur est déjà connecté via Supabase
    val idUtilisateurCourant = Supabase.client.auth.currentUserOrNull()?.id
    val destinationDepart = if (idUtilisateurCourant != null) "accueil" else "connexion"

    val items = listOf(
        BottomBarItem("accueil", "Réveils", Icons.Filled.Notifications),
        BottomBarItem("social", "Social", Icons.Filled.Group),
        BottomBarItem("sons", "Sons", Icons.Filled.Mic)
    )

    // 2. Observer la route actuelle pour masquer la barre sur l'écran de connexion
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val routeActuelle = currentDestination?.route?.substringBefore("?") // Ignore les paramètres de route
    val afficherBarreNavigation = routeActuelle in listOf("accueil", "social", "sons")

    Scaffold(
        topBar = {
            if (afficherBarreNavigation) {
                TopAppBar(
                    title = {
                        Text(if (currentDestination?.route == "profil") "Profil" else "")
                    },
                    navigationIcon = {
                        if (currentDestination?.route == "profil") {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                            }
                        }
                    },
                    actions = {
                        if (currentDestination?.route != "profil") {
                            IconButton(onClick = { navController.navigate("profil") }) {
                                Icon(Icons.Filled.AccountCircle, contentDescription = "Mon profil")
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            // 3. Condition pour afficher ou non la barre
            if (afficherBarreNavigation) {
                NavigationBar {
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route?.substringBefore("?") == item.route
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
            startDestination = destinationDepart,
            modifier = Modifier.padding(
                top = 64.dp, // Adjust this value to your liking
                bottom = innerPadding.calculateBottomPadding()
            )
        ) {
            // 5. Ajouter la route pour la connexion
            composable("connexion") {
                PageConnexion(
                    authViewModel = authViewModel,
                    onConnexionReussie = {
                        navController.navigate("accueil") {
                            popUpTo("connexion") { inclusive = true }
                        }
                    }
                )
            }

            composable("accueil") {
                EcranAccueilReveil(
                    onModifierReveil = { reveil ->
                        val nomEncode = Uri.encode(reveil.nom)
                        navController.navigate(
                            "modifier_reveil?id=${reveil.id}&createurId=${reveil.createur_id}&nom=$nomEncode&heure=${reveil.heure}&actif=${reveil.actif}&sonId=${reveil.son_id ?: ""}"
                        )
                    }
                )
            }

            composable(
                route = "modifier_reveil?id={id}&createurId={createurId}&nom={nom}&heure={heure}&actif={actif}&sonId={sonId}",
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType; defaultValue = "" },
                    navArgument("createurId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("nom") { type = NavType.StringType; defaultValue = "" },
                    navArgument("heure") { type = NavType.StringType; defaultValue = "07:30" },
                    navArgument("actif") { type = NavType.BoolType; defaultValue = true },
                    navArgument("sonId") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: ""
                val createurId = backStackEntry.arguments?.getString("createurId") ?: ""
                val nom = Uri.decode(backStackEntry.arguments?.getString("nom") ?: "")
                val heure = backStackEntry.arguments?.getString("heure") ?: "07:30"
                val actif = backStackEntry.arguments?.getBoolean("actif") ?: true
                val sonId = backStackEntry.arguments?.getString("sonId")?.takeIf { it.isNotEmpty() }

                val reveilSelectionne = Reveil(
                    id = id,
                    createur_id = createurId,
                    nom = nom,
                    heure = heure,
                    actif = actif,
                    son_id = sonId
                )

                EcranModificationReveil(
                    reveil = reveilSelectionne,
                    onRetour = { navController.popBackStack() }
                )
            }

            composable("social") {
                EcranContact()
            }

            composable("sons") {
                EcranSons()
            }
            composable("profil") {
                EcranProfil(
                    authViewModel = authViewModel,
                    onDeconnexion = {
                        navController.navigate("connexion") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

private data class BottomBarItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)