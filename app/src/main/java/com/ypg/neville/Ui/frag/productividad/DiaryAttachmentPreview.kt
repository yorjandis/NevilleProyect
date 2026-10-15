package com.ypg.neville.ui.frag

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.RotateLeft
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ypg.neville.R
import com.ypg.neville.model.db.room.DiarioAttachmentEntity
import com.ypg.neville.model.db.room.DiarioAttachmentRepository
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun DiaryAttachmentPreviewDialog(
    attachment: DiarioAttachmentEntity,
    repository: DiarioAttachmentRepository,
    onDismiss: () -> Unit,
    onSaved: (DiarioAttachmentEntity) -> Unit
) {
    var clearData by remember(attachment.id) { mutableStateOf<ByteArray?>(null) }
    var errorMessage by remember(attachment.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(attachment.id) {
        runCatching {
            withContext(Dispatchers.IO) { repository.decryptedData(attachment) }
        }.onSuccess { clearData = it }
            .onFailure { errorMessage = it.message }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        when {
            clearData == null && errorMessage == null -> PreviewLoadingScreen(attachment.fileName, onDismiss)
            errorMessage != null -> PreviewErrorScreen(attachment.fileName, errorMessage.orEmpty(), onDismiss)
            attachment.mimeType.startsWith("image/") -> DiaryImageEditor(
                attachment = attachment,
                data = clearData!!,
                repository = repository,
                onDismiss = onDismiss,
                onSaved = onSaved
            )
            attachment.mimeType.equals("application/pdf", ignoreCase = true) ||
                attachment.fileName.endsWith(".pdf", ignoreCase = true) -> DiaryPdfViewer(
                attachment = attachment,
                data = clearData!!,
                onDismiss = onDismiss
            )
            else -> GenericAttachmentPreview(
                attachment = attachment,
                repository = repository,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun PreviewTopBar(
    title: String,
    onClose: () -> Unit,
    saveEnabled: Boolean = false,
    saving: Boolean = false,
    onSave: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF121212))
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.common_close), tint = Color.White)
        }
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (saveEnabled) {
            TextButton(onClick = onSave, enabled = !saving) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Text(stringResource(R.string.common_save), modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun PreviewLoadingScreen(title: String, onDismiss: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFF101010))) {
        PreviewTopBar(title, onDismiss)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
    }
}

@Composable
private fun PreviewErrorScreen(title: String, message: String, onDismiss: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFF101010))) {
        PreviewTopBar(title, onDismiss)
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.diary_attachment_preview_error, message),
                color = Color.White
            )
        }
    }
}

private enum class DiaryCropPreset(val label: String) {
    ORIGINAL("Original"),
    SQUARE("1:1"),
    FOUR_THREE("4:3"),
    SIXTEEN_NINE("16:9"),
    MANUAL("Guías")
}

private data class DiaryImageEdits(
    val quarterTurns: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val cropPreset: DiaryCropPreset = DiaryCropPreset.ORIGINAL,
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f
)

private enum class ImageEditorPanel { CROP, LIGHT }

