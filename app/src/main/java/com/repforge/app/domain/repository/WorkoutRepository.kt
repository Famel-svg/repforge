package com.repforge.app.domain.repository

import com.repforge.app.domain.model.DashboardSummary
import com.repforge.app.domain.model.TrainingSheet
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun observeSheets(): Flow<List<TrainingSheet>>
    fun observeSummary(): Flow<DashboardSummary>
    suspend fun createSheet(name: String)
}
