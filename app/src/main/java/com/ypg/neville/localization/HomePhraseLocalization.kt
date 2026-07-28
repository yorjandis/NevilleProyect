package com.ypg.neville.localization

import android.content.Context
import java.util.concurrent.ConcurrentHashMap

/**
 * Localizes the contextual phrases shown on the alternative Home screen.
 * Spanish remains the canonical key and the safe fallback.
 */
object HomePhraseLocalization {
    private const val ROOT = "localized_content/home"
    private const val FILE_NAME = "phrases.tsv"
    private val catalogs = ConcurrentHashMap<String, Map<String, String>>()

    fun localized(context: Context, spanishPhrase: String): String {
        val languageFolder = languageFolder(context) ?: return spanishPhrase
        return catalog(context, languageFolder)[spanishPhrase] ?: spanishPhrase
    }

    private fun catalog(context: Context, languageFolder: String): Map<String, String> =
        catalogs.getOrPut(languageFolder) {
            runCatching {
                context.assets.open("$ROOT/$languageFolder/$FILE_NAME")
                    .bufferedReader(Charsets.UTF_8)
                    .useLines { lines ->
                        lines.mapNotNull { line ->
                            val parts = line.removePrefix("\uFEFF").split('\t', limit = 2)
                            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                                parts[0] to parts[1]
                            } else {
                                null
                            }
                        }.toMap()
                    }
            }.getOrDefault(emptyMap())
        }

    private fun languageFolder(context: Context): String? =
        when (context.resources.configuration.locales[0].language) {
            "en" -> "en"
            "zh" -> "zh-Hans"
            else -> null
        }
}

