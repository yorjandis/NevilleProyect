package com.ypg.neville.feature.cardiocoherence.domain

class SessionEngine(
    private val breathingModule: BreathingModule = BreathingModule(),
    private val emotionModule: EmotionModule = EmotionModule()
) {

    fun generate(userState: UserStateModel): GeneratedCardioCoherenceSession {
        val totalSeconds = userState.durationMinutes.coerceIn(5, 30) * 60
        val phaseDurations = distributePhaseDurations(
            totalSeconds = totalSeconds,
            weights = weightsFor(userState.initialState)
        )
        val elevatedEmotion = emotionModule.selectEmotion(userState.initialState)

        val phases = listOf(
            MeditationPhase(
                kind = MeditationPhaseKind.REGULATION,
                durationSeconds = phaseDurations[0],
                title = "Regulación",
                guidance = "Respira lento. Permite que el cuerpo baje el ritmo y encuentre estabilidad.",
                breathingPattern = breathingModule.slowCoherencePattern(userState.breathingRhythm),
                attentionCues = bodyAttentionCues()
            ),
            MeditationPhase(
                kind = MeditationPhaseKind.HEART_CONNECTION,
                durationSeconds = phaseDurations[1],
                title = "Conexión corazón",
                guidance = "Lleva la atención al centro del pecho. Imagina que el aire entra y sale desde el corazón.",
                breathingPattern = breathingModule.heartFocusedPattern(userState.breathingRhythm)
            ),
            MeditationPhase(
                kind = MeditationPhaseKind.EMOTIONAL_ACTIVATION,
                durationSeconds = phaseDurations[2],
                title = "Emoción elevada",
                guidance = emotionModule.guidanceFor(elevatedEmotion),
                breathingPattern = breathingModule.softEmotionPattern(userState.breathingRhythm),
                emotionCue = emotionModule.cueFor(elevatedEmotion, userState.intention)
            ),
            MeditationPhase(
                kind = MeditationPhaseKind.INTEGRATION,
                durationSeconds = phaseDurations[3],
                title = "Integración",
                guidance = "Permanece en silencio. Ancla esta coherencia y deja que tu intención quede sentida.",
                breathingPattern = breathingModule.integrationPattern(userState.breathingRhythm),
                emotionCue = userState.intention.takeIf { it.isNotBlank() }?.let {
                    EmotionCue(ElevatedEmotion.PEACE, "Siente tu intención como si ya fuera parte de ti: $it")
                }
            )
        )

        return GeneratedCardioCoherenceSession(
            userState = userState,
            phases = phases,
            totalDurationSeconds = totalSeconds
        )
    }

    private fun weightsFor(state: InitialEmotionalState): List<Float> {
        return when (state) {
            InitialEmotionalState.STRESS -> listOf(0.42f, 0.23f, 0.22f, 0.13f)
            InitialEmotionalState.ANXIETY -> listOf(0.28f, 0.36f, 0.22f, 0.14f)
            InitialEmotionalState.OVERWHELMED -> listOf(0.38f, 0.30f, 0.18f, 0.14f)
            InitialEmotionalState.SAD -> listOf(0.26f, 0.30f, 0.28f, 0.16f)
            InitialEmotionalState.TIRED -> listOf(0.30f, 0.26f, 0.28f, 0.16f)
            InitialEmotionalState.NEUTRAL -> listOf(0.28f, 0.27f, 0.28f, 0.17f)
            InitialEmotionalState.CALM -> listOf(0.22f, 0.27f, 0.33f, 0.18f)
            InitialEmotionalState.GOOD -> listOf(0.20f, 0.22f, 0.40f, 0.18f)
            InitialEmotionalState.GRATEFUL -> listOf(0.18f, 0.22f, 0.42f, 0.18f)
            InitialEmotionalState.ENERGETIC -> listOf(0.18f, 0.24f, 0.40f, 0.18f)
        }
    }

    private fun distributePhaseDurations(totalSeconds: Int, weights: List<Float>): List<Int> {
        val minimumSeconds = 45
        val base = List(weights.size) { minimumSeconds }
        val remaining = (totalSeconds - base.sum()).coerceAtLeast(0)
        val weighted = weights.map { (remaining * it).toInt() }
        val remainder = totalSeconds - base.sum() - weighted.sum()
        return base.mapIndexed { index, min ->
            min + weighted[index] + if (index == weighted.lastIndex) remainder else 0
        }
    }

    private fun bodyAttentionCues(): List<BodyAttentionCue> {
        return listOf(
            BodyAttentionCue(
                startFraction = 0.00f,
                text = "Relaja la mandíbula. Deja que la lengua descanse y que el rostro se suavice."
            ),
            BodyAttentionCue(
                startFraction = 0.20f,
                text = "Baja los hombros. Suelta cualquier esfuerzo innecesario en cuello y espalda."
            ),
            BodyAttentionCue(
                startFraction = 0.40f,
                text = "Siente el peso del cuerpo. Permite que el soporte debajo de ti te sostenga."
            ),
            BodyAttentionCue(
                startFraction = 0.60f,
                text = "Nota el pecho y el esternón. Lleva ahí una atención tranquila, sin forzar."
            ),
            BodyAttentionCue(
                startFraction = 0.80f,
                text = "Suaviza el abdomen. Deja que la respiración se vuelva amplia, lenta y cómoda."
            )
        )
    }
}
