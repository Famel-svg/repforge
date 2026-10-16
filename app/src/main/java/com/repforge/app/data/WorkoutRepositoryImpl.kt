package com.repforge.app.data

import androidx.room.withTransaction
import com.repforge.app.data.local.BodyWeightEntity
import com.repforge.app.data.local.EntryEntity
import com.repforge.app.data.local.ExerciseEntity
import com.repforge.app.data.local.SheetEntity
import com.repforge.app.data.local.SheetWithCount
import com.repforge.app.data.local.WorkoutDatabase
import com.repforge.app.domain.logging.joinWithNext
import com.repforge.app.domain.logging.movedOrder
import com.repforge.app.domain.logging.normalizeGroups
import com.repforge.app.domain.logging.separateAt
import com.repforge.app.domain.metrics.currentStreak
import com.repforge.app.domain.metrics.recentWindowStart
import com.repforge.app.domain.metrics.trainingDays
import com.repforge.app.domain.model.BodyWeight
import com.repforge.app.domain.model.DailyVolume
import com.repforge.app.domain.model.DashboardSummary
import com.repforge.app.domain.model.Exercise
import com.repforge.app.domain.model.HistoryEntry
import com.repforge.app.domain.model.SET_NORMAL
import com.repforge.app.domain.model.SET_WARMUP
import com.repforge.app.domain.model.SetEntry
import com.repforge.app.domain.model.TrainingSheet
import com.repforge.app.domain.repository.WorkoutRepository
import com.repforge.app.domain.templates.WorkoutTemplate
import com.repforge.app.domain.validation.validateBodyWeight
import com.repforge.app.domain.validation.validateSetInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId

class WorkoutRepositoryImpl(private val database: WorkoutDatabase) : WorkoutRepository {
    private val dao = database.workoutDao()

    override fun observeSheets(): Flow<List<TrainingSheet>> = dao.observeSheets().map { sheets -> sheets.map { it.toDomain() } }

    override fun observeSheet(sheetId: Long): Flow<TrainingSheet?> = dao.observeSheet(sheetId).map { it?.toDomain() }

    // The window start is resolved on each collection so a long-lived process does not keep a stale week.
    override fun observeSummary(): Flow<DashboardSummary> = flow {
        val since = recentWindowStart()
        val zone = ZoneId.systemDefault()
        val today = Instant.now().atZone(zone).toLocalDate()
        emitAll(combine(dao.observeWeeklyVolume(since), dao.observeWeeklySets(since), dao.observeSheetCount(), dao.observeHistory()) { volume, sets, sheetCount, history ->
            DashboardSummary(volume, sets, sheetCount, currentStreak(trainingDays(history.filter { it.setType != SET_WARMUP }.map { it.recordedAt }, zone), today))
        })
    }

    override suspend fun createSheet(name: String) {
        require(name.isNotBlank()) { "Nome da ficha não pode ficar vazio." }
        dao.insertSheet(SheetEntity(name = name.trim()))
    }

    override suspend fun duplicateSheet(sheetId: Long) {
        database.withTransaction {
            val source = dao.findSheet(sheetId) ?: return@withTransaction
            val copyId = dao.insertSheet(SheetEntity(name = "${source.name} (cópia)"))
            dao.exercisesOf(sheetId).forEach { dao.insertExercise(it.copy(id = 0, sheetId = copyId)) }
        }
    }

    override suspend fun deleteSheet(sheetId: Long) {
        dao.deleteSheet(sheetId)
    }

    override fun observeExercises(sheetId: Long): Flow<List<Exercise>> = dao.observeExercises(sheetId).map { rows ->
        rows.map { Exercise(it.id, it.sheetId, it.name, it.target, it.position, it.note, it.supersetGroup) }
    }

    override fun observeEntries(exerciseId: Long): Flow<List<SetEntry>> = dao.observeEntries(exerciseId).map { rows ->
        rows.map { SetEntry(it.id, it.exerciseId, it.sets, it.reps, it.weightKg, it.recordedAt, it.setType, it.rpe) }
    }

    override suspend fun addExercise(sheetId: Long, name: String, target: String) {
        require(name.isNotBlank()) { "Nome do exercício não pode ficar vazio." }
        database.withTransaction {
            dao.insertExercise(ExerciseEntity(sheetId = sheetId, name = name.trim(), target = target.trim(), position = dao.nextExercisePosition(sheetId)))
            dao.touchSheet(sheetId, System.currentTimeMillis())
        }
    }

    override suspend fun deleteExercise(exerciseId: Long) {
        database.withTransaction {
            val sheetId = dao.findExercise(exerciseId)?.sheetId
            dao.touchSheetOfExercise(exerciseId, System.currentTimeMillis())
            dao.deleteExercise(exerciseId)
            if (sheetId != null) regroup(sheetId) { normalizeGroups(it.map { e -> e.supersetGroup }) }
        }
    }

