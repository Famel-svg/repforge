package com.repforge.app.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.data.CatalogExercise
import com.repforge.app.data.ExerciseCatalog
import com.repforge.app.domain.model.TrainingSheet
import com.repforge.app.domain.repository.WorkoutRepository
import com.repforge.app.presentation.logExceptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryState(
    val query: String = "", val muscle: String? = null, val equipment: String? = null,
    val results: List<CatalogExercise> = emptyList(), val loading: Boolean = true
)

class LibraryViewModel(private val catalog: ExerciseCatalog, private val repository: WorkoutRepository) : ViewModel() {
    private data class Filters(val query: String = "", val muscle: String? = null, val equipment: String? = null)

    private val filters = MutableStateFlow(Filters())
    private val items = MutableStateFlow<List<CatalogExercise>?>(null)

    init { viewModelScope.launch(logExceptions) { items.value = catalog.all() } }

    val state: StateFlow<LibraryState> = combine(filters, items) { f, all ->
        LibraryState(f.query, f.muscle, f.equipment, all?.let { ExerciseCatalog.search(it, f.query, f.muscle, f.equipment) }.orEmpty(), all == null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryState())

    val sheets: StateFlow<List<TrainingSheet>> =
        repository.observeSheets().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Adds [exercise] to [sheetId] with its inferred default kind. */
    fun addToSheet(sheetId: Long, exercise: CatalogExercise, onAdded: () -> Unit) {
        viewModelScope.launch(logExceptions) {
            repository.addExercise(sheetId, exercise.name, exercise.muscle, exercise.defaultKind)
            onAdded()
        }
    }

    fun queryChanged(value: String) = filters.update { it.copy(query = value) }
    fun muscleChanged(value: String?) = filters.update { it.copy(muscle = value) }
    fun equipmentChanged(value: String?) = filters.update { it.copy(equipment = value) }
}
