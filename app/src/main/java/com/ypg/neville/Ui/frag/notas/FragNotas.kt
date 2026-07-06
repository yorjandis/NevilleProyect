package com.ypg.neville.ui.frag

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.Fragment
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.backup.BackupRestoreSignal
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import com.ypg.neville.model.db.room.NotaChecklistCodec
import com.ypg.neville.model.db.room.NotaChecklistItem
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

    private val dbExecutor = Executors.newSingleThreadExecutor()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private var screenRefreshTick by mutableStateOf(0)

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

    override fun onResume() {
        super.onResume()
        screenRefreshTick++
    }

    @Composable
    private fun NotasScreen() {
        val context = LocalContext.current
        val notas = remember { mutableStateListOf<NotaEntity>() }
        val restoreTick by BackupRestoreSignal.restoreTick.collectAsState()
        val refreshTick = screenRefreshTick

        var notaEnEdicion by remember { mutableStateOf<NotaEntity?>(null) }
        var notaAEliminar by remember { mutableStateOf<NotaEntity?>(null) }
        var categoriaARenombrar by remember { mutableStateOf<String?>(null) }
        var categoriaAMover by remember { mutableStateOf<String?>(null) }
        var categoriaAEliminar by remember { mutableStateOf<String?>(null) }
        var showEditor by remember { mutableStateOf(false) }
        var notaExpandidaId by remember { mutableStateOf<Long?>(null) }
        var modoLista by remember { mutableStateOf(NotasListMode.TODAS) }
        val categoriasPlegadas = remember { mutableStateListOf<String>() }

        var showFabMenu by remember { mutableStateOf(false) }
        var showFilterPanel by remember { mutableStateOf(false) }

        var filtroTitulo by remember { mutableStateOf("") }
        var filtroContenido by remember { mutableStateOf("") }
        var filtroCategoria by remember { mutableStateOf("") }
        var filtroFav by remember { mutableStateOf(FavoritoFiltro.TODAS) }

        fun recargarNotas() {
            dbExecutor.execute {
                val data = notaRepository().obtenerTodas()
                activity?.runOnUiThread {
                    notas.clear()
                    notas.addAll(data)
                }
            }
        }

        LaunchedEffect(Unit) {
            recargarNotas()
        }

        LaunchedEffect(restoreTick) {
            if (restoreTick > 0L) {
                recargarNotas()
            }
        }

        LaunchedEffect(refreshTick) {
            recargarNotas()
        }

        val notasFiltradas = notas.filter { nota ->
            val cumpleTitulo = filtroTitulo.isBlank() ||
                nota.titulo.contains(filtroTitulo.trim(), ignoreCase = true)

            val cumpleContenido = filtroContenido.isBlank() ||
                buildNotaSearchText(nota).contains(filtroContenido.trim(), ignoreCase = true)

            val cumpleFav = when (filtroFav) {
                FavoritoFiltro.TODAS -> true
                FavoritoFiltro.SOLO_FAVORITAS -> nota.isFav
                FavoritoFiltro.SOLO_NO_FAVORITAS -> !nota.isFav
            }

            val cumpleCategoria = filtroCategoria.isBlank() ||
                nombreCategoria(nota).contains(filtroCategoria.trim(), ignoreCase = true)

            cumpleTitulo && cumpleContenido && cumpleCategoria && cumpleFav
        }
        val categoriasExistentes = notas
            .map { it.categoria.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
        val notasAgrupadas = notasFiltradas
            .groupBy(::nombreCategoria)
            .toList()
            .sortedWith(
                compareBy<Pair<String, List<NotaEntity>>> {
                    if (it.first == SIN_CATEGORIA) 1 else 0
                }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.first }
            )

        if (notaExpandidaId != null && notasFiltradas.none { it.id == notaExpandidaId }) {
            notaExpandidaId = null
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                       colors = listOf(
                           Color(0xFFB3D7F0),
                           Color(0xFFC3DBEB),
                           Color(0xFFDDEBFF)
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
                            contentPadding = PaddingValues(bottom = if (showFilterPanel) 230.dp else 90.dp)
                        ) {
                            if (modoLista == NotasListMode.TODAS) {
                                items(notasFiltradas, key = { it.id }) { nota ->
                                    NotaListItem(
                                        nota = nota,
                                        isExpanded = notaExpandidaId == nota.id,
                                        onExpandChange = {
                                            notaExpandidaId = if (notaExpandidaId == nota.id) null else nota.id
                                        },
                                        onEdit = {
                                            notaEnEdicion = nota
                                            showEditor = true
                                        },
                                        onDelete = { notaAEliminar = nota },
                                        onRenameCategory = { categoriaARenombrar = it },
                                        onMoveCategory = { categoriaAMover = it },
                                        onDeleteCategory = { categoriaAEliminar = it },
                                        onReload = ::recargarNotas
                                    )
                                }
                            } else {
                                notasAgrupadas.forEach { (categoria, notasCategoria) ->
                                    item(key = "categoria-$categoria") {
                                        CategoryHeader(
                                            categoria = categoria,
                                            cantidad = notasCategoria.size,
                                            isCollapsed = categoria in categoriasPlegadas,
                                            onToggle = {
                                                if (categoria in categoriasPlegadas) {
                                                    categoriasPlegadas.remove(categoria)
                                                } else {
                                                    categoriasPlegadas.add(categoria)
                                                }
                                            },
                                            onRename = { categoriaARenombrar = categoria },
                                            onDelete = { categoriaAEliminar = categoria }
                                        )
                                    }
                                    if (categoria !in categoriasPlegadas) {
                                        items(notasCategoria, key = { it.id }) { nota ->
                                            NotaListItem(
                                                nota = nota,
                                                isExpanded = notaExpandidaId == nota.id,
                                                onExpandChange = {
                                                    notaExpandidaId = if (notaExpandidaId == nota.id) null else nota.id
                                                },
                                                onEdit = {
                                                    notaEnEdicion = nota
                                                    showEditor = true
                                                },
                                                onDelete = { notaAEliminar = nota },
                                                onRenameCategory = { categoriaARenombrar = it },
                                                onMoveCategory = { categoriaAMover = it },
                                                onDeleteCategory = { categoriaAEliminar = it },
                                                onReload = ::recargarNotas
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = if (showFilterPanel) 190.dp else 20.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnimatedVisibility(
                    visible = showFabMenu,
                    enter = fadeIn(animationSpec = tween(durationMillis = 150)) + scaleIn(
                        initialScale = 0.92f,
                        transformOrigin = TransformOrigin(1f, 1f),
                        animationSpec = tween(durationMillis = 220)
                    ),
                    exit = fadeOut(animationSpec = tween(durationMillis = 120)) + scaleOut(
                        targetScale = 0.92f,
                        transformOrigin = TransformOrigin(1f, 1f),
                        animationSpec = tween(durationMillis = 180)
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                            label = if (modoLista == NotasListMode.TODAS) "Por categorías" else "Todas las notas",
                            iconRes = if (modoLista == NotasListMode.TODAS) R.drawable.ic_folder else R.drawable.ic_list,
                            onClick = {
                                showFabMenu = false
                                modoLista = if (modoLista == NotasListMode.TODAS) {
                                    categoriasPlegadas.clear()
                                    categoriasPlegadas.addAll(notasAgrupadas.map { it.first })
                                    NotasListMode.POR_CATEGORIAS
                                } else {
                                    NotasListMode.TODAS
                                }
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
                }

                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                    containerColor = Color(0xFF6879A5),
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
                    filtroCategoria = filtroCategoria,
                    onFiltroCategoriaChange = { filtroCategoria = it },
                    filtroFav = filtroFav,
                    onFiltroFavChange = { filtroFav = it },
                    onClear = {
                        filtroTitulo = ""
                        filtroContenido = ""
                        filtroCategoria = ""
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
                categoriasExistentes = categoriasExistentes,
                onDismiss = { showEditor = false },
                onSave = { titulo, contenido, isFav, categoria, isChecklist, checklistJson ->
                    val checklistItems = NotaChecklistCodec.decode(checklistJson)
                    if (titulo.isBlank()) {
                        Toast.makeText(context, "Debes escribir un título", Toast.LENGTH_SHORT).show()
                        false
                    } else if (!isChecklist && contenido.isBlank()) {
                        Toast.makeText(context, "Debes escribir el contenido de la nota", Toast.LENGTH_SHORT).show()
                        false
                    } else if (isChecklist && checklistItems.isEmpty()) {
                        Toast.makeText(context, "Añade al menos un elemento al checklist", Toast.LENGTH_SHORT).show()
                        false
                    } else {
                        dbExecutor.execute {
                            val existing = notaEnEdicion
                            if (existing == null) {
                                notaRepository().insertar(
                                    titulo.trim(),
                                    contenido.trim(),
                                    isFav,
                                    categoria.trim(),
                                    isChecklist,
                                    checklistJson
                                )
                            } else {
                                notaRepository().actualizar(
                                    id = existing.id,
                                    titulo = titulo.trim(),
                                    nota = contenido.trim(),
                                    fechaCreacionOriginal = existing.fechaCreacion,
                                    isFav = isFav,
                                    categoria = categoria.trim(),
                                    isChecklist = isChecklist,
                                    checklistJson = checklistJson
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

        categoriaARenombrar?.let { categoria ->
            RenameCategoryDialog(
                categoria = categoria,
                onDismiss = { categoriaARenombrar = null },
                onRename = { nuevaCategoria ->
                    val normalizada = nuevaCategoria.trim()
                    dbExecutor.execute {
                        val afectadas = notas.filter { nombreCategoria(it) == categoria }
                        afectadas.forEach { notaRepository().cambiarCategoria(it, normalizada) }
                        activity?.runOnUiThread {
                            categoriasPlegadas.remove(categoria)
                            categoriasPlegadas.add(normalizada.ifEmpty { SIN_CATEGORIA })
                            categoriaARenombrar = null
                            recargarNotas()
                            Toast.makeText(
                                context,
                                "${afectadas.size} nota(s) actualizada(s)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
        }

        categoriaAMover?.let { categoria ->
            MoveCategoryDialog(
                categoria = categoria,
                categoriasExistentes = categoriasExistentes.filterNot {
                    it.equals(categoria, ignoreCase = true)
                },
                onDismiss = { categoriaAMover = null },
                onMove = { destino ->
                    val normalizada = destino.trim()
                    dbExecutor.execute {
                        val afectadas = notas.filter { nombreCategoria(it) == categoria }
                        afectadas.forEach { notaRepository().cambiarCategoria(it, normalizada) }
                        activity?.runOnUiThread {
                            categoriasPlegadas.remove(categoria)
                            categoriasPlegadas.add(normalizada.ifEmpty { SIN_CATEGORIA })
                            categoriaAMover = null
                            recargarNotas()
                            Toast.makeText(
                                context,
                                "${afectadas.size} nota(s) movida(s)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
        }

        categoriaAEliminar?.let { categoria ->
            AlertDialog(
                onDismissRequest = { categoriaAEliminar = null },
                title = { Text("Eliminar categoría") },
                text = { Text("¿Eliminar todas las notas de “$categoria”? Esta acción no se puede deshacer.") },
                dismissButton = {
                    TextButton(onClick = { categoriaAEliminar = null }) {
                        Text(getString(R.string.cancelar))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        dbExecutor.execute {
                            val afectadas = notas.filter { nombreCategoria(it) == categoria }
                            afectadas.forEach(notaRepository()::eliminar)
                            activity?.runOnUiThread {
                                categoriasPlegadas.remove(categoria)
                                categoriaAEliminar = null
                                recargarNotas()
                                Toast.makeText(
                                    context,
                                    "${afectadas.size} nota(s) eliminada(s)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }) {
                        Text(getString(R.string.eliminar))
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
                            notaRepository().eliminar(target)
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

    @Composable
    private fun NotaListItem(
        nota: NotaEntity,
        isExpanded: Boolean,
        onExpandChange: () -> Unit,
        onEdit: () -> Unit,
        onDelete: () -> Unit,
        onRenameCategory: (String) -> Unit,
        onMoveCategory: (String) -> Unit,
        onDeleteCategory: (String) -> Unit,
        onReload: () -> Unit
    ) {
        val context = LocalContext.current
        NotaRow(
            nota = nota,
            isExpanded = isExpanded,
            fechaTexto = "Creado: ${dateFormat.format(Date(nota.fechaCreacion))} | Modificado: ${dateFormat.format(Date(nota.fechaModificacion))}",
            onEdit = onEdit,
            onDelete = onDelete,
            onRenameCategory = onRenameCategory,
            onMoveCategory = onMoveCategory,
            onDeleteCategory = onDeleteCategory,
            onToggleExpand = onExpandChange,
            onToggleFav = {
                dbExecutor.execute {
                    notaRepository().cambiarFavorito(nota.id, !nota.isFav)
                    activity?.runOnUiThread(onReload)
                }
            },
            onToggleChecklistItem = { item ->
                val updatedJson = NotaChecklistCodec.encode(
                    NotaChecklistCodec.decode(nota.checklistJson).map {
                        if (it.id == item.id) it.copy(checked = !it.checked) else it
                    }
                )
                dbExecutor.execute {
                    notaRepository().actualizar(
                        id = nota.id,
                        titulo = nota.titulo,
                        nota = nota.nota,
                        fechaCreacionOriginal = nota.fechaCreacion,
                        isFav = nota.isFav,
                        categoria = nota.categoria,
                        isChecklist = nota.isChecklist,
                        checklistJson = updatedJson
                    )
                    activity?.runOnUiThread(onReload)
                }
            },
            onExportToFrases = {
                                        val frase = buildNotaPayload(nota).trim().ifBlank { nota.titulo.trim() }
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

    private fun buildNotaPayload(nota: NotaEntity): String {
        val titulo = nota.titulo.trim()
        val contenido = nota.nota.trim()
        val checklist = if (nota.isChecklist) {
            NotaChecklistCodec.decode(nota.checklistJson)
                .joinToString("\n") { item ->
                    "${if (item.checked) "[x]" else "[ ]"} ${item.text}"
                }
                .trim()
        } else {
            ""
        }
        return when {
            titulo.isNotBlank() && contenido.isNotBlank() && checklist.isNotBlank() -> "$titulo\n\n$contenido\n\n$checklist"
            titulo.isNotBlank() && checklist.isNotBlank() -> "$titulo\n\n$checklist"
            titulo.isNotBlank() && contenido.isNotBlank() -> "$titulo\n\n$contenido"
            contenido.isNotBlank() && checklist.isNotBlank() -> "$contenido\n\n$checklist"
            contenido.isNotBlank() -> contenido
            checklist.isNotBlank() -> checklist
            else -> titulo
        }
    }

    private fun buildNotaSearchText(nota: NotaEntity): String =
        buildString {
            append(nota.nota)
            if (nota.isChecklist) {
                append('\n')
                NotaChecklistCodec.decode(nota.checklistJson).forEach { appendLine(it.text) }
            }
        }

    private fun notaRepository(): NotaRepository {
        val db = NevilleRoomDatabase.getInstance(requireContext().applicationContext)
        return NotaRepository(db.notaDao())
    }

    @Composable
    private fun NotaFilterPanel(
        filtroTitulo: String,
        onFiltroTituloChange: (String) -> Unit,
        filtroContenido: String,
        onFiltroContenidoChange: (String) -> Unit,
        filtroCategoria: String,
        onFiltroCategoriaChange: (String) -> Unit,
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

            OutlinedTextField(
                value = filtroCategoria,
                onValueChange = onFiltroCategoriaChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Buscar en categoría", color = Color.White) },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.Gray
                )
            )

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
        categoriasExistentes: List<String>,
        onDismiss: () -> Unit,
        onSave: (String, String, Boolean, String, Boolean, String) -> Boolean
    ) {
        var titulo by remember(notaEnEdicion?.id) { mutableStateOf(notaEnEdicion?.titulo.orEmpty()) }
        var nota by remember(notaEnEdicion?.id) { mutableStateOf(notaEnEdicion?.nota.orEmpty()) }
        var categoria by remember(notaEnEdicion?.id) { mutableStateOf(notaEnEdicion?.categoria.orEmpty()) }
        var isFav by remember(notaEnEdicion?.id) { mutableStateOf(notaEnEdicion?.isFav ?: false) }
        var isChecklist by remember(notaEnEdicion?.id) { mutableStateOf(notaEnEdicion?.isChecklist ?: false) }
        val checklistItems = remember(notaEnEdicion?.id) {
            mutableStateListOf<NotaChecklistItem>().apply {
                addAll(NotaChecklistCodec.decode(notaEnEdicion?.checklistJson.orEmpty()))
            }
        }
        var showCategoryMenu by remember { mutableStateOf(false) }

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 720.dp)
                        .verticalScroll(rememberScrollState()),
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

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = categoria,
                            onValueChange = { categoria = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            label = { Text("Categoría", color = Color.White) },
                            placeholder = { Text(SIN_CATEGORIA, color = Color.White.copy(alpha = 0.7f)) },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(
                                    onClick = { showCategoryMenu = true },
                                    enabled = categoriasExistentes.isNotEmpty()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = "Categorías existentes",
                                        tint = Color.White
                                    )
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(SIN_CATEGORIA) },
                                onClick = {
                                    categoria = ""
                                    showCategoryMenu = false
                                }
                            )
                            categoriasExistentes.forEach { existente ->
                                DropdownMenuItem(
                                    text = { Text(existente) },
                                    onClick = {
                                        categoria = existente
                                        showCategoryMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NoteTypeChip(
                            label = "Texto",
                            selected = !isChecklist,
                            icon = Icons.Default.CheckBoxOutlineBlank,
                            onClick = { isChecklist = false },
                            modifier = Modifier.weight(1f)
                        )
                        NoteTypeChip(
                            label = "Checklist",
                            selected = isChecklist,
                            icon = Icons.Default.CheckBox,
                            onClick = { isChecklist = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

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
                        placeholder = {
                            Text(
                                if (isChecklist) "Descripción opcional del checklist" else "Escribe el contenido de tu nota",
                                color = Color.White
                            )
                        }
                    )

                    if (isChecklist) {
                        ChecklistEditorSection(
                            items = checklistItems,
                            onAdd = {
                                checklistItems.add(
                                    NotaChecklistItem(
                                        id = "item-${System.currentTimeMillis()}-${checklistItems.size}",
                                        text = "",
                                        checked = false
                                    )
                                )
                            },
                            onTextChange = { item, text ->
                                val index = checklistItems.indexOfFirst { it.id == item.id }
                                if (index >= 0) checklistItems[index] = item.copy(text = text)
                            },
                            onCheckedChange = { item, checked ->
                                val index = checklistItems.indexOfFirst { it.id == item.id }
                                if (index >= 0) checklistItems[index] = item.copy(checked = checked)
                            },
                            onDelete = { item ->
                                checklistItems.removeAll { it.id == item.id }
                            }
                        )
                    }

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
                        Button(
                            onClick = {
                                onSave(
                                    titulo,
                                    nota,
                                    isFav,
                                    categoria,
                                    isChecklist,
                                    NotaChecklistCodec.encode(checklistItems)
                                )
                            }
                        ) {
                            Text(stringResource(id = R.string.guardar))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun NoteTypeChip(
        label: String,
        selected: Boolean,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Row(
            modifier = modifier
                .background(
                    if (selected) Color(0xFFD2E4EE) else Color(0x55334C63),
                    RoundedCornerShape(12.dp)
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) Color(0xFF11232E) else Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                color = if (selected) Color(0xFF11232E) else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    @Composable
    private fun ChecklistEditorSection(
        items: List<NotaChecklistItem>,
        onAdd: () -> Unit,
        onTextChange: (NotaChecklistItem, String) -> Unit,
        onCheckedChange: (NotaChecklistItem, Boolean) -> Unit,
        onDelete: (NotaChecklistItem) -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x55334C63), RoundedCornerShape(14.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Checklist",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                IconButton(onClick = onAdd, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Añadir elemento",
                        tint = Color.White
                    )
                }
            }

            if (items.isEmpty()) {
                Text(
                    text = "Añade el primer elemento",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 14.sp
                )
            }

            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Checkbox(
                        checked = item.checked,
                        onCheckedChange = { onCheckedChange(item, it) }
                    )
                    OutlinedTextField(
                        value = item.text,
                        onValueChange = { onTextChange(item, it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("Elemento", color = Color.White.copy(alpha = 0.7f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    IconButton(onClick = { onDelete(item) }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Eliminar elemento",
                            tint = Color(0xFFFFA336)
                        )
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
        onRenameCategory: (String) -> Unit,
        onMoveCategory: (String) -> Unit,
        onDeleteCategory: (String) -> Unit,
        onToggleExpand: () -> Unit,
        onToggleFav: () -> Unit,
        onToggleChecklistItem: (NotaChecklistItem) -> Unit,
        onExportToFrases: () -> Unit,
        onExportToLienzo: () -> Unit,
        onGenerateQr: () -> Unit,
        onShare: () -> Unit,
        onCopyToClipboard: () -> Unit
    ) {
        var showContextMenu by remember(nota.id) { mutableStateOf(false) }
        var showCategoryMenu by remember(nota.id, nota.categoria) { mutableStateOf(false) }
        val checklistItems = remember(nota.checklistJson) { NotaChecklistCodec.decode(nota.checklistJson) }
        val categoriaTexto = nombreCategoria(nota)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF163E6D),
                            Color(0xFF205879),
                            Color(0xFF466082)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
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
                    tint = if (nota.isFav) Color(0xFFFF7A00) else Color(0xFF0B2F55),
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(onClick = onToggleFav)
                )
            }

            if (isExpanded && nota.nota.isNotBlank()) {
                Text(
                    text = nota.nota,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    overflow = TextOverflow.Clip,
                    color = Color.White,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth()
                        .clickable(onClick = onToggleExpand)
                )
            }

            if (isExpanded && nota.isChecklist) {
                ChecklistPreview(
                    items = checklistItems,
                    isExpanded = isExpanded,
                    onToggleExpand = onToggleExpand,
                    onToggleItem = onToggleChecklistItem
                )
            }

            Box(modifier = Modifier.padding(top = 6.dp)) {
                Text(
                    text = categoriaTexto,
                    fontSize = 12.sp,
                    color = Color(0xFFD9E8F2),
                    modifier = Modifier
                        .background(Color(0x55334C63), RoundedCornerShape(10.dp))
                        .clickable { showCategoryMenu = true }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
                DropdownMenu(
                    expanded = showCategoryMenu,
                    onDismissRequest = { showCategoryMenu = false },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Editar categoría") },
                        onClick = {
                            showCategoryMenu = false
                            onRenameCategory(categoriaTexto)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Mover a categoría existente") },
                        onClick = {
                            showCategoryMenu = false
                            onMoveCategory(categoriaTexto)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar notas de esta categoría") },
                        onClick = {
                            showCategoryMenu = false
                            onDeleteCategory(categoriaTexto)
                        }
                    )
                }
            }

            if (isExpanded) {
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
                            .size(24.dp)
                            .clickable(onClick = onEdit)
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete),
                        contentDescription = "Eliminar nota",
                        tint = Color(0xFFFFA336),
                        modifier = Modifier
                            .padding(start = 20.dp)
                            .size(24.dp)
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
                                tint = Color.White,
                                modifier = Modifier
                                    .size(24.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showContextMenu,
                            onDismissRequest = { showContextMenu = false },
                            shape = RoundedCornerShape(18.dp)
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (isExpanded) 2.dp else 0.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isExpanded) R.drawable.ic_arriba else R.drawable.ic_abajo
                    ),
                    contentDescription = if (isExpanded) "Colapsar nota" else "Expandir nota",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(onClick = onToggleExpand)
                )
            }
        }
    }

    @Composable
    private fun ChecklistPreview(
        items: List<NotaChecklistItem>,
        isExpanded: Boolean,
        onToggleExpand: () -> Unit,
        onToggleItem: (NotaChecklistItem) -> Unit
    ) {
        val visibleItems = if (isExpanded) items else items.take(3)
        val canCollapseExpand = items.size > 3
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            visibleItems.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = canCollapseExpand,
                            onClick = onToggleExpand
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = item.checked,
                        onCheckedChange = { onToggleItem(item) }
                    )
                    Text(
                        text = item.text,
                        fontSize = 15.sp,
                        lineHeight = 18.sp,
                        color = Color.White,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                        overflow = if (isExpanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                enabled = canCollapseExpand,
                                onClick = onToggleExpand
                            )
                    )
                }
            }
            if (canCollapseExpand) {
                Row(
                    modifier = Modifier
                        .padding(start = 48.dp)
                        .clickable(onClick = onToggleExpand),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Mostrar menos" else "+${items.size - visibleItems.size} más",
                        color = Color(0xFFD9E8F2),
                        fontSize = 12.sp
                    )
                    Icon(
                        painter = painterResource(
                            id = if (isExpanded) R.drawable.ic_arriba else R.drawable.ic_abajo
                        ),
                        contentDescription = if (isExpanded) "Colapsar checklist" else "Expandir checklist",
                        tint = Color(0xFFD9E8F2),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }

    private enum class FavoritoFiltro(val label: String) {
        TODAS("Todas"),
        SOLO_FAVORITAS("Favoritas"),
        SOLO_NO_FAVORITAS("No favoritas")
    }

    private enum class NotasListMode {
        TODAS,
        POR_CATEGORIAS
    }

    private fun nombreCategoria(nota: NotaEntity): String =
        nota.categoria.trim().ifEmpty { SIN_CATEGORIA }

    @Composable
    private fun CategoryHeader(
        categoria: String,
        cantidad: Int,
        isCollapsed: Boolean,
        onToggle: () -> Unit,
        onRename: () -> Unit,
        onDelete: () -> Unit
    ) {
        var showCategoryMenu by remember(categoria) { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xCC27465D), RoundedCornerShape(16.dp))
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isCollapsed) Icons.Default.Folder else Icons.Default.FolderOpen,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = categoria,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Box {
                IconButton(
                    onClick = { showCategoryMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opciones de categoría",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = showCategoryMenu,
                    onDismissRequest = { showCategoryMenu = false },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Renombrar categoría") },
                        onClick = {
                            showCategoryMenu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar categoría") },
                        onClick = {
                            showCategoryMenu = false
                            onDelete()
                        }
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = cantidad.toString(),
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp
            )
            Icon(
                imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                contentDescription = if (isCollapsed) "Mostrar notas" else "Ocultar notas",
                tint = Color.White
            )
        }
    }

    @Composable
    private fun RenameCategoryDialog(
        categoria: String,
        onDismiss: () -> Unit,
        onRename: (String) -> Unit
    ) {
        var draft by remember(categoria) {
            mutableStateOf(if (categoria == SIN_CATEGORIA) "" else categoria)
        }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Renombrar categoría") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("Categoría") },
                    placeholder = { Text(SIN_CATEGORIA) },
                    singleLine = true
                )
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(id = R.string.cancelar))
                }
            },
            confirmButton = {
                TextButton(onClick = { onRename(draft) }) {
                    Text("Actualizar")
                }
            }
        )
    }

    @Composable
    private fun MoveCategoryDialog(
        categoria: String,
        categoriasExistentes: List<String>,
        onDismiss: () -> Unit,
        onMove: (String) -> Unit
    ) {
        var selected by remember(categoria, categoriasExistentes) {
            mutableStateOf(categoriasExistentes.firstOrNull().orEmpty())
        }
        var showMenu by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Mover categoría") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Mover las notas de \"$categoria\" a:")
                    if (categoriasExistentes.isEmpty()) {
                        Text("No hay otra categoría existente disponible.")
                    } else {
                        Box {
                            OutlinedTextField(
                                value = selected,
                                onValueChange = {},
                                label = { Text("Categoría destino") },
                                readOnly = true,
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { showMenu = true }) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = "Seleccionar categoría"
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                categoriasExistentes.forEach { categoriaDestino ->
                                    DropdownMenuItem(
                                        text = { Text(categoriaDestino) },
                                        onClick = {
                                            selected = categoriaDestino
                                            showMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(id = R.string.cancelar))
                }
            },
            confirmButton = {
                TextButton(
                    enabled = selected.isNotBlank(),
                    onClick = { onMove(selected) }
                ) {
                    Text("Mover")
                }
            }
        )
    }

    private companion object {
        const val SIN_CATEGORIA = "Sin categoría"
    }
}
