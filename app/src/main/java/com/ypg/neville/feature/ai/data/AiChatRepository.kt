package com.ypg.neville.feature.ai.data

import android.content.Context
import com.ypg.neville.R
import com.ypg.neville.feature.ai.domain.AiAuthor
import com.ypg.neville.feature.ai.domain.AiConversation
import com.ypg.neville.feature.ai.domain.AiInstructions
import com.ypg.neville.feature.ai.domain.AiMessage
import com.ypg.neville.feature.ai.domain.AiMessageRole
import com.ypg.neville.feature.ai.domain.AiMessageStatus
import com.ypg.neville.model.security.PostQuantumAesTextCrypto
import java.util.UUID

class AiChatRepository(
    private val context: Context,
    private val dao: AiChatDao
) {
    init {
        PostQuantumAesTextCrypto.configure(context.applicationContext)
    }

    suspend fun conversations(): List<AiConversation> =
        dao.conversations().mapNotNull { runCatching { it.toDomain() }.getOrNull() }

    suspend fun conversation(id: String): AiConversation? =
        dao.conversation(id)?.let { runCatching { it.toDomain() }.getOrNull() }

    suspend fun messages(conversationId: String): List<AiMessage> =
        dao.messages(conversationId).mapNotNull { entity ->
            runCatching {
                val normalized = if (entity.status == AiMessageStatus.STREAMING.name) {
                    entity.copy(
                        status = AiMessageStatus.CANCELLED.name,
                        text = if (decryptMessageText(entity).isBlank()) {
                            encryptMessageText(
                                entity.id,
                                context.getString(R.string.ai_response_stopped)
                            )
                        } else {
                            entity.text
                        }
                    ).also { dao.updateMessage(it) }
                } else {
                    entity
                }
                normalized.toDomain()
            }.getOrNull()
        }

    suspend fun createConversation(
        author: AiAuthor,
        usesPersonalVoice: Boolean,
        modelIdentifier: String
    ): AiConversation {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val entity = AiConversationEntity(
            id = id,
            title = encryptConversationText(
                id,
                TITLE_FIELD,
                context.getString(R.string.ai_new_conversation)
            ),
            authorId = author.id,
            createdAt = now,
            updatedAt = now,
            summary = null,
            promptVersion = AiInstructions.PROMPT_VERSION,
            usesPersonalVoice = usesPersonalVoice,
            modelIdentifier = modelIdentifier
        )
        dao.insertConversation(entity)
        return entity.toDomain()
    }

    suspend fun appendMessage(message: AiMessage) {
        dao.insertMessage(message.toEntity())
        touchConversation(message.conversationId)
    }

    suspend fun updateMessage(message: AiMessage) {
        dao.updateMessage(message.toEntity())
        touchConversation(message.conversationId)
    }

    suspend fun renameConversation(id: String, title: String) {
        dao.renameConversation(
            id,
            encryptConversationText(id, TITLE_FIELD, title),
            System.currentTimeMillis()
        )
    }

    suspend fun updateSummary(id: String, summary: String?) {
        dao.updateSummary(
            id,
            summary?.let { encryptConversationText(id, SUMMARY_FIELD, it) },
            System.currentTimeMillis()
        )
    }

    suspend fun deleteConversation(id: String) {
        dao.deleteConversation(id)
    }

    private suspend fun touchConversation(id: String) {
        val current = dao.conversation(id) ?: return
        dao.updateConversation(current.copy(updatedAt = System.currentTimeMillis()))
    }

    private fun AiConversationEntity.toDomain(): AiConversation = AiConversation(
        id = id,
        title = decryptConversationText(id, TITLE_FIELD, title),
        author = AiAuthor.fromId(authorId),
        createdAt = createdAt,
        updatedAt = updatedAt,
        summary = summary?.let { decryptConversationText(id, SUMMARY_FIELD, it) },
        usesPersonalVoice = usesPersonalVoice,
        modelIdentifier = modelIdentifier
    )

    private fun AiMessageEntity.toDomain(): AiMessage = AiMessage(
        id = id,
        conversationId = conversationId,
        text = decryptMessageText(this),
        role = runCatching { AiMessageRole.valueOf(role) }.getOrDefault(AiMessageRole.USER),
        createdAt = createdAt,
        sequence = sequence,
        status = runCatching { AiMessageStatus.valueOf(status) }
            .getOrDefault(AiMessageStatus.COMPLETED),
        errorDescription = errorDescription?.let {
            decryptMessageField(id, ERROR_FIELD, it)
        },
        modelIdentifier = modelIdentifier
    )

    private fun AiMessage.toEntity(): AiMessageEntity = AiMessageEntity(
        id = id,
        conversationId = conversationId,
        text = encryptMessageText(id, text),
        role = role.name,
        createdAt = createdAt,
        sequence = sequence,
        status = status.name,
        errorDescription = errorDescription?.let {
            encryptMessageField(id, ERROR_FIELD, it)
        },
        modelIdentifier = modelIdentifier
    )

    private fun decryptMessageText(entity: AiMessageEntity): String =
        decryptMessageField(entity.id, TEXT_FIELD, entity.text)

    private fun encryptMessageText(id: String, value: String): String =
        encryptMessageField(id, TEXT_FIELD, value)

    private fun encryptConversationText(id: String, field: String, value: String): String =
        PostQuantumAesTextCrypto.encrypt(value, "ai-conversation:$id:$field")

    private fun decryptConversationText(id: String, field: String, value: String): String =
        PostQuantumAesTextCrypto.decrypt(value, "ai-conversation:$id:$field")

    private fun encryptMessageField(id: String, field: String, value: String): String =
        PostQuantumAesTextCrypto.encrypt(value, "ai-message:$id:$field")

    private fun decryptMessageField(id: String, field: String, value: String): String =
        PostQuantumAesTextCrypto.decrypt(value, "ai-message:$id:$field")

    companion object {
        private const val TITLE_FIELD = "title"
        private const val SUMMARY_FIELD = "summary"
        private const val TEXT_FIELD = "text"
        private const val ERROR_FIELD = "error"
    }
}
