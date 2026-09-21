package com.repforge.app.data

import androidx.room.withTransaction
import com.repforge.app.data.local.EntryEntity
import com.repforge.app.data.local.ExerciseEntity
import com.repforge.app.data.local.SheetEntity
import com.repforge.app.data.local.WorkoutDatabase
import org.json.JSONArray
import org.json.JSONObject

class BackupManager(private val database: WorkoutDatabase) {
    suspend fun exportJson(): String {
        val sheets = database.workoutDao().allSheets()
        val exercises = database.workoutDao().allExercises()
        val entries = database.workoutDao().allEntries()
        return JSONObject().apply {
            put("format", "repforge-backup")
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("sheets", JSONArray().apply { sheets.forEach { put(sheetJson(it)) } })
            put("exercises", JSONArray().apply { exercises.forEach { put(exerciseJson(it)) } })
            put("entries", JSONArray().apply { entries.forEach { put(entryJson(it)) } })
        }.toString(2)
    }

    suspend fun importJson(raw: String) {
        val root = JSONObject(raw)
        require(root.optString("format") == "repforge-backup") { "Arquivo não é um backup RepForge." }
        require(root.optInt("version") == 1) { "Versão de backup não suportada." }
        val sheets = root.optJSONArray("sheets") ?: throw IllegalArgumentException("Backup sem fichas.")
        val exercises = root.optJSONArray("exercises") ?: JSONArray()
        val entries = root.optJSONArray("entries") ?: JSONArray()

        database.withTransaction {
            val dao = database.workoutDao()
            dao.clearEntries(); dao.clearExercises(); dao.clearSheets()
            val sheetIds = mutableMapOf<Long, Long>()
            val exerciseIds = mutableMapOf<Long, Long>()
            for (index in 0 until sheets.length()) {
                val item = sheets.getJSONObject(index)
                val oldId = item.getLong("id")
                sheetIds[oldId] = dao.insertSheet(SheetEntity(name = item.getString("name"), updatedAt = item.optLong("updatedAt")))
            }
            for (index in 0 until exercises.length()) {
                val item = exercises.getJSONObject(index)
                val sheetId = sheetIds[item.getLong("sheetId")] ?: error("Exercício aponta para ficha inexistente.")
                exerciseIds[item.getLong("id")] = dao.insertExercise(ExerciseEntity(sheetId = sheetId, name = item.getString("name"), target = item.optString("target"), position = item.getInt("position")))
            }
            for (index in 0 until entries.length()) {
                val item = entries.getJSONObject(index)
                val exerciseId = exerciseIds[item.getLong("exerciseId")] ?: error("Série aponta para exercício inexistente.")
                dao.insertEntry(EntryEntity(exerciseId = exerciseId, sets = item.getInt("sets"), reps = item.getInt("reps"), weightKg = item.getDouble("weightKg"), recordedAt = item.getLong("recordedAt")))
            }
        }
    }

    private fun sheetJson(item: SheetEntity) = JSONObject().apply { put("id", item.id); put("name", item.name); put("updatedAt", item.updatedAt) }
    private fun exerciseJson(item: ExerciseEntity) = JSONObject().apply { put("id", item.id); put("sheetId", item.sheetId); put("name", item.name); put("target", item.target); put("position", item.position) }
    private fun entryJson(item: EntryEntity) = JSONObject().apply { put("id", item.id); put("exerciseId", item.exerciseId); put("sets", item.sets); put("reps", item.reps); put("weightKg", item.weightKg); put("recordedAt", item.recordedAt) }
}
