package com.ypg.neville.feature.ai.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_messages",
    foreignKeys = [
        ForeignKey(
            entity = AiConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["conversationId", "sequence"])
    ]
)
data class AiMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val text: String,
    val role: String,
    val createdAt: Long,
    val sequence: Long,
    val status: String,
    val errorDescription: String?,
    val modelIdentifier: String
)
