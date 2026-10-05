package com.ypg.neville.feature.ai.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.runBlocking

class AiTextProcessorTest {
    @Test
    fun splitTextPreservesAllContentAndMaximumSize() {
        val source = (1..150).joinToString("\n\n") {
            "Párrafo $it. Este contenido debe conservarse sin perder caracteres relevantes."
        }

        val chunks = AiTextProcessor.splitText(source, 500)

        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.length <= 500 })
        assertEquals(
            source.replace("\\s+".toRegex(), " ").trim(),
            chunks.joinToString(" ").replace("\\s+".toRegex(), " ").trim()
        )
    }

    @Test
    fun splitTextLeavesShortInputUntouched() {
        assertEquals(listOf("Texto breve."), AiTextProcessor.splitText("Texto breve.", 500))
    }

    @Test
    fun shortTextPublishesProgressiveUpdates() = runBlocking {
        val engine = RecordingTextEngine()
        val updates = mutableListOf<String>()

        val result = AiTextProcessor(engine).process(
            action = AiTextAction.INTERPRET,
            author = AiAuthor.NEVILLE,
            text = "Texto breve.",
            modelIdentifier = OpenRouterConfiguration.AUTOMATIC_FREE_MODEL,
            apiKey = "test-key",
            onUpdate = updates::add
        )

        assertEquals(listOf("Respuesta", "Respuesta progresiva"), updates)
        assertEquals("Respuesta progresiva final", result)
        assertEquals(1, engine.streamingCalls)
        assertEquals(0, engine.blockingCalls)
    }

    @Test
    fun longTextStreamsTheFinalSynthesis() = runBlocking {
        val engine = RecordingTextEngine()
        val updates = mutableListOf<String>()

        val result = AiTextProcessor(engine).process(
            action = AiTextAction.SUMMARY,
            author = AiAuthor.NEVILLE,
            text = "Contenido largo. ".repeat(1_100),
            modelIdentifier = OpenRouterConfiguration.AUTOMATIC_FREE_MODEL,
            apiKey = "test-key",
            onUpdate = updates::add
        )

        assertTrue(engine.blockingCalls > 0)
        assertEquals(1, engine.streamingCalls)
        assertEquals(listOf("Respuesta", "Respuesta progresiva"), updates)
        assertEquals("Respuesta progresiva final", result)
    }

    private class RecordingTextEngine : AiTextGenerationEngine {
        var blockingCalls = 0
        var streamingCalls = 0

        override suspend fun generateText(
            instructions: String,
            input: String,
            modelIdentifier: String,
            apiKey: String,
            maximumCompletionTokens: Int
        ): String {
            blockingCalls++
            return "Resultado intermedio compacto."
        }

        override suspend fun streamText(
            instructions: String,
            input: String,
            modelIdentifier: String,
            apiKey: String,
            maximumCompletionTokens: Int,
            onUpdate: (String) -> Unit
        ): String {
            streamingCalls++
            onUpdate("Respuesta")
            onUpdate("Respuesta progresiva")
            return "Respuesta progresiva final"
        }
    }
}
