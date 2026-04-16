package com.ypg.neville.feature.voice.domain

import com.ypg.neville.feature.voice.data.VoiceRecording
import com.ypg.neville.feature.voice.data.VoiceRecordingStore
import com.ypg.neville.feature.voice.media.VoicePlayerEngine
import com.ypg.neville.feature.voice.media.VoiceRecorderEngine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VoiceNotesController(
    private val store: VoiceRecordingStore,
    private val recorder: VoiceRecorderEngine,
    private val player: VoicePlayerEngine,
    private val audioDirectory: File
) {

    private var pendingOutputPath: String? = null

    fun list(): List<VoiceRecording> = store.list()

    fun startRecording(): Result<Unit> {
        return runCatching {
            ensureDirectory()
            val outputFile = File(audioDirectory, "voice_${System.currentTimeMillis()}.m4a")
            pendingOutputPath = outputFile.absolutePath
            recorder.start(outputFile.absolutePath)
        }
    }

    fun stopAndSave(title: String?): Result<VoiceRecording> {
        return runCatching {
            val pendingPath = requireNotNull(pendingOutputPath) { "No hay grabación pendiente" }
            val file = File(pendingPath)
            val durationMs = try {
                recorder.stop()
            } catch (error: Throwable) {
                pendingOutputPath = null
                file.delete()
                throw error
            }
            pendingOutputPath = null

            if (!file.exists() || durationMs <= 0L) {
                file.delete()
                error("La grabación no pudo guardarse")
            }

            val finalTitle = title
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: defaultTitle()

            store.create(
                title = finalTitle,
                filePath = file.absolutePath,
                durationMs = durationMs
            )
        }
    }

    fun cancelRecording() {
        pendingOutputPath?.let { path ->
            recorder.cancel()
            File(path).delete()
            pendingOutputPath = null
        }
    }

    fun rename(id: Long, newTitle: String): VoiceRecording? {
        val title = newTitle.trim()
        if (title.isBlank()) return null
        return store.updateTitle(id, title)
    }

    fun delete(item: VoiceRecording) {
        if (player.currentFilePath() == item.filePath) {
            player.stop()
        }
        store.deleteById(item.id)
        File(item.filePath).delete()
    }

    fun play(filePath: String, onCompletion: () -> Unit) {
        player.play(filePath, onCompletion)
    }

    fun stopPlayback() {
        player.stop()
    }

    fun isPlaying(filePath: String): Boolean {
        return player.isPlaying() && player.currentFilePath() == filePath
    }

    fun release() {
        cancelRecording()
        recorder.release()
        player.release()
    }

    private fun ensureDirectory() {
        if (!audioDirectory.exists()) {
            audioDirectory.mkdirs()
        }
    }

    private fun defaultTitle(): String {
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return "Nota de Voz ${format.format(Date())}"
    }

    companion object {
        const val MAX_RECORD_MS = 180_000L
    }
}
