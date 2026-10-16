package com.repforge.app.domain.schedule

import java.time.LocalDate

/** [sheetId] null = rest day; [moved] = a one-off override decided this day instead of the weekly plan. */
data class DayPlan(val date: LocalDate, val sheetId: Long?, val moved: Boolean)

/** [weekly] is keyed by ISO day of week (1 = Monday); [overrides] wins even when it maps to null (explicit rest). */
fun resolveDay(date: LocalDate, weekly: Map<Int, Long?>, overrides: Map<LocalDate, Long?>): DayPlan =
    if (date in overrides) DayPlan(date, overrides[date], true) else DayPlan(date, weekly[date.dayOfWeek.value], false)

/** Overrides that move the sheet planned on [from] to [to] (from becomes rest); empty when there is nothing to move. */
fun rescheduleChanges(from: LocalDate, to: LocalDate, weekly: Map<Int, Long?>, overrides: Map<LocalDate, Long?>): Map<LocalDate, Long?> {
    val sheetId = resolveDay(from, weekly, overrides).sheetId
    return if (from == to || sheetId == null) emptyMap() else mapOf(from to null, to to sheetId)
}
