package com.repforge.app.presentation.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.repforge.app.R
import com.repforge.app.RepForgeApplication
import com.repforge.app.domain.model.TrainingSheet
import com.repforge.app.domain.schedule.DayPlan
import com.repforge.app.domain.templates.WorkoutTemplate
import com.repforge.app.presentation.components.EmptyState
import com.repforge.app.presentation.components.GlyphSquare
import com.repforge.app.presentation.components.HairlineDivider
import com.repforge.app.presentation.components.LargeTitleHeader
import com.repforge.app.presentation.components.RfCard
import com.repforge.app.presentation.components.SectionHeader
import com.repforge.app.presentation.components.SoftPillButton
import com.repforge.app.presentation.components.Tag
import com.repforge.app.presentation.components.TagTone
import com.repforge.app.presentation.home.CreateSheetDialog
import com.repforge.app.presentation.pluralCount
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private const val MOVE_WINDOW_DAYS = 14L

/**
 * [onDuplicateSheet]/[onDeleteSheet] default to null: the screen then talks to the repository itself so
 * existing call sites keep working; pass them to route through a ViewModel instead.
 */
@Composable
fun PlanScreen(
    state: PlanUiState,
    onSetWeekday: (Int, Long?) -> Unit,
    onReschedule: (LocalDate, LocalDate) -> Unit,
    onCreateSheet: (String) -> Unit,
    onCreateSheetFromTemplate: (WorkoutTemplate) -> Unit,
    modifier: Modifier = Modifier,
    onStartSheet: (Long) -> Unit = {},
    onOpenSheet: (Long) -> Unit = {},
    onDuplicateSheet: ((Long, String) -> Unit)? = null,
    onDeleteSheet: ((Long) -> Unit)? = null
) {
    val locale = LocalConfiguration.current.locales[0]
    val names = state.sheets.associate { it.id to it.name }
    val repository = (LocalContext.current.applicationContext as RepForgeApplication).repository
    val scope = rememberCoroutineScope()
    val duplicate: (Long, String) -> Unit = onDuplicateSheet ?: { id, label -> scope.launch { repository.duplicateSheet(id, label) } }
    val delete: (Long) -> Unit = onDeleteSheet ?: { id -> scope.launch { repository.deleteSheet(id) } }
    val copyLabel = stringResource(R.string.copy_suffix)
    var moveFrom by remember { mutableStateOf<LocalDate?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var pickDay by remember { mutableStateOf<DayOfWeek?>(null) }
    var deleteTarget by remember { mutableStateOf<TrainingSheet?>(null) }
    val gutter = Modifier.padding(horizontal = 16.dp)

    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { LargeTitleHeader(stringResource(R.string.nav_plan), stringResource(R.string.t3_plan_subtitle)) }
        item { SectionHeader(stringResource(R.string.t3_week), modifier = gutter.padding(top = 8.dp)) }
        items(DayOfWeek.entries, key = { "w${it.value}" }) { day ->
            val dayName = day.getDisplayName(TextStyle.FULL, locale)
            val sheetName = names[state.weekly[day.value]]
            val description = stringResource(R.string.plan_choose_sheet, dayName) + ": " + (sheetName ?: stringResource(R.string.rest))
            RfCard(gutter.semantics { contentDescription = description }, padding = 14.dp, onClick = { pickDay = day }) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(dayName, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    if (sheetName != null) Tag(sheetName, TagTone.Accent) else Tag(stringResource(R.string.rest), TagTone.Neutral)
                    Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            SectionHeader(
                stringResource(R.string.t3_routines),
                trailing = { SoftPillButton(stringResource(R.string.t3_new), Icons.Outlined.Add, { showCreate = true }) },
                modifier = gutter.padding(top = 16.dp)
            )
        }
        if (state.sheets.isEmpty()) {
            item { EmptyState(Icons.Outlined.FitnessCenter, stringResource(R.string.t3_no_routines_title), stringResource(R.string.t3_no_routines_body)) }
        }
        items(state.sheets, key = { "s${it.id}" }) { sheet ->
            RoutineRow(
                sheet, gutter,
                onOpen = { onOpenSheet(sheet.id) }, onStart = { onStartSheet(sheet.id) },
                onDuplicate = { duplicate(sheet.id, copyLabel) }, onDelete = { deleteTarget = sheet }
            )
        }
        item { SectionHeader(stringResource(R.string.plan_next_days), modifier = gutter.padding(top = 16.dp)) }
        item {
            RfCard(gutter, padding = 0.dp) {
                Column {
                    state.upcoming.forEachIndexed { i, plan ->
                        if (i > 0) HairlineDivider(inset = 16.dp)
                        DayRow(plan, state.today, names[plan.sheetId], locale) { moveFrom = plan.date }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }

    pickDay?.let { day ->
        SheetPickerSheet(
            title = stringResource(R.string.plan_choose_sheet, day.getDisplayName(TextStyle.FULL, locale)), sheets = state.sheets,
            selectedId = state.weekly[day.value], includeRest = true,
            onDismiss = { pickDay = null }, onPick = { onSetWeekday(day.value, it); pickDay = null }
        )
    }
    moveFrom?.let { from ->
        MoveDialog(state.today, onDismiss = { moveFrom = null }, onPick = { onReschedule(from, it); moveFrom = null })
    }
    deleteTarget?.let { sheet ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.delete_sheet_title)) },
            text = { Text(stringResource(R.string.delete_sheet_body, sheet.name)) },
            confirmButton = { Button(onClick = { delete(sheet.id); deleteTarget = null }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { OutlinedButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
    if (showCreate) CreateSheetDialog(
        onDismiss = { showCreate = false },
        onCreate = { onCreateSheet(it); showCreate = false },
        onCreateFromTemplate = { onCreateSheetFromTemplate(it); showCreate = false }
    )
}

@Composable
private fun RoutineRow(
    sheet: TrainingSheet, modifier: Modifier,
    onOpen: () -> Unit, onStart: () -> Unit, onDuplicate: () -> Unit, onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    val openCd = stringResource(R.string.t3_open_routine_cd, sheet.name)
    RfCard(modifier, padding = 12.dp, onClick = onOpen) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlyphSquare(Icons.Outlined.FitnessCenter)
            Column(Modifier.weight(1f).semantics { contentDescription = openCd }) {
                Text(sheet.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(pluralCount(R.plurals.exercises_count, sheet.exerciseCount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Outlined.MoreVert, stringResource(R.string.t3_routine_options_cd, sheet.name), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.start)) }, onClick = { menu = false; onStart() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.duplicate)) }, onClick = { menu = false; onDuplicate() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; onDelete() })
                }
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Bottom sheet listing [sheets] (optionally with a leading "Rest" row); shared with the Library "+ Sheet" flow. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SheetPickerSheet(
    title: String, sheets: List<TrainingSheet>, selectedId: Long?, includeRest: Boolean,
    onDismiss: () -> Unit, onPick: (Long?) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
            if (includeRest) PickerRow(stringResource(R.string.rest), selectedId == null) { onPick(null) }
            sheets.forEach { PickerRow(it.name, it.id == selectedId) { onPick(it.id) } }
        }
    }
}

@Composable
private fun PickerRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        if (selected) Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun DayRow(plan: DayPlan, today: LocalDate, sheetName: String?, locale: Locale, onMove: () -> Unit) {
    val date = plan.date.format(DateTimeFormatter.ofPattern("EEE, d MMM", locale))
    val label = if (plan.date == today) stringResource(R.string.plan_today) + " · " + date else date
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(sheetName ?: stringResource(R.string.rest), style = MaterialTheme.typography.bodyLarge)
        }
        if (plan.moved) Tag(stringResource(R.string.moved_badge), TagTone.Neutral, Modifier.padding(end = 8.dp))
        if (sheetName != null) {
            val description = stringResource(R.string.plan_move_cd, sheetName)
            TextButton(onClick = onMove, modifier = Modifier.semantics { contentDescription = description }) { Text(stringResource(R.string.plan_move)) }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MoveDialog(today: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val last = today.plusDays(MOVE_WINDOW_DAYS - 1)
    // The picker speaks UTC-midnight millis, so dates round-trip through UTC to avoid zone drift.
    fun Long.toDate() = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis.toDate() in today..last
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = pickerState.selectedDateMillis != null,
                onClick = { pickerState.selectedDateMillis?.let { onPick(it.toDate()) } }
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    ) {
        DatePicker(pickerState, title = { Text(stringResource(R.string.plan_move_title), Modifier.padding(start = 24.dp, top = 16.dp)) })
    }
}
