package com.repforge.app.data

import com.repforge.app.data.local.SheetEntity
import com.repforge.app.data.local.WorkoutDao
import com.repforge.app.domain.model.DashboardSummary
import com.repforge.app.domain.model.TrainingSheet
import com.repforge.app.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class WorkoutRepositoryImpl(private val dao: WorkoutDao) : WorkoutRepository {
    private val weekStart: Long
        get() = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)

    override fun observeSheets(): Flow<List<TrainingSheet>> = dao.observeSheets().map { sheets ->
        sheets.map { TrainingSheet(it.id, it.name, it.exerciseCount, it.updatedAt) }
    }

    override fun observeSummary(): Flow<DashboardSummary> = combine(
        dao.observeWeeklyVolume(weekStart),
        dao.observeWeeklySets(weekStart),
        dao.observeSheetCount()
    ) { volume, sets, sheetCount -> DashboardSummary(volume, sets, sheetCount) }

    override suspend fun createSheet(name: String) {
        require(name.isNotBlank()) { "Nome da ficha não pode ficar vazio." }
        dao.insertSheet(SheetEntity(name = name.trim()))
    }
}
