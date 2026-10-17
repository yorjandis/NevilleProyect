package com.ypg.neville.model.db.room

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotaEntityVoiceNoteTest {

    @Test
    fun voiceNote_usesIosMarkerAndHidesItFromAssociatedText() {
        val note = NotaEntity(
            titulo = "Idea",
            nota = NotaEntity.voiceBody("Texto asociado"),
            voiceAudioData = byteArrayOf(1, 2, 3),
            voiceAudioFileExtension = "m4a"
        )

        assertTrue(note.isVoiceNote)
        assertTrue(note.hasVoiceAudio)
        assertEquals("Texto asociado", note.voiceNoteAssociatedText)
        assertEquals("Texto asociado", note.noteDisplayText)
    }

    @Test
    fun audioWithoutMarker_doesNotChangeRegularNoteType() {
        val note = NotaEntity(
            titulo = "Nota",
            nota = "Texto",
            voiceAudioData = byteArrayOf(1)
        )

        assertFalse(note.isVoiceNote)
        assertEquals("Texto", note.noteDisplayText)
    }
}
