package com.ypg.neville.feature.translation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ypg.neville.R
import com.ypg.neville.feature.translation.data.MlKitOnDeviceTranslationService
import com.ypg.neville.feature.translation.domain.OnDeviceTranslationService
import com.ypg.neville.feature.translation.domain.TranslationLanguage
import com.ypg.neville.feature.translation.domain.UnsupportedSourceLanguageException
import com.ypg.neville.model.db.room.NotaChecklistCodec
import com.ypg.neville.model.db.room.NotaChecklistItem
import com.ypg.neville.model.db.room.NotaEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class NoteTranslationDraft(
    val body: String,
    val checklistItems: List<NotaChecklistItem>
)

private enum class NoteSourceLanguage(val language: TranslationLanguage?) {
    AUTOMATIC(null),
    SPANISH(TranslationLanguage.SPANISH),
    ENGLISH(TranslationLanguage.ENGLISH),
    SIMPLIFIED_CHINESE(TranslationLanguage.SIMPLIFIED_CHINESE)
}

@Composable
fun NoteTranslationDialog(
    note: NotaEntity,
    onDismiss: () -> Unit,
    onReplace: (NoteTranslationDraft) -> Unit,
    onCreateCopy: (NoteTranslationDraft) -> Unit
) {
    val translationService = remember<OnDeviceTranslationService> {
        MlKitOnDeviceTranslationService()
    }
    DisposableEffect(translationService) {
        onDispose(translationService::close)
    }

    val originalChecklistItems = remember(note.id, note.checklistJson) {
        NotaChecklistCodec.decode(note.checklistJson)
    }
    var body by remember(note.id) { mutableStateOf(note.nota) }
    var checklistItems by remember(note.id) { mutableStateOf(originalChecklistItems) }
    var sourceLanguage by remember(note.id) { mutableStateOf(NoteSourceLanguage.AUTOMATIC) }
    var sourceMenuExpanded by remember { mutableStateOf(false) }
    var actionsExpanded by remember { mutableStateOf(false) }
    var detectedLanguage by remember { mutableStateOf<TranslationLanguage?>(null) }
    var translating by remember { mutableStateOf(false) }
    var machineTranslated by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val checklistText = remember(checklistItems) { renderChecklist(checklistItems) }
    val detectionText = remember(body, checklistItems) {
        buildList {
            if (body.isNotBlank()) add(body)
            addAll(checklistItems.map(NotaChecklistItem::text).filter(String::isNotBlank))
        }.joinToString("\n")
    }
    val unsupportedSourceMessage = stringResource(R.string.notes_translation_source_unsupported)
    val translationFailedMessage = stringResource(R.string.notes_translation_failed)
    val sameLanguageMessage = stringResource(R.string.notes_translation_same_language)

    LaunchedEffect(sourceLanguage, detectionText) {
        if (sourceLanguage != NoteSourceLanguage.AUTOMATIC || detectionText.isBlank()) {
            detectedLanguage = sourceLanguage.language
            return@LaunchedEffect
        }
        delay(250)
        detectedLanguage = runCatching {
            translationService.identifyLanguage(detectionText)
        }.getOrNull()
    }

    fun currentDraft(): NoteTranslationDraft = NoteTranslationDraft(
        body = body.trim(),
        checklistItems = checklistItems
            .map { it.copy(text = it.text.trim()) }
            .filter { it.text.isNotBlank() }
    )

    fun requestTranslation(target: TranslationLanguage) {
        if (detectionText.isBlank() || translating) return
        coroutineScope.launch {
            translating = true
            try {
                val resolvedSource = sourceLanguage.language
                    ?: detectedLanguage
                    ?: translationService.identifyLanguage(detectionText)
                detectedLanguage = resolvedSource
                if (resolvedSource == target) {
                    statusMessage = sameLanguageMessage
                    return@launch
                }

                val segments = buildList {
                    add(body)
                    addAll(checklistItems.map(NotaChecklistItem::text))
                }
                val result = translationService.translateBatch(
                    texts = segments,
                    targetLanguage = target,
                    sourceLanguage = resolvedSource
                )
                body = result.texts.first()
                checklistItems = checklistItems.mapIndexed { index, item ->
                    item.copy(text = result.texts[index + 1])
                }
                machineTranslated = result.wasTranslated
            } catch (_: UnsupportedSourceLanguageException) {
                statusMessage = unsupportedSourceMessage
            } catch (_: Exception) {
                statusMessage = translationFailedMessage
            } finally {
                translating = false
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!translating) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp
        ) {
            Box(
                modifier = Modifier.background(
                    Brush.linearGradient(
                        listOf(Color(0xFFFFE5C0), Color(0xFFCFE8D2))
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 86.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.notes_translation_preview),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismiss, enabled = !translating) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.common_close)
                            )
                        }
                    }

                    Text(
                        text = note.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.notes_translation_source_language),
                            color = Color.Black,
                            modifier = Modifier.weight(1f)
                        )
                        Box {
                            OutlinedButton(
                                onClick = { sourceMenuExpanded = true },
                                enabled = !translating
                            ) {
                                Text(sourceLanguageName(sourceLanguage), color = Color.Black)
                            }
                            DropdownMenu(
                                expanded = sourceMenuExpanded,
                                onDismissRequest = { sourceMenuExpanded = false },
                                containerColor = Color.White
                            ) {
                                NoteSourceLanguage.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(sourceLanguageName(option), color = Color.Black)
                                        },
                                        onClick = {
                                            sourceLanguage = option
                                            sourceMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (sourceLanguage == NoteSourceLanguage.AUTOMATIC) {
                        Text(
                            text = stringResource(
                                R.string.notes_translation_detected,
                                detectedLanguage?.let { translationLanguageName(it) }
                                    ?: stringResource(R.string.notes_translation_not_detected)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Black
                        )
                    }

                    Text(
                        text = stringResource(R.string.shared_import_translate_with_google),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            TranslationLanguage.SPANISH,
                            TranslationLanguage.SIMPLIFIED_CHINESE,
                            TranslationLanguage.ENGLISH
                        ).forEach { language ->
                            OutlinedButton(
                                onClick = { requestTranslation(language) },
                                enabled = !translating && detectionText.isNotBlank(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = translationLanguageName(language),
                                    color = Color.Black,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    if (body.isNotBlank() || !note.isChecklist) {
                        OutlinedTextField(
                            value = body,
                            onValueChange = {
                                body = it
                                machineTranslated = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = if (note.isChecklist) 100.dp else 260.dp),
                            label = {
                                Text(
                                    stringResource(
                                        if (note.isChecklist) R.string.notes_checklist_description
                                        else R.string.notes_content_placeholder
                                    ),
                                    color = Color.Black
                                )
                            },
                            enabled = !translating,
                            colors = translationTextFieldColors()
                        )
                    }

                    if (note.isChecklist) {
                        OutlinedTextField(
                            value = checklistText,
                            onValueChange = {
                                checklistItems = updateChecklistTexts(checklistItems, it)
                                machineTranslated = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 220.dp),
                            label = {
                                Text(
                                    stringResource(R.string.notes_checklist_type),
                                    color = Color.Black
                                )
                            },
                            enabled = !translating,
                            colors = translationTextFieldColors()
                        )
                    }

                    if (machineTranslated) {
                        Text(
                            text = stringResource(R.string.shared_import_google_attribution),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Black
                        )
                    }

                    Text(
                        text = stringResource(R.string.shared_import_translation_download_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black
                    )
                }

                if (translating) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(16.dp))
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                        Text(
                            stringResource(R.string.notes_translation_translating),
                            color = Color.Black
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                ) {
                    FloatingActionButton(
                        onClick = { actionsExpanded = true },
                        containerColor = Color(0xFF60966B),
                        contentColor = Color.White
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.notes_translation_actions)
                        )
                    }
                    DropdownMenu(
                        expanded = actionsExpanded,
                        onDismissRequest = { actionsExpanded = false },
                        containerColor = Color.White
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.notes_translation_original),
                                    color = Color.Black
                                )
                            },
                            onClick = {
                                body = note.nota
                                checklistItems = originalChecklistItems
                                machineTranslated = false
                                actionsExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.notes_translation_replace),
                                    color = Color.Black
                                )
                            },
                            onClick = {
                                actionsExpanded = false
                                onReplace(currentDraft())
                            },
                            enabled = !translating && detectionText.isNotBlank()
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.notes_translation_new_note),
                                    color = Color.Black
                                )
                            },
                            onClick = {
                                actionsExpanded = false
                                onCreateCopy(currentDraft())
                            },
                            enabled = !translating && detectionText.isNotBlank()
                        )
                    }
                }
            }
        }
    }

    statusMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { statusMessage = null },
            title = {
                Text(stringResource(R.string.notes_translation_title), color = Color.Black)
            },
            text = { Text(message, color = Color.Black) },
            confirmButton = {
                TextButton(onClick = { statusMessage = null }) {
                    Text(stringResource(R.string.common_accept), color = Color.Black)
                }
            }
        )
    }
}

