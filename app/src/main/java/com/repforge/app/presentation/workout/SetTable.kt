package com.repforge.app.presentation.workout

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.repforge.app.R
import com.repforge.app.domain.logging.SetPrefill
import com.repforge.app.domain.logging.WEIGHT_STEP_KG
import com.repforge.app.domain.logging.formatDistanceInput
import com.repforge.app.domain.logging.formatDuration
import com.repforge.app.domain.logging.formatWeightInput
import com.repforge.app.domain.logging.parseDuration
import com.repforge.app.domain.logging.stepReps
import com.repforge.app.domain.logging.stepWeightKg
import com.repforge.app.domain.model.Exercise
import com.repforge.app.domain.model.KIND_BODYWEIGHT
import com.repforge.app.domain.model.KIND_CARDIO
import com.repforge.app.domain.model.KIND_TIMED
import com.repforge.app.domain.model.RPE_VALUES
import com.repforge.app.domain.model.SET_NORMAL
import com.repforge.app.domain.model.SET_TYPES
import com.repforge.app.domain.model.SetEntry
import com.repforge.app.domain.validation.parseWeightKg
import com.repforge.app.presentation.components.RfCard

/** Editable values of one pending row; only the fields of the exercise kind are used when logging. */
data class RowDraft(
    val reps: Int,
    val weightKg: Double,
    val durationSec: Int,
    val distanceKm: Double,
    val setType: String = SET_NORMAL,
    val rpe: Double? = null
)

internal fun SetPrefill.toDraft(kind: String) = RowDraft(
    reps = reps, weightKg = weightKg ?: 0.0,
    durationSec = durationSec ?: if (kind == KIND_TIMED) 30 else 0, distanceKm = distanceKm ?: 0.0
)

/** One editable column: caption, how to show/step/parse a draft value, and how to show the same value of a logged entry. */
internal class SetColumn(
    @StringRes val caption: Int,
    val decimal: Boolean,
    val text: (RowDraft) -> String,
    val step: (RowDraft, Int) -> RowDraft,
    val parse: (RowDraft, String) -> RowDraft?,
    val entryText: (SetEntry) -> String
)

private fun load(kg: Double) = formatWeightInput(kg).ifBlank { "0" }
private fun extra(kg: Double) = if (kg > 0) "+" + formatWeightInput(kg) else "0"
private const val DURATION_STEP_SEC = 5
private const val DISTANCE_STEP_KM = 0.5

private val weightCol = SetColumn(
    R.string.t4_col_weight, true, { load(it.weightKg) }, { d, s -> d.copy(weightKg = stepWeightKg(d.weightKg, s * WEIGHT_STEP_KG)) },
    { d, t -> parseWeightKg(t)?.takeIf { it >= 0 }?.let { d.copy(weightKg = it) } }, { load(it.weightKg) }
)
private val extraCol = SetColumn(
    R.string.t4_col_extra, true, { extra(it.weightKg) }, { d, s -> d.copy(weightKg = stepWeightKg(d.weightKg, s * WEIGHT_STEP_KG)) },
    { d, t -> parseWeightKg(t)?.takeIf { it >= 0 }?.let { d.copy(weightKg = it) } }, { extra(it.weightKg) }
)
private val repsCol = SetColumn(
    R.string.t4_col_reps, false, { it.reps.toString() }, { d, s -> d.copy(reps = stepReps(d.reps, s)) },
    { d, t -> t.trim().toIntOrNull()?.takeIf { it > 0 }?.let { d.copy(reps = it) } }, { it.reps.toString() }
)
private val timeCol = SetColumn(
    R.string.t4_col_time, false, { formatDuration(it.durationSec) }, { d, s -> d.copy(durationSec = (d.durationSec + s * DURATION_STEP_SEC).coerceAtLeast(0)) },
    { d, t -> parseDuration(t.trim())?.takeIf { it >= 0 }?.let { d.copy(durationSec = it) } }, { formatDuration(it.durationSec ?: 0) }
)
private val distanceCol = SetColumn(
    R.string.t4_col_distance, true, { formatDistanceInput(it.distanceKm).ifBlank { "0" } },
    { d, s -> d.copy(distanceKm = (d.distanceKm + s * DISTANCE_STEP_KM).coerceAtLeast(0.0)) },
    { d, t -> parseWeightKg(t)?.takeIf { it >= 0 }?.let { d.copy(distanceKm = it) } }, { formatDistanceInput(it.distanceKm ?: 0.0).ifBlank { "0" } }
)

internal fun columnsFor(kind: String): List<SetColumn> = when (kind) {
    KIND_BODYWEIGHT -> listOf(repsCol, extraCol)
    KIND_TIMED -> listOf(timeCol)
    KIND_CARDIO -> listOf(timeCol, distanceCol)
    else -> listOf(weightCol, repsCol)
}

private val NUMBER_SIZE = 28.dp
private val CHECK_SIZE = 40.dp
private val ROW_GAP = 6.dp

/**
 * Set table card: one row per planned set. Logged sets ([done], oldest first) show dimmed with a filled check; pending rows have
 * steppers backed by [drafts] (key = row position). [onCheck] logs a pending row, [onUncheck] removes a logged one.
 */
