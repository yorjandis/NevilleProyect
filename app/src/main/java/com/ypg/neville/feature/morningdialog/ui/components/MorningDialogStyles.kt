package com.ypg.neville.feature.morningdialog.ui.components

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object MorningDialogStyles {
    val backgroundBrush: Brush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE68073),
            Color(0xFFA880CC)
        )
    )

    val ritualCardColor: Color = Color(0xFFF2CAA1)
    val ritualCardText: Color = Color(0xFF111111)

    val buttonColor: Color = Color(0xFF8CCBFF)
    val eveningButtonColor: Color = Color(0xFF9F92E8)
    val exploreButtonColor: Color = Color(0x33FFFFFF)
    val buttonTextColor: Color = Color(0xFF0E0E0E)
}
