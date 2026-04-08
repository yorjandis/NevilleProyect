package com.ypg.neville.model.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.preference.PreferenceManager
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
            .setTitle("Adicionar una nueva frase")
            .setMessage("Adicione sus propias frases a la biblioteca")
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
                        label = { Text("Frase") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(168.dp)
                            .focusRequester(focusRequester),
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = autor,
                        onValueChange = { autor = it },
                        label = { Text("Autor") },
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = fuente,
                        onValueChange = { fuente = it },
                        label = { Text("Fuente") },
                        shape = RoundedCornerShape(14.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("Guardar", modifier = Modifier.clickable {
                            if (frase.trim().isNotEmpty()) {
                                val res = utilsDB.insertNewFrase(pcontext, frase, autor, fuente, "0")
                                if (res < 0) {
                                    Toast.makeText(pcontext, "Error al adicionar la frases", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(pcontext, "Frase adicionada con éxito", Toast.LENGTH_SHORT).show()
                                    frase = ""
                                    autor = ""
                                    fuente = ""
                                }
                            } else {
                                Toast.makeText(pcontext, "Debe establecer el texto de la frase", Toast.LENGTH_SHORT).show()
                            }
                        })
                        Text("Compartir", modifier = Modifier.clickable {
                            if (frase.trim().isNotEmpty()) {
                                QRManager.ShowQRDialog(
                                    pcontext,
                                    "f::$frase:: $autor:: $fuente",
                                    "Compartir Frase",
                                    "Puede utilizar el lector QR para importar frases"
                                )
                            } else {
                                Toast.makeText(pcontext, "Debe establecer el texto de la frase", Toast.LENGTH_SHORT).show()
                            }
                        })
                        Text("Cerrar", modifier = Modifier.clickable { alertDialog.dismiss() })
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
            .setTitle("Apuntes Personales")
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
                        label = { Text("Título") },
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = nota,
                        onValueChange = { nota = it },
                        label = { Text("Nota") },
                        shape = RoundedCornerShape(14.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("Guardar", modifier = Modifier.clickable {
                            if (titulo.trim().isEmpty() || nota.trim().isEmpty()) {
                                Toast.makeText(context, "Debe establecer un título y una nota", Toast.LENGTH_LONG).show()
                            } else {
                                if (isUpdate) {
                                    if (utilsDB.updateApunte(context, titulo.trim(), nota.trim())) {
                                        Toast.makeText(context, "El apunte ha sido actualizado", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Error al adicionar the apunte", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    val res = utilsDB.insertNewApunte(context, titulo, nota)
                                    if (res < 0) {
                                        Toast.makeText(context, "Error al adicionar el apunte", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "El apunte fue adicionado!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        })

                        Text("Compartir", modifier = Modifier.clickable {
                            if (titulo.trim().isNotEmpty() && nota.trim().isNotEmpty()) {
                                QRManager.ShowQRDialog(
                                    context,
                                    "a::$titulo::$nota",
                                    "Compartir Apunte",
                                    "Puede utilizar el lector QR para importar apuntes"
                                )
                            } else {
                                Toast.makeText(context, "Debe establecer un título y una nota", Toast.LENGTH_SHORT).show()
                            }
                        })

                        Text("Cerrar", modifier = Modifier.clickable { alertDialog.dismiss() })
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
            .setTitle("Nota asociada")
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
                        label = { Text("Nota") },
                        modifier = Modifier.focusRequester(focusRequester),
                        shape = RoundedCornerShape(14.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("Guardar", modifier = Modifier.clickable {
                            if (utilsDB.updateNota(context, tableName, clumn_id, valor_id, notaTexto.trim())) {
                                Toast.makeText(context, "La nota fué actualizada", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Error al actualizar la nota", Toast.LENGTH_LONG).show()
                            }
                        })
                        Text("Compartir", modifier = Modifier.clickable {
                            if (notaTexto.trim().isEmpty()) {
                                Toast.makeText(context, "Debe haber una nota para generar el QR", Toast.LENGTH_SHORT).show()
                            } else {
                                QRManager.ShowQRDialog(context, notaTexto.trim(), "Compartir texto", null)
                            }
                        })
                        Text("Cerrar", modifier = Modifier.clickable { alertDialog.dismiss() })
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
            .setTitle(ptitle)
            .setMessage(pMessage)
            .setIcon(ico ?: AppCompatResources.getDrawable(pcontext, R.drawable.ic_help))
            .setCancelable(true)
            .setView(compose)
            .create()

        compose.setContent {
            com.ypg.neville.ui.theme.NevilleTheme {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(12.dp)) {
                    Text(pContenido)
                    if (showbotonocultarestaayuda) {
                        Text("No volver a mostrar", modifier = Modifier.clickable {
                            PreferenceManager.getDefaultSharedPreferences(pcontext).edit {
                                putBoolean("help_inline", false)
                            }
                            alertDialog.dismiss()
                        })
                    }
                    Text("Cerrar", modifier = Modifier.clickable { alertDialog.dismiss() })
                }
            }
        }

        alertDialog.show()
    }
}
