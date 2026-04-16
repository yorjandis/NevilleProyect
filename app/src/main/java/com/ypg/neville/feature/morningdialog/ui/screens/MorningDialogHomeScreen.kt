package com.ypg.neville.feature.morningdialog.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ypg.neville.feature.morningdialog.ui.components.SectionCard
import com.ypg.neville.feature.morningdialog.ui.components.MorningDialogStyles
import com.ypg.neville.feature.morningdialog.utils.MorningDialogCopy

@Composable
fun MorningDialogHomeScreen(
    todayCompleted: Boolean,
    onStartFlow: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MorningDialogStyles.backgroundBrush)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionCard(
            title = MorningDialogCopy.homeTitle,
            body = MorningDialogCopy.homeSubtitle
        ) {
            Text(
                text = if (todayCompleted) "Hoy ya completaste tu diálogo." else "Aún no has completado tu diálogo de hoy.",
                style = MaterialTheme.typography.bodyLarge,
                color = MorningDialogStyles.ritualCardText
            )
            Spacer(modifier = Modifier.height(2.dp))
            Button(
                onClick = onStartFlow,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MorningDialogStyles.buttonColor,
                    contentColor = MorningDialogStyles.buttonTextColor
                )
            ) {
                Text(if (todayCompleted) "Repetir diálogo" else "Iniciar diálogo")
            }
        }

        SectionCard(title = "Explorar") {
            OutlinedButton(
                onClick = onOpenHistory,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MorningDialogStyles.buttonColor,
                    contentColor = MorningDialogStyles.buttonTextColor
                )
            ) {
                Text("Ver historial")
            }
            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MorningDialogStyles.buttonColor,
                    contentColor = MorningDialogStyles.buttonTextColor
                )
            ) {
                Text("Ajustes matutinos")
            }
        }
    }
}
