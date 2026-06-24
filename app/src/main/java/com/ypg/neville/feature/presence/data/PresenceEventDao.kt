package com.ypg.neville.feature.presence.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PresenceEventDao {

    @Insert
    suspend fun insert(event: PresenceEventEntity)

    @Query("SELECT COUNT(*) FROM presence_events WHERE eventType = :type AND createdAtMillis >= :startMillis AND createdAtMillis < :endMillis")
    suspend fun countByTypeBetween(type: String, startMillis: Long, endMillis: Long): Int

    @Query("SELECT COUNT(*) FROM presence_events WHERE moodId = :moodId AND dayStartMillis >= :startMillis")
    suspend fun countMoodSince(moodId: String, startMillis: Long): Int

    @Query("SELECT * FROM presence_events WHERE dayStartMillis >= :startMillis ORDER BY dayStartMillis ASC, createdAtMillis ASC")
    suspend fun eventsSince(startMillis: Long): List<PresenceEventEntity>

    @Query("SELECT COUNT(*) FROM presence_events")
    fun observeTotalCount(): Flow<Int>

    @Query("DELETE FROM presence_events")
    suspend fun deleteAll()
}
