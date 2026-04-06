package com.ypg.neville.model.db.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface GoalUnitDao {
    @Query("SELECT * FROM goal_units WHERE goalId = :goalId ORDER BY unitIndex ASC")
    fun getByGoalId(goalId: String): List<GoalUnitEntity>

    @Query("SELECT * FROM goal_units WHERE id = :id LIMIT 1")
    fun getById(id: String): GoalUnitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(units: List<GoalUnitEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(unit: GoalUnitEntity)

    @Update
    fun update(unit: GoalUnitEntity)

    @Update
    fun updateAll(units: List<GoalUnitEntity>)

    @Query("DELETE FROM goal_units WHERE goalId = :goalId")
    fun deleteByGoalId(goalId: String)
}
