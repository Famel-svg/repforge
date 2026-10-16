package com.repforge.app.domain.metrics

import com.repforge.app.domain.model.HistoryEntry
import com.repforge.app.domain.model.KIND_REPS
import com.repforge.app.domain.model.KIND_TIMED
import com.repforge.app.domain.model.SET_WARMUP
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class SessionSummaryTest {
    private val day = 86_400_000L
    private val start = 10 * day
    private val end = start + 3_600_000
    private fun e(id: Long, at: Long, reps: Int, kg: Double, kind: String = KIND_REPS, sec: Int? = null, type: String = "normal") =
        HistoryEntry(id, "Ex$id", "A", 1, reps, kg, at, type, null, "Peito", kind, sec)

    private fun summary(vararg h: HistoryEntry) = sessionSummary(h.toList(), start, end, emptyMap(), ZoneOffset.UTC)

    @Test fun onlySetsInsideTheWindowCount() {
        val s = summary(e(1, start - 1, 10, 50.0), e(1, start + 10, 10, 60.0), e(1, end + 1, 10, 70.0), e(1, start + 20, 5, 60.0, type = SET_WARMUP))
        assertEquals(1, s.sets)
        assertEquals(600.0, s.volumeKg, 0.0)
        assertEquals(3_600_000L, s.durationMs)
        assertEquals(listOf(1L to "Ex1"), s.exercises)
        assertEquals(listOf(MuscleSets("Peito", 1.0)), s.muscles)
    }

    @Test fun recordNeedsAHigherBestThanBeforeTheSession() {
        val s = summary(e(1, start - 1, 10, 50.0), e(1, start + 10, 8, 55.0), e(2, start - 1, 10, 50.0), e(2, start + 5, 10, 50.0))
        assertEquals(listOf(SessionRecord(1, "Ex1", KIND_REPS, 55.0)), s.records)
    }

    @Test fun firstEverSetIsNotARecord() {
        assertTrue(summary(e(1, start + 10, 10, 50.0)).records.isEmpty())
    }

    @Test fun timedRecordUsesLongestHold() {
        val s = summary(e(3, start - 1, 1, 0.0, KIND_TIMED, 30), e(3, start + 10, 1, 0.0, KIND_TIMED, 45))
        assertEquals(SessionRecord(3, "Ex3", KIND_TIMED, 45.0), s.records.single())
    }

    @Test fun emptySessionIsAllZero() {
        val s = summary()
        assertEquals(0, s.sets); assertTrue(s.records.isEmpty()); assertTrue(s.muscles.isEmpty())
    }
}
