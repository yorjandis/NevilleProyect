package com.ypg.neville.feature.voice.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface VoiceRecordingDao {

    @Query("SELECT * FROM voice_recordings ORDER BY createdAt DESC")
    fun loadAll(): List<VoiceRecordingEntity>

    @Query("SELECT * FROM voice_recordings WHERE id = :id LIMIT 1")
    fun findById(id: Long): VoiceRecordingEntity?

    @Insert
    fun insert(item: VoiceRecordingEntity): Long

    @Update
    fun update(item: VoiceRecordingEntity)

    @Query("DELETE FROM voice_recordings WHERE id = :id")
    fun deleteById(id: Long)
}
