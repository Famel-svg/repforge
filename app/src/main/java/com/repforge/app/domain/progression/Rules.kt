package com.repforge.app.domain.progression

import com.repforge.app.domain.metrics.localDate
import com.repforge.app.domain.metrics.working
import com.repforge.app.domain.model.HistoryEntry
import com.repforge.app.domain.model.KIND_BODYWEIGHT
import com.repforge.app.domain.model.KIND_CARDIO
import com.repforge.app.domain.model.KIND_TIMED
import java.time.ZoneId

const val RULE_NONE = "none"
const val RULE_LINEAR = "linear"
const val RULE_DOUBLE = "double"
const val RULE_TIME = "time"
val PROGRESSION_RULES = listOf(RULE_NONE, RULE_LINEAR, RULE_DOUBLE, RULE_TIME)

const val LINEAR_GOAL_REPS = 8
const val LINEAR_FAILS_BEFORE_DELOAD = 3
const val TIME_STEP_SEC = 5
const val EXTRA_LOAD_START_KG = 2.5

/** Exercise override wins over the sheet's rule; unknown values (hand-edited data) fall back to the next level, then to double. */
fun effectiveRule(exerciseRule: String?, sheetRule: String?): String =
    exerciseRule?.takeIf { it in PROGRESSION_RULES } ?: sheetRule?.takeIf { it in PROGRESSION_RULES } ?: RULE_DOUBLE

private fun linearStep(load: Double) = if (load < 20.0) 1.25 else 2.5

/** Rounded to the nearest 0.5 kg, like the double-progression deload. */
private fun deloadLoad(load: Double) = Math.round(load * 0.9 * 2) / 2.0

/**
 * Next-session suggestion for one exercise. [history] is that exercise's entries (any order); warmups are ignored and sessions are
 * local days in [zone]. Null when the rule is none, the kind is cardio, or there is no working set yet.
 * A session that misses its rep target never raises the load. Timed exercises always use the hold-time engine
 * (so the default "double" sheet rule still helps them); the "time" rule on a rep exercise behaves as double.
 */
fun suggestNext(rule: String, kind: String, history: List<HistoryEntry>, zone: ZoneId, range: IntRange = 8..12): ProgressionSuggestion? {
    if (rule == RULE_NONE || kind == KIND_CARDIO) return null
    if (kind == KIND_TIMED) return suggestTime(history, zone)
    val sessions = sessionsNewestFirst(history, zone)
    val last = sessions.firstOrNull() ?: return null
    if (kind == KIND_BODYWEIGHT && last.maxOf { it.weightKg } == 0.0) return suggestBodyweightReps(last, range)
    return if (rule == RULE_LINEAR) suggestLinear(sessions) else suggestProgression(history, zone, range)
}

private fun sessionsNewestFirst(history: List<HistoryEntry>, zone: ZoneId) =
    history.working().groupBy { localDate(it.recordedAt, zone) }.toSortedMap(reverseOrder()).values.toList()

/** Reps first (up to the top of the range); once every set reaches it, start adding load. */
private fun suggestBodyweightReps(last: List<HistoryEntry>, range: IntRange): ProgressionSuggestion {
    val minReps = last.minOf { it.reps }
    return when {
        minReps >= range.last -> ProgressionSuggestion(range.first, EXTRA_LOAD_START_KG, ProgressionKind.ExtraLoad, range.last)
        minReps < range.first -> ProgressionSuggestion(range.first, 0.0, ProgressionKind.KeepLoad)
        else -> ProgressionSuggestion(minReps + 1, 0.0, ProgressionKind.AddRep)
    }
}

/** Every set at the goal -> step up; otherwise repeat the load; [LINEAR_FAILS_BEFORE_DELOAD] misses in a row -> -10%. */
private fun suggestLinear(sessions: List<List<HistoryEntry>>, goal: Int = LINEAR_GOAL_REPS): ProgressionSuggestion {
    val load = sessions.first().maxOf { it.weightKg }
    val failures = sessions.takeWhile { s -> s.minOf { it.reps } < goal }.size
    return when {
        failures >= LINEAR_FAILS_BEFORE_DELOAD -> ProgressionSuggestion(goal, deloadLoad(load), ProgressionKind.Deload, goal)
        failures == 0 -> ProgressionSuggestion(goal, load + linearStep(load), ProgressionKind.RaiseLoad, goal)
        else -> ProgressionSuggestion(goal, load, ProgressionKind.KeepLoad)
    }
}

/**
 * Hold time. The first session sets the baseline; each next session must reach the standing target
 * (previous hit + [TIME_STEP_SEC]) to move it up. Replays the sessions oldest to newest, so a miss keeps the target.
 */
private fun suggestTime(history: List<HistoryEntry>, zone: ZoneId): ProgressionSuggestion? {
    val holds = sessionsNewestFirst(history, zone).reversed().mapNotNull { s -> s.mapNotNull { it.durationSec }.minOrNull() }
    if (holds.isEmpty()) return null
    var target: Int? = null
    var hit = true
    holds.forEach { hold ->
        hit = target == null || hold >= target!!
        if (hit) target = hold + TIME_STEP_SEC
    }
    return ProgressionSuggestion(1, 0.0, if (hit) ProgressionKind.AddTime else ProgressionKind.KeepTime, durationSec = target)
}
