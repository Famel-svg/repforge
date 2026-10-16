package com.repforge.app.data

import androidx.room.withTransaction
import com.repforge.app.data.local.BodyWeightEntity
import com.repforge.app.data.local.EntryEntity
import com.repforge.app.data.local.ExerciseEntity
import com.repforge.app.data.local.SheetEntity
import com.repforge.app.data.local.WorkoutDatabase

class BackupManager(private val database: WorkoutDatabase) {
    suspend fun exportJson(): String {
        val dao = database.workoutDao()
        val entries = dao.allEntries().groupBy { it.exerciseId }
        val exercises = dao.allExercises().groupBy { it.sheetId }
        return BackupCodec.encode(dao.allSheets().map { sheet ->
            BackupSheet(sheet.name, sheet.updatedAt, exercises[sheet.id].orEmpty().map { exercise ->
                BackupExercise(exercise.name, exercise.target, exercise.position, entries[exercise.id].orEmpty().map {
                    BackupEntry(it.sets, it.reps, it.weightKg, it.recordedAt, it.setType, it.rpe)
                }, exercise.note, exercise.supersetGroup)
            })
        }, bodyWeights = dao.allBodyWeights().map { BackupBodyWeight(it.weightKg, it.recordedAt) })
    }

    /** Appends the backup to the current data and returns the number of imported sheets. */
    suspend fun importJson(raw: String): Int {
        return importSheets(BackupCodec.decode(raw), BackupCodec.decodeBodyWeights(raw))
    }

    /** Appends the sheets in a single transaction and returns how many were inserted. */
    suspend fun importSheets(sheets: List<BackupSheet>, bodyWeights: List<BackupBodyWeight> = emptyList()): Int {
        database.withTransaction {
            val dao = database.workoutDao()
            bodyWeights.forEach { dao.insertBodyWeight(BodyWeightEntity(weightKg = it.weightKg, recordedAt = it.recordedAt)) }
            sheets.forEach { sheet ->
                val sheetId = dao.insertSheet(SheetEntity(name = sheet.name, updatedAt = sheet.updatedAt))
                sheet.exercises.forEach { exercise ->
                    val exerciseId = dao.insertExercise(ExerciseEntity(sheetId = sheetId, name = exercise.name, target = exercise.target, position = exercise.position, note = exercise.note, supersetGroup = exercise.supersetGroup))
                    exercise.entries.forEach { entry ->
                        dao.insertEntry(EntryEntity(exerciseId = exerciseId, sets = entry.sets, reps = entry.reps, weightKg = entry.weightKg, recordedAt = entry.recordedAt, setType = entry.setType, rpe = entry.rpe))
                    }
                }
            }
        }
        return sheets.size
    }
}
