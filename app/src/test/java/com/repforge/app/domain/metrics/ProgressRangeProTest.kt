package com.repforge.app.domain.metrics

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressRangeProTest {
    @Test
    fun onlyLongRangesAreProGated() {
        assertEquals(listOf(ProgressRange.Days90, ProgressRange.All), ProgressRange.entries.filter { it.locked(false) })
        assertEquals(emptyList<ProgressRange>(), ProgressRange.entries.filter { it.locked(true) })
    }
}
