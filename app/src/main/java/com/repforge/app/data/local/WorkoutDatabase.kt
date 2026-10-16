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

@Entity(
    tableName = "exercises",
    foreignKeys = [androidx.room.ForeignKey(entity = SheetEntity::class, parentColumns = ["id"], childColumns = ["sheetId"], onDelete = androidx.room.ForeignKey.CASCADE)],
    indices = [androidx.room.Index("sheetId")]
)
data class ExerciseEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sheetId: Long,
    val name: String,
    val target: String,
    val position: Int,
    @androidx.room.ColumnInfo(defaultValue = "") val note: String = "",
    val supersetGroup: Int? = null
)

@Entity(tableName = "body_weights")
data class BodyWeightEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weightKg: Double,
    val recordedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "entries",
    foreignKeys = [androidx.room.ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = androidx.room.ForeignKey.CASCADE)],
    indices = [androidx.room.Index("exerciseId")]
)
data class EntryEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val sets: Int,
    val reps: Int,
    val weightKg: Double,
    val recordedAt: Long = System.currentTimeMillis(),
    @androidx.room.ColumnInfo(defaultValue = "normal") val setType: String = "normal",
    val rpe: Double? = null
)

@Entity(
    tableName = "schedule",
    foreignKeys = [androidx.room.ForeignKey(entity = SheetEntity::class, parentColumns = ["id"], childColumns = ["sheetId"], onDelete = androidx.room.ForeignKey.SET_NULL)],
    indices = [androidx.room.Index("sheetId")]
)
data class ScheduleEntity(@androidx.room.PrimaryKey val dayOfWeek: Int, val sheetId: Long?)

/** One-off change for a date ('yyyy-MM-dd'); null sheetId = rest day. */
@Entity(
    tableName = "schedule_overrides",
    foreignKeys = [androidx.room.ForeignKey(entity = SheetEntity::class, parentColumns = ["id"], childColumns = ["sheetId"], onDelete = androidx.room.ForeignKey.CASCADE)],
    indices = [androidx.room.Index("sheetId")]
)
data class ScheduleOverrideEntity(@androidx.room.PrimaryKey val date: String, val sheetId: Long?)

data class SheetWithCount(
    val id: Long,
    val name: String,
    val exerciseCount: Int,
    val updatedAt: Long
)

data class ExerciseRow(val id: Long, val sheetId: Long, val name: String, val target: String, val position: Int, val note: String, val supersetGroup: Int?)

data class EntryRow(val id: Long, val exerciseId: Long, val sets: Int, val reps: Int, val weightKg: Double, val recordedAt: Long, val setType: String, val rpe: Double?)

data class DailyVolumeRow(val day: String, val volumeKg: Double)

data class HistoryRow(
    val exerciseId: Long,
    val exerciseName: String,
    val sheetName: String,
    val sets: Int,
    val reps: Int,
    val weightKg: Double,
    val recordedAt: Long,
    val setType: String,
    val rpe: Double?,
    val target: String
)

@Dao
interface WorkoutDao {
    @Query("SELECT s.id, s.name, COUNT(e.id) AS exerciseCount, s.updatedAt FROM sheets s LEFT JOIN exercises e ON e.sheetId = s.id GROUP BY s.id ORDER BY s.updatedAt DESC")
    fun observeSheets(): Flow<List<SheetWithCount>>

