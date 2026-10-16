package com.repforge.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class BackupDedupeTest {
    private fun entry(at: Long, reps: Int = 5, kg: Double = 50.0) = BackupEntry(1, reps, kg, at)

    @Test
    fun skipsOnlyEntriesThatAlreadyExist() {
        val existing = setOf(entryKey(entry(1)), entryKey(entry(2, kg = 60.0)))
        val (fresh, old) = partitionNew(listOf(entry(1), entry(2), entry(3)), existing, ::entryKey)
        assertEquals(listOf(2L, 3L), fresh.map { it.recordedAt })
        assertEquals(listOf(1L), old.map { it.recordedAt })
    }

    @Test
    fun keepsIdenticalRowsInsideTheSameFile() {
        val (fresh, old) = partitionNew(listOf(entry(1), entry(1), entry(1)), emptySet<Triple<Long, Int, Double>>(), ::entryKey)
        assertEquals(3, fresh.size)
        assertEquals(0, old.size)
    }
}
