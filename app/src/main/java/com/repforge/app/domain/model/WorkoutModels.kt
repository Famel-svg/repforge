package com.repforge.app.domain.model

data class TrainingSheet(
    val id: Long,
    val name: String,
    val exerciseCount: Int,
    val updatedAt: Long
)

data class Exercise(
    val id: Long,
    val sheetId: Long,
    val name: String,
    val target: String,
    val position: Int
)

data class SetEntry(
    val id: Long,
    val exerciseId: Long,
    val sets: Int,
    val reps: Int,
    val weightKg: Double,
    val recordedAt: Long
)

data class DashboardSummary(
    val weeklyVolumeKg: Double = 0.0,
    val completedSets: Int = 0,
    val activeSheets: Int = 0,
    val currentStreak: Int = 0
)

data class DailyVolume(val day: String, val volumeKg: Double)
