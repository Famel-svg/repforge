package com.repforge.app.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sheets")
data class SheetEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sheetId: Long,
    val name: String,
    val target: String,
    val position: Int
)

@Entity(tableName = "entries")
data class EntryEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val sets: Int,
    val reps: Int,
    val weightKg: Double,
    val recordedAt: Long = System.currentTimeMillis()
)

data class SheetWithCount(
    val id: Long,
    val name: String,
    val exerciseCount: Int,
    val updatedAt: Long
)

data class ExerciseRow(val id: Long, val sheetId: Long, val name: String, val target: String, val position: Int)

data class EntryRow(val id: Long, val exerciseId: Long, val sets: Int, val reps: Int, val weightKg: Double, val recordedAt: Long)

data class DailyVolumeRow(val day: String, val volumeKg: Double)

@Dao
interface WorkoutDao {
    @Query("SELECT s.id, s.name, COUNT(e.id) AS exerciseCount, s.updatedAt FROM sheets s LEFT JOIN exercises e ON e.sheetId = s.id GROUP BY s.id ORDER BY s.updatedAt DESC")
    fun observeSheets(): Flow<List<SheetWithCount>>

    @Query("SELECT COALESCE(SUM(e.sets * e.reps * e.weightKg), 0.0) FROM entries e WHERE e.recordedAt >= :since")
    fun observeWeeklyVolume(since: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(e.sets), 0) FROM entries e WHERE e.recordedAt >= :since")
    fun observeWeeklySets(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM sheets")
    fun observeSheetCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSheet(sheet: SheetEntity): Long

    @Query("SELECT id, sheetId, name, target, position FROM exercises WHERE sheetId = :sheetId ORDER BY position, id")
    fun observeExercises(sheetId: Long): Flow<List<ExerciseRow>>

    @Query("SELECT id, exerciseId, sets, reps, weightKg, recordedAt FROM entries WHERE exerciseId = :exerciseId ORDER BY recordedAt DESC, id DESC")
    fun observeEntries(exerciseId: Long): Flow<List<EntryRow>>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM exercises WHERE sheetId = :sheetId")
    suspend fun nextExercisePosition(sheetId: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntry(entry: EntryEntity): Long

    @Query("SELECT * FROM sheets ORDER BY id")
    suspend fun allSheets(): List<SheetEntity>

    @Query("SELECT * FROM exercises ORDER BY sheetId, position, id")
    suspend fun allExercises(): List<ExerciseEntity>

    @Query("SELECT * FROM entries ORDER BY exerciseId, recordedAt, id")
    suspend fun allEntries(): List<EntryEntity>

    @Query("DELETE FROM entries")
    suspend fun clearEntries()

    @Query("DELETE FROM exercises")
    suspend fun clearExercises()

    @Query("DELETE FROM sheets")
    suspend fun clearSheets()

    @Query("SELECT strftime('%Y-%m-%d', recordedAt / 1000, 'unixepoch', 'localtime') AS day, COALESCE(SUM(sets * reps * weightKg), 0.0) AS volumeKg FROM entries WHERE recordedAt >= :since GROUP BY day ORDER BY day")
    fun observeDailyVolume(since: Long): Flow<List<DailyVolumeRow>>
}

@Database(entities = [SheetEntity::class, ExerciseEntity::class, EntryEntity::class], version = 1, exportSchema = false)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
}
