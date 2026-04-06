package com.ypg.neville.model.db.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ArchivedGoalDao {
    @Query("SELECT * FROM archived_goals ORDER BY completionDate DESC")
    fun getAll(): List<ArchivedGoalEntity>

    @Query("SELECT * FROM archived_goals WHERE id = :id LIMIT 1")
    fun getById(id: String): ArchivedGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(goal: ArchivedGoalEntity)

    @Update
    fun update(goal: ArchivedGoalEntity)

    @Delete
    fun delete(goal: ArchivedGoalEntity)

    @Query("DELETE FROM archived_goals WHERE id = :id")
    fun deleteById(id: String)
}
