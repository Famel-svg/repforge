package com.repforge.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.repForgePreferences by preferencesDataStore("repforge_preferences")

enum class ThemeMode { System, Light, Dark }

/** Brand accents as ARGB: [dark] is used on dark surfaces, [light] on light ones (both checked for WCAG AA in AccentContrastTest). */
enum class Accent(val dark: Long, val light: Long) {
    Ember(0xFFFF8A3D, 0xFFA63600), Rose(0xFFFF7A93, 0xFFB3123F), Violet(0xFFB9A2FF, 0xFF6340C9), Indigo(0xFF9DB0FF, 0xFF3949B8),
    Sky(0xFF5CC8FF, 0xFF006494), Teal(0xFF3FD9C6, 0xFF006B60), Green(0xFF7ADB6E, 0xFF2B6B1F), Gold(0xFFF2C14E, 0xFF7A5600)
}

data class AppPreferences(
    val restSeconds: Int = 90, val hapticsEnabled: Boolean = true, val restNotification: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.System, val accent: Accent = Accent.Ember, val soundsEnabled: Boolean = true
)

class SettingsStore(private val context: Context) {
    private object Keys {
        val restSeconds = intPreferencesKey("rest_seconds")
        val hapticsEnabled = booleanPreferencesKey("haptics_enabled")
        val restNotification = booleanPreferencesKey("rest_notification")
        val themeMode = stringPreferencesKey("theme_mode")
        val accent = stringPreferencesKey("accent")
        val soundsEnabled = booleanPreferencesKey("sounds_enabled")
        val onboardingDone = booleanPreferencesKey("onboarding_done")
    }

    val preferences: Flow<AppPreferences> = context.repForgePreferences.data.map { values ->
        AppPreferences(values[Keys.restSeconds] ?: 90, values[Keys.hapticsEnabled] ?: true, values[Keys.restNotification] ?: true,
            ThemeMode.entries.firstOrNull { it.name == values[Keys.themeMode] } ?: ThemeMode.System,
            Accent.entries.firstOrNull { it.name == values[Keys.accent] } ?: Accent.Ember,
            values[Keys.soundsEnabled] ?: true
        )
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

    suspend fun setRestNotification(value: Boolean) {
        context.repForgePreferences.edit { it[Keys.restNotification] = value }
    }

    suspend fun setThemeMode(value: ThemeMode) {
        context.repForgePreferences.edit { it[Keys.themeMode] = value.name }
    }

    suspend fun setAccent(value: Accent) {
        context.repForgePreferences.edit { it[Keys.accent] = value.name }
    }

    suspend fun setSoundsEnabled(value: Boolean) {
        context.repForgePreferences.edit { it[Keys.soundsEnabled] = value }
    }
}
