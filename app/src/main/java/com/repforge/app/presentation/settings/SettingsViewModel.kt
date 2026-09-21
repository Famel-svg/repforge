package com.repforge.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.repforge.app.data.AppPreferences
import com.repforge.app.data.SettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val store: SettingsStore) : ViewModel() {
    val state: StateFlow<AppPreferences> = store.preferences.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), AppPreferences()
    )

    fun setRestSeconds(value: Int) = viewModelScope.launch { store.setRestSeconds(value) }
    fun setHapticsEnabled(value: Boolean) = viewModelScope.launch { store.setHapticsEnabled(value) }
}
