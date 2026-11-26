package com.example.smartread

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import com.example.smartread.fokus.ui.ClockFace
import com.example.smartread.fokus.ui.ClockScreen
import com.example.smartread.fokus.ui.FokusTheme
import com.example.smartread.fokus.ui.liveClock

class ClockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        val preferences = getPreferences(MODE_PRIVATE)
        setContent {
            val configuration = LocalConfiguration.current
            val canRotate = !isInMultiWindowMode &&
                (Build.VERSION.SDK_INT < 36 || configuration.smallestScreenWidthDp < 600)
            var face by rememberSaveable {
                mutableStateOf(if (preferences.getBoolean("analog", false)) ClockFace.ANALOG else ClockFace.DIGITAL)
            }
            var landscape by rememberSaveable { mutableStateOf(false) }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
                val orientation = if (landscape && canRotate) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                if (requestedOrientation != orientation) requestedOrientation = orientation
            }
            FokusTheme(dark = true) {
                ClockScreen(
                    face = face, time = liveClock(face == ClockFace.ANALOG), landscape = landscape,
                    onFace = { next ->
                        face = next
                        preferences.edit { putBoolean("analog", next == ClockFace.ANALOG) }
                    },
                    onRotate = if (canRotate) ({ landscape = !landscape }) else null,
                    onClose = ::finish
                )
            }
        }
    }
}
