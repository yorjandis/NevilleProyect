package com.ypg.neville.ui.frag

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.ypg.neville.model.preferences.DbPreferences
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import com.ypg.neville.model.reminders.ReminderEntity
import com.ypg.neville.model.reminders.ReminderFrequency
import com.ypg.neville.model.reminders.ReminderRepository
import com.ypg.neville.model.reminders.ReminderScheduler
import com.ypg.neville.model.subscription.SubscriptionManager
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class FragReminders : Fragment() {

    private val dbExecutor = Executors.newSingleThreadExecutor()

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val db = NevilleRoomDatabase.getInstance(requireContext())
        val repository = ReminderRepository(db.reminderDao())

        (view as ComposeView).setContent {
            com.ypg.neville.ui.theme.NevilleTheme {
                RemindersScreen(repository)
            }
        }
    }

    @Composable
    private fun RemindersScreen(repository: ReminderRepository) {
        val reminders = remember { mutableStateListOf<ReminderEntity>() }
        val context = LocalContext.current
        val hasPremium = SubscriptionManager.hasActiveSubscriptionNow()

        var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
        var showCreate by remember { mutableStateOf(false) }
        var editing by remember { mutableStateOf<ReminderEntity?>(null) }
        var hideTextInProgress by remember {
            mutableStateOf(
                DbPreferences.default(context)
                    .getBoolean("hideTextInProgressReminder", false)
            )
        }

        fun persistHideText(value: Boolean) {
            hideTextInProgress = value
            DbPreferences.default(context)
                .edit()
                .putBoolean("hideTextInProgressReminder", value)
                .apply()
        }

        fun reload() {
            dbExecutor.execute {
                val list = repository.load()
                activity?.runOnUiThread {
                    reminders.clear()
                    reminders.addAll(list)
                }
            }
        }

        fun stop(reminder: ReminderEntity) {
            dbExecutor.execute {
                ReminderScheduler.stop(requireContext(), repository, reminder.id)
                activity?.runOnUiThread { reload() }
            }
        }

        fun resume(reminder: ReminderEntity) {
            dbExecutor.execute {
                ReminderScheduler.resume(requireContext(), repository, reminder.id)
                activity?.runOnUiThread { reload() }
            }
        }

        fun delete(reminder: ReminderEntity) {
            dbExecutor.execute {
                ReminderScheduler.cancel(requireContext(), repository, reminder.id)
                activity?.runOnUiThread { reload() }
            }
        }

        LaunchedEffect(Unit) {
            reload()
        }

        LaunchedEffect(Unit) {
            while (true) {
                delay(1000)
                nowMs = System.currentTimeMillis()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFDFBA), Color(0xFFFFFACD))
                    )
                )
                .padding(10.dp)
        ) {
            if (!hasPremium) {
                SubscriptionLockedState()
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Recordatorios",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(6.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = { showCreate = true }, modifier = Modifier.weight(1f)) {
                            Text("Nuevo")
                        }
                        Button(
                            onClick = { persistHideText(!hideTextInProgress) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (hideTextInProgress) "Mostrar tiempo" else "Ocultar tiempo")
                        }
                    }

                    if (reminders.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No hay recordatorios", style = MaterialTheme.typography.titleMedium)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(reminders, key = { it.id }) { reminder ->
                                ReminderCard(
                                    reminder = reminder,
                                    nowMs = nowMs,
                                    hideTextInProgress = hideTextInProgress,
                                    onEdit = { editing = reminder },
                                    onDelete = { delete(reminder) },
                                    onPlayPause = {
                                        if (reminder.isStarted) stop(reminder) else resume(reminder)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showCreate) {
            ReminderEditorDialog(
                initial = null,
                onDismiss = { showCreate = false },
                onSave = { title, message, frequency ->
                    dbExecutor.execute {
                        val created = repository.create(title, message, frequency)
                        ReminderScheduler.schedule(requireContext(), created)
                        activity?.runOnUiThread {
                            showCreate = false
                            reload()
                        }
                    }
                }
            )
        }

        editing?.let { current ->
            ReminderEditorDialog(
                initial = current,
                onDismiss = { editing = null },
                onSave = { title, message, frequency ->
                    dbExecutor.execute {
                        val latest = repository.get(current.id) ?: return@execute
                        val oldFrequency = ReminderFrequency.fromEntity(latest)
                        val changedFrequency = oldFrequency != frequency

                        val updatedBase = latest.copy(title = title, message = message)
                        val updated = ReminderFrequency.applyToEntity(updatedBase, frequency).copy(
                            startedAt = if (changedFrequency) System.currentTimeMillis() else latest.startedAt,
                            isStarted = latest.isStarted
                        )
                        repository.update(updated)

                        if (changedFrequency && latest.isStarted) {
                            ReminderScheduler.schedule(requireContext(), updated)
                        }

                        activity?.runOnUiThread {
                            editing = null
                            reload()
                        }
                    }
                }
            )
        }
    }

    @Composable
    private fun SubscriptionLockedState() {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 4.dp) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Recordatorios es una función premium", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Activa la suscripción anual para usar este módulo.")
                }
            }
        }
    }

    @Composable
    private fun ReminderCard(
        reminder: ReminderEntity,
        nowMs: Long,
        hideTextInProgress: Boolean,
        onEdit: () -> Unit,
        onDelete: () -> Unit,
        onPlayPause: () -> Unit
    ) {
        val frequency = ReminderFrequency.fromEntity(reminder)
        var expandText by remember(reminder.id) { mutableStateOf(false) }
        val cardTextColor = Color(0xFFFF9800)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF102247),
            tonalElevation = 2.dp,
            shadowElevation = 5.dp
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color = cardTextColor
                    )
                }

                Text(
                    text = reminder.message,
                    maxLines = if (expandText) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { expandText = !expandText },
                    color = cardTextColor
                )

                Text(
                    text = "Frecuencia: ${frequency?.description() ?: "Inválida"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = cardTextColor
                )

                HorizontalDivider(color = cardTextColor.copy(alpha = 0.35f))

                if (reminder.isStarted && reminder.startedAt != null && frequency != null) {
                    ReminderProgress(
                        frequency = frequency,
                        startedAt = reminder.startedAt,
                        nowMs = nowMs,
                        hideText = hideTextInProgress
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onPlayPause, modifier = Modifier.weight(1f)) {
                        Text(if (reminder.isStarted) "Detener" else "Iniciar")
                    }
                    if (reminder.isStarted) {
                        Button(onClick = onEdit, modifier = Modifier.weight(1f)) {
                            Text("Editar")
                        }
                    }
                    Button(onClick = onDelete, modifier = Modifier.weight(1f)) {
                        Text("Borrar")
                    }
                }
            }
        }
    }

    @Composable
    private fun ReminderProgress(
        frequency: ReminderFrequency,
        startedAt: Long,
        nowMs: Long,
        hideText: Boolean
    ) {
        val progress = calculateProgress(frequency, startedAt, nowMs) ?: return

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = { progress.fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            )
            if (!hideText) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatRemaining(progress.remainingMillis),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF8C2A)
                )
            }
        }
    }

    private data class ProgressData(
        val fraction: Float,
        val remainingMillis: Long
    )

    private fun calculateProgress(
        frequency: ReminderFrequency,
        startedAt: Long,
        nowMs: Long
    ): ProgressData? {
        if (nowMs < startedAt) return ProgressData(0f, 0L)

        return when (frequency) {
            is ReminderFrequency.Interval -> {
                val total = frequency.intervalMillis() ?: return null
                if (total <= 0L) return null
                val elapsed = (nowMs - startedAt).coerceAtLeast(0L)
                val cycleElapsed = elapsed % total
                val remaining = (total - cycleElapsed).coerceAtLeast(0L)
                ProgressData(cycleElapsed.toFloat() / total.toFloat(), remaining)
            }
            else -> {
                val next = frequency.nextFireAt(nowMs) ?: return null
                val total = (next - startedAt).coerceAtLeast(1000L)
                val elapsed = (nowMs - startedAt).coerceIn(0L, total)
                val remaining = (next - nowMs).coerceAtLeast(0L)
                ProgressData(elapsed.toFloat() / total.toFloat(), remaining)
            }
        }
    }

    private fun formatRemaining(remainingMillis: Long): String {
        val total = (remainingMillis / 1000L).toInt()

        val secondsInMinute = 60
        val secondsInHour = 3_600
        val secondsInDay = 86_400
        val secondsInMonth = 2_592_000

        val months = total / secondsInMonth
        val days = (total % secondsInMonth) / secondsInDay
        val hours = (total % secondsInDay) / secondsInHour
        val minutes = (total % secondsInHour) / secondsInMinute
        val seconds = total % secondsInMinute

        return when {
            months > 0 -> String.format(Locale.getDefault(), "%dM:%02dd:%02dh", months, days, hours)
            days > 0 -> String.format(Locale.getDefault(), "%dd:%02dh:%02dm", days, hours, minutes)
            hours > 0 -> String.format(Locale.getDefault(), "%02dh:%02dm:%02ds", hours, minutes, seconds)
            else -> String.format(Locale.getDefault(), "%02dm:%02ds", minutes, seconds)
        }
    }

    private enum class ReminderEditorMode(val label: String) {
        INTERVAL("Intervalo"),
        DAILY("Diario"),
        DATE("Fecha"),
        MONTHLY("Mensual"),
        YEARLY("Anual")
    }

    @Composable
    private fun ReminderEditorDialog(
        initial: ReminderEntity?,
        onDismiss: () -> Unit,
        onSave: (title: String, message: String, frequency: ReminderFrequency) -> Unit
    ) {
        val context = LocalContext.current

        var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
        var message by remember(initial?.id) { mutableStateOf(initial?.message.orEmpty()) }

        val initialFrequency = remember(initial?.id) { initial?.let { ReminderFrequency.fromEntity(it) } }

        var mode by remember(initial?.id) {
            mutableStateOf(
                when (initialFrequency) {
                    is ReminderFrequency.Interval -> ReminderEditorMode.INTERVAL
                    is ReminderFrequency.Daily -> ReminderEditorMode.DAILY
                    is ReminderFrequency.DateOnce -> ReminderEditorMode.DATE
                    is ReminderFrequency.Monthly -> ReminderEditorMode.MONTHLY
                    is ReminderFrequency.Yearly -> ReminderEditorMode.YEARLY
                    null -> ReminderEditorMode.INTERVAL
                }
            )
        }

        var intervalHours by remember(initial?.id) {
            mutableStateOf((initialFrequency as? ReminderFrequency.Interval)?.hours?.toString() ?: "0")
        }
        var intervalMinutes by remember(initial?.id) {
            mutableStateOf((initialFrequency as? ReminderFrequency.Interval)?.minutes?.toString() ?: "5")
        }

        var dailyHour by remember(initial?.id) {
            mutableStateOf((initialFrequency as? ReminderFrequency.Daily)?.hour?.toString() ?: "8")
        }
        var dailyMinute by remember(initial?.id) {
            mutableStateOf((initialFrequency as? ReminderFrequency.Daily)?.minute?.toString() ?: "0")
        }

        var dateMillis by remember(initial?.id) {
            mutableLongStateOf((initialFrequency as? ReminderFrequency.DateOnce)?.dateMillis ?: (System.currentTimeMillis() + 5 * 60_000L))
        }

        var monthlyDay by remember(initial?.id) {
            mutableStateOf((initialFrequency as? ReminderFrequency.Monthly)?.day?.toString() ?: "1")
        }
        var monthlyHour by remember(initial?.id) {
            mutableStateOf((initialFrequency as? ReminderFrequency.Monthly)?.hour?.toString() ?: "8")
        }
        var monthlyMinute by remember(initial?.id) {
            mutableStateOf((initialFrequency as? ReminderFrequency.Monthly)?.minute?.toString() ?: "0")
        }

        var yearlyMonth by remember(initial?.id) {
            mutableStateOf((initialFrequency as? ReminderFrequency.Yearly)?.month?.toString() ?: "1")
        }
        var yearlyDay by remember(initial?.id) {
            mutableStateOf((initialFrequency as? ReminderFrequency.Yearly)?.day?.toString() ?: "1")
        }
        var yearlyHour by remember(initial?.id) {
            mutableStateOf((initialFrequency as? ReminderFrequency.Yearly)?.hour?.toString() ?: "8")
        }
        var yearlyMinute by remember(initial?.id) {
            mutableStateOf((initialFrequency as? ReminderFrequency.Yearly)?.minute?.toString() ?: "0")
        }

        var errorText by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (initial == null) "Nuevo recordatorio" else "Editar recordatorio") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Contenido") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    Text("Frecuencia", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .background(
                                color = Color(0x14000000),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReminderEditorMode.entries.forEach { item ->
                            val selected = item == mode
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) Color(0xFFDEB887) else Color.Transparent,
                                modifier = Modifier.clickable { mode = item }
                            ) {
                                Text(
                                    text = item.label,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    when (mode) {
                        ReminderEditorMode.INTERVAL -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NumberField("Horas", intervalHours) { intervalHours = it }
                                NumberField("Minutos", intervalMinutes) { intervalMinutes = it }
                            }
                        }
                        ReminderEditorMode.DAILY -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NumberField("Hora", dailyHour) { dailyHour = it }
                                NumberField("Minuto", dailyMinute) { dailyMinute = it }
                            }
                        }
                        ReminderEditorMode.DATE -> {
                            Button(onClick = {
                                pickDateTime(context, dateMillis) { selected ->
                                    dateMillis = selected
                                }
                            }) {
                                val formatter = SimpleDateFormat("d MMM yyyy, HH:mm", Locale("es", "ES"))
                                Text("Fecha: ${formatter.format(Date(dateMillis))}")
                            }
                        }
                        ReminderEditorMode.MONTHLY -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NumberField("Día", monthlyDay) { monthlyDay = it }
                                NumberField("Hora", monthlyHour) { monthlyHour = it }
                                NumberField("Min", monthlyMinute) { monthlyMinute = it }
                            }
                            Text(
                                text = "Si el día no existe en un mes (p. ej. 30 en febrero), ese mes se omite.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        ReminderEditorMode.YEARLY -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NumberField("Mes", yearlyMonth) { yearlyMonth = it }
                                NumberField("Día", yearlyDay) { yearlyDay = it }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NumberField("Hora", yearlyHour) { yearlyHour = it }
                                NumberField("Min", yearlyMinute) { yearlyMinute = it }
                            }
                        }
                    }

                    if (errorText != null) {
                        Text(errorText!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val titleTrim = title.trim()
                    if (titleTrim.isEmpty()) {
                        errorText = "Debes introducir un título para el recordatorio"
                        return@TextButton
                    }

                    val messageTrim = message.trim()
                    if (messageTrim.isEmpty()) {
                        errorText = "Debes introducir un mensaje para el recordatorio"
                        return@TextButton
                    }

                    val frequency = when (mode) {
                        ReminderEditorMode.INTERVAL -> {
                            val h = intervalHours.toIntOrNull() ?: -1
                            val m = intervalMinutes.toIntOrNull() ?: -1
                            if (h < 0 || m < 0 || m > 59 || (h == 0 && m == 0)) null
                            else ReminderFrequency.Interval(h, m)
                        }
                        ReminderEditorMode.DAILY -> {
                            val h = dailyHour.toIntOrNull() ?: -1
                            val m = dailyMinute.toIntOrNull() ?: -1
                            if (h !in 0..23 || m !in 0..59) null
                            else ReminderFrequency.Daily(h, m)
                        }
                        ReminderEditorMode.DATE -> {
                            if (dateMillis <= System.currentTimeMillis()) null
                            else ReminderFrequency.DateOnce(dateMillis)
                        }
                        ReminderEditorMode.MONTHLY -> {
                            val day = monthlyDay.toIntOrNull() ?: -1
                            val hour = monthlyHour.toIntOrNull() ?: -1
                            val min = monthlyMinute.toIntOrNull() ?: -1
                            if (day !in 1..31 || hour !in 0..23 || min !in 0..59) null
                            else ReminderFrequency.Monthly(day, hour, min)
                        }
                        ReminderEditorMode.YEARLY -> {
                            val month = yearlyMonth.toIntOrNull() ?: -1
                            val day = yearlyDay.toIntOrNull() ?: -1
                            val hour = yearlyHour.toIntOrNull() ?: -1
                            val min = yearlyMinute.toIntOrNull() ?: -1
                            if (month !in 1..12 || day !in 1..31 || hour !in 0..23 || min !in 0..59) null
                            else ReminderFrequency.Yearly(month, day, hour, min)
                        }
                    }

                    if (frequency == null) {
                        errorText = "Debes elegir un tiempo válido para el recordatorio"
                        return@TextButton
                    }

                    onSave(titleTrim, messageTrim, frequency)
                }) {
                    Text(if (mode == ReminderEditorMode.DATE) "Programar" else "Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        )
    }

    @Composable
    private fun RowScope.NumberField(label: String, value: String, onChange: (String) -> Unit) {
        OutlinedTextField(
            value = value,
            onValueChange = { input ->
                if (input.all { it.isDigit() }) onChange(input)
            },
            label = { Text(label) },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
    }

    private fun pickDateTime(
        context: android.content.Context,
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
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            set(Calendar.HOUR_OF_DAY, hourOfDay)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onSelected(cal.timeInMillis)
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
}
