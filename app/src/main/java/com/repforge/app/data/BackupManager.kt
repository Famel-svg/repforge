package com.repforge.app.data

import androidx.room.withTransaction
import com.repforge.app.data.local.BodyWeightEntity
import com.repforge.app.data.local.EntryEntity
import com.repforge.app.data.local.ExerciseEntity
import com.repforge.app.data.local.ScheduleEntity
import com.repforge.app.data.local.ScheduleOverrideEntity
import com.repforge.app.data.local.SessionEntity
import com.repforge.app.data.local.SheetEntity
import com.repforge.app.data.local.WorkoutDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** [imported] and [skipped] count sets plus body weights; skipped ones already existed. */
data class ImportResult(val sheets: Int, val imported: Int, val skipped: Int)

/** Splits [incoming] into (new, alreadyExisting) by [key]; only [existing] counts, so identical rows inside one file are all kept. */
internal fun <T, K> partitionNew(incoming: List<T>, existing: Set<K>, key: (T) -> K): Pair<List<T>, List<T>> =
    incoming.partition { key(it) !in existing }

internal fun entryKey(entry: BackupEntry) = listOf(entry.recordedAt, entry.reps, entry.weightKg, entry.durationSec, entry.distanceKm)

class BackupManager(private val database: WorkoutDatabase) {
    suspend fun exportJson(): String {
        val dao = database.workoutDao()
        val entries = dao.allEntries().groupBy { it.exerciseId }
        val exercises = dao.allExercises().groupBy { it.sheetId }
        val sheets = dao.allSheets()
        val weights = dao.allBodyWeights()
        val schedule = dao.allSchedule()
        val overrides = dao.allOverrides()
        val names = sheets.associate { it.id to it.name }
        val sessions = dao.allSessions().mapNotNull { s -> s.endedAt?.let { BackupSession(s.sheetId?.let(names::get), s.startedAt, it, s.bodyWeightKg) } }
        return withContext(Dispatchers.Default) {
            BackupCodec.encode(sheets.map { sheet ->
                BackupSheet(sheet.name, sheet.updatedAt, exercises[sheet.id].orEmpty().map { exercise ->
                    BackupExercise(exercise.name, exercise.target, exercise.position, entries[exercise.id].orEmpty().map {
                        BackupEntry(it.sets, it.reps, it.weightKg, it.recordedAt, it.setType, it.rpe, it.durationSec, it.distanceKm)
                    }, exercise.note, exercise.supersetGroup, exercise.kind, exercise.perSide, exercise.progressionRule)
                }, sheet.progressionRule)
            }, bodyWeights = weights.map { BackupBodyWeight(it.weightKg, it.recordedAt) },
                plan = BackupPlan(schedule.associate { it.dayOfWeek to it.sheetId?.let(names::get) }, overrides.associate { it.date to it.sheetId?.let(names::get) }), sessions = sessions)
        }
    }

    /** Appends the backup to the current data, skipping what already exists. */
    suspend fun importJson(raw: String): ImportResult {
        val (sheets, weights, plan) = withContext(Dispatchers.Default) { Triple(BackupCodec.decode(raw), BackupCodec.decodeBodyWeights(raw), BackupCodec.decodePlan(raw)) }
        return importSheets(sheets, weights, plan, BackupCodec.decodeSessions(raw))
    }

    /**
     * Appends in a single transaction. A sheet with an existing name is reused, and so are its same-named exercises;
     * sets with the same exercise + time + reps + load, and body weights with the same time + load, are skipped.
     * Plan entries only fill weekdays/dates that are still unset, and are dropped when their sheet name is unknown.
     */
    suspend fun importSheets(sheets: List<BackupSheet>, bodyWeights: List<BackupBodyWeight> = emptyList(), plan: BackupPlan = BackupPlan(), sessions: List<BackupSession> = emptyList()): ImportResult {
        var imported = 0
        var skipped = 0
        database.withTransaction {
            val dao = database.workoutDao()
            val existingWeights = dao.allBodyWeights().map { it.recordedAt to it.weightKg }.toSet()
            val (newWeights, oldWeights) = partitionNew(bodyWeights, existingWeights) { it.recordedAt to it.weightKg }
            newWeights.forEach { dao.insertBodyWeight(BodyWeightEntity(weightKg = it.weightKg, recordedAt = it.recordedAt)) }
            imported += newWeights.size
            skipped += oldWeights.size
            val sheetsByName = dao.allSheets().groupBy { it.name }
            val exercisesBySheet = dao.allExercises().groupBy { it.sheetId }
            val keysByExercise = dao.allEntries().groupBy({ it.exerciseId }, { listOf(it.recordedAt, it.reps, it.weightKg, it.durationSec, it.distanceKm) })
            sheets.forEach { sheet ->
                val existing = sheetsByName[sheet.name]?.firstOrNull()
                val sheetId = existing?.id ?: dao.insertSheet(SheetEntity(name = sheet.name, updatedAt = sheet.updatedAt, progressionRule = sheet.progressionRule))
                val exerciseIds = exercisesBySheet[sheetId].orEmpty().groupBy { it.name }.mapValues { it.value.first().id }.toMutableMap()
                sheet.exercises.forEach { exercise ->
                    val exerciseId = exerciseIds.getOrPut(exercise.name) {
                        dao.insertExercise(ExerciseEntity(sheetId = sheetId, name = exercise.name, target = exercise.target, position = exercise.position, note = exercise.note, supersetGroup = exercise.supersetGroup, kind = exercise.kind, perSide = exercise.perSide, progressionRule = exercise.progressionRule))
                    }
                    val (fresh, old) = partitionNew(exercise.entries, keysByExercise[exerciseId].orEmpty().toSet(), ::entryKey)
                    fresh.forEach { entry ->
                        dao.insertEntry(EntryEntity(exerciseId = exerciseId, sets = entry.sets, reps = entry.reps, weightKg = entry.weightKg, recordedAt = entry.recordedAt, setType = entry.setType, rpe = entry.rpe, durationSec = entry.durationSec, distanceKm = entry.distanceKm))
                    }
                    imported += fresh.size
                    skipped += old.size
                }
            }
            val idByName = dao.allSheets().groupBy { it.name }.mapValues { it.value.first().id }
            plan.weekly.forEach { (day, name) ->
                if (name == null) dao.insertScheduleIfAbsent(ScheduleEntity(day, null)) else idByName[name]?.let { dao.insertScheduleIfAbsent(ScheduleEntity(day, it)) }
            }
            plan.overrides.forEach { (date, name) ->
                if (name == null) dao.insertOverrideIfAbsent(ScheduleOverrideEntity(date, null)) else idByName[name]?.let { dao.insertOverrideIfAbsent(ScheduleOverrideEntity(date, it)) }
            }
            // Sessions are deduped by startedAt; an unknown sheet name keeps the session with a null sheet.
            val seen = dao.allSessions().map { it.startedAt }.toMutableSet()
            sessions.forEach { s ->
                if (seen.add(s.startedAt)) dao.insertSession(SessionEntity(sheetId = s.sheet?.let(idByName::get), startedAt = s.startedAt, endedAt = s.endedAt, bodyWeightKg = s.bodyWeightKg))
            }
        }
        return ImportResult(sheets.size, imported, skipped)
    }
}
