package com.ypg.neville.feature.morningdialog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EveningReviewDao {
    @Query("SELECT * FROM evening_ritual_reviews ORDER BY completedAtEpochMillis DESC")
    fun observeAll(): Flow<List<EveningReviewEntity>>

    @Query("SELECT * FROM evening_ritual_reviews ORDER BY completedAtEpochMillis DESC")
    suspend fun getAll(): List<EveningReviewEntity>

    @Query("SELECT * FROM evening_ritual_reviews WHERE sessionDateEpochDay = :epochDay LIMIT 1")
    suspend fun getByDay(epochDay: Long): EveningReviewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(review: EveningReviewEntity): Long

    @Query("DELETE FROM evening_ritual_reviews WHERE id = :reviewId")
    suspend fun deleteById(reviewId: Long)
}
