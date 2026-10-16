package com.repforge.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.repForgePreferences by preferencesDataStore("repforge_preferences")

data class AppPreferences(val restSeconds: Int = 90, val hapticsEnabled: Boolean = true)

class SettingsStore(private val context: Context) {
    private object Keys {
        val restSeconds = intPreferencesKey("rest_seconds")
        val hapticsEnabled = booleanPreferencesKey("haptics_enabled")
        val onboardingDone = booleanPreferencesKey("onboarding_done")
    }

    val preferences: Flow<AppPreferences> = context.repForgePreferences.data.map { values ->
        AppPreferences(values[Keys.restSeconds] ?: 90, values[Keys.hapticsEnabled] ?: true)
    }

    suspend fun setRestSeconds(value: Int) {
        context.repForgePreferences.edit { it[Keys.restSeconds] = value.coerceIn(15, 600) }
    }

    // null until DataStore emits its first value, so the UI can avoid flashing the wrong start screen.
    val onboardingDone: Flow<Boolean> = context.repForgePreferences.data.map { it[Keys.onboardingDone] ?: false }

    suspend fun setOnboardingDone() {
        context.repForgePreferences.edit { it[Keys.onboardingDone] = true }
    }

    suspend fun setHapticsEnabled(value: Boolean) {
        context.repForgePreferences.edit { it[Keys.hapticsEnabled] = value }
    }
}
