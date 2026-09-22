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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ca.uqac.c8INF865.test.ui.theme.TestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity","On Create")
        enableEdgeToEdge()
        setContent {
            TestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    Greeting(
//                        name = "Yann",
//                        modifier = Modifier.padding(innerPadding)
//                    )
//                    AfficherMonImage()
                    EcranConnexion()
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

//@Composable
//fun Greeting(name: String, modifier: Modifier = Modifier) {
//    Column(modifier = modifier) {
//        Text(
//            text = "Hello $name!",
//            modifier = modifier
//        )
//        Text(
//            text = "Welcome to the app!",
//        )
//        Image(modifier = modifier .fillMaxSize(),
//            painter = painterResource(id = R.drawable.uqac_logo),
//            contentDescription = "Your Image"
//        )
//    }
//}
//@Composable
//fun AfficherMonImage(modifier : Modifier = Modifier) {
//    Image(
//        modifier = modifier .fillMaxSize (),
//        painter = painterResource(id = R.drawable.uqac),
//        contentDescription = "Description de l'image pour l'accessibilité"
//    )
//}

//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    TestTheme {
//        Greeting("Android")
//    }
//}