package com.ypg.neville.model.db.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class CompletedGoalUnitRow(
    val goalTitle: String,
    val unitName: String
)

@Dao
interface GoalUnitDao {
    @Query("SELECT * FROM goal_units WHERE goalId = :goalId ORDER BY unitIndex ASC")
    fun getByGoalId(goalId: String): List<GoalUnitEntity>

    @Query("SELECT * FROM goal_units WHERE id = :id LIMIT 1")
    fun getById(id: String): GoalUnitEntity?

    @Query(
        """
        SELECT goals.title AS goalTitle, goal_units.name AS unitName
        FROM goal_units
        INNER JOIN goals ON goals.id = goal_units.goalId
        WHERE goal_units.completedDate >= :startInclusive
            AND goal_units.completedDate < :endExclusive
        ORDER BY goal_units.completedDate ASC
        """
    )
    suspend fun getCompletedBetween(startInclusive: Long, endExclusive: Long): List<CompletedGoalUnitRow>

    @Query(
        """
        SELECT COUNT(*)
        FROM goal_units
        INNER JOIN goals ON goals.id = goal_units.goalId
        WHERE goals.isStarted = 1
            AND goal_units.status = 'pending'
            AND goal_units.startDate IS NOT NULL
            AND goal_units.startDate <= :nowMillis
            AND (goal_units.endDate IS NULL OR goal_units.endDate >= :nowMillis)
        """
    )
    fun observeReadyToCheckCount(nowMillis: Long): Flow<Int>

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
