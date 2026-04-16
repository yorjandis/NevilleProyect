package com.ypg.neville.feature.morningdialog.ui.components

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object MorningDialogStyles {
    val backgroundBrush: Brush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF302973),
            Color(0xFF2B5372),
            Color(0xFF6BA1CE)
        )
    )

    val ritualCardColor: Color = Color(0xFFEFE3C7)
    val ritualCardText: Color = Color(0xFF111111)

    val buttonColor: Color = Color(0xFF8CCBFF)
    val buttonTextColor: Color = Color(0xFF0E0E0E)
}
