package com.repforge.app.presentation.home

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakWeeksTest {
    private val monday = LocalDate.of(2026, 10, 12)

    @Test fun currentWeekInProgressDoesNotBreakStreak() {
        val counts = mapOf(monday to 1, monday.minusWeeks(1) to 3, monday.minusWeeks(2) to 4)
        assertEquals(2, streakWeeks(counts, 3, monday))
    }

    @Test fun currentWeekMetCountsAndGapStops() {
        val counts = mapOf(monday to 3, monday.minusWeeks(1) to 3, monday.minusWeeks(3) to 5)
        assertEquals(2, streakWeeks(counts, 3, monday))
        assertEquals(0, streakWeeks(emptyMap(), 3, monday))
    }
}
