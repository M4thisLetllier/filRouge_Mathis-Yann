package ca.uqac.c8INF865.test

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ca.uqac.c8INF865.test.ui.screens.EcranAlarme
import ca.uqac.c8INF865.test.ui.theme.TestTheme

class MainActivity : ComponentActivity() {
    private var alarmReveilId by mutableStateOf<String?>(null)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            openFullScreenIntentSettingsIfNeeded()
        }

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

        requestAlarmPermissions()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAlarmIntent(intent)
    }

    private fun handleAlarmIntent(intent: Intent) {
        when (intent.action) {
            ACTION_SHOW_ALARM -> {
                alarmReveilId = intent.getStringExtra("REVEIL_ID") ?: "default_id"
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            }
            ACTION_DISMISS_ALARM -> {
                alarmReveilId = null
            }
        }
    }

    private fun requestAlarmPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openFullScreenIntentSettingsIfNeeded()
        }
    }

    private fun openFullScreenIntentSettingsIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val notificationManager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            !notificationManager.canUseFullScreenIntent()
        ) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }

    override fun onPause() { super.onPause(); Log.d("MainActivity", "onPause") }
    override fun onStart() { super.onStart(); Log.d("MainActivity", "onStart") }
    override fun onResume() { super.onResume(); Log.d("MainActivity", "onResume") }
    override fun onStop() { super.onStop(); Log.d("MainActivity", "onStop") }
    override fun onDestroy() { super.onDestroy(); Log.d("MainActivity", "OnDestroy") }

    companion object {
        const val ACTION_SHOW_ALARM = "ca.uqac.c8INF865.test.ACTION_SHOW_ALARM"
        const val ACTION_DISMISS_ALARM = "ca.uqac.c8INF865.test.ACTION_DISMISS_ALARM"
    }
}