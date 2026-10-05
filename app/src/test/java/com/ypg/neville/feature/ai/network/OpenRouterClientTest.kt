package com.ypg.neville.feature.ai.network

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenRouterClientTest {
    @Test
    fun extractsPlainAndStructuredContent() {
        assertEquals("respuesta", OpenRouterClient.extractContent("respuesta"))

        val structured = JSONArray()
            .put(JSONObject().put("type", "text").put("text", "primera "))
            .put(JSONObject().put("type", "text").put("text", "segunda"))

        assertEquals("primera segunda", OpenRouterClient.extractContent(structured))
    }
}
