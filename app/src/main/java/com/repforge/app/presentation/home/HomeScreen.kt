package com.repforge.app.presentation.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.repforge.app.domain.model.TrainingSheet
import com.repforge.app.R
import androidx.compose.ui.res.stringResource
import com.repforge.app.presentation.pluralCount
import com.repforge.app.domain.logging.formatKg
import com.repforge.app.domain.templates.StarterTemplates
import com.repforge.app.domain.templates.WorkoutTemplate
import com.repforge.app.domain.validation.parseWeightKg
import com.repforge.app.domain.validation.validateBodyWeight
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.KeyboardType

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeScreen(
    state: HomeUiState,
    onCreateSheet: (String) -> Unit,
    /** Second argument is the body weight typed in the optional start prompt (null when skipped or the prompt is off). */
    onStartSheet: (TrainingSheet, Double?) -> Unit,
    onResumeSheet: (TrainingSheet) -> Unit,
    onDuplicateSheet: (Long, String) -> Unit,
    onDeleteSheet: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onCreateSheetFromTemplate: (WorkoutTemplate) -> Unit = {},
    /** One-shot: opens the create dialog on the template list (set by onboarding); [onTemplatePickerOpened] clears it. */
    openTemplatePicker: Boolean = false,
    onTemplatePickerOpened: () -> Unit = {},
    onOpenPlan: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    askBodyWeight: Boolean = false
) {
    var showCreate by remember { mutableStateOf(false) }
    var initialTemplateId by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<TrainingSheet?>(null) }
    var pendingStart by remember { mutableStateOf<TrainingSheet?>(null) }
    // Resuming the same sheet never asks again.
    fun start(sheet: TrainingSheet) { if (askBodyWeight && state.resume?.id != sheet.id) pendingStart = sheet else onStartSheet(sheet, null) }
    val copyLabel = stringResource(R.string.copy_suffix)
    LaunchedEffect(openTemplatePicker) {
        if (openTemplatePicker) {
            initialTemplateId = StarterTemplates.first().id
            showCreate = true
            onTemplatePickerOpened()
        }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) }, actions = {
                IconButton(onClick = onOpenSettings) { Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.nav_settings)) }
            })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { initialTemplateId = null; showCreate = true }) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.create_sheet))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.home_greeting), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                Text(stringResource(R.string.home_tagline), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.resume?.let { sheet -> item { ResumeCard(sheet.name, onResume = { onResumeSheet(sheet) }) } }
            state.today?.let { today ->
                item { TodayCard(today, onStart = { today.sheet?.let(::start) }, onOpenPlan = onOpenPlan) }
            }
            item { SummaryCard(state) }
            item {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.your_sheets), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                    OutlinedButton(onClick = { initialTemplateId = null; showCreate = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.new_sheet)) }
                }
            }
            if (state.sheets.isEmpty()) {
                item {
                    EmptySheetsCard(
                        onCreate = { initialTemplateId = null; showCreate = true },
                        onPickTemplate = { initialTemplateId = StarterTemplates.first().id; showCreate = true }
                    )
                }
            } else {
                items(state.sheets, key = { it.id }) { sheet ->
                    SheetCard(sheet, onStart = { start(sheet) }, onDuplicate = { onDuplicateSheet(sheet.id, copyLabel) }, onDelete = { deleteTarget = sheet })
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
    if (showCreate) CreateSheetDialog(
        onDismiss = { showCreate = false },
        onCreate = { name -> onCreateSheet(name); showCreate = false },
        onCreateFromTemplate = { template -> onCreateSheetFromTemplate(template); showCreate = false },
        initialTemplateId = initialTemplateId
    )
    pendingStart?.let { sheet ->
        BodyWeightPrompt(
            onSave = { kg -> pendingStart = null; onStartSheet(sheet, kg) },
            onSkip = { pendingStart = null; onStartSheet(sheet, null) },
            onDismiss = { pendingStart = null }
        )
    }
    deleteTarget?.let { sheet ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.delete_sheet_title)) },
            text = { Text(stringResource(R.string.delete_sheet_body, sheet.name)) },
            confirmButton = { Button(onClick = { onDeleteSheet(sheet.id); deleteTarget = null }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { OutlinedButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun ResumeCard(sheetName: String, onResume: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.workout_in_progress), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Text(sheetName, style = MaterialTheme.typography.headlineSmall)
            Button(onClick = onResume, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Text(stringResource(R.string.resume_workout))
            }
        }
    }
}

/** Optional: "Skip" starts the workout without a weight. */
@Composable
private fun BodyWeightPrompt(onSave: (Double) -> Unit, onSkip: () -> Unit, onDismiss: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    val kg = parseWeightKg(input)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.body_weight_prompt_title)) },
        text = {
            OutlinedTextField(
                input, { input = it }, label = { Text(stringResource(R.string.weight_kg)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        },
        confirmButton = { Button(enabled = validateBodyWeight(kg) == null, onClick = { onSave(kg!!) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onSkip) { Text(stringResource(R.string.skip)) } }
    )
}

@Composable
private fun TodayCard(today: TodayState, onStart: () -> Unit, onOpenPlan: () -> Unit) {
    Card {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.today_workout), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                if (today.moved) AssistChip(onClick = {}, label = { Text(stringResource(R.string.moved_badge)) })
            }
            when {
                today.sheet != null -> {
                    Text(today.sheet.name, style = MaterialTheme.typography.headlineSmall)
                    Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Text(stringResource(R.string.start_workout))
                    }
                }
                !today.configured -> {
                    Text(stringResource(R.string.set_plan_title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.set_plan_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onOpenPlan, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.open_plan)) }
                }
                else -> Text(stringResource(R.string.today_rest), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun SummaryCard(state: HomeUiState) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(stringResource(R.string.volume_last_7_days), color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        com.repforge.app.presentation.formatCount(
                            kotlin.math.round(state.summary.weeklyVolumeKg),
                            androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
                        ) + " kg",
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
                Icon(Icons.Rounded.AutoGraph, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                pluralCount(R.plurals.sets_count, state.summary.completedSets) + " · " +
                    pluralCount(R.plurals.active_sheets_count, state.summary.activeSheets)
            )
        }
    }
}

@Composable
private fun SheetCard(sheet: TrainingSheet, onStart: () -> Unit, onDuplicate: () -> Unit, onDelete: () -> Unit) {
    Card {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                Text(sheet.name, style = MaterialTheme.typography.titleMedium)
                Text(pluralCount(R.plurals.exercises_count, sheet.exerciseCount), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Text(stringResource(R.string.start_workout))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDuplicate, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.duplicate)) }
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.delete)) }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun EmptySheetsCard(onCreate: () -> Unit, onPickTemplate: () -> Unit) {
    Card {
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            Icon(Icons.Rounded.FitnessCenter, contentDescription = null)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.no_sheets_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Text(stringResource(R.string.no_sheets_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCreate) { Text(stringResource(R.string.create_first_sheet)) }
                OutlinedButton(onClick = onPickTemplate) { Text(stringResource(R.string.start_with_template)) }
            }
        }
    }
}
