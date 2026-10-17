package com.repforge.app.domain.metrics

import com.repforge.app.domain.model.BodyWeight
import com.repforge.app.domain.model.HistoryEntry
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** A finished workout; [sheetName] is null once its sheet was deleted. */
data class FinishedSession(val id: Long, val sheetName: String?, val startedAt: Long, val endedAt: Long)

/** Per-exercise totals of a training day; [bestWeightKg]/[bestReps] describe the heaviest set (0 for unloaded work). */
data class HistoryExercise(val name: String, val sets: Int, val bestWeightKg: Double, val bestReps: Int)

/**
 * One training day derived from entries. [sessionId]/[durationMs] are set when a finished session started that day,
 * and null otherwise (imported or older days logged without a session).
 */
data class HistoryDay(
    val date: LocalDate, val sheetNames: List<String>, val sets: Int, val volumeKg: Double,
    val durationMs: Long?, val sessionId: Long?, val exercises: List<HistoryExercise>
)

/** Training days from non-warmup entries, newest first. Sessions attach by the local day they started. */
fun historyDays(sessions: List<FinishedSession>, history: List<HistoryEntry>, zone: ZoneId): List<HistoryDay> {
    val sessionsByDay = sessions.groupBy { localDate(it.startedAt, zone) }
    return history.working().groupBy { localDate(it.recordedAt, zone) }.map { (date, entries) ->
        val day = sessionsByDay[date].orEmpty()
        val exercises = entries.groupBy { it.exerciseId }.values.map { g ->
            val best = g.filter { it.loaded }.maxByOrNull { it.weightKg }
            HistoryExercise(g.first().exerciseName, g.sumOf { it.sets }, best?.weightKg ?: 0.0, best?.reps ?: 0)
        }
        HistoryDay(
            date, entries.map { it.sheetName }.distinct(), entries.sumOf { it.sets }, entries.sumOf { it.volumeKg() },
            day.takeIf { it.isNotEmpty() }?.sumOf { (it.endedAt - it.startedAt).coerceAtLeast(0) },
            day.maxByOrNull { it.startedAt }?.id, exercises
        )
    }.sortedByDescending { it.date }
}

/** Newest month first, newest day first inside each month. */
fun groupByMonth(days: List<HistoryDay>): List<Pair<YearMonth, List<HistoryDay>>> =
    days.sortedByDescending { it.date }.groupBy { YearMonth.from(it.date) }.toList().sortedByDescending { it.first }

enum class WeightRange(val days: Long?) { Days30(30), Days90(90), Year(365), All(null) }

/** [chronological] oldest first; keeps records from the last [range] days (inclusive of today). */
fun filterWeights(chronological: List<BodyWeight>, range: WeightRange, now: Long, zone: ZoneId): List<BodyWeight> {
    val days = range.days ?: return chronological
    val start = localDate(now, zone).minusDays(days - 1)
    return chronological.filter { !localDate(it.recordedAt, zone).isBefore(start) }
}

/** Last minus first weight of [chronological]; null with fewer than two records. */
fun weightDelta(chronological: List<BodyWeight>): Double? =
    if (chronological.size < 2) null else chronological.last().weightKg - chronological.first().weightKg

/** Point indices used for the x-axis labels: first, middle and last (fewer when the series is short). */
fun labelIndices(count: Int): List<Int> = when {
    count <= 0 -> emptyList()
    count == 1 -> listOf(0)
    count == 2 -> listOf(0, 1)
    else -> listOf(0, (count - 1) / 2, count - 1)
}
