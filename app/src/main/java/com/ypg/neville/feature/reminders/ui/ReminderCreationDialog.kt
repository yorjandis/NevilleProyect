package com.ypg.neville.feature.reminders.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ypg.neville.R
import com.ypg.neville.model.reminders.ReminderFrequency
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

private enum class NoteReminderMode {
    INTERVAL,
    DAILY,
    DATE,
    MONTHLY,
    YEARLY
}

/**
 * Shared reminder creation view. Callers can optionally prefill its title and message.
 * The Reminders tool and note exports both use this same component.
 */
@Composable
fun ReminderCreationDialog(
    initialTitle: String = "",
    initialMessage: String = "",
    onDismiss: () -> Unit,
    onSave: (title: String, message: String, frequency: ReminderFrequency) -> Unit
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val titleRequiredMessage = stringResource(R.string.reminders_title_required)
    val messageRequiredMessage = stringResource(R.string.reminders_message_required)
    val invalidTimeMessage = stringResource(R.string.reminders_invalid_time)
    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    var message by remember(initialMessage) { mutableStateOf(initialMessage) }
    var mode by remember { mutableStateOf(NoteReminderMode.INTERVAL) }
    var intervalHours by remember { mutableStateOf("0") }
    var intervalMinutes by remember { mutableStateOf("5") }
    var dailyHour by remember { mutableStateOf("8") }
    var dailyMinute by remember { mutableStateOf("0") }
    var dateMillis by remember {
        mutableLongStateOf(System.currentTimeMillis() + DEFAULT_REMINDER_DELAY_MILLIS)
    }
    var monthlyDay by remember { mutableStateOf("1") }
    var monthlyHour by remember { mutableStateOf("8") }
    var monthlyMinute by remember { mutableStateOf("0") }
    var yearlyMonth by remember { mutableStateOf("1") }
    var yearlyDay by remember { mutableStateOf("1") }
    var yearlyHour by remember { mutableStateOf("8") }
    var yearlyMinute by remember { mutableStateOf("0") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminders_new_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.common_title)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text(stringResource(R.string.common_content)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Text(
                    stringResource(R.string.reminders_frequency),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .background(Color(0x14000000), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NoteReminderMode.entries.forEach { item ->
                        val selected = item == mode
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) Color(0xFFDEB887) else Color.Transparent,
                            modifier = Modifier.clickable {
                                mode = item
                                errorText = null
                            }
                        ) {
                            Text(
                                text = stringResource(item.labelResource()),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                when (mode) {
                    NoteReminderMode.INTERVAL -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(stringResource(R.string.reminders_hours), intervalHours) {
                            intervalHours = it
                        }
                        NumberField(stringResource(R.string.reminders_minutes), intervalMinutes) {
                            intervalMinutes = it
                        }
                    }

                    NoteReminderMode.DAILY -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(stringResource(R.string.reminders_hour), dailyHour) {
                            dailyHour = it
                        }
                        NumberField(stringResource(R.string.reminders_minute), dailyMinute) {
                            dailyMinute = it
                        }
                    }

                    NoteReminderMode.DATE -> Button(
                        onClick = {
                            pickDateTime(context, dateMillis) { selected -> dateMillis = selected }
                        }
                    ) {
                        val formatter = SimpleDateFormat("d MMM yyyy, HH:mm", locale)
                        Text(
                            stringResource(
                                R.string.reminders_date_value,
                                formatter.format(Date(dateMillis))
                            )
                        )
                    }

                    NoteReminderMode.MONTHLY -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberField(stringResource(R.string.reminders_day), monthlyDay) {
                                monthlyDay = it
                            }
                            NumberField(stringResource(R.string.reminders_hour), monthlyHour) {
                                monthlyHour = it
                            }
                            NumberField(stringResource(R.string.reminders_short_minute), monthlyMinute) {
                                monthlyMinute = it
                            }
                        }
                        Text(
                            text = stringResource(R.string.reminders_missing_day_hint),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    NoteReminderMode.YEARLY -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberField(stringResource(R.string.reminders_month), yearlyMonth) {
                                yearlyMonth = it
                            }
                            NumberField(stringResource(R.string.reminders_day), yearlyDay) {
                                yearlyDay = it
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberField(stringResource(R.string.reminders_hour), yearlyHour) {
                                yearlyHour = it
                            }
                            NumberField(stringResource(R.string.reminders_short_minute), yearlyMinute) {
                                yearlyMinute = it
                            }
                        }
                    }
                }

                errorText?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val titleTrimmed = title.trim()
                val messageTrimmed = message.trim()
                when {
                    titleTrimmed.isEmpty() -> {
                        errorText = titleRequiredMessage
                    }

                    messageTrimmed.isEmpty() -> {
                        errorText = messageRequiredMessage
                    }

                    else -> {
                        val frequency = buildFrequency(
                            mode = mode,
                            intervalHours = intervalHours,
                            intervalMinutes = intervalMinutes,
                            dailyHour = dailyHour,
                            dailyMinute = dailyMinute,
                            dateMillis = dateMillis,
                            monthlyDay = monthlyDay,
                            monthlyHour = monthlyHour,
                            monthlyMinute = monthlyMinute,
                            yearlyMonth = yearlyMonth,
                            yearlyDay = yearlyDay,
                            yearlyHour = yearlyHour,
                            yearlyMinute = yearlyMinute
                        )
                        if (frequency == null) {
                            errorText = invalidTimeMessage
                        } else {
                            onSave(titleTrimmed, messageTrimmed, frequency)
                        }
                    }
                }
            }) {
                Text(
                    if (mode == NoteReminderMode.DATE) {
                        stringResource(R.string.reminders_schedule)
                    } else {
                        stringResource(R.string.common_save)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun RowScope.NumberField(
    label: String,
    value: String,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.all(Char::isDigit)) onChange(input)
        },
        label = { Text(label) },
        modifier = Modifier.weight(1f),
        singleLine = true
    )
}

