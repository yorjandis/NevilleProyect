package com.ypg.neville.feature.voice.media

interface VoiceRecorderEngine {
    fun start(outputFilePath: String)
    fun stop(): Long
    fun cancel()
    fun isRecording(): Boolean
    fun release()
}
