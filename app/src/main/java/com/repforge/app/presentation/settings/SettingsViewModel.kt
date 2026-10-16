package com.repforge.app.presentation.settings

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

class SettingsViewModel(private val store: SettingsStore, private val backup: BackupManager) : ViewModel() {
    val state: StateFlow<AppPreferences> = store.preferences.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferences()
    )

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    fun setRestSeconds(value: Int) = viewModelScope.launch(logExceptions) { store.setRestSeconds(value) }
    fun setHapticsEnabled(value: Boolean) = viewModelScope.launch(logExceptions) { store.setHapticsEnabled(value) }

    fun exportBackup(write: (String) -> Unit) = viewModelScope.launch(logExceptions) {
        runCatching {
            val json = backup.exportJson()
            withContext(Dispatchers.IO) { write(json) }
        }.fold(
            { _messages.send("Backup exportado.") },
            { _messages.send("Falha ao exportar: ${it.message ?: "erro desconhecido"}") }
        )
    }

    fun importCsv(read: () -> String) = viewModelScope.launch(logExceptions) {
        runCatching {
            val text = withContext(Dispatchers.IO) { read() }
            val parsed = withContext(Dispatchers.Default) { parseCsvWorkoutsWithStats(text, ZoneId.systemDefault()) }
            parsed to backup.importSheets(parsed.sheets)
        }.fold(
            { (parsed, result) ->
                val ignored = if (parsed.skippedRows > 0) " (${parsed.skippedRows} linhas ignoradas)" else ""
                val pounds = if (parsed.maybePounds) " (verifique se o CSV está em lb)" else ""
                _messages.send("${parsed.sheets.size} fichas: ${importSummary(result)}$ignored$pounds")
            },
            { _messages.send("Falha ao importar: ${it.message ?: "erro desconhecido"}") }
        )
    }

    fun importBackup(read: () -> String) = viewModelScope.launch(logExceptions) {
        runCatching { backup.importJson(withContext(Dispatchers.IO) { read() }) }.fold(
            { result -> _messages.send(if (result.imported + result.skipped == 0) "Backup vazio; nada importado." else "${result.sheets} ficha(s): ${importSummary(result)}") },
            { _messages.send("Falha ao importar: ${it.message ?: "erro desconhecido"}") }
        )
    }

    private fun importSummary(result: ImportResult) = "${result.imported} novas, ${result.skipped} já existiam"
}
