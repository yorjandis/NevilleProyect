package com.ypg.neville.feature.cardiocoherence.data

import com.ypg.neville.feature.cardiocoherence.domain.InitialEmotionalState
import com.ypg.neville.feature.cardiocoherence.domain.MeditationPhaseKind
import com.ypg.neville.feature.cardiocoherence.domain.MeditationSessionRecord
import com.ypg.neville.feature.cardiocoherence.domain.PostSessionEmotion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CardioCoherenceRepository(
    private val dao: MeditationSessionRecordDao
) {

    suspend fun save(record: MeditationSessionRecord): Long {
        return dao.insert(record.toEntity())
    }

    fun observeRecords(): Flow<List<MeditationSessionRecord>> {
        return dao.observeAll().map { records -> records.map { it.toDomain() } }
    }

    suspend fun loadRecords(): List<MeditationSessionRecord> {
        return dao.all().map { it.toDomain() }
    }

    suspend fun latest(): MeditationSessionRecord? {
        return dao.latest()?.toDomain()
    }
}

private fun MeditationSessionRecord.toEntity(): MeditationSessionRecordEntity {
    return MeditationSessionRecordEntity(
        id = id,
        dateEpochMillis = dateEpochMillis,
        durationMinutes = durationMinutes,
        initialState = initialState.name,
        intention = intention,
        beforeScore = beforeScore,
        afterScore = afterScore,
        mentalClarityScore = mentalClarityScore,
        heartConnectionScore = heartConnectionScore,
        predominantEmotion = predominantEmotion.name,
        closingWord = closingWord,
        phasesCompleted = phasesCompleted.joinToString("|") { it.name }
    )
}

private fun MeditationSessionRecordEntity.toDomain(): MeditationSessionRecord {
    return MeditationSessionRecord(
        id = id,
        dateEpochMillis = dateEpochMillis,
        durationMinutes = durationMinutes,
        initialState = runCatching { InitialEmotionalState.valueOf(initialState) }
            .getOrDefault(InitialEmotionalState.NEUTRAL),
        intention = intention,
        beforeScore = beforeScore,
        afterScore = afterScore,
        mentalClarityScore = mentalClarityScore,
        heartConnectionScore = heartConnectionScore,
        predominantEmotion = runCatching { PostSessionEmotion.valueOf(predominantEmotion) }
            .getOrDefault(PostSessionEmotion.CALM),
        closingWord = closingWord,
        phasesCompleted = phasesCompleted
            .split("|")
            .filter { it.isNotBlank() }
            .mapNotNull { raw -> runCatching { MeditationPhaseKind.valueOf(raw) }.getOrNull() }
    )
}
