package com.repforge.app.presentation.workout

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.repforge.app.RepForgeApplication
import com.repforge.app.presentation.components.CircleIconButton
import com.repforge.app.presentation.components.SoftPillButton
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.repforge.app.presentation.pluralCount
import androidx.compose.ui.res.stringResource
import com.repforge.app.R
import com.repforge.app.data.AppPreferences
import com.repforge.app.domain.validation.AppError
import com.repforge.app.presentation.exerciseDisplayName
import com.repforge.app.presentation.muscleLabel
import com.repforge.app.presentation.res
import com.repforge.app.domain.logging.WEIGHT_STEP_KG
import com.repforge.app.domain.logging.beatsKindRecord
import com.repforge.app.domain.logging.formatDistanceInput
import com.repforge.app.domain.logging.formatDistanceKm
import com.repforge.app.domain.logging.formatDuration
import com.repforge.app.domain.logging.parseDuration
import com.repforge.app.data.ExerciseCatalog
import com.repforge.app.domain.metrics.bestDistanceKm
import com.repforge.app.domain.metrics.bestDurationSec
import com.repforge.app.domain.model.EXERCISE_KINDS
import com.repforge.app.domain.model.KIND_BODYWEIGHT
import com.repforge.app.domain.model.KIND_CARDIO
import com.repforge.app.domain.model.KIND_REPS
import com.repforge.app.domain.model.KIND_TIMED
import com.repforge.app.domain.validation.validateKindEffort
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
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
import com.repforge.app.domain.progression.PROGRESSION_RULES
import com.repforge.app.domain.progression.RULE_DOUBLE
import com.repforge.app.domain.progression.RULE_LINEAR
import com.repforge.app.domain.progression.RULE_NONE
import com.repforge.app.domain.progression.RULE_TIME
import com.repforge.app.domain.progression.effectiveRule
import com.repforge.app.domain.progression.suggestNext
import com.repforge.app.domain.model.RPE_VALUES
import com.repforge.app.domain.model.SET_DROP
import com.repforge.app.domain.model.SET_FAILURE
import com.repforge.app.domain.model.SET_NORMAL
import com.repforge.app.domain.model.SET_TYPES
import com.repforge.app.domain.model.SET_WARMUP
import com.repforge.app.domain.model.SetEntry
import com.repforge.app.domain.logging.parseLoadKg
import com.repforge.app.domain.validation.validateSetInput
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.time.ZoneId
import java.util.Date

private const val HISTORY_PAGE = 20

