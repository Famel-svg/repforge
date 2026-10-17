package com.repforge.app.domain.logging

import com.repforge.app.domain.metrics.bestDistanceKm
import com.repforge.app.domain.metrics.bestDurationSec
import com.repforge.app.domain.metrics.personalRecordKg
import com.repforge.app.domain.model.KIND_BODYWEIGHT
import com.repforge.app.domain.model.KIND_CARDIO
import com.repforge.app.domain.model.KIND_REPS
import com.repforge.app.domain.model.KIND_TIMED
import com.repforge.app.domain.model.SET_WARMUP
import com.repforge.app.domain.model.SetEntry
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

const val DEFAULT_REPS = 10
const val REPS_STEP = 1
const val WEIGHT_STEP_KG = 2.5

data class SetPrefill(val reps: Int, val weightKg: Double?, val durationSec: Int? = null, val distanceKm: Double? = null)

/** [entries] newest first, as the repository emits them. */
fun prefillFor(entries: List<SetEntry>): SetPrefill =
    (entries.firstOrNull { it.setType != SET_WARMUP } ?: entries.firstOrNull())
        ?.let { SetPrefill(it.reps, it.weightKg, it.durationSec, it.distanceKm) } ?: SetPrefill(DEFAULT_REPS, null)

/** A first-ever set has nothing to beat, so it is not announced as a record. */
fun beatsPersonalRecord(previous: List<SetEntry>, weightKg: Double): Boolean =
    previous.isNotEmpty() && weightKg > personalRecordKg(previous)

/** Whether the new set beats the stored record for the kind: heaviest load, longest hold or longest distance. */
fun beatsKindRecord(kind: String, previous: List<SetEntry>, weightKg: Double, durationSec: Int?, distanceKm: Double?): Boolean = previous.isNotEmpty() && when (kind) {
    KIND_TIMED -> (durationSec ?: 0) > bestDurationSec(previous)
    KIND_CARDIO -> (distanceKm ?: 0.0) > bestDistanceKm(previous)
    KIND_BODYWEIGHT -> weightKg > 0 && weightKg > personalRecordKg(previous)
    else -> beatsPersonalRecord(previous, weightKg)
}

/** Entries logged on the local calendar day of [now], oldest first. */
fun entriesOnDay(entries: List<SetEntry>, now: Long, zone: ZoneId): List<SetEntry> {
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    return entries.filter { Instant.ofEpochMilli(it.recordedAt).atZone(zone).toLocalDate() == today }
        .sortedWith(compareBy({ it.recordedAt }, { it.id }))
}

fun stepReps(current: Int?, delta: Int): Int = ((current ?: 0) + delta).coerceAtLeast(1)

fun stepWeightKg(current: Double?, delta: Double): Double = roundKg(((current ?: 0.0) + delta).coerceAtLeast(0.0))

/** Rounds to 0.01 (decimal HALF_UP, so 72.5 stays 72.5 and no binary-float noise leaks into storage). */
fun roundKg(kg: Double): Double = java.math.BigDecimal.valueOf(kg).setScale(2, java.math.RoundingMode.HALF_UP).toDouble()

/** Parses a typed load ("72.5" or "72,5") to at most 0.01 kg; null when malformed. */
fun parseLoadKg(text: String): Double? = com.repforge.app.domain.validation.parseWeightKg(text)?.let(::roundKg)

/** Ids in display order after moving [id] one slot; null when it is absent or already at that end. */
fun movedOrder(orderedIds: List<Long>, id: Long, up: Boolean): List<Long>? {
    val from = orderedIds.indexOf(id)
    val to = if (up) from - 1 else from + 1
    if (from < 0 || to !in orderedIds.indices) return null
    return orderedIds.toMutableList().apply { this[from] = this[to]; this[to] = id }
}

/** Numbers follow [locale] (device/app locale by default): "80,5" in pt-BR, "80.5" in en. */
fun formatKg(weightKg: Double, locale: Locale = Locale.getDefault()): String =
    (if (roundKg(weightKg) * 10 % 1.0 == 0.0) "%.1f kg" else "%.2f kg").format(locale, weightKg)

/** "m:ss", or "h:mm:ss" from one hour; whole seconds. */
fun formatDuration(totalSec: Int): String {
    val s = totalSec.coerceAtLeast(0)
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s % 3600 / 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
}

/** Accepts "90", "1:30" and "1:02:03"; null when malformed or negative. */
fun parseDuration(text: String): Int? {
    val parts = text.trim().split(':')
    if (text.isBlank() || parts.size > 3) return null
    val nums = parts.map { it.trim().toIntOrNull()?.takeIf { n -> n >= 0 } ?: return null }
    return nums.fold(0) { acc, n -> acc * 60 + n }
}

fun formatDistanceKm(km: Double, locale: Locale = Locale.getDefault()): String =
        java.text.NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 2 }.format(km) + " km"

/** Text for the distance field: "5" or "3,25" (at most two decimals). */
fun formatDistanceInput(km: Double, locale: Locale = Locale.getDefault()): String =
    if (km % 1.0 == 0.0) km.toLong().toString() else "%.2f".format(locale, km).trimEnd('0').trimEnd('.', ',')

/** Text for the load field: "80" or "82,5". */
fun formatWeightInput(weightKg: Double, locale: Locale = Locale.getDefault()): String =
    if (weightKg % 1.0 == 0.0) weightKg.toLong().toString() else "%.2f".format(locale, weightKg).trimEnd('0')

/**
 * Per-kind set text: reps "10 × 80,0 kg" (or "3 × 10 · 80,0 kg" for older aggregated rows), bodyweight "10×" / "10× + 5,0 kg",
 * timed "0:45", cardio "12:30 · 3,2 km". [perSideLabel] (e.g. "lado") is non-null for per-side exercises: "10/lado × 80,0 kg".
 */
fun formatSet(entry: SetEntry, locale: Locale = Locale.getDefault(), kind: String = KIND_REPS, perSideLabel: String? = null): String {
    val side = perSideLabel?.let { "/$it" }.orEmpty()
    return when (kind) {
        KIND_TIMED -> formatDuration(entry.durationSec ?: 0) + side
        KIND_CARDIO -> listOfNotNull(entry.durationSec?.let(::formatDuration), entry.distanceKm?.let { formatDistanceKm(it, locale) }).joinToString(" · ").ifEmpty { "-" }
        KIND_BODYWEIGHT -> "${entry.reps}$side×" + if (entry.weightKg > 0) " + ${formatKg(entry.weightKg, locale)}" else ""
        else -> if (entry.sets > 1) "${entry.sets} × ${entry.reps}$side · ${formatKg(entry.weightKg, locale)}" else "${entry.reps}$side × ${formatKg(entry.weightKg, locale)}"
    }
}
