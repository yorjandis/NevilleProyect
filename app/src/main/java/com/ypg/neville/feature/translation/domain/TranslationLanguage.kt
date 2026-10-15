package com.ypg.neville.feature.translation.domain

import java.util.Locale

/** Languages currently exposed by Neville's on-device translation features. */
enum class TranslationLanguage(
    val languageTag: String,
    internal val mlKitLanguageTag: String
) {
    SPANISH("es", "es"),
    ENGLISH("en", "en"),
    SIMPLIFIED_CHINESE("zh-Hans", "zh");

    companion object {
        fun fromLanguageTag(languageTag: String?): TranslationLanguage? {
            val normalizedTag = languageTag
                ?.trim()
                ?.replace('_', '-')
                ?.lowercase(Locale.ROOT)
                .orEmpty()

            return when {
                normalizedTag == "es" || normalizedTag.startsWith("es-") -> SPANISH
                normalizedTag == "en" || normalizedTag.startsWith("en-") -> ENGLISH
                normalizedTag == "zh" || normalizedTag.startsWith("zh-") -> SIMPLIFIED_CHINESE
                else -> null
            }
        }
    }
}
