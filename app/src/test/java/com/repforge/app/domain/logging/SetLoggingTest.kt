package com.repforge.app.domain.logging

import com.repforge.app.domain.model.SET_WARMUP
import com.repforge.app.domain.model.SetEntry
import com.repforge.app.domain.templates.StarterTemplates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class SetLoggingTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private fun at(h: Int, day: Int = 10) = ZonedDateTime.of(2026, 3, day, h, 0, 0, 0, zone).toInstant().toEpochMilli()
    private fun entry(id: Long, reps: Int, kg: Double, at: Long, sets: Int = 1) = SetEntry(id, 1, sets, reps, kg, at)

    @Test fun prefillUsesNewestEntryOrDefaults() {
        assertEquals(SetPrefill(8, 82.5), prefillFor(listOf(entry(2, 8, 82.5, at(10)), entry(1, 10, 80.0, at(9)))))
        assertEquals(SetPrefill(10, null), prefillFor(emptyList()))
        val warmupFirst = listOf(entry(1, 12, 40.0, at(11)).copy(setType = SET_WARMUP), entry(1, 8, 82.5, at(10)))
        assertEquals(SetPrefill(8, 82.5), prefillFor(warmupFirst))
    }

    @Test fun prDetectionNeedsHistoryAndStrictlyHigherLoad() {
        val history = listOf(entry(1, 5, 80.0, at(9)))
        assertTrue(beatsPersonalRecord(history, 82.5))
        assertFalse(beatsPersonalRecord(history, 80.0))
        assertFalse(beatsPersonalRecord(emptyList(), 50.0))
    }

    @Test fun todayFilterUsesZoneAndSortsOldestFirst() {
        val now = at(18)
        val list = listOf(
            entry(3, 10, 80.0, at(12)),
            entry(2, 10, 80.0, at(8)),
            entry(1, 10, 80.0, at(23, day = 9)),
            entry(4, 10, 80.0, at(0, day = 11))
        )
        assertEquals(listOf(2L, 3L), entriesOnDay(list, now, zone).map { it.id })
        // 23:30 on the 9th in São Paulo is already the 10th in UTC, so the zone matters.
        val late = ZonedDateTime.of(2026, 3, 9, 23, 30, 0, 0, zone).toInstant().toEpochMilli()
        assertTrue(entriesOnDay(listOf(entry(5, 1, 1.0, late)), now, ZoneId.of("UTC")).isNotEmpty())
        assertTrue(entriesOnDay(listOf(entry(5, 1, 1.0, late)), now, zone).isEmpty())
    }

    @Test fun steppersClamp() {
        assertEquals(1, stepReps(1, -1))
        assertEquals(11, stepReps(10, 1))
        assertEquals(0.0, stepWeightKg(1.0, -WEIGHT_STEP_KG), 0.0)
        assertEquals(2.5, stepWeightKg(null, WEIGHT_STEP_KG), 0.0)
        assertEquals(82.5, stepWeightKg(80.0, WEIGHT_STEP_KG), 0.0)
    }

    @Test fun movedOrderSwapsNeighboursAndRejectsEnds() {
        val ids = listOf(10L, 20L, 30L)
        assertEquals(listOf(20L, 10L, 30L), movedOrder(ids, 20, up = true))
        assertEquals(listOf(10L, 30L, 20L), movedOrder(ids, 20, up = false))
        assertNull(movedOrder(ids, 10, up = true))
        assertNull(movedOrder(ids, 30, up = false))
        assertNull(movedOrder(ids, 99, up = true))
    }

    @Test fun formatsPtBr() {
        val pt = java.util.Locale.forLanguageTag("pt-BR")
        assertEquals("80,0 kg", formatKg(80.0, pt))
        assertEquals("80", formatWeightInput(80.0, pt))
        assertEquals("82,5", formatWeightInput(82.5, pt))
        assertEquals("10 × 80,0 kg", formatSet(entry(1, 10, 80.0, 0), pt))
        assertEquals("3 × 10 · 80,0 kg", formatSet(entry(1, 10, 80.0, 0, sets = 3), pt))
        assertEquals("82.5", formatWeightInput(82.5, java.util.Locale.US))
    }

    @Test fun templatesAreWellFormed() {
        assertEquals(StarterTemplates.size, StarterTemplates.map { it.id }.toSet().size)
        assertTrue(StarterTemplates.all { it.exercises.isNotEmpty() && it.exercises.all { e -> e.name.isNotBlank() && e.target.isNotBlank() } })
        // Template names must match the catalog, otherwise they are never translated or linked to muscles.
        val catalog = org.json.JSONArray(java.io.File("src/main/assets/exercises.json").readText())
        val names = (0 until catalog.length()).map { catalog.getJSONObject(it).getString("name") }.toSet()
        assertEquals(emptyList<String>(), StarterTemplates.flatMap { it.exercises }.map { it.name }.filter { it !in names })
    }

    @Test fun durationFormatAndParse() {
        assertEquals("0:45", formatDuration(45))
        assertEquals("12:05", formatDuration(725))
        assertEquals("1:02:03", formatDuration(3723))
        assertEquals(90, parseDuration("1:30"))
        assertEquals(90, parseDuration("90"))
        assertEquals(3723, parseDuration("1:02:03"))
        assertNull(parseDuration(""))
        assertNull(parseDuration("a:10"))
        assertNull(parseDuration("-5"))
    }

    @Test fun formatSetPerKind() {
        val en = java.util.Locale.US
        val base = entry(1, 10, 80.0, at(9))
        assertEquals("10 × 80.0 kg", formatSet(base, en))
        assertEquals("10/side × 80.0 kg", formatSet(base, en, "reps", "side"))
        assertEquals("10×", formatSet(base.copy(weightKg = 0.0), en, "bodyweight"))
        assertEquals("10/side× + 5.0 kg", formatSet(base.copy(weightKg = 5.0), en, "bodyweight", "side"))
        assertEquals("0:45", formatSet(base.copy(reps = 1, weightKg = 0.0, durationSec = 45), en, "timed"))
        assertEquals("0:30/side", formatSet(base.copy(reps = 1, weightKg = 0.0, durationSec = 30), en, "timed", "side"))
        assertEquals("12:30 · 3.2 km", formatSet(base.copy(reps = 1, weightKg = 0.0, durationSec = 750, distanceKm = 3.2), en, "cardio"))
    }

    @Test fun kindRecords() {
        val prev = listOf(entry(1, 1, 0.0, at(9)).copy(durationSec = 40, distanceKm = 3.0))
        assertTrue(beatsKindRecord("timed", prev, 0.0, 41, null))
        assertFalse(beatsKindRecord("timed", prev, 0.0, 40, null))
        assertTrue(beatsKindRecord("cardio", prev, 0.0, 10, 3.5))
        assertFalse(beatsKindRecord("cardio", prev, 0.0, 999, 2.0))
        assertFalse(beatsKindRecord("bodyweight", prev, 0.0, null, null))
        assertFalse(beatsKindRecord("timed", emptyList(), 0.0, 60, null))
    }
}
