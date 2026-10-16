package com.repforge.app.domain.metrics

import com.repforge.app.domain.model.HistoryEntry
import com.repforge.app.domain.model.SET_WARMUP
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class MuscleVolumeTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 3, 20)
    private val now = today.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
    private val catalog = muscleLookup(listOf("Supino reto" to MuscleInfo("Peito", listOf("Tríceps", "Ombros"))))
    private fun e(name: String, sets: Int, daysAgo: Long, target: String = "", type: String = "normal") =
        HistoryEntry(1, name, "A", sets, 10, 50.0, today.minusDays(daysAgo).atTime(9, 0).toInstant(ZoneOffset.UTC).toEpochMilli(), type, null, target)

    @Test fun primaryOneSecondaryHalfWithAccentInsensitiveMatch() {
        val r = weeklyMuscleVolume(listOf(e("supino RETO", 4, 0)), catalog, now, zone)[0]
        assertEquals(listOf(MuscleSets("Peito", 4.0), MuscleSets("Ombros", 2.0), MuscleSets("Tríceps", 2.0)), r)
    }

    @Test fun fallsBackToTargetAndSkipsBlankAndWarmup() {
        val r = weeklyMuscleVolume(listOf(e("Rosca", 3, 1, "bíceps"), e("Rosca", 2, 1, "bíceps", SET_WARMUP), e("Sem alvo", 5, 1)), catalog, now, zone)[0]
        assertEquals(listOf(MuscleSets("bíceps", 3.0)), r)
    }

    @Test fun windowsAreSevenDaysAndOldOnesDropped() {
        val h = listOf(e("Supino reto", 1, 6), e("Supino reto", 2, 7), e("Supino reto", 3, 20), e("Supino reto", 9, 28))
        val w = weeklyMuscleVolume(h, catalog, now, zone, weeks = 4)
        assertEquals(1.0, w[0][0].sets, 0.0)
        assertEquals(2.0, w[1][0].sets, 0.0)
        assertEquals(3.0, w[2][0].sets, 0.0)
        assertEquals(emptyList<MuscleSets>(), w[3])
    }

    @Test fun freeWeeksOnlyCountsCurrent() {
        assertEquals(1, weeklyMuscleVolume(listOf(e("Supino reto", 2, 10)), catalog, now, zone).size)
    }
}

class BodyMuscleTotalsTest {
    @Test fun foldsAliasesAndIgnoresOthers() {
        val r = bodyMuscleTotals(listOf(listOf(MuscleSets("Panturrilhas", 2.0), MuscleSets("peito", 3.0), MuscleSets("Cardio", 9.0)), listOf(MuscleSets("Panturrilha", 1.0))))
        assertEquals(mapOf("Panturrilha" to 3.0, "Peito" to 3.0), r)
    }
}
