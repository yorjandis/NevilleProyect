package com.ypg.neville.model.db.room

import com.ypg.neville.model.security.PostQuantumAesTextCrypto

object SecureRoomText {
    private const val NOTA_TITULO_AAD = "neville.room.notas.titulo.v1"
    private const val NOTA_NOTA_AAD = "neville.room.notas.nota.v1"
    private const val DIARIO_TITLE_AAD = "neville.room.diario.title.v1"
    private const val DIARIO_CONTENT_AAD = "neville.room.diario.content.v1"

    fun encryptNota(entity: NotaEntity): NotaEntity {
        return entity.copy(
            titulo = encryptNotaTitulo(entity.titulo),
            nota = encryptNotaContenido(entity.nota)
        )
    }

    fun decryptNota(entity: NotaEntity): NotaEntity {
        return entity.copy(
            titulo = decryptNotaTitulo(entity.titulo),
            nota = decryptNotaContenido(entity.nota)
        )
    }

    fun encryptDiario(entity: DiarioEntity): DiarioEntity {
        return entity.copy(
            title = encryptDiarioTitle(entity.title),
            content = encryptDiarioContent(entity.content)
        )
    }

    fun decryptDiario(entity: DiarioEntity): DiarioEntity {
        return entity.copy(
            title = decryptDiarioTitle(entity.title),
            content = decryptDiarioContent(entity.content)
        )
    }

    fun encryptNotaTitulo(value: String): String = encrypt(value, NOTA_TITULO_AAD)
    fun encryptNotaContenido(value: String): String = encrypt(value, NOTA_NOTA_AAD)
    fun encryptDiarioTitle(value: String): String = encrypt(value, DIARIO_TITLE_AAD)
    fun encryptDiarioContent(value: String): String = encrypt(value, DIARIO_CONTENT_AAD)

    fun decryptNotaTitulo(value: String): String = decrypt(value, NOTA_TITULO_AAD)
    fun decryptNotaContenido(value: String): String = decrypt(value, NOTA_NOTA_AAD)
    fun decryptDiarioTitle(value: String): String = decrypt(value, DIARIO_TITLE_AAD)
    fun decryptDiarioContent(value: String): String = decrypt(value, DIARIO_CONTENT_AAD)

    fun isEncrypted(value: String): Boolean = PostQuantumAesTextCrypto.isEncrypted(value)

    fun isRecoverableEncrypted(value: String): Boolean = PostQuantumAesTextCrypto.isRecoverableEncrypted(value)

    private fun encrypt(value: String, aad: String): String {
        return if (PostQuantumAesTextCrypto.isRecoverableEncrypted(value)) {
            value
        } else {
            PostQuantumAesTextCrypto.encrypt(decrypt(value, aad), aad)
        }
    }

    private fun decrypt(value: String, aad: String): String {
        return PostQuantumAesTextCrypto.decrypt(value, aad)
    }
}
