package com.repforge.app.data

import com.repforge.app.domain.model.SET_TYPES
import com.repforge.app.domain.model.normalizeKind
import com.repforge.app.domain.validation.AppError
import com.repforge.app.domain.validation.AppException
import com.repforge.app.domain.validation.validateBodyWeight
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

data class BackupEntry(val sets: Int, val reps: Int, val weightKg: Double, val recordedAt: Long, val setType: String = "normal", val rpe: Double? = null, val durationSec: Int? = null, val distanceKm: Double? = null)
data class BackupExercise(
    val name: String, val target: String, val position: Int, val entries: List<BackupEntry>, val note: String = "", val supersetGroup: Int? = null,
    val kind: String = "reps", val perSide: Boolean = false, val progressionRule: String? = null
)
data class BackupBodyWeight(val weightKg: Double, val recordedAt: Long)
data class BackupSheet(val name: String, val updatedAt: Long, val exercises: List<BackupExercise>, val progressionRule: String = DEFAULT_RULE)
const val DEFAULT_RULE = "double"
private val RULES = setOf("none", "linear", "double", "time")

/** Plan entries reference sheets by name because sheet ids are renumbered on export; a null name is a rest day. */
data class BackupPlan(val weekly: Map<Int, String?> = emptyMap(), val overrides: Map<String, String?> = emptyMap())

/**
 * Reads and writes backup files. Besides the native format it accepts the nested
 * JSON exported by the legacy Expo app (1.0.x) so existing users can migrate.
 */
object BackupCodec {
    const val FORMAT = "repforge-backup"
    const val VERSION = 1

