package com.repforge.app.presentation.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.R
import com.repforge.app.domain.model.Exercise
import com.repforge.app.domain.model.SetEntry
import com.repforge.app.domain.model.WorkoutSession
import com.repforge.app.domain.progression.RULE_DOUBLE
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

/** Wrapper so "still loading" (null outer) differs from "no workout running on this sheet". */
data class SheetSession(val session: WorkoutSession?)

class WorkoutViewModel(private val repository: WorkoutRepository, private val sheetId: Long) : ViewModel() {
    private val _messages = Channel<Int>(Channel.BUFFERED)
    /** String resource ids; resolved by the screen. */
    val messages: Flow<Int> = _messages.receiveAsFlow()
    private val guard = CoroutineExceptionHandler { _, e ->
        android.util.Log.e("RepForge", "Falha em operação", e)
        _messages.trySend(R.string.action_failed)
    }

    val sheetName: StateFlow<String> = repository.observeSheet(sheetId).map { it?.name.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val sheetRule: StateFlow<String> = repository.observeSheet(sheetId).map { it?.progressionRule ?: RULE_DOUBLE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RULE_DOUBLE)
    /** The running workout on this sheet; its startedAt comes from the database, so the clock survives rotation and process death. */
    val session: StateFlow<SheetSession?> = repository.observeActiveSession().map { SheetSession(it?.takeIf { s -> s.sheetId == sheetId }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
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

    fun addExercise(name: String, target: String, kind: String, perSide: Boolean) = viewModelScope.launch(guard) {
        repository.addExercise(sheetId, name, target, kind, perSide)
    }

    fun deleteExercise(exerciseId: Long) = viewModelScope.launch(guard) {
        repository.deleteExercise(exerciseId)
    }

    fun updateExercise(exerciseId: Long, name: String, target: String, kind: String, perSide: Boolean, rule: String?) = viewModelScope.launch(guard) {
        repository.updateExercise(exerciseId, name, target, kind, perSide)
        repository.setExerciseRule(exerciseId, rule)
    }

    fun setSheetRule(rule: String) = viewModelScope.launch(guard) { repository.setSheetRule(sheetId, rule) }

    fun startSession() = viewModelScope.launch(guard) { repository.startSession(sheetId) }

    fun finishSession(sessionId: Long, onDone: (Long) -> Unit) = viewModelScope.launch(guard) {
        repository.finishSession(sessionId)
        onDone(sessionId)
    }

    fun discardSession(sessionId: Long, onDone: () -> Unit) = viewModelScope.launch(guard) {
        repository.discardSession(sessionId)
        onDone()
    }

    fun addEntry(exerciseId: Long, sets: Int, reps: Int, weightKg: Double, setType: String, rpe: Double?, durationSec: Int? = null, distanceKm: Double? = null) = viewModelScope.launch(guard) {
        repository.addEntry(exerciseId, sets, reps, weightKg, setType, rpe, durationSec, distanceKm)
    }

    fun updateNote(exerciseId: Long, note: String) = viewModelScope.launch(guard) {
        repository.updateExerciseNote(exerciseId, note)
    }
}
