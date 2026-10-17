package com.repforge.app.domain.repository

import com.repforge.app.domain.metrics.FinishedSession
import com.repforge.app.domain.model.BodyWeight
import com.repforge.app.domain.model.DashboardSummary
import com.repforge.app.domain.model.TrainingSheet
import com.repforge.app.domain.model.WorkoutSession
import com.repforge.app.domain.model.Exercise
import com.repforge.app.domain.model.KIND_REPS
import com.repforge.app.domain.model.SET_NORMAL
import com.repforge.app.domain.model.SetEntry
import com.repforge.app.domain.model.DailyVolume
import com.repforge.app.domain.model.HistoryEntry
import com.repforge.app.domain.templates.WorkoutTemplate
import com.repforge.app.domain.schedule.DayPlan
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

interface WorkoutRepository {
    fun observeSheets(): Flow<List<TrainingSheet>>
    fun observeSheet(sheetId: Long): Flow<TrainingSheet?>
    fun observeSummary(): Flow<DashboardSummary>
    suspend fun createSheet(name: String)
    /** [copyLabel] is the localized word appended to the copy's name, e.g. "copy". */
    suspend fun duplicateSheet(sheetId: Long, copyLabel: String)
    suspend fun deleteSheet(sheetId: Long)
    fun observeExercises(sheetId: Long): Flow<List<Exercise>>
    fun observeEntries(exerciseId: Long): Flow<List<SetEntry>>
    suspend fun addExercise(sheetId: Long, name: String, target: String, kind: String = KIND_REPS, perSide: Boolean = false)
    suspend fun updateExercise(exerciseId: Long, name: String, target: String, kind: String, perSide: Boolean)
    suspend fun deleteExercise(exerciseId: Long)
    suspend fun addEntry(exerciseId: Long, sets: Int, reps: Int, weightKg: Double, setType: String = SET_NORMAL, rpe: Double? = null, durationSec: Int? = null, distanceKm: Double? = null)
    suspend fun updateExerciseNote(exerciseId: Long, note: String)
    suspend fun deleteEntry(entryId: Long)
    suspend fun restoreEntry(entry: SetEntry)
    suspend fun moveExercise(exerciseId: Long, up: Boolean)
    suspend fun createSheetFromTemplate(template: WorkoutTemplate)
    fun observeDailyVolume(): Flow<List<DailyVolume>>
    fun observeHistory(): Flow<List<HistoryEntry>>
    suspend fun setSupersetWithNext(exerciseId: Long, join: Boolean)
    fun observeBodyWeights(): Flow<List<BodyWeight>>
    suspend fun addBodyWeight(weightKg: Double)
    suspend fun deleteBodyWeight(id: Long)
    /** Weekly plan keyed by ISO day of week (1 = Monday); a missing key or null value means rest. */
    fun observeWeeklyPlan(): Flow<Map<Int, Long?>>
    /** [days] resolved days starting at [from] (weekly plan with one-off overrides applied). */
    fun observePlan(from: LocalDate, days: Int): Flow<List<DayPlan>>
    suspend fun setWeekdaySheet(dayOfWeek: Int, sheetId: Long?)
    suspend fun todaySheet(now: Instant, zone: ZoneId): DayPlan
    /** Moves the sheet planned on [from] to [to] as overrides (from becomes rest); the weekly plan is untouched. */
    suspend fun reschedule(from: LocalDate, to: LocalDate)
    /** The workout in progress, if any; survives process death because it lives in the database. */
    fun observeActiveSession(): Flow<WorkoutSession?>
    /** Finished workouts, newest first. */
    fun observeFinishedSessions(): Flow<List<FinishedSession>>
    fun observeSession(sessionId: Long): Flow<WorkoutSession?>
    /**
     * Starts a workout on [sheetId] and returns its session id. Resumes the active session when it is already on this sheet;
     * an active session on another sheet is dropped first. [bodyWeightKg], when given, is also saved as a body-weight record.
     */
    suspend fun startSession(sheetId: Long, bodyWeightKg: Double? = null): Long
    /** Ends the session now; its sets are those logged between its start and end. */
    suspend fun finishSession(sessionId: Long)
    /** Ends the session without a summary. Logged sets stay. */
    suspend fun discardSession(sessionId: Long)
    suspend fun setSheetRule(sheetId: Long, rule: String)
    /** [rule] null = follow the sheet's rule. */
    suspend fun setExerciseRule(exerciseId: Long, rule: String?)
}
