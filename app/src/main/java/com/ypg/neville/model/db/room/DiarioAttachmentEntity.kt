package com.ypg.neville.model.db.room

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "diario_attachments",
    foreignKeys = [
        ForeignKey(
            entity = DiarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["diarioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("diarioId")]
)
data class DiarioAttachmentEntity(
    @PrimaryKey
    val id: String,
    val diarioId: Long,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val createdAt: Long,
    val cryptoVersion: Int = 1,
    val encryptedData: ByteArray
)

data class DiarioAttachmentCount(
    val diarioId: Long,
    val count: Int
)
