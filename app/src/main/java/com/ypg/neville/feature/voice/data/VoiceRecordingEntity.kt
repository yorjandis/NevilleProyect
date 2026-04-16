package com.ypg.neville.feature.voice.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "voice_recordings",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["updatedAt"])
    ]
)
data class VoiceRecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val durationMs: Long,
    val createdAt: Long,
    val updatedAt: Long
)
