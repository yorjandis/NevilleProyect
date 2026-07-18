package com.ypg.neville.feature.cardiocoherence.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeditationSessionRecordDao {

    @Insert
    suspend fun insert(record: MeditationSessionRecordEntity): Long

    @Query("SELECT * FROM cardio_coherence_records ORDER BY dateEpochMillis DESC")
    fun observeAll(): Flow<List<MeditationSessionRecordEntity>>

    @Query("SELECT * FROM cardio_coherence_records ORDER BY dateEpochMillis DESC")
    suspend fun all(): List<MeditationSessionRecordEntity>

    @Query("SELECT * FROM cardio_coherence_records ORDER BY dateEpochMillis DESC LIMIT 1")
    suspend fun latest(): MeditationSessionRecordEntity?

    @Query(
        "SELECT * FROM cardio_coherence_records " +
            "WHERE dateEpochMillis >= :startInclusive AND dateEpochMillis < :endExclusive " +
            "ORDER BY dateEpochMillis ASC"
    )
    suspend fun getBetween(startInclusive: Long, endExclusive: Long): List<MeditationSessionRecordEntity>
}
