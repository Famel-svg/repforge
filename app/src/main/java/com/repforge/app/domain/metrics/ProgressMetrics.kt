package com.repforge.app.domain.metrics

import com.repforge.app.domain.model.HistoryEntry
import com.repforge.app.domain.model.SET_WARMUP
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class Consistency(val currentStreak: Int = 0, val daysLast7: Int = 0, val totalDays: Int = 0)

data class DayVolume(val date: LocalDate, val volumeKg: Double)

data class OneRepMaxPoint(val date: LocalDate, val oneRepMaxKg: Double)

data class WorkoutDay(val date: LocalDate, val sheetNames: List<String>, val sets: Int, val volumeKg: Double)

data class ExerciseHistory(val exerciseId: Long, val name: String, val sheetName: String, val lastWeightKg: Double, val lastRecordedAt: Long)

data class ExerciseStats(val prKg: Double, val bestOneRepMaxKg: Double, val totalSets: Int)

enum class ProgressRange(val days: Long?, val pro: Boolean) { Days30(30, false), Days90(90, true), All(null, true) }

fun ProgressRange.locked(isPro: Boolean) = pro && !isPro

/** Warmup sets never count toward stats. */
fun List<HistoryEntry>.working() = filter { it.setType != SET_WARMUP }

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
        DayVolume(date, byDay[date].orEmpty().sumOf { it.sets * it.reps * it.weightKg })
    }
}

fun exercisesWithHistory(entries: List<HistoryEntry>): List<ExerciseHistory> = entries.working().groupBy { it.exerciseId }.values.map { group ->
    val last = group.maxByOrNull { it.recordedAt }!!
    ExerciseHistory(last.exerciseId, last.exerciseName, last.sheetName, last.weightKg, last.recordedAt)
}.sortedByDescending { it.lastRecordedAt }

/** Best estimated 1RM per local day, oldest first. */
fun oneRepMaxSeries(entries: List<HistoryEntry>, zone: ZoneId): List<OneRepMaxPoint> = entries.working()
    .filter { it.reps > 0 && it.weightKg >= 0 }
    .groupBy { localDate(it.recordedAt, zone) }
    .map { (date, day) -> OneRepMaxPoint(date, day.maxOf { estimatedOneRepMax(it.weightKg, it.reps) }) }
    .sortedBy { it.date }

fun filterByRange(series: List<OneRepMaxPoint>, range: ProgressRange, today: LocalDate): List<OneRepMaxPoint> {
    val days = range.days ?: return series
    val start = today.minusDays(days - 1)
    return series.filter { !it.date.isBefore(start) }
}

fun exerciseStats(all: List<HistoryEntry>): ExerciseStats {
    val entries = all.working()
    return ExerciseStats(
        prKg = entries.maxOfOrNull { it.weightKg } ?: 0.0,
        bestOneRepMaxKg = entries.filter { it.reps > 0 }.maxOfOrNull { estimatedOneRepMax(it.weightKg, it.reps) } ?: 0.0,
        totalSets = entries.sumOf { it.sets }
    )
}

/** Most recent training days first. */
fun recentWorkouts(entries: List<HistoryEntry>, zone: ZoneId, limit: Int = 10): List<WorkoutDay> = entries.working()
    .sortedBy { it.recordedAt }
    .groupBy { localDate(it.recordedAt, zone) }
    .map { (date, day) ->
        WorkoutDay(date, day.map { it.sheetName }.distinct(), day.sumOf { it.sets }, day.sumOf { it.sets * it.reps * it.weightKg })
    }
    .sortedByDescending { it.date }
    .take(limit)
