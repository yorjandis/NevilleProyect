package com.ypg.neville.model.db.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DiarioAttachmentDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(attachment: DiarioAttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(attachment: DiarioAttachmentEntity)

    @Query("SELECT * FROM diario_attachments WHERE diarioId = :diarioId ORDER BY createdAt ASC")
    fun getForDiario(diarioId: Long): List<DiarioAttachmentEntity>

    @Query("SELECT * FROM diario_attachments WHERE id = :id LIMIT 1")
    fun getById(id: String): DiarioAttachmentEntity?

    @Query("SELECT diarioId, COUNT(*) AS count FROM diario_attachments GROUP BY diarioId")
    fun getCounts(): List<DiarioAttachmentCount>

    @Query("UPDATE diario_attachments SET fileName = :fileName, updatedAt = :updatedAt WHERE id = :id")
    fun rename(id: String, fileName: String, updatedAt: Long)

    @Query("UPDATE diario_attachments SET diarioId = :destinationDiarioId, updatedAt = :updatedAt WHERE id = :id")
    fun move(id: String, destinationDiarioId: Long, updatedAt: Long)

    @Query(
        "UPDATE diario_attachments " +
            "SET mimeType = :mimeType, fileSize = :fileSize, encryptedData = :encryptedData, updatedAt = :updatedAt " +
            "WHERE id = :id"
    )
    fun replaceData(id: String, mimeType: String, fileSize: Long, encryptedData: ByteArray, updatedAt: Long)

    @Delete
    fun delete(attachment: DiarioAttachmentEntity)
}
