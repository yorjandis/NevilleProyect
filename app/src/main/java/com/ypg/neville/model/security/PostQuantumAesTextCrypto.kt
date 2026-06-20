package com.ypg.neville.model.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ypg.neville.model.db.room.PreferenceEntity
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object PostQuantumAesTextCrypto {

    fun configure(context: Context) {
        appContext = context.applicationContext
    }

    fun cacheRecoveryPassphrase(passphrase: String) {
        cachedRecoveryPassphrase = passphrase.trim().takeIf { it.isNotBlank() }
    }

    fun saveRecoveryPassphrase(passphrase: String) {
        val normalized = passphrase.trim()
        require(normalized.isNotBlank()) { "La frase secreta no puede estar vacía" }
        val dataKey = getOrCreateDataKey()
        val wrapped = wrapDataKeyWithPassphrase(dataKey, normalized)
        val dao = NevilleRoomDatabase.getInstance(requireContext()).preferenceDao()
        dao.upsertAll(
            listOf(
                PreferenceEntity(KEY_RECOVERY_SALT, wrapped.saltB64, TYPE_STRING),
                PreferenceEntity(KEY_RECOVERY_IV, wrapped.ivB64, TYPE_STRING),
                PreferenceEntity(KEY_RECOVERY_CIPHERTEXT, wrapped.ciphertextB64, TYPE_STRING),
                PreferenceEntity(KEY_RECOVERY_ITERATIONS, PBKDF2_ITERATIONS.toString(), TYPE_INT)
            )
        )
        cachedRecoveryPassphrase = normalized
    }

    fun clearRecoveryPassphrase() {
        NevilleRoomDatabase.getInstance(requireContext())
            .preferenceDao()
            .deleteByPrefix(RECOVERY_PREFIX)
        cachedRecoveryPassphrase = null
    }

    fun syncRecoveryKeyFromDatabase(db: SupportSQLiteDatabase) {
        if (cachedDataKey != null || localWrappedKeyExists()) return
        val wrapped = readRecoveryWrap(db) ?: return
        val passphrase = cachedRecoveryPassphrase
        if (passphrase.isNullOrBlank()) {
            recoveryPassphraseRequired = true
            return
        }
        val dataKey = unwrapDataKeyWithPassphrase(wrapped, passphrase)
        cachedDataKey = dataKey
        recoveryPassphraseRequired = false
        saveLocalWrappedDataKey(dataKey)
    }

    fun encrypt(plainText: String, aad: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(getOrCreateDataKey(), AES))
        cipher.updateAAD(aad.toByteArray(Charsets.UTF_8))

        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return PREFIX_V2 + Base64.encodeToString(join(iv, encrypted), Base64.NO_WRAP)
    }

    fun decrypt(value: String, aad: String): String {
        return when {
            value.startsWith(PREFIX_V2) -> decryptV2(value, aad)
            value.startsWith(PREFIX_V1) -> decryptLegacyV1(value, aad)
            else -> value
        }
    }

    fun isEncrypted(value: String): Boolean = value.startsWith(PREFIX_V2) || value.startsWith(PREFIX_V1)

    fun isRecoverableEncrypted(value: String): Boolean = value.startsWith(PREFIX_V2)

    private fun decryptV2(value: String, aad: String): String {
        val payload = Base64.decode(value.removePrefix(PREFIX_V2), Base64.NO_WRAP)
        require(payload.size > IV_SIZE_BYTES) { "Texto cifrado inválido" }

        val iv = payload.copyOfRange(0, IV_SIZE_BYTES)
        val encrypted = payload.copyOfRange(IV_SIZE_BYTES, payload.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(getOrCreateDataKey(), AES), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(aad.toByteArray(Charsets.UTF_8))

        return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }

    private fun decryptLegacyV1(value: String, aad: String): String {
        val payload = Base64.decode(value.removePrefix(PREFIX_V1), Base64.NO_WRAP)
        require(payload.size > IV_SIZE_BYTES) { "Texto cifrado inválido" }

        val iv = payload.copyOfRange(0, IV_SIZE_BYTES)
        val encrypted = payload.copyOfRange(IV_SIZE_BYTES, payload.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKeystoreKey(LEGACY_DIRECT_KEY_ALIAS),
            GCMParameterSpec(GCM_TAG_BITS, iv)
        )
        cipher.updateAAD(aad.toByteArray(Charsets.UTF_8))

        return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }

    private fun getOrCreateDataKey(): ByteArray {
        cachedDataKey?.let { return it }

        readLocalWrappedDataKey()?.let {
            cachedDataKey = it
            recoveryPassphraseRequired = false
            return it
        }

        if (recoveryPassphraseRequired) {
            error("Las notas y entradas cifradas requieren la frase secreta de recuperación")
        }

        val dataKey = ByteArray(KEY_SIZE_BYTES).also { secureRandom.nextBytes(it) }
        cachedDataKey = dataKey
        saveLocalWrappedDataKey(dataKey)

        cachedRecoveryPassphrase?.let { passphrase ->
            runCatching { saveRecoveryPassphrase(passphrase) }
        }

        return dataKey
    }

    private fun readLocalWrappedDataKey(): ByteArray? {
        val prefs = localPrefs()
        val ivB64 = prefs.getString(KEY_LOCAL_IV, null) ?: return null
        val ciphertextB64 = prefs.getString(KEY_LOCAL_CIPHERTEXT, null) ?: return null
        val iv = Base64.decode(ivB64, Base64.NO_WRAP)
        val ciphertext = Base64.decode(ciphertextB64, Base64.NO_WRAP)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKeystoreKey(DATA_KEY_WRAP_ALIAS),
            GCMParameterSpec(GCM_TAG_BITS, iv)
        )
        cipher.updateAAD(LOCAL_WRAP_AAD)
        return cipher.doFinal(ciphertext)
    }

    private fun saveLocalWrappedDataKey(dataKey: ByteArray) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKeystoreKey(DATA_KEY_WRAP_ALIAS))
        cipher.updateAAD(LOCAL_WRAP_AAD)
        val ciphertext = cipher.doFinal(dataKey)

        localPrefs().edit {
            putString(KEY_LOCAL_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            putString(KEY_LOCAL_CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
        }
    }

    private fun localWrappedKeyExists(): Boolean {
        val prefs = localPrefs()
        return !prefs.getString(KEY_LOCAL_IV, null).isNullOrBlank() &&
            !prefs.getString(KEY_LOCAL_CIPHERTEXT, null).isNullOrBlank()
    }

    private fun wrapDataKeyWithPassphrase(dataKey: ByteArray, passphrase: String): RecoveryWrap {
        val salt = ByteArray(SALT_SIZE_BYTES).also { secureRandom.nextBytes(it) }
        val key = derivePassphraseKey(passphrase, salt, PBKDF2_ITERATIONS)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(RECOVERY_WRAP_AAD)
        val ciphertext = cipher.doFinal(dataKey)
        return RecoveryWrap(
            saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP),
            ivB64 = Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
            ciphertextB64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
            iterations = PBKDF2_ITERATIONS
        )
    }

    private fun unwrapDataKeyWithPassphrase(wrapped: RecoveryWrap, passphrase: String): ByteArray {
        val salt = Base64.decode(wrapped.saltB64, Base64.NO_WRAP)
        val iv = Base64.decode(wrapped.ivB64, Base64.NO_WRAP)
        val ciphertext = Base64.decode(wrapped.ciphertextB64, Base64.NO_WRAP)
        val key = derivePassphraseKey(passphrase, salt, wrapped.iterations)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(RECOVERY_WRAP_AAD)
        return cipher.doFinal(ciphertext)
    }

    private fun derivePassphraseKey(passphrase: String, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, iterations, KEY_SIZE_BITS)
        val encoded = SecretKeyFactory.getInstance(KDF).generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(encoded, AES)
    }

    private fun readRecoveryWrap(db: SupportSQLiteDatabase): RecoveryWrap? {
        val values = mutableMapOf<String, String>()
        db.query(
            "SELECT prefKey, prefValue FROM preferences WHERE prefKey IN (?, ?, ?, ?)",
            arrayOf(KEY_RECOVERY_SALT, KEY_RECOVERY_IV, KEY_RECOVERY_CIPHERTEXT, KEY_RECOVERY_ITERATIONS)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                values[cursor.getString(0)] = cursor.getString(1)
            }
        }
        return RecoveryWrap(
            saltB64 = values[KEY_RECOVERY_SALT] ?: return null,
            ivB64 = values[KEY_RECOVERY_IV] ?: return null,
            ciphertextB64 = values[KEY_RECOVERY_CIPHERTEXT] ?: return null,
            iterations = values[KEY_RECOVERY_ITERATIONS]?.toIntOrNull() ?: PBKDF2_ITERATIONS
        )
    }

    private fun getOrCreateKeystoreKey(alias: String): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(alias, null)
        if (existing is SecretKey) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .build()

        generator.init(spec)
        return generator.generateKey()
    }

    private fun join(iv: ByteArray, encrypted: ByteArray): ByteArray {
        return ByteBuffer.allocate(iv.size + encrypted.size)
            .put(iv)
            .put(encrypted)
            .array()
    }

    private fun requireContext(): Context {
        return appContext ?: error("PostQuantumAesTextCrypto no está configurado")
    }

    private fun localPrefs() = requireContext().getSharedPreferences(LOCAL_PREFS, Context.MODE_PRIVATE)

    private data class RecoveryWrap(
        val saltB64: String,
        val ivB64: String,
        val ciphertextB64: String,
        val iterations: Int
    )

    private const val PREFIX_V2 = "NV_PQAESGCM_V2:"
    private const val PREFIX_V1 = "NV_PQAESGCM_V1:"

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val DATA_KEY_WRAP_ALIAS = "neville_secure_text_data_key_wrap_key"
    private const val LEGACY_DIRECT_KEY_ALIAS = "neville_notes_journal_aes256_gcm_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KDF = "PBKDF2WithHmacSHA256"
    private const val AES = "AES"

    private const val KEY_SIZE_BITS = 256
    private const val KEY_SIZE_BYTES = KEY_SIZE_BITS / 8
    private const val GCM_TAG_BITS = 128
    private const val IV_SIZE_BYTES = 12
    private const val SALT_SIZE_BYTES = 16
    private const val PBKDF2_ITERATIONS = 210_000

    private const val TYPE_STRING = "string"
    private const val TYPE_INT = "int"

    private const val LOCAL_PREFS = "secure_text_crypto_prefs"
    private const val KEY_LOCAL_IV = "secure_text_data_key_local_iv_b64"
    private const val KEY_LOCAL_CIPHERTEXT = "secure_text_data_key_local_ciphertext_b64"

    private const val RECOVERY_PREFIX = "secure_text_recovery_%"
    private const val KEY_RECOVERY_SALT = "secure_text_recovery_salt_b64"
    private const val KEY_RECOVERY_IV = "secure_text_recovery_iv_b64"
    private const val KEY_RECOVERY_CIPHERTEXT = "secure_text_recovery_ciphertext_b64"
    private const val KEY_RECOVERY_ITERATIONS = "secure_text_recovery_iterations"

    private val LOCAL_WRAP_AAD = "NEVILLE_SECURE_TEXT_LOCAL_DATA_KEY_V1".toByteArray(Charsets.UTF_8)
    private val RECOVERY_WRAP_AAD = "NEVILLE_SECURE_TEXT_RECOVERY_DATA_KEY_V1".toByteArray(Charsets.UTF_8)

    private var appContext: Context? = null
    private var cachedDataKey: ByteArray? = null
    private var cachedRecoveryPassphrase: String? = null
    private var recoveryPassphraseRequired: Boolean = false
    private val secureRandom = SecureRandom()
}
