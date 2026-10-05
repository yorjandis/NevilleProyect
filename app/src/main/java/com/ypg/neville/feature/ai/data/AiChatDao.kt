package com.ypg.neville.feature.ai.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface AiChatDao {
    @Query("SELECT * FROM ai_conversations ORDER BY updatedAt DESC")
    suspend fun conversations(): List<AiConversationEntity>

    @Query("SELECT * FROM ai_conversations WHERE id = :id LIMIT 1")
    suspend fun conversation(id: String): AiConversationEntity?

    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY sequence ASC, createdAt ASC")
    suspend fun messages(conversationId: String): List<AiMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: AiConversationEntity)

    @Update
    suspend fun updateConversation(conversation: AiConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AiMessageEntity)

    @Update
    suspend fun updateMessage(message: AiMessageEntity)

    @Query("DELETE FROM ai_conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)

    @Query("UPDATE ai_conversations SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun renameConversation(id: String, title: String, updatedAt: Long)

    @Query("UPDATE ai_conversations SET summary = :summary, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSummary(id: String, summary: String?, updatedAt: Long)
}
