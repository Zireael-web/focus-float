package com.focusfloat.app.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.focusfloat.app.design.FocusTextScale
import com.focusfloat.app.design.FocusThemeMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.focusFloatDataStore by preferencesDataStore(name = "focusfloat_settings")

data class FocusSettings(
    val themeMode: FocusThemeMode,
    val textScale: FocusTextScale,
    val showClock: Boolean,
    val showDate: Boolean,
    val onboardingCompleted: Boolean,
    val systemPausedAppsImported: Boolean,
    val primaryPauseCategoryId: Long?,
)

class SettingsRepository(private val context: Context) {
    private val dataStore = context.focusFloatDataStore

    val settings: Flow<FocusSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            FocusSettings(
                themeMode = prefs[Keys.themeMode]?.enumValueOrNull<FocusThemeMode>() ?: FocusThemeMode.Amoled,
                textScale = prefs[Keys.textScale]?.enumValueOrNull<FocusTextScale>() ?: FocusTextScale.Medium,
                showClock = prefs[Keys.showClock] ?: true,
                showDate = prefs[Keys.showDate] ?: true,
                onboardingCompleted = prefs[Keys.onboardingCompleted] ?: false,
                systemPausedAppsImported = prefs[Keys.systemPausedAppsImported] ?: false,
                primaryPauseCategoryId = prefs[Keys.primaryPauseCategoryId]?.takeIf { it > 0 },
            )
        }

    suspend fun setPrimaryPauseCategoryId(id: Long) {
        dataStore.edit { it[Keys.primaryPauseCategoryId] = id }
    }

    suspend fun setThemeMode(mode: FocusThemeMode) {
        dataStore.edit { it[Keys.themeMode] = mode.name }
    }

    suspend fun setTextScale(scale: FocusTextScale) {
        dataStore.edit { it[Keys.textScale] = scale.name }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.onboardingCompleted] = completed }
    }

    suspend fun setSystemPausedAppsImported(imported: Boolean) {
        dataStore.edit { it[Keys.systemPausedAppsImported] = imported }
    }

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val textScale = stringPreferencesKey("text_scale")
        val showClock = booleanPreferencesKey("show_clock")
        val showDate = booleanPreferencesKey("show_date")
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val systemPausedAppsImported = booleanPreferencesKey("system_paused_apps_imported")
        val primaryPauseCategoryId = longPreferencesKey("primary_pause_category_id")
        val executorPreference = stringPreferencesKey("pause_executor_preference")
    }
}

private inline fun <reified T : Enum<T>> String.enumValueOrNull(): T? {
    return enumValues<T>().firstOrNull { it.name == this }
}
