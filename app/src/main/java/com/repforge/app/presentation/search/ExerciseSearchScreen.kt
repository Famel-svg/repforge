package com.repforge.app.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import com.repforge.app.presentation.components.CircleIconButton
import com.repforge.app.presentation.components.LargeTitleHeader
import com.repforge.app.presentation.components.RfCard
import com.repforge.app.presentation.components.SoftPillButton
import com.repforge.app.presentation.components.Tag
import com.repforge.app.presentation.components.TagTone
import com.repforge.app.presentation.library.EQUIPMENT
import com.repforge.app.presentation.library.GUTTER
import com.repforge.app.presentation.library.MUSCLES
import com.repforge.app.presentation.library.PillChipRow
import com.repforge.app.presentation.library.RfSearchField
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.repforge.app.presentation.pluralCount
import androidx.compose.ui.res.stringResource
import com.repforge.app.R
import com.repforge.app.data.ExerciseCatalog
import com.repforge.app.presentation.equipmentLabel
import com.repforge.app.presentation.isPortuguese
import com.repforge.app.presentation.muscleLabel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ExerciseSearchScreen(viewModel: ExerciseSearchViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LargeTitleHeader(
                stringResource(R.string.search_exercise),
                if (state.loading) stringResource(R.string.loading) else pluralCount(R.plurals.exercises_count, state.results.size),
                action = { CircleIconButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back), onBack) }
            )
            RfSearchField(state.query, viewModel::queryChanged)
            PillChipRow(MUSCLES, state.muscle, viewModel::muscleChanged) { muscleLabel(it) }
            PillChipRow(EQUIPMENT, state.equipment, viewModel::equipmentChanged) { equipmentLabel(it) }
            LazyColumn(Modifier.weight(1f), contentPadding = GUTTER, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!state.loading && state.results.isEmpty()) {
                    item { Text(stringResource(R.string.no_exercises_found), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(state.results, key = { it.id }) { exercise ->
                    val added = ExerciseCatalog.normalize(exercise.name) in state.added
                    RfCard(Modifier.fillMaxWidth(), padding = 12.dp) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                // Primary name follows the app language; the other one stays as a subtitle (search matches both).
                                val english = !isPortuguese() && exercise.nameEn.isNotBlank()
                                Text(if (english) exercise.nameEn else exercise.name, style = MaterialTheme.typography.titleMedium)
                                val other = if (english) exercise.name else exercise.nameEn
                                if (other.isNotBlank()) Text(other, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(muscleLabel(exercise.muscle) + " · " + equipmentLabel(exercise.equipment), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (added) Tag(stringResource(R.string.added), TagTone.Accent)
                            else SoftPillButton(stringResource(R.string.add), Icons.Outlined.Add, { viewModel.add(exercise, onBack) })
                        }
                    }
                }
            }
            SoftPillButton(
                if (state.query.isBlank()) stringResource(R.string.create_custom_exercise) else stringResource(R.string.create_named, state.query.trim()),
                icon = null, onClick = { viewModel.addCustom(onBack) }, enabled = state.query.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(GUTTER).alpha(if (state.query.isBlank()) 0.5f else 1f)
            )
        }
    }
}
