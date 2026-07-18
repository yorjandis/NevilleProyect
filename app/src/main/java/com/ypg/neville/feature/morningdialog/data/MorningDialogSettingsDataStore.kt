package com.ypg.neville.feature.morningdialog.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.morningDialogDataStore by preferencesDataStore(name = "morning_dialog_settings")

data class MorningDialogSettings(
    val enabled: Boolean = false,
    val hour: Int = 7,
    val minute: Int = 30,
    val eveningReminderEnabled: Boolean = false,
    val eveningReminderHour: Int = 21,
    val eveningReminderMinute: Int = 30,
    val protectClosingReflections: Boolean = false
)

class MorningDialogSettingsDataStore(
    private val context: Context
) {

    val settingsFlow: Flow<MorningDialogSettings> = context.morningDialogDataStore.data
        .map { preferences -> preferences.toSettings() }

    suspend fun getSettings(): MorningDialogSettings {
        return context.morningDialogDataStore.data.first().toSettings()
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.morningDialogDataStore.edit { prefs ->
            prefs[Keys.ENABLED] = enabled
        }
    }

    suspend fun setTime(hour: Int, minute: Int) {
        context.morningDialogDataStore.edit { prefs ->
            prefs[Keys.HOUR] = hour.coerceIn(0, 23)
            prefs[Keys.MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setEveningReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.morningDialogDataStore.edit { prefs ->
            prefs[Keys.EVENING_ENABLED] = enabled
            prefs[Keys.EVENING_HOUR] = hour.coerceIn(0, 23)
            prefs[Keys.EVENING_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setClosingReflectionsProtection(enabled: Boolean) {
        context.morningDialogDataStore.edit { prefs ->
            prefs[Keys.PROTECT_CLOSING_REFLECTIONS] = enabled
        }
    }

    private fun Preferences.toSettings(): MorningDialogSettings {
        return MorningDialogSettings(
            enabled = this[Keys.ENABLED] ?: false,
            hour = (this[Keys.HOUR] ?: 7).coerceIn(0, 23),
            minute = (this[Keys.MINUTE] ?: 30).coerceIn(0, 59),
            eveningReminderEnabled = this[Keys.EVENING_ENABLED] ?: false,
            eveningReminderHour = (this[Keys.EVENING_HOUR] ?: 21).coerceIn(0, 23),
            eveningReminderMinute = (this[Keys.EVENING_MINUTE] ?: 30).coerceIn(0, 59),
            protectClosingReflections = this[Keys.PROTECT_CLOSING_REFLECTIONS] ?: false
        )
    }

    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val HOUR = intPreferencesKey("hour")
        val MINUTE = intPreferencesKey("minute")
        val EVENING_ENABLED = booleanPreferencesKey("evening_enabled")
        val EVENING_HOUR = intPreferencesKey("evening_hour")
        val EVENING_MINUTE = intPreferencesKey("evening_minute")
        val PROTECT_CLOSING_REFLECTIONS = booleanPreferencesKey("protect_closing_reflections")
    }
}
