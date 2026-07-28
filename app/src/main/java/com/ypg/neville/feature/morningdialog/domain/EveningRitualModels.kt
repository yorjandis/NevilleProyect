package com.ypg.neville.feature.morningdialog.domain

data class EveningReview(
    val id: Long = 0,
    val sessionDateEpochDay: Long,
    val completedAtEpochMillis: Long,
    val energy: Int,
    val predominantEmotionId: String,
    val whatWentWell: String,
    val learning: String,
    val autopilotMoment: String,
    val gratitude: String,
    val tomorrowPreparation: String,
    val identityAlignment: Int,
    val suggestion: String,
    val agendaCompletedCount: Int,
    val agendaTotalCount: Int,
    val goalUnitsCompletedCount: Int,
    val presenceReturns: Int,
    val automaticPilotEvents: Int,
    val coherenceSessionsCount: Int,
    val dayContextFingerprint: String,
    val journalEntryRequested: Boolean,
    val journalEntryCreated: Boolean,
    val journalEntryId: Long?
)

data class EveningReviewDraft(
    val energy: Int = 3,
    val predominantEmotionId: String = "sereno",
    val whatWentWell: String = "",
    val learning: String = "",
    val autopilotMoment: String = "",
    val gratitude: String = "",
    val tomorrowPreparation: String = "",
    val identityAlignment: Int = 3,
    val createJournalEntry: Boolean = true
)

data class CompletedAgendaItem(
    val id: String,
    val title: String
)

data class CompletedGoalUnit(
    val goalTitle: String,
    val unitName: String
)

data class EveningDaySnapshot(
    val agendaCompleted: List<CompletedAgendaItem> = emptyList(),
    val agendaTotalCount: Int = 0,
    val completedGoalUnits: List<CompletedGoalUnit> = emptyList(),
    val presenceReturns: Int = 0,
    val automaticPilotEvents: Int = 0,
    val coherenceSessionsCount: Int = 0,
    val coherenceAverageAfterScore: Int? = null
)

fun EveningReview.hasChangedContext(snapshot: EveningDaySnapshot): Boolean =
    (dayContextFingerprint.isNotBlank() && dayContextFingerprint != snapshot.fingerprint()) ||
        (dayContextFingerprint.isBlank() && (
        agendaCompletedCount != snapshot.agendaCompleted.size ||
        agendaTotalCount != snapshot.agendaTotalCount ||
        goalUnitsCompletedCount != snapshot.completedGoalUnits.size ||
        presenceReturns != snapshot.presenceReturns ||
        automaticPilotEvents != snapshot.automaticPilotEvents ||
        coherenceSessionsCount != snapshot.coherenceSessionsCount
        ))

fun EveningDaySnapshot.fingerprint(): String {
    val canonical = buildString {
        append("agendaTotal=").append(agendaTotalCount)
        agendaCompleted.sortedBy { it.id }.forEach {
            append("|agenda=").append(it.id).append(':').append(it.title)
        }
        completedGoalUnits.sortedWith(compareBy({ it.goalTitle }, { it.unitName })).forEach {
            append("|goal=").append(it.goalTitle).append(':').append(it.unitName)
        }
        append("|presence=").append(presenceReturns)
        append("|automatic=").append(automaticPilotEvents)
        append("|coherenceCount=").append(coherenceSessionsCount)
        append("|coherenceAverage=").append(coherenceAverageAfterScore ?: -1)
    }
    return java.security.MessageDigest.getInstance("SHA-256")
        .digest(canonical.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> (byte.toInt() and 0xff).toString(16).padStart(2, '0') }
}
