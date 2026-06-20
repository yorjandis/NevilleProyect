package com.ypg.neville.feature.cardiocoherence.domain

interface GuidanceAudioService {
    suspend fun prepare(session: GeneratedCardioCoherenceSession)
    suspend fun playPhase(phase: MeditationPhase)
    suspend fun pause()
    suspend fun resume()
    suspend fun stop()
}

class NoOpGuidanceAudioService : GuidanceAudioService {
    override suspend fun prepare(session: GeneratedCardioCoherenceSession) = Unit
    override suspend fun playPhase(phase: MeditationPhase) = Unit
    override suspend fun pause() = Unit
    override suspend fun resume() = Unit
    override suspend fun stop() = Unit
}
