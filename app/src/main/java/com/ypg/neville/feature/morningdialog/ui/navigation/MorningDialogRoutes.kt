package com.ypg.neville.feature.morningdialog.ui.navigation

object MorningDialogRoutes {
    const val HOME = "morning_home"
    const val FLOW = "morning_flow"
    const val HISTORY = "morning_history"
    const val NOTE = "morning_note/{sessionId}"
    const val SETTINGS = "morning_settings"

    fun note(sessionId: Long): String = "morning_note/$sessionId"
}
