package com.ypg.neville.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.preference.PreferenceManager

private val NevilleLightColors = lightColorScheme(
    primary = Color(0xFF365A7C),
    onPrimary = Color.White,
    secondary = Color(0xFF4A6E4A),
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE6EBF1),
    onSurfaceVariant = Color(0xFF40474F)
)

private val NevilleDarkColors = darkColorScheme(
    primary = Color(0xFF97C7F1),
    onPrimary = Color(0xFF002F4D),
    secondary = Color(0xFFA7D1A6),
    background = Color(0xFF121416),
    onBackground = Color(0xFFE3E6EA),
    surface = Color(0xFF191C1F),
    onSurface = Color(0xFFE3E6EA),
    surfaceVariant = Color(0xFF2C3136),
    onSurfaceVariant = Color(0xFFC2C8CF)
)

@Composable
fun NevilleTheme(
    isDarkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (isDarkTheme) NevilleDarkColors else NevilleLightColors,
        content = content
    )
}

@Composable
fun NevilleTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isDarkTheme = PreferenceManager
        .getDefaultSharedPreferences(context)
        .getBoolean("tema", true)
    NevilleTheme(isDarkTheme = isDarkTheme, content = content)
}
