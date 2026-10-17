package com.repforge.app.presentation.workout

import com.repforge.app.domain.logging.SetPrefill
import com.repforge.app.domain.model.SetEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SetRowsTest {
    private val zone = ZoneId.of("UTC")
    private fun at(day: LocalDate, hour: Int = 10) = day.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
    private fun entry(id: Long, day: LocalDate, reps: Int = 10, kg: Double = 50.0) = SetEntry(id, 1, 1, reps, kg, at(day, 8 + id.toInt() % 10))
    private val today = LocalDate.of(2026, 3, 10)
    private val now = at(today, 18)

    @Test fun usesLatestPriorDayOnly() {
        val entries = listOf(entry(1, today.minusDays(9)), entry(2, today.minusDays(2)), entry(3, today.minusDays(2)), entry(4, today))
        assertEquals(listOf(2L, 3L), previousSessionSets(entries, now, zone).map { it.id })
    }

    @Test fun fallsBackToThreeWithoutHistory() {
        assertEquals(3, plannedSetCount(previousSessionSets(listOf(entry(1, today)), now, zone)))
        assertEquals(4, plannedSetCount(previousSessionSets((1L..4L).map { entry(it, today.minusDays(3)) }, now, zone)))
    }

    @Test fun totalRowsNeverBelowDone() {
        assertEquals(3, totalRows(0, 3, 0))
        assertEquals(5, totalRows(0, 3, 2))
        assertEquals(6, totalRows(6, 3, 0))
    }

    @Test fun defaultsMirrorLastSessionThenFollowLatest() {
        val prev = listOf(entry(1, today.minusDays(1), 8, 60.0), entry(2, today.minusDays(1), 6, 70.0))
        val latest = SetPrefill(12, 40.0)
        assertEquals(SetPrefill(6, 70.0), defaultsFor(1, 0, prev, latest))
        assertEquals(latest, defaultsFor(2, 0, prev, latest))
        assertEquals(latest, defaultsFor(1, 1, prev, latest))
    }
}
