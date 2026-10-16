package com.repforge.app.domain.schedule

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ScheduleTest {
    private val monday = LocalDate.of(2026, 6, 15)
    private val tuesday = monday.plusDays(1)
    private val weekly = mapOf(1 to 10L, 2 to null)

    @Test fun weeklyPlanApplies() {
        assertEquals(DayPlan(monday, 10L, false), resolveDay(monday, weekly, emptyMap()))
        assertEquals(DayPlan(tuesday, null, false), resolveDay(tuesday, weekly, emptyMap()))
    }

    @Test fun overrideWins() {
        assertEquals(DayPlan(monday, 20L, true), resolveDay(monday, weekly, mapOf(monday to 20L)))
    }

    @Test fun restOverrideBeatsPlan() {
        assertEquals(DayPlan(monday, null, true), resolveDay(monday, weekly, mapOf(monday to null)))
    }

    @Test fun missingPlanIsRest() {
        assertEquals(DayPlan(monday.plusDays(2), null, false), resolveDay(monday.plusDays(2), weekly, emptyMap()))
    }

    @Test fun rescheduleMovesPlannedSheet() {
        assertEquals(mapOf(monday to null, tuesday to 10L), rescheduleChanges(monday, tuesday, weekly, emptyMap()))
    }

    @Test fun rescheduleUsesEffectiveSheetOfMovedDay() {
        assertEquals(mapOf(tuesday to null, monday to 20L), rescheduleChanges(tuesday, monday, weekly, mapOf(tuesday to 20L)))
    }

    @Test fun rescheduleOfRestOrSameDayIsNoop() {
        assertEquals(emptyMap<LocalDate, Long?>(), rescheduleChanges(tuesday, monday, weekly, emptyMap()))
        assertEquals(emptyMap<LocalDate, Long?>(), rescheduleChanges(monday, monday, weekly, emptyMap()))
    }
}
