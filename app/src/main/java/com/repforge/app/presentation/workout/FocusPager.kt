package com.repforge.app.presentation.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.repforge.app.R
import com.repforge.app.domain.logging.formatDistanceKm
import com.repforge.app.domain.logging.formatDuration
import com.repforge.app.domain.logging.formatKg
import com.repforge.app.domain.logging.formatSet
import com.repforge.app.domain.logging.prefillFor
import com.repforge.app.domain.metrics.bestDistanceKm
import com.repforge.app.domain.metrics.bestDurationSec
import com.repforge.app.domain.metrics.bestEstimatedOneRepMax
import com.repforge.app.domain.metrics.personalRecordKg
import com.repforge.app.domain.model.Exercise
import com.repforge.app.domain.model.HistoryEntry
import com.repforge.app.domain.model.KIND_CARDIO
import com.repforge.app.domain.model.KIND_TIMED
import com.repforge.app.domain.model.SetEntry
import com.repforge.app.domain.progression.RULE_NONE
import com.repforge.app.domain.progression.effectiveRule
import com.repforge.app.domain.progression.suggestNext
import com.repforge.app.presentation.components.CircleIconButton
import com.repforge.app.presentation.components.Tag
import com.repforge.app.presentation.components.TagTone
import com.repforge.app.presentation.equipmentLabel
import com.repforge.app.presentation.exerciseDisplayName
import com.repforge.app.presentation.muscleLabel
import kotlinx.coroutines.delay
import java.time.ZoneId

/** Everything a focus page can ask of its screen; keeps [FocusPage] free of view-model plumbing. */
internal class FocusActions(
    val onSuperset: (join: Boolean) -> Unit,
    val onMove: (up: Boolean) -> Unit,
    val onEdit: (name: String, target: String, kind: String, perSide: Boolean, rule: String?) -> Unit,
    val onNote: (String) -> Unit,
    val onDelete: () -> Unit,
    val onUpgrade: () -> Unit,
    /** Logs a pending row; returns true when the set was valid and stored. */
    val onLog: (position: Int, draft: RowDraft) -> Boolean,
    val onUncheck: (SetEntry) -> Unit,
    val onAddRow: () -> Unit
)

