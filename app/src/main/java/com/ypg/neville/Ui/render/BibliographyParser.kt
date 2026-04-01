package com.ypg.neville.ui.render

import android.content.Context

object BibliographyParser {

    fun parseBibliographyText(rawText: String): List<BibliographyEntry> {
        val rawEntries = rawText
            .split("\n\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        return rawEntries.map { entry ->
            val lines = entry
                .lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            val linkLine = lines.firstOrNull { it.lowercase().startsWith("enlace:") }
            val extractedUrl = linkLine
                ?.removePrefix("enlace:")
                ?.trim()
                ?.takeIf { it.isNotBlank() }

            BibliographyEntry(
                text = lines.joinToString("\n"),
                url = extractedUrl
            )
        }
    }

    fun parseBibliographyAsset(
        context: Context,
        assetPath: String,
        omitFirstLines: Int = 1
    ): List<BibliographyEntry> {
        val text = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        val normalized = text
            .lines()
            .drop(omitFirstLines.coerceAtLeast(0))
            .joinToString("\n")
        return parseBibliographyText(normalized)
    }
}
