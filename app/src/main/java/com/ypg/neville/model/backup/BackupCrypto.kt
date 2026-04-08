package com.ypg.neville.model.backup

import java.io.BufferedInputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupCrypto {

    fun openEncryptedPayloadOutputStream(rawOutput: OutputStream, passphrase: CharArray): OutputStream {
        val salt = ByteArray(SALT_SIZE).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_SIZE).also { secureRandom.nextBytes(it) }

        val key = deriveAesKey(passphrase, salt, PBKDF2_ITERATIONS)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(AAD)

        writeHeader(rawOutput, salt, iv, PBKDF2_ITERATIONS)
        return CipherOutputStream(rawOutput, cipher)
    }

    fun openPayloadInputStream(rawInput: InputStream, passphrase: CharArray): InputStream {
        val buffered = BufferedInputStream(rawInput)
        buffered.mark(HEADER_MAX_SIZE)

        val magic = ByteArray(MAGIC.size)
        if (buffered.read(magic) != MAGIC.size || !magic.contentEquals(MAGIC)) {
            buffered.reset()
            return buffered
        }

        val version = buffered.read()
        if (version != VERSION.toInt()) {
            throw IllegalStateException("Versión de backup no soportada")
        }

        val saltLength = buffered.read()
        val ivLength = buffered.read()
        if (saltLength <= 0 || ivLength <= 0) {
            throw IllegalStateException("Header de backup inválido")
        }

        val iterationBytes = ByteArray(4)
        if (buffered.read(iterationBytes) != 4) {
            throw IllegalStateException("Header de backup incompleto")
        }
        val iterations = ByteBuffer.wrap(iterationBytes)
            .order(ByteOrder.BIG_ENDIAN)
            .int

        val salt = ByteArray(saltLength)
        if (buffered.read(salt) != saltLength) {
            throw IllegalStateException("Header de backup incompleto (salt)")
        }

        val iv = ByteArray(ivLength)
        if (buffered.read(iv) != ivLength) {
            throw IllegalStateException("Header de backup incompleto (iv)")
        }

        if (passphrase.isEmpty()) {
            throw IllegalStateException("El backup está cifrado y requiere clave")
        }

        val key = deriveAesKey(passphrase, salt, iterations)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(AAD)

        return CipherInputStream(buffered, cipher)
    }

    private fun writeHeader(output: OutputStream, salt: ByteArray, iv: ByteArray, iterations: Int) {
        output.write(MAGIC)
        output.write(VERSION.toInt())
        output.write(salt.size)
        output.write(iv.size)
        output.write(
            ByteBuffer.allocate(4)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(iterations)
                .array()
        )
        output.write(salt)
        output.write(iv)
    }

    private fun deriveAesKey(passphrase: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_SIZE_BITS)
        val factory = SecretKeyFactory.getInstance(KDF)
        val encoded = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(encoded, AES)
    }

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KDF = "PBKDF2WithHmacSHA256"
        private const val AES = "AES"
        private const val VERSION: Byte = 1

        private const val KEY_SIZE_BITS = 256
        private const val GCM_TAG_BITS = 128
        private const val SALT_SIZE = 16
        private const val IV_SIZE = 12
        private const val PBKDF2_ITERATIONS = 210_000

        private val MAGIC = byteArrayOf('N'.code.toByte(), 'V'.code.toByte(), 'B'.code.toByte(), 'K'.code.toByte())
        private val AAD = "NVBK_AES256_GCM_PBKDF2".toByteArray(Charsets.UTF_8)
        private const val HEADER_MAX_SIZE = 64

        private val secureRandom = SecureRandom()
    }
}
