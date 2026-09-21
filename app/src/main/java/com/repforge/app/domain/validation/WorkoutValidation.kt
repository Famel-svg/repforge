package com.repforge.app.domain.validation

data class SetInputError(val message: String)

fun validateSetInput(sets: Int?, reps: Int?, weightKg: Double?): SetInputError? {
    if (sets == null || reps == null || weightKg == null) return SetInputError("Preencha séries, repetições e carga.")
    if (sets <= 0) return SetInputError("Séries devem ser maiores que zero.")
    if (reps <= 0) return SetInputError("Repetições devem ser maiores que zero.")
    if (weightKg < 0) return SetInputError("Carga não pode ser negativa.")
    return null
}
