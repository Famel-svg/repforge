package com.repforge.app.domain.metrics

import com.repforge.app.domain.model.SET_WARMUP
import com.repforge.app.domain.model.SetEntry
import java.time.Instant
import java.time.ZoneId

fun estimatedOneRepMax(weightKg: Double, reps: Int): Double {
    require(weightKg >= 0) { "Carga não pode ser negativa." }
    require(reps > 0) { "Repetições devem ser maiores que zero." }
    return weightKg * (1.0 + reps / 30.0)
}

fun bestEstimatedOneRepMax(entries: List<SetEntry>): Double = entries.filter { it.setType != SET_WARMUP }.maxOfOrNull {
    estimatedOneRepMax(it.weightKg, it.reps)
} ?: 0.0

fun personalRecordKg(entries: List<SetEntry>): Double = entries.filter { it.setType != SET_WARMUP }.maxOfOrNull { it.weightKg } ?: 0.0

/** Start of the 7-calendar-day window (today plus the six previous local days). */
fun recentWindowStart(now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Long =
    Instant.ofEpochMilli(now).atZone(zone).toLocalDate().minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()

/** Longest hold or session, in seconds (timed/cardio records). */
fun bestDurationSec(entries: List<SetEntry>): Int = entries.filter { it.setType != SET_WARMUP }.maxOfOrNull { it.durationSec ?: 0 } ?: 0

/** Longest distance in a single entry (cardio record). */
fun bestDistanceKm(entries: List<SetEntry>): Double = entries.filter { it.setType != SET_WARMUP }.maxOfOrNull { it.distanceKm ?: 0.0 } ?: 0.0
