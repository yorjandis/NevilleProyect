package com.ypg.neville.localization

import java.util.Locale

/** Languages currently supported by Neville's user interface. */
enum class AppLanguage(val languageTag: String) {
    SPANISH("es"),
    ENGLISH("en"),
    SIMPLIFIED_CHINESE("zh-Hans");

    companion object {
        fun fromLanguageTag(languageTag: String?): AppLanguage? {
            val normalizedTag = languageTag
                ?.trim()
                ?.replace('_', '-')
                ?.lowercase(Locale.ROOT)
                .orEmpty()

            return when {
                normalizedTag == "es" || normalizedTag.startsWith("es-") -> SPANISH
                normalizedTag == "en" || normalizedTag.startsWith("en-") -> ENGLISH
                normalizedTag == "zh" ||
                    normalizedTag == "zh-cn" ||
                    normalizedTag == "zh-sg" ||
                    normalizedTag.startsWith("zh-hans") -> SIMPLIFIED_CHINESE
                else -> null
            }
        }
    }
}
