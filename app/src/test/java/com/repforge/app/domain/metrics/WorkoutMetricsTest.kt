package com.repforge.app.domain.metrics

import com.repforge.app.domain.model.SetEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class WorkoutMetricsTest {
    private val entries = listOf(
        SetEntry(1, 1, 3, 10, 60.0, 1),
        SetEntry(2, 1, 3, 5, 70.0, 2)
    )

    @Test fun calculatesEpleyOneRepMax() {
        assertEquals(110.0, estimatedOneRepMax(100.0, 3), 0.001)
    }

    @Test fun selectsBestEntry() {
        assertEquals(81.666, bestEstimatedOneRepMax(entries), 0.001)
    }

    @Test fun personalRecordIsHeaviestLoad() {
        assertEquals(70.0, personalRecordKg(entries), 0.001)
        assertEquals(0.0, personalRecordKg(emptyList()), 0.001)
    }

    @Test fun warmupIsIgnoredByPrAndOneRepMax() {
        val withWarmup = entries + SetEntry(3, 1, 1, 1, 200.0, 3, "warmup")
        assertEquals(70.0, personalRecordKg(withWarmup), 0.001)
        assertEquals(81.666, bestEstimatedOneRepMax(withWarmup), 0.001)
    }

    @Test fun recentWindowStartsAtLocalMidnightSixDaysBack() {
        val zone = ZoneId.of("America/Sao_Paulo")
        val now = ZonedDateTime.of(2026, 10, 10, 15, 30, 0, 0, zone).toInstant().toEpochMilli()
        val expected = ZonedDateTime.of(2026, 10, 4, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(expected, recentWindowStart(now, zone))
    }

    @Test fun holdAndDistanceRecordsIgnoreWarmups() {
        val list = listOf(
            SetEntry(1, 1, 1, 1, 0.0, 1, durationSec = 40, distanceKm = 2.0),
            SetEntry(2, 1, 1, 1, 0.0, 2, durationSec = 55, distanceKm = 1.0),
            SetEntry(3, 1, 1, 1, 0.0, 3, "warmup", durationSec = 99, distanceKm = 9.0)
        )
        assertEquals(55, bestDurationSec(list))
        assertEquals(2.0, bestDistanceKm(list), 0.001)
        assertEquals(0, bestDurationSec(emptyList()))
    }
}
