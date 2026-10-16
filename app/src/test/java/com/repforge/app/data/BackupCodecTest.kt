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
                BackupExercise("Supino", "Peito", 0, listOf(BackupEntry(3, 10, 80.5, 2_000), BackupEntry(1, 5, 40.0, 2_100, "warmup", 7.5)), "Pegada aberta", 1),
                BackupExercise("Crucifixo", "", 1, emptyList(), supersetGroup = 1)
            )
        ),
        BackupSheet("Ficha B", 3_000, emptyList())
    )

    @Test fun roundTripsNativeFormat() {
        assertEquals(sample, BackupCodec.decode(BackupCodec.encode(sample)))
    }

    @Test fun roundTripsBodyWeights() {
        val weights = listOf(BackupBodyWeight(82.5, 10), BackupBodyWeight(81.0, 20))
        val raw = BackupCodec.encode(sample, bodyWeights = weights)
        assertEquals(weights, BackupCodec.decodeBodyWeights(raw))
        assertEquals(sample, BackupCodec.decode(raw))
    }

    @Test fun rejectsOutOfRangeBodyWeight() {
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decodeBodyWeights("""{"bodyWeights":[{"weightKg":5,"recordedAt":1}]}""") }
    }

    @Test fun oldNativeBackupWithoutNewFieldsGetsDefaults() {
        val raw = """{"format":"repforge-backup","version":1,"sheets":[{"id":1,"name":"A","updatedAt":1}],
            "exercises":[{"id":1,"sheetId":1,"name":"X","target":"","position":0}],
            "entries":[{"id":1,"exerciseId":1,"sets":1,"reps":5,"weightKg":10,"recordedAt":5}]}"""
        val exercise = BackupCodec.decode(raw).single().exercises.single()
        assertEquals("", exercise.note)
        assertEquals(null, exercise.supersetGroup)
        assertEquals(emptyList<BackupBodyWeight>(), BackupCodec.decodeBodyWeights(raw))
        assertEquals(BackupEntry(1, 5, 10.0, 5, "normal", null), exercise.entries.single())
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
