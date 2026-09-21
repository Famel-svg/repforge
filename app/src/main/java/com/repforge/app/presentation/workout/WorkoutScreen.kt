package com.repforge.app.presentation.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.repforge.app.domain.model.Exercise
import com.repforge.app.domain.model.SetEntry
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    sheetId: Long,
    sheetName: String,
    viewModel: WorkoutViewModel,
    restSeconds: Int = 90,
    onBack: () -> Unit
) {
    val exercises by viewModel.exercises(sheetId).collectAsStateWithLifecycle(emptyList())
    var showAdd by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { TopAppBar(title = { Text(sheetName) }, navigationIcon = { IconButton(onBack) { Icon(Icons.Rounded.ArrowBack, "Voltar") } }) },
        floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Rounded.Add, "Adicionar exercício") } }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (exercises.isEmpty()) item { Text("Adicione exercícios para iniciar seu treino.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(exercises, key = { it.id }) { exercise ->
                val entries by viewModel.entries(exercise.id).collectAsStateWithLifecycle(emptyList())
                ExerciseCard(exercise, entries) { sets, reps, weight ->
                    viewModel.addEntry(exercise.id, sets, reps, weight)
                }
            }
            item { RestTimerCard(restSeconds) }
        }
    }
    if (showAdd) AddExerciseDialog(
        onDismiss = { showAdd = false },
        onAdd = { name, target -> viewModel.addExercise(sheetId, name, target); showAdd = false }
    )
}

@Composable
private fun ExerciseCard(exercise: Exercise, entries: List<SetEntry>, onAddEntry: (Int, Int, Double) -> Unit) {
    var sets by remember { mutableStateOf("3") }
    var reps by remember { mutableStateOf("10") }
    var weight by remember { mutableStateOf("0") }
    var invalidInput by remember { mutableStateOf(false) }
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(exercise.name, style = MaterialTheme.typography.titleMedium)
            Text(exercise.target.ifBlank { "Alvo não definido" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            entries.take(3).forEach { entry ->
                Text("${entry.sets} séries × ${entry.reps} reps · ${entry.weightKg} kg", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(sets, { sets = it }, Modifier.weight(1f), label = { Text("Séries") }, singleLine = true)
                OutlinedTextField(reps, { reps = it }, Modifier.weight(1f), label = { Text("Reps") }, singleLine = true)
                OutlinedTextField(weight, { weight = it }, Modifier.weight(1f), label = { Text("Kg") }, singleLine = true)
            }
            Button(
                onClick = {
                    val parsedSets = sets.toIntOrNull()
                    val parsedReps = reps.toIntOrNull()
                    val parsedWeight = weight.toDoubleOrNull()
                    val validInput = parsedSets != null && parsedReps != null && parsedWeight != null && parsedSets > 0 && parsedReps > 0 && parsedWeight >= 0
                    invalidInput = !validInput
                    if (validInput) {
                        onAddEntry(parsedSets!!, parsedReps!!, parsedWeight!!)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Registrar série") }
            if (invalidInput) Text("Use séries/reps maiores que zero e carga não negativa.", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun RestTimerCard(defaultSeconds: Int) {
    var remaining by remember(defaultSeconds) { mutableStateOf(defaultSeconds) }
    var running by remember { mutableStateOf(false) }
    LaunchedEffect(running, defaultSeconds) {
        while (running && remaining > 0) {
            delay(1_000)
            remaining -= 1
        }
        if (remaining == 0) running = false
    }
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Timer de descanso", style = MaterialTheme.typography.titleMedium)
            Text("%02d:%02d".format(remaining / 60, remaining % 60), style = MaterialTheme.typography.headlineMedium)
            LinearProgressIndicator(progress = { remaining / defaultSeconds.toFloat() }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { running = !running }) { Text(if (running) "Pausar" else "Iniciar") }
                Button(onClick = { running = false; remaining = defaultSeconds }) { Text("Resetar") }
            }
        }
    }
}

@Composable
private fun AddExerciseDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar exercício") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true)
            OutlinedTextField(target, { target = it }, label = { Text("Grupo muscular") }, singleLine = true)
        } },
        confirmButton = { Button(enabled = name.isNotBlank(), onClick = { onAdd(name, target) }) { Text("Adicionar") } },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancelar") } }
    )
}
