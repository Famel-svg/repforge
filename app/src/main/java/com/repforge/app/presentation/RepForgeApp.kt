package com.repforge.app.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.NavigationBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment

private enum class Destination(val label: String, val icon: ImageVector) {
    Home("Treino", Icons.Rounded.FitnessCenter),
    Progress("Evolução", Icons.Rounded.Timeline),
    Settings("Ajustes", Icons.Rounded.Settings)
}

@Composable
fun RepForgeApp() {
    var selected by remember { mutableStateOf(Destination.Home) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = selected == destination,
                        onClick = { selected = destination },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("RepForge · ${selected.label}")
        }
    }
}
