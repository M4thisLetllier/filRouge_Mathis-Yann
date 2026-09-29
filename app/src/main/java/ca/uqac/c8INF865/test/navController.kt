package ca.uqac.c8INF865.test

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ca.uqac.c8INF865.test.screens.EcranAccueilReveil
import ca.uqac.c8INF865.test.screens.EcranConnexion

@Composable
fun MonApp() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "accueil"
    ) {
        composable("accueil") {
            EcranConnexion(onVoirDetail = {
                navController.navigate("detail")
            })
        }
    }
}

//EcranConnexion()
//EcranAccueilReveil()
//EcranContact()