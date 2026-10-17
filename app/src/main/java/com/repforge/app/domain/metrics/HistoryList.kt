package com.repforge.app.domain.metrics

import com.repforge.app.domain.model.BodyWeight
import com.repforge.app.domain.model.HistoryEntry
import java.time.YearMonth
import java.time.ZoneId

/** A finished workout; [sheetName] is null once its sheet was deleted. */
data class FinishedSession(val id: Long, val sheetName: String?, val startedAt: Long, val endedAt: Long)

data class HistoryItem(val id: Long, val sheetName: String?, val startedAt: Long, val durationMs: Long, val sets: Int, val volumeKg: Double)

/** Sets belong to a session when logged between its start and end (same rule as [sessionSummary]); warmups are excluded. */
fun historyItems(sessions: List<FinishedSession>, history: List<HistoryEntry>): List<HistoryItem> {
    val work = history.working()
    return sessions.map { s ->
        val inside = work.filter { it.recordedAt in s.startedAt..s.endedAt }
        HistoryItem(s.id, s.sheetName, s.startedAt, (s.endedAt - s.startedAt).coerceAtLeast(0), inside.sumOf { it.sets }, inside.sumOf { it.volumeKg() })
    }
}

/** Newest month first, newest session first inside each month. */
fun groupByMonth(items: List<HistoryItem>, zone: ZoneId): List<Pair<YearMonth, List<HistoryItem>>> =
    items.sortedByDescending { it.startedAt }
        .groupBy { YearMonth.from(localDate(it.startedAt, zone)) }
        .toList()
        .sortedByDescending { it.first }

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
