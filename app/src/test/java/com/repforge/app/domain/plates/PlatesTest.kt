package com.repforge.app.domain.plates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatesTest {
    @Test fun exactLoad() {
        val r = calculatePlates(100.0)
        assertEquals(listOf(25.0, 15.0), r.perSideKg)
        assertTrue(r.exact)
        assertEquals(100.0, r.loadKg, 0.0)
    }

    @Test fun smallPlatesAreExact() {
        val r = calculatePlates(62.5)
        assertEquals(listOf(20.0, 1.25), r.perSideKg)
        assertTrue(r.exact)
    }

    @Test fun impossibleLoadRoundsDown() {
        val r = calculatePlates(61.0)
        assertEquals(listOf(20.0), r.perSideKg)
        assertFalse(r.exact)
        assertEquals(60.0, r.loadKg, 0.0)
    }

    @Test fun belowBarIsEmptyBar() {
        val r = calculatePlates(10.0, barKg = 15.0)
        assertTrue(r.perSideKg.isEmpty())
        assertEquals(15.0, r.loadKg, 0.0)
        assertFalse(r.exact)
    }

    @Test fun lighterBarAndRepeatedPlates() {
        val r = calculatePlates(150.0, barKg = 10.0)
        assertEquals(listOf(25.0, 25.0, 20.0), r.perSideKg)
        assertEquals(150.0, r.loadKg, 0.0)
    }
}
