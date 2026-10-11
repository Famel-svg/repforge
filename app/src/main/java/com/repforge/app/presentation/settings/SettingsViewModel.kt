package com.repforge.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.data.AppPreferences
import com.repforge.app.data.BackupManager
import com.repforge.app.data.SettingsStore
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

    fun setRestSeconds(value: Int) = viewModelScope.launch { store.setRestSeconds(value) }
    fun setHapticsEnabled(value: Boolean) = viewModelScope.launch { store.setHapticsEnabled(value) }

    fun exportBackup(write: (String) -> Unit) = viewModelScope.launch {
        runCatching {
            val json = backup.exportJson()
            withContext(Dispatchers.IO) { write(json) }
        }.fold(
            { _messages.send("Backup exportado.") },
            { _messages.send("Falha ao exportar: ${it.message ?: "erro desconhecido"}") }
        )
    }

    fun importBackup(read: () -> String) = viewModelScope.launch {
        runCatching { backup.importJson(withContext(Dispatchers.IO) { read() }) }.fold(
            { count -> _messages.send(if (count == 0) "Backup vazio; nada importado." else "$count ficha(s) importada(s).") },
            { _messages.send("Falha ao importar: ${it.message ?: "erro desconhecido"}") }
        )
    }
}
