package ca.uqac.c8INF865.test

import android.app.ComponentCaller
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ca.uqac.c8INF865.test.ui.theme.TestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity","On Create")
        enableEdgeToEdge()
        setContent {
            TestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Yann",
                        modifier = Modifier.padding(innerPadding)
                    )
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
            modifier = modifier
        )
    }
}

//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    TestTheme {
//        Greeting("Android")
//    }
//}