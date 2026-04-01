package com.ypg.neville.model.utils.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import com.ypg.neville.R
import com.ypg.neville.model.db.room.NotaEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotasAdapter(
    private val context: Context,
    private val notas: MutableList<NotaEntity>,
    private val onEdit: (NotaEntity) -> Unit,
    private val onDelete: (NotaEntity) -> Unit
) : BaseAdapter() {

    private val inflater = LayoutInflater.from(context)
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun getCount(): Int = notas.size

    override fun getItem(position: Int): NotaEntity = notas[position]

    override fun getItemId(position: Int): Long = notas[position].id

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: inflater.inflate(R.layout.row_nota_item, parent, false)
        val nota = getItem(position)

        val titulo = view.findViewById<TextView>(R.id.row_nota_titulo)
        val texto = view.findViewById<TextView>(R.id.row_nota_texto)
        val fecha = view.findViewById<TextView>(R.id.row_nota_fecha)
        val edit = view.findViewById<AppCompatImageView>(R.id.row_nota_edit)
        val delete = view.findViewById<AppCompatImageView>(R.id.row_nota_delete)

        titulo.text = nota.titulo
        texto.text = nota.nota
        fecha.text = "Creación: ${dateFormat.format(Date(nota.fechaCreacion))} | Modificación: ${dateFormat.format(Date(nota.fechaModificacion))}"

        edit.setOnClickListener { onEdit(nota) }
        delete.setOnClickListener { onDelete(nota) }

        return view
    }

    fun updateData(newData: List<NotaEntity>) {
        notas.clear()
        notas.addAll(newData)
        notifyDataSetChanged()
    }
}
