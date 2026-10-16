package com.repforge.app.domain.progression

import com.repforge.app.domain.model.HistoryEntry
import com.repforge.app.domain.model.SET_WARMUP
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class ProgressionTest {
    private val zone = ZoneOffset.UTC
    private fun at(day: Int) = LocalDate.of(2026, 3, day).atTime(10, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
    private fun e(day: Int, reps: Int, kg: Double, type: String = "normal", sets: Int = 1) =
        HistoryEntry(1, "Supino", "A", sets, reps, kg, at(day), type)

    @Test fun emptyOrWarmupOnlyIsNull() {
        assertNull(suggestProgression(emptyList(), zone))
        assertNull(suggestProgression(listOf(e(1, 10, 20.0, SET_WARMUP)), zone))
    }

    @Test fun topOfRangeRaisesLoad() {
        val s = suggestProgression(listOf(e(2, 12, 40.0), e(2, 12, 40.0)), zone)!!
        assertEquals(ProgressionKind.RaiseLoad, s.kind)
        assertEquals(42.5, s.weightKg, 0.0)
        assertEquals(8, s.reps)
        assertEquals(12, s.topReps)
    }

    @Test fun lightLoadRaisesByOneKg() {
        assertEquals(11.0, suggestProgression(listOf(e(2, 12, 10.0)), zone)!!.weightKg, 0.0)
    }

    @Test fun belowBottomKeepsLoad() {
        val s = suggestProgression(listOf(e(2, 10, 40.0), e(2, 6, 40.0)), zone)!!
        assertEquals(ProgressionKind.KeepLoad, s.kind)
        assertEquals(40.0, s.weightKg, 0.0)
        assertEquals(8, s.reps)
    }

    @Test fun inRangeAddsOneRep() {
        val s = suggestProgression(listOf(e(2, 11, 40.0), e(2, 9, 40.0)), zone)!!
        assertEquals(ProgressionKind.AddRep, s.kind)
        assertEquals(10, s.reps)
        assertEquals(40.0, s.weightKg, 0.0)
    }

    @Test fun twoRegressionsDeloadRoundedToHalfKg() {
        // e1RM falls over three sessions: 50x10 -> 50x9 -> 50x8
        val s = suggestProgression(listOf(e(3, 8, 50.0), e(2, 9, 50.0), e(1, 10, 50.0)), zone)!!
        assertEquals(ProgressionKind.Deload, s.kind)
        assertEquals(45.0, s.weightKg, 0.0)
        assertEquals(8, s.reps)
        assertEquals(26.5, suggestProgression(listOf(e(3, 8, 29.5), e(2, 9, 29.5), e(1, 10, 29.5)), zone)!!.weightKg, 0.0)
    }

    @Test fun singleRegressionIsNotDeload() {
        val s = suggestProgression(listOf(e(3, 8, 50.0), e(2, 9, 50.0), e(1, 9, 50.0)), zone)!!
        assertEquals(ProgressionKind.AddRep, s.kind)
    }

    @Test fun warmupsIgnoredAndCustomRange() {
        val s = suggestProgression(listOf(e(2, 15, 30.0), e(2, 3, 10.0, SET_WARMUP)), zone, 10..15)!!
        assertEquals(ProgressionKind.RaiseLoad, s.kind)
        assertEquals(10, s.reps)
        assertEquals(32.5, s.weightKg, 0.0)
    }

    @Test fun sessionsAreLocalDays() {
        // 23:30 and 00:30 UTC are the same day in UTC-3? No: 20:30 and 21:30 local -> one session; UTC would split them.
        val zoneBr = java.time.ZoneId.of("America/Sao_Paulo")
        val a = HistoryEntry(1, "x", "A", 1, 12, 40.0, LocalDate.of(2026, 3, 2).atTime(23, 30).toInstant(ZoneOffset.UTC).toEpochMilli())
        val b = HistoryEntry(1, "x", "A", 1, 8, 40.0, LocalDate.of(2026, 3, 3).atTime(0, 30).toInstant(ZoneOffset.UTC).toEpochMilli())
        // same local day: min reps 8 -> add rep (not raise, which the newest-only session of 8 would also not give; 12 alone would raise)
        assertEquals(ProgressionKind.AddRep, suggestProgression(listOf(b, a), zoneBr)!!.kind)
    }
}
