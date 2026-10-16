package com.repforge.app

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.repforge.app.data.local.MIGRATION_1_2
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
}
