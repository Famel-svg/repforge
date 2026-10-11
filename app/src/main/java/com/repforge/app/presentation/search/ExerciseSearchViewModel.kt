package com.repforge.app.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.data.RemoteExercise
import com.repforge.app.data.WorkoutXClient
import com.repforge.app.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchState(
    val query: String = "",
    val bodyPart: String? = null,
    val equipment: String? = null,
    val results: List<RemoteExercise> = emptyList(),
    val searched: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null
)

class ExerciseSearchViewModel(
    private val client: WorkoutXClient,
    private val repository: WorkoutRepository,
    private val sheetId: Long
) : ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    fun queryChanged(value: String) = _state.update { it.copy(query = value) }
    fun bodyPartChanged(value: String?) = _state.update { it.copy(bodyPart = value) }
    fun equipmentChanged(value: String?) = _state.update { it.copy(equipment = value) }

    fun search() = viewModelScope.launch {
        val current = _state.value
        _state.update { it.copy(loading = true, error = null) }
        runCatching {
            client.search(current.query, current.bodyPart?.let(WorkoutXClient.BODY_PARTS::get), current.equipment?.let(WorkoutXClient.EQUIPMENT::get))
        }
            .onSuccess { results -> _state.update { it.copy(results = results, searched = true, loading = false) } }
            .onFailure { error -> _state.update { it.copy(loading = false, error = error.message ?: "Falha na busca.") } }
    }

    fun add(exercise: RemoteExercise, onAdded: () -> Unit) = viewModelScope.launch {
        repository.addExercise(sheetId, exercise.name, exercise.target)
        onAdded()
    }
}
