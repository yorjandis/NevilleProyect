package com.ypg.neville.model.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageView
import androidx.preference.PreferenceManager
import com.ypg.neville.R
import com.ypg.neville.model.db.DBManager
import com.ypg.neville.model.db.DatabaseHelper
import com.ypg.neville.model.db.utilsDB

object UiModalWindows {

    /**
     * Diálogo para adicionar una nueva frase
     * @param pcontext
     * @param contentValues Conjunto de valores a cargar en los campos
     */
    @JvmStatic
    fun Add_New_frase(pcontext: Context, contentValues: ContentValues?) {
        val builder = AlertDialog.Builder(pcontext, androidx.appcompat.R.style.Base_Theme_AppCompat_Dialog_Alert)
        builder.setTitle("Adicionar una nueva frase")
        builder.setMessage("Adicione sus propias frases a la biblioteca")
        builder.setIcon(R.drawable.neville)
        builder.setCancelable(false)

        val view = LayoutInflater.from(pcontext).inflate(R.layout.modal_add_frase, null)
        val editFrase = view.findViewById<EditText>(R.id.mod_add_frase_edit_frase)
        val editAutor = view.findViewById<EditText>(R.id.mod_add_frase_edit_autor)
        val editFuente = view.findViewById<EditText>(R.id.mod_add_frase_edit_fuente)
        val btn_save = view.findViewById<Button>(R.id.mod_add_frase_btn_save)
        val btn_cancel = view.findViewById<Button>(R.id.mod_add_frase_btn_salir)
        val img_shared = view.findViewById<AppCompatImageView>(R.id.mod_add_frase_shared)

        builder.setView(view)
        val alertDialog = builder.create()

        if (contentValues != null) {
            editFrase.setText(contentValues.getAsString("frase"))
            editAutor.setText(contentValues.getAsString("autor"))
            editFuente.setText(contentValues.getAsString("fuente"))
        }

        btn_cancel.setOnClickListener { alertDialog.dismiss() }

        btn_save.setOnClickListener {
            if (editFrase.text.toString().trim().isNotEmpty()) {
                val res = utilsDB.insertNewFrase(pcontext, editFrase.text.toString(),
                    editAutor.text.toString(),
                    editFuente.text.toString(),
                    "0")
                if (res < 0) {
                    Toast.makeText(pcontext, "Error al adicionar la frases", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(pcontext, "Frase adicionada con éxito", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(pcontext, "Debe establecer el texto de la frase", Toast.LENGTH_SHORT).show()
            }
            editFrase.setText("")
            editAutor.setText("")
            editFuente.setText("")
            editFrase.requestFocus()
        }

        img_shared.setOnClickListener {
            if (editFrase.text.toString().trim().isNotEmpty()) {
                QRManager.ShowQRDialog(pcontext, "f::" +
                        editFrase.text.toString() + ":: " +
                        editAutor.text.toString() + ":: " +
                        editFuente.text.toString(), "Compartir Frase",
                    "Puede utilizar el lector QR para importar frases")
            } else {
                Toast.makeText(pcontext, "Debe establecer el texto de la frase", Toast.LENGTH_SHORT).show()
            }
        }

        editFrase.requestFocus()
        alertDialog.show()

        val imm = pcontext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    /**
     * Muestra, guarda, actualiza Apuntes. Trabaja sobre la tabla de apuntes
     * @param context contexto de trabajo
     * @param titleInDB Si es dado: carga la info del apunte desde la BD
     * @param contentValues Si es dado: Carga los valores contenidos en el contentValue
     * @param isUpdate Si es true, actualiza la info del apunte
     */
    @JvmStatic
    fun ApunteManager(context: Context, titleInDB: String, contentValues: ContentValues?, isUpdate: Boolean) {
        val builder = AlertDialog.Builder(context, androidx.appcompat.R.style.Base_Theme_AppCompat_Dialog_Alert)
        builder.setTitle("Apuntes Personales")

        val view = LayoutInflater.from(context).inflate(R.layout.modal_add_notas, null)
        val edit_titulo = view.findViewById<EditText>(R.id.modal_add_nota_edit_title)
        val edit_nota = view.findViewById<EditText>(R.id.modal_add_nota_edit_nota)
        val btn_save = view.findViewById<Button>(R.id.modal_add_nota_edit_btnguardar)
        val btn_cancel = view.findViewById<Button>(R.id.modal_add_nota_edit_btnsalir)
        val img_shared = view.findViewById<ImageView>(R.id.modal_add_nota_shared)

        builder.setView(view)
        val alertDialog = builder.create()
        alertDialog.show()

        if (titleInDB.isNotEmpty()) {
            val query = "SELECT * FROM ${DatabaseHelper.T_Apuntes} WHERE ${DatabaseHelper.C_apunte_title} = '$titleInDB';"
            val dbManager = DBManager(context).open()
            val cursor = dbManager.ejectSQLRawQuery(query)
            if (cursor.moveToFirst()) {
                edit_titulo.setText(cursor.getString(1))
                edit_nota.setText(cursor.getString(2))
            }
            cursor.close()
            dbManager.close()
        }

        if (contentValues != null) {
            edit_titulo.setText(contentValues.getAsString("title"))
            edit_nota.setText(contentValues.getAsString("apunte"))
        }

        if (isUpdate) {
            edit_titulo.isEnabled = false
        }

        img_shared.setOnClickListener {
            if (edit_titulo.text.toString().trim().isNotEmpty() && edit_nota.text.toString().trim().isNotEmpty()) {
                QRManager.ShowQRDialog(context, "a::" +
                        edit_titulo.text.toString() + "::" +
                        edit_nota.text.toString(), "Compartir Apunte",
                    "Puede utilizar el lector QR para importar apuntes")
            } else {
                Toast.makeText(context, "Debe establecer un título y una nota", Toast.LENGTH_SHORT).show()
            }
        }

        btn_save.setOnClickListener {
            if (edit_titulo.text.toString().trim().isEmpty() || edit_nota.text.toString().trim().isEmpty()) {
                Toast.makeText(context, "Debe establecer un título y una nota", Toast.LENGTH_LONG).show()
            } else {
                if (isUpdate) {
                    if (utilsDB.updateApunte(context, edit_titulo.text.toString().trim(), edit_nota.text.toString().trim())) {
                        Toast.makeText(context, "El apunte ha sido actualizado", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Error al adicionar the apunte", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val res = utilsDB.insertNewApunte(context, edit_titulo.text.toString(), edit_nota.text.toString())
                    if (res < 0) {
                        Toast.makeText(context, "Error al adicionar el apunte", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "El apunte fue adicionado!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btn_cancel.setOnClickListener { alertDialog.dismiss() }
    }

    /**
     * Muestra/modifica el contenido de una nota
     */
    @JvmStatic
    fun NotaManager(context: Context, nota: String, tableName: String, clumn_id: String, valor_id: String) {
        val builder = AlertDialog.Builder(context, androidx.appcompat.R.style.Base_Theme_AppCompat_Dialog_Alert)
        builder.setTitle("Nota asociada")

        val view = LayoutInflater.from(context).inflate(R.layout.modal_add_notas, null)
        view.findViewById<View>(R.id.modal_add_nota_edit_title).visibility = View.GONE
        val edit_nota = view.findViewById<EditText>(R.id.modal_add_nota_edit_nota)
        val btn_cancel = view.findViewById<Button>(R.id.modal_add_nota_edit_btnsalir)
        val btn_save = view.findViewById<Button>(R.id.modal_add_nota_edit_btnguardar)
        val ic_shared = view.findViewById<ImageView>(R.id.modal_add_nota_shared)

        builder.setView(view)
        edit_nota.setText(nota)

        val alert = builder.create()
        alert.show()

        btn_cancel.setOnClickListener { alert.dismiss() }

        btn_save.setOnClickListener {
            if (utilsDB.updateNota(context, tableName, clumn_id, valor_id, edit_nota.text.toString().trim())) {
                Toast.makeText(context, "La nota fué actualizada", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Error al actualizar la nota", Toast.LENGTH_LONG).show()
            }
        }

        ic_shared.setOnClickListener {
            if (edit_nota.text.toString().trim().isEmpty()) {
                Toast.makeText(context, "Debe haber una nota para generar el QR", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            QRManager.ShowQRDialog(context, edit_nota.text.toString().trim(), "Compartir texto", null)
        }
    }

    /**
     * Ventana de Ayuda contextual
     */
    @JvmStatic
    fun showAyudaContectual(pcontext: Context, ptitle: String, pMessage: String, pContenido: String, showbotonocultarestaayuda: Boolean, ico: Drawable?) {
        val builder = AlertDialog.Builder(pcontext, R.style.Dialog)
        builder.setTitle(ptitle)
        builder.setMessage(pMessage)
        if (ico != null) {
            builder.setIcon(ico)
        } else {
            builder.setIcon(R.drawable.ic_help)
        }
        builder.setCancelable(true)

        val view = LayoutInflater.from(pcontext).inflate(R.layout.layout_ayuda, null)
        val text = view.findViewById<TextView>(R.id.layout_ayuda_text)
        val btn = view.findViewById<Button>(R.id.layout_ayuda_btn_hide)
        val btn_cerrar = view.findViewById<Button>(R.id.layout_ayuda_btn_cerrar)

        text.text = pContenido

        if (!showbotonocultarestaayuda) {
            btn.visibility = View.INVISIBLE
        }

        builder.setView(view)
        val alertDialog = builder.create()

        btn.setOnClickListener {
            PreferenceManager.getDefaultSharedPreferences(pcontext).edit().putBoolean("help_inline", false).apply()
            alertDialog.dismiss()
        }

        btn_cerrar.setOnClickListener { alertDialog.dismiss() }
        alertDialog.show()
    }
}
