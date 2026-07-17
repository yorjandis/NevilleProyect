package com.ypg.neville.model.migration

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class MigrationCryptoTest {
    private val decomposedPassword = "12345678901234e\u0301".toCharArray()
    private val composedPassword = "12345678901234é".toCharArray()

    @Test
    fun `YPGEXP-2 matches libsodium Argon2id key and normalizes password to NFC`() {
        val crypto = MigrationCrypto()
        val parameters = MigrationCrypto.Parameters(
            salt = ByteArray(MigrationFormat.SALT_BYTES) { it.toByte() },
            nonce = ByteArray(MigrationFormat.NONCE_BYTES) { (it + 16).toByte() }
        )
        val plaintext = plaintext(crypto, parameters)

        val file = crypto.encrypt(plaintext, decomposedPassword, parameters)
        val packageData = crypto.decrypt(file, composedPassword)

        val headerEnd = file.indexOf('\n'.code.toByte(), MigrationFormat.MAGIC.length + 1)
        val payloadOffset = headerEnd + 1
        val independentCipher = Cipher.getInstance("AES/GCM/NoPadding")
        independentCipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(LIBSODIUM_EXPECTED_KEY.hexToBytes(), "AES"),
            GCMParameterSpec(MigrationFormat.TAG_BITS, parameters.nonce)
        )
        independentCipher.updateAAD(file.copyOfRange(0, payloadOffset))
        val independentlyDecrypted = independentCipher.doFinal(file, payloadOffset, file.size - payloadOffset)

        assertTrue(file.copyOfRange(0, MigrationFormat.MAGIC.length).contentEquals(MigrationFormat.MAGIC.toByteArray()))
        assertArrayEquals(plaintext, independentlyDecrypted)
        assertEquals(MigrationFormat.FORMAT_NAME, packageData.manifest.getString("format"))
        assertEquals("{\"type\":\"test\"}\n", packageData.ndjson)
    }

    @Test
    fun `wrong password and modified authenticated bytes are rejected`() {
        val crypto = MigrationCrypto()
        val parameters = crypto.newParameters()
        val file = crypto.encrypt(plaintext(crypto, parameters), composedPassword, parameters)

        assertAuthenticationFailure {
            crypto.decrypt(file, "otra-clave-segura".toCharArray())
        }

        val headerEnd = file.indexOf('\n'.code.toByte(), MigrationFormat.MAGIC.length + 1)
        val changedHeaderBytes = ByteArray(file.size + 1).also { changed ->
            file.copyInto(changed, endIndex = headerEnd)
            changed[headerEnd] = ' '.code.toByte()
            file.copyInto(changed, destinationOffset = headerEnd + 1, startIndex = headerEnd)
        }
        assertAuthenticationFailure {
            crypto.decrypt(changedHeaderBytes, composedPassword)
        }

        val changedCiphertext = file.clone().also { it[headerEnd + 1] = (it[headerEnd + 1].toInt() xor 1).toByte() }
        assertAuthenticationFailure {
            crypto.decrypt(changedCiphertext, composedPassword)
        }

        val changedTag = file.clone().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        assertAuthenticationFailure {
            crypto.decrypt(changedTag, composedPassword)
        }
    }

    @Test
    fun `legacy magic and non-contract header values are rejected`() {
        val crypto = MigrationCrypto()
        assertFailure {
            crypto.decrypt("MYAPPEXPORT-1\n{}\n01234567890123456".toByteArray(), composedPassword)
        }

        val parameters = crypto.newParameters()
        val file = crypto.encrypt(plaintext(crypto, parameters), composedPassword, parameters)
        val headerStart = MigrationFormat.MAGIC.length + 1
        val headerEnd = file.indexOf('\n'.code.toByte(), headerStart)
        val header = file.copyOfRange(headerStart, headerEnd).toString(Charsets.UTF_8)
        val modifiedHeader = header.replace("\"kdfParallelism\":1", "\"kdfParallelism\":4")
        assertTrue("El vector de prueba debe modificar el parámetro", modifiedHeader != header)
        val changed = file.copyOf().also {
            modifiedHeader.toByteArray().copyInto(it, destinationOffset = headerStart)
        }
        assertFailure { crypto.decrypt(changed, composedPassword) }
    }

    private fun plaintext(crypto: MigrationCrypto, parameters: MigrationCrypto.Parameters): ByteArray {
        val manifest = JSONObject()
            .put("format", MigrationFormat.FORMAT_NAME)
            .put("formatVersion", MigrationFormat.FORMAT_VERSION)
            .put("encryption", crypto.encryptionJson(parameters))
        return MigrationFormat.packPlaintext(manifest, "{\"type\":\"test\"}\n")
    }

    private fun assertAuthenticationFailure(block: () -> Unit) {
        val error = runCatching(block).exceptionOrNull()
        assertTrue("Se esperaba un error de autenticación", error is SecurityException)
        assertTrue(error?.message?.contains("autenticar") == true)
    }

    private fun assertFailure(block: () -> Unit) {
        assertTrue("Se esperaba que el archivo fuese rechazado", runCatching(block).isFailure)
    }

    private fun ByteArray.indexOf(value: Byte, startIndex: Int): Int {
        for (index in startIndex until size) {
            if (this[index] == value) return index
        }
        return -1
    }

    private fun String.hexToBytes(): ByteArray {
        require(length % 2 == 0)
        return ByteArray(length / 2) { index ->
            substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }

    private companion object {
        // libsodium crypto_pwhash(ARGON2ID13), m=65536 KiB, t=3, p=1, salt=00..0f.
        const val LIBSODIUM_EXPECTED_KEY =
            "d875cfd274da5b84610615437419ac06573a980492700d2895639ab67360732a"
    }
}
