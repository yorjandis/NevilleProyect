package com.ypg.neville.feature.emotionalanchors.domain

import com.ypg.neville.feature.emotionalanchors.data.EmotionalAnchor
import com.ypg.neville.feature.emotionalanchors.data.EmotionalAnchorStore
import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryEventLogger
import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryEventType
import com.ypg.neville.feature.voice.media.VoicePlayerEngine
import com.ypg.neville.feature.voice.media.VoiceRecorderEngine
import java.io.File

data class RecordedAnchorAudio(
    val filePath: String,
    val durationMs: Long
)

class EmotionalAnchorsController(
    private val store: EmotionalAnchorStore,
    private val recorder: VoiceRecorderEngine,
    private val player: VoicePlayerEngine,
    private val audioDirectory: File
) {

    private var pendingOutputPath: String? = null

    fun list(): List<EmotionalAnchor> = store.list()
    fun getById(id: Long): EmotionalAnchor? = store.getById(id)

    fun startRecordingAudio(): Result<Unit> {
        return runCatching {
            ensureDirectory()
            val outputFile = File(audioDirectory, "emotional_anchor_${System.currentTimeMillis()}.m4a")
            pendingOutputPath = outputFile.absolutePath
            recorder.start(outputFile.absolutePath)
        }
    }

    fun stopRecordingAudio(): Result<RecordedAnchorAudio> {
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
                error("No se pudo guardar el audio")
            }

            RecordedAnchorAudio(filePath = file.absolutePath, durationMs = durationMs)
        }
    }

    fun cancelRecordingAudio() {
        pendingOutputPath?.let { path ->
            recorder.cancel()
            File(path).delete()
            pendingOutputPath = null
        }
    }

    fun createAnchor(
        phrase: String,
        breathingTechnique: BreathingTechnique,
        imagePath: String,
        audio: RecordedAnchorAudio?
    ): Result<EmotionalAnchor> {
        return runCatching {
            val finalPhrase = phrase.trim()
            require(finalPhrase.isNotBlank()) { "La frase no puede estar vacía" }

            store.create(
                phrase = finalPhrase,
                breathingTechniqueId = breathingTechnique.id,
                breathingTechniqueName = breathingTechnique.name,
                breathingTechniquePattern = breathingTechnique.pattern,
                breathingTechniqueGuide = breathingTechnique.guide,
                imagePath = imagePath,
                audioPath = audio?.filePath.orEmpty(),
                audioDurationMs = audio?.durationMs ?: 0L
            )
        }
    }

    fun updateAnchor(
        existing: EmotionalAnchor,
        phrase: String,
        breathingTechnique: BreathingTechnique,
        imagePath: String,
        audio: RecordedAnchorAudio?
    ): Result<EmotionalAnchor> {
        return runCatching {
            val finalPhrase = phrase.trim()
            require(finalPhrase.isNotBlank()) { "La frase no puede estar vacía" }

            val updated = store.update(
                id = existing.id,
                phrase = finalPhrase,
                breathingTechniqueId = breathingTechnique.id,
                breathingTechniqueName = breathingTechnique.name,
                breathingTechniquePattern = breathingTechnique.pattern,
                breathingTechniqueGuide = breathingTechnique.guide,
                imagePath = imagePath,
                audioPath = audio?.filePath ?: existing.audioPath,
                audioDurationMs = audio?.durationMs ?: existing.audioDurationMs
            )

            if (existing.imagePath != updated.imagePath) {
                File(existing.imagePath).delete()
            }
            if (existing.audioPath.isNotBlank() && existing.audioPath != updated.audioPath) {
                if (player.currentFilePath() == existing.audioPath) {
                    player.stop()
                }
                File(existing.audioPath).delete()
            }
            updated
        }
    }

    fun deleteAnchor(anchor: EmotionalAnchor) {
        if (player.currentFilePath() == anchor.audioPath) {
            player.stop()
        }
        store.deleteById(anchor.id)
        if (anchor.audioPath.isNotBlank()) {
            File(anchor.audioPath).delete()
        }
        File(anchor.imagePath).delete()
    }

    fun playAnchor(filePath: String, onCompletion: () -> Unit) {
        WeeklySummaryEventLogger.log(WeeklySummaryEventType.ANCHORS_USED, targetKey = filePath)
        player.play(filePath, onCompletion)
    }

    fun stopPlayback() {
        player.stop()
    }

    fun isPlaying(filePath: String): Boolean {
        return player.isPlaying() && player.currentFilePath() == filePath
    }

    fun release() {
        cancelRecordingAudio()
        recorder.release()
        player.release()
    }

    private fun ensureDirectory() {
        if (!audioDirectory.exists()) {
            audioDirectory.mkdirs()
        }
    }
}
