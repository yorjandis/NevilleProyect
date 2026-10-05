package com.ypg.neville.feature.transformation.data

import com.ypg.neville.feature.transformation.domain.TransformationConfiguration
import com.ypg.neville.feature.transformation.domain.TransformationDayEntry
import com.ypg.neville.feature.transformation.domain.TransformationJournal
import com.ypg.neville.feature.transformation.domain.TransformationParaEvent
import com.ypg.neville.feature.transformation.domain.TransformationScore
import com.ypg.neville.feature.transformation.domain.TransformationState
import org.junit.Assert.assertEquals
import org.junit.Test

class TransformationStateCodecTest {
    @Test
    fun roundTrip_preservesCompleteProtocolState() {
        val state = TransformationState(
            configuration = TransformationConfiguration(
                "Defensa", "crítica", "me atacan", "tensión", "interrumpo", "discusión",
                "respiro y pregunto", "incomodidad", "escucha", 1234L, true, 480, 840, 1260
            ),
            entries = listOf(
                TransformationDayEntry(
                    day = 1, commitment = "Escuchar", exerciseNotes = "Detecté tensión",
                    morningCompleted = true, actionCompleted = true, eveningCompleted = true,
                    journal = TransformationJournal("Reunión", "Ataque", "Mandíbula", "Responder", "Pregunté", "Pausar", 8, 15),
                    score = TransformationScore(2, 2, 1, 2, 2), evidences = listOf("Esperé")
                )
            ),
            paraEvents = listOf(TransformationParaEvent("id", 99L, "tensión", "miedo", "preguntar", 30)),
            completedAtMillis = 999L
        )

        assertEquals(state, TransformationStateCodec.decode(TransformationStateCodec.encode(state)))
    }
}
