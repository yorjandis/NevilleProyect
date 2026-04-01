package com.ypg.neville.ui.render

import java.util.UUID

// Bloque base de contenido, equivalente al modelo Swift.
data class ContentBlock(
    val id: String = UUID.randomUUID().toString(),
    val content: BlockType
)

sealed class BlockType {
    data class Text(val value: String) : BlockType()
    data class Markdown(val value: String) : BlockType()
    data class ImageLocal(val assetPath: String, val sizeDp: Float = 220f) : BlockType()
    data class ImageRemote(val url: String, val sizeDp: Float = 220f) : BlockType()
    data class Link(val title: String, val url: String) : BlockType()
    data class BulletList(val items: List<String>) : BlockType()
    data class Quote(val value: String) : BlockType()
    data class Code(val value: String) : BlockType()
    data class Bibliography(val title: String?, val entries: List<BibliographyEntry>) : BlockType()
    data class Related(val title: String?, val items: List<RelatedItem>) : BlockType()
    data object Divider : BlockType()
}

data class RelatedItem(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val displayName: String
)

data class BibliographyEntry(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val url: String?
)

fun String.splitIntoParagraphs(): List<String> {
    return this
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toList()
}

fun List<ContentBlock>.expandedTextBlocks(): List<ContentBlock> {
    return flatMap { block ->
        when (val content = block.content) {
            is BlockType.Text -> {
                content.value
                    .splitIntoParagraphs()
                    .map { paragraph -> ContentBlock(content = BlockType.Text(paragraph)) }
            }
            else -> listOf(block)
        }
    }
}
