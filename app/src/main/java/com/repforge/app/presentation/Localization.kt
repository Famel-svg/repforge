package com.repforge.app.presentation

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.repforge.app.R
import com.repforge.app.data.ExerciseCatalog
import com.repforge.app.data.ProError
import com.repforge.app.domain.validation.AppError
import com.repforge.app.domain.validation.AppException
import java.text.NumberFormat
import java.util.Locale

/** Normalized pt-BR catalog name -> English name, provided once at the app root. Stored data keeps the pt-BR key. */
val LocalCatalogNames = staticCompositionLocalOf<Map<String, String>> { emptyMap() }

@Composable
fun isPortuguese(): Boolean = LocalConfiguration.current.locales[0].language == "pt"

/** Catalog exercises show their English name outside Portuguese locales; custom names are shown as typed. */
@Composable
fun exerciseDisplayName(name: String): String {
    if (isPortuguese()) return name
    return LocalCatalogNames.current[ExerciseCatalog.normalize(name)] ?: name
}

private val MUSCLES = mapOf(
    "Peito" to R.string.muscle_chest, "Costas" to R.string.muscle_back, "Ombros" to R.string.muscle_shoulders,
    "Bíceps" to R.string.muscle_biceps, "Tríceps" to R.string.muscle_triceps, "Antebraço" to R.string.muscle_forearm,
    "Abdômen" to R.string.muscle_abs, "Quadríceps" to R.string.muscle_quads, "Posterior de coxa" to R.string.muscle_hamstrings,
    "Glúteos" to R.string.muscle_glutes, "Panturrilha" to R.string.muscle_calves, "Panturrilhas" to R.string.muscle_calves,
    "Trapézio" to R.string.muscle_traps, "Lombar" to R.string.muscle_lower_back, "Cardio" to R.string.muscle_cardio,
    "Corpo inteiro" to R.string.muscle_full_body, "Pernas" to R.string.muscle_legs
)

private val EQUIPMENT = mapOf(
    "Barra" to R.string.equipment_barbell, "Halteres" to R.string.equipment_dumbbells, "Máquina" to R.string.equipment_machine,
    "Polia" to R.string.equipment_cable, "Peso corporal" to R.string.equipment_bodyweight, "Kettlebell" to R.string.equipment_kettlebell,
    "Elástico" to R.string.equipment_band, "Smith" to R.string.equipment_smith, "Barra W" to R.string.equipment_ez_bar,
    "Outros" to R.string.equipment_other
)

/** Maps the fixed pt-BR muscle values used in the catalog and stored on exercises; free text passes through. */
@Composable
fun muscleLabel(pt: String): String = MUSCLES[pt]?.let { stringResource(it) } ?: pt

@Composable
fun equipmentLabel(pt: String): String = EQUIPMENT[pt]?.let { stringResource(it) } ?: pt

@StringRes
fun AppError.res(): Int = when (this) {
    AppError.SetMissing -> R.string.err_set_missing
    AppError.SetsNotPositive -> R.string.err_sets_not_positive
    AppError.RepsNotPositive -> R.string.err_reps_not_positive
    AppError.WeightNegative -> R.string.err_weight_negative
    AppError.BodyWeightMissing -> R.string.err_body_weight_missing
    AppError.BodyWeightRange -> R.string.err_body_weight_range
    AppError.InvalidJson -> R.string.err_invalid_json
    AppError.UnsupportedVersion -> R.string.err_unsupported_version
    AppError.NoSheets -> R.string.err_no_sheets
    AppError.NotRepForgeBackup -> R.string.err_not_repforge_backup
    AppError.Corrupt -> R.string.err_corrupt
    AppError.EmptyName -> R.string.err_empty_name
    AppError.InvalidPosition -> R.string.err_invalid_position
    AppError.InvalidSet -> R.string.err_invalid_set
    AppError.InvalidBodyWeight -> R.string.err_invalid_body_weight
    AppError.OrphanEntry -> R.string.err_orphan_entry
    AppError.OrphanExercise -> R.string.err_orphan_exercise
    AppError.InvalidDate -> R.string.err_invalid_date
    AppError.CsvFormat -> R.string.err_csv_format
    AppError.SheetNameEmpty -> R.string.err_sheet_name_empty
    AppError.DestinationUnavailable -> R.string.err_destination_unavailable
    AppError.FileUnavailable -> R.string.err_file_unavailable
    AppError.FileTooLarge -> R.string.err_file_too_large
}

/** Message for any thrown error; [AppException] kinds are translated, anything else shows its technical message. */
fun Context.errorText(error: Throwable): String = when (error) {
    is AppException -> getString(error.error.res(), error.arg.orEmpty())
    else -> error.message ?: getString(R.string.error_unknown)
}

fun Context.proErrorText(error: ProError): String = when (error) {
    ProError.Unavailable -> getString(R.string.pro_err_unavailable)
    ProError.Pending -> getString(R.string.pro_err_pending)
    is ProError.PurchaseFailed -> getString(R.string.pro_err_purchase_failed, error.code)
}

/** Decimal number in the app locale with at most one fraction digit: "1,5" in pt-BR, "1.5" in en. */
fun formatCount(value: Double, locale: Locale = Locale.getDefault()): String =
    NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1; minimumFractionDigits = 0 }.format(value)
