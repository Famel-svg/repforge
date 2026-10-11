package com.repforge.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

class BackupCodecTest {
    private val sample = listOf(
        BackupSheet(
            "Ficha A", 1_000,
            listOf(
                BackupExercise("Supino", "Peito", 0, listOf(BackupEntry(3, 10, 80.5, 2_000))),
                BackupExercise("Crucifixo", "", 1, emptyList())
            )
        ),
        BackupSheet("Ficha B", 3_000, emptyList())
    )

    @Test fun roundTripsNativeFormat() {
        assertEquals(sample, BackupCodec.decode(BackupCodec.encode(sample)))
    }

    @Test fun readsLegacyExpoBackup() {
        val raw = """
            {"exported_at":"2026-06-14T10:30:00.000Z","version":1,"sheets":[{"name":"Ficha A - Peito",
            "updated_at":"2026-06-14T10:00:00.000Z","exercises":[{"name":"Bench Press","gif_url":"https://x/y.gif",
            "position":0,"entries":[{"sets":3,"reps":10,"weight_kg":80.0,"recorded_at":"2026-06-14T09:00:00.000Z"}]}]}]}
        """.trimIndent()
        val sheet = BackupCodec.decode(raw).single()
        assertEquals("Ficha A - Peito", sheet.name)
        assertEquals(Instant.parse("2026-06-14T10:00:00Z").toEpochMilli(), sheet.updatedAt)
        val exercise = sheet.exercises.single()
        assertEquals("Bench Press", exercise.name)
        assertEquals(BackupEntry(3, 10, 80.0, Instant.parse("2026-06-14T09:00:00Z").toEpochMilli()), exercise.entries.single())
    }

    @Test fun rejectsInvalidJson() {
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode("not json") }
    }

    @Test fun rejectsUnsupportedVersion() {
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode("""{"format":"repforge-backup","version":2,"sheets":[]}""") }
    }

    @Test fun rejectsDanglingExercise() {
        val raw = """{"format":"repforge-backup","version":1,"sheets":[{"id":1,"name":"A","updatedAt":1}],
            "exercises":[{"id":1,"sheetId":9,"name":"X","target":"","position":0}],"entries":[]}"""
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(raw) }
    }

    @Test fun rejectsInvalidSet() {
        val raw = """{"version":1,"sheets":[{"name":"A","updated_at":"2026-06-14T10:00:00Z","exercises":[{"name":"X","position":0,
            "entries":[{"sets":0,"reps":10,"weight_kg":10,"recorded_at":"2026-06-14T10:00:00Z"}]}]}]}"""
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(raw) }
    }

    @Test fun rejectsForeignJson() {
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode("""{"version":1,"sheets":[{"name":"A","updated_at":"2026-06-14T10:00:00Z"}]}""") }
    }
}
