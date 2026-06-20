package com.ypg.neville.feature.cardiocoherence.domain

enum class InitialEmotionalState(val label: String, val emoji: String) {
    STRESS("Estrés", "😣"),
    ANXIETY("Ansiedad", "😟"),
    OVERWHELMED("Saturación", "😵‍💫"),
    SAD("Tristeza", "😔"),
    TIRED("Cansancio", "😴"),
    NEUTRAL("Neutral", "😐"),
    CALM("Calma", "😌"),
    GOOD("Bien", "🙂"),
    GRATEFUL("Gratitud", "🙏"),
    ENERGETIC("Energía", "✨")
}

enum class SessionDurationOption(val minutes: Int) {
    FIVE(5),
    TEN(10),
    FIFTEEN(15),
    TWENTY(20),
    THIRTY(30)
}

enum class BreathingRhythmOption(
    val label: String,
    val inhaleMillis: Int,
    val exhaleMillis: Int
) {
    FIVE_FIVE("5s / 5s", 5_000, 5_000),
    FIVE_HALF_FIVE_HALF("5.5s / 5.5s", 5_500, 5_500),
    SIX_SIX("6s / 6s", 6_000, 6_000),
    FOUR_HALF_FIVE_HALF("4.5s / 5.5s", 4_500, 5_500)
}

enum class MeditationPhaseKind(val label: String) {
    REGULATION("Regulación"),
    HEART_CONNECTION("Conexión corazón"),
    EMOTIONAL_ACTIVATION("Activación emocional"),
    INTEGRATION("Integración")
}

data class UserStateModel(
    val initialState: InitialEmotionalState,
    val durationMinutes: Int,
    val intention: String,
    val beforeScore: Int,
    val breathingRhythm: BreathingRhythmOption
)

data class MeditationPhase(
    val kind: MeditationPhaseKind,
    val durationSeconds: Int,
    val title: String,
    val guidance: String,
    val breathingPattern: BreathingPattern,
    val emotionCue: EmotionCue? = null,
    val attentionCues: List<BodyAttentionCue> = emptyList()
)

data class BodyAttentionCue(
    val startFraction: Float,
    val text: String
)

data class BreathingPattern(
    val inhaleMillis: Int,
    val exhaleMillis: Int,
    val inhaleLabel: String = "Inhala",
    val exhaleLabel: String = "Exhala"
) {
    val displayLabel: String
        get() = "${inhaleMillis.toBreathSecondsLabel()} / ${exhaleMillis.toBreathSecondsLabel()}"
}

private fun Int.toBreathSecondsLabel(): String {
    val seconds = this / 1000f
    return if (this % 1000 == 0) {
        "${seconds.toInt()}s"
    } else {
        "${seconds}s"
    }
}

data class EmotionCue(
    val emotion: ElevatedEmotion,
    val prompt: String
)

enum class ElevatedEmotion(val label: String) {
    GRATITUDE("Gratitud"),
    LOVE("Amor"),
    PEACE("Paz"),
    JOY("Alegría")
}

enum class PostSessionEmotion(val label: String, val emoji: String) {
    CALM("Calma", "😌"),
    GRATITUDE("Gratitud", "🙏"),
    LOVE("Amor", "💗"),
    PEACE("Paz", "🕊️"),
    JOY("Alegría", "🙂"),
    CLARITY("Claridad", "✨"),
    HOPE("Esperanza", "🌱"),
    NEUTRAL("Neutral", "😐")
}

data class GeneratedCardioCoherenceSession(
    val userState: UserStateModel,
    val phases: List<MeditationPhase>,
    val totalDurationSeconds: Int
)

data class MeditationSessionRecord(
    val id: Long = 0,
    val dateEpochMillis: Long,
    val durationMinutes: Int,
    val initialState: InitialEmotionalState,
    val intention: String,
    val beforeScore: Int,
    val afterScore: Int,
    val mentalClarityScore: Int,
    val heartConnectionScore: Int,
    val predominantEmotion: PostSessionEmotion,
    val closingWord: String,
    val phasesCompleted: List<MeditationPhaseKind>
)