    @Query("SELECT COALESCE(SUM(e.sets * e.reps * e.weightKg), 0.0) FROM entries e WHERE e.recordedAt >= :since AND e.setType != 'warmup'")
    fun observeWeeklyVolume(since: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(e.sets), 0) FROM entries e WHERE e.recordedAt >= :since AND e.setType != 'warmup'")
    fun observeWeeklySets(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM sheets")
    fun observeSheetCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSheet(sheet: SheetEntity): Long

    @Query("DELETE FROM sheets WHERE id = :sheetId")
    suspend fun deleteSheet(sheetId: Long)

    @Query("SELECT s.id, s.name, COUNT(e.id) AS exerciseCount, s.updatedAt FROM sheets s LEFT JOIN exercises e ON e.sheetId = s.id WHERE s.id = :sheetId GROUP BY s.id")
    fun observeSheet(sheetId: Long): Flow<SheetWithCount?>

    @Query("SELECT * FROM sheets WHERE id = :sheetId")
    suspend fun findSheet(sheetId: Long): SheetEntity?

    @Query("SELECT * FROM exercises WHERE sheetId = :sheetId ORDER BY position, id")
    suspend fun exercisesOf(sheetId: Long): List<ExerciseEntity>

    @Query("UPDATE sheets SET updatedAt = :now WHERE id = :sheetId")
    suspend fun touchSheet(sheetId: Long, now: Long)

    @Query("UPDATE sheets SET updatedAt = :now WHERE id = (SELECT sheetId FROM exercises WHERE id = :exerciseId)")
    suspend fun touchSheetOfExercise(exerciseId: Long, now: Long)

    @Query("SELECT id, sheetId, name, target, position, note, supersetGroup FROM exercises WHERE sheetId = :sheetId ORDER BY position, id")
    fun observeExercises(sheetId: Long): Flow<List<ExerciseRow>>

    @Query("SELECT id, exerciseId, sets, reps, weightKg, recordedAt, setType, rpe FROM entries WHERE exerciseId = :exerciseId ORDER BY recordedAt DESC, id DESC")
    fun observeEntries(exerciseId: Long): Flow<List<EntryRow>>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM exercises WHERE sheetId = :sheetId")
    suspend fun nextExercisePosition(sheetId: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Query("DELETE FROM exercises WHERE id = :exerciseId")
    suspend fun deleteExercise(exerciseId: Long)

    @Query("UPDATE exercises SET name = :name, target = :target WHERE id = :exerciseId")
    suspend fun updateExercise(exerciseId: Long, name: String, target: String)

    @Query("UPDATE exercises SET note = :note WHERE id = :exerciseId")
    suspend fun updateExerciseNote(exerciseId: Long, note: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntry(entry: EntryEntity): Long

    @Query("SELECT * FROM sheets ORDER BY id")
    suspend fun allSheets(): List<SheetEntity>

    @Query("SELECT * FROM exercises ORDER BY sheetId, position, id")
    suspend fun allExercises(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    suspend fun findExercise(exerciseId: Long): ExerciseEntity?

    @Query("UPDATE exercises SET position = :position WHERE id = :exerciseId")
    suspend fun updateExercisePosition(exerciseId: Long, position: Int)

    @Query("DELETE FROM entries WHERE id = :entryId")
    suspend fun deleteEntry(entryId: Long)

    @Query("UPDATE exercises SET supersetGroup = :group WHERE id = :exerciseId")
    suspend fun updateSupersetGroup(exerciseId: Long, group: Int?)

    @Query("SELECT * FROM body_weights ORDER BY recordedAt DESC, id DESC")
    fun observeBodyWeights(): Flow<List<BodyWeightEntity>>

    @Query("SELECT * FROM body_weights ORDER BY recordedAt, id")
    suspend fun allBodyWeights(): List<BodyWeightEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBodyWeight(entry: BodyWeightEntity): Long

    @Query("DELETE FROM body_weights WHERE id = :id")
    suspend fun deleteBodyWeight(id: Long)

    @Query("SELECT * FROM schedule")
    fun observeSchedule(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedule")
    suspend fun allSchedule(): List<ScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSchedule(row: ScheduleEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertScheduleIfAbsent(row: ScheduleEntity)

    @Query("SELECT * FROM schedule_overrides WHERE date >= :from")
    fun observeOverrides(from: String): Flow<List<ScheduleOverrideEntity>>

    @Query("SELECT * FROM schedule_overrides ORDER BY date")
    suspend fun allOverrides(): List<ScheduleOverrideEntity>

    @Query("SELECT * FROM schedule_overrides WHERE date IN (:dates)")
    suspend fun overridesOn(dates: List<String>): List<ScheduleOverrideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOverride(row: ScheduleOverrideEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOverrideIfAbsent(row: ScheduleOverrideEntity)

    @Query("DELETE FROM schedule_overrides WHERE date = :date")
    suspend fun deleteOverride(date: String)

    @Query("SELECT * FROM entries ORDER BY exerciseId, recordedAt, id")
    suspend fun allEntries(): List<EntryEntity>

    @Query("SELECT strftime('%Y-%m-%d', recordedAt / 1000, 'unixepoch', 'localtime') AS day, COALESCE(SUM(sets * reps * weightKg), 0.0) AS volumeKg FROM entries WHERE recordedAt >= :since AND setType != 'warmup' GROUP BY day ORDER BY day")
    fun observeDailyVolume(since: Long): Flow<List<DailyVolumeRow>>

    @Query("SELECT ex.id AS exerciseId, ex.name AS exerciseName, s.name AS sheetName, en.sets AS sets, en.reps AS reps, en.weightKg AS weightKg, en.recordedAt AS recordedAt, en.setType AS setType, en.rpe AS rpe, ex.target AS target FROM entries en JOIN exercises ex ON ex.id = en.exerciseId JOIN sheets s ON s.id = ex.sheetId ORDER BY en.recordedAt DESC, en.id DESC")
    fun observeHistory(): Flow<List<HistoryRow>>
}

val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE entries ADD COLUMN setType TEXT NOT NULL DEFAULT 'normal'")
        db.execSQL("ALTER TABLE entries ADD COLUMN rpe REAL")
        db.execSQL("ALTER TABLE exercises ADD COLUMN note TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE exercises ADD COLUMN supersetGroup INTEGER")
        db.execSQL("CREATE TABLE IF NOT EXISTS `body_weights` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `weightKg` REAL NOT NULL, `recordedAt` INTEGER NOT NULL)")
    }
}

val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `schedule` (`dayOfWeek` INTEGER NOT NULL, `sheetId` INTEGER, PRIMARY KEY(`dayOfWeek`), FOREIGN KEY(`sheetId`) REFERENCES `sheets`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_schedule_sheetId` ON `schedule` (`sheetId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `schedule_overrides` (`date` TEXT NOT NULL, `sheetId` INTEGER, PRIMARY KEY(`date`), FOREIGN KEY(`sheetId`) REFERENCES `sheets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_schedule_overrides_sheetId` ON `schedule_overrides` (`sheetId`)")
    }
}

@Database(entities = [SheetEntity::class, ExerciseEntity::class, EntryEntity::class, BodyWeightEntity::class, ScheduleEntity::class, ScheduleOverrideEntity::class], version = 4, exportSchema = true)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
}
