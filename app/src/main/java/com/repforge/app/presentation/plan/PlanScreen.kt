package com.repforge.app.presentation.plan

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.repforge.app.R
import com.repforge.app.domain.model.TrainingSheet
import com.repforge.app.domain.schedule.DayPlan
import com.repforge.app.domain.templates.WorkoutTemplate
import com.repforge.app.presentation.home.CreateSheetDialog
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private const val MOVE_WINDOW_DAYS = 14L

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PlanScreen(
    state: PlanUiState,
    onSetWeekday: (Int, Long?) -> Unit,
    onReschedule: (LocalDate, LocalDate) -> Unit,
    onCreateSheet: (String) -> Unit,
    onCreateSheetFromTemplate: (WorkoutTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val names = state.sheets.associate { it.id to it.name }
    var moveFrom by remember { mutableStateOf<LocalDate?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    Scaffold(modifier = modifier, topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_plan)) }) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Header(R.string.plan_weekly) }
            if (state.sheets.isEmpty()) item { Text(stringResource(R.string.plan_no_sheets), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(DayOfWeek.entries, key = { "w${it.value}" }) { day ->
                val dayName = day.getDisplayName(TextStyle.FULL, locale)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(dayName, style = MaterialTheme.typography.bodyLarge)
                    SheetPicker(dayName, state.weekly[day.value], state.sheets) { onSetWeekday(day.value, it) }
                }
            }
            item { Spacer(Modifier.height(8.dp)); Header(R.string.plan_next_days) }
            items(state.upcoming, key = { "d${it.date}" }) { plan ->
                DayRow(plan, state.today, names[plan.sheetId], locale) { moveFrom = plan.date }
            }
            item { Spacer(Modifier.height(8.dp)); Header(R.string.your_sheets) }
            items(state.sheets, key = { "s${it.id}" }) { sheet -> SheetRow(sheet) }
            item {
                OutlinedButton(onClick = { showCreate = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.new_sheet)) }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    moveFrom?.let { from ->
        MoveDialog(state.today, onDismiss = { moveFrom = null }, onPick = { onReschedule(from, it); moveFrom = null })
    }
    if (showCreate) CreateSheetDialog(
        onDismiss = { showCreate = false },
        onCreate = { onCreateSheet(it); showCreate = false },
        onCreateFromTemplate = { onCreateSheetFromTemplate(it); showCreate = false }
    )
}

@Composable
private fun Header(@StringRes res: Int) {
    Text(stringResource(res), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
}

@Composable
private fun SheetRow(sheet: TrainingSheet) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(sheet.name, style = MaterialTheme.typography.titleMedium)
            Text(pluralStringResource(R.plurals.exercises_count, sheet.exerciseCount, sheet.exerciseCount), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SheetPicker(dayName: String, selected: Long?, sheets: List<TrainingSheet>, onPick: (Long?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val label = sheets.firstOrNull { it.id == selected }?.name ?: stringResource(R.string.rest)
    val description = stringResource(R.string.plan_choose_sheet, dayName) + ": " + label
    Box {
        TextButton(onClick = { open = true }, modifier = Modifier.semantics { contentDescription = description }) {
            Text(label)
            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.rest)) }, onClick = { onPick(null); open = false })
            sheets.forEach { DropdownMenuItem(text = { Text(it.name) }, onClick = { onPick(it.id); open = false }) }
        }
    }
}

@Composable
private fun DayRow(plan: DayPlan, today: LocalDate, sheetName: String?, locale: Locale, onMove: () -> Unit) {
    val date = plan.date.format(DateTimeFormatter.ofPattern("EEE, d MMM", locale))
    val label = if (plan.date == today) stringResource(R.string.plan_today) + " · " + date else date
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(sheetName ?: stringResource(R.string.rest), style = MaterialTheme.typography.bodyLarge)
        }
        if (plan.moved) AssistChip(onClick = {}, label = { Text(stringResource(R.string.moved_badge)) }, modifier = Modifier.padding(end = 8.dp))
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
