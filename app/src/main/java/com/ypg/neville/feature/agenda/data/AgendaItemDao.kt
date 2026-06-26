package com.ypg.neville.feature.agenda.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AgendaItemDao {

    @Query("SELECT * FROM agenda_items ORDER BY activityDateMillis ASC, activityTimeMillis ASC")
    fun loadAll(): List<AgendaItemEntity>

    @Query("SELECT * FROM agenda_items WHERE id = :id LIMIT 1")
    fun findById(id: String): AgendaItemEntity?

    @Query(
        "SELECT COUNT(*) FROM agenda_items " +
            "WHERE activityDateMillis >= :startInclusive AND activityDateMillis < :endExclusive"
    )
    fun observeCountBetween(startInclusive: Long, endExclusive: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: AgendaItemEntity)

    @Update
    fun update(item: AgendaItemEntity)

    @Query("DELETE FROM agenda_items WHERE id = :id")
    fun deleteById(id: String)
}
