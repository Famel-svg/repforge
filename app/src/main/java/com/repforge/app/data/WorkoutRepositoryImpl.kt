package com.repforge.app.data

import androidx.room.withTransaction
import com.repforge.app.data.local.BodyWeightEntity
import com.repforge.app.data.local.EntryEntity
import com.repforge.app.data.local.ExerciseEntity
import com.repforge.app.data.local.ScheduleEntity
import com.repforge.app.data.local.ScheduleOverrideEntity
import com.repforge.app.data.local.SheetEntity
import com.repforge.app.data.local.SessionEntity
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
import com.repforge.app.domain.model.normalizeKind
import com.repforge.app.domain.model.SET_WARMUP
import com.repforge.app.domain.model.SetEntry
import com.repforge.app.domain.model.TrainingSheet
import com.repforge.app.domain.model.WorkoutSession
import com.repforge.app.domain.metrics.FinishedSession
import com.repforge.app.domain.progression.PROGRESSION_RULES
import com.repforge.app.domain.repository.WorkoutRepository
import com.repforge.app.domain.schedule.DayPlan
import com.repforge.app.domain.schedule.rescheduleChanges
import com.repforge.app.domain.schedule.resolveDay
import com.repforge.app.domain.templates.WorkoutTemplate
import com.repforge.app.domain.validation.AppError
import com.repforge.app.domain.validation.AppException
import com.repforge.app.domain.validation.validateBodyWeight
import com.repforge.app.domain.validation.validateEffort
import com.repforge.app.domain.validation.validateSetInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** [onChanged] fires after writes the home-screen widget displays (sheets, logged sets). */
class WorkoutRepositoryImpl(private val database: WorkoutDatabase, private val onChanged: () -> Unit = {}) : WorkoutRepository {
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
        onChanged()
    }

    override suspend fun duplicateSheet(sheetId: Long, copyLabel: String) {
        database.withTransaction {
            val source = dao.findSheet(sheetId) ?: return@withTransaction
            val copyId = dao.insertSheet(SheetEntity(name = "${source.name} ($copyLabel)", progressionRule = source.progressionRule))
            dao.exercisesOf(sheetId).forEach { dao.insertExercise(it.copy(id = 0, sheetId = copyId)) }
        }
        onChanged()
    }

    override suspend fun deleteSheet(sheetId: Long) {
        database.withTransaction {
            dao.deleteActiveSessionsOf(sheetId)
            dao.deleteSheet(sheetId)
        }
        onChanged()
    }

    override fun observeExercises(sheetId: Long): Flow<List<Exercise>> = dao.observeExercises(sheetId).map { rows ->
        rows.map { Exercise(it.id, it.sheetId, it.name, it.target, it.position, it.note, it.supersetGroup, it.kind, it.perSide, it.progressionRule) }
    }

    override fun observeEntries(exerciseId: Long): Flow<List<SetEntry>> = dao.observeEntries(exerciseId).map { rows ->
        rows.map { SetEntry(it.id, it.exerciseId, it.sets, it.reps, it.weightKg, it.recordedAt, it.setType, it.rpe, it.durationSec, it.distanceKm) }
    }

    override suspend fun addExercise(sheetId: Long, name: String, target: String, kind: String, perSide: Boolean) {
        require(name.isNotBlank()) { "Nome do exercício não pode ficar vazio." }
        database.withTransaction {
            dao.insertExercise(ExerciseEntity(sheetId = sheetId, name = name.trim(), target = target.trim(), position = dao.nextExercisePosition(sheetId), kind = normalizeKind(kind), perSide = perSide))
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

    override suspend fun updateExercise(exerciseId: Long, name: String, target: String, kind: String, perSide: Boolean) {
        require(name.isNotBlank()) { "Nome do exercício não pode ficar vazio." }
        database.withTransaction {
            dao.updateExercise(exerciseId, name.trim(), target.trim(), normalizeKind(kind), perSide)
            dao.touchSheetOfExercise(exerciseId, System.currentTimeMillis())
        }
    }

    override suspend fun addEntry(exerciseId: Long, sets: Int, reps: Int, weightKg: Double, setType: String, rpe: Double?, durationSec: Int?, distanceKm: Double?) {
        validateSetInput(sets, reps, weightKg)?.let { throw AppException(it) }
        validateEffort(durationSec, distanceKm)?.let { throw AppException(it) }
        database.withTransaction {
            dao.insertEntry(EntryEntity(exerciseId = exerciseId, sets = sets, reps = reps, weightKg = weightKg, setType = setType, rpe = rpe, durationSec = durationSec, distanceKm = distanceKm))
            dao.touchSheetOfExercise(exerciseId, System.currentTimeMillis())
        }
        onChanged()
    }

    override suspend fun updateExerciseNote(exerciseId: Long, note: String) {
        dao.updateExerciseNote(exerciseId, note.trim())
    }

    override suspend fun deleteEntry(entryId: Long) {
        dao.deleteEntry(entryId)
        onChanged()
    }

    override suspend fun restoreEntry(entry: SetEntry) {
        if (dao.findExercise(entry.exerciseId) == null) return
        dao.insertEntry(EntryEntity(entry.id, entry.exerciseId, entry.sets, entry.reps, entry.weightKg, entry.recordedAt, entry.setType, entry.rpe, entry.durationSec, entry.distanceKm))
        onChanged()
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
        validateBodyWeight(weightKg)?.let { throw AppException(it) }
        dao.insertBodyWeight(BodyWeightEntity(weightKg = weightKg))
    }

    override suspend fun deleteBodyWeight(id: Long) {
        dao.deleteBodyWeight(id)
    }

    override fun observeWeeklyPlan(): Flow<Map<Int, Long?>> = dao.observeSchedule().map { rows -> rows.associate { it.dayOfWeek to it.sheetId } }

    override fun observePlan(from: LocalDate, days: Int): Flow<List<DayPlan>> = combine(observeWeeklyPlan(), dao.observeOverrides(from.toString())) { weekly, overrides ->
        val byDate = overrides.associate { LocalDate.parse(it.date) to it.sheetId }
        (0 until days).map { resolveDay(from.plusDays(it.toLong()), weekly, byDate) }
    }

    override suspend fun setWeekdaySheet(dayOfWeek: Int, sheetId: Long?) {
        require(dayOfWeek in 1..7) { "Dia da semana inválido." }
        dao.upsertSchedule(ScheduleEntity(dayOfWeek, sheetId))
    }

    override suspend fun todaySheet(now: Instant, zone: ZoneId): DayPlan {
        val today = now.atZone(zone).toLocalDate()
        return resolveDay(today, weeklyMap(), dao.overridesOn(listOf(today.toString())).associate { LocalDate.parse(it.date) to it.sheetId })
    }

    override suspend fun reschedule(from: LocalDate, to: LocalDate) {
        database.withTransaction {
            val weekly = weeklyMap()
            val overrides = dao.overridesOn(listOf(from.toString(), to.toString())).associate { LocalDate.parse(it.date) to it.sheetId }
            rescheduleChanges(from, to, weekly, overrides).forEach { (date, sheetId) ->
                // An override equal to the weekly plan is redundant, so dropping it makes "moved back" clear the badge.
                if (sheetId == weekly[date.dayOfWeek.value]) dao.deleteOverride(date.toString()) else dao.upsertOverride(ScheduleOverrideEntity(date.toString(), sheetId))
            }
        }
    }

    private suspend fun weeklyMap() = dao.allSchedule().associate { it.dayOfWeek to it.sheetId }

    override suspend fun createSheetFromTemplate(template: WorkoutTemplate) {
        if (template.sheetName.isBlank()) throw AppException(AppError.SheetNameEmpty)
        database.withTransaction {
            val sheetId = dao.insertSheet(SheetEntity(name = template.sheetName.trim()))
            template.exercises.forEachIndexed { index, e ->
                // Same kind inference as catalog adds, so template holds like "Prancha" are timed.
                dao.insertExercise(ExerciseEntity(sheetId = sheetId, name = e.name, target = e.target, position = index, kind = ExerciseCatalog.inferKind(e.name, muscle = e.target)))
            }
        }
        onChanged()
    }

    override fun observeDailyVolume(): Flow<List<DailyVolume>> = flow {
        emitAll(dao.observeDailyVolume(recentWindowStart()).map { rows -> rows.map { DailyVolume(it.day, it.volumeKg) } })
    }

    override fun observeHistory(): Flow<List<HistoryEntry>> = dao.observeHistory().map { rows ->
        rows.map { HistoryEntry(it.exerciseId, it.exerciseName, it.sheetName, it.sets, it.reps, it.weightKg, it.recordedAt, it.setType, it.rpe, it.target, it.kind, it.durationSec, it.distanceKm) }
    }

    override fun observeFinishedSessions(): Flow<List<FinishedSession>> =
        dao.observeFinishedSessions().map { rows -> rows.map { FinishedSession(it.id, it.sheetName, it.startedAt, it.endedAt) } }

    override fun observeActiveSession(): Flow<WorkoutSession?> = dao.observeActiveSession().map { it?.toDomain() }

    override fun observeSession(sessionId: Long): Flow<WorkoutSession?> = dao.observeSession(sessionId).map { it?.toDomain() }

    override suspend fun startSession(sheetId: Long, bodyWeightKg: Double?): Long {
        bodyWeightKg?.let { w -> validateBodyWeight(w)?.let { throw AppException(it) } }
        return database.withTransaction {
            val active = dao.activeSession()
            if (active?.sheetId == sheetId) return@withTransaction active.id
            active?.let { dao.deleteSession(it.id) }
            bodyWeightKg?.let { dao.insertBodyWeight(BodyWeightEntity(weightKg = it)) }
            dao.insertSession(SessionEntity(sheetId = sheetId, startedAt = System.currentTimeMillis(), bodyWeightKg = bodyWeightKg))
        }
    }

    override suspend fun finishSession(sessionId: Long) {
        dao.finishSession(sessionId, System.currentTimeMillis(), null)
    }

    override suspend fun discardSession(sessionId: Long) {
        dao.deleteSession(sessionId)
    }

    override suspend fun setSheetRule(sheetId: Long, rule: String) {
        require(rule in PROGRESSION_RULES) { "Regra de progressão inválida." }
        dao.updateSheetRule(sheetId, rule)
    }

    override suspend fun setExerciseRule(exerciseId: Long, rule: String?) {
        require(rule == null || rule in PROGRESSION_RULES) { "Regra de progressão inválida." }
        dao.updateExerciseRule(exerciseId, rule)
    }

    private fun SessionEntity.toDomain() = WorkoutSession(id, sheetId, startedAt, endedAt, bodyWeightKg)

    private fun SheetWithCount.toDomain() = TrainingSheet(id, name, exerciseCount, updatedAt, progressionRule)
}
