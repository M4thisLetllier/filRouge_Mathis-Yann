package ca.uqac.c8INF865.test

import android.app.ComponentCaller
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.compose.ui.res.painterResource
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ca.uqac.c8INF865.test.ui.theme.TestTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity","On Create")
        enableEdgeToEdge()
        setContent {
            // Appelle ta fonction d'interface ici
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PageDeConnexion()
                }
            }
        }
    }

    override fun onPause() {
        Log.d("MainActivity","onPause")
        super.onPause()
    }
    override fun onStart() {
        Log.d("MainActivity","onStart")
        super.onStart()
    }
    override fun onResume() {
        Log.d("MainActivity","onResume")
        super.onResume()
    }
    override fun onStop() {
        Log.d("MainActivity","onStop")
        super.onStop()
    }
    override fun onDestroy() {
        Log.d("MainActivity","OnDestroy")
        super.onDestroy()
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "Hello $name!",
            modifier = modifier
        )
        Text(
            text = "Welcome to the app!",
        )
        Image(modifier = modifier .fillMaxSize(),
            painter = painterResource(id = R.drawable.uqac_logo),
            contentDescription = "Your Image"
        )
    }
}
@Composable
fun AfficherMonImage(modifier : Modifier = Modifier) {
    Image(
        modifier = modifier .fillMaxSize (),
        painter = painterResource(id = R.drawable.uqac),
        contentDescription = "Description de l'image pour l'accessibilité"
    )
}

@Composable
fun PageDeConnexion() {
    // États locaux pour stocker le texte saisi (nécessaire pour l'UI de Compose)
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Column permet d'aligner les éléments verticalement
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp), // Marges autour de l'écran
        verticalArrangement = Arrangement.Center, // Centre tout au milieu de l'écran
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Titre de la page
        Text(
            text = "Bienvenue",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Champ Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Adresse e-mail") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp)) // Espace entre les champs

        // Champ Mot de passe
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mot de passe") },
            visualTransformation = PasswordVisualTransformation(), // Masque le mot de passe (***)
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Bouton de connexion
        Button(
            onClick = {
                // TODO: Ajouter la logique d'événement plus tard
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp) // Hauteur confortable pour cliquer
        ) {
            Text(text = "Se connecter")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lien "Mot de passe oublié"
        TextButton(
            onClick = {
                // TODO: Redirection plus tard
            }
        ) {
            Text(text = "Mot de passe oublié ?")
        }
    }
}
//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    TestTheme {
//        Greeting("Android")
//    }
//}