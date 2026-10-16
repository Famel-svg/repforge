package com.repforge.app.domain.metrics

import com.repforge.app.domain.model.HistoryEntry
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class HeatmapCell(val date: LocalDate, val sets: Int, val level: Int)

/** [weeks] columns, Monday-first, 7 rows each; days after today are null. [trainedDays] counts days with working sets. */
data class Heatmap(val weeks: List<List<HeatmapCell?>>, val trainedDays: Int)

/** 0 = none, then 1-4, 5-9, 10-14, 15+ working sets per day. */
fun heatLevel(sets: Int) = when {
    sets <= 0 -> 0
    sets < 5 -> 1
    sets < 10 -> 2
    sets < 15 -> 3
    else -> 4
}

fun activityHeatmap(history: List<HistoryEntry>, now: Long, zone: ZoneId, weeks: Int = 52): Heatmap {
    val today = localDate(now, zone)
    val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(weeks - 1L)
    val perDay = HashMap<LocalDate, Int>()
    history.working().forEach { e ->
        val d = localDate(e.recordedAt, zone)
        if (!d.isBefore(start) && !d.isAfter(today)) perDay.merge(d, e.sets, Int::plus)
    }
    val columns = List(weeks) { w ->
        List(7) { r ->
            val d = start.plusDays(w * 7L + r)
            if (d.isAfter(today)) null else perDay[d].let { s -> HeatmapCell(d, s ?: 0, heatLevel(s ?: 0)) }
        }
    }
    return Heatmap(columns, perDay.count { it.value > 0 })
}
