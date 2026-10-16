package com.repforge.app.domain.metrics

import com.repforge.app.domain.model.HistoryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ProgressMetricsTest {
    private val sp = ZoneId.of("America/Sao_Paulo")
    private val today = LocalDate.of(2026, 10, 13)

    private fun at(date: LocalDate, hour: Int = 12, minute: Int = 0) =
        ZonedDateTime.of(date.year, date.monthValue, date.dayOfMonth, hour, minute, 0, 0, sp).toInstant().toEpochMilli()

    private fun entry(date: LocalDate, hour: Int = 12, ex: Long = 1, sets: Int = 3, reps: Int = 10, kg: Double = 50.0, sheet: String = "A") =
        HistoryEntry(ex, "Ex$ex", sheet, sets, reps, kg, at(date, hour))

    @Test fun warmupSetsAreExcludedFromStats() {
        val warm = HistoryEntry(1, "Ex1", "A", 1, 1, 300.0, at(today), "warmup")
        val list = listOf(entry(today), warm, HistoryEntry(2, "Ex2", "A", 1, 5, 20.0, at(today.minusDays(1)), "warmup"))
        assertEquals(50.0, exerciseStats(list).prKg, 0.001)
        assertEquals(3, exerciseStats(list).totalSets)
        assertEquals(1500.0, last7DaysVolume(list, at(today), sp).last().volumeKg, 0.001)
        assertEquals(1, consistency(list, at(today), sp).currentStreak)
        assertEquals(1, progressSeries(list, sp).size)
        assertEquals(1, exercisesWithHistory(list).size)
        assertEquals(3, recentWorkouts(list, sp).single().sets)
    }

    @Test fun streakEmptyIsZero() = assertEquals(0, currentStreak(emptySet(), today))

    @Test fun streakCountsFromToday() {
        assertEquals(3, currentStreak(setOf(today, today.minusDays(1), today.minusDays(2)), today))
    }

    @Test fun streakFromYesterdayWhenNothingToday() {
        assertEquals(2, currentStreak(setOf(today.minusDays(1), today.minusDays(2)), today))
    }

    @Test fun streakBreaksOnGap() {
        assertEquals(1, currentStreak(setOf(today, today.minusDays(2)), today))
        assertEquals(0, currentStreak(setOf(today.minusDays(2)), today))
    }

    @Test fun bucketsByLocalDayAcrossMidnight() {
        val late = at(today.minusDays(1), 23, 30)
        val early = at(today, 0, 30)
        assertEquals(setOf(today.minusDays(1), today), trainingDays(listOf(late, early), sp))
        assertEquals(today.minusDays(1), localDate(late, sp))
    }

    @Test fun last7DaysHasSevenItemsWithZeros() {
        val list = last7DaysVolume(
            listOf(entry(today, sets = 2, reps = 5, kg = 10.0), entry(today.minusDays(6), sets = 1, reps = 1, kg = 5.0), entry(today.minusDays(7))),
            at(today, 8), sp
        )
        assertEquals(7, list.size)
        assertEquals(today.minusDays(6), list.first().date)
        assertEquals(5.0, list.first().volumeKg, 0.001)
        assertEquals(0.0, list[3].volumeKg, 0.001)
        assertEquals(100.0, list.last().volumeKg, 0.001)
    }

    @Test fun consistencyCountsWindowAndTotal() {
        val c = consistency(listOf(entry(today), entry(today.minusDays(1)), entry(today.minusDays(10))), at(today, 8), sp)
        assertEquals(Consistency(2, 2, 3), c)
    }

    @Test fun progressSeriesPicksBestPerDay() {
        val list = listOf(
            entry(today, hour = 9, reps = 10, kg = 60.0),
            entry(today, hour = 10, reps = 3, kg = 100.0),
            entry(today.minusDays(2), reps = 1, kg = 90.0)
        )
        val series = progressSeries(list, sp)
        assertEquals(listOf(today.minusDays(2), today), series.map { it.date })
        assertEquals(90.0 * (1 + 1 / 30.0), series[0].value, 0.001)
        assertEquals(110.0, series[1].value, 0.001)
    }

    @Test fun rangeFilterKeepsWindow() {
        val series = listOf(30L, 29L, 5L, 0L).map { ProgressPoint(today.minusDays(it), 1.0) }
        assertEquals(3, filterByRange(series, ProgressRange.Days30, today).size)
        assertEquals(4, filterByRange(series, ProgressRange.All, today).size)
        assertTrue(filterByRange(emptyList(), ProgressRange.Days90, today).isEmpty())
    }

    @Test fun exerciseStatsAndListing() {
        val list = listOf(
            entry(today, ex = 1, sets = 3, kg = 80.0),
            entry(today.minusDays(3), ex = 1, sets = 2, kg = 100.0),
            entry(today.minusDays(1), ex = 2, kg = 20.0)
        )
        val stats = exerciseStats(list.filter { it.exerciseId == 1L })
        assertEquals(100.0, stats.prKg, 0.001)
        assertEquals(5, stats.totalSets)
        val exs = exercisesWithHistory(list)
        assertEquals(listOf(1L, 2L), exs.map { it.exerciseId })
        assertEquals(80.0, exs[0].lastWeightKg, 0.001)
        assertEquals(ExerciseStats(0.0, 0.0, 0), exerciseStats(emptyList()))
    }

    @Test fun recentWorkoutsGroupsDaysAndLimits() {
        val list = listOf(
            entry(today, hour = 8, sheet = "Peito", sets = 3, reps = 10, kg = 10.0),
            entry(today, hour = 18, sheet = "Costas", sets = 2, reps = 5, kg = 20.0),
            entry(today, hour = 19, sheet = "Costas", sets = 1, reps = 1, kg = 1.0)
        ) + (1..12).map { entry(today.minusDays(it.toLong())) }
        val recent = recentWorkouts(list, sp)
        assertEquals(10, recent.size)
        assertEquals(today, recent[0].date)
        assertEquals(listOf("Peito", "Costas"), recent[0].sheetNames)
        assertEquals(6, recent[0].sets)
        assertEquals(501.0, recent[0].volumeKg, 0.001)
    }

    private fun held(date: LocalDate, sec: Int, kind: String = "timed", km: Double? = null) =
        HistoryEntry(7, "Prancha", "A", 1, 1, 0.0, at(date), kind = kind, durationSec = sec, distanceKm = km)

    @Test fun timedSeriesAndStatsUseLongestHold() {
        val list = listOf(held(today, 30), held(today, 45), held(today.minusDays(2), 20))
        val series = progressSeries(list, sp)
        assertEquals(listOf(20.0, 45.0), series.map { it.value })
        val stats = exerciseStats(list)
        assertEquals("timed", stats.kind)
        assertEquals(45, stats.bestDurationSec)
        assertEquals(0.0, stats.prKg, 0.001)
        assertEquals(0.0, stats.bestOneRepMaxKg, 0.001)
        assertEquals(0.0, last7DaysVolume(list, at(today), sp).last().volumeKg, 0.001)
    }

    @Test fun cardioSeriesAndStatsUseLongestDistance() {
        val list = listOf(held(today, 1500, "cardio", 5.0), held(today.minusDays(1), 900, "cardio", 3.2), held(today.minusDays(3), 600, "cardio", null))
        assertEquals(listOf(3.2, 5.0), progressSeries(list, sp).map { it.value })
        val stats = exerciseStats(list)
        assertEquals(5.0, stats.bestDistanceKm, 0.001)
        assertEquals(1500, stats.bestDurationSec)
        assertEquals(1500, exercisesWithHistory(list).single().lastDurationSec)
    }

    @Test fun bodyweightCountsOnlyWhenLoaded() {
        fun bw(date: LocalDate, kg: Double) = HistoryEntry(8, "Barra", "A", 1, 10, kg, at(date), kind = "bodyweight")
        val plain = listOf(bw(today, 0.0))
        assertTrue(progressSeries(plain, sp).isEmpty())
        assertEquals(0.0, last7DaysVolume(plain, at(today), sp).last().volumeKg, 0.001)
        val loaded = listOf(bw(today, 0.0), bw(today.minusDays(1), 10.0))
        assertEquals(1, progressSeries(loaded, sp).size)
        assertEquals(10.0, exerciseStats(loaded).prKg, 0.001)
        assertEquals(100.0, last7DaysVolume(loaded, at(today), sp).sumOf { it.volumeKg }, 0.001)
    }
}