@Composable
internal fun SetTable(
    exercise: Exercise,
    done: List<SetEntry>,
    totalRows: Int,
    draftAt: (Int) -> RowDraft,
    drafts: SnapshotStateMap<Int, RowDraft>,
    onCheck: (position: Int, draft: RowDraft) -> Unit,
    onUncheck: (SetEntry) -> Unit,
    onAddRow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cols = columnsFor(exercise.kind)
    var editing by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var optionsFor by remember { mutableStateOf<Int?>(null) }
    RfCard(modifier.fillMaxWidth(), padding = 12.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ROW_GAP), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(NUMBER_SIZE))
                cols.forEach {
                    Text(
                        stringResource(it.caption), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(CHECK_SIZE))
            }
            for (p in 0 until maxOf(totalRows, done.size)) {
                val entry = done.getOrNull(p)
                if (entry != null) DoneRow(p + 1, entry, cols, onUncheck = { onUncheck(entry) })
                else {
                    val draft = draftAt(p)
                    PendingRow(
                        p + 1, draft, cols,
                        onStep = { c, s -> drafts[p] = cols[c].step(draft, s) },
                        onEdit = { c -> editing = p to c },
                        onOptions = { optionsFor = p },
                        onCheck = { onCheck(p, draft) }
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(MaterialTheme.shapes.small).clickable(role = Role.Button, onClick = onAddRow),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(" " + stringResource(R.string.t4_add_set), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
    editing?.let { (p, c) ->
        val draft = draftAt(p)
        EditValueDialog(
            initial = cols[c].text(draft).removePrefix("+"), decimal = cols[c].decimal,
            onConfirm = { text -> cols[c].parse(draft, text)?.let { drafts[p] = it } },
            onDismiss = { editing = null }
        )
    }
    optionsFor?.let { p ->
        SetOptionsDialog(p + 1, draftAt(p), onChange = { drafts[p] = it }, onDismiss = { optionsFor = null })
    }
}

@Composable
private fun NumberCircle(number: Int, done: Boolean, marker: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier.size(NUMBER_SIZE).background(if (done) cs.primary else cs.surfaceContainerHigh, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (marker.isBlank()) number.toString() else marker.trim(), style = MaterialTheme.typography.labelMedium,
            color = if (done) cs.onPrimary else cs.onSurfaceVariant, maxLines = 1
        )
    }
}

@Composable
private fun CheckCircle(done: Boolean, description: String, onToggle: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier.size(CHECK_SIZE).toggleable(done, role = Role.Checkbox, onValueChange = onToggle).semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(32.dp).background(if (done) cs.primary else cs.surfaceContainerHighest, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Check, null, tint = if (done) cs.onPrimary else cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun DoneRow(number: Int, entry: SetEntry, cols: List<SetColumn>, onUncheck: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ROW_GAP), verticalAlignment = Alignment.CenterVertically) {
        NumberCircle(number, done = true, marker = setMarker(entry.setType))
        cols.forEach {
            Box(
                Modifier.weight(1f).heightIn(min = 48.dp).clip(MaterialTheme.shapes.small).background(cs.surfaceContainerHigh).alpha(0.6f),
                contentAlignment = Alignment.Center
            ) { Text(it.entryText(entry), style = MaterialTheme.typography.titleMedium, color = cs.onSurface) }
        }
        CheckCircle(true, stringResource(R.string.t4_unlog_set, number)) { onUncheck() }
    }
}

@Composable
private fun PendingRow(
    number: Int,
    draft: RowDraft,
    cols: List<SetColumn>,
    onStep: (col: Int, sign: Int) -> Unit,
    onEdit: (col: Int) -> Unit,
    onOptions: () -> Unit,
    onCheck: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ROW_GAP), verticalAlignment = Alignment.CenterVertically) {
        val optionsCd = stringResource(R.string.t4_set_options, number)
        NumberCircle(number, done = false, marker = setMarker(draft.setType), modifier = Modifier.clickable(onClickLabel = optionsCd, role = Role.Button, onClick = onOptions))
        cols.forEachIndexed { i, col ->
            CompactStepper(col.text(draft), { onStep(i, -1) }, { onStep(i, 1) }, { onEdit(i) }, Modifier.weight(1f))
        }
        CheckCircle(false, stringResource(R.string.t4_log_set, number)) { onCheck() }
    }
}

/** Same look as PillStepper but with tight 48dp buttons so two of them plus the row chrome fit 360dp wide screens. */
@Composable
private fun CompactStepper(value: String, onDec: () -> Unit, onInc: () -> Unit, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Row(modifier.heightIn(min = 48.dp).clip(MaterialTheme.shapes.small).background(cs.surfaceContainerHigh), verticalAlignment = Alignment.CenterVertically) {
        StepButton(Icons.Outlined.Remove, stringResource(R.string.t4_decrease), onDec)
        Box(
            Modifier.weight(1f).fillMaxHeight().clickable(role = Role.Button, onClickLabel = stringResource(R.string.t1_edit_value), onClick = onEdit),
            contentAlignment = Alignment.Center
        ) { Text(value, style = MaterialTheme.typography.titleMedium, color = cs.onSurface, textAlign = TextAlign.Center, maxLines = 1) }
        StepButton(Icons.Outlined.Add, stringResource(R.string.t4_increase), onInc)
    }
}

@Composable
private fun StepButton(icon: ImageVector, cd: String, onClick: () -> Unit) {
    Box(Modifier.width(48.dp).heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, cd, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
    }
}

@Composable
internal fun EditValueDialog(initial: String, decimal: Boolean, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.t1_edit_value)) },
        text = {
            OutlinedTextField(
                text, { text = it }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number)
            )
        },
        confirmButton = { Button(onClick = { onConfirm(text); onDismiss() }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetOptionsDialog(number: Int, draft: RowDraft, onChange: (RowDraft) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.t4_set_options, number)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.t4_set_type), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SET_TYPES.forEach { t -> FilterChip(draft.setType == t, { onChange(draft.copy(setType = t)) }, { Text(setTypeLabel(t)) }) }
                }
                Text(stringResource(R.string.rpe), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RPE_VALUES.forEach { r -> FilterChip(draft.rpe == r, { onChange(draft.copy(rpe = if (draft.rpe == r) null else r)) }, { Text(formatRpe(r)) }) }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
    )
}
