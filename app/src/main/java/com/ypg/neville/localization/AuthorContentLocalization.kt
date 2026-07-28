package com.ypg.neville.localization

import android.content.Context
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/** Resolves author content to its bundled translation, falling back to Spanish. */
object AuthorContentLocalization {
    private const val ROOT = "localized_content/authors"
    private val fileIndexes = ConcurrentHashMap<String, Map<String, String>>()

    fun resolveAssetPath(context: Context, spanishAssetPath: String): String {
        if (!spanishAssetPath.startsWith("autores/") && !spanishAssetPath.startsWith("frases/")) {
            return spanishAssetPath
        }
        val languageFolder = languageFolder(context) ?: return spanishAssetPath
        val spanishFileName = spanishAssetPath.substringAfterLast('/')
        val localizedFileName = when {
            spanishAssetPath.contains("/cita/") && !spanishFileName.startsWith("cita_", ignoreCase = true) -> {
                "cita_$spanishFileName"
            }
            spanishFileName.equals("listfrases_gregg.txt", ignoreCase = true) -> {
                "listfrases_de_gregg.txt"
            }
            else -> spanishFileName
        }

        val actualFileName = localizedFileIndex(context, languageFolder)[normalizeFileName(localizedFileName)]
            ?: return spanishAssetPath
        return "$ROOT/$languageFolder/$actualFileName"
    }

    fun localizedCitationTitle(context: Context, spanishResourceName: String): String? {
        val originalPath = "autores/neville/cita/$spanishResourceName.txt"
        val localizedPath = resolveAssetPath(context, originalPath)
        if (localizedPath == originalPath) return null

        return runCatching {
            context.assets.open(localizedPath).bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.firstOrNull { it.isNotBlank() }?.trim()?.removePrefix("\uFEFF")
            }
        }.getOrNull()?.takeIf(String::isNotBlank)
    }

    private fun languageFolder(context: Context): String? =
        when (context.resources.configuration.locales[0].language) {
            "en" -> "en"
            "zh" -> "zh-Hans"
            else -> null
        }

    private fun localizedFileIndex(context: Context, languageFolder: String): Map<String, String> =
        fileIndexes.getOrPut(languageFolder) {
            context.assets.list("$ROOT/$languageFolder")
                .orEmpty()
                .associateBy(::normalizeFileName)
        }

    private fun normalizeFileName(value: String): String = Normalizer
        .normalize(value, Normalizer.Form.NFC)
        .lowercase(Locale.ROOT)
}
