package com.ypg.neville.feature.cardiocoherence.domain

class BreathingModule {

    fun slowCoherencePattern(rhythm: BreathingRhythmOption): BreathingPattern {
        return patternFor(rhythm)
    }

    fun heartFocusedPattern(rhythm: BreathingRhythmOption): BreathingPattern {
        return patternFor(rhythm)
    }

    fun softEmotionPattern(rhythm: BreathingRhythmOption): BreathingPattern {
        return patternFor(rhythm)
    }

    fun integrationPattern(rhythm: BreathingRhythmOption): BreathingPattern {
        return BreathingPattern(
            inhaleMillis = rhythm.inhaleMillis,
            exhaleMillis = rhythm.exhaleMillis,
            inhaleLabel = "Recibe",
            exhaleLabel = "Integra"
        )
    }

    private fun patternFor(rhythm: BreathingRhythmOption): BreathingPattern {
        return BreathingPattern(
            inhaleMillis = rhythm.inhaleMillis,
            exhaleMillis = rhythm.exhaleMillis
        )
    }
}
