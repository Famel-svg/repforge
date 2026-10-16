package com.repforge.app.domain.progression

import com.repforge.app.domain.metrics.localDate
import com.repforge.app.domain.metrics.working
import com.repforge.app.domain.model.HistoryEntry
import java.time.ZoneId

enum class ProgressionKind { RaiseLoad, KeepLoad, AddRep, Deload }

data class ProgressionSuggestion(val reps: Int, val weightKg: Double, val kind: ProgressionKind, val topReps: Int = 0)

/** Epley estimate of the session's best set; used only to compare sessions with each other. */
private fun sessionScore(sets: List<HistoryEntry>) = sets.maxOf { it.weightKg * (1 + it.reps / 30.0) }

/**
 * Double progression. [history] is one exercise's entries, newest first; warmups are ignored and
 * sessions are local days in [zone]. Returns null when there is no working set to build on.
 * shortcut: "regressed" = lower best-set e1RM than the previous session, so deload needs 3 sessions.
 */
fun suggestProgression(history: List<HistoryEntry>, zone: ZoneId, range: IntRange = 8..12): ProgressionSuggestion? {
    val sessions = history.working().groupBy { localDate(it.recordedAt, zone) }.toSortedMap(reverseOrder()).values.toList()
    val last = sessions.firstOrNull() ?: return null
    val load = last.maxOf { it.weightKg }
    val bottom = range.first
    val top = range.last
    val minReps = last.minOf { it.reps }
    val regressed = sessions.size >= 3 &&
        sessionScore(sessions[0]) < sessionScore(sessions[1]) && sessionScore(sessions[1]) < sessionScore(sessions[2])
    return when {
        regressed -> {
            val deload = Math.round(load * 0.9 * 2) / 2.0
            ProgressionSuggestion(bottom, deload, ProgressionKind.Deload)
        }
        minReps >= top -> {
            val next = load + if (load < 20.0) 1.0 else 2.5
            ProgressionSuggestion(bottom, next, ProgressionKind.RaiseLoad, top)
        }
        minReps < bottom ->
            ProgressionSuggestion(bottom, load, ProgressionKind.KeepLoad)
        else ->
            ProgressionSuggestion(minReps + 1, load, ProgressionKind.AddRep)
    }
}
