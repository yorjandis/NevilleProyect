package com.ypg.neville.feature.weeklysummary.domain

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.content.edit
import com.ypg.neville.R
import com.ypg.neville.feature.weeklysummary.worker.WeeklySummaryReminderScheduler
import com.ypg.neville.model.preferences.DbPreferences
import java.time.DayOfWeek

enum class WeeklySummaryReviewDay(
    val storedValue: Int,
    val dayOfWeek: DayOfWeek,
    @param:StringRes val titleRes: Int
) {
    Sunday(1, DayOfWeek.SUNDAY, R.string.weekly_settings_sunday),
    Monday(2, DayOfWeek.MONDAY, R.string.weekly_settings_monday),
    Tuesday(3, DayOfWeek.TUESDAY, R.string.weekly_settings_tuesday),
    Wednesday(4, DayOfWeek.WEDNESDAY, R.string.weekly_settings_wednesday),
    Thursday(5, DayOfWeek.THURSDAY, R.string.weekly_settings_thursday),
    Friday(6, DayOfWeek.FRIDAY, R.string.weekly_settings_friday),
    Saturday(7, DayOfWeek.SATURDAY, R.string.weekly_settings_saturday);

    companion object {
        fun fromStoredValue(value: Int): WeeklySummaryReviewDay {
            return entries.firstOrNull { it.storedValue == value } ?: Sunday
        }
    }
}

object WeeklySummarySettings {

    const val AVAILABLE_HOUR = 6
    const val DEFAULT_RECORDS_TO_KEEP = 30
    const val MIN_RECORDS_TO_KEEP = 5
    const val MAX_RECORDS_TO_KEEP = 100
    const val RECORD_STEP = 5

    private const val PREF_WEEKDAY = "weekly_summary_review_weekday"
    private const val PREF_NOTIFICATIONS_ENABLED = "weekly_summary_review_notifications_enabled"
    private const val PREF_RECORDS_TO_KEEP = "weekly_summary_records_to_keep"

    data class Config(
        val day: WeeklySummaryReviewDay,
        val notificationsEnabled: Boolean,
        val recordsToKeep: Int
    )

    fun readConfig(context: Context): Config {
        val prefs = DbPreferences.default(context.applicationContext)
        return Config(
            day = WeeklySummaryReviewDay.fromStoredValue(
                prefs.getInt(PREF_WEEKDAY, WeeklySummaryReviewDay.Sunday.storedValue)
            ),
            notificationsEnabled = prefs.getBoolean(PREF_NOTIFICATIONS_ENABLED, false),
            recordsToKeep = normalizedRecordsToKeep(
                prefs.getInt(PREF_RECORDS_TO_KEEP, DEFAULT_RECORDS_TO_KEEP)
            )
        )
    }

    fun setReviewDay(context: Context, day: WeeklySummaryReviewDay): Config {
        val appContext = context.applicationContext
        DbPreferences.default(appContext).edit {
            putInt(PREF_WEEKDAY, day.storedValue)
        }
        WeeklySummaryReminderScheduler.sync(appContext)
        return readConfig(appContext)
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean): Config {
        val appContext = context.applicationContext
        DbPreferences.default(appContext).edit {
            putBoolean(PREF_NOTIFICATIONS_ENABLED, enabled)
        }
        WeeklySummaryReminderScheduler.sync(appContext)
        return readConfig(appContext)
    }

    fun setRecordsToKeep(context: Context, value: Int): Config {
        val appContext = context.applicationContext
        val normalized = normalizedRecordsToKeep(value)
        DbPreferences.default(appContext).edit {
            putInt(PREF_RECORDS_TO_KEEP, normalized)
        }
        return readConfig(appContext)
    }

    fun normalizedRecordsToKeep(value: Int): Int {
        val clamped = value.coerceIn(MIN_RECORDS_TO_KEEP, MAX_RECORDS_TO_KEEP)
        return (clamped / RECORD_STEP) * RECORD_STEP
    }
}
