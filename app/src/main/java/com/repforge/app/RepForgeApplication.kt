package com.repforge.app

import android.app.Application
import androidx.room.Room
import com.repforge.app.data.WorkoutRepositoryImpl
import com.repforge.app.data.ProStore
import com.repforge.app.data.SettingsStore
import com.repforge.app.data.BackupManager
import com.repforge.app.data.ExerciseCatalog
import com.repforge.app.data.local.MIGRATION_1_2
import com.repforge.app.data.local.MIGRATION_2_3
import com.repforge.app.data.local.WorkoutDatabase
import com.repforge.app.widget.WidgetRefresher

class RepForgeApplication : Application() {
    // Schema changes must bump the version and register a Migration here; app/schemas keeps the exported history.
    val database by lazy { Room.databaseBuilder(this, WorkoutDatabase::class.java, "repforge.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3).build() }
    val repository by lazy { WorkoutRepositoryImpl(database, onChanged = widgetRefresher::refresh) }
    val widgetRefresher by lazy { WidgetRefresher(this) }
    val settings by lazy { SettingsStore(this) }
    val pro by lazy { ProStore(this) }
    val backup by lazy { BackupManager(database) }
    val catalog by lazy { ExerciseCatalog(this) }
}
