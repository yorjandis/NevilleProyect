package com.ypg.neville.feature.morningdialog.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ypg.neville.feature.morningdialog.data.MorningDialogSettings

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    com.ypg.neville.ui.theme.NevilleTheme {
        MorningDialogHomeScreen(
            todayCompleted = false,
            onStartFlow = {},
            onOpenHistory = {},
            onOpenSettings = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    com.ypg.neville.ui.theme.NevilleTheme {
        MorningDialogSettingsScreen(
            settings = MorningDialogSettings(enabled = true, hour = 7, minute = 30),
            onSaveSettings = { _, _, _ -> }
        )
    }
}
