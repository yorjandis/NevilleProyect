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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ypg.neville.feature.morningdialog.data.MorningDialogSettings
import com.ypg.neville.feature.morningdialog.ui.components.MorningDialogStyles
import com.ypg.neville.feature.morningdialog.ui.components.SectionCard
import java.util.Locale

@Composable
fun MorningDialogSettingsScreen(
    settings: MorningDialogSettings,
    onSaveSettings: (enabled: Boolean, hour: Int, minute: Int) -> Unit
) {
    val context = LocalContext.current
    var enabled by remember(settings.enabled) { mutableStateOf(settings.enabled) }
    var hour by remember(settings.hour) { mutableIntStateOf(settings.hour) }
    var minute by remember(settings.minute) { mutableIntStateOf(settings.minute) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    val notificationsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        infoMessage = if (granted) {
            "Permiso de notificación concedido."
        } else {
            "Sin permiso de notificación, el aviso matutino no podrá mostrarse."
        }
    }

    LaunchedEffect(settings) {
        enabled = settings.enabled
        hour = settings.hour
        minute = settings.minute
    }

    val timeText = remember(hour, minute) {
        String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MorningDialogStyles.backgroundBrush)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Ajustes del ritual matutino",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )

        SectionCard(title = "Configuración") {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Activar ritual diario")
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }

            OutlinedButton(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, selectedHour, selectedMinute ->
                            hour = selectedHour
                            minute = selectedMinute
                        },
                        hour,
                        minute,
                        true
                    ).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MorningDialogStyles.buttonColor,
                    contentColor = MorningDialogStyles.buttonTextColor
                )
            ) {
                Text("Hora diaria: $timeText")
            }

            Button(
                onClick = {
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!hasPermission) {
                            notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    onSaveSettings(enabled, hour, minute)
                    infoMessage = "Cambios guardados y programación actualizada."
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MorningDialogStyles.buttonColor,
                    contentColor = MorningDialogStyles.buttonTextColor
                )
            ) {
                Text("Guardar cambios")
            }
        }

        infoMessage?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = Color.White)
        }
    }
}
