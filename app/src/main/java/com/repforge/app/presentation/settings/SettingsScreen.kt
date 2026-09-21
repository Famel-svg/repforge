package com.repforge.app.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text("Ajustes") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Treino", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
            Card {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Descanso padrão", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    Text("${state.restSeconds} segundos entre séries")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(60, 90, 120, 180).forEach { seconds ->
                            OutlinedButton(onClick = { viewModel.setRestSeconds(seconds) }) { Text("${seconds}s") }
                        }
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("Feedback háptico", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                            Text("Vibrar ao registrar uma série", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = state.hapticsEnabled, onCheckedChange = viewModel::setHapticsEnabled)
                    }
                }
            }
            Text("RepForge 0.1.0", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Seus treinos ficam no aparelho e funcionam sem conta.", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
