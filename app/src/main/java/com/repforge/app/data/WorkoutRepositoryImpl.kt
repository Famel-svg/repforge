package com.repforge.app.data

import com.repforge.app.data.local.SheetEntity
import com.repforge.app.data.local.EntryEntity
import com.repforge.app.data.local.ExerciseEntity
import com.repforge.app.data.local.WorkoutDao
import com.repforge.app.domain.model.DashboardSummary
import com.repforge.app.domain.model.TrainingSheet
import com.repforge.app.domain.model.Exercise
import com.repforge.app.domain.model.SetEntry
import com.repforge.app.domain.model.DailyVolume
import com.repforge.app.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import com.repforge.app.domain.validation.validateSetInput

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

    override suspend fun deleteSheet(sheetId: Long) {
        dao.deleteSheet(sheetId)
    }

    override fun observeExercises(sheetId: Long): Flow<List<Exercise>> = dao.observeExercises(sheetId).map { rows ->
        rows.map { Exercise(it.id, it.sheetId, it.name, it.target, it.position) }
    }

    override fun observeEntries(exerciseId: Long): Flow<List<SetEntry>> = dao.observeEntries(exerciseId).map { rows ->
        rows.map { SetEntry(it.id, it.exerciseId, it.sets, it.reps, it.weightKg, it.recordedAt) }
    }

    override suspend fun addExercise(sheetId: Long, name: String, target: String) {
        require(name.isNotBlank()) { "Nome do exercício não pode ficar vazio." }
        dao.insertExercise(ExerciseEntity(sheetId = sheetId, name = name.trim(), target = target.trim(), position = dao.nextExercisePosition(sheetId)))
    }

    override suspend fun deleteExercise(exerciseId: Long) {
        dao.deleteExercise(exerciseId)
    }

    override suspend fun updateExercise(exerciseId: Long, name: String, target: String) {
        require(name.isNotBlank()) { "Nome do exercício não pode ficar vazio." }
        dao.updateExercise(exerciseId, name.trim(), target.trim())
    }

    override suspend fun addEntry(exerciseId: Long, sets: Int, reps: Int, weightKg: Double) {
        require(validateSetInput(sets, reps, weightKg) == null) { validateSetInput(sets, reps, weightKg)?.message ?: "Valores de série inválidos." }
        dao.insertEntry(EntryEntity(exerciseId = exerciseId, sets = sets, reps = reps, weightKg = weightKg))
    }

    override fun observeDailyVolume(since: Long): Flow<List<DailyVolume>> = dao.observeDailyVolume(since).map {
        it.map { row -> DailyVolume(row.day, row.volumeKg) }
    }
}
