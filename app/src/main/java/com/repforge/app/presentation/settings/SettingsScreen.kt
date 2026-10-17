package com.repforge.app.presentation.settings

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.repforge.app.R
import com.repforge.app.data.Accent
import com.repforge.app.data.ImportResult
import com.repforge.app.data.ProStore
import com.repforge.app.data.ThemeMode
import com.repforge.app.domain.validation.AppError
import com.repforge.app.domain.validation.AppException
import com.repforge.app.presentation.components.HairlineDivider
import com.repforge.app.presentation.components.LargeTitleHeader
import com.repforge.app.presentation.components.PillStepper
import com.repforge.app.presentation.components.RfCard
import com.repforge.app.presentation.components.SectionHeader
import com.repforge.app.presentation.components.SegmentedPill
import com.repforge.app.presentation.components.SoftPillButton
import com.repforge.app.presentation.errorText
import com.repforge.app.presentation.proErrorText
import com.repforge.app.presentation.theme.contrast
import java.time.LocalDate
import java.util.Locale

private val RowInset = 16.dp + 28.dp + 12.dp
private val Blue = Color(0xFF0A84FF)
private val Orange = Color(0xFFFF9F0A)
private val Purple = Color(0xFFBF5AF2)
private val Red = Color(0xFFFF453A)
private val Teal = Color(0xFF30B0C7)
private val Grey = Color(0xFF8E8E93)
private val Yellow = Color(0xFFB38F00)
private val Green = Color(0xFF30A14E)

