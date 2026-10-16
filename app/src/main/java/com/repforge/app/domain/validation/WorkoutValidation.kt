package com.repforge.app.domain.validation

fun validateSetInput(sets: Int?, reps: Int?, weightKg: Double?): AppError? {
    if (sets == null || reps == null || weightKg == null) return AppError.SetMissing
    if (sets <= 0) return AppError.SetsNotPositive
    if (reps <= 0) return AppError.RepsNotPositive
    if (weightKg < 0) return AppError.WeightNegative
    return null
}

/** Accepts both "80.5" and the pt-BR "80,5". */
fun parseWeightKg(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

const val MIN_BODY_WEIGHT_KG = 20.0
const val MAX_BODY_WEIGHT_KG = 400.0

fun validateBodyWeight(weightKg: Double?): AppError? = when {
    weightKg == null -> AppError.BodyWeightMissing
    weightKg !in MIN_BODY_WEIGHT_KG..MAX_BODY_WEIGHT_KG -> AppError.BodyWeightRange
    else -> null
}

/** Duration/distance, when given, must be positive and finite. */
fun validateEffort(durationSec: Int?, distanceKm: Double?): AppError? =
    if ((durationSec != null && durationSec <= 0) || (distanceKm != null && !(distanceKm > 0 && distanceKm.isFinite()))) AppError.DurationInvalid else null

/** Timed sets need a duration; cardio needs a duration or a distance. */
fun validateKindEffort(kind: String, durationSec: Int?, distanceKm: Double?): AppError? = validateEffort(durationSec, distanceKm) ?: when {
    kind == "timed" && durationSec == null -> AppError.DurationInvalid
    kind == "cardio" && durationSec == null && distanceKm == null -> AppError.DurationInvalid
    else -> null
}
