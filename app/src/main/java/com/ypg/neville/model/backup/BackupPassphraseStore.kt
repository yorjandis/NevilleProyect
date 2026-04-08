package com.ypg.neville.model.backup

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

class BackupPassphraseStore(private val context: Context) {

    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    fun hasPassphrase(): Boolean {
        return !prefs.getString(KEY_CIPHERTEXT_B64, null).isNullOrBlank() &&
            !prefs.getString(KEY_IV_B64, null).isNullOrBlank()
    }

    fun save(passphrase: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())

        val iv = cipher.iv
        val ciphertext = cipher.doFinal(passphrase.toByteArray(Charsets.UTF_8))

        prefs.edit {
            putString(KEY_IV_B64, Base64.encodeToString(iv, Base64.NO_WRAP))
            putString(KEY_CIPHERTEXT_B64, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
        }
    }

    fun get(): String? {
        val ivB64 = prefs.getString(KEY_IV_B64, null) ?: return null
        val ciphertextB64 = prefs.getString(KEY_CIPHERTEXT_B64, null) ?: return null

        val iv = Base64.decode(ivB64, Base64.NO_WRAP)
        val ciphertext = Base64.decode(ciphertextB64, Base64.NO_WRAP)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(GCM_TAG_BITS, iv)
        )

        val plain = cipher.doFinal(ciphertext)
        return plain.toString(Charsets.UTF_8)
    }

    fun clear() {
        prefs.edit {
            remove(KEY_IV_B64)
            remove(KEY_CIPHERTEXT_B64)
        }
    }

    private fun getOrCreateSecretKey(): javax.crypto.SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null)
        if (existing is javax.crypto.SecretKey) {
            return existing
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "neville_backup_passphrase_wrap_key"

        private const val KEY_IV_B64 = "cloud_backup_passphrase_iv_b64"
        private const val KEY_CIPHERTEXT_B64 = "cloud_backup_passphrase_ciphertext_b64"
    }
}