@Composable
private fun DiaryImageEditor(
    attachment: DiarioAttachmentEntity,
    data: ByteArray,
    repository: DiarioAttachmentRepository,
    onDismiss: () -> Unit,
    onSaved: (DiarioAttachmentEntity) -> Unit
) {
    val scope = rememberCoroutineScope()
    val source = remember(data) { BitmapFactory.decodeByteArray(data, 0, data.size) }
    var edits by remember(attachment.id) { mutableStateOf(DiaryImageEdits()) }
    var undoStack by remember(attachment.id) { mutableStateOf(emptyList<DiaryImageEdits>()) }
    var redoStack by remember(attachment.id) { mutableStateOf(emptyList<DiaryImageEdits>()) }
    var activePanel by remember { mutableStateOf<ImageEditorPanel?>(null) }
    var preview by remember(source) { mutableStateOf(source) }
    var rendering by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var showDiscardConfirmation by remember { mutableStateOf(false) }
    var continuousBaseline by remember { mutableStateOf<DiaryImageEdits?>(null) }

    fun applyEdit(change: (DiaryImageEdits) -> DiaryImageEdits) {
        val next = change(edits)
        if (next == edits) return
        undoStack = undoStack + edits
        redoStack = emptyList()
        edits = next
    }

    fun updateContinuous(change: (DiaryImageEdits) -> DiaryImageEdits) {
        if (continuousBaseline == null) continuousBaseline = edits
        edits = change(edits)
    }

    fun finishContinuous() {
        val baseline = continuousBaseline
        continuousBaseline = null
        if (baseline != null && baseline != edits) {
            undoStack = undoStack + baseline
            redoStack = emptyList()
        }
    }

    fun requestClose() {
        if (edits == DiaryImageEdits()) onDismiss() else showDiscardConfirmation = true
    }

    LaunchedEffect(source, edits) {
        if (source == null) return@LaunchedEffect
        rendering = true
        preview = withContext(Dispatchers.Default) { renderDiaryImage(source, edits) }
        rendering = false
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        PreviewTopBar(
            title = attachment.fileName,
            onClose = ::requestClose,
            saveEnabled = edits != DiaryImageEdits(),
            saving = saving,
            onSave = {
                val editedBitmap = preview ?: return@PreviewTopBar
                saving = true
                val targetMime = if (attachment.mimeType.equals("image/png", true)) "image/png" else "image/jpeg"
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            val bytes = encodeDiaryImage(editedBitmap, targetMime)
                            repository.replaceData(attachment, bytes, targetMime)
                        }
                    }.onSuccess { updated ->
                        edits = DiaryImageEdits()
                        undoStack = emptyList()
                        redoStack = emptyList()
                        activePanel = null
                        onSaved(updated)
                    }.onFailure { saveError = it.message }
                    saving = false
                }
            }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (source == null) {
                Text(stringResource(R.string.diary_attachment_invalid_image), color = Color.White)
            } else if (preview != null) {
                Image(
                    bitmap = preview!!.asImageBitmap(),
                    contentDescription = attachment.fileName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                )
            }
            if (rendering) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .padding(10.dp)
                        .size(28.dp),
                    strokeWidth = 2.dp
                )
            }
        }

        when (activePanel) {
            ImageEditorPanel.CROP -> CropAdjustmentPanel(
                edits = edits,
                onPreset = { preset -> applyEdit { it.copy(cropPreset = preset) } },
                onContinuousChange = ::updateContinuous,
                onContinuousFinished = ::finishContinuous
            )
            ImageEditorPanel.LIGHT -> LightAdjustmentPanel(
                edits = edits,
                onContinuousChange = ::updateContinuous,
                onContinuousFinished = ::finishContinuous,
                onReset = { applyEdit { it.copy(brightness = 0f, contrast = 1f) } }
            )
            null -> Unit
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .background(Color(0xFF171717))
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EditorToolButton(Icons.AutoMirrored.Rounded.Undo, stringResource(R.string.diary_image_undo), undoStack.isNotEmpty()) {
                val previous = undoStack.lastOrNull() ?: return@EditorToolButton
                redoStack = redoStack + edits
                undoStack = undoStack.dropLast(1)
                edits = previous
            }
            EditorToolButton(Icons.AutoMirrored.Rounded.Redo, stringResource(R.string.diary_image_redo), redoStack.isNotEmpty()) {
                val next = redoStack.lastOrNull() ?: return@EditorToolButton
                undoStack = undoStack + edits
                redoStack = redoStack.dropLast(1)
                edits = next
            }
            EditorToolButton(Icons.Rounded.Crop, stringResource(R.string.diary_image_crop)) {
                activePanel = if (activePanel == ImageEditorPanel.CROP) null else ImageEditorPanel.CROP
            }
            EditorToolButton(Icons.AutoMirrored.Rounded.RotateLeft, stringResource(R.string.diary_image_rotate_left)) {
                applyEdit { it.copy(quarterTurns = it.quarterTurns - 1) }
            }
            EditorToolButton(Icons.AutoMirrored.Rounded.RotateRight, stringResource(R.string.diary_image_rotate_right)) {
                applyEdit { it.copy(quarterTurns = it.quarterTurns + 1) }
            }
            TextToolButton("⇄", stringResource(R.string.diary_image_flip_horizontal)) {
                applyEdit { it.copy(flipHorizontal = !it.flipHorizontal) }
            }
            TextToolButton("⇅", stringResource(R.string.diary_image_flip_vertical)) {
                applyEdit { it.copy(flipVertical = !it.flipVertical) }
            }
            EditorToolButton(Icons.Rounded.Tune, stringResource(R.string.diary_image_light)) {
                activePanel = if (activePanel == ImageEditorPanel.LIGHT) null else ImageEditorPanel.LIGHT
            }
        }
    }

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text(stringResource(R.string.diary_image_discard_title)) },
            text = { Text(stringResource(R.string.diary_image_discard_message)) },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmation = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.diary_image_discard_action), color = Color(0xFFB3261E))
                }
            }
        )
    }

    saveError?.let { message ->
        AlertDialog(
            onDismissRequest = { saveError = null },
            title = { Text(stringResource(R.string.diary_image_save_error_title)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { saveError = null }) { Text(stringResource(R.string.common_accept)) }
            }
        )
    }
}

