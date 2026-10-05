package com.ypg.neville.feature.ai.domain

import android.content.Context
import androidx.core.content.edit
import com.ypg.neville.model.preferences.DbPreferences

object OpenRouterConfiguration {
    const val AUTOMATIC_FREE_MODEL = "openrouter/free"
    const val DEFAULT_MODEL = AUTOMATIC_FREE_MODEL
    const val STANDARD_DAILY_FREE_LIMIT = 50
    const val CREDITED_DAILY_FREE_LIMIT = 1_000
    const val CURRENT_CONSENT_VERSION = 1

    private const val SELECTED_MODEL_KEY = "ai.openrouter.selected-free-model"
    private const val DAILY_LIMIT_KEY = "ai.openrouter.daily-free-request-limit"
    private const val PRIVACY_CONSENT_KEY = "ai.openrouter.privacy-consent-version"
    private const val AI_TERMS_KEY = "AceptacionDescargoIA"
    private const val PERSONAL_VOICE_KEY = "setting_IA_TratamientoPersonal"

    val fallbackModels = listOf(
        OpenRouterModel(
            id = AUTOMATIC_FREE_MODEL,
            name = "Selección automática gratuita",
            contextLength = null,
            description = "OpenRouter selecciona automáticamente un modelo gratuito disponible y puede evitar modelos temporalmente saturados."
        )
    )

    fun isFreeModelIdentifier(identifier: String): Boolean =
        identifier == AUTOMATIC_FREE_MODEL || identifier.endsWith(":free")

    fun selectedModelIdentifier(context: Context): String {
        val stored = DbPreferences.default(context).getString(SELECTED_MODEL_KEY, null)
        return stored?.takeIf(::isFreeModelIdentifier) ?: DEFAULT_MODEL
    }

    fun hasSelectedModelIdentifier(context: Context): Boolean {
        val stored = DbPreferences.default(context).getString(SELECTED_MODEL_KEY, null)
        return stored != null && isFreeModelIdentifier(stored)
    }

    fun selectModel(context: Context, identifier: String) {
        if (!isFreeModelIdentifier(identifier)) return
        DbPreferences.default(context).edit { putString(SELECTED_MODEL_KEY, identifier) }
    }

    fun dailyFreeRequestLimit(context: Context): Int {
        val stored = DbPreferences.default(context).getInt(DAILY_LIMIT_KEY, 0)
        return stored.takeIf { it > 0 } ?: STANDARD_DAILY_FREE_LIMIT
    }

    fun updateDailyFreeRequestLimit(context: Context, limit: Int) {
        if (limit <= 0) return
        DbPreferences.default(context).edit { putInt(DAILY_LIMIT_KEY, limit) }
    }

    fun resetDailyFreeRequestLimit(context: Context) {
        DbPreferences.default(context).edit { remove(DAILY_LIMIT_KEY) }
    }

    fun hasPrivacyConsent(context: Context): Boolean =
        DbPreferences.default(context).getInt(PRIVACY_CONSENT_KEY, 0) >= CURRENT_CONSENT_VERSION

    fun acceptPrivacyConsent(context: Context) {
        DbPreferences.default(context).edit {
            putInt(PRIVACY_CONSENT_KEY, CURRENT_CONSENT_VERSION)
        }
    }

    fun revokePrivacyConsent(context: Context) {
        DbPreferences.default(context).edit { remove(PRIVACY_CONSENT_KEY) }
    }

    fun hasAcceptedAiTerms(context: Context): Boolean =
        DbPreferences.default(context).getBoolean(AI_TERMS_KEY, false)

    fun setAcceptedAiTerms(context: Context, accepted: Boolean) {
        DbPreferences.default(context).edit { putBoolean(AI_TERMS_KEY, accepted) }
    }

    fun usesPersonalVoice(context: Context): Boolean =
        DbPreferences.default(context).getBoolean(PERSONAL_VOICE_KEY, true)

    fun setUsesPersonalVoice(context: Context, enabled: Boolean) {
        DbPreferences.default(context).edit { putBoolean(PERSONAL_VOICE_KEY, enabled) }
    }
}
