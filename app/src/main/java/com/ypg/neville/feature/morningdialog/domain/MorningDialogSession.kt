package com.ypg.neville.feature.morningdialog.domain

data class MorningDialogSession(
    val id: Long = 0,
    val sessionDateEpochDay: Long,
    val completedAtEpochMillis: Long,
    val goals: List<String>,
    val identity: String,
    val emotions: List<String>,
    val anticipatedSituations: List<String>,
    val consciousResponses: List<String>,
    val noteText: String = "",
    val completed: Boolean
)

fun MorningDialogSession.isCompletedToday(todayEpochDay: Long): Boolean {
    return completed && sessionDateEpochDay == todayEpochDay
}
