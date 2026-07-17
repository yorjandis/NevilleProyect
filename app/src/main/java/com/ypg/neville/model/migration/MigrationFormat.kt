package com.ypg.neville.model.migration

import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.CodingErrorAction
import java.time.Instant
import java.util.Base64

object MigrationFormat {
    const val FILE_EXTENSION = ".ypgexp"
    const val FORMAT_NAME = "com.ypg.neville.ndjson.export"
    const val FORMAT_VERSION = 2
    const val SCHEMA_VERSION = 1
    const val MAGIC = "YPGEXP-2"
    const val CIPHER = "AES-256-GCM"
    const val KDF = "ARGON2ID"
    const val KDF_VERSION = 19
    const val KDF_MEMORY_KIB = 65_536
    const val KDF_ITERATIONS = 3
    const val KDF_PARALLELISM = 1
    const val PASSWORD_NORMALIZATION = "NFC"
    const val SALT_BYTES = 16
    const val NONCE_BYTES = 12
    const val KEY_BITS = 256
    const val TAG_BITS = 128
    const val MINIMUM_PASSWORD_CODE_POINTS = 15
    const val MAXIMUM_PASSWORD_BYTES = 1_024
    const val MAXIMUM_HEADER_BYTES = 4 * 1_024
    const val MAXIMUM_MANIFEST_BYTES = 1 * 1_024 * 1_024
    const val MAXIMUM_PLAINTEXT_BYTES = 512 * 1_024 * 1_024
    const val MAXIMUM_FILE_BYTES = MAXIMUM_PLAINTEXT_BYTES + MAXIMUM_HEADER_BYTES + 128

    fun utcNow(): String = Instant.now().toString()

    fun millisToIso(millis: Long): String = Instant.ofEpochMilli(millis).toString()

    fun isoToMillis(value: String): Long = Instant.parse(value).toEpochMilli()

    fun packPlaintext(manifest: JSONObject, ndjson: String): ByteArray {
        val manifestBytes = manifest.toString().toByteArray(Charsets.UTF_8)
        val dataBytes = ndjson.toByteArray(Charsets.UTF_8)
        require(manifestBytes.size in 1..MAXIMUM_MANIFEST_BYTES) {
            "El manifiesto supera el tamaño máximo permitido"
        }
        val totalSize = 4L + manifestBytes.size + dataBytes.size
        require(totalSize <= MAXIMUM_PLAINTEXT_BYTES) {
            "La exportación supera el tamaño máximo permitido"
        }
        return ByteArray(totalSize.toInt()).also { output ->
            ByteBuffer.wrap(output, 0, 4).order(ByteOrder.BIG_ENDIAN).putInt(manifestBytes.size)
            manifestBytes.copyInto(output, destinationOffset = 4)
            dataBytes.copyInto(output, destinationOffset = 4 + manifestBytes.size)
        }
    }

    fun unpackPlaintext(bytes: ByteArray): PlainPackage {
        require(bytes.size in 4..MAXIMUM_PLAINTEXT_BYTES) { "Paquete descifrado incompleto" }
        val manifestSize = Integer.toUnsignedLong(
            ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.BIG_ENDIAN).int
        )
        require(manifestSize in 1..MAXIMUM_MANIFEST_BYTES.toLong()) { "Tamaño de manifiesto inválido" }
        val manifestEnd = 4L + manifestSize
        require(manifestEnd <= bytes.size.toLong()) { "Tamaño de manifiesto inválido" }
        val end = manifestEnd.toInt()
        val manifest = decodeUtf8(bytes, 4, end, "manifest.json no es UTF-8 válido")
        val ndjson = decodeUtf8(bytes, end, bytes.size, "data.ndjson no es UTF-8 válido")
        return PlainPackage(JSONObject(manifest), ndjson)
    }

    fun encodeBase64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    fun decodeBase64(value: String): ByteArray = Base64.getDecoder().decode(value)

    private fun decodeUtf8(bytes: ByteArray, start: Int, end: Int, errorMessage: String): String {
        return try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes, start, end - start))
                .toString()
        } catch (error: Exception) {
            throw IllegalArgumentException(errorMessage, error)
        }
    }

    data class PlainPackage(
        val manifest: JSONObject,
        val ndjson: String
    )
}
