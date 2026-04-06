package com.ypg.neville.model.db.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ArchivedUnitDao {
    @Query("SELECT * FROM archived_units WHERE goalId = :goalId ORDER BY unitIndex ASC")
    fun getByGoalId(goalId: String): List<ArchivedUnitEntity>

    @Query("SELECT * FROM archived_units WHERE id = :id LIMIT 1")
    fun getById(id: String): ArchivedUnitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(units: List<ArchivedUnitEntity>)

    @Update
    fun update(unit: ArchivedUnitEntity)

    @Query("DELETE FROM archived_units WHERE goalId = :goalId")
    fun deleteByGoalId(goalId: String)
}
