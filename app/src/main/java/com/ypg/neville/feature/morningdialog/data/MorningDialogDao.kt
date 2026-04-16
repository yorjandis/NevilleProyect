package com.ypg.neville.feature.morningdialog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MorningDialogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: MorningDialogSessionEntity)

    @Query("UPDATE morning_dialog_sessions SET noteText = :noteText WHERE id = :sessionId")
    suspend fun updateNote(sessionId: Long, noteText: String)

    @Query("DELETE FROM morning_dialog_sessions WHERE id = :sessionId")
    fun deleteById(sessionId: Long)

    @Query("SELECT * FROM morning_dialog_sessions ORDER BY completedAtEpochMillis DESC")
    fun observeAll(): Flow<List<MorningDialogSessionEntity>>

    @Query("SELECT * FROM morning_dialog_sessions ORDER BY completedAtEpochMillis DESC")
    suspend fun getAll(): List<MorningDialogSessionEntity>

    @Query("SELECT * FROM morning_dialog_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getById(sessionId: Long): MorningDialogSessionEntity?

    @Query("SELECT * FROM morning_dialog_sessions WHERE sessionDateEpochDay = :epochDay LIMIT 1")
    suspend fun getByDay(epochDay: Long): MorningDialogSessionEntity?
}
