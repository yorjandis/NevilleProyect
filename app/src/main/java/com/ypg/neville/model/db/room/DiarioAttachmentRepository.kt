package com.ypg.neville.model.db.room

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.ypg.neville.model.security.PostQuantumAesTextCrypto
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

data class PendingDiarioAttachment(
    val id: String = UUID.randomUUID().toString(),
    val data: ByteArray,
    val fileName: String,
    val mimeType: String
)

class DiarioAttachmentRepository(
    private val context: Context,
    private val database: NevilleRoomDatabase = NevilleRoomDatabase.getInstance(context)
) {
    private val attachmentDao = database.diarioAttachmentDao()
    private val diarioDao = database.diarioDao()

    fun attachmentsFor(diarioId: Long): List<DiarioAttachmentEntity> =
        attachmentDao.getForDiario(diarioId)

    fun attachmentCounts(): Map<Long, Int> =
        attachmentDao.getCounts().associate { it.diarioId to it.count }

    fun readPending(uri: Uri): PendingDiarioAttachment {
        val resolver = context.contentResolver
        val document = DocumentFile.fromSingleUri(context, uri)
        val mimeType = resolver.getType(uri).orEmpty().ifBlank { "application/octet-stream" }
        val fileName = document?.name?.takeIf(String::isNotBlank)
            ?: "Archivo-${UUID.randomUUID().toString().take(8)}"
        val source = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("No se pudo leer el archivo seleccionado")
        return PendingDiarioAttachment(
            data = compressImageIfNeeded(source, mimeType),
            fileName = fileName,
            mimeType = mimeType
        )
    }

    fun insertPending(diarioId: Long, pending: PendingDiarioAttachment): DiarioAttachmentEntity {
        require(diarioDao.getById(diarioId) != null) { "La entrada del Diario ya no existe" }
        val attachmentId = UUID.randomUUID().toString()
        val entity = DiarioAttachmentEntity(
            id = attachmentId,
            diarioId = diarioId,
            fileName = pending.fileName,
            mimeType = pending.mimeType,
            fileSize = pending.data.size.toLong(),
            createdAt = System.currentTimeMillis(),
            encryptedData = PostQuantumAesTextCrypto.encryptBytes(
                pending.data,
                attachmentAad(attachmentId)
            )
        )
        database.runInTransaction {
            attachmentDao.insert(entity)
            diarioDao.touch(diarioId)
        }
        return entity
    }

    fun importUris(diarioId: Long, uris: List<Uri>): Int {
        var imported = 0
        uris.forEach { uri ->
            insertPending(diarioId, readPending(uri))
            imported++
        }
        return imported
    }

    fun rename(attachment: DiarioAttachmentEntity, proposedName: String) {
        val trimmed = proposedName.trim()
        require(trimmed.isNotEmpty()) { "El nombre del archivo no puede estar vacío" }
        val existingExtension = attachment.fileName.substringAfterLast('.', "")
        val finalName = if ('.' in trimmed || existingExtension.isEmpty()) {
            trimmed
        } else {
            "$trimmed.$existingExtension"
        }
        database.runInTransaction {
            attachmentDao.rename(attachment.id, finalName)
            diarioDao.touch(attachment.diarioId)
        }
    }

    fun move(attachment: DiarioAttachmentEntity, destinationDiarioId: Long) {
        if (attachment.diarioId == destinationDiarioId) return
        require(diarioDao.getById(destinationDiarioId) != null) { "La entrada de destino ya no existe" }
        database.runInTransaction {
            attachmentDao.move(attachment.id, destinationDiarioId)
            diarioDao.touch(attachment.diarioId)
            diarioDao.touch(destinationDiarioId)
        }
    }

    fun delete(attachment: DiarioAttachmentEntity) {
        database.runInTransaction {
            attachmentDao.delete(attachment)
            diarioDao.touch(attachment.diarioId)
        }
    }

    fun exportTo(attachment: DiarioAttachmentEntity, destination: Uri) {
        val clearData = decryptedData(attachment)
        context.contentResolver.openOutputStream(destination, "w")?.use { it.write(clearData) }
            ?: error("No se pudo crear el archivo de destino")
    }

    fun decryptedData(attachment: DiarioAttachmentEntity): ByteArray {
        require(attachment.cryptoVersion == 1) { "Versión de cifrado no compatible" }
        return PostQuantumAesTextCrypto.decryptBytes(
            attachment.encryptedData,
            attachmentAad(attachment.id)
        )
    }

    fun replaceData(
        attachment: DiarioAttachmentEntity,
        clearData: ByteArray,
        mimeType: String
    ): DiarioAttachmentEntity {
        require(mimeType.startsWith("image/")) { "El archivo editado debe ser una imagen" }
        val encryptedData = PostQuantumAesTextCrypto.encryptBytes(
            clearData,
            attachmentAad(attachment.id)
        )
        database.runInTransaction {
            attachmentDao.replaceData(
                id = attachment.id,
                mimeType = mimeType,
                fileSize = clearData.size.toLong(),
                encryptedData = encryptedData
            )
            diarioDao.touch(attachment.diarioId)
        }
        return attachment.copy(
            mimeType = mimeType,
            fileSize = clearData.size.toLong(),
            encryptedData = encryptedData
        )
    }

    fun openAttachment(attachment: DiarioAttachmentEntity) {
        val cacheDirectory = File(context.cacheDir, "diary_attachments").apply { mkdirs() }
        val safeName = attachment.fileName
            .replace(Regex("[^A-Za-z0-9._ -]"), "_")
            .ifBlank { "archivo" }
        val previewFile = File(cacheDirectory, "${attachment.id}-$safeName")
        previewFile.writeBytes(decryptedData(attachment))
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            previewFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, attachment.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, attachment.fileName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun compressImageIfNeeded(data: ByteArray, mimeType: String): ByteArray {
        if (!mimeType.equals("image/jpeg", ignoreCase = true)) return data
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
        var sampleSize = 1
        while (
            bounds.outWidth / sampleSize > MAX_IMAGE_DIMENSION ||
            bounds.outHeight / sampleSize > MAX_IMAGE_DIMENSION
        ) {
            sampleSize *= 2
        }
        val bitmap = try {
            BitmapFactory.decodeByteArray(
                data,
                0,
                data.size,
                BitmapFactory.Options().apply { inSampleSize = sampleSize }
            ) ?: return data
        } catch (_: OutOfMemoryError) {
            return data
        }
        return try {
            ByteArrayOutputStream().use { output ->
                if (bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, output)) {
                    output.toByteArray()
                } else {
                    data
                }
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun attachmentAad(id: String): String = "diary-attachment:$id"

    companion object {
        private const val IMAGE_QUALITY = 82
        private const val MAX_IMAGE_DIMENSION = 2_560
    }
}
