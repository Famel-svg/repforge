package com.repforge.app.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.repforge.app.data.ExerciseCatalog

private val MUSCLES = listOf(
    "Peito", "Costas", "Ombros", "Bíceps", "Tríceps", "Antebraço", "Abdômen", "Quadríceps",
    "Posterior de coxa", "Glúteos", "Panturrilha", "Trapézio", "Lombar", "Cardio", "Corpo inteiro"
)
private val EQUIPMENT = listOf("Barra", "Halteres", "Máquina", "Polia", "Peso corporal", "Kettlebell", "Elástico", "Smith", "Barra W", "Outros")
private val GUTTER = PaddingValues(horizontal = 20.dp)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ExerciseSearchScreen(viewModel: ExerciseSearchViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text("Buscar exercício") }, navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Voltar") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                state.query, viewModel::queryChanged, Modifier.fillMaxWidth().padding(GUTTER),
                label = { Text("Nome, músculo ou equipamento") }, singleLine = true
            )
            ChipRow(MUSCLES, state.muscle, viewModel::muscleChanged)
            ChipRow(EQUIPMENT, state.equipment, viewModel::equipmentChanged)
            Text(
                if (state.loading) "Carregando…" else "${state.results.size} exercícios",
                Modifier.padding(GUTTER), color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyColumn(Modifier.weight(1f), contentPadding = GUTTER, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!state.loading && state.results.isEmpty()) {
                    item { Text("Nenhum exercício encontrado.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(state.results, key = { it.id }) { exercise ->
                    val added = ExerciseCatalog.normalize(exercise.name) in state.added
                    Card {
                        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                                if (exercise.nameEn.isNotBlank()) Text(exercise.nameEn, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${exercise.muscle} · ${exercise.equipment}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(onClick = { viewModel.add(exercise, onBack) }, enabled = !added) { Text(if (added) "Adicionado" else "Adicionar") }
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = { viewModel.addCustom(onBack) }, enabled = state.query.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(GUTTER)
            ) { Text(if (state.query.isBlank()) "Criar exercício personalizado" else "Criar \"${state.query.trim()}\"") }
        }
    }
}

@Composable
private fun ChipRow(options: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = GUTTER) {
        items(options) { option ->
            FilterChip(selected == option, { onSelect(if (selected == option) null else option) }, label = { Text(option) })
        }
    }
}
