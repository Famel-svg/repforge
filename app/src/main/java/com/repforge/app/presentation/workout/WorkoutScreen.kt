package com.repforge.app.presentation.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.repforge.app.data.AppPreferences
import com.repforge.app.domain.logging.WEIGHT_STEP_KG
import com.repforge.app.domain.logging.beatsPersonalRecord
import com.repforge.app.domain.logging.entriesOnDay
import com.repforge.app.domain.logging.formatKg
import com.repforge.app.domain.logging.formatSet
import com.repforge.app.domain.logging.formatWeightInput
import com.repforge.app.domain.logging.prefillFor
import com.repforge.app.domain.logging.stepReps
import com.repforge.app.domain.logging.stepWeightKg
import com.repforge.app.domain.metrics.bestEstimatedOneRepMax
import com.repforge.app.domain.metrics.personalRecordKg
import com.repforge.app.domain.model.Exercise
import com.repforge.app.domain.model.RPE_VALUES
import com.repforge.app.domain.model.SET_DROP
import com.repforge.app.domain.model.SET_FAILURE
import com.repforge.app.domain.model.SET_NORMAL
import com.repforge.app.domain.model.SET_TYPES
import com.repforge.app.domain.model.SET_WARMUP
import com.repforge.app.domain.model.SetEntry
import com.repforge.app.domain.validation.parseWeightKg
import com.repforge.app.domain.validation.validateSetInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.time.ZoneId
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
    val entriesByExercise by viewModel.entriesByExercise.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var expandedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var expansionResolved by rememberSaveable { mutableStateOf(false) }
    val restSeconds = preferences.restSeconds
    var remaining by rememberSaveable(restSeconds) { mutableStateOf(restSeconds) }
    var running by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

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

    // Decided once, when every exercise's entries are known, so cards do not collapse under the user mid-workout.
    LaunchedEffect(exercises, entriesByExercise) {
        val byExercise = entriesByExercise ?: return@LaunchedEffect
        if (expansionResolved || exercises.isEmpty() || exercises.any { it.id !in byExercise }) return@LaunchedEffect
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        expandedId = exercises.firstOrNull { entriesOnDay(byExercise[it.id].orEmpty(), now, zone).isEmpty() }?.id
        expansionResolved = true
    }

    fun announce(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message, actionLabel, duration = if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) onAction()
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
            itemsIndexed(exercises, key = { _, item -> item.id }) { index, exercise ->
                val entries = entriesByExercise?.get(exercise.id).orEmpty()
                ExerciseCard(
                    exercise, entries,
                    expanded = expandedId == exercise.id,
                    canMoveUp = index > 0,
                    canMoveDown = index < exercises.lastIndex,
                    onToggle = { expandedId = if (expandedId == exercise.id) null else exercise.id },
                    onMove = { up -> viewModel.moveExercise(exercise.id, up) },
                    onEdit = { name, target -> viewModel.updateExercise(exercise.id, name, target) },
                    onNote = { viewModel.updateNote(exercise.id, it) },
                    onDelete = { viewModel.deleteExercise(exercise.id) },
                    onDeleteEntry = { entry ->
                        viewModel.deleteEntry(entry.id)
                        announce("Série removida", "Desfazer") { viewModel.restoreEntry(entry) }
                    }
                ) { reps, weight, newRecord, setType, rpe ->
                    viewModel.addEntry(exercise.id, 1, reps, weight, setType, rpe)
                    if (preferences.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    // Logging a set starts the rest countdown from the configured default.
                    remaining = restSeconds
                    running = true
                    announce(if (newRecord) "Novo PR: ${formatKg(weight)}" else "Série registrada")
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseCard(
    exercise: Exercise,
    entries: List<SetEntry>,
    expanded: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: () -> Unit,
    onMove: (Boolean) -> Unit,
    onEdit: (String, String) -> Unit,
    onNote: (String) -> Unit,
    onDelete: () -> Unit,
    onDeleteEntry: (SetEntry) -> Unit,
    onLog: (reps: Int, weightKg: Double, newRecord: Boolean, setType: String, rpe: Double?) -> Unit
) {
    // Null means "untouched": the field follows the last logged set instead of freezing the first prefill.
    var repsInput by rememberSaveable { mutableStateOf<String?>(null) }
    var weightInput by rememberSaveable { mutableStateOf<String?>(null) }
    var inputError by remember { mutableStateOf<String?>(null) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }
    var showMore by rememberSaveable { mutableStateOf(false) }
    var setType by rememberSaveable { mutableStateOf(SET_NORMAL) }
    var rpe by rememberSaveable { mutableStateOf<Double?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    val today = remember(entries) { entriesOnDay(entries, System.currentTimeMillis(), ZoneId.systemDefault()) }
    val prefill = prefillFor(entries)
    val repsText = repsInput ?: prefill.reps.toString()
    val weightText = weightInput ?: prefill.weightKg?.let(::formatWeightInput).orEmpty()
    val summary = when {
        today.isNotEmpty() -> if (today.size == 1) "1 série hoje" else "${today.size} séries hoje"
        entries.isNotEmpty() -> "Último: ${formatSet(entries.first())}"
        else -> "Sem registros"
    }
    Card {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).heightIn(min = 56.dp)
                        .clickable(onClickLabel = if (expanded) "Recolher" else "Expandir", role = Role.Button, onClick = onToggle)
                        .semantics { stateDescription = if (expanded) "Expandido" else "Recolhido" }
                        .padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(exercise.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                        Text(exercise.target.ifBlank { "Alvo não definido" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (exercise.note.isNotBlank()) Text(exercise.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!expanded) Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
                }
                Box {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Rounded.MoreVert, "Mais ações de ${exercise.name}") }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Mover para cima") }, enabled = canMoveUp,
                            leadingIcon = { Icon(Icons.Rounded.KeyboardArrowUp, null) },
                            onClick = { showMenu = false; onMove(true) }
                        )
                        DropdownMenuItem(
                            text = { Text("Mover para baixo") }, enabled = canMoveDown,
                            leadingIcon = { Icon(Icons.Rounded.KeyboardArrowDown, null) },
                            onClick = { showMenu = false; onMove(false) }
                        )
                        DropdownMenuItem(
                            text = { Text("Editar exercício") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                            onClick = { showMenu = false; showEdit = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Nota do exercício") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                            onClick = { showMenu = false; showNote = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Remover exercício") },
                            leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                            onClick = { showMenu = false; showDeleteConfirmation = true }
                        )
                    }
                }
            }
            if (expanded) Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (entries.isNotEmpty()) Text(
                    "PR: %.1f kg · 1RM estimado: %.1f kg".format(personalRecordKg(entries), bestEstimatedOneRepMax(entries)),
                    color = MaterialTheme.colorScheme.primary
                )
                if (today.isNotEmpty()) Column {
                    Text("Hoje", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    today.forEachIndexed { index, entry ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}. ${setMarker(entry.setType)}${formatSet(entry)}${entry.rpe?.let { " @" + formatRpe(it) }.orEmpty()}", Modifier.weight(1f))
                            IconButton(onClick = { onDeleteEntry(entry) }) { Icon(Icons.Rounded.Delete, "Remover série ${index + 1}") }
                        }
                    }
                }
                StepperField(
                    value = repsText, label = "Repetições", keyboardType = KeyboardType.Number,
                    minusEnabled = (repsText.trim().toIntOrNull() ?: 0) > 1,
                    minusDescription = "Diminuir repetições", plusDescription = "Aumentar repetições",
                    onValueChange = { repsInput = it; inputError = null },
                    onMinus = { repsInput = stepReps(repsText.trim().toIntOrNull(), -1).toString(); inputError = null },
                    onPlus = { repsInput = stepReps(repsText.trim().toIntOrNull(), 1).toString(); inputError = null }
                )
                StepperField(
                    value = weightText, label = "Carga (kg)", keyboardType = KeyboardType.Decimal,
                    minusEnabled = (parseWeightKg(weightText) ?: 0.0) > 0.0,
                    minusDescription = "Diminuir carga", plusDescription = "Aumentar carga",
                    onValueChange = { weightInput = it; inputError = null },
                    onMinus = { weightInput = formatWeightInput(stepWeightKg(parseWeightKg(weightText), -WEIGHT_STEP_KG)); inputError = null },
                    onPlus = { weightInput = formatWeightInput(stepWeightKg(parseWeightKg(weightText), WEIGHT_STEP_KG)); inputError = null }
                )
                TextButton(onClick = { showMore = !showMore }) { Text(if (showMore) "Menos" else "Mais") }
                if (showMore) Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SET_TYPES.forEach { t -> FilterChip(setType == t, { setType = t }, { Text(setTypeLabel(t)) }) }
                    }
                    Text("RPE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RPE_VALUES.forEach { r -> FilterChip(rpe == r, { rpe = if (rpe == r) null else r }, { Text(formatRpe(r)) }) }
                    }
                }
                Button(
                    onClick = {
                        val parsedReps = repsText.trim().toIntOrNull()
                        // Blank load means bodyweight; only a malformed number is an error.
                        val parsedWeight = if (weightText.isBlank()) 0.0 else parseWeightKg(weightText)
                        inputError = validateSetInput(1, parsedReps, parsedWeight)?.message
                        if (inputError == null) {
                            onLog(parsedReps!!, parsedWeight!!, setType != SET_WARMUP && beatsPersonalRecord(entries, parsedWeight), setType, rpe)
                            setType = SET_NORMAL; rpe = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) { Text("Registrar série") }
                inputError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (entries.isNotEmpty()) {
                    TextButton(onClick = { showHistory = !showHistory }) {
                        Text(if (showHistory) "Ocultar histórico" else "Ver histórico (${entries.size})")
                    }
                    if (showHistory) entries.forEach { entry ->
                        Text(
                            "${DateFormat.getDateInstance(DateFormat.SHORT).format(Date(entry.recordedAt))} · ${formatSet(entry)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
    if (showNote) {
        var note by rememberSaveable { mutableStateOf(exercise.note) }
        AlertDialog(
            onDismissRequest = { showNote = false },
            title = { Text("Nota do exercício") },
            text = { OutlinedTextField(note, { note = it }, label = { Text("Nota") }, minLines = 2) },
            confirmButton = { Button(onClick = { showNote = false; onNote(note) }) { Text("Salvar") } },
            dismissButton = { OutlinedButton(onClick = { showNote = false }) { Text("Cancelar") } }
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

private fun setTypeLabel(t: String) = when (t) { SET_WARMUP -> "Aquecimento"; SET_DROP -> "Drop"; SET_FAILURE -> "Falha"; else -> "Normal" }

private fun setMarker(t: String) = when (t) { SET_WARMUP -> "A "; SET_DROP -> "D "; SET_FAILURE -> "F "; else -> "" }

private fun formatRpe(r: Double) = if (r % 1.0 == 0.0) r.toInt().toString() else "%.1f".format(java.util.Locale.forLanguageTag("pt-BR"), r)

@Composable
private fun StepperField(
    value: String,
    label: String,
    keyboardType: KeyboardType,
    minusEnabled: Boolean,
    minusDescription: String,
    plusDescription: String,
    onValueChange: (String) -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = onMinus, modifier = Modifier.size(48.dp), enabled = minusEnabled) { Icon(Icons.Rounded.Remove, minusDescription) }
        OutlinedTextField(
            value, onValueChange, Modifier.weight(1f), label = { Text(label) }, singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
        )
        FilledTonalIconButton(onClick = onPlus, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.Add, plusDescription) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RestTimerBar(remaining: Int, total: Int, running: Boolean, onToggle: () -> Unit, onReset: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.padding(end = 16.dp)) {
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
