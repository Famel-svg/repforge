package com.repforge.app

import android.app.Application
import androidx.room.Room
import com.repforge.app.data.WorkoutRepositoryImpl
import com.repforge.app.data.SettingsStore
import com.repforge.app.data.BackupManager
import com.repforge.app.data.WorkoutXClient
import com.repforge.app.data.local.WorkoutDatabase

class RepForgeApplication : Application() {
    val database by lazy { Room.databaseBuilder(this, WorkoutDatabase::class.java, "repforge.db").build() }
    val repository by lazy { WorkoutRepositoryImpl(database.workoutDao()) }
    val settings by lazy { SettingsStore(this) }
    val backup by lazy { BackupManager(database) }
    val workoutX by lazy { WorkoutXClient(this) }
}
