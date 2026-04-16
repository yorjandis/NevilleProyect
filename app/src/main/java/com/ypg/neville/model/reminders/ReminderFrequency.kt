package com.ypg.neville.model.reminders

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class ReminderFrequency {
    data class Interval(val hours: Int, val minutes: Int) : ReminderFrequency()
    data class Daily(val hour: Int, val minute: Int) : ReminderFrequency()
    data class DateOnce(val dateMillis: Long) : ReminderFrequency()
    data class Monthly(val day: Int, val hour: Int, val minute: Int) : ReminderFrequency()
    data class Yearly(val month: Int, val day: Int, val hour: Int, val minute: Int) : ReminderFrequency()

    fun description(locale: Locale = Locale("es", "ES")): String {
        return when (this) {
            is Interval -> {
                val parts = mutableListOf<String>()
                if (hours > 0) parts.add("${hours}h")
                if (minutes > 0) parts.add("${minutes}m")
                "Cada ${parts.joinToString(" ")}"
            }
            is Daily -> String.format(locale, "Todos los días a las %02d:%02d", hour, minute)
            is DateOnce -> {
                val formatter = SimpleDateFormat("d MMM yyyy, HH:mm", locale)
                "El ${formatter.format(Date(dateMillis))}"
            }
            is Monthly -> String.format(locale, "Cada mes el día %d a las %02d:%02d", day, hour, minute)
            is Yearly -> {
                val calendar = Calendar.getInstance(locale).apply {
                    set(Calendar.MONTH, month - 1)
                    set(Calendar.DAY_OF_MONTH, day)
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                }
                val formatter = SimpleDateFormat("d 'de' MMMM 'a las' HH:mm", locale)
                "Cada año el ${formatter.format(calendar.time)}"
            }
        }
    }

    fun intervalMillis(): Long? {
        return when (this) {
            is Interval -> ((hours * 60L) + minutes) * 60_000L
            else -> null
        }
    }

    fun nextFireAt(nowMillis: Long): Long? {
        val calendar = Calendar.getInstance()
        return when (this) {
            is Interval -> null
            is Daily -> {
                calendar.timeInMillis = nowMillis
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                if (calendar.timeInMillis <= nowMillis) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }
                calendar.timeInMillis
            }
            is DateOnce -> if (dateMillis > nowMillis) dateMillis else null
            is Monthly -> {
                var result: Long? = null
                repeat(24) { offset ->
                    if (result != null) return@repeat
                    val probe = Calendar.getInstance().apply {
                        timeInMillis = nowMillis
                        add(Calendar.MONTH, offset)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                    }
                    if (probe.get(Calendar.DAY_OF_MONTH) == day && probe.timeInMillis > nowMillis) {
                        result = probe.timeInMillis
                        return@repeat
                    }
                }
                result
            }
            is Yearly -> {
                var result: Long? = null
                repeat(10) { offset ->
                    if (result != null) return@repeat
                    val probe = Calendar.getInstance().apply {
                        timeInMillis = nowMillis
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        set(Calendar.YEAR, get(Calendar.YEAR) + offset)
                        set(Calendar.MONTH, month - 1)
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                    }
                    if (
                        probe.timeInMillis > nowMillis &&
                        probe.get(Calendar.MONTH) == month - 1 &&
                        probe.get(Calendar.DAY_OF_MONTH) == day
                    ) {
                        result = probe.timeInMillis
                        return@repeat
                    }
                }
                result
            }
        }
    }

    companion object {
        fun fromEntity(entity: ReminderEntity): ReminderFrequency? {
            return when (entity.frequencyType) {
                "interval" -> {
                    val h = entity.intervalHours ?: return null
                    val m = entity.intervalMinutes ?: return null
                    Interval(h, m)
                }
                "daily" -> {
                    val h = entity.dailyHour ?: return null
                    val m = entity.dailyMinute ?: return null
                    Daily(h, m)
                }
                "date" -> {
                    val millis = entity.dateTimeMillis ?: return null
                    DateOnce(millis)
                }
                "monthly" -> {
                    val day = entity.monthlyDay ?: return null
                    val h = entity.monthlyHour ?: return null
                    val m = entity.monthlyMinute ?: return null
                    Monthly(day, h, m)
                }
                "yearly" -> {
                    val month = entity.yearlyMonth ?: return null
                    val day = entity.yearlyDay ?: return null
                    val h = entity.yearlyHour ?: return null
                    val m = entity.yearlyMinute ?: return null
                    Yearly(month, day, h, m)
                }
                else -> null
            }
        }

        fun applyToEntity(base: ReminderEntity, frequency: ReminderFrequency): ReminderEntity {
            return when (frequency) {
                is Interval -> base.copy(
                    frequencyType = "interval",
                    intervalHours = frequency.hours,
                    intervalMinutes = frequency.minutes,
                    dailyHour = null,
                    dailyMinute = null,
                    dateTimeMillis = null,
                    monthlyDay = null,
                    monthlyHour = null,
                    monthlyMinute = null,
                    yearlyMonth = null,
                    yearlyDay = null,
                    yearlyHour = null,
                    yearlyMinute = null
                )
                is Daily -> base.copy(
                    frequencyType = "daily",
                    intervalHours = null,
                    intervalMinutes = null,
                    dailyHour = frequency.hour,
                    dailyMinute = frequency.minute,
                    dateTimeMillis = null,
                    monthlyDay = null,
                    monthlyHour = null,
                    monthlyMinute = null,
                    yearlyMonth = null,
                    yearlyDay = null,
                    yearlyHour = null,
                    yearlyMinute = null
                )
                is DateOnce -> base.copy(
                    frequencyType = "date",
                    intervalHours = null,
                    intervalMinutes = null,
                    dailyHour = null,
                    dailyMinute = null,
                    dateTimeMillis = frequency.dateMillis,
                    monthlyDay = null,
                    monthlyHour = null,
                    monthlyMinute = null,
                    yearlyMonth = null,
                    yearlyDay = null,
                    yearlyHour = null,
                    yearlyMinute = null
                )
                is Monthly -> base.copy(
                    frequencyType = "monthly",
                    intervalHours = null,
                    intervalMinutes = null,
                    dailyHour = null,
                    dailyMinute = null,
                    dateTimeMillis = null,
                    monthlyDay = frequency.day,
                    monthlyHour = frequency.hour,
                    monthlyMinute = frequency.minute,
                    yearlyMonth = null,
                    yearlyDay = null,
                    yearlyHour = null,
                    yearlyMinute = null
                )
                is Yearly -> base.copy(
                    frequencyType = "yearly",
                    intervalHours = null,
                    intervalMinutes = null,
                    dailyHour = null,
                    dailyMinute = null,
                    dateTimeMillis = null,
                    monthlyDay = null,
                    monthlyHour = null,
                    monthlyMinute = null,
                    yearlyMonth = frequency.month,
                    yearlyDay = frequency.day,
                    yearlyHour = frequency.hour,
                    yearlyMinute = frequency.minute
                )
            }
        }
    }
}