@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    preferences: AppPreferences,
    onSearch: () -> Unit,
    onBack: () -> Unit,
    isPro: Boolean = false,
    onOpenPro: () -> Unit = {},
    /** Called with the session id once it is finished; the caller opens the summary. */
    onFinished: (sessionId: Long) -> Unit = {}
) {
    val sheetName by viewModel.sheetName.collectAsStateWithLifecycle()
    val sheetRule by viewModel.sheetRule.collectAsStateWithLifecycle()
    val sheetSession by viewModel.session.collectAsStateWithLifecycle()
    val session = sheetSession?.session
    var showMenu by remember { mutableStateOf(false) }
    var showRule by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val entriesByExercise by viewModel.entriesByExercise.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var expandedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var expansionResolved by rememberSaveable { mutableStateOf(false) }
    var listMode by rememberSaveable { mutableStateOf(false) }
    val restSeconds = preferences.restSeconds
    var remaining by rememberSaveable { mutableStateOf(restSeconds) }
    var restTotal by rememberSaveable { mutableStateOf(restSeconds) }
    var appliedRest by rememberSaveable { mutableStateOf(restSeconds) }
    var restExerciseId by rememberSaveable { mutableStateOf(-1L) }
    var running by rememberSaveable { mutableStateOf(false) }
    // Bumped by "+15s" so the notification/alarm is rescheduled for the new end time.
    var extended by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbarHostState.showSnackbar(context.getString(it)) } }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current
    val reducedMotion = rememberReducedMotion()
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    val pagerState = rememberPagerState { exercises.size }
    // Typed-but-unlogged rows survive rotation; flattened to primitives: [exerciseId, position, reps, kg, sec, km, type, rpe(-1 = none)].
    val draftsByExercise = rememberSaveable(
        saver = listSaver<MutableMap<Long, SnapshotStateMap<Int, RowDraft>>, Any>(
            save = { all -> all.flatMap { (id, rows) -> rows.flatMap { (pos, d) -> listOf(id, pos, d.reps, d.weightKg, d.durationSec, d.distanceKm, d.setType, d.rpe ?: -1.0) } } },
            restore = { flat ->
                mutableMapOf<Long, SnapshotStateMap<Int, RowDraft>>().also { all ->
                    flat.chunked(8).forEach { c ->
                        all.getOrPut(c[0] as Long) { mutableStateMapOf() }[c[1] as Int] =
                            RowDraft(c[2] as Int, c[3] as Double, c[4] as Int, c[5] as Double, c[6] as String, (c[7] as Double).takeIf { it >= 0 })
                    }
                }
            }
        )
    ) { mutableMapOf<Long, SnapshotStateMap<Int, RowDraft>>() }
    val extraRows = rememberSaveable(
        saver = listSaver<SnapshotStateMap<Long, Int>, Long>(
            save = { map -> map.flatMap { (k, v) -> listOf(k, v.toLong()) } },
            restore = { flat -> mutableStateMapOf<Long, Int>().also { m -> flat.chunked(2).forEach { m[it[0]] = it[1].toInt() } } }
        )
    ) { mutableStateMapOf() }
    val equipmentByName by produceState(emptyMap<String, String>()) {
        val catalog = (context.applicationContext as? RepForgeApplication)?.catalog ?: return@produceState
        value = runCatching {
            buildMap { catalog.all().forEach { put(ExerciseCatalog.normalize(it.name), it.equipment); put(ExerciseCatalog.normalize(it.nameEn), it.equipment) } }
        }.getOrDefault(emptyMap())
    }

    // A new default only applies to an idle timer, and never on rotation (appliedRest survives it).
    LaunchedEffect(restSeconds) {
        if (restSeconds != appliedRest) {
            appliedRest = restSeconds
            if (!running) { remaining = restSeconds; restTotal = restSeconds }
        }
    }

    var askedNotifications by rememberSaveable { mutableStateOf(false) }
    // Denial is fine: the in-app timer keeps working without notifications.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(Unit) { onDispose { RestNotifier.cancel(context) } }

    LaunchedEffect(running, extended) {
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
        val firstOpen = exercises.indexOfFirst { entriesOnDay(byExercise[it.id].orEmpty(), now, zone).isEmpty() }
        expandedId = exercises.getOrNull(firstOpen)?.id
        if (firstOpen > 0) pagerState.scrollToPage(firstOpen)
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

    fun startRest() { remaining = restSeconds; restTotal = restSeconds; running = true }

    fun goTo(page: Int) {
        scope.launch { if (reducedMotion) pagerState.scrollToPage(page) else pagerState.animateScrollToPage(page) }
    }

    /** Shared tail of every logged set: haptics, rest (only after a superset's last exercise) and the confirmation snackbar. */
    fun afterLog(index: Int, groups: List<Int?>, record: String?) {
        if (preferences.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (isGroupEnd(groups, index)) { restExerciseId = exercises[index].id; startRest() }
        else { expandedId = exercises[index + 1].id; goTo(index + 1) }
        announce(if (record != null) context.getString(R.string.new_pr, record) else context.getString(R.string.set_logged))
    }

    fun deleteWithUndo(entry: SetEntry) {
        // Unchecking the set that started the running rest cancels that rest.
        if (restExerciseId == entry.exerciseId && entriesByExercise?.get(entry.exerciseId)?.firstOrNull()?.id == entry.id) {
            running = false; remaining = restSeconds; restTotal = restSeconds; restExerciseId = -1L
        }
        viewModel.deleteEntry(entry.id)
        announce(context.getString(R.string.set_removed), context.getString(R.string.undo)) { viewModel.restoreEntry(entry) }
    }

    /** Logs one focus-mode row through the same path as list mode (validation, PR detection, view-model addEntry). */
    fun logDraft(index: Int, exercise: Exercise, entries: List<SetEntry>, d: RowDraft): Boolean {
        val kind = exercise.kind
        val isLoad = kind == KIND_REPS || kind == KIND_BODYWEIGHT
        val reps = if (isLoad) d.reps else 1
        val weight = if (isLoad) d.weightKg else 0.0
        val duration = if (isLoad) null else d.durationSec.takeIf { it > 0 }
        val distance = if (kind == KIND_CARDIO) d.distanceKm.takeIf { it > 0 } else null
        val error = validateSetInput(1, reps, weight) ?: validateKindEffort(kind, duration, distance)
        if (error != null) { announce(context.getString(error.res())); return false }
        val beat = d.setType != SET_WARMUP && beatsKindRecord(kind, entries, weight, duration, distance)
        val record = if (!beat) null else when (kind) {
            KIND_TIMED -> formatDuration(duration ?: 0)
            KIND_CARDIO -> formatDistanceKm(distance ?: 0.0)
            else -> formatKg(weight)
        }
        viewModel.addEntry(exercise.id, 1, reps, weight, d.setType, d.rpe, duration, distance)
        afterLog(index, exercises.map { it.supersetGroup }, record)
        return true
    }

    val zone = remember { ZoneId.systemDefault() }
    val nowMs = remember(entriesByExercise) { System.currentTimeMillis() }
    val extraSnapshot = extraRows.toMap()
    val (doneSets, totalSets) = remember(exercises, entriesByExercise, extraSnapshot) {
        var done = 0
        var total = 0
        exercises.forEach { ex ->
            val all = entriesByExercise?.get(ex.id).orEmpty()
            val d = entriesOnDay(all, nowMs, zone).size
            done += d
            total += totalRows(d, plannedSetCount(previousSessionSets(all, nowMs, zone)), extraSnapshot[ex.id] ?: 0)
        }
        done to total
    }
    val restVisible = running || remaining != restSeconds
    var restCardHeightPx by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            Column(Modifier.statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircleIconButton(Icons.Outlined.Close, stringResource(R.string.t4_close_workout), onBack)
                    Column(Modifier.weight(1f).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(sheetName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        HeaderMeta(session?.startedAt, doneSets, totalSets)
                    }
                    // No running workout (e.g. opened from the widget): offer to start one instead of finishing.
                    when {
                        session != null -> CircleIconButton(Icons.Outlined.Check, stringResource(R.string.finish_workout), { viewModel.finishSession(session.id, onFinished) }, tint = MaterialTheme.colorScheme.primary)
                        sheetSession != null -> SoftPillButton(stringResource(R.string.start), onClick = { viewModel.startSession() })
                        else -> Spacer(Modifier.size(48.dp))
                    }
                }
                ProgressBar(if (totalSets == 0) 0f else doneSets / totalSets.toFloat(), Modifier.padding(horizontal = 16.dp))
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (!listMode && exercises.isNotEmpty()) stringResource(R.string.t4_exercise_of, pagerState.currentPage + 1, exercises.size) else "",
                        Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onSearch) { Icon(Icons.Outlined.Search, stringResource(R.string.search_exercise)) }
                    IconButton({ listMode = !listMode }) {
                        Icon(
                            if (listMode) Icons.Outlined.ViewCarousel else Icons.Outlined.ViewList,
                            stringResource(if (listMode) R.string.t4_focus_mode else R.string.t4_list_mode)
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.more)) }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.add_exercise)) }, leadingIcon = { Icon(Icons.Outlined.Add, null) }, onClick = { showMenu = false; showAdd = true })
                            DropdownMenuItem(text = { Text(stringResource(R.string.progression_rule)) }, onClick = { showMenu = false; showRule = true })
                            if (session != null) DropdownMenuItem(text = { Text(stringResource(R.string.discard_workout)) }, onClick = { showMenu = false; showDiscard = true })
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!listMode && exercises.isNotEmpty()) Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { goTo(pagerState.currentPage - 1) }, enabled = pagerState.currentPage > 0,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = CircleShape
                ) { Text(stringResource(R.string.t4_prev), style = MaterialTheme.typography.labelLarge) }
                Button(
                    onClick = { goTo(pagerState.currentPage + 1) }, enabled = pagerState.currentPage < exercises.lastIndex,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = CircleShape
                ) { Text(stringResource(R.string.t4_next), style = MaterialTheme.typography.labelLarge) }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            // Measured card height plus its 12dp margins; 0 until first layout.
            val restInset = if (restVisible) with(LocalDensity.current) { restCardHeightPx.toDp() } + 24.dp else 0.dp
            if (exercises.isEmpty()) Text(stringResource(R.string.add_exercises_hint), Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            else if (listMode) LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(exercises, key = { _, item -> item.id }) { index, exercise ->
                    val entries = entriesByExercise?.get(exercise.id).orEmpty()
                    val groups = exercises.map { it.supersetGroup }
                    val linkedPrev = exercise.supersetGroup != null && groups.getOrNull(index - 1) == exercise.supersetGroup
                    val linkedNext = exercise.supersetGroup != null && groups.getOrNull(index + 1) == exercise.supersetGroup
                    ExerciseCard(
                        exercise, entries, sheetRule = sheetRule, isPro = isPro, onUpgrade = onOpenPro,
                        expanded = expandedId == exercise.id,
                        canMoveUp = index > 0,
                        canMoveDown = index < exercises.lastIndex,
                        linkedPrev = linkedPrev,
                        linkedNext = linkedNext,
                        onSuperset = { join -> viewModel.setSuperset(exercise.id, join) },
                        onToggle = { expandedId = if (expandedId == exercise.id) null else exercise.id },
                        onMove = { up -> viewModel.moveExercise(exercise.id, up) },
                        onEdit = { name, target, kind, perSide, rule -> viewModel.updateExercise(exercise.id, name, target, kind, perSide, rule) },
                        onNote = { viewModel.updateNote(exercise.id, it) },
                        onDelete = { viewModel.deleteExercise(exercise.id) },
                        onDeleteEntry = ::deleteWithUndo
                    ) { reps, weight, record, setType, rpe, durationSec, distanceKm ->
                        viewModel.addEntry(exercise.id, 1, reps, weight, setType, rpe, durationSec, distanceKm)
                        afterLog(index, groups, record)
                    }
                }
                item { Spacer(Modifier.height(24.dp + restInset)) }
            } else HorizontalPager(pagerState, Modifier.fillMaxSize(), key = { exercises.getOrNull(it)?.id ?: it }) { page ->
                val exercise = exercises.getOrNull(page) ?: return@HorizontalPager
                val entries = entriesByExercise?.get(exercise.id).orEmpty()
                val groups = exercises.map { it.supersetGroup }
                val actions = FocusActions(
                    onSuperset = { join -> viewModel.setSuperset(exercise.id, join) },
                    onMove = { up -> viewModel.moveExercise(exercise.id, up) },
                    onEdit = { name, target, kind, perSide, rule -> viewModel.updateExercise(exercise.id, name, target, kind, perSide, rule) },
                    onNote = { viewModel.updateNote(exercise.id, it) },
                    onDelete = { viewModel.deleteExercise(exercise.id) },
                    onUpgrade = onOpenPro,
                    onLog = { _, draft -> logDraft(page, exercise, entries, draft) },
                    onUncheck = ::deleteWithUndo,
                    onAddRow = { extraRows[exercise.id] = (extraRows[exercise.id] ?: 0) + 1 }
                )
                FocusPage(
                    exercise, entries, extraRows[exercise.id] ?: 0,
                    equipment = equipmentByName[ExerciseCatalog.normalize(exercise.name)],
                    sheetRule = sheetRule, isPro = isPro,
                    drafts = draftsByExercise.getOrPut(exercise.id) { mutableStateMapOf() },
                    canMoveUp = page > 0, canMoveDown = page < exercises.lastIndex,
                    linkedNext = exercise.supersetGroup != null && groups.getOrNull(page + 1) == exercise.supersetGroup,
                    actions = actions,
                    bottomInset = restInset
                )
            }
            RestTimerCard(
                visible = restVisible, remaining = remaining, total = restTotal,
                onToggle = { if (remaining == 0) startRest() else running = !running },
                onAdd = { remaining += 15; restTotal = maxOf(restTotal, remaining); extended++ },
                onSkip = { running = false; remaining = restSeconds; restTotal = restSeconds },
                onHeight = { restCardHeightPx = it },
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
            )
        }
    }
    if (showAdd) ExerciseDialog(
        onDismiss = { showAdd = false },
        onConfirm = { name, target, kind, perSide, _ -> viewModel.addExercise(name, target, kind, perSide); showAdd = false }
    )
    if (showRule) AlertDialog(
        onDismissRequest = { showRule = false },
        title = { Text(stringResource(R.string.progression_rule)) },
        text = { RuleChips(sheetRule, allowInherit = false) { viewModel.setSheetRule(it ?: RULE_DOUBLE); showRule = false } },
        confirmButton = { TextButton(onClick = { showRule = false }) { Text(stringResource(R.string.close)) } }
    )
    if (showDiscard && session != null) AlertDialog(
        onDismissRequest = { showDiscard = false },
        title = { Text(stringResource(R.string.discard_title)) },
        text = { Text(stringResource(R.string.discard_body)) },
        confirmButton = { Button(onClick = { showDiscard = false; viewModel.discardSession(session.id, onBack) }) { Text(stringResource(R.string.discard)) } },
        dismissButton = { OutlinedButton(onClick = { showDiscard = false }) { Text(stringResource(R.string.cancel)) } }
    )
}

