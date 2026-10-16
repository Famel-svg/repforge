package com.repforge.app.presentation.workout

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.repforge.app.domain.plates.BAR_OPTIONS_KG
import com.repforge.app.domain.plates.calculatePlates
import com.repforge.app.notify.RestNotifier
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
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.repforge.app.R
import com.repforge.app.data.AppPreferences
import com.repforge.app.domain.validation.AppError
import com.repforge.app.presentation.exerciseDisplayName
import com.repforge.app.presentation.muscleLabel
import com.repforge.app.presentation.res
import com.repforge.app.domain.logging.WEIGHT_STEP_KG
import com.repforge.app.domain.logging.beatsPersonalRecord
import com.repforge.app.domain.logging.entriesOnDay
import com.repforge.app.domain.logging.formatKg
import com.repforge.app.domain.logging.formatSet
import com.repforge.app.domain.logging.formatWeightInput
import com.repforge.app.domain.logging.isGroupEnd
import com.repforge.app.domain.logging.prefillFor
import com.repforge.app.domain.logging.stepReps
import com.repforge.app.domain.logging.stepWeightKg
import com.repforge.app.domain.metrics.bestEstimatedOneRepMax
import com.repforge.app.domain.metrics.personalRecordKg
import com.repforge.app.domain.model.Exercise
import com.repforge.app.domain.model.HistoryEntry
import com.repforge.app.domain.progression.suggestProgression
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

