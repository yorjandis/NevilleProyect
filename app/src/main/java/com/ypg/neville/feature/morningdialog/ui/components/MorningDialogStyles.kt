package com.ypg.neville.feature.morningdialog.ui.components

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object MorningDialogStyles {
    val backgroundBrush: Brush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F2437),
            Color(0xFF1A3147),
            Color(0xFF243E56)
        )
    )

    val ritualCardColor: Color = Color(0xFFD5B46B)
    val ritualCardText: Color = Color(0xFF111111)

    val buttonColor: Color = Color(0xFF8CCBFF)
    val buttonTextColor: Color = Color(0xFF0E0E0E)
}
