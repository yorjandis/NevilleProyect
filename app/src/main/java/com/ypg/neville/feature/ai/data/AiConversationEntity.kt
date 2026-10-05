package com.ypg.neville.feature.ai.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_conversations",
    indices = [Index(value = ["updatedAt"])]
)
data class AiConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val authorId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val summary: String?,
    val promptVersion: Int,
    val usesPersonalVoice: Boolean,
    val modelIdentifier: String
)
