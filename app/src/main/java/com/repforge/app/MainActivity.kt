package com.repforge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import com.repforge.app.data.ThemeMode
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.repforge.app.data.AppPreferences
import com.repforge.app.presentation.RepForgeApp
import com.repforge.app.presentation.theme.RepForgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = (application as RepForgeApplication).settings
        setContent {
            val prefs by settings.preferences.collectAsStateWithLifecycle(AppPreferences())
            val dark = when (prefs.themeMode) { ThemeMode.System -> isSystemInDarkTheme(); ThemeMode.Light -> false; ThemeMode.Dark -> true }
            // Bar icon colors follow the in-app theme, not the system one.
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT) else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            RepForgeTheme(themeMode = prefs.themeMode, accent = prefs.accent) {
                RepForgeApp(initialSheetId = intent.getLongExtra(EXTRA_SHEET_ID, -1L).takeIf { it > 0 })
            }
        }
    }

    companion object {
        const val EXTRA_SHEET_ID = "com.repforge.app.SHEET_ID"
    }
}
