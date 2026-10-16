package com.repforge.app.domain.logging

import com.repforge.app.domain.metrics.personalRecordKg
import com.repforge.app.domain.model.SET_WARMUP
import com.repforge.app.domain.model.SetEntry
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

const val DEFAULT_REPS = 10
const val REPS_STEP = 1
const val WEIGHT_STEP_KG = 2.5

private val PtBr: Locale = Locale.forLanguageTag("pt-BR")

data class SetPrefill(val reps: Int, val weightKg: Double?)

/** [entries] newest first, as the repository emits them. */
fun prefillFor(entries: List<SetEntry>): SetPrefill =
    (entries.firstOrNull { it.setType != SET_WARMUP } ?: entries.firstOrNull())
        ?.let { SetPrefill(it.reps, it.weightKg) } ?: SetPrefill(DEFAULT_REPS, null)

/** A first-ever set has nothing to beat, so it is not announced as a record. */
fun beatsPersonalRecord(previous: List<SetEntry>, weightKg: Double): Boolean =
    previous.isNotEmpty() && weightKg > personalRecordKg(previous)

/** Entries logged on the local calendar day of [now], oldest first. */
fun entriesOnDay(entries: List<SetEntry>, now: Long, zone: ZoneId): List<SetEntry> {
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    return entries.filter { Instant.ofEpochMilli(it.recordedAt).atZone(zone).toLocalDate() == today }
        .sortedWith(compareBy({ it.recordedAt }, { it.id }))
}

fun stepReps(current: Int?, delta: Int): Int = ((current ?: 0) + delta).coerceAtLeast(1)

fun stepWeightKg(current: Double?, delta: Double): Double = ((current ?: 0.0) + delta).coerceAtLeast(0.0)

/** Ids in display order after moving [id] one slot; null when it is absent or already at that end. */
fun movedOrder(orderedIds: List<Long>, id: Long, up: Boolean): List<Long>? {
    val from = orderedIds.indexOf(id)
    val to = if (up) from - 1 else from + 1
    if (from < 0 || to !in orderedIds.indices) return null
    return orderedIds.toMutableList().apply { this[from] = this[to]; this[to] = id }
}

fun formatKg(weightKg: Double): String = "%.1f kg".format(PtBr, weightKg)

/** Text for the load field: "80" or "82,5". */
fun formatWeightInput(weightKg: Double): String =
    if (weightKg % 1.0 == 0.0) weightKg.toLong().toString() else "%.1f".format(PtBr, weightKg)

/** "10 × 80,0 kg", or "3 × 10 · 80,0 kg" for older aggregated rows. */
fun formatSet(entry: SetEntry): String =
    if (entry.sets > 1) "${entry.sets} × ${entry.reps} · ${formatKg(entry.weightKg)}"
    else "${entry.reps} × ${formatKg(entry.weightKg)}"
