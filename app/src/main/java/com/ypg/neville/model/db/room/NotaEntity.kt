package com.ypg.neville.model.db.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notas")
data class NotaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "titulo")
    val titulo: String,

    @ColumnInfo(name = "nota")
    val nota: String,

    @ColumnInfo(name = "fechaCreacion")
    val fechaCreacion: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "fechaModificacion")
    val fechaModificacion: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "isFav")
    val isFav: Boolean = false,

    @ColumnInfo(name = "categoria")
    val categoria: String = "",

    @ColumnInfo(name = "isChecklist")
    val isChecklist: Boolean = false,

    @ColumnInfo(name = "checklistJson")
    val checklistJson: String = "",

    @ColumnInfo(name = "syncId")
    val syncId: String = "",

    /**
     * Kept only so databases created by the short-lived v34 implementation can
     * be opened and their audio migrated. New voice notes never write a path.
     */
    @ColumnInfo(name = "audioPath", defaultValue = "''")
    val legacyAudioPath: String = "",

    @ColumnInfo(name = "voiceAudioData")
    val voiceAudioData: ByteArray? = null,

    @ColumnInfo(name = "voiceAudioFileExtension", defaultValue = "''")
    val voiceAudioFileExtension: String = ""
) {
    val isVoiceNote: Boolean
        get() = nota.startsWith(VOICE_NOTE_MARKER)

    val hasVoiceAudio: Boolean
        get() = voiceAudioData?.isNotEmpty() == true

    val voiceNoteAssociatedText: String
        get() = if (nota.startsWith(VOICE_NOTE_MARKER)) nota.removePrefix(VOICE_NOTE_MARKER) else nota

    val noteDisplayText: String
        get() = if (isVoiceNote) voiceNoteAssociatedText else nota

    companion object {
        const val VOICE_NOTE_MARKER = "\u200B[[VOICE-NOTE]]"

        fun voiceBody(associatedText: String): String = VOICE_NOTE_MARKER + associatedText
    }
}
