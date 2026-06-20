package com.ypg.neville.feature.calmspace.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CalmPersonalPhraseDao {

    @Query("SELECT * FROM calm_personal_phrases ORDER BY updatedAt DESC, id DESC")
    fun getAll(): List<CalmPersonalPhraseEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(entity: CalmPersonalPhraseEntity): Long

    @Query(
        "UPDATE calm_personal_phrases " +
            "SET phrase = :phrase, updatedAt = :updatedAt " +
            "WHERE id = :id"
    )
    fun updatePhraseById(id: Long, phrase: String, updatedAt: Long): Int

    @Query("DELETE FROM calm_personal_phrases WHERE id = :id")
    fun deleteById(id: Long): Int
}

