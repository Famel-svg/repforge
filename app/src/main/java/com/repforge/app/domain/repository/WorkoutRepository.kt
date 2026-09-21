package com.repforge.app.domain.repository

import com.repforge.app.domain.model.DashboardSummary
import com.repforge.app.domain.model.TrainingSheet
import com.repforge.app.domain.model.Exercise
import com.repforge.app.domain.model.SetEntry
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun observeSheets(): Flow<List<TrainingSheet>>
    fun observeSummary(): Flow<DashboardSummary>
    suspend fun createSheet(name: String)
    fun observeExercises(sheetId: Long): Flow<List<Exercise>>
    fun observeEntries(exerciseId: Long): Flow<List<SetEntry>>
    suspend fun addExercise(sheetId: Long, name: String, target: String)
    suspend fun addEntry(exerciseId: Long, sets: Int, reps: Int, weightKg: Double)
}