    fun encode(sheets: List<BackupSheet>, exportedAt: Long = System.currentTimeMillis(), bodyWeights: List<BackupBodyWeight> = emptyList(), plan: BackupPlan = BackupPlan()): String {
        val sheetArray = JSONArray()
        val exerciseArray = JSONArray()
        val entryArray = JSONArray()
        var exerciseId = 0L
        var entryId = 0L
        sheets.forEachIndexed { sheetIndex, sheet ->
            val sheetId = sheetIndex + 1L
            sheetArray.put(JSONObject().apply { put("id", sheetId); put("name", sheet.name); put("updatedAt", sheet.updatedAt); put("progressionRule", sheet.progressionRule) })
            sheet.exercises.forEach { exercise ->
                exerciseId += 1
                exerciseArray.put(JSONObject().apply {
                    put("id", exerciseId); put("sheetId", sheetId); put("name", exercise.name)
                    put("target", exercise.target); put("position", exercise.position); put("note", exercise.note)
                    exercise.supersetGroup?.let { put("supersetGroup", it) }
                    put("kind", exercise.kind); put("perSide", exercise.perSide); exercise.progressionRule?.let { put("progressionRule", it) }
                })
                exercise.entries.forEach { entry ->
                    entryId += 1
                    entryArray.put(JSONObject().apply {
                        put("id", entryId); put("exerciseId", exerciseId); put("sets", entry.sets); put("reps", entry.reps)
                        put("weightKg", entry.weightKg); put("recordedAt", entry.recordedAt)
                        put("setType", entry.setType); entry.rpe?.let { put("rpe", it) }
                        entry.durationSec?.let { put("durationSec", it) }; entry.distanceKm?.let { put("distanceKm", it) }
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
            put("bodyWeights", JSONArray().apply { bodyWeights.forEach { put(JSONObject().apply { put("weightKg", it.weightKg); put("recordedAt", it.recordedAt) }) } })
            put("schedule", JSONArray().apply { plan.weekly.forEach { (day, sheet) -> put(JSONObject().apply { put("dayOfWeek", day); put("sheet", sheet ?: JSONObject.NULL) }) } })
            put("scheduleOverrides", JSONArray().apply { plan.overrides.forEach { (date, sheet) -> put(JSONObject().apply { put("date", date); put("sheet", sheet ?: JSONObject.NULL) }) } })
        }.toString(2)
    }

    fun decode(raw: String): List<BackupSheet> {
        val root = try { JSONObject(raw) } catch (_: JSONException) { throw AppException(AppError.InvalidJson) }
        if (root.optInt("version") != VERSION) throw AppException(AppError.UnsupportedVersion)
        val sheets = root.optJSONArray("sheets") ?: throw AppException(AppError.NoSheets)
        return try {
            if (root.optString("format") == FORMAT) decodeNative(sheets, root) else decodeLegacy(sheets)
        } catch (error: JSONException) {
            throw AppException(AppError.Corrupt, error.message)
        }
    }

    /** Optional top-level array; legacy and pre-3 native files simply have none. */
    fun decodeBodyWeights(raw: String): List<BackupBodyWeight> {
        val array = try { JSONObject(raw).optJSONArray("bodyWeights") } catch (_: JSONException) { null } ?: return emptyList()
        return try {
            (0 until array.length()).map {
                val item = array.getJSONObject(it)
                BackupBodyWeight(item.getDouble("weightKg"), item.getLong("recordedAt")).also { w -> if (validateBodyWeight(w.weightKg) != null) throw AppException(AppError.InvalidBodyWeight) }
            }
        } catch (error: JSONException) {
            throw AppException(AppError.Corrupt, error.message)
        }
    }

    /** Optional top-level arrays (weekly plan + dated overrides); older files have none. */
    fun decodePlan(raw: String): BackupPlan {
        val root = try { JSONObject(raw) } catch (_: JSONException) { null } ?: return BackupPlan()
        fun sheetOf(item: JSONObject) = if (item.isNull("sheet")) null else item.getString("sheet")
        return try {
            val weekly = root.optJSONArray("schedule")?.let { a -> (0 until a.length()).map { a.getJSONObject(it) } }.orEmpty()
                .associate { item -> item.getInt("dayOfWeek").also { if (it !in 1..7) throw AppException(AppError.Corrupt) } to sheetOf(item) }
            val overrides = root.optJSONArray("scheduleOverrides")?.let { a -> (0 until a.length()).map { a.getJSONObject(it) } }.orEmpty()
                .associate { item -> item.getString("date").also { d -> runCatching { LocalDate.parse(d) }.getOrElse { throw AppException(AppError.InvalidDate, d) } } to sheetOf(item) }
            BackupPlan(weekly, overrides)
        } catch (error: JSONException) {
            throw AppException(AppError.Corrupt, error.message)
        }
    }

    private fun decodeNative(sheets: JSONArray, root: JSONObject): List<BackupSheet> {
        val exercises = root.optJSONArray("exercises") ?: JSONArray()
        val entries = root.optJSONArray("entries") ?: JSONArray()
        val sheetIds = (0 until sheets.length()).map { sheets.getJSONObject(it).getLong("id") }.toSet()
        val exerciseIds = (0 until exercises.length()).map { exercises.getJSONObject(it).getLong("id") }.toSet()

        val entriesByExercise = (0 until entries.length()).map { entries.getJSONObject(it) }.groupBy { item ->
            item.getLong("exerciseId").also { if (it !in exerciseIds) throw AppException(AppError.OrphanEntry) }
        }
        val exercisesBySheet = (0 until exercises.length()).map { exercises.getJSONObject(it) }.groupBy { item ->
            item.getLong("sheetId").also { if (it !in sheetIds) throw AppException(AppError.OrphanExercise) }
        }
        return (0 until sheets.length()).map { index ->
            val sheet = sheets.getJSONObject(index)
            BackupSheet(
                name = name(sheet),
                updatedAt = sheet.optLong("updatedAt", System.currentTimeMillis()),
                progressionRule = rule(sheet.optString("progressionRule")) ?: DEFAULT_RULE,
                exercises = exercisesBySheet[sheet.getLong("id")].orEmpty().map { exercise ->
                    BackupExercise(
                        name = name(exercise),
                        target = exercise.optString("target").trim(),
                        position = position(exercise),
                        entries = entriesByExercise[exercise.getLong("id")].orEmpty().map { entry ->
                            entry(entry.getInt("sets"), entry.getInt("reps"), entry.getDouble("weightKg"), entry.getLong("recordedAt"),
                                entry.optString("setType", "normal"), if (entry.isNull("rpe")) null else entry.optDouble("rpe"),
                                if (entry.isNull("durationSec")) null else entry.optInt("durationSec"), if (entry.isNull("distanceKm")) null else entry.optDouble("distanceKm"))
                        },
                        note = exercise.optString("note").trim(),
                        supersetGroup = if (exercise.isNull("supersetGroup")) null else exercise.optInt("supersetGroup"),
                        kind = normalizeKind(exercise.optString("kind")),
                        perSide = exercise.optBoolean("perSide", false),
                        progressionRule = rule(exercise.optString("progressionRule"))
                    )
                }
            )
        }
    }

    private fun decodeLegacy(sheets: JSONArray): List<BackupSheet> = (0 until sheets.length()).map { index ->
        val sheet = sheets.getJSONObject(index)
        val exercises = sheet.optJSONArray("exercises") ?: throw AppException(AppError.NotRepForgeBackup)
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
        item.getString("name").trim().also { if (it.isEmpty()) throw AppException(AppError.EmptyName) }

    private fun position(item: JSONObject): Int =
        item.getInt("position").also { if (it < 0) throw AppException(AppError.InvalidPosition) }

    private fun rule(value: String): String? = value.takeIf { it in RULES }

    private fun entry(sets: Int, reps: Int, weightKg: Double, recordedAt: Long, setType: String = "normal", rpe: Double? = null, durationSec: Int? = null, distanceKm: Double? = null): BackupEntry {
        if (!(sets > 0 && reps > 0 && weightKg >= 0 && weightKg.isFinite())) throw AppException(AppError.InvalidSet)
        // Unknown set types and out-of-range RPE degrade to defaults instead of rejecting the file.
        return BackupEntry(sets, reps, weightKg, recordedAt, if (setType in SET_TYPES) setType else "normal", rpe?.takeIf { it in 6.0..10.0 },
            durationSec?.takeIf { it > 0 }, distanceKm?.takeIf { it > 0 && it.isFinite() })
    }

    private fun either(item: JSONObject, first: String, second: String): String =
        if (item.has(first)) item.getString(first) else item.getString(second)

    private fun isoMillis(value: String): Long =
        runCatching { Instant.parse(value).toEpochMilli() }
            .recoverCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
            .recoverCatching { LocalDateTime.parse(value).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }
            .getOrElse { throw AppException(AppError.InvalidDate, value) }
}
