package com.ypg.neville.feature.emotionalanchors.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface EmotionalAnchorDao {

    @Query("SELECT * FROM emotional_anchors ORDER BY updatedAt DESC")
    fun loadAll(): List<EmotionalAnchorEntity>

    @Query("SELECT * FROM emotional_anchors WHERE id = :id LIMIT 1")
    fun findById(id: Long): EmotionalAnchorEntity?

    @Insert
    fun insert(item: EmotionalAnchorEntity): Long

    @Update
    fun update(item: EmotionalAnchorEntity)

    @Query("DELETE FROM emotional_anchors WHERE id = :id")
    fun deleteById(id: Long)
}
