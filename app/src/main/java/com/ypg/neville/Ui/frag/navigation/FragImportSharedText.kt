package com.ypg.neville.ui.frag

import android.os.Bundle
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.model.utils.FraseContextActions
import java.util.concurrent.Executors

class FragImportSharedText : Fragment() {

    private val dbExecutor = Executors.newSingleThreadExecutor()

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
        super.onDestroy()
    }

    @Composable
    private fun ImportSharedTextScreen(initialText: String) {
        val context = LocalContext.current
        var sharedText by rememberSaveable { mutableStateOf(initialText) }
        var target by rememberSaveable { mutableStateOf(ImportTarget.NOTA) }
        var notaTitulo by rememberSaveable { mutableStateOf(defaultNoteTitle(context, initialText)) }
        var fraseAutor by rememberSaveable { mutableStateOf("Web") }
        var fraseFuente by rememberSaveable { mutableStateOf("") }
        var showInvalidDialog by remember { mutableStateOf(false) }
        var saving by remember { mutableStateOf(false) }

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
                                Toast.makeText(context, context.getString(R.string.shared_import_success_notes), Toast.LENGTH_SHORT).show()
                                dismissHostSheet()
                            }
                            ImportTarget.FRASE -> {
                                Toast.makeText(context, context.getString(R.string.shared_import_success_quotes), Toast.LENGTH_SHORT).show()
                                dismissHostSheet()
                            }
                            ImportTarget.LIENZO -> {
                                FraseContextActions.cargarFraseEnLienzo(context, cleanText)
                                dismissHostSheet()
                            }
                        }
                    } else {
                        Toast.makeText(context, context.getString(R.string.shared_import_failed), Toast.LENGTH_SHORT).show()
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
                onValueChange = { sharedText = it },
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

            if (target == ImportTarget.NOTA) {
                OutlinedTextField(
                    value = notaTitulo,
                    onValueChange = { notaTitulo = it },
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
                contentPadding = PaddingValues(vertical = 12.dp)
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

    private enum class ImportTarget {
        NOTA,
        FRASE,
        LIENZO
    }
}