@Composable
private fun EditorToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun TextToolButton(symbol: String, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(Color.White.copy(alpha = 0.1f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CropAdjustmentPanel(
    edits: DiaryImageEdits,
    onPreset: (DiaryCropPreset) -> Unit,
    onContinuousChange: ((DiaryImageEdits) -> DiaryImageEdits) -> Unit,
    onContinuousFinished: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xEE242424))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.diary_image_crop), color = Color.White, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DiaryCropPreset.entries.forEach { preset ->
                Text(
                    text = preset.label,
                    color = if (edits.cropPreset == preset) Color.Black else Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (edits.cropPreset == preset) Color(0xFFFFB74D) else Color.White.copy(alpha = 0.12f))
                        .clickable { onPreset(preset) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
        if (edits.cropPreset == DiaryCropPreset.MANUAL) {
            CropSlider(stringResource(R.string.diary_image_crop_left), edits.cropLeft, 0f..max(0f, edits.cropRight - 0.08f), {
                onContinuousChange { current -> current.copy(cropLeft = it) }
            }, onContinuousFinished)
            CropSlider(stringResource(R.string.diary_image_crop_right), edits.cropRight, min(1f, edits.cropLeft + 0.08f)..1f, {
                onContinuousChange { current -> current.copy(cropRight = it) }
            }, onContinuousFinished)
            CropSlider(stringResource(R.string.diary_image_crop_top), edits.cropTop, 0f..max(0f, edits.cropBottom - 0.08f), {
                onContinuousChange { current -> current.copy(cropTop = it) }
            }, onContinuousFinished)
            CropSlider(stringResource(R.string.diary_image_crop_bottom), edits.cropBottom, min(1f, edits.cropTop + 0.08f)..1f, {
                onContinuousChange { current -> current.copy(cropBottom = it) }
            }, onContinuousFinished)
        }
    }
}

@Composable
private fun CropSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onFinished: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp, modifier = Modifier.weight(0.25f))
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            onValueChangeFinished = onFinished,
            modifier = Modifier.weight(0.75f)
        )
    }
}

@Composable
private fun LightAdjustmentPanel(
    edits: DiaryImageEdits,
    onContinuousChange: ((DiaryImageEdits) -> DiaryImageEdits) -> Unit,
    onContinuousFinished: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Color(0xEE242424)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(stringResource(R.string.diary_image_light), color = Color.White, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.diary_image_brightness), color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
        Slider(
            value = edits.brightness,
            onValueChange = { value -> onContinuousChange { it.copy(brightness = value) } },
            valueRange = -0.5f..0.5f,
            onValueChangeFinished = onContinuousFinished
        )
        Text(stringResource(R.string.diary_image_contrast), color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
        Slider(
            value = edits.contrast,
            onValueChange = { value -> onContinuousChange { it.copy(contrast = value) } },
            valueRange = 0.5f..1.5f,
            onValueChangeFinished = onContinuousFinished
        )
        TextButton(onClick = onReset) { Text(stringResource(R.string.diary_image_reset_light)) }
    }
}

private fun renderDiaryImage(source: Bitmap, edits: DiaryImageEdits): Bitmap {
    val transform = Matrix().apply {
        postScale(if (edits.flipHorizontal) -1f else 1f, if (edits.flipVertical) -1f else 1f)
        postRotate((((edits.quarterTurns % 4) + 4) % 4) * 90f)
    }
    val transformed = Bitmap.createBitmap(source, 0, 0, source.width, source.height, transform, true)
    val crop = cropRect(transformed.width, transformed.height, edits)
    val cropped = if (crop.first == 0 && crop.second == 0 && crop.third == transformed.width && crop.fourth == transformed.height) {
        transformed
    } else {
        Bitmap.createBitmap(transformed, crop.first, crop.second, crop.third, crop.fourth)
    }

    if (edits.brightness == 0f && edits.contrast == 1f) return cropped
    val output = Bitmap.createBitmap(cropped.width, cropped.height, Bitmap.Config.ARGB_8888)
    val offset = edits.brightness * 255f + (1f - edits.contrast) * 128f
    val matrix = ColorMatrix(
        floatArrayOf(
            edits.contrast, 0f, 0f, 0f, offset,
            0f, edits.contrast, 0f, 0f, offset,
            0f, 0f, edits.contrast, 0f, offset,
            0f, 0f, 0f, 1f, 0f
        )
    )
    Canvas(output).drawBitmap(cropped, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(matrix)
    })
    return output
}

