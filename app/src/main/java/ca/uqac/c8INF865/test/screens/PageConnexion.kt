package ca.uqac.c8INF865.test.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ca.uqac.c8INF865.test.R
import com.google.firebase.auth.FirebaseAuth

@Composable
fun EcranConnexion(onVoirDetail: () -> Unit) {
    var courriel by rememberSaveable { mutableStateOf("") }
    var mdp by rememberSaveable { mutableStateOf("") }
    val scrollState = rememberScrollState()

    // 1. Récupérer l'instance de Firebase Auth et le contexte pour les messages
    val auth = FirebaseAuth.getInstance()
    val contexte = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            painter = painterResource(id = R.drawable.uqac_logo),
            contentDescription = "Logo de l'UQAC"
        )
        Row(horizontalArrangement = Arrangement.Center) {
            OutlinedTextField(
                value = courriel,
                onValueChange = { nouveauTexte -> courriel = nouveauTexte },
                label = { Text("Adresse courriel") }, // Firebase utilise des courriels
                modifier = Modifier.weight(1f).fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
        }
        Spacer(modifier = Modifier.padding(8.dp))
        Row(horizontalArrangement = Arrangement.Center) {
            OutlinedTextField(
                value = mdp,
                onValueChange = { nouveauMdp -> mdp = nouveauMdp },
                label = { Text("Mot de passe") },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
        }
        Spacer(modifier = Modifier.padding(8.dp))

        // 2. Bouton Connexion
        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth().padding(25.dp),
            onClick = {
                if (courriel.isNotEmpty() && mdp.isNotEmpty()) {
                    auth.signInWithEmailAndPassword(courriel, mdp)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(contexte, "Connexion réussie !", Toast.LENGTH_SHORT).show()
                                onVoirDetail() // Redirige vers l'accueil
                            } else {
                                Toast.makeText(contexte, "Erreur : ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Toast.makeText(contexte, "Remplissez tous les champs", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            Text("Connexion")
        }
        Spacer(modifier = Modifier.padding(8.dp))

        // 3. Bouton Nouveau compte
        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = {
                if (courriel.isNotEmpty() && mdp.isNotEmpty()) {
                    auth.createUserWithEmailAndPassword(courriel, mdp)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(contexte, "Compte créé !", Toast.LENGTH_SHORT).show()
                                onVoirDetail() // Redirige vers l'accueil
                            } else {
                                Toast.makeText(contexte, "Erreur : ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Toast.makeText(contexte, "Remplissez tous les champs", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            Text("Nouveau compte")
        }
        Spacer(modifier = Modifier.padding(8.dp))
        TextButton(onClick = { /* TODO: Réinitialisation du mot de passe */ }) {
            Text(text = "Mot de passe oublié ?")
        }
    }
}