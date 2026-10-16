package com.repforge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.repforge.app.presentation.RepForgeApp
import com.repforge.app.presentation.theme.RepForgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { RepForgeTheme { RepForgeApp(initialSheetId = intent.getLongExtra(EXTRA_SHEET_ID, -1L).takeIf { it > 0 }) } }
    }

    companion object {
        const val EXTRA_SHEET_ID = "com.repforge.app.SHEET_ID"
    }
}
