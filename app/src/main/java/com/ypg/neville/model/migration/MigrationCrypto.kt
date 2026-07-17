package com.ypg.neville.model.migration

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.text.Normalizer
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class MigrationCrypto(
    private val secureRandom: SecureRandom = SecureRandom()
) {
    data class Parameters(
        val salt: ByteArray,
        val nonce: ByteArray
    )

    fun newParameters(): Parameters {
        return Parameters(
            salt = ByteArray(MigrationFormat.SALT_BYTES).also(secureRandom::nextBytes),
            nonce = ByteArray(MigrationFormat.NONCE_BYTES).also(secureRandom::nextBytes)
        )
    }

    fun encryptionJson(parameters: Parameters): JSONObject {
        return JSONObject()
            .put("cipher", MigrationFormat.CIPHER)
            .put("kdf", MigrationFormat.KDF)
            .put("kdfVersion", MigrationFormat.KDF_VERSION)
            .put("kdfMemoryKiB", MigrationFormat.KDF_MEMORY_KIB)
            .put("kdfIterations", MigrationFormat.KDF_ITERATIONS)
            .put("kdfParallelism", MigrationFormat.KDF_PARALLELISM)
            .put("passwordNormalization", MigrationFormat.PASSWORD_NORMALIZATION)
            .put("salt", MigrationFormat.encodeBase64(parameters.salt))
            .put("nonce", MigrationFormat.encodeBase64(parameters.nonce))
            .put("keyLengthBits", MigrationFormat.KEY_BITS)
            .put("tagLengthBits", MigrationFormat.TAG_BITS)
    }

    fun encrypt(plaintext: ByteArray, password: CharArray, parameters: Parameters): ByteArray {
        require(plaintext.size <= MigrationFormat.MAXIMUM_PLAINTEXT_BYTES) {
            "La exportación supera el tamaño máximo permitido"
        }
        validateParameters(parameters)
        val headerBytes = headerJson(parameters).toByteArray(Charsets.UTF_8)
        require(headerBytes.size in 1..MigrationFormat.MAXIMUM_HEADER_BYTES) {
            "La cabecera de cifrado supera el tamaño máximo permitido"
        }
        val authenticatedPrefix = filePrefix(headerBytes)
        val keyBytes = deriveKey(password, parameters.salt, enforceCreationPolicy = true)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(keyBytes, "AES"),
                GCMParameterSpec(MigrationFormat.TAG_BITS, parameters.nonce)
            )
            cipher.updateAAD(authenticatedPrefix)
            val ciphertextAndTag = cipher.doFinal(plaintext)
            val fileSize = authenticatedPrefix.size.toLong() + ciphertextAndTag.size
            require(fileSize <= MigrationFormat.MAXIMUM_FILE_BYTES) {
                "La exportación supera el tamaño máximo permitido"
            }
            return ByteArray(fileSize.toInt()).also { output ->
                authenticatedPrefix.copyInto(output)
                ciphertextAndTag.copyInto(output, destinationOffset = authenticatedPrefix.size)
            }
        } finally {
            keyBytes.fill(0)
        }
    }

    fun decrypt(fileBytes: ByteArray, password: CharArray): MigrationFormat.PlainPackage {
        require(fileBytes.size <= MigrationFormat.MAXIMUM_FILE_BYTES) {
            "El archivo supera el tamaño máximo permitido"
        }
        val magicBytes = MigrationFormat.MAGIC.toByteArray(StandardCharsets.US_ASCII)
        require(fileBytes.size > magicBytes.size + 1 &&
            fileBytes.copyOfRange(0, magicBytes.size).contentEquals(magicBytes) &&
            fileBytes[magicBytes.size] == NEWLINE
        ) { "Formato de archivo no reconocido; se requiere YPGEXP-2" }

        val headerStart = magicBytes.size + 1
        val secondBreak = findHeaderBreak(fileBytes, headerStart)
        val headerSize = secondBreak - headerStart
        require(headerSize in 1..MigrationFormat.MAXIMUM_HEADER_BYTES) { "Cabecera de cifrado inválida" }
        val headerBytes = fileBytes.copyOfRange(headerStart, secondBreak)
        require(headerBytes.none { it == CARRIAGE_RETURN || it == NEWLINE }) {
            "La cabecera debe usar saltos de línea LF"
        }
        val header = JSONObject(decodeUtf8(headerBytes, "Cabecera de cifrado no es UTF-8 válido"))
        val validatedHeader = validateHeader(header)

        val payloadOffset = secondBreak + 1
        val payloadSize = fileBytes.size - payloadOffset
        require(payloadSize > MigrationFormat.TAG_BITS / 8) { "Texto cifrado incompleto" }
        val authenticatedPrefix = fileBytes.copyOfRange(0, payloadOffset)
        val keyBytes = deriveKey(password, validatedHeader.salt, enforceCreationPolicy = false)
        val plaintext = try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(keyBytes, "AES"),
                GCMParameterSpec(MigrationFormat.TAG_BITS, validatedHeader.nonce)
            )
            cipher.updateAAD(authenticatedPrefix)
            try {
                cipher.doFinal(fileBytes, payloadOffset, payloadSize)
            } catch (_: AEADBadTagException) {
                throw SecurityException(AUTHENTICATION_ERROR)
            } catch (_: Exception) {
                throw SecurityException(AUTHENTICATION_ERROR)
            }
        } finally {
            keyBytes.fill(0)
        }
        return try {
            val packageData = MigrationFormat.unpackPlaintext(plaintext)
            validateManifestEncryption(packageData.manifest, header)
            packageData
        } finally {
            plaintext.fill(0)
        }
    }

    private fun headerJson(parameters: Parameters): String {
        return JSONObject()
            .put("format", MigrationFormat.FORMAT_NAME)
            .put("formatVersion", MigrationFormat.FORMAT_VERSION)
            .put("cipher", MigrationFormat.CIPHER)
            .put("kdf", MigrationFormat.KDF)
            .put("kdfVersion", MigrationFormat.KDF_VERSION)
            .put("kdfMemoryKiB", MigrationFormat.KDF_MEMORY_KIB)
            .put("kdfIterations", MigrationFormat.KDF_ITERATIONS)
            .put("kdfParallelism", MigrationFormat.KDF_PARALLELISM)
            .put("passwordNormalization", MigrationFormat.PASSWORD_NORMALIZATION)
            .put("salt", MigrationFormat.encodeBase64(parameters.salt))
            .put("nonce", MigrationFormat.encodeBase64(parameters.nonce))
            .put("keyLengthBits", MigrationFormat.KEY_BITS)
            .put("tagLengthBits", MigrationFormat.TAG_BITS)
            .toString()
    }

    private fun validateHeader(header: JSONObject): ValidatedHeader {
        require(jsonKeys(header) == HEADER_KEYS) { "Campos de cabecera no soportados" }
        require(stringField(header, "format") == MigrationFormat.FORMAT_NAME) { "Formato no soportado" }
        require(intField(header, "formatVersion") == MigrationFormat.FORMAT_VERSION) { "Versión no soportada" }
        require(stringField(header, "cipher") == MigrationFormat.CIPHER) { "Cifrado no soportado" }
        require(stringField(header, "kdf") == MigrationFormat.KDF) { "KDF no soportado" }
        require(intField(header, "kdfVersion") == MigrationFormat.KDF_VERSION) { "Versión de Argon2id no soportada" }
        require(intField(header, "kdfMemoryKiB") == MigrationFormat.KDF_MEMORY_KIB) { "Memoria Argon2id no soportada" }
        require(intField(header, "kdfIterations") == MigrationFormat.KDF_ITERATIONS) { "Iteraciones Argon2id no soportadas" }
        require(intField(header, "kdfParallelism") == MigrationFormat.KDF_PARALLELISM) { "Paralelismo Argon2id no soportado" }
        require(stringField(header, "passwordNormalization") == MigrationFormat.PASSWORD_NORMALIZATION) {
            "Normalización de contraseña no soportada"
        }
        require(intField(header, "keyLengthBits") == MigrationFormat.KEY_BITS) { "Longitud de clave no soportada" }
        require(intField(header, "tagLengthBits") == MigrationFormat.TAG_BITS) { "Etiqueta GCM no soportada" }

        val salt = canonicalBase64(header, "salt")
        val nonce = canonicalBase64(header, "nonce")
        require(salt.size == MigrationFormat.SALT_BYTES && nonce.size == MigrationFormat.NONCE_BYTES) {
            "Longitud de salt o nonce inválida"
        }
        return ValidatedHeader(salt, nonce)
    }

    fun validateExportPassword(password: CharArray) {
        normalizedPasswordBytes(password, enforceCreationPolicy = true).fill(0)
    }

    private fun validateParameters(parameters: Parameters) {
        require(parameters.salt.size == MigrationFormat.SALT_BYTES) { "Longitud de salt inválida" }
        require(parameters.nonce.size == MigrationFormat.NONCE_BYTES) { "Longitud de nonce inválida" }
    }

    private fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        enforceCreationPolicy: Boolean
    ): ByteArray {
        require(salt.size == MigrationFormat.SALT_BYTES) { "Longitud de salt inválida" }
        val passwordBytes = normalizedPasswordBytes(password, enforceCreationPolicy)
        try {
            val parameters = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withSalt(salt)
                .withMemoryAsKB(MigrationFormat.KDF_MEMORY_KIB)
                .withIterations(MigrationFormat.KDF_ITERATIONS)
                .withParallelism(MigrationFormat.KDF_PARALLELISM)
                .build()
            return ByteArray(MigrationFormat.KEY_BITS / 8).also { output ->
                Argon2BytesGenerator().apply { init(parameters) }.generateBytes(passwordBytes, output)
            }
        } finally {
            passwordBytes.fill(0)
        }
    }

    private fun normalizedPasswordBytes(password: CharArray, enforceCreationPolicy: Boolean): ByteArray {
        val normalized = Normalizer.normalize(String(password), Normalizer.Form.NFC)
        if (enforceCreationPolicy) {
            require(normalized.codePointCount(0, normalized.length) >= MigrationFormat.MINIMUM_PASSWORD_CODE_POINTS) {
                "La contraseña del archivo debe tener al menos ${MigrationFormat.MINIMUM_PASSWORD_CODE_POINTS} caracteres."
            }
        }
        val passwordBytes = normalized.toByteArray(StandardCharsets.UTF_8)
        require(passwordBytes.size <= MigrationFormat.MAXIMUM_PASSWORD_BYTES) {
            "La contraseña del archivo es demasiado larga."
        }
        return passwordBytes
    }

    private fun validateManifestEncryption(manifest: JSONObject, header: JSONObject) {
        val encryption = manifest.opt("encryption") as? JSONObject
            ?: throw IllegalArgumentException("Metadatos de cifrado ausentes")
        STRING_ENCRYPTION_KEYS.forEach { key ->
            require(stringField(encryption, key) == stringField(header, key)) {
                "El manifiesto y la cabecera criptográfica no coinciden"
            }
        }
        INTEGER_ENCRYPTION_KEYS.forEach { key ->
            require(intField(encryption, key) == intField(header, key)) {
                "El manifiesto y la cabecera criptográfica no coinciden"
            }
        }
    }

    private fun canonicalBase64(json: JSONObject, key: String): ByteArray {
        val encoded = stringField(json, key)
        val decoded = try {
            MigrationFormat.decodeBase64(encoded)
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException("Base64 inválido en $key", error)
        }
        require(MigrationFormat.encodeBase64(decoded) == encoded) { "Base64 no canónico en $key" }
        return decoded
    }

    private fun findHeaderBreak(bytes: ByteArray, headerStart: Int): Int {
        val searchEndExclusive = minOf(bytes.size, headerStart + MigrationFormat.MAXIMUM_HEADER_BYTES + 1)
        for (index in headerStart until searchEndExclusive) {
            if (bytes[index] == NEWLINE) return index
        }
        throw IllegalArgumentException("Cabecera de cifrado inválida")
    }

    private fun filePrefix(headerBytes: ByteArray): ByteArray {
        val magic = MigrationFormat.MAGIC.toByteArray(StandardCharsets.US_ASCII)
        return ByteArray(magic.size + headerBytes.size + 2).also { prefix ->
            magic.copyInto(prefix)
            prefix[magic.size] = NEWLINE
            headerBytes.copyInto(prefix, destinationOffset = magic.size + 1)
            prefix[prefix.lastIndex] = NEWLINE
        }
    }

    private fun decodeUtf8(bytes: ByteArray, errorMessage: String): String {
        return try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (error: Exception) {
            throw IllegalArgumentException(errorMessage, error)
        }
    }

    private fun stringField(json: JSONObject, key: String): String {
        return json.opt(key) as? String
            ?: throw IllegalArgumentException("$key debe ser una cadena JSON")
    }

    private fun intField(json: JSONObject, key: String): Int {
        return when (val value = json.opt(key)) {
            is Int -> value
            is Long -> {
                require(value in Int.MIN_VALUE..Int.MAX_VALUE) { "$key está fuera de rango" }
                value.toInt()
            }
            else -> throw IllegalArgumentException("$key debe ser un número entero JSON")
        }
    }

    private fun jsonKeys(json: JSONObject): Set<String> {
        val result = mutableSetOf<String>()
        val iterator = json.keys()
        while (iterator.hasNext()) result += iterator.next()
        return result
    }

    private data class ValidatedHeader(
        val salt: ByteArray,
        val nonce: ByteArray
    )

    private companion object {
        val NEWLINE = '\n'.code.toByte()
        val CARRIAGE_RETURN = '\r'.code.toByte()
        const val AUTHENTICATION_ERROR =
            "No se pudo autenticar el archivo. Revisa la contraseña o descarta un archivo manipulado."

        val STRING_ENCRYPTION_KEYS = setOf("cipher", "kdf", "passwordNormalization", "salt", "nonce")
        val INTEGER_ENCRYPTION_KEYS = setOf(
            "kdfVersion",
            "kdfMemoryKiB",
            "kdfIterations",
            "kdfParallelism",
            "keyLengthBits",
            "tagLengthBits"
        )
        val HEADER_KEYS = STRING_ENCRYPTION_KEYS + INTEGER_ENCRYPTION_KEYS + setOf("format", "formatVersion")
    }
}
