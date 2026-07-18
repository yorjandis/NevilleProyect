package com.ypg.neville.model.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.ypg.neville.model.preferences.DbPreferences
import com.ypg.neville.R
import com.ypg.neville.model.db.utilsDB

@Suppress("FunctionName")
object UiModalWindows {

    private fun applyDialogKeyboardBehavior(dialog: AlertDialog, composeView: ComposeView) {
        dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        dialog.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
        )
        composeView.isFocusableInTouchMode = true
        composeView.requestFocus()
    }

    @JvmStatic
    fun Add_New_frase(pcontext: Context, contentValues: ContentValues?) {
        val compose = ComposeView(pcontext)
        val alertDialog = AlertDialog.Builder(pcontext, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert)
            .setTitle(pcontext.getString(R.string.shared_dialog_new_quote_title))
            .setMessage(pcontext.getString(R.string.shared_dialog_new_quote_message))
            .setIcon(R.drawable.neville)
            .setCancelable(false)
            .setView(compose)
            .create()

        compose.setContent {
            com.ypg.neville.ui.theme.NevilleTheme {
                var frase by remember { mutableStateOf(contentValues?.getAsString("frase") ?: "") }
                var autor by remember { mutableStateOf(contentValues?.getAsString("autor") ?: "") }
                var fuente by remember { mutableStateOf(contentValues?.getAsString("fuente") ?: "") }
                val focusRequester = remember { FocusRequester() }
                val keyboardController = LocalSoftwareKeyboardController.current

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = frase,
                        onValueChange = { frase = it },
                        label = { Text(stringResource(R.string.phrases_quote_field)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(168.dp)
                            .focusRequester(focusRequester),
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = autor,
                        onValueChange = { autor = it },
                        label = { Text(stringResource(R.string.phrases_author_field)) },
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = fuente,
                        onValueChange = { fuente = it },
                        label = { Text(stringResource(R.string.phrases_source_field)) },
                        shape = RoundedCornerShape(14.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.common_save), modifier = Modifier.clickable {
                            if (frase.trim().isNotEmpty()) {
                                val res = utilsDB.insertNewFrase(pcontext, frase, autor, fuente, "0")
                                if (res < 0) {
                                    Toast.makeText(pcontext, pcontext.getString(R.string.shared_dialog_quote_add_error), Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(pcontext, pcontext.getString(R.string.shared_dialog_quote_added), Toast.LENGTH_SHORT).show()
                                    frase = ""
                                    autor = ""
                                    fuente = ""
                                }
                            } else {
                                Toast.makeText(pcontext, pcontext.getString(R.string.shared_dialog_quote_required), Toast.LENGTH_SHORT).show()
                            }
                        })
                        Text(stringResource(R.string.common_share), modifier = Modifier.clickable {
                            if (frase.trim().isNotEmpty()) {
                                QRManager.ShowQRDialog(
                                    pcontext,
                                    "f::$frase:: $autor:: $fuente",
                                    pcontext.getString(R.string.shared_dialog_share_quote),
                                    pcontext.getString(R.string.shared_dialog_qr_quote_hint)
                                )
                            } else {
                                Toast.makeText(pcontext, pcontext.getString(R.string.shared_dialog_quote_required), Toast.LENGTH_SHORT).show()
                            }
                        })
                        Text(stringResource(R.string.common_close), modifier = Modifier.clickable { alertDialog.dismiss() })
                    }
                }
            }
        }

        alertDialog.show()
        applyDialogKeyboardBehavior(alertDialog, compose)
    }

    @JvmStatic
    fun ApunteManager(context: Context, titleInDB: String, contentValues: ContentValues?, isUpdate: Boolean) {
        val compose = ComposeView(context)
        val alertDialog = AlertDialog.Builder(context, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert)
            .setTitle(context.getString(R.string.shared_dialog_personal_notes))
            .setView(compose)
            .create()

        val initial = if (titleInDB.isNotEmpty()) utilsDB.getApunteByTitle(context, titleInDB) else null

        compose.setContent {
            com.ypg.neville.ui.theme.NevilleTheme {
                var titulo by remember { mutableStateOf(contentValues?.getAsString("title") ?: initial?.titulo.orEmpty()) }
                var nota by remember { mutableStateOf(contentValues?.getAsString("apunte") ?: initial?.nota.orEmpty()) }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = titulo,
                        onValueChange = { if (!isUpdate) titulo = it },
                        enabled = !isUpdate,
                        label = { Text(stringResource(R.string.common_title)) },
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = nota,
                        onValueChange = { nota = it },
                        label = { Text(stringResource(R.string.common_note)) },
                        shape = RoundedCornerShape(14.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.common_save), modifier = Modifier.clickable {
                            if (titulo.trim().isEmpty() || nota.trim().isEmpty()) {
                                Toast.makeText(context, context.getString(R.string.shared_dialog_title_note_required), Toast.LENGTH_LONG).show()
                            } else {
                                if (isUpdate) {
                                    if (utilsDB.updateApunte(context, titulo.trim(), nota.trim())) {
                                        Toast.makeText(context, context.getString(R.string.shared_dialog_note_updated), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.shared_dialog_note_add_error), Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    val res = utilsDB.insertNewApunte(context, titulo, nota)
                                    if (res < 0) {
                                        Toast.makeText(context, context.getString(R.string.shared_dialog_note_add_error), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.shared_dialog_note_added), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        })

                        Text(stringResource(R.string.common_share), modifier = Modifier.clickable {
                            if (titulo.trim().isNotEmpty() && nota.trim().isNotEmpty()) {
                                QRManager.ShowQRDialog(
                                    context,
                                    "a::$titulo::$nota",
                                    context.getString(R.string.shared_dialog_share_personal_note),
                                    context.getString(R.string.shared_dialog_qr_personal_note_hint)
                                )
                            } else {
                                Toast.makeText(context, context.getString(R.string.shared_dialog_title_note_required), Toast.LENGTH_SHORT).show()
                            }
                        })

                        Text(stringResource(R.string.common_close), modifier = Modifier.clickable { alertDialog.dismiss() })
                    }
                }
            }
        }

        alertDialog.show()
    }

    @JvmStatic
    fun NotaManager(context: Context, nota: String, tableName: String, clumn_id: String, valor_id: String) {
        val compose = ComposeView(context)
        val alertDialog = AlertDialog.Builder(context, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert)
            .setTitle(context.getString(R.string.shared_dialog_associated_note))
            .setView(compose)
            .create()

        compose.setContent {
            com.ypg.neville.ui.theme.NevilleTheme {
                var notaTexto by remember { mutableStateOf(nota) }
                val focusRequester = remember { FocusRequester() }
                val keyboardController = LocalSoftwareKeyboardController.current

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = notaTexto,
                        onValueChange = { notaTexto = it },
                        label = { Text(stringResource(R.string.common_note)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp)
                            .focusRequester(focusRequester),
                        shape = RoundedCornerShape(14.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.common_save), modifier = Modifier.clickable {
                            if (utilsDB.updateNota(context, tableName, clumn_id, valor_id, notaTexto.trim())) {
                                Toast.makeText(context, context.getString(R.string.shared_dialog_associated_note_updated), Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, context.getString(R.string.shared_dialog_associated_note_update_error), Toast.LENGTH_LONG).show()
                            }
                        })
                        Text(stringResource(R.string.common_share), modifier = Modifier.clickable {
                            if (notaTexto.trim().isEmpty()) {
                                Toast.makeText(context, context.getString(R.string.shared_dialog_note_share_required), Toast.LENGTH_SHORT).show()
                            } else {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, notaTexto.trim())
                                }
                                context.startActivity(
                                    Intent.createChooser(intent, context.getString(R.string.shared_dialog_share_note))
                                )
                            }
                        })
                        Text(stringResource(R.string.shared_dialog_generate_qr), modifier = Modifier.clickable {
                            if (notaTexto.trim().isEmpty()) {
                                Toast.makeText(context, context.getString(R.string.shared_dialog_note_qr_required), Toast.LENGTH_SHORT).show()
                            } else {
                                QRManager.ShowQRDialog(
                                    context,
                                    notaTexto.trim(),
                                    context.getString(R.string.shared_dialog_share_text),
                                    null
                                )
                            }
                        })
                        Text(stringResource(R.string.common_close), modifier = Modifier.clickable { alertDialog.dismiss() })
                    }
                }
            }
        }

        alertDialog.show()
        applyDialogKeyboardBehavior(alertDialog, compose)
    }

    @JvmStatic
    fun showAyudaContectual(
        pcontext: Context,
        ptitle: String,
        pMessage: String,
        pContenido: String,
        showbotonocultarestaayuda: Boolean,
        ico: Drawable?
    ) {
        val compose = ComposeView(pcontext)
        val alertDialog = AlertDialog.Builder(pcontext, R.style.Dialog)
            .setCancelable(true)
            .setView(compose)
            .create()

        compose.setContent {
            com.ypg.neville.ui.theme.NevilleTheme {
                val accentColor = Color(0xFFFF9800)
                Surface(
                    modifier = Modifier.padding(12.dp),
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = ptitle,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        Text(
                            text = pMessage,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(color = accentColor.copy(alpha = 0.5f))
                        Text(
                            text = pContenido,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp)
                                .verticalScroll(rememberScrollState()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (showbotonocultarestaayuda) {
                                Button(
                                    onClick = {
                                        DbPreferences.default(pcontext).edit {
                                            putBoolean("help_inline", false)
                                        }
                                        alertDialog.dismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Text(stringResource(R.string.shared_dialog_dont_show_again))
                                }
                            }
                            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                            Button(
                                onClick = { alertDialog.dismiss() },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Text(stringResource(R.string.common_close), color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        runCatching {
            alertDialog.setIcon(ico ?: AppCompatResources.getDrawable(pcontext, R.drawable.ic_help))
        }

        alertDialog.show()
    }
}
