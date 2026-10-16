package com.repforge.app.domain.metrics

import com.repforge.app.domain.model.HistoryEntry
import com.repforge.app.domain.model.SET_WARMUP
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

class HeatmapTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 3, 18) // Wednesday
    private val now = today.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
    private fun e(sets: Int, daysAgo: Long, type: String = "normal") =
        HistoryEntry(1, "X", "A", sets, 10, 50.0, today.minusDays(daysAgo).atTime(9, 0).toInstant(ZoneOffset.UTC).toEpochMilli(), type)

    @Test fun shapeMondayFirstAndFutureNull() {
        val h = activityHeatmap(emptyList(), now, zone)
        assertEquals(52, h.weeks.size)
        assertEquals(DayOfWeek.MONDAY, h.weeks[0][0]!!.date.dayOfWeek)
        assertEquals(today, h.weeks.last()[2]!!.date)
        assertNull(h.weeks.last()[3])
    }

    @Test fun bucketsSumSetsSkipWarmupAndOldDays() {
        val h = activityHeatmap(listOf(e(3, 0), e(4, 0), e(9, 0, SET_WARMUP), e(15, 1), e(2, 400)), now, zone)
        val last = h.weeks.last()
        assertEquals(7, last[2]!!.sets)
        assertEquals(2, last[2]!!.level)
        assertEquals(4, last[1]!!.level)
        assertEquals(2, h.trainedDays)
    }

    @Test fun levels() {
        assertEquals(listOf(0, 1, 1, 2, 2, 3, 3, 4), listOf(0, 1, 4, 5, 9, 10, 14, 15).map(::heatLevel))
    }
}
