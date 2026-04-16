package com.ypg.neville.feature.morningdialog.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RitualDiaryExportDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(item: RitualDiaryExportEntity): Long

    @Query("SELECT COUNT(*) > 0 FROM ritual_diary_exports WHERE sessionId = :sessionId")
    fun existsBySessionId(sessionId: Long): Boolean

    @Query("SELECT * FROM ritual_diary_exports WHERE sessionId = :sessionId LIMIT 1")
    fun getBySessionId(sessionId: Long): RitualDiaryExportEntity?

    @Query("DELETE FROM ritual_diary_exports WHERE sessionId = :sessionId")
    fun deleteBySessionId(sessionId: Long)
}
