package com.repforge.app.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.presentation.logExceptions
import com.repforge.app.data.CatalogExercise
import com.repforge.app.data.ExerciseCatalog
import com.repforge.app.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchState(
    val query: String = "",
    val muscle: String? = null,
    val equipment: String? = null,
    val results: List<CatalogExercise> = emptyList(),
    val added: Set<String> = emptySet(),
    val loading: Boolean = true
)

class ExerciseSearchViewModel(
    private val catalog: ExerciseCatalog,
    private val repository: WorkoutRepository,
    private val sheetId: Long
) : ViewModel() {
    private data class Filters(val query: String = "", val muscle: String? = null, val equipment: String? = null)

    private val filters = MutableStateFlow(Filters())
    private val items = MutableStateFlow<List<CatalogExercise>?>(null)

    init { viewModelScope.launch(logExceptions) { items.value = catalog.all() } }

    val state: StateFlow<SearchState> = combine(filters, items, repository.observeExercises(sheetId)) { filter, all, exercises ->
        SearchState(
            filter.query, filter.muscle, filter.equipment,
            all?.let { ExerciseCatalog.search(it, filter.query, filter.muscle, filter.equipment) }.orEmpty(),
            exercises.map { ExerciseCatalog.normalize(it.name) }.toSet(),
            all == null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchState())

    fun queryChanged(value: String) = filters.update { it.copy(query = value) }
    fun muscleChanged(value: String?) = filters.update { it.copy(muscle = value) }
    fun equipmentChanged(value: String?) = filters.update { it.copy(equipment = value) }

    // Ignores taps while a previous add is still running (a double tap would add the exercise twice).
    private var adding = false

    private fun addOnce(name: String, muscle: String, onAdded: () -> Unit) {
        if (adding) return
        adding = true
        viewModelScope.launch(logExceptions) {
            try {
                repository.addExercise(sheetId, name, muscle)
                onAdded()
            } finally {
                adding = false
            }
        }
    }

    fun add(exercise: CatalogExercise, onAdded: () -> Unit) = addOnce(exercise.name, exercise.muscle, onAdded)

    fun addCustom(onAdded: () -> Unit) {
        val name = filters.value.query.trim()
        if (name.isNotEmpty()) addOnce(name, filters.value.muscle.orEmpty(), onAdded)
    }
}
