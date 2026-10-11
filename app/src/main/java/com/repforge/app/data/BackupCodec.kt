package com.repforge.app.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

data class BackupEntry(val sets: Int, val reps: Int, val weightKg: Double, val recordedAt: Long)
data class BackupExercise(val name: String, val target: String, val position: Int, val entries: List<BackupEntry>)
data class BackupSheet(val name: String, val updatedAt: Long, val exercises: List<BackupExercise>)

/**
 * Reads and writes backup files. Besides the native format it accepts the nested
 * JSON exported by the legacy Expo app (1.0.x) so existing users can migrate.
 */
object BackupCodec {
    const val FORMAT = "repforge-backup"
    const val VERSION = 1

    fun encode(sheets: List<BackupSheet>, exportedAt: Long = System.currentTimeMillis()): String {
        val sheetArray = JSONArray()
        val exerciseArray = JSONArray()
        val entryArray = JSONArray()
        var exerciseId = 0L
        var entryId = 0L
        sheets.forEachIndexed { sheetIndex, sheet ->
            val sheetId = sheetIndex + 1L
            sheetArray.put(JSONObject().apply { put("id", sheetId); put("name", sheet.name); put("updatedAt", sheet.updatedAt) })
            sheet.exercises.forEach { exercise ->
                exerciseId += 1
                exerciseArray.put(JSONObject().apply {
                    put("id", exerciseId); put("sheetId", sheetId); put("name", exercise.name)
                    put("target", exercise.target); put("position", exercise.position)
                })
                exercise.entries.forEach { entry ->
                    entryId += 1
                    entryArray.put(JSONObject().apply {
                        put("id", entryId); put("exerciseId", exerciseId); put("sets", entry.sets); put("reps", entry.reps)
                        put("weightKg", entry.weightKg); put("recordedAt", entry.recordedAt)
                    })
                }
            }
        }
        return JSONObject().apply {
            put("format", FORMAT)
            put("version", VERSION)
            put("exportedAt", exportedAt)
            put("sheets", sheetArray)
            put("exercises", exerciseArray)
            put("entries", entryArray)
        }.toString(2)
    }

    fun decode(raw: String): List<BackupSheet> {
        val root = try { JSONObject(raw) } catch (_: JSONException) { throw IllegalArgumentException("Arquivo não é um JSON válido.") }
        require(root.optInt("version") == VERSION) { "Versão de backup não suportada." }
        val sheets = root.optJSONArray("sheets") ?: throw IllegalArgumentException("Backup sem fichas.")
        return try {
            if (root.optString("format") == FORMAT) decodeNative(sheets, root) else decodeLegacy(sheets)
        } catch (error: JSONException) {
            throw IllegalArgumentException("Backup corrompido: ${error.message}")
        }
    }

    private fun decodeNative(sheets: JSONArray, root: JSONObject): List<BackupSheet> {
        val exercises = root.optJSONArray("exercises") ?: JSONArray()
        val entries = root.optJSONArray("entries") ?: JSONArray()
        val sheetIds = (0 until sheets.length()).map { sheets.getJSONObject(it).getLong("id") }.toSet()
        val exerciseIds = (0 until exercises.length()).map { exercises.getJSONObject(it).getLong("id") }.toSet()

        val entriesByExercise = (0 until entries.length()).map { entries.getJSONObject(it) }.groupBy { item ->
            item.getLong("exerciseId").also { require(it in exerciseIds) { "Série aponta para exercício inexistente." } }
        }
        val exercisesBySheet = (0 until exercises.length()).map { exercises.getJSONObject(it) }.groupBy { item ->
            item.getLong("sheetId").also { require(it in sheetIds) { "Exercício aponta para ficha inexistente." } }
        }
        return (0 until sheets.length()).map { index ->
            val sheet = sheets.getJSONObject(index)
            BackupSheet(
                name = name(sheet),
                updatedAt = sheet.optLong("updatedAt", System.currentTimeMillis()),
                exercises = exercisesBySheet[sheet.getLong("id")].orEmpty().map { exercise ->
                    BackupExercise(
                        name = name(exercise),
                        target = exercise.optString("target").trim(),
                        position = position(exercise),
                        entries = entriesByExercise[exercise.getLong("id")].orEmpty().map { entry ->
                            entry(entry.getInt("sets"), entry.getInt("reps"), entry.getDouble("weightKg"), entry.getLong("recordedAt"))
                        }
                    )
                }
            )
        }
    }

    private fun decodeLegacy(sheets: JSONArray): List<BackupSheet> = (0 until sheets.length()).map { index ->
        val sheet = sheets.getJSONObject(index)
        val exercises = sheet.optJSONArray("exercises") ?: throw IllegalArgumentException("Arquivo não é um backup RepForge.")
        BackupSheet(
            name = name(sheet),
            updatedAt = isoMillis(either(sheet, "updated_at", "updatedAt")),
            exercises = (0 until exercises.length()).map { exerciseIndex ->
                val exercise = exercises.getJSONObject(exerciseIndex)
                val entries = exercise.optJSONArray("entries") ?: JSONArray()
                BackupExercise(
                    name = name(exercise),
                    target = exercise.optString("target").trim(),
                    position = position(exercise),
                    entries = (0 until entries.length()).map { entryIndex ->
                        val entry = entries.getJSONObject(entryIndex)
                        entry(
                            entry.getInt("sets"),
                            entry.getInt("reps"),
                            if (entry.has("weight_kg")) entry.getDouble("weight_kg") else entry.getDouble("weightKg"),
                            isoMillis(either(entry, "recorded_at", "recordedAt"))
                        )
                    }
                )
            }
        )
    }

    private fun name(item: JSONObject): String =
        item.getString("name").trim().also { require(it.isNotEmpty()) { "Backup contém nome vazio." } }

    private fun position(item: JSONObject): Int =
        item.getInt("position").also { require(it >= 0) { "Backup contém posição inválida." } }

    private fun entry(sets: Int, reps: Int, weightKg: Double, recordedAt: Long): BackupEntry {
        require(sets > 0 && reps > 0 && weightKg >= 0 && weightKg.isFinite()) { "Backup contém série inválida." }
        return BackupEntry(sets, reps, weightKg, recordedAt)
    }

    private fun either(item: JSONObject, first: String, second: String): String =
        if (item.has(first)) item.getString(first) else item.getString(second)

    private fun isoMillis(value: String): Long =
        runCatching { Instant.parse(value).toEpochMilli() }
            .recoverCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
            .recoverCatching { LocalDateTime.parse(value).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }
            .getOrElse { throw IllegalArgumentException("Data inválida no backup: $value") }
}
