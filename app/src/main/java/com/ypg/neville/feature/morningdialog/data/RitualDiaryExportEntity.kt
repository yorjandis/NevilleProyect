package com.ypg.neville.feature.morningdialog.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ritual_diary_exports",
    indices = [Index(value = ["diarioId"])]
)
data class RitualDiaryExportEntity(
    @PrimaryKey
    val sessionId: Long,
    val diarioId: Long,
    val createdAt: Long
)
