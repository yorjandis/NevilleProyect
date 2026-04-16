package com.ypg.neville.model.reminders

import android.content.Context
import androidx.core.content.edit
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import com.ypg.neville.model.preferences.DbPreferences

object JournalDailyReminderManager {

    private const val REMINDER_ID = "journal_daily_reminder"
    private const val REMINDER_TITLE = "Recordatorio Diario"

    const val DEFAULT_HOUR = 22
    const val DEFAULT_MINUTE = 0
    const val DEFAULT_MESSAGE = "Recodatorio de escribir en Diario la experiencia del día"

    private const val PREF_ENABLED = "journal_daily_reminder_enabled"
    private const val PREF_HOUR = "journal_daily_reminder_hour"
    private const val PREF_MINUTE = "journal_daily_reminder_minute"
    private const val PREF_MESSAGE = "journal_daily_reminder_message"

    data class Config(
        val enabled: Boolean,
        val hour: Int,
        val minute: Int,
        val customMessage: String
    ) {
        val resolvedMessage: String
            get() = customMessage.trim().ifBlank { DEFAULT_MESSAGE }
    }

    fun readConfig(context: Context): Config {
        val prefs = DbPreferences.default(context.applicationContext)
        return Config(
            enabled = prefs.getBoolean(PREF_ENABLED, true),
            hour = prefs.getInt(PREF_HOUR, DEFAULT_HOUR).coerceIn(0, 23),
            minute = prefs.getInt(PREF_MINUTE, DEFAULT_MINUTE).coerceIn(0, 59),
            customMessage = prefs.getString(PREF_MESSAGE, "").orEmpty()
        )
    }

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        val prefs = DbPreferences.default(appContext)
        var changed = false
        prefs.edit {
            if (!prefs.contains(PREF_ENABLED)) {
                putBoolean(PREF_ENABLED, true)
                changed = true
            }
            if (!prefs.contains(PREF_HOUR)) {
                putInt(PREF_HOUR, DEFAULT_HOUR)
                changed = true
            }
            if (!prefs.contains(PREF_MINUTE)) {
                putInt(PREF_MINUTE, DEFAULT_MINUTE)
                changed = true
            }
            if (!prefs.contains(PREF_MESSAGE)) {
                putString(PREF_MESSAGE, "")
                changed = true
            }
        }
        if (changed) {
            sync(appContext)
            return
        }
        ensureScheduledState(appContext)
    }

    fun setEnabled(context: Context, enabled: Boolean): Config {
        val appContext = context.applicationContext
        DbPreferences.default(appContext).edit { putBoolean(PREF_ENABLED, enabled) }
        return sync(appContext)
    }

    fun setTime(context: Context, hour: Int, minute: Int): Config {
        val appContext = context.applicationContext
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        DbPreferences.default(appContext).edit {
            putInt(PREF_HOUR, safeHour)
            putInt(PREF_MINUTE, safeMinute)
        }
        return sync(appContext)
    }

    fun setCustomMessage(context: Context, customMessage: String): Config {
        val appContext = context.applicationContext
        DbPreferences.default(appContext).edit { putString(PREF_MESSAGE, customMessage.trim()) }
        return sync(appContext)
    }

    fun sync(context: Context): Config {
        val appContext = context.applicationContext
        val config = readConfig(appContext)
        val dao = NevilleRoomDatabase.getInstance(appContext).reminderDao()
        val existing = dao.findById(REMINDER_ID)
        val now = System.currentTimeMillis()

        val base = existing ?: ReminderEntity(
            id = REMINDER_ID,
            title = REMINDER_TITLE,
            message = config.resolvedMessage,
            frequencyType = "daily",
            isStarted = config.enabled,
            startedAt = if (config.enabled) now else null,
            isPinned = true,
            sortOrder = -100
        )

        val updated = ReminderFrequency.applyToEntity(
            base.copy(
                title = REMINDER_TITLE,
                message = config.resolvedMessage,
                isPinned = true
            ),
            ReminderFrequency.Daily(config.hour, config.minute)
        ).copy(
            isStarted = config.enabled,
            startedAt = if (config.enabled) (existing?.startedAt ?: now) else null
        )

        dao.insert(updated)
        if (config.enabled) {
            ReminderScheduler.cancelPending(appContext, REMINDER_ID)
            JournalDailyAlarmScheduler.schedule(appContext, config.hour, config.minute)
        } else {
            ReminderScheduler.cancelPending(appContext, REMINDER_ID)
            JournalDailyAlarmScheduler.cancel(appContext)
        }
        return config
    }

    private fun ensureScheduledState(context: Context) {
        val appContext = context.applicationContext
        val config = readConfig(appContext)
        val dao = NevilleRoomDatabase.getInstance(appContext).reminderDao()
        val entity = dao.findById(REMINDER_ID)
        if (entity == null) {
            sync(appContext)
            return
        }
        if (!config.enabled) {
            ReminderScheduler.cancelPending(appContext, REMINDER_ID)
            JournalDailyAlarmScheduler.cancel(appContext)
            return
        }
        ReminderScheduler.cancelPending(appContext, REMINDER_ID)
        JournalDailyAlarmScheduler.schedule(appContext, config.hour, config.minute)
    }
}
