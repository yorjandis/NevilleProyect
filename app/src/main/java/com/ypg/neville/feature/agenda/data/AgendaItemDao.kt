package com.ypg.neville.feature.agenda.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface AgendaItemDao {

    @Query("SELECT * FROM agenda_items ORDER BY activityDateMillis ASC, activityTimeMillis ASC")
    fun loadAll(): List<AgendaItemEntity>

    @Query("SELECT * FROM agenda_items WHERE id = :id LIMIT 1")
    fun findById(id: String): AgendaItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: AgendaItemEntity)

    @Update
    fun update(item: AgendaItemEntity)

    @Query("DELETE FROM agenda_items WHERE id = :id")
    fun deleteById(id: String)
}
