package com.ypg.neville.feature.ai.domain

interface AiTextGenerationEngine {
    suspend fun generateText(
        instructions: String,
        input: String,
        modelIdentifier: String,
        apiKey: String,
        maximumCompletionTokens: Int = 2_000
    ): String

    suspend fun streamText(
        instructions: String,
        input: String,
        modelIdentifier: String,
        apiKey: String,
        maximumCompletionTokens: Int = 2_000,
        onUpdate: (String) -> Unit
    ): String
}
