package com.ypg.neville.feature.ai.domain

enum class AiAuthor(
    val id: String,
    val displayName: String,
    val imageName: String
) {
    NEVILLE("neville", "Neville Goddard", "nev_min"),
    JOE_DISPENZA("joe_dispenza", "Joe Dispenza", "jd"),
    BRUCE_LIPTON("bruce_lipton", "Dr. Bruce Lipton", "bruce"),
    GREGG_BRADEN("gregg_braden", "Gregg Braden", "gregg");

    companion object {
        fun fromId(id: String): AiAuthor = entries.firstOrNull { it.id == id } ?: NEVILLE
    }
}

enum class AiMessageRole(val apiValue: String) {
    USER("user"),
    ASSISTANT("assistant")
}

enum class AiMessageStatus {
    STREAMING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class AiMessage(
    val id: String,
    val conversationId: String,
    val text: String,
    val role: AiMessageRole,
    val createdAt: Long,
    val sequence: Long,
    val status: AiMessageStatus,
    val errorDescription: String? = null,
    val modelIdentifier: String
) {
    val isUser: Boolean get() = role == AiMessageRole.USER
}

data class AiConversation(
    val id: String,
    val title: String,
    val author: AiAuthor,
    val createdAt: Long,
    val updatedAt: Long,
    val summary: String?,
    val usesPersonalVoice: Boolean,
    val modelIdentifier: String
)

data class OpenRouterModel(
    val id: String,
    val name: String,
    val contextLength: Int?,
    val description: String?
) {
    val isAutomaticFreeSelection: Boolean
        get() = id == OpenRouterConfiguration.AUTOMATIC_FREE_MODEL

    val displayName: String
        get() = if (isAutomaticFreeSelection) {
            "Selección automática gratuita"
        } else {
            name
        }
}

data class OpenRouterModelCatalog(
    val models: List<OpenRouterModel>,
    val dailyFreeRequestLimit: Int
)

data class OpenRouterContextMessage(
    val role: AiMessageRole,
    val text: String
)

data class OpenRouterChatRequest(
    val instructions: String,
    val summary: String?,
    val messages: List<OpenRouterContextMessage>,
    val prompt: String,
    val modelIdentifier: String
)

enum class AiTextAction(val id: String, val requiresAuthor: Boolean) {
    KEY_POINTS("key_points", false),
    SUMMARY("summary", false),
    PRACTICES("practices", false),
    CONCRETE_PRACTICE("concrete_practice", true),
    INTERPRET("interpret", true);

    companion object {
        fun fromId(id: String): AiTextAction =
            entries.firstOrNull { it.id == id } ?: INTERPRET
    }
}
