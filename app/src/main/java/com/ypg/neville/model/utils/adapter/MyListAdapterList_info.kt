package com.ypg.neville.model.utils.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageView
import com.ypg.neville.R
import com.ypg.neville.Ui.frag.frag_list_info
import com.ypg.neville.model.db.DBManager
import com.ypg.neville.model.db.DatabaseHelper
import com.ypg.neville.model.utils.QRManager
import com.ypg.neville.model.utils.UiModalWindows
import com.ypg.neville.model.utils.utilsFields
import java.util.LinkedList

class MyListAdapterList_info(context: Context, private val layout: Int, objects: List<String>) :
    ArrayAdapter<String>(context, layout, objects) {

    private val internalList: MutableList<String> = LinkedList()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val holder: ViewHolder

        if (view == null) {
            val inflater = LayoutInflater.from(context)
            view = inflater.inflate(layout, parent, false)
            holder = ViewHolder()
            holder.text = view.findViewById(R.id.row_list_info_text_conf)
            holder.textAutor = view.findViewById(R.id.row_list_info_text_autor)
            holder.ic_del = view.findViewById(R.id.row_list_info_ic_del)
            holder.ic_share = view.findViewById(R.id.row_list_info_ic_shared)
            view.tag = holder
        } else {
            holder = view.tag as ViewHolder
        }

        val item = getItem(position)
        holder.text?.text = item

        holder.ic_share?.setOnClickListener {
            val spinnerText = frag_list_info.spinnerStatic?.selectedItem?.toString() ?: ""
            if (spinnerText.contains("Frases")) {
                val texto = "f&&${holder.text?.text}&&"
                QRManager.ShowQRDialog(context, texto, "Compartir Frase", null)
            } else if (spinnerText.contains("Apuntes")) {
                UiModalWindows.ApunteManager(context, holder.text?.text.toString(), null, true)
            }
        }

        holder.ic_del?.setOnClickListener {
            if (utilsFields.spinnerListInfoItemSelected == "Frases inbuilt" ||
                utilsFields.spinnerListInfoItemSelected == "Frases inbuilt favoritas" ||
                utilsFields.spinnerListInfoItemSelected == "Frases inbuilt con notas" ||
                utilsFields.spinnerListInfoItemSelected == "Conferencias favoritas" ||
                utilsFields.spinnerListInfoItemSelected == "Conferencias con notas" ||
                utilsFields.spinnerListInfoItemSelected == "Videos inbuilt favoritos" ||
                utilsFields.spinnerListInfoItemSelected == "Videos inbuilt con notas") {
                return@setOnClickListener
            }

            val builder = AlertDialog.Builder(context)
            builder.setTitle("¿Eliminando elemento en: ${utilsFields.spinnerListInfoItemSelected}?")
            builder.setPositiveButton("Eliminar") { _, _ ->
                val dbManager = DBManager(context).open()
                internalList.clear()

                when (utilsFields.spinnerListInfoItemSelected) {
                    "Frases personales", "Frases favoritas personales", "Frases personales con notas" -> {
                        dbManager.delete_ForIdStr(DatabaseHelper.T_Frases, DatabaseHelper.C_frases_frase, holder.text?.text.toString())
                    }
                    "Apuntes" -> {
                        dbManager.delete_ForIdStr(DatabaseHelper.T_Apuntes, DatabaseHelper.C_apunte_title, holder.text?.text.toString())
                    }
                }

                val cursor = dbManager.getListado(utilsFields.spinnerListInfoItemSelected)
                if (cursor.moveToFirst()) {
                    do {
                        internalList.add(cursor.getString(1))
                    } while (cursor.moveToNext())
                }
                cursor.close()
                dbManager.close()

                clear()
                addAll(internalList)
                notifyDataSetChanged()
            }
            builder.show()
        }

        return view!!
    }

    private class ViewHolder {
        var text: TextView? = null
        var textAutor: TextView? = null
        var ic_del: AppCompatImageView? = null
        var ic_share: AppCompatImageView? = null
    }
}
