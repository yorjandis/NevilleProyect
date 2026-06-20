package com.ypg.neville.feature.cardiocoherence.domain

class EmotionModule {

    fun selectEmotion(initialState: InitialEmotionalState): ElevatedEmotion {
        return when (initialState) {
            InitialEmotionalState.STRESS -> ElevatedEmotion.PEACE
            InitialEmotionalState.ANXIETY -> ElevatedEmotion.LOVE
            InitialEmotionalState.OVERWHELMED -> ElevatedEmotion.PEACE
            InitialEmotionalState.SAD -> ElevatedEmotion.LOVE
            InitialEmotionalState.TIRED -> ElevatedEmotion.JOY
            InitialEmotionalState.NEUTRAL -> ElevatedEmotion.GRATITUDE
            InitialEmotionalState.CALM -> ElevatedEmotion.GRATITUDE
            InitialEmotionalState.GOOD -> ElevatedEmotion.JOY
            InitialEmotionalState.GRATEFUL -> ElevatedEmotion.GRATITUDE
            InitialEmotionalState.ENERGETIC -> ElevatedEmotion.JOY
        }
    }

    fun guidanceFor(emotion: ElevatedEmotion): String {
        return when (emotion) {
            ElevatedEmotion.GRATITUDE -> "Trae un recuerdo sencillo por el que puedas sentir gratitud real. Deja que crezca en el pecho."
            ElevatedEmotion.LOVE -> "Recuerda a alguien, un lugar o un instante que despierte amor. Respira dentro de esa sensación."
            ElevatedEmotion.PEACE -> "Visualiza una escena serena. Permite que la paz se vuelva una sensación corporal."
            ElevatedEmotion.JOY -> "Evoca un momento de alegría limpia. Sonríe suavemente y deja que el cuerpo lo reconozca."
        }
    }

    fun cueFor(emotion: ElevatedEmotion, intention: String): EmotionCue {
        val suffix = intention.takeIf { it.isNotBlank() }?.let {
            " Orienta esa emoción hacia tu intención: $it"
        }.orEmpty()
        return EmotionCue(
            emotion = emotion,
            prompt = "${emotion.label}: permite que esta emoción sea estable, amplia y natural.$suffix"
        )
    }
}