private fun cropRect(width: Int, height: Int, edits: DiaryImageEdits): IntRect {
    if (edits.cropPreset == DiaryCropPreset.ORIGINAL) return IntRect(0, 0, width, height)
    if (edits.cropPreset == DiaryCropPreset.MANUAL) {
        val left = (edits.cropLeft * width).toInt().coerceIn(0, width - 1)
        val top = (edits.cropTop * height).toInt().coerceIn(0, height - 1)
        val right = (edits.cropRight * width).toInt().coerceIn(left + 1, width)
        val bottom = (edits.cropBottom * height).toInt().coerceIn(top + 1, height)
        return IntRect(left, top, right - left, bottom - top)
    }
    val ratio = when (edits.cropPreset) {
        DiaryCropPreset.SQUARE -> 1f
        DiaryCropPreset.FOUR_THREE -> 4f / 3f
        DiaryCropPreset.SIXTEEN_NINE -> 16f / 9f
        DiaryCropPreset.ORIGINAL, DiaryCropPreset.MANUAL -> width.toFloat() / height
    }
    val current = width.toFloat() / height
    return if (current > ratio) {
        val cropWidth = max(1, (height * ratio).toInt())
        IntRect((width - cropWidth) / 2, 0, cropWidth, height)
    } else {
        val cropHeight = max(1, (width / ratio).toInt())
        IntRect(0, (height - cropHeight) / 2, width, cropHeight)
    }
}

private data class IntRect(val first: Int, val second: Int, val third: Int, val fourth: Int)

private fun encodeDiaryImage(bitmap: Bitmap, mimeType: String): ByteArray =
    ByteArrayOutputStream().use { output ->
        val format = if (mimeType == "image/png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        check(bitmap.compress(format, 92, output)) { "No se pudo codificar la imagen editada" }
        output.toByteArray()
    }

@Composable
private fun DiaryPdfViewer(
    attachment: DiarioAttachmentEntity,
    data: ByteArray,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var pdfFile by remember(attachment.id) { mutableStateOf<File?>(null) }
    var pageCount by remember(attachment.id) { mutableStateOf(0) }
    var error by remember(attachment.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(attachment.id, data) {
        runCatching {
            withContext(Dispatchers.IO) {
                val directory = File(context.cacheDir, "diary_attachments/pdf").apply { mkdirs() }
                val file = File(directory, "${attachment.id}.pdf").apply { writeBytes(data) }
                val count = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                    PdfRenderer(descriptor).use { it.pageCount }
                }
                file to count
            }
        }.onSuccess { (file, count) ->
            pdfFile = file
            pageCount = count
        }.onFailure { error = it.message }
    }

    DisposableEffect(pdfFile) {
        onDispose { pdfFile?.delete() }
    }

    Column(Modifier.fillMaxSize().background(Color(0xFF2B2B2B))) {
        PreviewTopBar(attachment.fileName, onDismiss)
        when {
            error != null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.diary_attachment_preview_error, error.orEmpty()), color = Color.White)
            }
            pdfFile == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items((0 until pageCount).toList(), key = { it }) { pageIndex ->
                    PdfPage(file = pdfFile!!, pageIndex = pageIndex)
                }
            }
        }
    }
}

@Composable
private fun PdfPage(file: File, pageIndex: Int) {
    var bitmap by remember(file.path, pageIndex) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(file.path, pageIndex) {
        bitmap = withContext(Dispatchers.IO) {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    renderer.openPage(pageIndex).use { page ->
                        val targetWidth = min(1_400, max(1, page.width * 2))
                        val targetHeight = max(1, (targetWidth * (page.height.toFloat() / page.width)).toInt())
                        Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888).also { target ->
                            target.eraseColor(android.graphics.Color.WHITE)
                            page.render(target, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        val pageBitmap = bitmap
        if (pageBitmap == null) {
            Box(Modifier.fillMaxWidth().height(420.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Image(
                bitmap = pageBitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.diary_pdf_page, pageIndex + 1),
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun GenericAttachmentPreview(
    attachment: DiarioAttachmentEntity,
    repository: DiarioAttachmentRepository,
    onDismiss: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(Color(0xFF101010))) {
        PreviewTopBar(attachment.fileName, onDismiss)
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📎", fontSize = 56.sp)
            Spacer(Modifier.height(16.dp))
            Text(attachment.fileName, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { repository.openAttachment(attachment) }) {
                Text(stringResource(R.string.diary_attachment_open_external))
            }
        }
    }
}
