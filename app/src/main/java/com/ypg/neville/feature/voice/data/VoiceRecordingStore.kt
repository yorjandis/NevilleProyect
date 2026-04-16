package com.ypg.neville.feature.voice.data

interface VoiceRecordingStore {
    fun list(): List<VoiceRecording>
    fun create(title: String, filePath: String, durationMs: Long): VoiceRecording
    fun updateTitle(id: Long, title: String): VoiceRecording?
    fun deleteById(id: Long)
}
