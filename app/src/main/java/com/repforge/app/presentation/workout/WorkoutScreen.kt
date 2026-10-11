package com.repforge.app.presentation.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.repforge.app.data.AppPreferences
import com.repforge.app.domain.metrics.bestEstimatedOneRepMax
import com.repforge.app.domain.metrics.personalRecordKg
import com.repforge.app.domain.model.Exercise
import com.repforge.app.domain.model.SetEntry
import com.repforge.app.domain.validation.parseWeightKg
import com.repforge.app.domain.validation.validateSetInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    preferences: AppPreferences,
    onSearch: () -> Unit,
    onBack: () -> Unit
) {
    val sheetName by viewModel.sheetName.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    val restSeconds = preferences.restSeconds
    var remaining by rememberSaveable(restSeconds) { mutableStateOf(restSeconds) }
    var running by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(running) {
        while (running && remaining > 0) {
            delay(1_000)
            remaining -= 1
        }
        if (running && remaining == 0) {
            running = false
            if (preferences.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(sheetName) }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Voltar") } }, actions = { IconButton(onSearch) { Icon(Icons.Rounded.Search, "Buscar exercício") } }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Rounded.Add, "Adicionar exercício") }
        },
        bottomBar = {
            RestTimerBar(
                remaining = remaining, total = restSeconds, running = running,
                onToggle = { if (remaining == 0) remaining = restSeconds; running = !running },
                onReset = { running = false; remaining = restSeconds }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (exercises.isEmpty()) item { Text("Adicione exercícios para iniciar seu treino.", Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(exercises, key = { it.id }) { exercise ->
                val entries by remember(exercise.id) { viewModel.entries(exercise.id) }.collectAsStateWithLifecycle(emptyList())
                ExerciseCard(exercise, entries, onEdit = { name, target -> viewModel.updateExercise(exercise.id, name, target) }, onDelete = { viewModel.deleteExercise(exercise.id) }) { sets, reps, weight ->
                    viewModel.addEntry(exercise.id, sets, reps, weight)
                    if (preferences.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    // Logging a set starts the rest countdown from the configured default.
                    remaining = restSeconds
                    running = true
                    scope.launch { snackbarHostState.showSnackbar("Série registrada") }
                }
            }
            // Keeps the last card clear of the floating action button.
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
    if (showAdd) ExerciseDialog(
        onDismiss = { showAdd = false },
        onConfirm = { name, target -> viewModel.addExercise(name, target); showAdd = false }
    )
}

@Composable
private fun ExerciseCard(
    exercise: Exercise,
    entries: List<SetEntry>,
    onEdit: (String, String) -> Unit,
    onDelete: () -> Unit,
    onAddEntry: (Int, Int, Double) -> Unit
) {
    var sets by rememberSaveable { mutableStateOf("3") }
    var reps by rememberSaveable { mutableStateOf("10") }
    var weight by rememberSaveable { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    val numberKeyboard = KeyboardOptions(keyboardType = KeyboardType.Number)
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(exercise.name, style = MaterialTheme.typography.titleMedium)
            Text(exercise.target.ifBlank { "Alvo não definido" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (entries.isNotEmpty()) {
                Text(
                    "PR: %.1f kg · 1RM estimado: %.1f kg".format(personalRecordKg(entries), bestEstimatedOneRepMax(entries)),
                    color = MaterialTheme.colorScheme.primary
                )
                (if (showHistory) entries else entries.take(3)).forEach { entry ->
                    Text(
                        "${DateFormat.getDateInstance(DateFormat.SHORT).format(Date(entry.recordedAt))} · ${entry.sets} × ${entry.reps} · %.1f kg".format(entry.weightKg),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (entries.size > 3) TextButton(onClick = { showHistory = !showHistory }) {
                    Text(if (showHistory) "Ocultar histórico" else "Ver histórico (${entries.size})")
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(sets, { sets = it }, Modifier.fillMaxWidth(), label = { Text("Séries") }, singleLine = true, keyboardOptions = numberKeyboard)
                OutlinedTextField(reps, { reps = it }, Modifier.fillMaxWidth(), label = { Text("Repetições") }, singleLine = true, keyboardOptions = numberKeyboard)
                OutlinedTextField(
                    weight, { weight = it }, Modifier.fillMaxWidth(), label = { Text("Carga (kg)") }, singleLine = true,
                    placeholder = { entries.firstOrNull()?.let { Text("%.1f".format(it.weightKg)) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
            Button(
                onClick = {
                    val parsedSets = sets.trim().toIntOrNull()
                    val parsedReps = reps.trim().toIntOrNull()
                    // An empty load repeats the previous one, which is the common case between sets.
                    val parsedWeight = if (weight.isBlank()) entries.firstOrNull()?.weightKg else parseWeightKg(weight)
                    inputError = validateSetInput(parsedSets, parsedReps, parsedWeight)?.message
                    if (inputError == null) onAddEntry(parsedSets!!, parsedReps!!, parsedWeight!!)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Registrar série") }
            inputError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            OutlinedButton(onClick = { showEdit = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Editar exercício")
            }
            OutlinedButton(onClick = { showDeleteConfirmation = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Remover exercício")
            }
        }
    }
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Remover exercício?") },
            text = { Text("O histórico deste exercício também será removido.") },
            confirmButton = {
                Button(onClick = { showDeleteConfirmation = false; onDelete() }) { Text("Remover") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmation = false }) { Text("Cancelar") }
            }
        )
    }
    if (showEdit) {
        ExerciseDialog(
            initialName = exercise.name,
            initialTarget = exercise.target,
            title = "Editar exercício",
            actionLabel = "Salvar",
            onDismiss = { showEdit = false },
            onConfirm = { name, target -> showEdit = false; onEdit(name, target) }
        )
    }
}

@Composable
private fun RestTimerBar(remaining: Int, total: Int, running: Boolean, onToggle: () -> Unit, onReset: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Descanso", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("%02d:%02d".format(remaining / 60, remaining % 60), style = MaterialTheme.typography.headlineSmall)
                }
                Button(onClick = onToggle) { Text(if (running) "Pausar" else "Iniciar") }
                OutlinedButton(onClick = onReset) { Text("Resetar") }
            }
            LinearProgressIndicator(progress = { remaining / total.coerceAtLeast(1).toFloat() }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ExerciseDialog(
    initialName: String = "",
    initialTarget: String = "",
    title: String = "Adicionar exercício",
    actionLabel: String = "Adicionar",
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var target by rememberSaveable { mutableStateOf(initialTarget) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true)
            OutlinedTextField(target, { target = it }, label = { Text("Grupo muscular") }, singleLine = true)
        } },
        confirmButton = { Button(enabled = name.isNotBlank(), onClick = { onConfirm(name, target) }) { Text(actionLabel) } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
