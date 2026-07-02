package com.ypg.neville.model.db.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals")
    fun getAll(): List<GoalEntity>

    @Query("SELECT COUNT(*) FROM goals WHERE isStarted = 1")
    fun observeStartedCount(): Flow<Int>

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    fun getById(id: String): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(goal: GoalEntity)

    @Update
    fun update(goal: GoalEntity)

    @Delete
    fun delete(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    fun deleteById(id: String)
}
