package com.repforge.app.domain.metrics

import com.repforge.app.domain.model.BodyWeight
import com.repforge.app.domain.model.HistoryEntry
import com.repforge.app.domain.model.SET_WARMUP
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class HistoryListTest {
    private val zone = ZoneOffset.UTC
    private fun ms(d: LocalDate, h: Int = 9) = d.atTime(h, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
    private fun entry(at: Long, type: String = "normal") = HistoryEntry(1, "X", "A", 3, 10, 50.0, at, type)

    @Test fun daysComeFromEntriesAndAttachSession() {
        val d = LocalDate.of(2026, 3, 10)
        val s = FinishedSession(7, "A", ms(d, 9), ms(d, 10))
        val days = historyDays(listOf(s), listOf(entry(ms(d, 9)), entry(ms(d, 10), SET_WARMUP), entry(ms(d, 12))), zone)
        assertEquals(1, days.size)
        assertEquals(6, days[0].sets)
        assertEquals(3000.0, days[0].volumeKg, 0.0)
        assertEquals(3_600_000L, days[0].durationMs)
        assertEquals(7L, days[0].sessionId)
        assertEquals(listOf("A"), days[0].sheetNames)
    }

    @Test fun daysWithoutSessionStillListedWithExercises() {
        val d1 = LocalDate.of(2026, 3, 10)
        val d2 = LocalDate.of(2026, 3, 11)
        val days = historyDays(emptyList(), listOf(entry(ms(d1)), entry(ms(d2)), entry(ms(d2, 10)), entry(ms(d2), SET_WARMUP)), zone)
        assertEquals(listOf(d2, d1), days.map { it.date })
        assertNull(days[0].sessionId)
        assertNull(days[0].durationMs)
        assertEquals(1, days[0].exercises.size)
        assertEquals(6, days[0].exercises[0].sets)
        assertEquals(50.0, days[0].exercises[0].bestWeightKg, 0.0)
    }

    @Test fun groupsNewestMonthFirst() {
        fun day(d: LocalDate) = HistoryDay(d, emptyList(), 0, 0.0, null, null, emptyList())
        val g = groupByMonth(listOf(day(LocalDate.of(2026, 2, 28)), day(LocalDate.of(2026, 3, 1)), day(LocalDate.of(2026, 3, 20))))
        assertEquals(listOf(YearMonth.of(2026, 3), YearMonth.of(2026, 2)), g.map { it.first })
        assertEquals(listOf(LocalDate.of(2026, 3, 20), LocalDate.of(2026, 3, 1)), g[0].second.map { it.date })
    }

    @Test fun weightRangeFilterAndDelta() {
        val today = LocalDate.of(2026, 3, 18)
        val w = listOf(100 to 90.0, 40 to 85.0, 5 to 82.0).map { (ago, kg) -> BodyWeight(0, kg, ms(today.minusDays(ago.toLong()))) }
        val now = ms(today, 12)
        assertEquals(1, filterWeights(w, WeightRange.Days30, now, zone).size)
        assertEquals(2, filterWeights(w, WeightRange.Days90, now, zone).size)
        assertEquals(3, filterWeights(w, WeightRange.All, now, zone).size)
        assertEquals(-8.0, weightDelta(w)!!, 0.0)
        assertNull(weightDelta(w.take(1)))
    }

    @Test fun labelIndicesSpread() {
        assertEquals(emptyList<Int>(), labelIndices(0))
        assertEquals(listOf(0), labelIndices(1))
        assertEquals(listOf(0, 1), labelIndices(2))
        assertEquals(listOf(0, 2, 4), labelIndices(5))
    }
}