private const val HISTORY_PAGE = 20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    preferences: AppPreferences,
    onSearch: () -> Unit,
    onBack: () -> Unit,
    isPro: Boolean = false,
    onOpenPro: () -> Unit = {}
) {
    val sheetName by viewModel.sheetName.collectAsStateWithLifecycle()
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val entriesByExercise by viewModel.entriesByExercise.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var expandedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var expansionResolved by rememberSaveable { mutableStateOf(false) }
    val restSeconds = preferences.restSeconds
    var remaining by rememberSaveable { mutableStateOf(restSeconds) }
    var appliedRest by rememberSaveable { mutableStateOf(restSeconds) }
    var running by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbarHostState.showSnackbar(context.getString(it)) } }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // A new default only applies to an idle timer, and never on rotation (appliedRest survives it).
    LaunchedEffect(restSeconds) {
        if (restSeconds != appliedRest) {
            appliedRest = restSeconds
            if (!running) remaining = restSeconds
        }
    }

    var askedNotifications by rememberSaveable { mutableStateOf(false) }
    // Denial is fine: the in-app timer keeps working without notifications.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(Unit) { onDispose { RestNotifier.cancel(context) } }

    LaunchedEffect(running) {
        if (running && remaining > 0 && preferences.restNotification) {
            if (Build.VERSION.SDK_INT >= 33 && !askedNotifications &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) { askedNotifications = true; notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
            RestNotifier.start(context, System.currentTimeMillis() + remaining * 1_000L, preferences.hapticsEnabled)
        } else RestNotifier.cancel(context)
        while (running && remaining > 0) {
            delay(1_000)
            remaining -= 1
        }
        if (running && remaining == 0) {
            running = false
            if (preferences.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            // Visible app already buzzed; the alert is for when the user is elsewhere (the alarm covers a dead process).
            if (preferences.restNotification && !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) RestNotifier.done(context, preferences.hapticsEnabled)
            else com.repforge.app.presentation.playRestDone(context)
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
        topBar = { TopAppBar(title = { Text(sheetName) }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) } }, actions = { IconButton(onSearch) { Icon(Icons.Rounded.Search, stringResource(R.string.search_exercise)) } }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Rounded.Add, stringResource(R.string.add_exercise)) }
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
            if (exercises.isEmpty()) item { Text(stringResource(R.string.add_exercises_hint), Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            itemsIndexed(exercises, key = { _, item -> item.id }) { index, exercise ->
                val entries = entriesByExercise?.get(exercise.id).orEmpty()
                val groups = exercises.map { it.supersetGroup }
                val linkedPrev = exercise.supersetGroup != null && groups.getOrNull(index - 1) == exercise.supersetGroup
                val linkedNext = exercise.supersetGroup != null && groups.getOrNull(index + 1) == exercise.supersetGroup
                ExerciseCard(
                    exercise, entries, isPro = isPro, onUpgrade = onOpenPro,
                    expanded = expandedId == exercise.id,
                    canMoveUp = index > 0,
                    canMoveDown = index < exercises.lastIndex,
                    linkedPrev = linkedPrev,
                    linkedNext = linkedNext,
                    onSuperset = { join -> viewModel.setSuperset(exercise.id, join) },
                    onToggle = { expandedId = if (expandedId == exercise.id) null else exercise.id },
                    onMove = { up -> viewModel.moveExercise(exercise.id, up) },
                    onEdit = { name, target -> viewModel.updateExercise(exercise.id, name, target) },
                    onNote = { viewModel.updateNote(exercise.id, it) },
                    onDelete = { viewModel.deleteExercise(exercise.id) },
                    onDeleteEntry = { entry ->
                        viewModel.deleteEntry(entry.id)
                        announce(context.getString(R.string.set_removed), context.getString(R.string.undo)) { viewModel.restoreEntry(entry) }
                    }
                ) { reps, weight, newRecord, setType, rpe ->
                    viewModel.addEntry(exercise.id, 1, reps, weight, setType, rpe)
                    if (preferences.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    // Inside a superset the rest only starts after its last exercise; until then the next card opens.
                    if (isGroupEnd(groups, index)) {
                        remaining = restSeconds
                        running = true
                    } else expandedId = exercises[index + 1].id
                    announce(if (newRecord) context.getString(R.string.new_pr, formatKg(weight)) else context.getString(R.string.set_logged))
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
    isPro: Boolean,
    onUpgrade: () -> Unit,
    expanded: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    linkedPrev: Boolean,
    linkedNext: Boolean,
    onSuperset: (join: Boolean) -> Unit,
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
    var inputError by remember { mutableStateOf<AppError?>(null) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var historyLimit by rememberSaveable { mutableStateOf(HISTORY_PAGE) }
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.SHORT) }
    val suggestion = remember(entries) {
        suggestProgression(entries.map { HistoryEntry(it.exerciseId, exercise.name, "", it.sets, it.reps, it.weightKg, it.recordedAt, it.setType, it.rpe, exercise.target) }, ZoneId.systemDefault())
    }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }
    var showMore by rememberSaveable { mutableStateOf(false) }
    var setType by rememberSaveable { mutableStateOf(SET_NORMAL) }
    var rpe by rememberSaveable { mutableStateOf<Double?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showPlates by rememberSaveable { mutableStateOf(false) }
    val today = remember(entries) { entriesOnDay(entries, System.currentTimeMillis(), ZoneId.systemDefault()) }
    val prefill = prefillFor(entries)
    val repsText = repsInput ?: prefill.reps.toString()
    val weightText = weightInput ?: prefill.weightKg?.let(::formatWeightInput).orEmpty()
    val summary = when {
        today.isNotEmpty() -> pluralStringResource(R.plurals.sets_today, today.size, today.size)
        entries.isNotEmpty() -> stringResource(R.string.last_set, formatSet(entries.first()))
        else -> stringResource(R.string.no_records)
    }
    val group = exercise.supersetGroup
    val displayName = exerciseDisplayName(exercise.name)
    val stateExpanded = stringResource(if (expanded) R.string.state_expanded else R.string.state_collapsed)
    val barColor = MaterialTheme.colorScheme.let { listOf(it.primary, it.tertiary, it.secondary) }[(group ?: 0).mod(3)]
    // The list spaces items by 12dp; the bar is drawn through that gap so linked cards read as one block.
    val gapPx = with(LocalDensity.current) { 12.dp.toPx() }
    Row(
        if (group != null) Modifier.drawBehind {
            drawRect(barColor, Offset(0f, if (linkedPrev) -gapPx else 0f), Size(4.dp.toPx(), size.height + (if (linkedPrev) gapPx else 0f) + (if (linkedNext) gapPx else 0f)))
        }.padding(start = 10.dp) else Modifier
    ) {
    Card(Modifier.weight(1f)) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).heightIn(min = 56.dp)
                        .clickable(onClickLabel = stringResource(if (expanded) R.string.collapse else R.string.expand), role = Role.Button, onClick = onToggle)
                        .semantics { stateDescription = stateExpanded }
                        .padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        if (group != null) Text(stringResource(R.string.superset), style = MaterialTheme.typography.labelMedium, color = barColor)
                        Text(displayName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                        Text(if (exercise.target.isBlank()) stringResource(R.string.target_not_set) else muscleLabel(exercise.target), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (exercise.note.isNotBlank()) Text(exercise.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!expanded) Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
                }
                Box {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.more_actions_for, displayName)) }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.move_up)) }, enabled = canMoveUp,
                            leadingIcon = { Icon(Icons.Rounded.KeyboardArrowUp, null) },
                            onClick = { showMenu = false; onMove(true) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.move_down)) }, enabled = canMoveDown,
                            leadingIcon = { Icon(Icons.Rounded.KeyboardArrowDown, null) },
                            onClick = { showMenu = false; onMove(false) }
                        )
                        if (canMoveDown && !linkedNext) DropdownMenuItem(
                            text = { Text(stringResource(R.string.join_superset)) },
                            leadingIcon = { Icon(Icons.Rounded.Link, null) },
                            onClick = { showMenu = false; onSuperset(true) }
                        )
                        if (group != null) DropdownMenuItem(
                            text = { Text(stringResource(R.string.leave_superset)) },
                            leadingIcon = { Icon(Icons.Rounded.LinkOff, null) },
                            onClick = { showMenu = false; onSuperset(false) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit_exercise)) },
                            leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                            onClick = { showMenu = false; showEdit = true }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.exercise_note)) },
                            leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                            onClick = { showMenu = false; showNote = true }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.remove_exercise)) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                            onClick = { showMenu = false; showDeleteConfirmation = true }
                        )
                    }
                }
            }
            if (expanded) Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (entries.isNotEmpty()) Text(
                    stringResource(R.string.pr_and_1rm, formatKg(personalRecordKg(entries)), formatKg(bestEstimatedOneRepMax(entries))),
                    color = MaterialTheme.colorScheme.primary
                )
                if (today.isNotEmpty()) Column {
                    Text(stringResource(R.string.today), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    today.forEachIndexed { index, entry ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}. ${setMarker(entry.setType)}${formatSet(entry)}${entry.rpe?.let { " @" + formatRpe(it) }.orEmpty()}", Modifier.weight(1f))
                            IconButton(onClick = { onDeleteEntry(entry) }) { Icon(Icons.Rounded.Delete, stringResource(R.string.remove_set_n, index + 1)) }
                        }
                    }
                }
                ProgressionHint(
                    suggestion, isPro, onUpgrade = onUpgrade,
                    onApply = { reps, weightKg -> repsInput = reps.toString(); weightInput = formatWeightInput(weightKg); inputError = null }
                )
                StepperField(
                    value = repsText, label = stringResource(R.string.reps), keyboardType = KeyboardType.Number,
                    minusEnabled = (repsText.trim().toIntOrNull() ?: 0) > 1,
                    minusDescription = stringResource(R.string.decrease_reps), plusDescription = stringResource(R.string.increase_reps),
                    onValueChange = { repsInput = it; inputError = null },
                    onMinus = { repsInput = stepReps(repsText.trim().toIntOrNull(), -1).toString(); inputError = null },
                    onPlus = { repsInput = stepReps(repsText.trim().toIntOrNull(), 1).toString(); inputError = null }
                )
                StepperField(
                    value = weightText, label = stringResource(R.string.load_kg), keyboardType = KeyboardType.Decimal,
                    minusEnabled = (parseWeightKg(weightText) ?: 0.0) > 0.0,
                    minusDescription = stringResource(R.string.decrease_load), plusDescription = stringResource(R.string.increase_load),
                    onValueChange = { weightInput = it; inputError = null },
                    onMinus = { weightInput = formatWeightInput(stepWeightKg(parseWeightKg(weightText), -WEIGHT_STEP_KG)); inputError = null },
                    onPlus = { weightInput = formatWeightInput(stepWeightKg(parseWeightKg(weightText), WEIGHT_STEP_KG)); inputError = null },
                    trailing = { IconButton(onClick = { showPlates = true }) { Icon(Icons.Rounded.FitnessCenter, stringResource(R.string.plate_calculator)) } }
                )
                TextButton(onClick = { showMore = !showMore }) { Text(stringResource(if (showMore) R.string.less else R.string.more)) }
                if (showMore) Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SET_TYPES.forEach { t -> FilterChip(setType == t, { setType = t }, { Text(setTypeLabel(t)) }) }
                    }
                    Text(stringResource(R.string.rpe), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RPE_VALUES.forEach { r -> FilterChip(rpe == r, { rpe = if (rpe == r) null else r }, { Text(formatRpe(r)) }) }
                    }
                }
                Button(
                    onClick = {
                        val parsedReps = repsText.trim().toIntOrNull()
                        // Blank load means bodyweight; only a malformed number is an error.
                        val parsedWeight = if (weightText.isBlank()) 0.0 else parseWeightKg(weightText)
                        inputError = validateSetInput(1, parsedReps, parsedWeight)
                        if (inputError == null) {
                            onLog(parsedReps!!, parsedWeight!!, setType != SET_WARMUP && beatsPersonalRecord(entries, parsedWeight), setType, rpe)
                            setType = SET_NORMAL; rpe = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) { Text(stringResource(R.string.log_set)) }
                inputError?.let { Text(stringResource(it.res()), color = MaterialTheme.colorScheme.error) }
                if (entries.isNotEmpty()) {
                    TextButton(onClick = { showHistory = !showHistory }) {
                        Text(if (showHistory) stringResource(R.string.hide_history) else stringResource(R.string.view_history, entries.size))
                    }
                    if (showHistory) {
                        entries.take(historyLimit).forEach { entry ->
                            Text(
                                "${dateFormat.format(Date(entry.recordedAt))} · ${formatSet(entry)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (entries.size > historyLimit) TextButton(onClick = { historyLimit += HISTORY_PAGE }) { Text(stringResource(R.string.see_more)) }
                    }
                }
            }
        }
    }
    }
    if (showPlates) PlatesDialog(parseWeightKg(weightText) ?: 0.0, isPro, onUpgrade = { showPlates = false; onUpgrade() }, onDismiss = { showPlates = false })
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.remove_exercise_title)) },
            text = { Text(stringResource(R.string.remove_exercise_body)) },
            confirmButton = {
                Button(onClick = { showDeleteConfirmation = false; onDelete() }) { Text(stringResource(R.string.remove)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmation = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
    if (showNote) {
        var note by rememberSaveable { mutableStateOf(exercise.note) }
        AlertDialog(
            onDismissRequest = { showNote = false },
            title = { Text(stringResource(R.string.exercise_note)) },
            text = { OutlinedTextField(note, { note = it }, label = { Text(stringResource(R.string.note)) }, minLines = 2) },
            confirmButton = { Button(onClick = { showNote = false; onNote(note) }) { Text(stringResource(R.string.save)) } },
            dismissButton = { OutlinedButton(onClick = { showNote = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
    if (showEdit) {
        ExerciseDialog(
            initialName = exercise.name,
            initialTarget = exercise.target,
            title = stringResource(R.string.edit_exercise),
            actionLabel = stringResource(R.string.save),
            onDismiss = { showEdit = false },
            onConfirm = { name, target -> showEdit = false; onEdit(name, target) }
        )
    }
}

@Composable
private fun setTypeLabel(t: String) = stringResource(when (t) { SET_WARMUP -> R.string.set_warmup; SET_DROP -> R.string.set_drop; SET_FAILURE -> R.string.set_failure; else -> R.string.set_normal })

@Composable
private fun setMarker(t: String) = when (t) { SET_WARMUP -> stringResource(R.string.marker_warmup) + " "; SET_DROP -> stringResource(R.string.marker_drop) + " "; SET_FAILURE -> stringResource(R.string.marker_failure) + " "; else -> "" }

private fun formatRpe(r: Double) = if (r % 1.0 == 0.0) r.toInt().toString() else "%.1f".format(java.util.Locale.getDefault(), r)

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
    onPlus: () -> Unit,
    trailing: @Composable () -> Unit = {}
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = onMinus, modifier = Modifier.size(48.dp), enabled = minusEnabled) { Icon(Icons.Rounded.Remove, minusDescription) }
        OutlinedTextField(
            value, onValueChange, Modifier.weight(1f), label = { Text(label) }, singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
        )
        FilledTonalIconButton(onClick = onPlus, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.Add, plusDescription) }
        trailing()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlatesDialog(targetKg: Double, isPro: Boolean, onUpgrade: () -> Unit, onDismiss: () -> Unit) {
    var bar by rememberSaveable { mutableStateOf(BAR_OPTIONS_KG.first()) }
    val result = remember(targetKg, bar) { calculatePlates(targetKg, bar) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.plate_calculator)) },
        text = {
            if (!isPro) Text(stringResource(R.string.plate_pro_body))
            else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.plate_bar), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BAR_OPTIONS_KG.forEach { b -> FilterChip(bar == b, { bar = b }, { Text(formatKg(b)) }) }
                }
                Text(stringResource(R.string.plate_per_side), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (result.perSideKg.isEmpty()) Text(stringResource(R.string.plate_empty_bar))
                else Text(result.perSideKg.joinToString(" + ") { formatWeightInput(it) } + " kg", style = MaterialTheme.typography.titleMedium)
                if (!result.exact) Text(stringResource(R.string.plate_not_exact, formatKg(result.loadKg)), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            if (isPro) Button(onClick = onDismiss) { Text(stringResource(R.string.close)) }
            else Button(onClick = onUpgrade) { Text(stringResource(R.string.learn_pro)) }
        },
        dismissButton = { if (!isPro) OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RestTimerBar(remaining: Int, total: Int, running: Boolean, onToggle: () -> Unit, onReset: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.padding(end = 16.dp)) {
                    Text(stringResource(R.string.rest), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("%02d:%02d".format(remaining / 60, remaining % 60), style = MaterialTheme.typography.headlineSmall)
                }
                Button(onClick = onToggle) { Text(stringResource(if (running) R.string.pause else R.string.start)) }
                OutlinedButton(onClick = onReset) { Text(stringResource(R.string.reset)) }
            }
            LinearProgressIndicator(progress = { remaining / total.coerceAtLeast(1).toFloat() }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ExerciseDialog(
    initialName: String = "",
    initialTarget: String = "",
    title: String = stringResource(R.string.add_exercise),
    actionLabel: String = stringResource(R.string.add),
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var target by rememberSaveable { mutableStateOf(initialTarget) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.name)) }, singleLine = true)
            OutlinedTextField(target, { target = it }, label = { Text(stringResource(R.string.muscle_group)) }, singleLine = true)
        } },
        confirmButton = { Button(enabled = name.isNotBlank(), onClick = { onConfirm(name, target) }) { Text(actionLabel) } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
