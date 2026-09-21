package com.repforge.app.presentation.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.domain.model.Exercise
import com.repforge.app.domain.model.SetEntry
import com.repforge.app.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class WorkoutViewModel(private val repository: WorkoutRepository) : ViewModel() {
    fun exercises(sheetId: Long): Flow<List<Exercise>> = repository.observeExercises(sheetId)
    fun entries(exerciseId: Long): Flow<List<SetEntry>> = repository.observeEntries(exerciseId)

    fun addExercise(sheetId: Long, name: String, target: String) = viewModelScope.launch {
        repository.addExercise(sheetId, name, target)
    }

    fun deleteExercise(exerciseId: Long) = viewModelScope.launch {
        repository.deleteExercise(exerciseId)
    }

    fun addEntry(exerciseId: Long, sets: Int, reps: Int, weightKg: Double) = viewModelScope.launch {
        repository.addEntry(exerciseId, sets, reps, weightKg)
    }
}
