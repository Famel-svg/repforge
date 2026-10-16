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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class WorkoutViewModel(private val repository: WorkoutRepository, private val sheetId: Long) : ViewModel() {
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()
    private val guard = CoroutineExceptionHandler { _, e ->
        android.util.Log.e("RepForge", "Falha em operação", e)
        _messages.trySend("Não foi possível concluir a ação.")
    }

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

    fun deleteEntry(entryId: Long) = viewModelScope.launch(guard) {
        repository.deleteEntry(entryId)
    }

    fun restoreEntry(entry: SetEntry) = viewModelScope.launch(guard) {
        repository.restoreEntry(entry)
    }

    fun moveExercise(exerciseId: Long, up: Boolean) = viewModelScope.launch(guard) {
        repository.moveExercise(exerciseId, up)
    }

    fun setSuperset(exerciseId: Long, join: Boolean) = viewModelScope.launch(guard) {
        repository.setSupersetWithNext(exerciseId, join)
    }

    fun addExercise(name: String, target: String) = viewModelScope.launch(guard) {
        repository.addExercise(sheetId, name, target)
    }

    fun deleteExercise(exerciseId: Long) = viewModelScope.launch(guard) {
        repository.deleteExercise(exerciseId)
    }

    fun updateExercise(exerciseId: Long, name: String, target: String) = viewModelScope.launch(guard) {
        repository.updateExercise(exerciseId, name, target)
    }

    fun addEntry(exerciseId: Long, sets: Int, reps: Int, weightKg: Double, setType: String, rpe: Double?) = viewModelScope.launch(guard) {
        repository.addEntry(exerciseId, sets, reps, weightKg, setType, rpe)
    }

    fun updateNote(exerciseId: Long, note: String) = viewModelScope.launch(guard) {
        repository.updateExerciseNote(exerciseId, note)
    }
}
