package com.ypg.neville.feature.voice.media

import android.content.Context
import com.ypg.neville.model.db.room.NotaEntity
import java.io.File
import java.util.UUID

/**
 * The database blob is the source of truth. Files are only recording targets
 * or materialized playback copies, so notes remain portable between devices.
 */
object VoiceNoteAudioStore {
    const val DEFAULT_EXTENSION = "m4a"

    fun newRecordingFile(context: Context): File {
        val directory = File(context.cacheDir, "voice_note_recordings")
        check(directory.exists() || directory.mkdirs()) { "No se pudo crear el directorio de grabación" }
        return File(directory, "${UUID.randomUUID()}.$DEFAULT_EXTENSION")
    }

    fun readRecording(file: File): ByteArray {
        require(file.isFile && file.length() > 0L) { "La grabación está vacía" }
        return file.readBytes()
    }

    fun playbackFile(context: Context, note: NotaEntity): File {
        val audio = requireNotNull(note.voiceAudioData).takeIf { it.isNotEmpty() }
            ?: error("La nota de voz no contiene audio")
        val extension = note.voiceAudioFileExtension
            .lowercase()
            .filter(Char::isLetterOrDigit)
            .ifBlank { DEFAULT_EXTENSION }
        val key = note.syncId.ifBlank { note.id.toString() }
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
        val directory = File(context.filesDir, "voice_notes")
        check(directory.exists() || directory.mkdirs()) { "No se pudo preparar el audio" }
        val destination = File(directory, "note_$key.$extension")
        val isCurrent = runCatching {
            destination.isFile && destination.readBytes().contentEquals(audio)
        }.getOrDefault(false)
        if (!isCurrent) {
            destination.writeBytes(audio)
        }
        return destination
    }

    fun previewFile(context: Context, audio: ByteArray, extension: String): File {
        require(audio.isNotEmpty()) { "La nota de voz no contiene audio" }
        val safeExtension = extension.lowercase().filter(Char::isLetterOrDigit).ifBlank { DEFAULT_EXTENSION }
        val destination = File(context.cacheDir, "voice_note_preview.$safeExtension")
        destination.writeBytes(audio)
        return destination
    }

    fun deleteMaterializedAudio(context: Context, note: NotaEntity) {
        val key = note.syncId.ifBlank { note.id.toString() }
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
        File(context.filesDir, "voice_notes").listFiles()
            ?.filter { it.name.startsWith("note_$key.") }
            ?.forEach(File::delete)
    }
}
