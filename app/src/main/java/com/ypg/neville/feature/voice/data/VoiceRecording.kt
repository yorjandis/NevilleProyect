package com.ypg.neville.feature.voice.data

data class VoiceRecording(
    val id: Long,
    val title: String,
    val filePath: String,
    val durationMs: Long,
    val createdAt: Long,
    val updatedAt: Long
)
