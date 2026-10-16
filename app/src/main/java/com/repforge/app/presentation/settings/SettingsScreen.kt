package com.repforge.app.presentation.settings

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import com.repforge.app.data.ProStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.repforge.app.data.Accent
import com.repforge.app.data.ThemeMode
import com.repforge.app.presentation.theme.contrast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.repforge.app.R
import com.repforge.app.data.ImportResult
import com.repforge.app.domain.validation.AppError
import com.repforge.app.domain.validation.AppException
import com.repforge.app.presentation.errorText
import com.repforge.app.presentation.proErrorText
import java.io.IOException
import java.time.LocalDate

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_settings)) }) }, snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.pro_title), style = MaterialTheme.typography.headlineSmall)
            Card {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isPro) {
                        Text(stringResource(R.string.pro_active), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.pro_thanks), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text(price?.let { stringResource(R.string.pro_lifetime_price, it) } ?: stringResource(R.string.pro_lifetime), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.pro_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        proError?.let { Text(context.proErrorText(it), color = MaterialTheme.colorScheme.error) }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { activity?.let(pro::purchase) }, enabled = activity != null) { Text(price?.let { stringResource(R.string.buy_for, it) } ?: stringResource(R.string.buy_pro)) }
                            OutlinedButton(onClick = pro::restore) { Text(stringResource(R.string.restore_purchase)) }
                        }
                    }
                }
            }
            Text(stringResource(R.string.workout_section), style = MaterialTheme.typography.headlineSmall)
            Card {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onOpenBodyWeight) { Text(stringResource(R.string.body_weight)) }
                    Text(stringResource(R.string.default_rest), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.rest_seconds_between_sets, state.restSeconds))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(60, 90, 120, 180).forEach { seconds ->
                            FilterChip(selected = seconds == state.restSeconds, onClick = { viewModel.setRestSeconds(seconds) }, label = { Text(stringResource(R.string.seconds_short, seconds)) })
                        }
                    }
                    HorizontalDivider()
                    Row(
                        Modifier.fillMaxWidth().toggleable(state.hapticsEnabled, role = Role.Switch, onValueChange = { viewModel.setHapticsEnabled(it) }),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.haptics), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.haptics_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = state.hapticsEnabled, onCheckedChange = null)
                    }
                    HorizontalDivider()
                    Row(
                        Modifier.fillMaxWidth().toggleable(state.restNotification, role = Role.Switch, onValueChange = { viewModel.setRestNotification(it) }),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.rest_notification), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.rest_notification_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = state.restNotification, onCheckedChange = null)
                    }
                }
            }
            Text(stringResource(R.string.appearance), style = MaterialTheme.typography.headlineSmall)
            Card {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        val modes = ThemeMode.entries
                        modes.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = mode == state.themeMode, onClick = { viewModel.setThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index, modes.size)
                            ) { Text(stringResource(mode.label())) }
                        }
                    }
                    Text(stringResource(R.string.accent_color), style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Accent.entries.forEach { accent ->
                            val selected = accent == state.accent
                            val name = stringResource(accent.label())
                            val swatch = Color(if (isSystemInDarkTheme()) accent.dark else accent.light)
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
                    HorizontalDivider()
                    Row(
                        Modifier.fillMaxWidth().toggleable(state.soundsEnabled, role = Role.Switch, onValueChange = { viewModel.setSoundsEnabled(it) }),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.sounds), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.sounds_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = state.soundsEnabled, onCheckedChange = null)
                    }
                }
            }
            Text(stringResource(R.string.backup), style = MaterialTheme.typography.headlineSmall)
            Card {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.backup_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { exportLauncher.launch("repforge-backup-${LocalDate.now()}.json") }) { Text(stringResource(R.string.export_backup)) }
                        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) { Text(stringResource(R.string.import_backup)) }
                        OutlinedButton(onClick = { csvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain")) }) { Text(stringResource(R.string.import_csv)) }
                    }
                }
            }
            Text(stringResource(R.string.version_line, versionName), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.local_only_note), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
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
