package com.repforge.app.presentation.settings

import com.repforge.app.data.Accent
import com.repforge.app.data.ThemeMode
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.data.AppPreferences
import com.repforge.app.data.BackupManager
import com.repforge.app.data.ImportResult
import com.repforge.app.presentation.logExceptions
import com.repforge.app.data.SettingsStore
import com.repforge.app.data.parseCsvWorkoutsWithStats
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Outcome events; the screen turns them into localized text. */
sealed interface SettingsMessage {
    data object Exported : SettingsMessage
    data class ExportFailed(val error: Throwable) : SettingsMessage
    data class CsvImported(val sheets: Int, val result: ImportResult, val skippedRows: Int, val maybePounds: Boolean) : SettingsMessage
    data object BackupEmpty : SettingsMessage
    data class BackupImported(val sheets: Int, val result: ImportResult) : SettingsMessage
    data class ImportFailed(val error: Throwable) : SettingsMessage
}

class SettingsViewModel(private val store: SettingsStore, private val backup: BackupManager) : ViewModel() {
    val state: StateFlow<AppPreferences> = store.preferences.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferences()
    )

    private val _messages = Channel<SettingsMessage>(Channel.BUFFERED)
    val messages: Flow<SettingsMessage> = _messages.receiveAsFlow()

    fun setRestSeconds(value: Int) = viewModelScope.launch(logExceptions) { store.setRestSeconds(value) }
    fun setHapticsEnabled(value: Boolean) = viewModelScope.launch(logExceptions) { store.setHapticsEnabled(value) }
    fun setRestNotification(value: Boolean) = viewModelScope.launch(logExceptions) { store.setRestNotification(value) }

    fun setThemeMode(value: ThemeMode) = viewModelScope.launch(logExceptions) { store.setThemeMode(value) }
    fun setAccent(value: Accent) = viewModelScope.launch(logExceptions) { store.setAccent(value) }
    fun setAskBodyWeight(value: Boolean) = viewModelScope.launch(logExceptions) { store.setAskBodyWeight(value) }
    fun setWeeklyTarget(value: Int) = viewModelScope.launch(logExceptions) { store.setWeeklyTarget(value) }
    fun setWeightGoalKg(value: Double?) = viewModelScope.launch(logExceptions) { store.setWeightGoalKg(value) }
    fun setSoundsEnabled(value: Boolean) = viewModelScope.launch(logExceptions) { store.setSoundsEnabled(value) }

    fun exportBackup(write: (String) -> Unit) = viewModelScope.launch(logExceptions) {
        runCatching {
            val json = backup.exportJson()
            withContext(Dispatchers.IO) { write(json) }
        }.fold(
            { _messages.send(SettingsMessage.Exported) },
            { _messages.send(SettingsMessage.ExportFailed(it)) }
        )
    }

    fun importCsv(read: () -> String) = viewModelScope.launch(logExceptions) {
        runCatching {
            val text = withContext(Dispatchers.IO) { read() }
            val parsed = withContext(Dispatchers.Default) { parseCsvWorkoutsWithStats(text, ZoneId.systemDefault()) }
            parsed to backup.importSheets(parsed.sheets)
        }.fold(
            { (parsed, result) -> _messages.send(SettingsMessage.CsvImported(parsed.sheets.size, result, parsed.skippedRows, parsed.maybePounds)) },
            { _messages.send(SettingsMessage.ImportFailed(it)) }
        )
    }

    fun importBackup(read: () -> String) = viewModelScope.launch(logExceptions) {
        runCatching { backup.importJson(withContext(Dispatchers.IO) { read() }) }.fold(
            { result -> _messages.send(if (result.imported + result.skipped == 0) SettingsMessage.BackupEmpty else SettingsMessage.BackupImported(result.sheets, result)) },
            { _messages.send(SettingsMessage.ImportFailed(it)) }
        )
    }
}