/** "mm:ss · a/b séries". Own composable so the once-a-second tick only recomposes the meta line. Time comes from the stored start. */
@Composable
private fun HeaderMeta(startedAt: Long?, done: Int, total: Int) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startedAt) {
        while (startedAt != null) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val text = if (startedAt == null) stringResource(R.string.t4_meta_plain, done, total)
    else stringResource(R.string.t4_meta_clock, formatDuration(((now - startedAt) / 1000).toInt().coerceAtLeast(0)), done, total)
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseCard(
    exercise: Exercise,
    entries: List<SetEntry>,
    sheetRule: String,
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
    onEdit: (String, String, String, Boolean, String?) -> Unit,
    onNote: (String) -> Unit,
    onDelete: () -> Unit,
    onDeleteEntry: (SetEntry) -> Unit,
    /** [record] is the formatted new record (load, hold or distance) or null when nothing was beaten. */
    onLog: (reps: Int, weightKg: Double, record: String?, setType: String, rpe: Double?, durationSec: Int?, distanceKm: Double?) -> Unit
) {
    // Null means "untouched": the field follows the last logged set instead of freezing the first prefill.
    var repsInput by rememberSaveable { mutableStateOf<String?>(null) }
    var weightInput by rememberSaveable { mutableStateOf<String?>(null) }
    var inputError by remember { mutableStateOf<AppError?>(null) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var historyLimit by rememberSaveable { mutableStateOf(HISTORY_PAGE) }
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.SHORT) }
    val rule = effectiveRule(exercise.progressionRule, sheetRule)
    val suggestion = remember(entries, rule, exercise.kind) {
        suggestNext(
            rule, exercise.kind,
            entries.map { HistoryEntry(it.exerciseId, exercise.name, "", it.sets, it.reps, it.weightKg, it.recordedAt, it.setType, it.rpe, exercise.target, exercise.kind, it.durationSec, it.distanceKm) },
            ZoneId.systemDefault()
        )
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
    val kind = exercise.kind
    val isLoadKind = kind == KIND_REPS || kind == KIND_BODYWEIGHT
    val sideLabel = if (exercise.perSide) stringResource(R.string.per_side_unit) else null
    // Timed/cardio inputs follow the last logged set until touched, like reps and load.
    var durationInput by rememberSaveable { mutableStateOf<String?>(null) }
    var distanceInput by rememberSaveable { mutableStateOf<String?>(null) }
    // Bodyweight: the extra-load field stays collapsed unless the last set carried load or the user opened it.
    var extraOpen by rememberSaveable { mutableStateOf<Boolean?>(null) }
    // Wall-clock start of the work stopwatch (0 = stopped), so rotation and recomposition keep counting.
    var stopwatchStart by rememberSaveable { mutableLongStateOf(0L) }
    var elapsedSec by remember { mutableIntStateOf(0) }
    LaunchedEffect(stopwatchStart) {
        while (stopwatchStart > 0) {
            elapsedSec = ((System.currentTimeMillis() - stopwatchStart) / 1000).toInt().coerceAtLeast(0)
            delay(250)
        }
    }
    val repsText = repsInput ?: prefill.reps.toString()
    val weightText = weightInput ?: prefill.weightKg?.let(::formatWeightInput).orEmpty()
    val durationText = durationInput ?: prefill.durationSec?.let(::formatDuration).orEmpty()
    val distanceText = distanceInput ?: prefill.distanceKm?.let { formatDistanceInput(it) }.orEmpty()
    val extraShown = extraOpen ?: ((prefill.weightKg ?: 0.0) > 0.0)
    val showLoadField = kind == KIND_REPS || (kind == KIND_BODYWEIGHT && extraShown)
    val summary = when {
        today.isNotEmpty() -> pluralCount(R.plurals.sets_today, today.size)
        entries.isNotEmpty() -> stringResource(R.string.last_set, formatSet(entries.first(), kind = kind, perSideLabel = sideLabel))
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
                if (entries.isNotEmpty()) when (kind) {
                    KIND_TIMED -> Text(stringResource(R.string.record_longest_hold, formatDuration(bestDurationSec(entries))), color = MaterialTheme.colorScheme.primary)
                    KIND_CARDIO -> bestDistanceKm(entries).takeIf { it > 0 }?.let {
                        Text(stringResource(R.string.record_longest_distance, formatDistanceKm(it)), color = MaterialTheme.colorScheme.primary)
                    }
                    else -> if (kind == KIND_REPS || personalRecordKg(entries) > 0) Text(
                        stringResource(R.string.pr_and_1rm, formatKg(personalRecordKg(entries)), formatKg(bestEstimatedOneRepMax(entries))),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (today.isNotEmpty()) Column {
                    Text(stringResource(R.string.today), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    today.forEachIndexed { index, entry ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}. ${setMarker(entry.setType)}${formatSet(entry, kind = kind, perSideLabel = sideLabel)}${entry.rpe?.let { " @" + formatRpe(it) }.orEmpty()}", Modifier.weight(1f))
                            IconButton(onClick = { onDeleteEntry(entry) }) { Icon(Icons.Rounded.Delete, stringResource(R.string.remove_set_n, index + 1)) }
                        }
                    }
                }
                if (kind != KIND_CARDIO && rule != RULE_NONE) ProgressionHint(
                    suggestion, isPro, onUpgrade = onUpgrade,
                    onApply = { reps, weightKg, durationSec ->
                        if (durationSec != null) durationInput = formatDuration(durationSec)
                        else {
                            repsInput = reps.toString()
                            // Bodyweight with no extra load keeps its load field closed.
                            if (kind == KIND_REPS || weightKg > 0) { weightInput = formatWeightInput(weightKg); if (kind == KIND_BODYWEIGHT) extraOpen = true }
                        }
                        inputError = null
                    }
                )
                if (sideLabel != null) Text(stringResource(R.string.per_side), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (kind == KIND_TIMED) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(formatDuration(if (stopwatchStart > 0) elapsedSec else 0), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                        Button(onClick = {
                            if (stopwatchStart > 0) {
                                durationInput = formatDuration(((System.currentTimeMillis() - stopwatchStart) / 1000).toInt()); stopwatchStart = 0
                            } else { elapsedSec = 0; stopwatchStart = System.currentTimeMillis() }
                            inputError = null
                        }) { Text(stringResource(if (stopwatchStart > 0) R.string.stop else R.string.start)) }
                    }
                }
                if (!isLoadKind) OutlinedTextField(
                    durationText, { durationInput = it; inputError = null }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.duration_label)) }, singleLine = true,
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center), placeholder = { Text("0:00") }
                )
                if (kind == KIND_CARDIO) OutlinedTextField(
                    distanceText, { distanceInput = it; inputError = null }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.distance_km)) }, singleLine = true,
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                if (isLoadKind) StepperField(
                    value = repsText, label = stringResource(R.string.reps), keyboardType = KeyboardType.Number,
                    minusEnabled = (repsText.trim().toIntOrNull() ?: 0) > 1,
                    minusDescription = stringResource(R.string.decrease_reps), plusDescription = stringResource(R.string.increase_reps),
                    onValueChange = { repsInput = it; inputError = null },
                    onMinus = { repsInput = stepReps(repsText.trim().toIntOrNull(), -1).toString(); inputError = null },
                    onPlus = { repsInput = stepReps(repsText.trim().toIntOrNull(), 1).toString(); inputError = null }
                )
                if (kind == KIND_BODYWEIGHT) TextButton(onClick = {
                    if (extraShown) { weightInput = ""; extraOpen = false } else { weightInput = null; extraOpen = true }
                    inputError = null
                }) {
                    Text(stringResource(R.string.extra_load))
                    Icon(if (extraShown) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, contentDescription = null)
                }
                if (showLoadField) StepperField(
                    value = weightText, label = stringResource(R.string.load_kg), keyboardType = KeyboardType.Decimal,
                    minusEnabled = (parseLoadKg(weightText) ?: 0.0) > 0.0,
                    minusDescription = stringResource(R.string.decrease_load), plusDescription = stringResource(R.string.increase_load),
                    onValueChange = { weightInput = it; inputError = null },
                    onMinus = { weightInput = formatWeightInput(stepWeightKg(parseLoadKg(weightText), -WEIGHT_STEP_KG)); inputError = null },
                    onPlus = { weightInput = formatWeightInput(stepWeightKg(parseLoadKg(weightText), WEIGHT_STEP_KG)); inputError = null },
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
                        // Timed/cardio sets log one rep and no load; a malformed duration/distance becomes -1 so validation rejects it.
                        val parsedReps = if (isLoadKind) repsText.trim().toIntOrNull() else 1
                        // Blank load means bodyweight; only a malformed number is an error.
                        val parsedWeight = if (showLoadField) (if (weightText.isBlank()) 0.0 else parseLoadKg(weightText)) else 0.0
                        val duration = if (isLoadKind) null else durationText.trim().let { if (it.isEmpty()) null else parseDuration(it) ?: -1 }
                        val distance = if (kind != KIND_CARDIO) null else distanceText.trim().let { if (it.isEmpty()) null else parseLoadKg(it) ?: -1.0 }
                        inputError = validateSetInput(1, parsedReps, parsedWeight) ?: validateKindEffort(kind, duration, distance)
                        if (inputError == null) {
                            val beat = setType != SET_WARMUP && beatsKindRecord(kind, entries, parsedWeight!!, duration, distance)
                            val record = if (!beat) null else when (kind) {
                                KIND_TIMED -> formatDuration(duration ?: 0)
                                KIND_CARDIO -> formatDistanceKm(distance ?: 0.0)
                                else -> formatKg(parsedWeight!!)
                            }
                            onLog(parsedReps!!, parsedWeight!!, record, setType, rpe, duration, distance)
                            setType = SET_NORMAL; rpe = null; durationInput = null; distanceInput = null
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
                                "${dateFormat.format(Date(entry.recordedAt))} · ${formatSet(entry, kind = kind, perSideLabel = sideLabel)}",
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
    if (showPlates) PlatesDialog(parseLoadKg(weightText) ?: 0.0, isPro, onUpgrade = { showPlates = false; onUpgrade() }, onDismiss = { showPlates = false })
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
            initialKind = exercise.kind,
            initialPerSide = exercise.perSide,
            showRule = true,
            initialRule = exercise.progressionRule,
            title = stringResource(R.string.edit_exercise),
            actionLabel = stringResource(R.string.save),
            onDismiss = { showEdit = false },
            onConfirm = { name, target, newKind, perSide, newRule -> showEdit = false; onEdit(name, target, newKind, perSide, newRule) }
        )
    }
}

