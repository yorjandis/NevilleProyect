package com.ypg.neville.model.db.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PreferenceDao {

    @Query("SELECT * FROM preferences WHERE prefKey = :key LIMIT 1")
    fun getByKey(key: String): PreferenceEntity?

    @Query("SELECT * FROM preferences WHERE prefKey LIKE :prefix")
    fun getByPrefix(prefix: String): List<PreferenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: PreferenceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(entities: List<PreferenceEntity>)

    @Query("DELETE FROM preferences WHERE prefKey = :key")
    fun deleteByKey(key: String)

    @Query("DELETE FROM preferences WHERE prefKey LIKE :prefix")
    fun deleteByPrefix(prefix: String)
}
