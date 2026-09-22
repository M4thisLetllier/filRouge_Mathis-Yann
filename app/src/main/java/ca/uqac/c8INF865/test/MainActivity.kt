package ca.uqac.c8INF865.test

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ca.uqac.c8INF865.test.screens.EcranConnexion
import ca.uqac.c8INF865.test.ui.theme.TestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity","On Create")
        enableEdgeToEdge()
        setContent {
            TestTheme {
                EcranConnexion()
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