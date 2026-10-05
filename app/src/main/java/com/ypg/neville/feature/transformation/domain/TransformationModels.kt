package com.ypg.neville.feature.transformation.domain

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

data class TransformationConfiguration(
    val patternName: String,
    val trigger: String,
    val automaticThought: String,
    val emotion: String,
    val oldBehavior: String,
    val consequence: String,
    val alternativeBehavior: String,
    val toleratedEmotion: String,
    val identity: String,
    val startedAtMillis: Long,
    val remindersEnabled: Boolean,
    val morningMinuteOfDay: Int,
    val pauseMinuteOfDay: Int,
    val eveningMinuteOfDay: Int
) {
    val patternFormula: String
        get() = "Cuando ocurre $trigger, suelo pensar $automaticThought, siento $emotion y termino $oldBehavior, lo que produce $consequence."

    val alternativeFormula: String
        get() = "Cuando ocurra $trigger, haré $alternativeBehavior, aunque sienta $toleratedEmotion."
}

data class TransformationScore(
    val awareness: Int = 0,
    val pause: Int = 0,
    val regulation: Int = 0,
    val alternativeBehavior: Int = 0,
    val recovery: Int = 0
) {
    val total: Int get() = awareness + pause + regulation + alternativeBehavior + recovery
}

data class TransformationJournal(
    val situation: String = "",
    val thought: String = "",
    val emotionAndBody: String = "",
    val impulse: String = "",
    val response: String = "",
    val learning: String = "",
    val maximumIntensity: Int = 0,
    val recoveryMinutes: Int = 0
)

data class TransformationDayEntry(
    val day: Int,
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val commitment: String = "",
    val exerciseNotes: String = "",
    val morningCompleted: Boolean = false,
    val actionCompleted: Boolean = false,
    val eveningCompleted: Boolean = false,
    val journal: TransformationJournal = TransformationJournal(),
    val score: TransformationScore = TransformationScore(),
    val evidences: List<String> = emptyList()
) {
    val isCompleted: Boolean get() = morningCompleted && actionCompleted && eveningCompleted
}

data class TransformationParaEvent(
    val id: String = UUID.randomUUID().toString(),
    val createdAtMillis: Long = System.currentTimeMillis(),
    val perceivedSignal: String,
    val emotion: String,
    val alternativeAction: String,
    val pauseSeconds: Int
)

data class TransformationState(
    val schemaVersion: Int = 1,
    val configuration: TransformationConfiguration? = null,
    val entries: List<TransformationDayEntry> = emptyList(),
    val paraEvents: List<TransformationParaEvent> = emptyList(),
    val completedAtMillis: Long? = null
) {
    val completedDays: Int get() = entries.count { it.isCompleted }
    val progress: Float get() = completedDays / 21f
    val averageScore: Double
        get() = entries.filter { it.eveningCompleted || it.score.total > 0 }
            .map { it.score.total }
            .takeIf { it.isNotEmpty() }
            ?.average() ?: 0.0
}

fun TransformationState.currentDay(
    clock: Clock = Clock.systemDefaultZone(),
    zoneId: ZoneId = ZoneId.systemDefault()
): Int {
    val started = configuration?.startedAtMillis ?: return 1
    val startDay = Instant.ofEpochMilli(started).atZone(zoneId).toLocalDate()
    val today = LocalDate.now(clock.withZone(zoneId))
    return (ChronoUnit.DAYS.between(startDay, today).toInt() + 1).coerceIn(1, 21)
}

data class TransformationDayPlan(
    val number: Int,
    val title: String,
    val phase: String,
    val objective: String,
    val exercise: String,
    val prompts: List<String>,
    val action: String,
    val principle: String? = null
)

data class TransformationExample(
    val title: String,
    val patternName: String,
    val trigger: String,
    val automaticThought: String,
    val emotion: String,
    val oldBehavior: String,
    val consequence: String,
    val alternativeBehavior: String,
    val toleratedEmotion: String,
    val identity: String
)

data class TransformationRoutineStep(val title: String, val minutes: Int, val detail: String)
