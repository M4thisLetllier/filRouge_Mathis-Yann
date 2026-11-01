package ca.uqac.c8INF865.test

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ca.uqac.c8INF865.test.ui.screens.EcranAlarme
import ca.uqac.c8INF865.test.ui.theme.TestTheme

class MainActivity : ComponentActivity() {
    private var alarmReveilId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "On Create")
        enableEdgeToEdge()
        handleAlarmIntent(intent)

        setContent {
            TestTheme {
                val currentAlarmId = alarmReveilId
                if (currentAlarmId != null) {
                    EcranAlarme(
                        reveilId = currentAlarmId,
                        onDismissed = { alarmReveilId = null }
                    )
                } else {
                    MonApp()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAlarmIntent(intent)
    }

    private fun handleAlarmIntent(intent: Intent) {
        if (intent.action == ACTION_SHOW_ALARM) {
            alarmReveilId = intent.getStringExtra("REVEIL_ID") ?: "default_id"
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
    }

    override fun onPause() { super.onPause(); Log.d("MainActivity", "onPause") }
    override fun onStart() { super.onStart(); Log.d("MainActivity", "onStart") }
    override fun onResume() { super.onResume(); Log.d("MainActivity", "onResume") }
    override fun onStop() { super.onStop(); Log.d("MainActivity", "onStop") }
    override fun onDestroy() { super.onDestroy(); Log.d("MainActivity", "OnDestroy") }

    companion object {
        const val ACTION_SHOW_ALARM = "ca.uqac.c8INF865.test.ACTION_SHOW_ALARM"
    }
}