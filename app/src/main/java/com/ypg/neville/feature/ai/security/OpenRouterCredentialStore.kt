package com.ypg.neville.feature.ai.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class OpenRouterCredentialStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun hasApiKey(): Boolean = readApiKey() != null

    fun saveApiKey(value: String) {
        val clean = value.trim()
        require(clean.isNotEmpty()) { "Introduce una clave de API de OpenRouter." }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        cipher.updateAAD(AAD)
        val encrypted = cipher.doFinal(clean.toByteArray(Charsets.UTF_8))
        preferences.edit {
            putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            putString(KEY_VALUE, Base64.encodeToString(encrypted, Base64.NO_WRAP))
        }
    }

    fun readApiKey(): String? {
        val ivValue = preferences.getString(KEY_IV, null) ?: return null
        val encryptedValue = preferences.getString(KEY_VALUE, null) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(GCM_TAG_BITS, Base64.decode(ivValue, Base64.NO_WRAP))
            )
            cipher.updateAAD(AAD)
            cipher.doFinal(Base64.decode(encryptedValue, Base64.NO_WRAP))
                .toString(Charsets.UTF_8)
                .trim()
                .takeIf { it.isNotEmpty() }
        }.getOrElse {
            deleteApiKey()
            null
        }
    }

    fun deleteApiKey() {
        preferences.edit { clear() }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            generateKey()
        }
    }

    companion object {
        private const val PREFERENCES = "openrouter_secure_credentials"
        private const val KEY_IV = "api_key_iv"
        private const val KEY_VALUE = "api_key_value"
        private const val KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "neville.openrouter.api-key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        private val AAD = "com.ypg.neville.openrouter.api-key".toByteArray(Charsets.UTF_8)
    }
}
