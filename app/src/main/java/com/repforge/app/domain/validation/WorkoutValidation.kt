package com.repforge.app.domain.validation

data class SetInputError(val message: String)

fun validateSetInput(sets: Int?, reps: Int?, weightKg: Double?): SetInputError? {
    if (sets == null || reps == null || weightKg == null) return SetInputError("Preencha séries, repetições e carga.")
    if (sets <= 0) return SetInputError("Séries devem ser maiores que zero.")
    if (reps <= 0) return SetInputError("Repetições devem ser maiores que zero.")
    if (weightKg < 0) return SetInputError("Carga não pode ser negativa.")
    return null
}

/** Accepts both "80.5" and the pt-BR "80,5". */
fun parseWeightKg(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

const val MIN_BODY_WEIGHT_KG = 20.0
const val MAX_BODY_WEIGHT_KG = 400.0

fun validateBodyWeight(weightKg: Double?): SetInputError? = when {
    weightKg == null -> SetInputError("Informe o peso em kg.")
    weightKg !in MIN_BODY_WEIGHT_KG..MAX_BODY_WEIGHT_KG -> SetInputError("Peso deve ficar entre 20 e 400 kg.")
    else -> null
}