private fun buildFrequency(
    mode: NoteReminderMode,
    intervalHours: String,
    intervalMinutes: String,
    dailyHour: String,
    dailyMinute: String,
    dateMillis: Long,
    monthlyDay: String,
    monthlyHour: String,
    monthlyMinute: String,
    yearlyMonth: String,
    yearlyDay: String,
    yearlyHour: String,
    yearlyMinute: String
): ReminderFrequency? = when (mode) {
    NoteReminderMode.INTERVAL -> {
        val hours = intervalHours.toIntOrNull() ?: -1
        val minutes = intervalMinutes.toIntOrNull() ?: -1
        if (hours < 0 || minutes !in 0..59 || hours == 0 && minutes == 0) null
        else ReminderFrequency.Interval(hours, minutes)
    }

    NoteReminderMode.DAILY -> {
        val hour = dailyHour.toIntOrNull() ?: -1
        val minute = dailyMinute.toIntOrNull() ?: -1
        if (hour !in 0..23 || minute !in 0..59) null
        else ReminderFrequency.Daily(hour, minute)
    }

    NoteReminderMode.DATE -> {
        if (dateMillis <= System.currentTimeMillis()) null
        else ReminderFrequency.DateOnce(dateMillis)
    }

    NoteReminderMode.MONTHLY -> {
        val day = monthlyDay.toIntOrNull() ?: -1
        val hour = monthlyHour.toIntOrNull() ?: -1
        val minute = monthlyMinute.toIntOrNull() ?: -1
        if (day !in 1..31 || hour !in 0..23 || minute !in 0..59) null
        else ReminderFrequency.Monthly(day, hour, minute)
    }

    NoteReminderMode.YEARLY -> {
        val month = yearlyMonth.toIntOrNull() ?: -1
        val day = yearlyDay.toIntOrNull() ?: -1
        val hour = yearlyHour.toIntOrNull() ?: -1
        val minute = yearlyMinute.toIntOrNull() ?: -1
        if (month !in 1..12 || day !in 1..31 || hour !in 0..23 || minute !in 0..59) null
        else ReminderFrequency.Yearly(month, day, hour, minute)
    }
}

private fun NoteReminderMode.labelResource(): Int = when (this) {
    NoteReminderMode.INTERVAL -> R.string.reminders_mode_interval
    NoteReminderMode.DAILY -> R.string.reminders_mode_daily
    NoteReminderMode.DATE -> R.string.reminders_mode_date
    NoteReminderMode.MONTHLY -> R.string.reminders_mode_monthly
    NoteReminderMode.YEARLY -> R.string.reminders_mode_yearly
}

private fun pickDateTime(
    context: Context,
    initialMillis: Long,
    onSelected: (Long) -> Unit
) {
    val initial = Calendar.getInstance().apply { timeInMillis = initialMillis }
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    val selected = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onSelected(selected.timeInMillis)
                },
                initial.get(Calendar.HOUR_OF_DAY),
                initial.get(Calendar.MINUTE),
                true
            ).show()
        },
        initial.get(Calendar.YEAR),
        initial.get(Calendar.MONTH),
        initial.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private const val DEFAULT_REMINDER_DELAY_MILLIS = 5 * 60_000L
