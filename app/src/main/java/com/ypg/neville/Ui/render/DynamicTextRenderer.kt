package com.ypg.neville.ui.render

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme


data class RenderStyle(
    val fontSizeSp: Float = 18f,
    val textColor: Color = Color.Black,
    val paragraphSpacingDp: Float = 16f
)

@Composable
fun DynamicTextRenderer(
    blocks: List<ContentBlock>,
    style: RenderStyle = RenderStyle(),
    onRelatedItemClick: (RelatedItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val processedBlocks = remember(blocks) { blocks.expandedTextBlocks() }

    SelectionContainer {
        LazyColumn(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(style.paragraphSpacingDp.dp)
        ) {
            items(processedBlocks, key = { it.id }) { block ->
                BlockRenderer(
                    block = block,
                    style = style,
                    onRelatedItemClick = onRelatedItemClick
                )
            }
        }
    }
}

@Composable
private fun BlockRenderer(
    block: ContentBlock,
    style: RenderStyle,
    onRelatedItemClick: (RelatedItem) -> Unit
) {
    val uriHandler = LocalUriHandler.current

    when (val content = block.content) {
        is BlockType.Text -> {
            Text(
                text = content.value,
                color = style.textColor,
                style = TextStyle(fontSize = style.fontSizeSp.sp)
            )
        }

        is BlockType.Markdown -> {
            Text(
                text = content.value,
                color = style.textColor,
                style = TextStyle(fontSize = style.fontSizeSp.sp)
            )
        }

        is BlockType.ImageLocal -> {
            AssetImageBlock(
                assetPath = content.assetPath,
                sizeDp = content.sizeDp
            )
        }

        is BlockType.ImageRemote -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Imagen remota",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                DisableSelection {
                    TextButton(onClick = { uriHandler.openUri(content.url) }) {
                        Text(text = content.url)
                    }
                }
            }
        }

        is BlockType.Link -> {
            DisableSelection {
                TextButton(onClick = { uriHandler.openUri(content.url) }) {
                    Text(text = content.title)
                }
            }
        }

        is BlockType.BulletList -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                content.items.forEach { item ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text(text = "• ", color = style.textColor)
                        Text(
                            text = item,
                            color = style.textColor,
                            style = TextStyle(fontSize = style.fontSizeSp.sp)
                        )
                    }
                }
            }
        }

        is BlockType.Quote -> {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(56.dp)
                        .background(Color.Gray.copy(alpha = 0.4f))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = content.value,
                    style = TextStyle(fontSize = style.fontSizeSp.sp, fontStyle = FontStyle.Italic),
                    color = Color.Gray
                )
            }
        }

        is BlockType.Code -> {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = content.value,
                    fontFamily = FontFamily.Monospace,
                    style = TextStyle(fontSize = (style.fontSizeSp - 1f).coerceAtLeast(12f).sp),
                    color = style.textColor
                )
            }
        }

        is BlockType.Divider -> HorizontalDivider()

        is BlockType.Bibliography -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF7F7F7), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                content.title?.let {
                    Text(text = it, style = MaterialTheme.typography.titleMedium)
                }

                content.entries.forEachIndexed { index, entry ->
                    Row(verticalAlignment = Alignment.Top) {
                        val idx = "[${index + 1}]"
                        if (!entry.url.isNullOrBlank()) {
                            DisableSelection {
                                Text(
                                    text = idx,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { uriHandler.openUri(entry.url) }
                                )
                            }
                        } else {
                            Text(text = idx, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = entry.text,
                            color = style.textColor,
                            style = TextStyle(fontSize = (style.fontSizeSp - 2f).coerceAtLeast(12f).sp)
                        )
                    }
                }
            }
        }

        is BlockType.Related -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF7F7F7), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content.title?.let {
                    Text(text = it, style = MaterialTheme.typography.titleSmall)
                }

                content.items.forEach { item ->
                    DisableSelection {
                        Text(
                            text = "• ${item.displayName}",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onRelatedItemClick(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetImageBlock(assetPath: String, sizeDp: Float) {
    val context = LocalContext.current
    val bitmap = remember(assetPath) {
        runCatching {
            context.assets.open(assetPath).use { input ->
                BitmapFactory.decodeStream(input)
            }
        }.getOrNull()
    }

    if (bitmap == null) {
        Text(
            text = "No se pudo cargar imagen: $assetPath",
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall
        )
        return
    }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier.size(sizeDp.dp)
    )
}
