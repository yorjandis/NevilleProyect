package com.ypg.neville.feature.ai

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.ypg.neville.feature.ai.domain.AiAuthor
import com.ypg.neville.feature.ai.domain.AiInstructions
import com.ypg.neville.feature.ai.domain.AiTextAction
import com.ypg.neville.feature.ai.domain.AiTextProcessor
import com.ypg.neville.feature.ai.domain.OpenRouterChatRequest
import com.ypg.neville.feature.ai.domain.OpenRouterConfiguration
import com.ypg.neville.feature.ai.network.OpenRouterClient
import com.ypg.neville.feature.ai.security.OpenRouterCredentialStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class OpenRouterLiveIntegrationTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val credentialStore = OpenRouterCredentialStore(context)
    private val client = OpenRouterClient()

    @Test
    fun savedCredentialLoadsFreeModelCatalog() = runBlocking {
        val catalog = client.availableModels(requireSavedKey())

        assertTrue(catalog.models.isNotEmpty())
        assertTrue(catalog.models.all {
            OpenRouterConfiguration.isFreeModelIdentifier(it.id)
        })
        assertTrue(catalog.dailyFreeRequestLimit > 0)
    }

    @Test
    fun textToolPublishesProgressiveResponse() = runBlocking {
        val updates = mutableListOf<String>()
        val result = AiTextProcessor(client).process(
            action = AiTextAction.INTERPRET,
            author = AiAuthor.NEVILLE,
            text = "La imaginación crea la realidad. Explica esta idea en cinco frases breves.",
            modelIdentifier = OpenRouterConfiguration.selectedModelIdentifier(context),
            apiKey = requireSavedKey(),
            onUpdate = updates::add
        )

        assertTrue(result.isNotBlank())
        assertTrue(updates.isNotEmpty())
        assertTrue(updates.zipWithNext().all { (before, after) ->
            after.startsWith(before) && after.length >= before.length
        })
        assertEquals(result, updates.last().trim())
    }

    @Test
    fun chatPublishesStreamingDeltas() = runBlocking {
        val deltas = mutableListOf<String>()
        val request = OpenRouterChatRequest(
            instructions = AiInstructions.chat(
                author = AiAuthor.NEVILLE,
                usesPersonalVoice = false
            ),
            summary = null,
            messages = emptyList(),
            prompt = "Explica la revisión en cuatro puntos muy breves.",
            modelIdentifier = OpenRouterConfiguration.selectedModelIdentifier(context)
        )

        client.streamChat(request, requireSavedKey()) { delta ->
            deltas += delta
        }

        assertFalse(deltas.isEmpty())
        assertTrue(deltas.joinToString("").isNotBlank())
    }

    private fun requireSavedKey(): String =
        credentialStore.readApiKey()
            ?.takeIf { it.isNotBlank() }
            ?: error("No hay una clave de OpenRouter guardada en este dispositivo.")
}
