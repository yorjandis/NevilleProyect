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
    val minute: Int = 30
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

    private fun Preferences.toSettings(): MorningDialogSettings {
        return MorningDialogSettings(
            enabled = this[Keys.ENABLED] ?: false,
            hour = (this[Keys.HOUR] ?: 7).coerceIn(0, 23),
            minute = (this[Keys.MINUTE] ?: 30).coerceIn(0, 59)
        )
    }

    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val HOUR = intPreferencesKey("hour")
        val MINUTE = intPreferencesKey("minute")
    }
}
