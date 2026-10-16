package com.repforge.app.domain.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupersetsTest {
    @Test fun joinCreatesGroupForNeighbours() {
        assertEquals(listOf(1, 1, null), joinWithNext(listOf(null, null, null), 0))
    }

    @Test fun joinExtendsExistingGroup() {
        assertEquals(listOf(1, 1, 1), joinWithNext(listOf(1, 1, null), 1))
    }

    @Test fun joinMergesTwoGroups() {
        assertEquals(listOf(1, 1, 1, 1), joinWithNext(listOf(1, 1, 2, 2), 1))
    }

    @Test fun joinOnLastIsNoop() {
        assertEquals(listOf(null, null), joinWithNext(listOf(null, null), 1))
    }

    @Test fun separateLeavesLoneExerciseUngrouped() {
        assertEquals(listOf(null, null, null), separateAt(listOf(1, 1, null), 0))
    }

    @Test fun separateMiddleSplitsGroupIntoLonersAndDropsThem() {
        assertEquals(listOf(null, null, null), separateAt(listOf(1, null, 1), 1))
    }

    @Test fun separateMiddleOfFourKeepsBothPairsDistinct() {
        val result = separateAt(listOf(1, 1, 1, 1, 1), 2)
        assertEquals(listOf(result[0], result[0], null, result[4], result[4]), result)
        assertTrue(result[0] != result[4])
    }

    @Test fun normalizeDropsLonersAndSplitsRepeatedIds() {
        assertEquals(listOf(null, 1, 1, null), normalizeGroups(listOf(2, 1, 1, 3)))
        val result = normalizeGroups(listOf(1, 1, null, 1, 1))
        assertTrue(result[0] == result[1] && result[3] == result[4] && result[0] != result[3])
    }

    @Test fun movingOutOfGroupBreaksIt() {
        // [A:1, B:1, C] move B down -> [A, C, B]
        assertEquals(listOf(null, null, null), normalizeGroups(listOf(1, null, 1)))
    }

    @Test fun restOnlyAfterLastOfGroup() {
        val groups = listOf(null, 1, 1, 2, 2, 2)
        assertEquals(listOf(true, false, true, false, false, true), groups.indices.map { isGroupEnd(groups, it) })
        assertFalse(isGroupEnd(listOf(1, 1), 0))
    }
}