@Composable
private fun translationTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.Black,
    unfocusedTextColor = Color.Black,
    disabledTextColor = Color.Black,
    focusedLabelColor = Color.Black,
    unfocusedLabelColor = Color.Black,
    disabledLabelColor = Color.Black,
    cursorColor = Color.Black
)

private fun renderChecklist(items: List<NotaChecklistItem>): String =
    items.joinToString("\n", transform = NotaChecklistItem::text)

private fun updateChecklistTexts(
    current: List<NotaChecklistItem>,
    plainText: String
): List<NotaChecklistItem> {
    val lines = plainText.lines().filter(String::isNotBlank)
    return lines.mapIndexed { index, line ->
        current.getOrNull(index)?.copy(text = line)
            ?: NotaChecklistItem(
                id = "translated-${System.nanoTime()}-$index",
                text = line,
                checked = false
            )
    }
}

@Composable
private fun sourceLanguageName(language: NoteSourceLanguage): String = when (language) {
    NoteSourceLanguage.AUTOMATIC -> stringResource(R.string.notes_translation_automatic)
    NoteSourceLanguage.SPANISH -> translationLanguageName(TranslationLanguage.SPANISH)
    NoteSourceLanguage.ENGLISH -> translationLanguageName(TranslationLanguage.ENGLISH)
    NoteSourceLanguage.SIMPLIFIED_CHINESE ->
        translationLanguageName(TranslationLanguage.SIMPLIFIED_CHINESE)
}

@Composable
private fun translationLanguageName(language: TranslationLanguage): String = stringResource(
    when (language) {
        TranslationLanguage.SPANISH -> R.string.translation_language_spanish
        TranslationLanguage.ENGLISH -> R.string.translation_language_english
        TranslationLanguage.SIMPLIFIED_CHINESE -> R.string.translation_language_mandarin_chinese
    }
)
