package com.ypg.neville.ui.frag

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.Fragment
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import com.ypg.neville.model.db.room.NotaEntity
import com.ypg.neville.model.db.room.NotaRepository
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.model.utils.FraseContextActions
import com.ypg.neville.model.utils.QRManager
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
            com.ypg.neville.ui.theme.NevilleTheme {
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
    private fun NotasScreen() {
        val context = LocalContext.current
        val notas = remember { mutableStateListOf<NotaEntity>() }

        var notaEnEdicion by remember { mutableStateOf<NotaEntity?>(null) }
        var notaAEliminar by remember { mutableStateOf<NotaEntity?>(null) }
        var showEditor by remember { mutableStateOf(false) }
        var notaExpandidaId by remember { mutableStateOf<Long?>(null) }

        var showFabMenu by remember { mutableStateOf(false) }
        var showFilterPanel by remember { mutableStateOf(false) }

        var filtroTitulo by remember { mutableStateOf("") }
        var filtroContenido by remember { mutableStateOf("") }
        var filtroFav by remember { mutableStateOf(FavoritoFiltro.TODAS) }

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

        val notasFiltradas = notas.filter { nota ->
            val cumpleTitulo = filtroTitulo.isBlank() ||
                nota.titulo.contains(filtroTitulo.trim(), ignoreCase = true)

            val cumpleContenido = filtroContenido.isBlank() ||
                nota.nota.contains(filtroContenido.trim(), ignoreCase = true)

            val cumpleFav = when (filtroFav) {
                FavoritoFiltro.TODAS -> true
                FavoritoFiltro.SOLO_FAVORITAS -> nota.isFav
                FavoritoFiltro.SOLO_NO_FAVORITAS -> !nota.isFav
            }

            cumpleTitulo && cumpleContenido && cumpleFav
        }

        if (notaExpandidaId != null && notasFiltradas.none { it.id == notaExpandidaId }) {
            notaExpandidaId = null
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                       colors = listOf(
                            Color(0xFFC1955C),
                            Color(0xFFB8AA5F),
                            Color(0xFFB8B1A8)
                        )
                    )
                )
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
                    if (notasFiltradas.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No hay notas que coincidan con los filtros", fontSize = 16.sp, color = Color(0xFFD7D7D7))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = if (showFilterPanel) 170.dp else 90.dp)
                        ) {
                            items(notasFiltradas, key = { it.id }) { nota ->
                                NotaRow(
                                    nota = nota,
                                    isExpanded = notaExpandidaId == nota.id,
                                    fechaTexto = "Creación: ${dateFormat.format(Date(nota.fechaCreacion))} | Modificación: ${dateFormat.format(Date(nota.fechaModificacion))}",
                                    onEdit = {
                                        notaEnEdicion = nota
                                        showEditor = true
                                    },
                                    onDelete = { notaAEliminar = nota },
                                    onToggleExpand = {
                                        notaExpandidaId = if (notaExpandidaId == nota.id) null else nota.id
                                    },
                                    onToggleFav = {
                                        dbExecutor.execute {
                                            repository.cambiarFavorito(nota.id, !nota.isFav)
                                            activity?.runOnUiThread { recargarNotas() }
                                        }
                                    },
                                    onExportToFrases = {
                                        val frase = nota.nota.trim().ifBlank { nota.titulo.trim() }
                                        if (frase.isBlank()) {
                                            Toast.makeText(context, "La nota está vacía", Toast.LENGTH_SHORT).show()
                                            return@NotaRow
                                        }
                                        dbExecutor.execute {
                                            val result = utilsDB.insertNewFrase(
                                                context,
                                                frase,
                                                "Notas",
                                                nota.titulo.trim(),
                                                "0"
                                            )
                                            activity?.runOnUiThread {
                                                if (result >= 0) {
                                                    Toast.makeText(context, "Nota exportada a Frases", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "No se pudo exportar a Frases (puede que ya exista)",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        }
                                    },
                                    onExportToLienzo = {
                                        FraseContextActions.cargarFraseEnLienzo(context, buildNotaPayload(nota))
                                    },
                                    onGenerateQr = {
                                        val payload = buildNotaPayload(nota)
                                        if (payload.isBlank()) {
                                            Toast.makeText(context, "La nota está vacía", Toast.LENGTH_SHORT).show()
                                        } else {
                                            QRManager.ShowQRDialog(
                                                context,
                                                payload,
                                                "Compartir Nota",
                                                "Puede utilizar el lector QR para importar notas"
                                            )
                                        }
                                    },
                                    onShare = {
                                        val payload = buildNotaPayload(nota)
                                        if (payload.isBlank()) {
                                            Toast.makeText(context, "La nota está vacía", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, payload)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Compartir nota"))
                                        }
                                    },
                                    onCopyToClipboard = {
                                        val payload = buildNotaPayload(nota)
                                        if (payload.isBlank()) {
                                            Toast.makeText(context, "La nota está vacía", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val clipboard = context.getSystemService(ClipboardManager::class.java)
                                            clipboard?.setPrimaryClip(ClipData.newPlainText("nota", payload))
                                            Toast.makeText(context, "Nota copiada al portapapeles", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = if (showFilterPanel) 130.dp else 20.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (showFabMenu) {
                    FabActionItem(
                        label = "Crear nota",
                        iconRes = R.drawable.ic_note_add,
                        onClick = {
                            showFabMenu = false
                            notaEnEdicion = null
                            showEditor = true
                        }
                    )
                    FabActionItem(
                        label = if (showFilterPanel) "Ocultar filtros" else "Mostrar filtros",
                        iconRes = R.drawable.ic_show,
                        onClick = {
                            showFabMenu = false
                            showFilterPanel = !showFilterPanel
                        }
                    )
                }

                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                    containerColor = Color(0xFF062D48),
                    contentColor = Color.White
                ) {
                    Icon(
                        painter = painterResource(id = if (showFabMenu) R.drawable.ic_abajo else R.drawable.ic_menu_open),
                        tint = Color.White,
                        contentDescription = "Menú Notas"
                    )
                }
            }

            if (showFilterPanel) {
                NotaFilterPanel(
                    filtroTitulo = filtroTitulo,
                    onFiltroTituloChange = { filtroTitulo = it },
                    filtroContenido = filtroContenido,
                    onFiltroContenidoChange = { filtroContenido = it },
                    filtroFav = filtroFav,
                    onFiltroFavChange = { filtroFav = it },
                    onClear = {
                        filtroTitulo = ""
                        filtroContenido = ""
                        filtroFav = FavoritoFiltro.TODAS
                    },
                    onHide = { showFilterPanel = false },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                )
            }
        }

        if (showEditor) {
            NotaEditorDialog(
                notaEnEdicion = notaEnEdicion,
                onDismiss = { showEditor = false },
                onSave = { titulo, contenido, isFav ->
                    if (titulo.isBlank() || contenido.isBlank()) {
                        Toast.makeText(context, "Debes escribir título y nota", Toast.LENGTH_SHORT).show()
                        false
                    } else {
                        dbExecutor.execute {
                            val existing = notaEnEdicion
                            if (existing == null) {
                                repository.insertar(titulo.trim(), contenido.trim(), isFav)
                            } else {
                                repository.actualizar(
                                    id = existing.id,
                                    titulo = titulo.trim(),
                                    nota = contenido.trim(),
                                    fechaCreacionOriginal = existing.fechaCreacion,
                                    isFav = isFav
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
            AlertDialog(
                onDismissRequest = { notaAEliminar = null },
                title = { Text("Eliminar nota") },
                text = { Text("¿Seguro que quieres eliminar '${target.titulo}'?") },
                dismissButton = {
                    TextButton(onClick = { notaAEliminar = null }) {
                        Text(getString(R.string.cancelar))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        dbExecutor.execute {
                            repository.eliminar(target)
                            activity?.runOnUiThread {
                                notaAEliminar = null
                                recargarNotas()
                                Toast.makeText(context, "Nota eliminada", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Text(getString(R.string.eliminar))
                    }
                }
            )
        }
    }

    private fun buildNotaPayload(nota: NotaEntity): String {
        val titulo = nota.titulo.trim()
        val contenido = nota.nota.trim()
        return when {
            titulo.isNotBlank() && contenido.isNotBlank() -> "$titulo\n\n$contenido"
            contenido.isNotBlank() -> contenido
            else -> titulo
        }
    }

    @Composable
    private fun NotaFilterPanel(
        filtroTitulo: String,
        onFiltroTituloChange: (String) -> Unit,
        filtroContenido: String,
        onFiltroContenidoChange: (String) -> Unit,
        filtroFav: FavoritoFiltro,
        onFiltroFavChange: (FavoritoFiltro) -> Unit,
        onClear: () -> Unit,
        onHide: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(Color(0xE61F323D), RoundedCornerShape(20.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Filtros", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row {
                    TextButton(onClick = onClear) {
                        Text("Limpiar", color = Color.White, fontSize = 14.sp)
                    }
                    TextButton(onClick = onHide) {
                        Text("Ocultar", color = Color.White, fontSize = 14.sp)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = filtroTitulo,
                    onValueChange = onFiltroTituloChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Buscar en título", color = Color.White) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.Gray
                    )
                )
                OutlinedTextField(
                    value = filtroContenido,
                    onValueChange = onFiltroContenidoChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Buscar en nota", color = Color.White) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.Gray
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FavoritoFiltro.entries.forEach { option ->
                    FilterChip(
                        label = option.label,
                        selected = filtroFav == option,
                        onClick = { onFiltroFavChange(option) }
                    )
                }
            }
        }
    }

    @Composable
    private fun FilterChip(
        label: String,
        selected: Boolean,
        onClick: () -> Unit
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFF11232E) else Color(0xFFE6EDF1),
            fontSize = 12.sp,
            modifier = Modifier
                .background(
                    if (selected) Color(0xFFD2E4EE) else Color(0x664E5E68),
                    RoundedCornerShape(12.dp)
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }

    @Composable
    private fun FabActionItem(
        label: String,
        iconRes: Int,
        onClick: () -> Unit
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                color = Color(0xFFEAF2F7),
                fontSize = 12.sp,
                modifier = Modifier
                    .background(Color(0xC2343E46), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
            FloatingActionButton(
                onClick = onClick,
                containerColor = Color(0xFFCFDAE3),
                contentColor = Color(0xFF1A2A33),
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    @Composable
    private fun NotaEditorDialog(
        notaEnEdicion: NotaEntity?,
        onDismiss: () -> Unit,
        onSave: (String, String, Boolean) -> Boolean
    ) {
        var titulo by remember(notaEnEdicion?.id) { mutableStateOf(notaEnEdicion?.titulo.orEmpty()) }
        var nota by remember(notaEnEdicion?.id) { mutableStateOf(notaEnEdicion?.nota.orEmpty()) }
        var isFav by remember(notaEnEdicion?.id) { mutableStateOf(notaEnEdicion?.isFav ?: false) }

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
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
                        shape = RoundedCornerShape(14.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start,
                            color = colorResource(id = R.color.nota_title)
                        ),
                        placeholder = { Text("Título de la nota", color = Color.White) },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = nota,
                        onValueChange = { nota = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        shape = RoundedCornerShape(14.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Start,
                            color = colorResource(id = R.color.light_blue_50)
                        ),
                        placeholder = { Text("Escribe el contenido de tu nota", color = Color.White) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_toolbar_favorite),
                            contentDescription = "Favorito",
                            tint = if (isFav)
                                Color(0xFFFF9800)
                            else
                                Color(0xFF726D5F),
                            modifier = Modifier.size(22.dp)
                        )
                        TextButton(onClick = { isFav = !isFav }) {
                            Text(if (isFav) "Quitar favorito" else "Marcar favorito", color = Color.White)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.padding(end = 50.dp)
                        ) {
                            Text(stringResource(id = R.string.cerrar))
                        }
                        Button(onClick = { onSave(titulo, nota, isFav) }) {
                            Text(stringResource(id = R.string.guardar))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun NotaRow(
        nota: NotaEntity,
        isExpanded: Boolean,
        fechaTexto: String,
        onEdit: () -> Unit,
        onDelete: () -> Unit,
        onToggleExpand: () -> Unit,
        onToggleFav: () -> Unit,
        onExportToFrases: () -> Unit,
        onExportToLienzo: () -> Unit,
        onGenerateQr: () -> Unit,
        onShare: () -> Unit,
        onCopyToClipboard: () -> Unit
    ) {
        var showContextMenu by remember(nota.id) { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                .background(Color(0xFF4B535B), RoundedCornerShape(20.dp))
                .clickable(onClick = onEdit)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = nota.titulo,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_toolbar_favorite),
                    contentDescription = "Favorita",
                    tint = if (nota.isFav) Color(0xFFFF9800) else Color(0xFF726D5F),
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(onClick = onToggleFav)
                )
            }

            Text(
                text = nota.nota,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                overflow = if (isExpanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                color = Color.White,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .then(
                        if (isExpanded) {
                            Modifier
                        } else {
                            Modifier.heightIn(min = 60.dp)
                        }
                    )
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
                    tint = Color(0xFFFF7E09),
                    modifier = Modifier
                        .padding(start = 20.dp)
                        .size(22.dp)
                        .clickable(onClick = onDelete)
                )
                Box {
                    IconButton(
                        onClick = { showContextMenu = true },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_menu_open),
                            contentDescription = "Menú contextual de nota",
                            tint = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = showContextMenu,
                        onDismissRequest = { showContextMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Exportar a Frases") },
                            onClick = {
                                showContextMenu = false
                                onExportToFrases()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Exportar a Lienzo") },
                            onClick = {
                                showContextMenu = false
                                onExportToLienzo()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Generar QR") },
                            onClick = {
                                showContextMenu = false
                                onGenerateQr()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Compartir...") },
                            onClick = {
                                showContextMenu = false
                                onShare()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copiar al portapapeles") },
                            onClick = {
                                showContextMenu = false
                                onCopyToClipboard()
                            }
                        )
                    }
                }
            }
        }
    }

    private enum class FavoritoFiltro(val label: String) {
        TODAS("Todas"),
        SOLO_FAVORITAS("Favoritas"),
        SOLO_NO_FAVORITAS("No favoritas")
    }
}
