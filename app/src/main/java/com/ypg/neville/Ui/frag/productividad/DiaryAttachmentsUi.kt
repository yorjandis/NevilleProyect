package com.ypg.neville.ui.frag

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import com.ypg.neville.R
import com.ypg.neville.model.db.room.DiarioAttachmentEntity
import com.ypg.neville.model.db.room.DiarioAttachmentRepository
import com.ypg.neville.model.db.room.DiarioEntity
import com.ypg.neville.model.db.room.PendingDiarioAttachment
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun PendingDiaryAttachmentsEditor(
    attachments: List<PendingDiarioAttachment>,
    onAdd: (PendingDiarioAttachment) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showTitle: Boolean = true
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (showTitle) {
            Text(
                text = stringResource(R.string.diary_attachments_title, attachments.size),
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
        attachments.forEach { attachment ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(attachmentIcon(attachment.mimeType), fontSize = 18.sp)
                Text(
                    text = attachment.fileName,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
                TextButton(onClick = { onRemove(attachment.id) }, enabled = enabled) {
                    Text("×", color = Color(0xFFFF9A9A), fontSize = 22.sp)
                }
            }
        }
        DiaryAttachmentPickerButton(onPicked = onAdd, enabled = enabled)
        Text(
            text = stringResource(R.string.diary_attachments_pending_help),
            color = Color(0xFFD6DCE0),
            fontSize = 12.sp
        )
    }
}

@Composable
internal fun DiaryAttachmentsSection(
    entry: DiarioEntity,
    allEntries: List<DiarioEntity>,
    onChanged: () -> Unit,
    modifier: Modifier = Modifier,
    showAddButton: Boolean = true,
    darkTheme: Boolean = false
) {
    val context = LocalContext.current
    val repository = remember { DiarioAttachmentRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }
    var attachments by remember(entry.id) { mutableStateOf(emptyList<DiarioAttachmentEntity>()) }
    var menuFor by remember { mutableStateOf<DiarioAttachmentEntity?>(null) }
    var renameFor by remember { mutableStateOf<DiarioAttachmentEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var moveFor by remember { mutableStateOf<DiarioAttachmentEntity?>(null) }
    var moveDestination by remember { mutableStateOf<DiarioEntity?>(null) }
    var exportFor by remember { mutableStateOf<DiarioAttachmentEntity?>(null) }
    var previewFor by remember { mutableStateOf<DiarioAttachmentEntity?>(null) }
    val unknownErrorMessage = stringResource(R.string.common_unknown_error)
    val attachmentErrorTemplate = stringResource(R.string.diary_attachment_error, "%s")

    fun reportError(error: Throwable) {
        Toast.makeText(
            context,
            attachmentErrorTemplate.replace("%s", error.message ?: unknownErrorMessage),
            Toast.LENGTH_LONG
        ).show()
    }

    fun reload() {
        refresh++
        onChanged()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val attachment = exportFor
        exportFor = null
        if (uri != null && attachment != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) { repository.exportTo(attachment, uri) }
                }.onSuccess {
                    Toast.makeText(context, R.string.diary_attachment_exported, Toast.LENGTH_SHORT).show()
                }.onFailure(::reportError)
            }
        }
    }

    LaunchedEffect(entry.id, refresh) {
        attachments = withContext(Dispatchers.IO) { repository.attachmentsFor(entry.id) }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.diary_attachments_title, attachments.size),
            fontWeight = FontWeight.SemiBold,
            color = if (darkTheme) Color.White else Color(0xFF28343C)
        )

        attachments.forEach { attachment ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        previewFor = attachment
                    }
                    .background(
                        if (darkTheme) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.68f),
                        androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 10.dp)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(attachmentIcon(attachment.mimeType), fontSize = 18.sp)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        attachment.fileName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (darkTheme) Color.White else Color(0xFF202A30)
                    )
                    Text(
                        Formatter.formatShortFileSize(context, attachment.fileSize),
                        fontSize = 11.sp,
                        color = if (darkTheme) Color.White.copy(alpha = 0.65f) else Color(0xFF66757D)
                    )
                }
                IconButton(onClick = { menuFor = attachment }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.MoreHoriz,
                        contentDescription = stringResource(R.string.common_options),
                        tint = if (darkTheme) Color.White else Color(0xFF26343C),
                        modifier = Modifier
                            .size(26.dp)
                            .border(
                                width = 1.5.dp,
                                color = if (darkTheme) Color.White.copy(alpha = 0.8f) else Color(0xFF26343C),
                                shape = CircleShape
                            )
                            .padding(3.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuFor?.id == attachment.id,
                    onDismissRequest = { menuFor = null }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.diary_attachment_view)) },
                        onClick = {
                            menuFor = null
                            previewFor = attachment
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.diary_attachment_rename)) },
                        onClick = {
                            menuFor = null
                            renameFor = attachment
                            renameText = attachment.fileName
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.diary_attachment_move)) },
                        enabled = allEntries.size > 1,
                        onClick = {
                            menuFor = null
                            moveFor = attachment
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.diary_attachment_export)) },
                        onClick = {
                            menuFor = null
                            exportFor = attachment
                            exportLauncher.launch(attachment.fileName)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.common_delete), color = Color(0xFFB3261E)) },
                        onClick = {
                            menuFor = null
                            scope.launch {
                                runCatching {
                                    withContext(Dispatchers.IO) { repository.delete(attachment) }
                                }.onSuccess { reload() }.onFailure(::reportError)
                            }
                        }
                    )
                }
            }
        }

        if (showAddButton) {
            DiaryAttachmentPickerButton(
                onPicked = { pending ->
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) { repository.insertPending(entry.id, pending) }
                        }.onSuccess { reload() }.onFailure(::reportError)
                    }
                }
            )
        }
    }

    previewFor?.let { attachment ->
        DiaryAttachmentPreviewDialog(
            attachment = attachment,
            repository = repository,
            onDismiss = { previewFor = null },
            onSaved = {
                previewFor = null
                reload()
            }
        )
    }

    renameFor?.let { attachment ->
        AlertDialog(
            onDismissRequest = { renameFor = null },
            title = { Text(stringResource(R.string.diary_attachment_rename)) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.diary_attachment_file_name)) }
                )
            },
            dismissButton = {
                TextButton(onClick = { renameFor = null }) { Text(stringResource(R.string.common_cancel)) }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) { repository.rename(attachment, renameText) }
                        }.onSuccess {
                            renameFor = null
                            reload()
                        }.onFailure(::reportError)
                    }
                }) { Text(stringResource(R.string.common_save)) }
            }
        )
    }

    moveFor?.let { attachment ->
        AlertDialog(
            onDismissRequest = { moveFor = null },
            title = { Text(stringResource(R.string.diary_attachment_move)) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(allEntries.filter { it.id != entry.id }, key = { it.id }) { destination ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { moveDestination = destination }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(destination.title.ifBlank { stringResource(R.string.diary_untitled_entry) }, fontWeight = FontWeight.SemiBold)
                            if (destination.content.isNotBlank()) {
                                Text(destination.content, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { moveFor = null }) { Text(stringResource(R.string.common_cancel)) }
            }
        )

        moveDestination?.let { destination ->
            AlertDialog(
                onDismissRequest = { moveDestination = null },
                title = { Text(stringResource(R.string.diary_attachment_confirm_move)) },
                text = {
                    Text(stringResource(R.string.diary_attachment_move_question, attachment.fileName, destination.title))
                },
                dismissButton = {
                    TextButton(onClick = { moveDestination = null }) { Text(stringResource(R.string.common_cancel)) }
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            runCatching {
                                withContext(Dispatchers.IO) { repository.move(attachment, destination.id) }
                            }.onSuccess {
                                moveDestination = null
                                moveFor = null
                                reload()
                            }.onFailure(::reportError)
                        }
                    }) { Text(stringResource(R.string.diary_attachment_move_action)) }
                }
            )
        }
    }
}

