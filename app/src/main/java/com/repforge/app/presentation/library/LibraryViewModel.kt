package com.repforge.app.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.data.CatalogExercise
import com.repforge.app.data.ExerciseCatalog
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

class LibraryViewModel(private val catalog: ExerciseCatalog) : ViewModel() {
    private data class Filters(val query: String = "", val muscle: String? = null, val equipment: String? = null)

    private val filters = MutableStateFlow(Filters())
    private val items = MutableStateFlow<List<CatalogExercise>?>(null)

    init { viewModelScope.launch(logExceptions) { items.value = catalog.all() } }

    val state: StateFlow<LibraryState> = combine(filters, items) { f, all ->
        LibraryState(f.query, f.muscle, f.equipment, all?.let { ExerciseCatalog.search(it, f.query, f.muscle, f.equipment) }.orEmpty(), all == null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryState())

    fun queryChanged(value: String) = filters.update { it.copy(query = value) }
    fun muscleChanged(value: String?) = filters.update { it.copy(muscle = value) }
    fun equipmentChanged(value: String?) = filters.update { it.copy(equipment = value) }
}
