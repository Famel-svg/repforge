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
    val position: Int,
    val note: String = "",
    val supersetGroup: Int? = null
)

data class BodyWeight(val id: Long, val weightKg: Double, val recordedAt: Long)

data class SetEntry(
    val id: Long,
    val exerciseId: Long,
    val sets: Int,
    val reps: Int,
    val weightKg: Double,
    val recordedAt: Long,
    val setType: String = SET_NORMAL,
    val rpe: Double? = null
)

const val SET_NORMAL = "normal"
const val SET_WARMUP = "warmup"
const val SET_DROP = "drop"
const val SET_FAILURE = "failure"
val SET_TYPES = listOf(SET_NORMAL, SET_WARMUP, SET_DROP, SET_FAILURE)
val RPE_VALUES = (12..20).map { it / 2.0 }

data class DashboardSummary(
    val weeklyVolumeKg: Double = 0.0,
    val completedSets: Int = 0,
    val activeSheets: Int = 0,
    val currentStreak: Int = 0
)

data class DailyVolume(val day: String, val volumeKg: Double)

data class HistoryEntry(
    val exerciseId: Long,
    val exerciseName: String,
    val sheetName: String,
    val sets: Int,
    val reps: Int,
    val weightKg: Double,
    val recordedAt: Long,
    val setType: String = SET_NORMAL,
    val rpe: Double? = null,
    /** Free-text muscle group of the exercise; blank until the history query joins exercises.target. */
    val target: String = ""
)
