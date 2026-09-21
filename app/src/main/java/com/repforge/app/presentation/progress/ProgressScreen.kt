package com.repforge.app.presentation.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ProgressScreen(viewModel: ProgressViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val maxVolume = state.days.maxOfOrNull { it.volumeKg }?.coerceAtLeast(1.0) ?: 1.0
    Scaffold(topBar = { TopAppBar(title = { Text("Evolução") }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("Ritmo dos últimos 7 dias", style = MaterialTheme.typography.headlineSmall)
                Text("Acompanhe consistência, não perfeição.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Volume total", style = MaterialTheme.typography.titleMedium)
                        Text("%.1f kg".format(state.summary.weeklyVolumeKg), style = MaterialTheme.typography.headlineMedium)
                        Text("${state.summary.completedSets} séries registradas")
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Volume por dia", style = MaterialTheme.typography.titleMedium)
                        if (state.days.isEmpty()) Text("Registre seu primeiro treino para ver evolução.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        state.days.forEach { day ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(day.day)
                                Text("%.1f kg".format(day.volumeKg))
                            }
                            LinearProgressIndicator({ (day.volumeKg / maxVolume).toFloat() }, Modifier.fillMaxWidth().height(8.dp))
                        }
                    }
                }
            }
        }
    }
}
