package com.ypg.neville.feature.voice.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock

class AndroidVoiceRecorderEngine(
    private val context: Context
) : VoiceRecorderEngine {

    private var recorder: MediaRecorder? = null
    private var recordingStartElapsedMs: Long = 0L

    override fun start(outputFilePath: String) {
        if (recorder != null) {
            throw IllegalStateException("Ya existe una grabación en curso")
        }

        val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        newRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(96_000)
            setAudioSamplingRate(44_100)
            setMaxDuration(MAX_RECORD_MS)
            setOutputFile(outputFilePath)
            prepare()
            start()
        }

        recorder = newRecorder
        recordingStartElapsedMs = SystemClock.elapsedRealtime()
    }

    override fun stop(): Long {
        val current = recorder ?: return 0L
        val elapsedMs = (SystemClock.elapsedRealtime() - recordingStartElapsedMs).coerceAtLeast(0L)

        return try {
            current.stop()
            elapsedMs
        } finally {
            current.reset()
            current.release()
            recorder = null
            recordingStartElapsedMs = 0L
        }
    }

    override fun cancel() {
        val current = recorder ?: return
        try {
            runCatching { current.stop() }
        } finally {
            current.reset()
            current.release()
            recorder = null
            recordingStartElapsedMs = 0L
        }
    }

    override fun isRecording(): Boolean = recorder != null

    override fun release() {
        cancel()
    }

    companion object {
        const val MAX_RECORD_MS = 180_000
    }
}
