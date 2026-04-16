package com.ypg.neville.model.reminders

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY sortOrder ASC")
    fun loadAll(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    fun findById(id: String): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(reminder: ReminderEntity)

    @Update
    fun update(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    fun deleteById(id: String)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM reminders")
    fun nextSortOrder(): Int
}
