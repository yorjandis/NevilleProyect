package com.ypg.neville.ui.frag

import android.content.Context

data class AccessCardPlaceholder(
    val title: String,
    val primaryButton: String,
    val primaryAssetPath: String,
    val secondaryButton: String? = null,
    val secondaryAssetPath: String? = null,
    val menuItems: List<AccessCardMenuItem> = emptyList()
)

data class AccessCardMenuItem(
    val title: String,
    val assetPath: String
)

private fun formatBookTitleFromFolderName(rawName: String): String {
    val normalized = rawName
        .replace('_', ' ')
        .replace('-', ' ')
        .replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
        .replace(Regex("([A-Z])([A-Z][a-z])"), "$1 $2")
        .trim()

    return normalized
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString(" ") { token ->
            token.lowercase().replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase() else ch.toString()
            }
        }
}

fun loadAuthorBiographyAssetPath(context: Context, authorAssetsFolder: String): String? {
    val biographyFolder = "$authorAssetsFolder/biografia"
    val files = runCatching { context.assets.list(biographyFolder).orEmpty() }.getOrDefault(emptyArray())
    return files
        .filter { it.endsWith(".txt", ignoreCase = true) }
        .sorted()
        .firstOrNull()
        ?.let { "$biographyFolder/$it" }
}

fun loadAuthorTeachingSummaryAssetPath(context: Context, authorAssetsFolder: String): String? {
    val children = runCatching { context.assets.list(authorAssetsFolder).orEmpty() }.getOrDefault(emptyArray())
    val summaryFolder = children
        .sorted()
        .firstOrNull { it.startsWith("resumenEnseñanza", ignoreCase = true) }
        ?.let { "$authorAssetsFolder/$it" }
        ?: return null

    fun findFirstTxt(folder: String): String? {
        val entries = runCatching { context.assets.list(folder).orEmpty() }.getOrDefault(emptyArray())
        if (entries.isEmpty()) return null

        entries.sorted().forEach { entry ->
            if (entry.isBlank() || entry == ".DS_Store") return@forEach
            val path = "$folder/$entry"
            val nested = runCatching { context.assets.list(path).orEmpty() }.getOrDefault(emptyArray())
            if (nested.isNotEmpty()) {
                findFirstTxt(path)?.let { return it }
            } else if (entry.endsWith(".txt", ignoreCase = true)) {
                return path
            }
        }
        return null
    }

    return findFirstTxt(summaryFolder)
}

fun loadAuthorResourceCards(context: Context, authorAssetsFolder: String): List<AccessCardPlaceholder> {
    val resourcesByBook = linkedMapOf<String, MutableMap<String, String>>()
    val librosRoot = "$authorAssetsFolder/libros"

    fun visit(folder: String) {
        val children = runCatching { context.assets.list(folder).orEmpty() }.getOrDefault(emptyArray())
        if (children.isEmpty()) return

        children.forEach { child ->
            if (child.isBlank() || child == ".DS_Store") return@forEach
            val childPath = "$folder/$child"
            val nested = runCatching { context.assets.list(childPath).orEmpty() }.getOrDefault(emptyArray())
            if (nested.isNotEmpty()) {
                visit(childPath)
                return@forEach
            }
            if (!child.endsWith(".txt", ignoreCase = true)) return@forEach

            val cleanName = child.removeSuffix(".txt")
            val type = when {
                cleanName.startsWith("resumen_libro_") -> "resumen"
                cleanName.startsWith("plan_libro_") -> "plan"
                else -> null
            } ?: return@forEach

            val bookName = childPath
                .substringBeforeLast('/')
                .substringAfterLast('/')
                .trim()
            if (bookName.isBlank()) return@forEach

            val bookResources = resourcesByBook.getOrPut(bookName) { mutableMapOf() }
            bookResources[type] = childPath
        }
    }

    visit(librosRoot)

    return resourcesByBook
        .toSortedMap()
        .mapNotNull { (bookName, resources) ->
            val readableTitle = formatBookTitleFromFolderName(bookName)
            val resumen = resources["resumen"]
            val plan = resources["plan"]
            when {
                !resumen.isNullOrBlank() && !plan.isNullOrBlank() -> {
                    AccessCardPlaceholder(
                        title = readableTitle,
                        primaryButton = "Resumen",
                        primaryAssetPath = resumen,
                        secondaryButton = "Plan",
                        secondaryAssetPath = plan
                    )
                }

                !resumen.isNullOrBlank() -> {
                    AccessCardPlaceholder(
                        title = readableTitle,
                        primaryButton = "Resumen",
                        primaryAssetPath = resumen
                    )
                }

                !plan.isNullOrBlank() -> {
                    AccessCardPlaceholder(
                        title = readableTitle,
                        primaryButton = "Plan",
                        primaryAssetPath = plan
                    )
                }

                else -> null
            }
        }
}

fun loadBruceEvolutionSeriesCard(context: Context): AccessCardPlaceholder? {
    val folder = "autores/bruceLipton/materialBruce/serieEvolucionInterior"
    val files = runCatching { context.assets.list(folder).orEmpty() }.getOrDefault(emptyArray())
        .filter { it.endsWith(".txt", ignoreCase = true) }
        .sortedBy { file ->
            val number = file
                .removeSuffix(".txt")
                .substringAfterLast('_', "")
                .toIntOrNull()
            number ?: Int.MAX_VALUE
        }

    if (files.isEmpty()) return null

    val menuItems = files.mapIndexed { index, file ->
        AccessCardMenuItem(
            title = "Capítulo ${index + 1}",
            assetPath = "$folder/$file"
        )
    }

    return AccessCardPlaceholder(
        title = "Resumen Serie Evolución Interior",
        primaryButton = "Capítulos",
        primaryAssetPath = menuItems.first().assetPath,
        menuItems = menuItems
    )
}
