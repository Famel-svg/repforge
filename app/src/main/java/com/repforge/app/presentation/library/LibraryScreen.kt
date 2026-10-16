package com.repforge.app.presentation.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.repforge.app.presentation.pluralCount
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repforge.app.R
import com.repforge.app.data.CatalogExercise
import com.repforge.app.data.ExerciseCatalog
import com.repforge.app.presentation.equipmentLabel
import com.repforge.app.presentation.isPortuguese
import com.repforge.app.presentation.muscleLabel

private val MUSCLES = listOf(
    "Peito", "Costas", "Ombros", "Bíceps", "Tríceps", "Antebraço", "Abdômen", "Quadríceps",
    "Posterior de coxa", "Glúteos", "Panturrilha", "Trapézio", "Lombar", "Cardio", "Corpo inteiro"
)
private val EQUIPMENT = listOf("Barra", "Halteres", "Máquina", "Polia", "Peso corporal", "Kettlebell", "Elástico", "Smith", "Barra W", "Outros")
private val GUTTER = PaddingValues(horizontal = 20.dp)

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
fun LibraryScreen(catalog: ExerciseCatalog, modifier: Modifier = Modifier) {
    val viewModel = viewModel { LibraryViewModel(catalog) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    // Known muscles keep catalog order; unknown ones go last.
    val groups = state.results.groupBy { it.muscle }.toList().sortedBy { (m, _) -> MUSCLES.indexOf(m).let { if (it < 0) MUSCLES.size else it } }

    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            state.query, viewModel::queryChanged, Modifier.fillMaxWidth().padding(GUTTER),
            label = { Text(stringResource(R.string.search_hint)) }, singleLine = true
        )
        ChipRow(MUSCLES, state.muscle, viewModel::muscleChanged) { muscleLabel(it) }
        ChipRow(EQUIPMENT, state.equipment, viewModel::equipmentChanged) { equipmentLabel(it) }
        Text(
            if (state.loading) stringResource(R.string.loading) else pluralCount(R.plurals.exercises_count, state.results.size),
            Modifier.padding(GUTTER), color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
            if (!state.loading && state.results.isEmpty()) {
                item { Text(stringResource(R.string.no_exercises_found), Modifier.padding(GUTTER), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            groups.forEach { (muscle, exercises) ->
                stickyHeader(key = "h$muscle") {
                    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Text(
                            muscleLabel(muscle) + " · " + exercises.size, Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                items(exercises, key = { it.id }) { exercise ->
                    ListItem(
                        headlineContent = { Text(primaryName(exercise)) },
                        supportingContent = { Text(equipmentLabel(exercise.equipment)) },
                        modifier = Modifier.clickable { selected = exercise.id }
                    )
                }
            }
        }
    }

    state.results.firstOrNull { it.id == selected }?.let { exercise ->
        ModalBottomSheet(onDismissRequest = { selected = null }) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(primaryName(exercise), style = MaterialTheme.typography.headlineSmall)
                otherName(exercise)?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                HorizontalDivider()
                Detail(R.string.library_primary_muscle, muscleLabel(exercise.muscle))
                val secondary = exercise.secondary.map { muscleLabel(it) }
                Detail(R.string.library_secondary_muscles, if (secondary.isEmpty()) stringResource(R.string.library_none) else secondary.joinToString())
                Detail(R.string.library_equipment, equipmentLabel(exercise.equipment))
            }
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

@Composable
private fun ChipRow(options: List<String>, selected: String?, onSelect: (String?) -> Unit, label: @Composable (String) -> String) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = GUTTER) {
        items(options) { option ->
            FilterChip(selected == option, { onSelect(if (selected == option) null else option) }, label = { Text(label(option)) })
        }
    }
}
