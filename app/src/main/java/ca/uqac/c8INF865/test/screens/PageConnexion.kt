package ca.uqac.c8INF865.test.screens

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ca.uqac.c8INF865.test.R
import ca.uqac.c8INF865.test.ui.theme.TestTheme

@Composable
fun EcranConnexion() {
    Column(
        modifier = Modifier .fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            painter = painterResource(id = R.drawable.uqac_logo),
            contentDescription = "Logo de l'UQAC"
        )
        Row(
            horizontalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Nom d'utilisateur") },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.padding(8.dp))
        Row(
            horizontalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Mot de passe") },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.padding(8.dp))
        Button(modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth().padding(25.dp), onClick = { /* Handle login click */ })
        {
            Text("Connection")
        }
        Spacer(modifier = Modifier.padding(8.dp))
        Button(modifier = Modifier.align(Alignment.CenterHorizontally), onClick = { /* Handle signup click */ })
        {
            Text("Nouveau compte")
        }
        Spacer(modifier = Modifier.padding(8.dp))
        TextButton(
            onClick = {
                // TODO: Redirection plus tard
            }
        ) {
            Text(text = "Mot de passe oublié ?")
        }
    }
}