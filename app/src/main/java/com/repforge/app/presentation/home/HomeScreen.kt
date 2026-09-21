package com.repforge.app.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.repforge.app.domain.model.TrainingSheet

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeScreen(
    state: HomeUiState,
    onCreateSheet: (String) -> Unit,
    onStartSheet: (TrainingSheet) -> Unit,
    onDeleteSheet: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreate by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<TrainingSheet?>(null) }
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("RepForge") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Rounded.Add, contentDescription = "Criar ficha")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Text("Bom treino.", style = MaterialTheme.typography.headlineMedium)
                Text("Seu progresso começa no próximo set.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { SummaryCard(state) }
            item {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Suas fichas", style = MaterialTheme.typography.titleLarge)
                    OutlinedButton(onClick = { showCreate = true }, modifier = Modifier.fillMaxWidth()) { Text("Nova ficha") }
                }
            }
            if (state.sheets.isEmpty()) {
                item { EmptySheetsCard(onCreate = { showCreate = true }) }
            } else {
                items(state.sheets, key = { it.id }) { sheet ->
                    SheetCard(sheet, onStart = { onStartSheet(sheet) }, onDelete = { deleteTarget = sheet })
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
    if (showCreate) CreateSheetDialog(
        onDismiss = { showCreate = false },
        onCreate = { name -> onCreateSheet(name); showCreate = false }
    )
    deleteTarget?.let { sheet ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Excluir ficha?") },
            text = { Text("${sheet.name} e todo seu histórico serão removidos deste aparelho.") },
            confirmButton = { Button(onClick = { onDeleteSheet(sheet.id); deleteTarget = null }) { Text("Excluir") } },
            dismissButton = { Button(onClick = { deleteTarget = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun SummaryCard(state: HomeUiState) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Volume últimos 7 dias", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("%.1f kg".format(state.summary.weeklyVolumeKg), style = MaterialTheme.typography.headlineLarge)
                }
                Icon(Icons.Rounded.AutoGraph, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(16.dp))
            Text("${state.summary.completedSets} séries · ${state.summary.activeSheets} fichas ativas")
        }
    }
}

@Composable
private fun SheetCard(sheet: TrainingSheet, onStart: () -> Unit = {}, onDelete: () -> Unit = {}) {
    Card {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(sheet.name, style = MaterialTheme.typography.titleMedium)
                Text("${sheet.exerciseCount} exercícios", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStart) { Icon(Icons.Rounded.PlayArrow, contentDescription = "Iniciar") }
                OutlinedButton(onClick = onDelete) { Text("Excluir") }
            }
        }
    }
}

@Composable
private fun EmptySheetsCard(onCreate: () -> Unit) {
    Card {
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            Icon(Icons.Rounded.FitnessCenter, contentDescription = null)
            Spacer(Modifier.height(12.dp))
            Text("Nenhuma ficha ainda", style = MaterialTheme.typography.titleMedium)
            Text("Monte sua primeira rotina e registre evolução sem depender de internet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onCreate) { Text("Criar primeira ficha") }
        }
    }
}
