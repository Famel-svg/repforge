package com.repforge.app.domain.metrics

import com.repforge.app.domain.model.SetEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutMetricsTest {
    @Test fun calculatesEpleyOneRepMax() {
        assertEquals(110.0, estimatedOneRepMax(100.0, 3), 0.001)
    }

    @Test fun selectsBestEntry() {
        val entries = listOf(
            SetEntry(1, 1, 3, 10, 60.0, 1),
            SetEntry(2, 1, 3, 5, 70.0, 2)
        )
        assertEquals(81.666, bestEstimatedOneRepMax(entries), 0.001)
    }
}
