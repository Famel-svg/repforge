package com.repforge.app.data

import androidx.room.withTransaction
import com.repforge.app.data.local.BodyWeightEntity
import com.repforge.app.data.local.EntryEntity
import com.repforge.app.data.local.ExerciseEntity
import com.repforge.app.data.local.SheetEntity
import com.repforge.app.data.local.WorkoutDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** [imported] and [skipped] count sets plus body weights; skipped ones already existed. */
data class ImportResult(val sheets: Int, val imported: Int, val skipped: Int)

/** Splits [incoming] into (new, alreadyExisting) by [key]; only [existing] counts, so identical rows inside one file are all kept. */
internal fun <T, K> partitionNew(incoming: List<T>, existing: Set<K>, key: (T) -> K): Pair<List<T>, List<T>> =
    incoming.partition { key(it) !in existing }

internal fun entryKey(entry: BackupEntry) = Triple(entry.recordedAt, entry.reps, entry.weightKg)

class BackupManager(private val database: WorkoutDatabase) {
    suspend fun exportJson(): String {
        val dao = database.workoutDao()
        val entries = dao.allEntries().groupBy { it.exerciseId }
        val exercises = dao.allExercises().groupBy { it.sheetId }
        val sheets = dao.allSheets()
        val weights = dao.allBodyWeights()
        return withContext(Dispatchers.Default) {
            BackupCodec.encode(sheets.map { sheet ->
                BackupSheet(sheet.name, sheet.updatedAt, exercises[sheet.id].orEmpty().map { exercise ->
                    BackupExercise(exercise.name, exercise.target, exercise.position, entries[exercise.id].orEmpty().map {
                        BackupEntry(it.sets, it.reps, it.weightKg, it.recordedAt, it.setType, it.rpe)
                    }, exercise.note, exercise.supersetGroup)
                })
            }, bodyWeights = weights.map { BackupBodyWeight(it.weightKg, it.recordedAt) })
        }
    }

    /** Appends the backup to the current data, skipping what already exists. */
    suspend fun importJson(raw: String): ImportResult {
        val (sheets, weights) = withContext(Dispatchers.Default) { BackupCodec.decode(raw) to BackupCodec.decodeBodyWeights(raw) }
        return importSheets(sheets, weights)
    }

    /**
     * Appends in a single transaction. A sheet with an existing name is reused, and so are its same-named exercises;
     * sets with the same exercise + time + reps + load, and body weights with the same time + load, are skipped.
     */
    suspend fun importSheets(sheets: List<BackupSheet>, bodyWeights: List<BackupBodyWeight> = emptyList()): ImportResult {
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
            val keysByExercise = dao.allEntries().groupBy({ it.exerciseId }, { Triple(it.recordedAt, it.reps, it.weightKg) })
            sheets.forEach { sheet ->
                val existing = sheetsByName[sheet.name]?.firstOrNull()
                val sheetId = existing?.id ?: dao.insertSheet(SheetEntity(name = sheet.name, updatedAt = sheet.updatedAt))
                val exerciseIds = exercisesBySheet[sheetId].orEmpty().groupBy { it.name }.mapValues { it.value.first().id }.toMutableMap()
                sheet.exercises.forEach { exercise ->
                    val exerciseId = exerciseIds.getOrPut(exercise.name) {
                        dao.insertExercise(ExerciseEntity(sheetId = sheetId, name = exercise.name, target = exercise.target, position = exercise.position, note = exercise.note, supersetGroup = exercise.supersetGroup))
                    }
                    val (fresh, old) = partitionNew(exercise.entries, keysByExercise[exerciseId].orEmpty().toSet(), ::entryKey)
                    fresh.forEach { entry ->
                        dao.insertEntry(EntryEntity(exerciseId = exerciseId, sets = entry.sets, reps = entry.reps, weightKg = entry.weightKg, recordedAt = entry.recordedAt, setType = entry.setType, rpe = entry.rpe))
                    }
                    imported += fresh.size
                    skipped += old.size
                }
            }
        }
        return ImportResult(sheets.size, imported, skipped)
    }
}
