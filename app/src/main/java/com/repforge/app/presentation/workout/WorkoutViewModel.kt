package com.repforge.app.presentation.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.domain.model.Exercise
import com.repforge.app.domain.model.SetEntry
import com.repforge.app.domain.repository.WorkoutRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutViewModel(private val repository: WorkoutRepository, private val sheetId: Long) : ViewModel() {
    val sheetName: StateFlow<String> = repository.observeSheet(sheetId).map { it?.name.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val exercises: StateFlow<List<Exercise>> = repository.observeExercises(sheetId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Null until every exercise has emitted its entries. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val entriesByExercise: StateFlow<Map<Long, List<SetEntry>>?> = exercises.flatMapLatest { list ->
        if (list.isEmpty()) flowOf(emptyMap<Long, List<SetEntry>>())
        else combine(list.map { ex -> repository.observeEntries(ex.id).map { ex.id to it } }) { it.toMap() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun entries(exerciseId: Long): Flow<List<SetEntry>> = repository.observeEntries(exerciseId)

    fun deleteEntry(entryId: Long) = viewModelScope.launch {
        repository.deleteEntry(entryId)
    }

    fun restoreEntry(entry: SetEntry) = viewModelScope.launch {
        repository.restoreEntry(entry)
    }

    fun moveExercise(exerciseId: Long, up: Boolean) = viewModelScope.launch {
        repository.moveExercise(exerciseId, up)
    }

    fun setSuperset(exerciseId: Long, join: Boolean) = viewModelScope.launch {
        repository.setSupersetWithNext(exerciseId, join)
    }

    fun addExercise(name: String, target: String) = viewModelScope.launch {
        repository.addExercise(sheetId, name, target)
    }

    fun deleteExercise(exerciseId: Long) = viewModelScope.launch {
        repository.deleteExercise(exerciseId)
    }

    fun updateExercise(exerciseId: Long, name: String, target: String) = viewModelScope.launch {
        repository.updateExercise(exerciseId, name, target)
    }

    fun addEntry(exerciseId: Long, sets: Int, reps: Int, weightKg: Double, setType: String, rpe: Double?) = viewModelScope.launch {
        repository.addEntry(exerciseId, sets, reps, weightKg, setType, rpe)
    }

    fun updateNote(exerciseId: Long, note: String) = viewModelScope.launch {
        repository.updateExerciseNote(exerciseId, note)
    }
}
