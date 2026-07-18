package com.ypg.neville.feature.morningdialog.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ypg.neville.feature.morningdialog.ui.components.MorningDialogStyles
import com.ypg.neville.feature.morningdialog.ui.components.SectionCard

@Composable
fun MorningDialogHomeScreen(
    todayCompleted: Boolean,
    todayReviewCompleted: Boolean,
    todayReviewNeedsUpdate: Boolean,
    onStartFlow: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenEvening: () -> Unit,
    onUpdateEvening: () -> Unit,
    onOpenSummary: () -> Unit,
    onOpenMyDay: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MorningDialogStyles.backgroundBrush)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Ritual Matutino",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MorningDialogStyles.ritualCardText
        )

        SectionCard(
            title = "Diseña con intención este día",
            body = "Un ritual breve para estructurar tu día con claridad, intención y presencia."
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (todayCompleted) "Completado hoy" else "Ritual pendiente",
                    color = MorningDialogStyles.ritualCardText,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(Icons.Rounded.WbSunny, null, tint = MorningDialogStyles.ritualCardText)
            }
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

        SectionCard(
            title = "Cierre consciente",
            body = if (todayReviewCompleted) {
                "Tu día ya tiene un cierre. Vuelve a él para recordar lo que aprendiste."
            } else {
                "Termina el día con claridad, integra lo vivido y deja una única mejora para mañana."
            }
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (todayReviewCompleted) "Completado hoy" else "Cierre pendiente",
                    color = MorningDialogStyles.ritualCardText,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(Icons.Rounded.NightsStay, null, tint = MorningDialogStyles.ritualCardText)
            }
            if (todayReviewNeedsUpdate) {
                OutlinedButton(
                    onClick = onUpdateEvening,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFFFFE0A3),
                        contentColor = MorningDialogStyles.buttonTextColor
                    )
                ) {
                    Text("Tu día ha cambiado · actualizar cierre", maxLines = 2)
                }
            }
            Button(
                onClick = onOpenEvening,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MorningDialogStyles.eveningButtonColor,
                    contentColor = MorningDialogStyles.buttonTextColor
                )
            ) {
                Text(if (todayReviewCompleted) "Ver cierre" else "Cerrar día")
            }
        }

        SectionCard(title = "Explorar") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExploreButton("Ajustes", Icons.Rounded.Settings, Modifier.weight(1f), onOpenSettings)
                ExploreButton("Historial", Icons.Rounded.History, Modifier.weight(1f), onOpenHistory)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExploreButton("Resumen", Icons.Rounded.BarChart, Modifier.weight(1f), onOpenSummary)
                ExploreButton("Mi día", Icons.Rounded.Timeline, Modifier.weight(1f), onOpenMyDay)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ExploreButton(title: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MorningDialogStyles.exploreButtonColor,
            contentColor = MorningDialogStyles.buttonTextColor
        )
    ) {
        Icon(icon, null)
        Spacer(Modifier.height(4.dp))
        Text(title, maxLines = 1)
    }
}
