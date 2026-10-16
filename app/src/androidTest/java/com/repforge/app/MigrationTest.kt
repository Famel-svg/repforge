package com.repforge.app

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.repforge.app.data.local.MIGRATION_1_2
import com.repforge.app.data.local.MIGRATION_2_3
import com.repforge.app.data.local.MIGRATION_3_4
import com.repforge.app.data.local.MIGRATION_4_5
import com.repforge.app.data.local.WorkoutDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), WorkoutDatabase::class.java)

    @Test fun migrate1To2KeepsDataAndAddsDefaults() {
        helper.createDatabase("migration-test", 1).apply {
            execSQL("INSERT INTO sheets (id, name, updatedAt) VALUES (1, 'A', 1)")
            execSQL("INSERT INTO exercises (id, sheetId, name, target, position) VALUES (1, 1, 'Supino', 'Peito', 0)")
            execSQL("INSERT INTO entries (id, exerciseId, sets, reps, weightKg, recordedAt) VALUES (1, 1, 1, 10, 80.0, 5)")
            close()
        }
        val db = helper.runMigrationsAndValidate("migration-test", 2, true, MIGRATION_1_2)
        db.query("SELECT setType, rpe, weightKg FROM entries").use {
            it.moveToFirst()
            assertEquals("normal", it.getString(0))
            assertEquals(true, it.isNull(1))
            assertEquals(80.0, it.getDouble(2), 0.0)
        }
        db.query("SELECT note FROM exercises").use { it.moveToFirst(); assertEquals("", it.getString(0)) }
    }

    @Test fun migrate2To3KeepsDataAndAddsSupersetAndBodyWeights() {
        helper.createDatabase("migration-test-3", 2).apply {
            execSQL("INSERT INTO sheets (id, name, updatedAt) VALUES (1, 'A', 1)")
            execSQL("INSERT INTO exercises (id, sheetId, name, target, position, note) VALUES (1, 1, 'Supino', 'Peito', 0, 'n')")
            execSQL("INSERT INTO entries (id, exerciseId, sets, reps, weightKg, recordedAt, setType) VALUES (1, 1, 1, 10, 80.0, 5, 'normal')")
            close()
        }
        val db = helper.runMigrationsAndValidate("migration-test-3", 3, true, MIGRATION_2_3)
        db.query("SELECT name, note, supersetGroup FROM exercises").use {
            it.moveToFirst()
            assertEquals("Supino", it.getString(0))
            assertEquals("n", it.getString(1))
            assertEquals(true, it.isNull(2))
        }
        db.query("SELECT weightKg FROM entries").use { it.moveToFirst(); assertEquals(80.0, it.getDouble(0), 0.0) }
        db.execSQL("INSERT INTO body_weights (weightKg, recordedAt) VALUES (82.5, 1)")
        db.query("SELECT COUNT(*) FROM body_weights").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
    }

    @Test fun migrate3To4KeepsDataAndAddsScheduleTables() {
        helper.createDatabase("migration-test-4", 3).apply {
            execSQL("INSERT INTO sheets (id, name, updatedAt) VALUES (1, 'A', 1)")
            execSQL("INSERT INTO exercises (id, sheetId, name, target, position, note) VALUES (1, 1, 'Supino', 'Peito', 0, 'n')")
            close()
        }
        val db = helper.runMigrationsAndValidate("migration-test-4", 4, true, MIGRATION_3_4)
        db.query("SELECT name FROM exercises").use { it.moveToFirst(); assertEquals("Supino", it.getString(0)) }
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("INSERT INTO schedule (dayOfWeek, sheetId) VALUES (1, 1)")
        db.execSQL("INSERT INTO schedule_overrides (date, sheetId) VALUES ('2026-06-15', 1)")
        db.execSQL("INSERT INTO schedule_overrides (date, sheetId) VALUES ('2026-06-16', NULL)")
        db.execSQL("DELETE FROM sheets WHERE id = 1")
        db.query("SELECT sheetId FROM schedule WHERE dayOfWeek = 1").use { it.moveToFirst(); assertEquals(true, it.isNull(0)) }
        db.query("SELECT COUNT(*) FROM schedule_overrides").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
    }

    @Test fun migrate4To5KeepsDataAndAddsKindsAndSessions() {
        helper.createDatabase("migration-test-5", 4).apply {
            execSQL("INSERT INTO sheets (id, name, updatedAt) VALUES (1, 'A', 1)")
            execSQL("INSERT INTO exercises (id, sheetId, name, target, position, note) VALUES (1, 1, 'Supino', 'Peito', 0, 'n')")
            execSQL("INSERT INTO entries (id, exerciseId, sets, reps, weightKg, recordedAt, setType) VALUES (1, 1, 1, 10, 80.0, 5, 'normal')")
            close()
        }
        val db = helper.runMigrationsAndValidate("migration-test-5", 5, true, MIGRATION_4_5)
        db.query("SELECT name, note, kind, perSide, progressionRule FROM exercises").use {
            it.moveToFirst()
            assertEquals("Supino", it.getString(0))
            assertEquals("n", it.getString(1))
            assertEquals("reps", it.getString(2))
            assertEquals(0, it.getInt(3))
            assertEquals(true, it.isNull(4))
        }
        db.query("SELECT progressionRule FROM sheets").use { it.moveToFirst(); assertEquals("double", it.getString(0)) }
        db.query("SELECT weightKg, durationSec, distanceKm FROM entries").use {
            it.moveToFirst()
            assertEquals(80.0, it.getDouble(0), 0.0)
            assertEquals(true, it.isNull(1))
            assertEquals(true, it.isNull(2))
        }
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("INSERT INTO sessions (id, sheetId, startedAt) VALUES (1, 1, 10)")
        db.execSQL("DELETE FROM sheets WHERE id = 1")
        db.query("SELECT sheetId, endedAt, bodyWeightKg FROM sessions").use {
            it.moveToFirst()
            assertEquals(true, it.isNull(0))
            assertEquals(true, it.isNull(1))
            assertEquals(true, it.isNull(2))
        }
    }
}
