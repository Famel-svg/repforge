package com.repforge.app.domain.repository

import com.repforge.app.domain.model.DashboardSummary
import com.repforge.app.domain.model.TrainingSheet
import com.repforge.app.domain.model.Exercise
import com.repforge.app.domain.model.SetEntry
import com.repforge.app.domain.model.DailyVolume
import com.repforge.app.domain.model.HistoryEntry
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun observeSheets(): Flow<List<TrainingSheet>>
    fun observeSheet(sheetId: Long): Flow<TrainingSheet?>
    fun observeSummary(): Flow<DashboardSummary>
    suspend fun createSheet(name: String)
    suspend fun duplicateSheet(sheetId: Long)
    suspend fun deleteSheet(sheetId: Long)
    fun observeExercises(sheetId: Long): Flow<List<Exercise>>
    fun observeEntries(exerciseId: Long): Flow<List<SetEntry>>
    suspend fun addExercise(sheetId: Long, name: String, target: String)
    suspend fun updateExercise(exerciseId: Long, name: String, target: String)
    suspend fun deleteExercise(exerciseId: Long)
    suspend fun addEntry(exerciseId: Long, sets: Int, reps: Int, weightKg: Double)
    fun observeDailyVolume(): Flow<List<DailyVolume>>
    fun observeHistory(): Flow<List<HistoryEntry>>
}
