package com.ypg.neville.ui.frag

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.Fragment
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import com.ypg.neville.model.db.room.NotaEntity
import com.ypg.neville.model.db.room.NotaRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class FragNotas : Fragment() {

    private lateinit var repository: NotaRepository
    private val dbExecutor = Executors.newSingleThreadExecutor()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = NevilleRoomDatabase.getInstance(requireContext())
        repository = NotaRepository(db.notaDao())

        (view as ComposeView).setContent {
            MaterialTheme {
                NotasScreen()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        MainActivity.currentInstance()?.icToolsBarFraseAdd?.visibility = View.GONE
        MainActivity.currentInstance()?.icToolsBarNotaAdd?.visibility = View.GONE
        MainActivity.currentInstance()?.icToolsBarFav?.visibility = View.GONE
    }

    override fun onStop() {
        super.onStop()
        MainActivity.currentInstance()?.icToolsBarFraseAdd?.visibility = View.VISIBLE
        MainActivity.currentInstance()?.icToolsBarNotaAdd?.visibility = View.VISIBLE
    }

    @Composable
    @Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
    private fun NotasScreen() {
        val context = LocalContext.current
        val notas = remember { mutableStateListOf<NotaEntity>() }

        var notaEnEdicion by remember { mutableStateOf<NotaEntity?>(null) }
        var notaAEliminar by remember { mutableStateOf<NotaEntity?>(null) }
        var showEditor by remember { mutableStateOf(false) }

        fun recargarNotas() {
            dbExecutor.execute {
                val data = repository.obtenerTodas()
                activity?.runOnUiThread {
                    notas.clear()
                    notas.addAll(data)
                }
            }
        }

        LaunchedEffect(Unit) {
            recargarNotas()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Notas",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 8.dp)
                )

                if (notas.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay notas todavía", fontSize = 18.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(notas, key = { it.id }) { nota ->
                            NotaRow(
                                nota = nota,
                                fechaTexto = "Creación: ${dateFormat.format(Date(nota.fechaCreacion))} | Modificación: ${dateFormat.format(Date(nota.fechaModificacion))}",
                                onEdit = {
                                    notaEnEdicion = nota
                                    showEditor = true
                                },
                                onDelete = { notaAEliminar = nota }
                            )
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = {
                    notaEnEdicion = null
                    showEditor = true
                },
                containerColor = colorResource(id = R.color.light_blue_200),
                contentColor = Color.Black,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 20.dp)
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_note_add), contentDescription = "Crear nota")
            }
        }

        if (showEditor) {
            LegacyNotaEditorDialog(
                notaEnEdicion = notaEnEdicion,
                onDismiss = { showEditor = false },
                onSave = { titulo, contenido ->
                    if (titulo.isBlank() || contenido.isBlank()) {
                        Toast.makeText(context, "Debes escribir título y nota", Toast.LENGTH_SHORT).show()
                        false
                    } else {
                        dbExecutor.execute {
                            val existing = notaEnEdicion
                            if (existing == null) {
                                repository.insertar(titulo.trim(), contenido.trim())
                            } else {
                                repository.actualizar(
                                    id = existing.id,
                                    titulo = titulo.trim(),
                                    nota = contenido.trim(),
                                    fechaCreacionOriginal = existing.fechaCreacion
                                )
                            }

                            activity?.runOnUiThread {
                                showEditor = false
                                recargarNotas()
                            }
                        }

                        true
                    }
                }
            )
        }

        notaAEliminar?.let { target ->
            LegacyDeleteNotaDialog(
                nota = target,
                onDismiss = { notaAEliminar = null },
                onConfirm = { nota ->
                    dbExecutor.execute {
                        repository.eliminar(nota)
                        activity?.runOnUiThread {
                            notaAEliminar = null
                            recargarNotas()
                            Toast.makeText(context, "Nota eliminada", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }

    @Composable
    private fun LegacyNotaEditorDialog(
        notaEnEdicion: NotaEntity?,
        onDismiss: () -> Unit,
        onSave: (String, String) -> Boolean
    ) {
        var titulo by remember(notaEnEdicion?.id) { mutableStateOf(notaEnEdicion?.titulo.orEmpty()) }
        var nota by remember(notaEnEdicion?.id) { mutableStateOf(notaEnEdicion?.nota.orEmpty()) }

        Dialog(onDismissRequest = onDismiss) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorResource(id = R.color.color_base), RoundedCornerShape(16.dp))
                    .padding(10.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = titulo,
                        onValueChange = { titulo = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = colorResource(id = R.color.nota_title)
                        ),
                        placeholder = {
                            Text(
                                text = getString(R.string.titulo),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = nota,
                        onValueChange = { nota = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = colorResource(id = R.color.light_blue_50)
                        ),
                        placeholder = {
                            Text(
                                text = getString(R.string.nota),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.padding(end = 50.dp)
                        ) {
                            Text(getString(R.string.cerrar))
                        }
                        Button(
                            onClick = {
                                val saved = onSave(titulo, nota)
                                if (saved) onDismiss()
                            }
                        ) {
                            Text(getString(R.string.guardar))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun LegacyDeleteNotaDialog(
        nota: NotaEntity,
        onDismiss: () -> Unit,
        onConfirm: (NotaEntity) -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Eliminar nota") },
            text = { Text("¿Seguro que quieres eliminar '${nota.titulo}'?") },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(getString(R.string.cancelar))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onConfirm(nota)
                    onDismiss()
                }) {
                    Text(getString(R.string.eliminar))
                }
            }
        )
    }

    @Composable
    private fun NotaRow(
        nota: NotaEntity,
        fechaTexto: String,
        onEdit: () -> Unit,
        onDelete: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                .background(Color(0xFF5C5B5B), RoundedCornerShape(20.dp))
                .clickable(onClick = onEdit)
                .padding(12.dp)
        ) {
            Text(text = nota.titulo, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                text = nota.nota,
                fontSize = 16.sp,
                maxLines = 2,
                color = Color.White,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = fechaTexto,
                fontSize = 12.sp,
                color = Color(0xFFE0E0E0),
                modifier = Modifier.padding(top = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_edit_note),
                    contentDescription = "Editar nota",
                    tint = colorResource(id = R.color.shared_social),
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(onClick = onEdit)
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_delete),
                    contentDescription = "Eliminar nota",
                    tint = colorResource(id = R.color.fav_active),
                    modifier = Modifier
                        .padding(start = 20.dp)
                        .size(22.dp)
                        .clickable(onClick = onDelete)
                )
            }
        }
    }
}
