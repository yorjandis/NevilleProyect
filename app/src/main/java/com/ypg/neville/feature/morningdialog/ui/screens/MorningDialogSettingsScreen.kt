package com.ypg.neville.feature.morningdialog.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ypg.neville.feature.morningdialog.data.MorningDialogSettings
import com.ypg.neville.feature.morningdialog.ui.components.MorningDialogStyles
import com.ypg.neville.feature.morningdialog.ui.components.SectionCard
import java.util.Locale

@Composable
fun MorningDialogSettingsScreen(
    settings: MorningDialogSettings,
    onSaveSettings: (
        enabled: Boolean,
        hour: Int,
        minute: Int,
        eveningEnabled: Boolean,
        eveningHour: Int,
        eveningMinute: Int,
        protectClosingReflections: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(settings.enabled) }
    var hour by remember { mutableIntStateOf(settings.hour) }
    var minute by remember { mutableIntStateOf(settings.minute) }
    var eveningEnabled by remember { mutableStateOf(settings.eveningReminderEnabled) }
    var eveningHour by remember { mutableIntStateOf(settings.eveningReminderHour) }
    var eveningMinute by remember { mutableIntStateOf(settings.eveningReminderMinute) }
    var protectClosingReflections by remember { mutableStateOf(settings.protectClosingReflections) }
    var message by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        message = if (granted) "Permiso de notificación concedido." else "Sin permiso, los recordatorios no podrán mostrarse."
    }

    LaunchedEffect(settings) {
        enabled = settings.enabled
        hour = settings.hour
        minute = settings.minute
        eveningEnabled = settings.eveningReminderEnabled
        eveningHour = settings.eveningReminderHour
        eveningMinute = settings.eveningReminderMinute
        protectClosingReflections = settings.protectClosingReflections
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MorningDialogStyles.backgroundBrush)
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Ajustes de rituales", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        ReminderSettingsCard(
            title = "Recordatorio matutino",
            subtitle = "Empieza el día con intención.",
            enabled = enabled,
            hour = hour,
            minute = minute,
            onEnabledChange = { enabled = it },
            onChooseTime = {
                TimePickerDialog(context, { _, h, m -> hour = h; minute = m }, hour, minute, true).show()
            },
            evening = false
        )
        ReminderSettingsCard(
            title = "Recordatorio de cierre",
            subtitle = "Una pausa para integrar el día y preparar mañana.",
            enabled = eveningEnabled,
            hour = eveningHour,
            minute = eveningMinute,
            onEnabledChange = { eveningEnabled = it },
            onChooseTime = {
                TimePickerDialog(context, { _, h, m -> eveningHour = h; eveningMinute = m }, eveningHour, eveningMinute, true).show()
            },
            evening = true
        )

        SectionCard(
            title = "Proteger reflexiones de cierre",
            body = "Pide biometría o el código del dispositivo para abrir cierres y el panel Mi día."
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Protección del dispositivo", Modifier.weight(1f))
                Switch(
                    checked = protectClosingReflections,
                    onCheckedChange = { protectClosingReflections = it }
                )
            }
        }

        Button(
            onClick = {
                if ((enabled || eveningEnabled) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                onSaveSettings(
                    enabled,
                    hour,
                    minute,
                    eveningEnabled,
                    eveningHour,
                    eveningMinute,
                    protectClosingReflections
                )
                message = "Cambios guardados y programación actualizada."
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MorningDialogStyles.buttonColor,
                contentColor = MorningDialogStyles.buttonTextColor
            )
        ) { Text("Guardar cambios") }

        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun ReminderSettingsCard(
    title: String,
    subtitle: String,
    enabled: Boolean,
    hour: Int,
    minute: Int,
    onEnabledChange: (Boolean) -> Unit,
    onChooseTime: () -> Unit,
    evening: Boolean
) {
    SectionCard(title = title, body = subtitle) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (evening) Icons.Rounded.NightsStay else Icons.Rounded.WbSunny, null)
            Text("Activar recordatorio", Modifier.weight(1f).padding(start = 8.dp))
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
        OutlinedButton(
            onClick = onChooseTime,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MorningDialogStyles.buttonColor,
                contentColor = MorningDialogStyles.buttonTextColor
            )
        ) {
            Text("Hora diaria: ${String.format(Locale.getDefault(), "%02d:%02d", hour, minute)}")
        }
    }
}