@Composable
internal fun setTypeLabel(t: String) = stringResource(when (t) { SET_WARMUP -> R.string.set_warmup; SET_DROP -> R.string.set_drop; SET_FAILURE -> R.string.set_failure; else -> R.string.set_normal })

@Composable
internal fun setMarker(t: String) = when (t) { SET_WARMUP -> stringResource(R.string.marker_warmup) + " "; SET_DROP -> stringResource(R.string.marker_drop) + " "; SET_FAILURE -> stringResource(R.string.marker_failure) + " "; else -> "" }

internal fun formatRpe(r: Double) = if (r % 1.0 == 0.0) r.toInt().toString() else "%.1f".format(java.util.Locale.getDefault(), r)

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
internal fun PlatesDialog(targetKg: Double, isPro: Boolean, onUpgrade: () -> Unit, onDismiss: () -> Unit) {
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

@Composable
private fun kindLabel(kind: String) = stringResource(when (kind) {
    KIND_BODYWEIGHT -> R.string.kind_bodyweight; KIND_TIMED -> R.string.kind_timed; KIND_CARDIO -> R.string.kind_cardio; else -> R.string.kind_reps
})

@Composable
private fun ruleLabel(rule: String) = stringResource(when (rule) {
    RULE_NONE -> R.string.rule_none; RULE_LINEAR -> R.string.rule_linear; RULE_TIME -> R.string.rule_time; else -> R.string.rule_double
})

/** [selected] null = follow the sheet (only offered when [allowInherit]). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RuleChips(selected: String?, allowInherit: Boolean, onSelect: (String?) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (allowInherit) FilterChip(selected == null, { onSelect(null) }, { Text(stringResource(R.string.rule_inherit)) })
        PROGRESSION_RULES.forEach { r -> FilterChip(selected == r, { onSelect(r) }, { Text(ruleLabel(r)) }) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ExerciseDialog(
    initialName: String = "",
    initialTarget: String = "",
    initialKind: String = KIND_REPS,
    initialPerSide: Boolean = false,
    showRule: Boolean = false,
    initialRule: String? = null,
    title: String = stringResource(R.string.add_exercise),
    actionLabel: String = stringResource(R.string.add),
    onDismiss: () -> Unit,
    onConfirm: (name: String, target: String, kind: String, perSide: Boolean, rule: String?) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var target by rememberSaveable { mutableStateOf(initialTarget) }
    var kind by rememberSaveable { mutableStateOf(initialKind) }
    var perSide by rememberSaveable { mutableStateOf(initialPerSide) }
    var rule by rememberSaveable { mutableStateOf(initialRule) }
    // A new exercise guesses its kind from the name until the user picks one.
    var kindPicked by rememberSaveable { mutableStateOf(initialName.isNotEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it; if (!kindPicked) kind = ExerciseCatalog.inferKind(it, muscle = target) }, label = { Text(stringResource(R.string.name)) }, singleLine = true)
            OutlinedTextField(target, { target = it }, label = { Text(stringResource(R.string.muscle_group)) }, singleLine = true)
            Text(stringResource(R.string.exercise_kind), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EXERCISE_KINDS.forEach { k -> FilterChip(kind == k, { kind = k; kindPicked = true }, { Text(kindLabel(k)) }) }
            }
            if (kind != KIND_CARDIO) Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(perSide, role = Role.Switch, onValueChange = { perSide = it }),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.per_side))
                Switch(perSide, onCheckedChange = null)
            }
            if (showRule) {
                Text(stringResource(R.string.progression_rule), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                RuleChips(rule, allowInherit = true) { rule = it }
            }
        } },
        confirmButton = { Button(enabled = name.isNotBlank(), onClick = { onConfirm(name, target, kind, perSide && kind != KIND_CARDIO, rule) }) { Text(actionLabel) } },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
