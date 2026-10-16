package com.repforge.app.data

import com.repforge.app.domain.model.SET_DROP
import com.repforge.app.domain.model.SET_FAILURE
import com.repforge.app.domain.model.SET_NORMAL
import com.repforge.app.domain.model.SET_WARMUP
import com.repforge.app.domain.validation.AppError
import com.repforge.app.domain.validation.AppException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Parsed sheets plus the number of rows skipped (no reps, bad date, malformed). */
data class CsvImportResult(val sheets: List<BackupSheet>, val skippedRows: Int, val maybePounds: Boolean = false)

private const val LBS_TO_KG = 0.45359237
private val STRONG_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
private val HEVY_DATE = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH)

fun parseCsvWorkouts(raw: String, zone: ZoneId): List<BackupSheet> = parseCsvWorkoutsWithStats(raw, zone).sheets

fun parseCsvWorkoutsWithStats(raw: String, zone: ZoneId): CsvImportResult {
    val rows = readCsv(raw)
    val header = rows.firstOrNull()?.map { it.trim().lowercase() }.orEmpty()
    val strong = listOf("date", "workout name", "exercise name", "set order", "weight", "reps").all { it in header }
    val hevy = listOf("title", "start_time", "exercise_title", "set_index", "set_type", "reps").all { it in header } &&
        ("weight_kg" in header || "weight_lbs" in header)
    if (!(strong || hevy)) throw AppException(AppError.CsvFormat)

    fun col(row: List<String>, name: String) = header.indexOf(name).takeIf { it >= 0 }?.let { row.getOrNull(it) }?.trim().orEmpty()
    // Accepts "12", "12.0" and "12,5" (decimal comma).
    fun number(text: String) = text.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }

    val sheets = LinkedHashMap<String, LinkedHashMap<String, MutableList<BackupEntry>>>()
    val updated = HashMap<String, Long>()
    var skipped = 0
    rows.drop(1).filter { row -> row.any { it.isNotBlank() } }.forEach { row ->
        val parsed = runCatching {
            val title = col(row, if (strong) "workout name" else "title").ifEmpty { error("sem nome") }
            val exercise = col(row, if (strong) "exercise name" else "exercise_title").ifEmpty { error("sem exercício") }
            val at = LocalDateTime.parse(col(row, if (strong) "date" else "start_time"), if (strong) STRONG_DATE else HEVY_DATE)
                .atZone(zone).toInstant().toEpochMilli()
            val reps = number(col(row, "reps"))?.toInt()?.takeIf { it > 0 } ?: error("sem repetições")
            val weightKg = if (strong) {
                val weight = number(col(row, "weight")) ?: 0.0
                if (col(row, "weight unit").lowercase().startsWith("lb")) weight * LBS_TO_KG else weight
            } else if ("weight_kg" in header) number(col(row, "weight_kg")) ?: 0.0
            else (number(col(row, "weight_lbs")) ?: 0.0) * LBS_TO_KG
            val marker = col(row, if (strong) "set order" else "set_type").lowercase()
            Triple(title, exercise, BackupEntry(1, reps, weightKg, at, setTypeOf(marker)))
        }.getOrNull()
        if (parsed == null) { skipped++; return@forEach }
        val (title, exercise, entry) = parsed
        sheets.getOrPut(title) { LinkedHashMap() }.getOrPut(exercise) { mutableListOf() }.add(entry)
        updated.merge(title, entry.recordedAt, ::maxOf)
    }
    return CsvImportResult(
        sheets.map { (title, exercises) ->
            BackupSheet(title, updated.getValue(title), exercises.entries.mapIndexed { index, (name, entries) ->
                BackupExercise(name, "", index, entries)
            })
        },
        skipped,
        strong && "weight unit" !in header && sheets.values.any { ex -> ex.values.any { l -> l.any { it.weightKg > 300 } } }
    )
}

/** RFC-4180 reader: quoted fields, doubled quotes, embedded delimiters/newlines, BOM, `,` or `;` delimiter. */
private fun readCsv(input: String): List<List<String>> {
    val text = input.removePrefix("\uFEFF")
    val firstLine = text.lineSequence().firstOrNull().orEmpty()
    val delimiter = if (firstLine.count { it == ';' } > firstLine.count { it == ',' }) ';' else ','
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val field = StringBuilder()
    var quoted = false
    var i = 0
    while (i < text.length) {
        val c = text[i]
        when {
            quoted && c == '"' && text.getOrNull(i + 1) == '"' -> { field.append('"'); i++ }
            c == '"' -> quoted = !quoted
            quoted -> field.append(c)
            c == delimiter -> { row.add(field.toString()); field.clear() }
            c == '\n' || c == '\r' -> {
                if (c == '\r' && text.getOrNull(i + 1) == '\n') i++
                row.add(field.toString()); field.clear()
                rows.add(row); row = mutableListOf()
            }
            else -> field.append(c)
        }
        i++
    }
    if (field.isNotEmpty() || row.isNotEmpty()) { row.add(field.toString()); rows.add(row) }
    return rows
}

/** Hevy `set_type` words and Strong `Set Order` letters (W/D/F); anything else, including numbers, is a normal set. */
private fun setTypeOf(marker: String) = when (marker) {
    "warmup", "w" -> SET_WARMUP
    "dropset", "d" -> SET_DROP
    "failure", "f" -> SET_FAILURE
    else -> SET_NORMAL
}
