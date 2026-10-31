package ca.uqac.c8INF865.test.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ca.uqac.c8INF865.test.viewModel.AuthViewModel

@Composable
fun EcranProfil(
    authViewModel: AuthViewModel,
    onDeconnexion: () -> Unit
) {
    val profil by authViewModel.profil.collectAsState()
    val chargement by authViewModel.chargementProfil.collectAsState()
    val sauvegarde by authViewModel.sauvegardeProfil.collectAsState()
    val erreur by authViewModel.erreurProfil.collectAsState()
    var nom by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }
    var messageSucces by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authViewModel.recupererProfil()
    }

    LaunchedEffect(profil) {
        profil?.let {
            nom = it.nom
            telephone = it.telephone
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text("Mon profil", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        if (chargement) {
            CircularProgressIndicator()
        } else {
            OutlinedTextField(
                value = nom,
                onValueChange = {
                    nom = it
                    messageSucces = false
                },
                label = { Text("Nom") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = telephone,
                onValueChange = {
                    telephone = it
                    messageSucces = false
                },
                label = { Text("Numéro de téléphone") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    authViewModel.mettreAJourProfil(nom, telephone) {
                        messageSucces = true
                    }
                },
                enabled = !sauvegarde && nom.isNotBlank() && telephone.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (sauvegarde) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Enregistrer")
                }
            }
        }

        if (messageSucces) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Profil mis à jour.", color = MaterialTheme.colorScheme.primary)
        }
        erreur?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(
            onClick = { authViewModel.deconnexion(onDeconnexion) },
            enabled = !sauvegarde,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Se déconnecter")
        }
    }
}
