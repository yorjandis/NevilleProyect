package com.ypg.neville.model.db.room

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotaChecklistCodecTest {
    @Test
    fun `old checklist items decode as tasks`() {
        val items = NotaChecklistCodec.decode(
            """[{"id":"1","text":"Anterior","checked":true}]"""
        )

        assertEquals(1, items.size)
        assertTrue(items.single().checked)
        assertEquals(NotaChecklistItemKind.TASK, items.single().kind)
    }

    @Test
    fun `note blocks and iOS checked key round trip`() {
        val source = listOf(
            NotaChecklistItem("a", "Introducción", false, NotaChecklistItemKind.LEADING_NOTE),
            NotaChecklistItem("b", "Tarea =2+2", true),
            NotaChecklistItem("c", "Cierre", false, NotaChecklistItemKind.TRAILING_NOTE)
        )

        val json = NotaChecklistCodec.encode(source)
        val decoded = NotaChecklistCodec.decode(json)

        assertEquals(source, decoded)
        assertTrue(json.contains("\"isChecked\":true"))
        assertTrue(decoded[1].isTask)
        assertFalse(decoded[0].isTask)
    }
}
