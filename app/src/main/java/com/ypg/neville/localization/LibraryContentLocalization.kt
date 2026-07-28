package com.ypg.neville.localization

import android.content.Context
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves translated Encyclopedia, Reflections and Help assets while keeping
 * the original Spanish paths as stable identifiers and as a safe fallback.
 */
object LibraryContentLocalization {
    private const val ROOT = "localized_content/library"
    private val fileIndexes = ConcurrentHashMap<String, Map<String, String>>()
    private val titleIndexes = ConcurrentHashMap<String, Map<String, String>>()

    fun resolveAssetPath(context: Context, spanishAssetPath: String): String {
        if (!isSupportedPath(spanishAssetPath)) return spanishAssetPath

        val languageFolder = languageFolder(context) ?: return spanishAssetPath
        val fileName = spanishAssetPath.substringAfterLast('/')
        val actualFileName = localizedFileIndex(context, languageFolder)[normalize(fileName)]
            ?: return spanishAssetPath
        return "$ROOT/$languageFolder/$actualFileName"
    }

    fun localizedTitle(context: Context, spanishAssetPath: String): String? {
        val languageFolder = languageFolder(context) ?: return null
        val resourceName = spanishAssetPath.substringAfterLast('/').removeSuffix(".txt")

        titleIndex(context, languageFolder)[normalize(resourceName)]?.let { return it }

        if (!spanishAssetPath.startsWith("reflexiones/")) return null
        val localizedPath = resolveAssetPath(context, spanishAssetPath)
        if (localizedPath == spanishAssetPath) return null

        return runCatching {
            val raw = context.assets.open(localizedPath).bufferedReader(Charsets.UTF_8).use { it.readText() }
            JSONObject(raw).optString("titulo").trim()
        }.getOrNull()?.takeIf(String::isNotBlank)
    }

    fun localizedEncyclopediaCategoryTitle(context: Context, spanishFolderName: String): String? {
        val languageFolder = languageFolder(context) ?: return null
        return titleIndex(context, languageFolder)[normalize("category:$spanishFolderName")]
    }

    private fun isSupportedPath(path: String): Boolean =
        path.startsWith("enciclopedia/") ||
            path.startsWith("reflexiones/") ||
            path.startsWith("ayudas/")

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
                .filter { it.endsWith(".txt", ignoreCase = true) }
                .associateBy(::normalize)
        }

    private fun titleIndex(context: Context, languageFolder: String): Map<String, String> =
        titleIndexes.getOrPut(languageFolder) {
            runCatching {
                context.assets.open("$ROOT/$languageFolder/titles.tsv")
                    .bufferedReader(Charsets.UTF_8)
                    .useLines { lines ->
                        lines.mapNotNull { line ->
                            val parts = line.removePrefix("\uFEFF").split('\t', limit = 2)
                            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                                normalize(parts[0]) to parts[1].trim()
                            } else {
                                null
                            }
                        }.toMap()
                    }
            }.getOrDefault(emptyMap())
        }

    private fun normalize(value: String): String = Normalizer
        .normalize(value, Normalizer.Form.NFC)
        .lowercase(Locale.ROOT)
}
