package com.repforge.app.presentation.workout

import com.repforge.app.domain.logging.SetPrefill
import com.repforge.app.domain.model.SetEntry
import java.time.Instant
import java.time.ZoneId

const val DEFAULT_PLANNED_SETS = 3

/** Sets of the most recent calendar day before today that has entries, oldest first. [entries] may be in any order. */
fun previousSessionSets(entries: List<SetEntry>, now: Long, zone: ZoneId): List<SetEntry> {
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val byDay = entries.groupBy { Instant.ofEpochMilli(it.recordedAt).atZone(zone).toLocalDate() }.filterKeys { it < today }
    val last = byDay.keys.maxOrNull() ?: return emptyList()
    return byDay.getValue(last).sortedWith(compareBy({ it.recordedAt }, { it.id }))
}

/** Rows planned for today: as many sets as the last session had, else [fallback]. */
fun plannedSetCount(previous: List<SetEntry>, fallback: Int = DEFAULT_PLANNED_SETS): Int = previous.size.takeIf { it > 0 } ?: fallback

/** Total rows shown: the plan plus rows the user added, never fewer than the sets already done. */
fun totalRows(done: Int, planned: Int, extra: Int): Int = maxOf(done, planned + extra)

/**
 * Starting values of the pending row at [position] (0-based). Before anything is logged today it mirrors the same set of the last
 * session; afterwards it follows the latest logged set, like the old single-form flow.
 */
fun defaultsFor(position: Int, doneToday: Int, previous: List<SetEntry>, latest: SetPrefill): SetPrefill {
    if (doneToday > 0) return latest
    return previous.getOrNull(position)?.let { SetPrefill(it.reps, it.weightKg, it.durationSec, it.distanceKm) } ?: latest
}
