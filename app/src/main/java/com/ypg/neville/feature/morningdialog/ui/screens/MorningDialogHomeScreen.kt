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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ypg.neville.R
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
            stringResource(R.string.ritual_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MorningDialogStyles.ritualCardText
        )

        SectionCard(
            title = stringResource(R.string.ritual_design_day),
            body = stringResource(R.string.ritual_design_day_body)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (todayCompleted) stringResource(R.string.ritual_completed_today) else stringResource(R.string.ritual_pending),
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
                Text(if (todayCompleted) stringResource(R.string.ritual_repeat_dialog) else stringResource(R.string.ritual_start_dialog))
            }
        }

        SectionCard(
            title = stringResource(R.string.ritual_evening),
            body = if (todayReviewCompleted) {
                stringResource(R.string.ritual_evening_completed_body)
            } else {
                stringResource(R.string.ritual_evening_pending_body)
            }
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (todayReviewCompleted) stringResource(R.string.ritual_completed_today) else stringResource(R.string.ritual_evening_pending),
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
                    Text(stringResource(R.string.ritual_day_changed_update), maxLines = 2)
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
                Text(if (todayReviewCompleted) stringResource(R.string.ritual_view_evening) else stringResource(R.string.ritual_close_day))
            }
        }

        SectionCard(title = stringResource(R.string.ritual_explore)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExploreButton(stringResource(R.string.ritual_settings), Icons.Rounded.Settings, Modifier.weight(1f), onOpenSettings)
                ExploreButton(stringResource(R.string.ritual_history), Icons.Rounded.History, Modifier.weight(1f), onOpenHistory)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExploreButton(stringResource(R.string.ritual_summary), Icons.Rounded.BarChart, Modifier.weight(1f), onOpenSummary)
                ExploreButton(stringResource(R.string.ritual_my_day), Icons.Rounded.Timeline, Modifier.weight(1f), onOpenMyDay)
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