    override suspend fun updateExercise(exerciseId: Long, name: String, target: String) {
        require(name.isNotBlank()) { "Nome do exercício não pode ficar vazio." }
        database.withTransaction {
            dao.updateExercise(exerciseId, name.trim(), target.trim())
            dao.touchSheetOfExercise(exerciseId, System.currentTimeMillis())
        }
    }

    override suspend fun addEntry(exerciseId: Long, sets: Int, reps: Int, weightKg: Double, setType: String, rpe: Double?) {
        validateSetInput(sets, reps, weightKg)?.let { throw IllegalArgumentException(it.message) }
        database.withTransaction {
            dao.insertEntry(EntryEntity(exerciseId = exerciseId, sets = sets, reps = reps, weightKg = weightKg, setType = setType, rpe = rpe))
            dao.touchSheetOfExercise(exerciseId, System.currentTimeMillis())
        }
    }

    override suspend fun updateExerciseNote(exerciseId: Long, note: String) {
        dao.updateExerciseNote(exerciseId, note.trim())
    }

    override suspend fun deleteEntry(entryId: Long) {
        dao.deleteEntry(entryId)
    }

    override suspend fun restoreEntry(entry: SetEntry) {
        dao.insertEntry(EntryEntity(entry.id, entry.exerciseId, entry.sets, entry.reps, entry.weightKg, entry.recordedAt, entry.setType, entry.rpe))
    }

    override suspend fun moveExercise(exerciseId: Long, up: Boolean) {
        database.withTransaction {
            val sheetId = dao.findExercise(exerciseId)?.sheetId ?: return@withTransaction
            val ordered = dao.exercisesOf(sheetId)
            val newOrder = movedOrder(ordered.map { it.id }, exerciseId, up) ?: return@withTransaction
            val oldIndex = ordered.map { it.id }.withIndex().associate { it.value to it.index }
            newOrder.forEachIndexed { index, id -> if (oldIndex[id] != index) dao.updateExercisePosition(id, index) }
            regroup(sheetId) { normalizeGroups(it.map { e -> e.supersetGroup }) }
            dao.touchSheet(sheetId, System.currentTimeMillis())
        }
    }

    override suspend fun setSupersetWithNext(exerciseId: Long, join: Boolean) {
        database.withTransaction {
            val sheetId = dao.findExercise(exerciseId)?.sheetId ?: return@withTransaction
            regroup(sheetId) { list ->
                val groups = list.map { it.supersetGroup }
                val index = list.indexOfFirst { it.id == exerciseId }
                if (join) joinWithNext(groups, index) else separateAt(groups, index)
            }
            dao.touchSheet(sheetId, System.currentTimeMillis())
        }
    }

    /** Persists only the groups that changed; [transform] gets the sheet's exercises in display order. */
    private suspend fun regroup(sheetId: Long, transform: (List<ExerciseEntity>) -> List<Int?>) {
        val ordered = dao.exercisesOf(sheetId)
        val after = transform(ordered)
        ordered.forEachIndexed { i, e -> if (after[i] != e.supersetGroup) dao.updateSupersetGroup(e.id, after[i]) }
    }

    override fun observeBodyWeights(): Flow<List<BodyWeight>> = dao.observeBodyWeights().map { rows -> rows.map { BodyWeight(it.id, it.weightKg, it.recordedAt) } }

    override suspend fun addBodyWeight(weightKg: Double) {
        validateBodyWeight(weightKg)?.let { throw IllegalArgumentException(it.message) }
        dao.insertBodyWeight(BodyWeightEntity(weightKg = weightKg))
    }

    override suspend fun deleteBodyWeight(id: Long) {
        dao.deleteBodyWeight(id)
    }

    override suspend fun createSheetFromTemplate(template: WorkoutTemplate) {
        require(template.sheetName.isNotBlank()) { "Nome da ficha não pode ficar vazio." }
        database.withTransaction {
            val sheetId = dao.insertSheet(SheetEntity(name = template.sheetName.trim()))
            template.exercises.forEachIndexed { index, e ->
                dao.insertExercise(ExerciseEntity(sheetId = sheetId, name = e.name, target = e.target, position = index))
            }
        }
    }

    override fun observeDailyVolume(): Flow<List<DailyVolume>> = flow {
        emitAll(dao.observeDailyVolume(recentWindowStart()).map { rows -> rows.map { DailyVolume(it.day, it.volumeKg) } })
    }

    override fun observeHistory(): Flow<List<HistoryEntry>> = dao.observeHistory().map { rows ->
        rows.map { HistoryEntry(it.exerciseId, it.exerciseName, it.sheetName, it.sets, it.reps, it.weightKg, it.recordedAt, it.setType, it.rpe) }
    }

    private fun SheetWithCount.toDomain() = TrainingSheet(id, name, exerciseCount, updatedAt)
}
