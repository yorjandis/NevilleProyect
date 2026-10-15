package com.ypg.neville.ui.frag

import android.os.Bundle
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.feature.translation.data.MlKitOnDeviceTranslationService
import com.ypg.neville.feature.translation.domain.OnDeviceTranslationService
import com.ypg.neville.feature.translation.domain.TranslationLanguage
import com.ypg.neville.feature.translation.domain.UnsupportedSourceLanguageException
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.model.utils.FraseContextActions
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class FragImportSharedText : Fragment() {

    private val dbExecutor = Executors.newSingleThreadExecutor()
    private val translationServiceDelegate = lazy<OnDeviceTranslationService> {
        MlKitOnDeviceTranslationService()
    }
    private val translationService by translationServiceDelegate

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val sharedText = arguments?.getString(MainActivity.EXTRA_SHARED_TEXT).orEmpty()

        (view as ComposeView).setContent {
            com.ypg.neville.ui.theme.NevilleTheme {
                ImportSharedTextScreen(sharedText)
            }
        }
    }

    override fun onDestroy() {
        dbExecutor.shutdown()
        if (translationServiceDelegate.isInitialized()) {
            translationService.close()
        }
        super.onDestroy()
    }

    @Composable
    private fun ImportSharedTextScreen(initialText: String) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        var sharedText by rememberSaveable { mutableStateOf(initialText) }
        var target by rememberSaveable { mutableStateOf(ImportTarget.NOTA) }
        var notaTitulo by rememberSaveable { mutableStateOf(defaultNoteTitle(context, initialText)) }
        var noteTitleEdited by rememberSaveable { mutableStateOf(false) }
        var fraseAutor by rememberSaveable { mutableStateOf("Web") }
        var fraseFuente by rememberSaveable { mutableStateOf("") }
        var showInvalidDialog by remember { mutableStateOf(false) }
        var saving by remember { mutableStateOf(false) }
        var selectedTranslationLanguage by rememberSaveable {
            mutableStateOf(defaultTranslationLanguage(context))
        }
        var languageMenuExpanded by remember { mutableStateOf(false) }
        var translating by remember { mutableStateOf(false) }
        var translationErrorMessage by remember { mutableStateOf<Int?>(null) }
        var isMachineTranslation by rememberSaveable { mutableStateOf(false) }
        val selectedTranslationLanguageName = translationLanguageName(selectedTranslationLanguage)
        val alreadyInLanguageMessage = stringResource(
            R.string.shared_import_already_in_language,
            selectedTranslationLanguageName
        )
        val importNotesSuccessMessage = stringResource(R.string.shared_import_success_notes)
        val importQuotesSuccessMessage = stringResource(R.string.shared_import_success_quotes)
        val importFailedMessage = stringResource(R.string.shared_import_failed)

        fun translateSharedText() {
            val sourceText = sharedText.trim()
            if (sourceText.isBlank() || translating) return

            coroutineScope.launch {
                translating = true
                try {
                    val result = translationService.translate(
                        text = sourceText,
                        targetLanguage = selectedTranslationLanguage
                    )
                    sharedText = result.text
                    isMachineTranslation = result.wasTranslated
                    if (!noteTitleEdited && result.wasTranslated) {
                        notaTitulo = defaultNoteTitle(context, result.text)
                    }
                    if (!result.wasTranslated) {
                        Toast.makeText(
                            context,
                            alreadyInLanguageMessage,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (_: UnsupportedSourceLanguageException) {
                    translationErrorMessage = R.string.shared_import_translation_unsupported_source
                } catch (_: Exception) {
                    translationErrorMessage = R.string.shared_import_translation_failed_body
                } finally {
                    translating = false
                }
            }
        }

        fun saveImport() {
            val cleanText = sharedText.trim()
            if (cleanText.isBlank()) {
                showInvalidDialog = true
                return
            }
            saving = true
            dbExecutor.execute {
                val ok = when (target) {
                    ImportTarget.NOTA -> {
                        val title = notaTitulo.trim().ifBlank { defaultNoteTitle(context, cleanText) }
                        utilsDB.insertNewApunte(context, title, cleanText) > 0L
                    }
                    ImportTarget.FRASE -> {
                        utilsDB.insertNewFrase(
                            context,
                            cleanText,
                            fraseAutor.trim().ifBlank { "Web" },
                            fraseFuente.trim(),
                            "0"
                        ) > 0L
                    }
                    ImportTarget.LIENZO -> true
                }

                activity?.runOnUiThread {
                    saving = false
                    if (ok) {
                        when (target) {
                            ImportTarget.NOTA -> {
                                Toast.makeText(context, importNotesSuccessMessage, Toast.LENGTH_SHORT).show()
                                dismissHostSheet()
                            }
                            ImportTarget.FRASE -> {
                                Toast.makeText(context, importQuotesSuccessMessage, Toast.LENGTH_SHORT).show()
                                dismissHostSheet()
                            }
                            ImportTarget.LIENZO -> {
                                FraseContextActions.cargarFraseEnLienzo(context, cleanText)
                                dismissHostSheet()
                            }
                        }
                    } else {
                        Toast.makeText(context, importFailedMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF9C94F1),
                            Color(0xFFA7A4D4),
                            Color(0xFF799E93)
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.shared_import_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(R.string.shared_import_subtitle),
                color = Color.Black,
                style = MaterialTheme.typography.bodyMedium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { target = ImportTarget.NOTA },
                    label = { Text(stringResource(R.string.home_nav_notes), color = Color.Black) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (target == ImportTarget.NOTA) Color(0xFFFF9800) else Color.White
                    )
                )
                AssistChip(
                    onClick = { target = ImportTarget.FRASE },
                    label = { Text(stringResource(R.string.home_nav_quotes), color = Color.Black) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (target == ImportTarget.FRASE) Color(0xFFFF9800) else Color.White
                    )
                )
                AssistChip(
                    onClick = { target = ImportTarget.LIENZO },
                    label = { Text(stringResource(R.string.home_nav_canvas), color = Color.Black) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (target == ImportTarget.LIENZO) Color(0xFFFF9800) else Color.White
                    )
                )
            }

            OutlinedTextField(
                value = sharedText,
                onValueChange = {
                    sharedText = it
                    isMachineTranslation = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                label = { Text(stringResource(R.string.shared_import_text), color = Color.Black) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                minLines = 8,
                maxLines = 14
            )

            if (isMachineTranslation) {
                Text(
                    text = stringResource(R.string.shared_import_google_attribution),
                    color = Color.Black,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Text(
                text = stringResource(R.string.shared_import_translate_to),
                color = Color.Black,
                style = MaterialTheme.typography.labelLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { languageMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !translating
                    ) {
                        Text(
                            text = selectedTranslationLanguageName,
                            color = Color.Black
                        )
                    }
                    DropdownMenu(
                        expanded = languageMenuExpanded,
                        onDismissRequest = { languageMenuExpanded = false }
                    ) {
                        TranslationLanguage.entries.forEach { language ->
                            DropdownMenuItem(
                                text = {
                                    Text(translationLanguageName(language))
                                },
                                onClick = {
                                    selectedTranslationLanguage = language
                                    languageMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = { translateSharedText() },
                    enabled = sharedText.isNotBlank() && !translating && !saving
                ) {
                    if (translating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.Black
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.shared_import_translate_with_google),
                            color = Color.Black
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.shared_import_translation_download_hint),
                color = Color.Black,
                style = MaterialTheme.typography.bodySmall
            )

            if (target == ImportTarget.NOTA) {
                OutlinedTextField(
                    value = notaTitulo,
                    onValueChange = {
                        notaTitulo = it
                        noteTitleEdited = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.shared_import_note_title), color = Color.Black) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    singleLine = true
                )
            } else if (target == ImportTarget.FRASE) {
                OutlinedTextField(
                    value = fraseAutor,
                    onValueChange = { fraseAutor = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.phrases_author_field), color = Color.Black) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    singleLine = true
                )
                OutlinedTextField(
                    value = fraseFuente,
                    onValueChange = { fraseFuente = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.phrases_source_field), color = Color.Black) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    singleLine = true
                )
            }

            Button(
                onClick = { if (!saving) saveImport() },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp),
                enabled = !saving && !translating
            ) {
                Text(
                    stringResource(
                        if (saving) R.string.shared_import_saving
                        else if (target == ImportTarget.LIENZO) R.string.shared_import_load_canvas
                        else R.string.common_save
                    ),
                    color = Color.Black
                )
            }
        }

        if (showInvalidDialog) {
            AlertDialog(
                onDismissRequest = { showInvalidDialog = false },
                title = { Text(stringResource(R.string.shared_import_empty_title)) },
                text = { Text(stringResource(R.string.shared_import_empty_body)) },
                confirmButton = {
                    TextButton(onClick = { showInvalidDialog = false }) {
                        Text(stringResource(R.string.shared_import_understood))
                    }
                }
            )
        }

        translationErrorMessage?.let { messageResId ->
            AlertDialog(
                onDismissRequest = { translationErrorMessage = null },
                title = { Text(stringResource(R.string.shared_import_translation_failed_title)) },
                text = { Text(stringResource(messageResId)) },
                confirmButton = {
                    TextButton(onClick = { translationErrorMessage = null }) {
                        Text(stringResource(R.string.shared_import_understood))
                    }
                }
            )
        }
    }

    private fun dismissHostSheet() {
        var current: Fragment? = this
        while (current != null) {
            if (current is DialogFragment) {
                current.dismiss()
                return
            }
            current = current.parentFragment
        }
    }

    private fun defaultNoteTitle(context: Context, text: String): String {
        val candidate = text
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
            .orEmpty()
            .take(50)
            .trim()

        return if (candidate.isBlank()) {
            context.getString(R.string.shared_import_default_title)
        } else {
            context.getString(R.string.shared_import_default_title_format, candidate)
        }
    }

    private fun defaultTranslationLanguage(context: Context): TranslationLanguage =
        TranslationLanguage.fromLanguageTag(
            context.resources.configuration.locales[0].toLanguageTag()
        ) ?: TranslationLanguage.SPANISH

    @Composable
    private fun translationLanguageName(language: TranslationLanguage): String = stringResource(
        when (language) {
            TranslationLanguage.SPANISH -> R.string.translation_language_spanish
            TranslationLanguage.ENGLISH -> R.string.translation_language_english
            TranslationLanguage.SIMPLIFIED_CHINESE -> R.string.translation_language_mandarin_chinese
        }
    )

    private enum class ImportTarget {
        NOTA,
        FRASE,
        LIENZO
    }
}
