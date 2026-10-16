package com.repforge.app.domain.validation

/** User-facing failure kinds. The domain/data layers never hold display text; the UI maps each kind to a string resource. */
enum class AppError {
    SetMissing, SetsNotPositive, RepsNotPositive, WeightNegative, BodyWeightMissing, BodyWeightRange,
    InvalidJson, UnsupportedVersion, NoSheets, NotRepForgeBackup, Corrupt, EmptyName, InvalidPosition,
    InvalidSet, InvalidBodyWeight, OrphanEntry, OrphanExercise, InvalidDate, CsvFormat, SheetNameEmpty,
    DestinationUnavailable, FileUnavailable, FileTooLarge, DurationInvalid
}

/** [arg] carries the technical detail (parser message, offending value) for kinds that show one. */
class AppException(val error: AppError, val arg: String? = null) : IllegalArgumentException(error.name + (arg?.let { ": $it" } ?: ""))
