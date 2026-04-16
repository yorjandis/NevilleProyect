package com.ypg.neville.feature.morningdialog.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StepProgress(currentStep: Int, totalSteps: Int, modifier: Modifier = Modifier) {
    val progress = currentStep.toFloat() / totalSteps.toFloat()
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Paso $currentStep de $totalSteps",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.86f)
        )
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
        )
    }
}

@Composable
fun SectionCard(
    title: String,
    body: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MorningDialogStyles.ritualCardColor),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides MorningDialogStyles.ritualCardText) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MorningDialogStyles.ritualCardText,
                    fontWeight = FontWeight.Bold
                )
                body?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MorningDialogStyles.ritualCardText
                    )
                }
                content()
            }
        }
    }
}

@Composable
fun SelectableChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.wrapContentWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) Color(0xFF8CCBFF) else Color(0xFFF2CAA1),
        border = BorderStroke(
            width = 1.dp,
            color = MorningDialogStyles.ritualCardText.copy(alpha = 0.35f)
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MorningDialogStyles.ritualCardText
        )
    }
}

@Composable
fun EditableRow(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    trailingActionText: String,
    onTrailingAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black)
        )
        OutlinedButton(
            onClick = onTrailingAction,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MorningDialogStyles.buttonColor,
                contentColor = MorningDialogStyles.buttonTextColor
            )
        ) {
            Text(trailingActionText)
        }
    }
}