@Composable
private fun DiaryAttachmentPickerButton(
    onPicked: (PendingDiarioAttachment) -> Unit,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val repository = remember { DiarioAttachmentRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraFile by remember { mutableStateOf<File?>(null) }
    val unknownErrorMessage = stringResource(R.string.common_unknown_error)
    val attachmentErrorTemplate = stringResource(R.string.diary_attachment_error, "%s")
    val cameraUnavailableMessage = stringResource(R.string.diary_attachment_camera_unavailable)
    val cameraPermissionDeniedMessage = stringResource(R.string.diary_attachment_camera_permission_denied)
    val cameraAvailable = remember {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).resolveActivity(context.packageManager) != null
    }

    fun showError(error: Throwable) {
        Toast.makeText(
            context,
            attachmentErrorTemplate.replace("%s", error.message ?: unknownErrorMessage),
            Toast.LENGTH_LONG
        ).show()
    }

    fun load(uris: List<Uri>, afterLoad: () -> Unit = {}) {
        if (uris.isEmpty()) return
        scope.launch {
            try {
                uris.forEach { uri ->
                    runCatching {
                        withContext(Dispatchers.IO) { repository.readPending(uri) }
                    }.onSuccess(onPicked).onFailure(::showError)
                }
            } finally {
                afterLoad()
            }
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { load(it) }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { load(it) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = cameraUri
        val file = cameraFile
        if (success && uri != null) {
            load(listOf(uri)) { file?.delete() }
        } else {
            file?.delete()
        }
        cameraUri = null
        cameraFile = null
    }

    fun launchCameraCapture() {
        runCatching {
            val directory = File(context.cacheDir, "diary_attachment_camera")
            check(directory.exists() || directory.mkdirs()) {
                "No se pudo preparar la carpeta temporal de la cámara"
            }
            val file = File.createTempFile("Foto-", ".jpg", directory)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            cameraFile = file
            cameraUri = uri
            cameraLauncher.launch(uri)
        }.onFailure { error ->
            cameraFile?.delete()
            cameraFile = null
            cameraUri = null
            showError(error)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCameraCapture()
        } else {
            Toast.makeText(context, cameraPermissionDeniedMessage, Toast.LENGTH_LONG).show()
        }
    }

    Column {
        Button(onClick = { menuExpanded = true }, enabled = enabled) {
            Text("📎  ${stringResource(R.string.diary_attachment_add)}")
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.diary_attachment_file)) },
                onClick = {
                    menuExpanded = false
                    fileLauncher.launch(arrayOf("*/*"))
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.diary_attachment_photos)) },
                onClick = {
                    menuExpanded = false
                    photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.diary_attachment_camera)) },
                enabled = cameraAvailable,
                onClick = {
                    menuExpanded = false
                    if (!cameraAvailable) {
                        Toast.makeText(context, cameraUnavailableMessage, Toast.LENGTH_LONG).show()
                        return@DropdownMenuItem
                    }
                    if (
                        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        launchCameraCapture()
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            )
        }
    }
}

private fun attachmentIcon(mimeType: String): String = when {
    mimeType.startsWith("image/") -> "🖼️"
    mimeType == "application/pdf" -> "📕"
    mimeType.startsWith("text/") -> "📄"
    mimeType.contains("spreadsheet") || mimeType.contains("excel") -> "📊"
    mimeType.contains("presentation") || mimeType.contains("powerpoint") -> "📽️"
    else -> "📎"
}
