package com.repforge.app.domain.metrics

import com.repforge.app.domain.model.HistoryEntry
import com.repforge.app.domain.model.KIND_BODYWEIGHT
import com.repforge.app.domain.model.KIND_CARDIO
import com.repforge.app.domain.model.KIND_REPS
import com.repforge.app.domain.model.KIND_TIMED
import java.time.ZoneId

/** [value] is kg for reps/bodyweight, seconds for timed and km for cardio, according to [kind]. */
data class SessionRecord(val exerciseId: Long, val exerciseName: String, val kind: String, val value: Double)

data class SessionSummary(
    val durationMs: Long,
    val sets: Int,
    val volumeKg: Double,
    val records: List<SessionRecord>,
    /** Exercises with at least one working set, in the order first logged. */
    val exercises: List<Pair<Long, String>>,
    val muscles: List<MuscleSets>
)

private fun HistoryEntry.recordValue(): Double = when (kind) {
    KIND_TIMED -> (durationSec ?: 0).toDouble()
    KIND_CARDIO -> distanceKm ?: 0.0
    else -> weightKg
}

/**
 * Sets belong to a session when logged in [startedAt]..[endedAt] (no schema link). A record is a best (heaviest load, longest hold,
 * longest distance) above everything logged before [startedAt]; a first-ever set has nothing to beat, as when logging.
 */
fun sessionSummary(history: List<HistoryEntry>, startedAt: Long, endedAt: Long, catalog: Map<String, MuscleInfo>, zone: ZoneId): SessionSummary {
    val work = history.working()
    val inSession = work.filter { it.recordedAt in startedAt..endedAt }.sortedBy { it.recordedAt }
    val before = work.filter { it.recordedAt < startedAt }.groupBy { it.exerciseId }
    val records = inSession.groupBy { it.exerciseId }.mapNotNull { (id, sets) ->
        val prior = before[id]?.map { it.recordValue() }?.maxOrNull() ?: return@mapNotNull null
        val best = sets.maxBy { it.recordValue() }
        // Bodyweight reps without extra load have no load record.
        if (best.recordValue() > prior && best.recordValue() > 0) SessionRecord(id, best.exerciseName, best.kind, best.recordValue()) else null
    }
    return SessionSummary(
        durationMs = (endedAt - startedAt).coerceAtLeast(0),
        sets = inSession.sumOf { it.sets },
        volumeKg = inSession.sumOf { it.volumeKg() },
        records = records,
        exercises = inSession.map { it.exerciseId to it.exerciseName }.distinct(),
        muscles = weeklyMuscleVolume(inSession, catalog, endedAt, zone).first()
    )
}
