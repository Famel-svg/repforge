package com.repforge.app.domain.metrics

import com.repforge.app.domain.model.SetEntry

fun estimatedOneRepMax(weightKg: Double, reps: Int): Double {
    require(weightKg >= 0) { "Carga não pode ser negativa." }
    require(reps > 0) { "Repetições devem ser maiores que zero." }
    return weightKg * (1.0 + reps / 30.0)
}

fun bestEstimatedOneRepMax(entries: List<SetEntry>): Double = entries.maxOfOrNull {
    estimatedOneRepMax(it.weightKg, it.reps)
} ?: 0.0
