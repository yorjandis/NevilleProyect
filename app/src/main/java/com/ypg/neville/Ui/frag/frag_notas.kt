package com.ypg.neville.Ui.frag

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import com.ypg.neville.model.db.room.NotaEntity
import com.ypg.neville.model.db.room.NotaRepository
import com.ypg.neville.model.utils.adapter.NotasAdapter
import java.util.concurrent.Executors

class frag_notas : Fragment() {

    private lateinit var listView: ListView
    private lateinit var txtEmpty: TextView
    private lateinit var fabAdd: FloatingActionButton

    private lateinit var adapter: NotasAdapter
    private lateinit var repository: NotaRepository

    private val dbExecutor = Executors.newSingleThreadExecutor()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.frag_notas, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView = view.findViewById(R.id.frag_notas_list)
        txtEmpty = view.findViewById(R.id.frag_notas_empty)
        fabAdd = view.findViewById(R.id.frag_notas_fab)

        val db = NevilleRoomDatabase.getInstance(requireContext())
        repository = NotaRepository(db.notaDao())

        adapter = NotasAdapter(
            context = requireContext(),
            notas = mutableListOf(),
            onEdit = { nota -> showNotaDialog(nota) },
            onDelete = { nota -> confirmDelete(nota) }
        )

        listView.adapter = adapter
        fabAdd.setOnClickListener { showNotaDialog(null) }

        loadNotas()
    }

    override fun onStart() {
        super.onStart()
        MainActivity.mainActivityThis?.ic_toolsBar_frase_add?.visibility = View.GONE
        MainActivity.mainActivityThis?.ic_toolsBar_nota_add?.visibility = View.GONE
        MainActivity.mainActivityThis?.ic_toolsBar_fav?.visibility = View.GONE
    }

    override fun onStop() {
        super.onStop()
        MainActivity.mainActivityThis?.ic_toolsBar_frase_add?.visibility = View.VISIBLE
        MainActivity.mainActivityThis?.ic_toolsBar_nota_add?.visibility = View.VISIBLE
    }

    private fun loadNotas() {
        dbExecutor.execute {
            val notas = repository.obtenerTodas()
            requireActivity().runOnUiThread {
                adapter.updateData(notas)
                txtEmpty.visibility = if (notas.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showNotaDialog(nota: NotaEntity?) {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.modal_add_notas, null)
        val editTitulo = view.findViewById<EditText>(R.id.modal_add_nota_edit_title)
        val editNota = view.findViewById<EditText>(R.id.modal_add_nota_edit_nota)
        val btnSave = view.findViewById<Button>(R.id.modal_add_nota_edit_btnguardar)
        val btnClose = view.findViewById<Button>(R.id.modal_add_nota_edit_btnsalir)
        val btnShare = view.findViewById<ImageView>(R.id.modal_add_nota_shared)

        btnShare.visibility = View.GONE

        if (nota != null) {
            editTitulo.setText(nota.titulo)
            editNota.setText(nota.nota)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(if (nota == null) "Nueva nota" else "Editar nota")
            .setView(view)
            .create()

        btnClose.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val titulo = editTitulo.text.toString().trim()
            val contenido = editNota.text.toString().trim()

            if (titulo.isEmpty() || contenido.isEmpty()) {
                Toast.makeText(requireContext(), "Debes escribir título y nota", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dbExecutor.execute {
                if (nota == null) {
                    repository.insertar(titulo, contenido)
                } else {
                    repository.actualizar(
                        id = nota.id,
                        titulo = titulo,
                        nota = contenido,
                        fechaCreacionOriginal = nota.fechaCreacion
                    )
                }
                requireActivity().runOnUiThread {
                    dialog.dismiss()
                    loadNotas()
                }
            }
        }

        dialog.show()
    }

    private fun confirmDelete(nota: NotaEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar nota")
            .setMessage("¿Seguro que quieres eliminar '${nota.titulo}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                dbExecutor.execute {
                    repository.eliminar(nota)
                    requireActivity().runOnUiThread {
                        loadNotas()
                        Toast.makeText(requireContext(), "Nota eliminada", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
