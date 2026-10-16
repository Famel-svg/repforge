package com.repforge.app.domain.metrics

import com.repforge.app.domain.model.HistoryEntry
import com.repforge.app.domain.model.KIND_CARDIO
import com.repforge.app.domain.model.KIND_REPS
import com.repforge.app.domain.model.KIND_TIMED
import com.repforge.app.domain.model.SET_WARMUP
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class Consistency(val currentStreak: Int = 0, val daysLast7: Int = 0, val totalDays: Int = 0)

data class DayVolume(val date: LocalDate, val volumeKg: Double)

/** One chart point: estimated 1RM (kg) for load kinds, hold seconds for timed, distance (km) for cardio. */
data class ProgressPoint(val date: LocalDate, val value: Double)

data class WorkoutDay(val date: LocalDate, val sheetNames: List<String>, val sets: Int, val volumeKg: Double)

data class ExerciseHistory(
    val exerciseId: Long, val name: String, val sheetName: String, val lastWeightKg: Double, val lastRecordedAt: Long,
    val kind: String = KIND_REPS, val lastDurationSec: Int? = null, val lastDistanceKm: Double? = null
)

/** [prKg]/[bestOneRepMaxKg] only count load work; [bestDurationSec] is the longest hold/session, [bestDistanceKm] the longest distance. */
data class ExerciseStats(
    val prKg: Double, val bestOneRepMaxKg: Double, val totalSets: Int,
    val kind: String = KIND_REPS, val bestDurationSec: Int = 0, val bestDistanceKm: Double = 0.0
)

enum class ProgressRange(val days: Long?, val pro: Boolean) { Days30(30, false), Days90(90, true), All(null, true) }

fun ProgressRange.locked(isPro: Boolean) = pro && !isPro

/** Warmup sets never count toward stats. */
fun List<HistoryEntry>.working() = filter { it.setType != SET_WARMUP }

/** Kilograms moved; zero for holds, cardio and unloaded bodyweight sets. */
fun HistoryEntry.volumeKg() = if (loaded) sets * reps * weightKg else 0.0

fun localDate(epochMillis: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

fun trainingDays(timestamps: Collection<Long>, zone: ZoneId): Set<LocalDate> = timestamps.mapTo(HashSet()) { localDate(it, zone) }

/** Consecutive trained days ending today, or yesterday when today has no entry yet. */
fun currentStreak(days: Set<LocalDate>, today: LocalDate): Int {
    var cursor = if (today in days) today else today.minusDays(1)
    var streak = 0
    while (cursor in days) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}

fun consistency(entries: List<HistoryEntry>, now: Long, zone: ZoneId): Consistency {
    val days = trainingDays(entries.working().map { it.recordedAt }, zone)
    val today = localDate(now, zone)
    val windowStart = today.minusDays(6)
    return Consistency(currentStreak(days, today), days.count { !it.isBefore(windowStart) && !it.isAfter(today) }, days.size)
}

/** Always seven items, oldest first, zero-volume days included. */
fun last7DaysVolume(entries: List<HistoryEntry>, now: Long, zone: ZoneId): List<DayVolume> {
    val today = localDate(now, zone)
    val byDay = entries.working().groupBy { localDate(it.recordedAt, zone) }
    return (6 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong())
        DayVolume(date, byDay[date].orEmpty().sumOf { it.volumeKg() })
    }
}

fun exercisesWithHistory(entries: List<HistoryEntry>): List<ExerciseHistory> = entries.working().groupBy { it.exerciseId }.values.map { group ->
    val last = group.maxByOrNull { it.recordedAt }!!
    ExerciseHistory(last.exerciseId, last.exerciseName, last.sheetName, last.weightKg, last.recordedAt, last.kind, last.durationSec, last.distanceKm)
}.sortedByDescending { it.lastRecordedAt }

/** Best value per local day, oldest first; the metric follows the exercise kind (1RM, longest hold, longest distance). */
fun progressSeries(entries: List<HistoryEntry>, zone: ZoneId): List<ProgressPoint> {
    val work = entries.working()
    val kind = work.firstOrNull()?.kind ?: KIND_REPS
    val valued = when (kind) {
        KIND_TIMED -> work.mapNotNull { e -> e.durationSec?.takeIf { it > 0 }?.let { e to it.toDouble() } }
        KIND_CARDIO -> work.mapNotNull { e -> e.distanceKm?.takeIf { it > 0 }?.let { e to it } }
        else -> work.filter { it.reps > 0 && it.loaded }.map { it to estimatedOneRepMax(it.weightKg, it.reps) }
    }
    return valued.groupBy({ localDate(it.first.recordedAt, zone) }, { it.second })
        .map { (date, day) -> ProgressPoint(date, day.max()) }
        .sortedBy { it.date }
}

fun filterByRange(series: List<ProgressPoint>, range: ProgressRange, today: LocalDate): List<ProgressPoint> {
    val days = range.days ?: return series
    val start = today.minusDays(days - 1)
    return series.filter { !it.date.isBefore(start) }
}

fun exerciseStats(all: List<HistoryEntry>): ExerciseStats {
    val entries = all.working()
    val loaded = entries.filter { it.loaded }
    return ExerciseStats(
        prKg = loaded.maxOfOrNull { it.weightKg } ?: 0.0,
        bestOneRepMaxKg = loaded.filter { it.reps > 0 }.maxOfOrNull { estimatedOneRepMax(it.weightKg, it.reps) } ?: 0.0,
        totalSets = entries.sumOf { it.sets },
        kind = entries.firstOrNull()?.kind ?: KIND_REPS,
        bestDurationSec = entries.maxOfOrNull { it.durationSec ?: 0 } ?: 0,
        bestDistanceKm = entries.maxOfOrNull { it.distanceKm ?: 0.0 } ?: 0.0
    )
}

/** Most recent training days first. */
fun recentWorkouts(entries: List<HistoryEntry>, zone: ZoneId, limit: Int = 10): List<WorkoutDay> = entries.working()
    .sortedBy { it.recordedAt }
    .groupBy { localDate(it.recordedAt, zone) }
    .map { (date, day) ->
        WorkoutDay(date, day.map { it.sheetName }.distinct(), day.sumOf { it.sets }, day.sumOf { it.volumeKg() })
    }
    .sortedByDescending { it.date }
    .take(limit)
