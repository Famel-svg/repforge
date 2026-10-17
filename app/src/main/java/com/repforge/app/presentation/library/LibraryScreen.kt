package com.repforge.app.presentation.library

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repforge.app.R
import com.repforge.app.RepForgeApplication
import com.repforge.app.data.CatalogExercise
import com.repforge.app.data.ExerciseCatalog
import com.repforge.app.presentation.components.EmptyState
import com.repforge.app.presentation.components.LargeTitleHeader
import com.repforge.app.presentation.components.RfCard
import com.repforge.app.presentation.components.SoftPillButton
import com.repforge.app.presentation.equipmentLabel
import com.repforge.app.presentation.isPortuguese
import com.repforge.app.presentation.muscleLabel
import com.repforge.app.presentation.plan.SheetPickerSheet
import com.repforge.app.presentation.pluralCount

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LibraryScreen(catalog: ExerciseCatalog, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = (context.applicationContext as RepForgeApplication).repository
    val viewModel = viewModel { LibraryViewModel(catalog, repository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheets by viewModel.sheets.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var addingId by rememberSaveable { mutableStateOf<String?>(null) }
    val subtitle = if (state.loading) stringResource(R.string.loading) else pluralCount(R.plurals.exercises_count, state.results.size)

    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LargeTitleHeader(stringResource(R.string.nav_library), subtitle)
        RfSearchField(state.query, viewModel::queryChanged)
        PillChipRow(MUSCLES, state.muscle, viewModel::muscleChanged) { muscleLabel(it) }
        PillChipRow(EQUIPMENT, state.equipment, viewModel::equipmentChanged) { equipmentLabel(it) }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!state.loading && state.results.isEmpty()) {
                item { EmptyState(Icons.Outlined.Search, stringResource(R.string.no_exercises_found), stringResource(R.string.t3_no_exercises_hint)) }
            }
            items(state.results, key = { it.id }) { exercise ->
                ExerciseCard(exercise, onOpen = { selected = exercise.id }, onAdd = { addingId = exercise.id })
            }
        }
    }

    state.results.firstOrNull { it.id == selected }?.let { exercise ->
        ModalBottomSheet(onDismissRequest = { selected = null }, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(primaryName(exercise), style = MaterialTheme.typography.headlineSmall)
                otherName(exercise)?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Detail(R.string.library_primary_muscle, muscleLabel(exercise.muscle))
                val secondary = exercise.secondary.map { muscleLabel(it) }
                Detail(R.string.library_secondary_muscles, if (secondary.isEmpty()) stringResource(R.string.library_none) else secondary.joinToString())
                Detail(R.string.library_equipment, equipmentLabel(exercise.equipment))
            }
        }
    }

    state.results.firstOrNull { it.id == addingId }?.let { exercise ->
        if (sheets.isEmpty()) {
            ModalBottomSheet(onDismissRequest = { addingId = null }, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(24.dp)) {
                    Text(stringResource(R.string.t3_no_sheets_library), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            SheetPickerSheet(
                title = stringResource(R.string.t3_choose_sheet_title), sheets = sheets, selectedId = null, includeRest = false,
                onDismiss = { addingId = null },
                onPick = { sheetId ->
                    val sheet = sheets.firstOrNull { it.id == sheetId }
                    addingId = null
                    if (sheetId != null) viewModel.addToSheet(sheetId, exercise) {
                        Toast.makeText(context, context.getString(R.string.t3_added_to, sheet?.name.orEmpty()), Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

@Composable
private fun ExerciseCard(exercise: CatalogExercise, onOpen: () -> Unit, onAdd: () -> Unit) {
    val name = primaryName(exercise)
    val addCd = stringResource(R.string.t3_add_to_sheet_cd, name)
    RfCard(Modifier.fillMaxWidth(), padding = 12.dp, onClick = onOpen) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(56.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.FitnessCenter, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                Text(
                    muscleLabel(exercise.muscle) + " · " + equipmentLabel(exercise.equipment),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1
                )
            }
            SoftPillButton(stringResource(R.string.t3_add_to_sheet), Icons.Outlined.Add, onAdd, Modifier.semantics { contentDescription = addCd })
        }
    }
}

@Composable
private fun Detail(label: Int, value: String) = Column {
    Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = MaterialTheme.typography.bodyLarge)
}

// Primary name follows the app language; the other stays as secondary text (search matches both).
@Composable
private fun primaryName(e: CatalogExercise) = if (!isPortuguese() && e.nameEn.isNotBlank()) e.nameEn else e.name

@Composable
private fun otherName(e: CatalogExercise) = (if (!isPortuguese() && e.nameEn.isNotBlank()) e.name else e.nameEn).takeIf { it.isNotBlank() }