/** One exercise of the focus pager: header, tag pills, last-time line, progression hint and the set table. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FocusPage(
    exercise: Exercise,
    entries: List<SetEntry>,
    extraRows: Int,
    equipment: String?,
    sheetRule: String,
    isPro: Boolean,
    drafts: SnapshotStateMap<Int, RowDraft>,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    linkedNext: Boolean,
    actions: FocusActions,
    modifier: Modifier = Modifier
) {
    val zone = remember { ZoneId.systemDefault() }
    val now = remember(entries) { System.currentTimeMillis() }
    val today = remember(entries) { com.repforge.app.domain.logging.entriesOnDay(entries, now, zone) }
    val previous = remember(entries) { previousSessionSets(entries, now, zone) }
    val latest = remember(entries) { prefillFor(entries) }
    val kind = exercise.kind
    val planned = plannedSetCount(previous)
    val rows = totalRows(today.size, planned, extraRows)
    val rule = effectiveRule(exercise.progressionRule, sheetRule)
    val suggestion = remember(entries, rule, kind) {
        suggestNext(
            rule, kind,
            entries.map { HistoryEntry(it.exerciseId, exercise.name, "", it.sets, it.reps, it.weightKg, it.recordedAt, it.setType, it.rpe, exercise.target, kind, it.durationSec, it.distanceKm) },
            zone
        )
    }
    val sideLabel = if (exercise.perSide) stringResource(R.string.per_side_unit) else null
    fun draftAt(p: Int): RowDraft = drafts[p] ?: defaultsFor(p, today.size, previous, latest).toDraft(kind)

    var showMenu by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showPlates by rememberSaveable { mutableStateOf(false) }
    val displayName = exerciseDisplayName(exercise.name)

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (exercise.supersetGroup != null) Text(stringResource(R.string.superset), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Text(displayName, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            }
            CircleIconButton(Icons.Outlined.Info, stringResource(R.string.t4_exercise_info), { showInfo = true })
            androidx.compose.foundation.layout.Box {
                CircleIconButton(Icons.Outlined.MoreVert, stringResource(R.string.more_actions_for, displayName), { showMenu = true })
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.move_up)) }, enabled = canMoveUp, leadingIcon = { Icon(Icons.Outlined.KeyboardArrowUp, null) }, onClick = { showMenu = false; actions.onMove(true) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.move_down)) }, enabled = canMoveDown, leadingIcon = { Icon(Icons.Outlined.KeyboardArrowDown, null) }, onClick = { showMenu = false; actions.onMove(false) })
                    if (canMoveDown && !linkedNext) DropdownMenuItem(text = { Text(stringResource(R.string.join_superset)) }, leadingIcon = { Icon(Icons.Outlined.Link, null) }, onClick = { showMenu = false; actions.onSuperset(true) })
                    if (exercise.supersetGroup != null) DropdownMenuItem(text = { Text(stringResource(R.string.leave_superset)) }, leadingIcon = { Icon(Icons.Outlined.LinkOff, null) }, onClick = { showMenu = false; actions.onSuperset(false) })
                    DropdownMenuItem(text = { Text(stringResource(R.string.plate_calculator)) }, leadingIcon = { Icon(Icons.Outlined.FitnessCenter, null) }, onClick = { showMenu = false; showPlates = true })
                    DropdownMenuItem(text = { Text(stringResource(R.string.edit_exercise)) }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = { showMenu = false; showEdit = true })
                    DropdownMenuItem(text = { Text(stringResource(R.string.exercise_note)) }, leadingIcon = { Icon(Icons.Outlined.Notes, null) }, onClick = { showMenu = false; showNote = true })
                    DropdownMenuItem(text = { Text(stringResource(R.string.remove_exercise)) }, leadingIcon = { Icon(Icons.Outlined.Delete, null) }, onClick = { showMenu = false; showDelete = true })
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (exercise.target.isNotBlank()) Tag(muscleLabel(exercise.target))
            if (equipment != null) Tag(equipmentLabel(equipment))
            recordText(kind, entries)?.let { Tag(stringResource(R.string.t4_record, it), TagTone.Accent) }
        }
        Text(
            if (previous.isEmpty()) stringResource(R.string.t4_first_time)
            else stringResource(R.string.t4_last_time, previous.take(3).joinToString(" · ") { formatSet(it, kind = kind, perSideLabel = sideLabel) } + if (previous.size > 3) " …" else ""),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis
        )
        if (exercise.note.isNotBlank()) Text(exercise.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (kind != KIND_CARDIO && rule != RULE_NONE) ProgressionHint(
            suggestion, isPro, onUpgrade = actions.onUpgrade,
            onApply = { reps, weightKg, durationSec ->
                val p = today.size
                val base = draftAt(p)
                drafts[p] = if (durationSec != null) base.copy(durationSec = durationSec) else base.copy(reps = reps, weightKg = weightKg)
            }
        )
        if (sideLabel != null) Text(stringResource(R.string.per_side), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (kind == KIND_TIMED) Stopwatch { sec ->
            val p = today.size
            drafts[p] = draftAt(p).copy(durationSec = sec)
        }
        SetTable(
            exercise, today, rows, ::draftAt, drafts,
            onCheck = { p, d -> if (actions.onLog(p, d)) drafts.remove(p) },
            onUncheck = actions.onUncheck, onAddRow = actions.onAddRow
        )
        Spacer(Modifier.height(24.dp))
    }

    if (showPlates) PlatesDialog(draftAt(today.size).weightKg, isPro, onUpgrade = { showPlates = false; actions.onUpgrade() }, onDismiss = { showPlates = false })
    if (showInfo) AlertDialog(
        onDismissRequest = { showInfo = false },
        title = { Text(displayName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (exercise.target.isNotBlank()) Text(stringResource(R.string.t4_muscle) + ": " + muscleLabel(exercise.target))
                if (equipment != null) Text(stringResource(R.string.t4_equipment) + ": " + equipmentLabel(equipment))
                recordText(kind, entries)?.let { Text(stringResource(R.string.t4_record, it), color = MaterialTheme.colorScheme.primary) }
                if (kind != KIND_TIMED && kind != KIND_CARDIO && entries.isNotEmpty()) Text(stringResource(R.string.pr_and_1rm, formatKg(personalRecordKg(entries)), formatKg(bestEstimatedOneRepMax(entries))))
                if (exercise.note.isNotBlank()) Text(exercise.note, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { Button(onClick = { showInfo = false }) { Text(stringResource(R.string.close)) } }
    )
    if (showDelete) AlertDialog(
        onDismissRequest = { showDelete = false },
        title = { Text(stringResource(R.string.remove_exercise_title)) },
        text = { Text(stringResource(R.string.remove_exercise_body)) },
        confirmButton = { Button(onClick = { showDelete = false; actions.onDelete() }) { Text(stringResource(R.string.remove)) } },
        dismissButton = { OutlinedButton(onClick = { showDelete = false }) { Text(stringResource(R.string.cancel)) } }
    )
    if (showNote) {
        var note by rememberSaveable { mutableStateOf(exercise.note) }
        AlertDialog(
            onDismissRequest = { showNote = false },
            title = { Text(stringResource(R.string.exercise_note)) },
            text = { OutlinedTextField(note, { note = it }, label = { Text(stringResource(R.string.note)) }, minLines = 2) },
            confirmButton = { Button(onClick = { showNote = false; actions.onNote(note) }) { Text(stringResource(R.string.save)) } },
            dismissButton = { OutlinedButton(onClick = { showNote = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
    if (showEdit) ExerciseDialog(
        initialName = exercise.name, initialTarget = exercise.target, initialKind = kind, initialPerSide = exercise.perSide,
        showRule = true, initialRule = exercise.progressionRule,
        title = stringResource(R.string.edit_exercise), actionLabel = stringResource(R.string.save),
        onDismiss = { showEdit = false },
        onConfirm = { name, target, newKind, perSide, newRule -> showEdit = false; actions.onEdit(name, target, newKind, perSide, newRule) }
    )
}

@Composable
private fun recordText(kind: String, entries: List<SetEntry>): String? {
    if (entries.isEmpty()) return null
    return when (kind) {
        KIND_TIMED -> formatDuration(bestDurationSec(entries)).takeIf { bestDurationSec(entries) > 0 }
        KIND_CARDIO -> bestDistanceKm(entries).takeIf { it > 0 }?.let { formatDistanceKm(it) }
        else -> personalRecordKg(entries).takeIf { it > 0 }?.let { formatKg(it) }
    }
}

/** Work stopwatch for timed holds; stopping hands the elapsed seconds to [onStop]. Wall-clock based so rotation keeps counting. */
@Composable
private fun Stopwatch(onStop: (Int) -> Unit) {
    var start by rememberSaveable { mutableLongStateOf(0L) }
    var elapsed by remember { mutableIntStateOf(0) }
    LaunchedEffect(start) {
        while (start > 0) {
            elapsed = ((System.currentTimeMillis() - start) / 1000).toInt().coerceAtLeast(0)
            delay(250)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(formatDuration(if (start > 0) elapsed else 0), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
        Button(onClick = {
            if (start > 0) { onStop(((System.currentTimeMillis() - start) / 1000).toInt()); start = 0 }
            else { elapsed = 0; start = System.currentTimeMillis() }
        }) { Text(stringResource(if (start > 0) R.string.stop else R.string.start)) }
    }
}
