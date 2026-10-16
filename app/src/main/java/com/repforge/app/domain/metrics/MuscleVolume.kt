package com.repforge.app.domain.metrics

import com.repforge.app.domain.model.HistoryEntry
import java.text.Normalizer
import java.time.ZoneId

data class MuscleInfo(val primary: String, val secondary: List<String>)

data class MuscleSets(val muscle: String, val sets: Double)

private val MARKS = Regex("\\p{M}+")
private fun key(value: String) = MARKS.replace(Normalizer.normalize(value.trim(), Normalizer.Form.NFD), "").lowercase()

/** Catalog lookup keyed by accent/case-insensitive exercise name. */
fun muscleLookup(items: Iterable<Pair<String, MuscleInfo>>): Map<String, MuscleInfo> = items.associate { key(it.first) to it.second }

/**
 * Weekly sets per muscle for [weeks] consecutive 7-day windows ending today (index 0 = last 7 days).
 * Primary muscle counts 1 per set, each secondary 0.5. A catalog match on the exercise name wins; otherwise
 * the free-text [HistoryEntry.target] is the primary muscle with no secondaries. Warmups excluded.
 */
fun weeklyMuscleVolume(history: List<HistoryEntry>, catalog: Map<String, MuscleInfo>, now: Long, zone: ZoneId, weeks: Int = 1): List<List<MuscleSets>> {
    val today = localDate(now, zone)
    val totals = List(weeks) { HashMap<String, Double>() }
    val labels = HashMap<String, String>()
    fun add(week: Int, muscle: String, sets: Double) {
        val k = key(muscle).ifEmpty { return }
        labels.getOrPut(k) { muscle.trim() }
        totals[week].merge(k, sets, Double::plus)
    }
    for (e in history.working()) {
        val week = (java.time.temporal.ChronoUnit.DAYS.between(localDate(e.recordedAt, zone), today) / 7).toInt()
        if (week !in 0 until weeks || localDate(e.recordedAt, zone).isAfter(today)) continue
        val info = catalog[key(e.exerciseName)] ?: MuscleInfo(e.target, emptyList())
        add(week, info.primary, e.sets.toDouble())
        info.secondary.forEach { add(week, it, e.sets * 0.5) }
    }
    return totals.map { t -> t.map { (k, v) -> MuscleSets(labels.getValue(k), v) }.sortedWith(compareByDescending<MuscleSets> { it.sets }.thenBy { it.muscle }) }
}
