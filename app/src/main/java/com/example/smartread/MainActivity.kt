package com.example.smartread

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import com.example.smartread.fokus.Appearance
import com.example.smartread.fokus.FokusController
import com.example.smartread.fokus.ui.DeviceAccess
import com.example.smartread.fokus.ui.FokusApp
import com.example.smartread.fokus.ui.FokusButton
import com.example.smartread.fokus.ui.FokusTheme

class MainActivity : ComponentActivity() {
    private val controller by lazy { FokusController.get(this) }
    private var access by mutableStateOf(DeviceAccess(false, false, false))
    private var settingsRequest by mutableIntStateOf(0)
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshAccess()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            val state by controller.state.collectAsState()
            var inAppAod by remember { mutableStateOf(false) }
            var aodDimmed by remember { mutableStateOf(false) }
            val dark = when (state?.preferences?.appearance) {
                Appearance.DARK -> true
                Appearance.LIGHT -> false
                else -> isSystemInDarkTheme()
            }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark && !inAppAod
                    isAppearanceLightNavigationBars = !dark && !inAppAod
                    if (inAppAod) {
                        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        hide(WindowInsetsCompat.Type.systemBars())
                    } else {
                        show(WindowInsetsCompat.Type.systemBars())
                    }
                }
                val attrs = window.attributes
                val desiredBrightness = if (inAppAod && aodDimmed) 0.14f
                else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                if (attrs.screenBrightness != desiredBrightness) {
                    attrs.screenBrightness = desiredBrightness
                    window.attributes = attrs
                }
                if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
            }
            FokusTheme(dark) {
                val current = state
                if (current == null) {
                    val loadFailed by controller.initialLoadFailed.collectAsState()
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Box(contentAlignment = Alignment.Center) {
                            if (loadFailed) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Fokus couldn’t open its local data.")
                                    FokusButton(onClick = { controller.sync() }) { Text("Try again") }
                                }
                            } else CircularProgressIndicator()
                        }
                    }
                } else {
                    FokusApp(
                        current, controller, access, settingsRequest,
                        keepScreenOn = { keep ->
                            val flag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                            if (keep) window.addFlags(flag) else window.clearFlags(flag)
                        },
                        onAodState = { active, dim ->
                            inAppAod = active
                            aodDimmed = dim
                        },
                        openClock = {
                            startActivity(Intent(this, ClockActivity::class.java))
                        },
                        openNotifications = ::openNotifications,
                        openQuietAccess = { openSettings(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) },
                        openExactAlarms = {
                            if (Build.VERSION.SDK_INT >= 31) openSettings(Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:$packageName".toUri()
                            ))
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshAccess()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == NotificationManager.ACTION_AUTOMATIC_ZEN_RULE) settingsRequest++
    }

    private fun refreshAccess() {
        val notifications = getSystemService(NotificationManager::class.java)
        access = DeviceAccess(
            notifications.areNotificationsEnabled(),
            notifications.isNotificationPolicyAccessGranted,
            Build.VERSION.SDK_INT < 31 || getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        )
        controller.sync()
    }

    private fun openNotifications() {
        val prefs = getPreferences(MODE_PRIVATE)
        if (Build.VERSION.SDK_INT >= 33 && !access.notifications &&
            (!prefs.getBoolean("askedForNotifications", false) ||
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))
        ) {
            prefs.edit { putBoolean("askedForNotifications", true) }
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            openSettings(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        }
    }

    private fun openSettings(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "Open Android Settings to manage this permission.", Toast.LENGTH_LONG).show()
        }
    }
}
