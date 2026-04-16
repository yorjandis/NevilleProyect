package com.ypg.neville.feature.voice.media

interface VoicePlayerEngine {
    fun play(filePath: String, onCompletion: () -> Unit)
    fun stop()
    fun isPlaying(): Boolean
    fun currentFilePath(): String?
    fun release()
}