@Composable
private fun IconSquare(icon: ImageVector, color: Color) {
    Box(Modifier.size(28.dp).background(color, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

/** One list row inside a [Group]: icon square, title/subtitle, trailing slot. */
@Composable
private fun SettingRow(
    icon: ImageVector, color: Color, title: String, modifier: Modifier = Modifier, subtitle: String? = null,
    trailing: @Composable () -> Unit = {},
) {
    Row(modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconSquare(icon, color)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        trailing()
    }
}

@Composable
private fun ValueChevron(value: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (value != null) Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, color: Color, title: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    SettingRow(icon, color, title, Modifier.toggleable(checked, role = Role.Switch, onValueChange = onChange), hint) {
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun Group(content: @Composable () -> Unit) = RfCard(Modifier.fillMaxWidth(), padding = 0.dp) { Column { content() } }

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun SettingsScreen(viewModel: SettingsViewModel, pro: ProStore, onOpenBodyWeight: () -> Unit = {}) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isPro by pro.isPro.collectAsStateWithLifecycle()
    val price by pro.price.collectAsStateWithLifecycle()
    val proError by pro.error.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val context = LocalContext.current
    val resolver = context.contentResolver
    val versionName = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbarHostState.showSnackbar(context.messageText(it)) } }
    var goalDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.exportBackup { json ->
            (resolver.openOutputStream(uri) ?: throw AppException(AppError.DestinationUnavailable)).use { it.write(json.toByteArray()) }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importBackup {
            resolver.readTextCapped(uri)
        }
    }

    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importCsv {
            resolver.readTextCapped(uri)
        }
    }

    if (goalDialog) WeightGoalDialog(
        current = state.weightGoalKg,
        onDismiss = { goalDialog = false },
        onSave = { viewModel.setWeightGoalKg(it); goalDialog = false },
    )

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        ) {
            LargeTitleHeader(stringResource(R.string.nav_settings))
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(stringResource(R.string.pro_title))
                Group {
                    if (isPro) {
                        SettingRow(Icons.Rounded.Star, Orange, stringResource(R.string.pro_active), subtitle = stringResource(R.string.pro_thanks))
                    } else {
                        SettingRow(
                            Icons.Rounded.Star, Orange,
                            price?.let { stringResource(R.string.pro_lifetime_price, it) } ?: stringResource(R.string.pro_lifetime),
                            subtitle = stringResource(R.string.pro_description),
                        )
                        proError?.let { Text(context.proErrorText(it), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp)) }
                        FlowRow(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SoftPillButton(price?.let { stringResource(R.string.buy_for, it) } ?: stringResource(R.string.buy_pro), onClick = { activity?.let(pro::purchase) })
                            SoftPillButton(stringResource(R.string.restore_purchase), onClick = pro::restore)
                        }
                    }
                }

                Spacer(Modifier.size(8.dp))
                SectionHeader(stringResource(R.string.workout_section))
                Group {
                    SettingRow(Icons.Rounded.Timer, Blue, stringResource(R.string.default_rest), subtitle = stringResource(R.string.rest_seconds_between_sets, state.restSeconds))
                    val rests = listOf(60, 90, 120, 180)
                    SegmentedPill(
                        rests.map { stringResource(R.string.seconds_short, it) }, rests.indexOf(state.restSeconds),
                        { viewModel.setRestSeconds(rests[it]) }, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                    HairlineDivider(RowInset)
                    SwitchRow(Icons.Rounded.Vibration, Purple, stringResource(R.string.haptics), stringResource(R.string.haptics_hint), state.hapticsEnabled, viewModel::setHapticsEnabled)
                    HairlineDivider(RowInset)
                    SwitchRow(Icons.Rounded.Notifications, Red, stringResource(R.string.rest_notification), stringResource(R.string.rest_notification_hint), state.restNotification, viewModel::setRestNotification)
                    HairlineDivider(RowInset)
                    SwitchRow(Icons.Rounded.MonitorWeight, Teal, stringResource(R.string.ask_body_weight), stringResource(R.string.ask_body_weight_hint), state.askBodyWeight, viewModel::setAskBodyWeight)
                    HairlineDivider(RowInset)
                    SettingRow(Icons.Rounded.TrackChanges, Green, stringResource(R.string.t6_weekly_target), subtitle = stringResource(R.string.t6_weekly_target_hint)) {
                        PillStepper(
                            state.weeklyTarget.toString(),
                            onDec = { viewModel.setWeeklyTarget(state.weeklyTarget - 1) },
                            onInc = { viewModel.setWeeklyTarget(state.weeklyTarget + 1) },
                            onEdit = {},
                        )
                    }
                    HairlineDivider(RowInset)
                    SettingRow(Icons.Rounded.Flag, Yellow, stringResource(R.string.t6_weight_goal), Modifier.clickable(role = Role.Button) { goalDialog = true }) {
                        ValueChevron(state.weightGoalKg?.let { stringResource(R.string.t6_weight_goal_value, formatKg(it)) } ?: stringResource(R.string.t6_weight_goal_unset))
                    }
                }

                Spacer(Modifier.size(8.dp))
                SectionHeader(stringResource(R.string.appearance))
                Group {
                    val modes = ThemeMode.entries
                    SegmentedPill(
                        modes.map { stringResource(it.label()) }, modes.indexOf(state.themeMode),
                        { viewModel.setThemeMode(modes[it]) }, Modifier.fillMaxWidth().padding(16.dp),
                    )
                    HairlineDivider()
                    SettingRow(Icons.Rounded.Palette, Blue, stringResource(R.string.accent_color))
                    val dark = isSystemInDarkTheme()
                    FlowRow(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Accent.entries.forEach { accent ->
                            val selected = accent == state.accent
                            val name = stringResource(accent.label())
                            val swatch = Color(if (dark) accent.dark else accent.light)
                            Box(
                                Modifier.size(48.dp).semantics { contentDescription = name }
                                    .selectable(selected, role = Role.RadioButton, onClick = { viewModel.setAccent(accent) }),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    Modifier.size(36.dp).clip(CircleShape).background(swatch)
                                        .border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) Icon(Icons.Rounded.Check, null, tint = if (contrast(swatch, Color.White) >= contrast(swatch, Color.Black)) Color.White else Color.Black)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.size(8.dp))
                SectionHeader(stringResource(R.string.t6_section_sounds))
                Group {
                    SwitchRow(Icons.Rounded.VolumeUp, Orange, stringResource(R.string.sounds), stringResource(R.string.sounds_hint), state.soundsEnabled, viewModel::setSoundsEnabled)
                }

                Spacer(Modifier.size(8.dp))
                SectionHeader(stringResource(R.string.t6_section_data))
                Group {
                    SettingRow(Icons.Rounded.FileUpload, Blue, stringResource(R.string.export_backup), Modifier.clickable(role = Role.Button) { exportLauncher.launch("repforge-backup-${LocalDate.now()}.json") }) { ValueChevron(null) }
                    HairlineDivider(RowInset)
                    SettingRow(Icons.Rounded.FileDownload, Green, stringResource(R.string.import_backup), Modifier.clickable(role = Role.Button) { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) { ValueChevron(null) }
                    HairlineDivider(RowInset)
                    SettingRow(Icons.Rounded.CloudDownload, Purple, stringResource(R.string.import_csv), Modifier.clickable(role = Role.Button) { csvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain")) }) { ValueChevron(null) }
                    HairlineDivider(RowInset)
                    SettingRow(Icons.Rounded.FitnessCenter, Teal, stringResource(R.string.body_weight), Modifier.clickable(role = Role.Button, onClick = onOpenBodyWeight)) { ValueChevron(null) }
                }
                Text(stringResource(R.string.backup_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))

                Spacer(Modifier.size(8.dp))
                SectionHeader(stringResource(R.string.t6_section_about))
                Group {
                    SettingRow(Icons.Rounded.Info, Grey, stringResource(R.string.version_line, versionName), subtitle = stringResource(R.string.local_only_note))
                }
            }
        }
    }
}

private fun formatKg(value: Double) = String.format(Locale.getDefault(), "%.1f", value)

/** Accepts "72,5" or "72.5"; null when not a number in 20..500. */
internal fun parseWeightKg(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it in 20.0..500.0 }

@Composable
private fun WeightGoalDialog(current: Double?, onDismiss: () -> Unit, onSave: (Double?) -> Unit) {
    var text by remember { mutableStateOf(current?.let(::formatKg).orEmpty()) }
    val parsed = parseWeightKg(text)
    val invalid = text.isNotBlank() && parsed == null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.t6_weight_goal)) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it.take(7) }, singleLine = true, isError = invalid,
                label = { Text(stringResource(R.string.t6_weight_goal_label)) },
                supportingText = if (invalid) ({ Text(stringResource(R.string.t6_weight_goal_invalid)) }) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(parsed) }, enabled = parsed != null) { Text(stringResource(R.string.save)) } },
        dismissButton = {
            Row {
                if (current != null) TextButton(onClick = { onSave(null) }) { Text(stringResource(R.string.t6_clear)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

private fun ThemeMode.label() = when (this) {
    ThemeMode.System -> R.string.theme_system
    ThemeMode.Light -> R.string.theme_light
    ThemeMode.Dark -> R.string.theme_dark
}

private fun Accent.label() = when (this) {
    Accent.Ember -> R.string.accent_ember
    Accent.Rose -> R.string.accent_rose
    Accent.Violet -> R.string.accent_violet
    Accent.Indigo -> R.string.accent_indigo
    Accent.Sky -> R.string.accent_sky
    Accent.Teal -> R.string.accent_teal
    Accent.Green -> R.string.accent_green
    Accent.Gold -> R.string.accent_gold
}

private fun Context.messageText(message: SettingsMessage): String = when (message) {
    SettingsMessage.Exported -> getString(R.string.backup_exported)
    is SettingsMessage.ExportFailed -> getString(R.string.export_failed, errorText(message.error))
    SettingsMessage.BackupEmpty -> getString(R.string.backup_empty)
    is SettingsMessage.BackupImported ->
        getString(R.string.import_result, resources.getQuantityString(R.plurals.sheets_count, message.sheets, message.sheets), importSummary(message.result))
    is SettingsMessage.CsvImported -> buildString {
        append(getString(R.string.import_result, resources.getQuantityString(R.plurals.sheets_count, message.sheets, message.sheets), importSummary(message.result)))
        if (message.skippedRows > 0) append(" (").append(resources.getQuantityString(R.plurals.rows_ignored, message.skippedRows, message.skippedRows)).append(")")
        if (message.maybePounds) append(" (").append(getString(R.string.check_csv_lb)).append(")")
    }
    is SettingsMessage.ImportFailed -> getString(R.string.import_failed, errorText(message.error))
}

private fun Context.importSummary(result: ImportResult) = getString(
    R.string.import_counts,
    resources.getQuantityString(R.plurals.import_new, result.imported, result.imported),
    resources.getQuantityString(R.plurals.import_existing, result.skipped, result.skipped)
)

private const val MAX_IMPORT_BYTES = 20L * 1024 * 1024

/** Reads a picked file as text, refusing anything over 20 MB before loading it. */
private fun ContentResolver.readTextCapped(uri: Uri): String {
    val size = query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else null }
    if (size != null && size > MAX_IMPORT_BYTES) throw AppException(AppError.FileTooLarge)
    return (openInputStream(uri) ?: throw AppException(AppError.FileUnavailable)).use { it.readBytes().decodeToString() }
}
