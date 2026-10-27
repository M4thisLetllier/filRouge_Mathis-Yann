package ca.uqac.c8INF865.test.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.uqac.c8INF865.test.viewModel.AuthViewModel

@Composable
fun PageConnexion(
    authViewModel: AuthViewModel = viewModel(),
    onConnexionReussie: () -> Unit // Fonction à appeler pour naviguer vers l'accueil
) {
    var modeInscription by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var motDePasse by remember { mutableStateOf("") }
    var nom by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }

    val chargement by authViewModel.chargement.collectAsState()
    val erreur by authViewModel.erreur.collectAsState()
    val estConnecte by authViewModel.connecte.collectAsState()

    // Redirection automatique si la connexion réussit
    LaunchedEffect(estConnecte) {
        if (estConnecte) {
            onConnexionReussie()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (modeInscription) "Créer un compte" else "Connexion",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (erreur != null) {
            Text(text = erreur!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Adresse email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = motDePasse,
            onValueChange = { motDePasse = it },
            label = { Text("Mot de passe") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (modeInscription) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = nom,
                onValueChange = { nom = it },
                label = { Text("Nom complet") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = telephone,
                onValueChange = { telephone = it },
                label = { Text("Numéro de téléphone") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (modeInscription) {
                    authViewModel.inscription(email, motDePasse, nom, telephone)
                } else {
                    authViewModel.connexion(email, motDePasse)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !chargement
        ) {
            if (chargement) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(if (modeInscription) "S'inscrire" else "Se connecter")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { modeInscription = !modeInscription }) {
            Text(if (modeInscription) "Déjà un compte ? Se connecter" else "Pas de compte ? S'inscrire")
        }
    }
}