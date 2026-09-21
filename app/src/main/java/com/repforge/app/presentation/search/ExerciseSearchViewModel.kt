package com.repforge.app.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.data.RemoteExercise
import com.repforge.app.data.WorkoutXClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchState(val query: String = "", val results: List<RemoteExercise> = emptyList(), val loading: Boolean = false, val error: String? = null)

class ExerciseSearchViewModel(private val client: WorkoutXClient) : ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()
    fun queryChanged(value: String) { _state.value = _state.value.copy(query = value) }
    fun search() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { client.search(_state.value.query) }
            .onSuccess { _state.value = _state.value.copy(results = it, loading = false) }
            .onFailure { _state.value = _state.value.copy(loading = false, error = it.message ?: "Falha na busca.") }
    }
}
