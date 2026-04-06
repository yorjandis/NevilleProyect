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
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
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
import com.ypg.neville.model.db.room.DiarioEntity
import com.ypg.neville.model.db.room.DiarioRepository
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.absoluteValue

class FragDiario : Fragment() {

    private lateinit var repository: DiarioRepository
    private val dbExecutor = Executors.newSingleThreadExecutor()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val emocionesDisponibles = listOf("😌", "😊", "😁", "😎", "🤩", "😔", "😢", "😡", "😴", "🙏")
    private val filtrosAntiguedad = listOf(
        AgeFilter("Todo", null),
        AgeFilter("1 semana", 7L * 24 * 60 * 60 * 1000),
        AgeFilter("15 días", 15L * 24 * 60 * 60 * 1000),
        AgeFilter("1 mes", 30L * 24 * 60 * 60 * 1000),
        AgeFilter("3 meses", 90L * 24 * 60 * 60 * 1000),
        AgeFilter("6 meses", 180L * 24 * 60 * 60 * 1000),
        AgeFilter("1 año", 365L * 24 * 60 * 60 * 1000)
    )

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
        repository = DiarioRepository(db.diarioDao())

        (view as ComposeView).setContent {
            com.ypg.neville.ui.theme.NevilleTheme {
                DiarioScreen()
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
    private fun DiarioScreen() {
        val context = LocalContext.current
        val entradas = remember { mutableStateListOf<DiarioEntity>() }

        var entradaEnEdicion by remember { mutableStateOf<DiarioEntity?>(null) }
        var entradaAEliminar by remember { mutableStateOf<DiarioEntity?>(null) }
        var showEditor by remember { mutableStateOf(false) }
        var entradaExpandidaId by remember { mutableStateOf<Long?>(null) }
        var showFabMenu by remember { mutableStateOf(false) }
        var showFilterPanel by remember { mutableStateOf(false) }
        var filtroTitulo by remember { mutableStateOf("") }
        var filtroContenido by remember { mutableStateOf("") }
        var filtroEmocion by remember { mutableStateOf("Todas") }
        var filtroFav by remember { mutableStateOf(FavoritoFiltro.TODAS) }
        var filtroAntiguedad by remember { mutableStateOf(filtrosAntiguedad.first()) }

        fun recargarEntradas() {
            dbExecutor.execute {
                val data = repository.obtenerTodas()
                activity?.runOnUiThread {
                    entradas.clear()
                    entradas.addAll(data)
                }
            }
        }

        LaunchedEffect(Unit) {
            recargarEntradas()
        }

        val ahora = System.currentTimeMillis()
        val entradasFiltradas = entradas.filter { entrada ->
            val cumpleTitulo = filtroTitulo.isBlank() ||
                entrada.title.contains(filtroTitulo.trim(), ignoreCase = true)

            val cumpleContenido = filtroContenido.isBlank() ||
                entrada.content.contains(filtroContenido.trim(), ignoreCase = true)

            val cumpleEmocion = filtroEmocion == "Todas" || entrada.emocion == filtroEmocion

            val cumpleFav = when (filtroFav) {
                FavoritoFiltro.TODAS -> true
                FavoritoFiltro.SOLO_FAVORITAS -> entrada.isFav
                FavoritoFiltro.SOLO_NO_FAVORITAS -> !entrada.isFav
            }

            val maxAgeMillis = filtroAntiguedad.maxAgeMillis
            val cumpleAntiguedad = maxAgeMillis == null ||
                (ahora - entrada.fecha).absoluteValue <= maxAgeMillis

            cumpleTitulo && cumpleContenido && cumpleEmocion && cumpleFav && cumpleAntiguedad
        }

        if (entradaExpandidaId != null && entradasFiltradas.none { it.id == entradaExpandidaId }) {
            entradaExpandidaId = null
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Diario",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 8.dp)
                )

                if (entradas.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay entradas todavía", fontSize = 18.sp)
                    }
                } else {
                    if (entradasFiltradas.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No hay entradas que coincidan con los filtros", fontSize = 16.sp, color = Color(0xFFD7D7D7))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = if (showFilterPanel) 210.dp else 90.dp)
                        ) {
                            items(entradasFiltradas, key = { it.id }) { entrada ->
                                DiarioRow(
                                    entrada = entrada,
                                    isExpanded = entradaExpandidaId == entrada.id,
                                    fechaTexto = "Creación: ${dateFormat.format(Date(entrada.fecha))} | Modificación: ${dateFormat.format(Date(entrada.fechaM))}",
                                    onEdit = {
                                        entradaEnEdicion = entrada
                                        showEditor = true
                                    },
                                    onDelete = { entradaAEliminar = entrada },
                                    onToggleExpand = {
                                        entradaExpandidaId = if (entradaExpandidaId == entrada.id) null else entrada.id
                                    },
                                    onToggleFav = {
                                        dbExecutor.execute {
                                            repository.cambiarFavorito(entrada.id, !entrada.isFav)
                                            activity?.runOnUiThread { recargarEntradas() }
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
                    .padding(end = 20.dp, bottom = if (showFilterPanel) 150.dp else 20.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (showFabMenu) {
                    FabActionItem(
                        label = "Crear entrada",
                        iconRes = R.drawable.ic_note_add,
                        onClick = {
                            showFabMenu = false
                            entradaEnEdicion = null
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
                    containerColor = colorResource(id = R.color.light_blue_200),
                    contentColor = Color.Black
                ) {
                    Icon(
                        painter = painterResource(id = if (showFabMenu) R.drawable.ic_abajo else R.drawable.ic_menu_open),
                        contentDescription = "Menú Diario"
                    )
                }
            }

            if (showFilterPanel) {
                DiarioFilterPanel(
                    filtroTitulo = filtroTitulo,
                    onFiltroTituloChange = { filtroTitulo = it },
                    filtroContenido = filtroContenido,
                    onFiltroContenidoChange = { filtroContenido = it },
                    filtroEmocion = filtroEmocion,
                    emocionesDisponibles = emocionesDisponibles,
                    onFiltroEmocionChange = { filtroEmocion = it },
                    filtroFav = filtroFav,
                    onFiltroFavChange = { filtroFav = it },
                    filtroAntiguedad = filtroAntiguedad,
                    filtrosAntiguedad = filtrosAntiguedad,
                    onFiltroAntiguedadChange = { filtroAntiguedad = it },
                    onClear = {
                        filtroTitulo = ""
                        filtroContenido = ""
                        filtroEmocion = "Todas"
                        filtroFav = FavoritoFiltro.TODAS
                        filtroAntiguedad = filtrosAntiguedad.first()
                    },
                    onHide = { showFilterPanel = false },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                )
            }
        }

        if (showEditor) {
            DiarioEditorDialog(
                entradaEnEdicion = entradaEnEdicion,
                emocionesDisponibles = emocionesDisponibles,
                onDismiss = { showEditor = false },
                onSave = { title, content, emocion, isFav ->
                    if (title.isBlank() || content.isBlank()) {
                        Toast.makeText(context, "Debes escribir título y contenido", Toast.LENGTH_SHORT).show()
                        false
                    } else {
                        dbExecutor.execute {
                            val existing = entradaEnEdicion
                            if (existing == null) {
                                repository.insertar(
                                    title = title.trim(),
                                    content = content.trim(),
                                    emocion = emocion,
                                    isFav = isFav
                                )
                            } else {
                                repository.actualizar(
                                    id = existing.id,
                                    title = title.trim(),
                                    content = content.trim(),
                                    emocion = emocion,
                                    isFav = isFav,
                                    fechaOriginal = existing.fecha
                                )
                            }

                            activity?.runOnUiThread {
                                showEditor = false
                                recargarEntradas()
                            }
                        }
                        true
                    }
                }
            )
        }

        entradaAEliminar?.let { target ->
            AlertDialog(
                onDismissRequest = { entradaAEliminar = null },
                title = { Text("Eliminar entrada") },
                text = { Text("¿Seguro que quieres eliminar '${target.title}'?") },
                dismissButton = {
                    TextButton(onClick = { entradaAEliminar = null }) {
                        Text(getString(R.string.cancelar))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        dbExecutor.execute {
                            repository.eliminar(target)
                            activity?.runOnUiThread {
                                entradaAEliminar = null
                                recargarEntradas()
                                Toast.makeText(context, "Entrada eliminada", Toast.LENGTH_SHORT).show()
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
    private fun DiarioFilterPanel(
        filtroTitulo: String,
        onFiltroTituloChange: (String) -> Unit,
        filtroContenido: String,
        onFiltroContenidoChange: (String) -> Unit,
        filtroEmocion: String,
        emocionesDisponibles: List<String>,
        onFiltroEmocionChange: (String) -> Unit,
        filtroFav: FavoritoFiltro,
        onFiltroFavChange: (FavoritoFiltro) -> Unit,
        filtroAntiguedad: AgeFilter,
        filtrosAntiguedad: List<AgeFilter>,
        onFiltroAntiguedadChange: (AgeFilter) -> Unit,
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
                    label = { Text("Título", color = Color.White) },
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
                    label = { Text("Contenido", color = Color.White) },
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

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    label = "Todas",
                    selected = filtroEmocion == "Todas",
                    onClick = { onFiltroEmocionChange("Todas") }
                )
                emocionesDisponibles.forEach { emoji ->
                    FilterChip(
                        label = emoji,
                        selected = filtroEmocion == emoji,
                        onClick = { onFiltroEmocionChange(emoji) }
                    )
                }
            }

            FlowRow(
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

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filtrosAntiguedad.forEach { age ->
                    FilterChip(
                        label = age.label,
                        selected = filtroAntiguedad == age,
                        onClick = { onFiltroAntiguedadChange(age) }
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
    private fun DiarioEditorDialog(
        entradaEnEdicion: DiarioEntity?,
        emocionesDisponibles: List<String>,
        onDismiss: () -> Unit,
        onSave: (String, String, String, Boolean) -> Boolean
    ) {
        var titulo by remember(entradaEnEdicion?.id) {
            mutableStateOf(entradaEnEdicion?.title.orEmpty())
        }
        var contenido by remember(entradaEnEdicion?.id) {
            mutableStateOf(entradaEnEdicion?.content.orEmpty())
        }
        var emocion by remember(entradaEnEdicion?.id) {
            mutableStateOf(entradaEnEdicion?.emocion ?: "😌")
        }
        var isFav by remember(entradaEnEdicion?.id) {
            mutableStateOf(entradaEnEdicion?.isFav ?: false)
        }

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false) // 👈 clave
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f) // 👈 ocupa el 95% del ancho de pantalla
                    .background(
                        colorResource(id = R.color.color_base),
                        RoundedCornerShape(16.dp)
                    )
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
                        placeholder = { Text("Título de la entrada", color = Color.White) },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = contenido,
                        onValueChange = { contenido = it },
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
                        placeholder = { Text("¿Qué ocurrió hoy?", color = Color.White) }
                    )

                    Text(
                        text = "Emoción",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        emocionesDisponibles.forEach { emoji ->
                            val selected = emocion == emoji
                            Text(
                                text = emoji,
                                fontSize = 24.sp,
                                modifier = Modifier
                                    .background(
                                        if (selected) Color(0xFF92B395) else Color(0x66585858),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { emocion = emoji }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_toolbar_favorite),
                            contentDescription = "Favorito",
                            tint = if (isFav)
                                colorResource(id = R.color.fav_active)
                            else
                                colorResource(id = R.color.fav_inactive),
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
                                onSave(titulo, contenido, emocion, isFav)
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
    private fun DiarioRow(
        entrada: DiarioEntity,
        isExpanded: Boolean,
        fechaTexto: String,
        onEdit: () -> Unit,
        onDelete: () -> Unit,
        onToggleExpand: () -> Unit,
        onToggleFav: () -> Unit
    ) {
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
                Text(text = entrada.emocion, fontSize = 24.sp)
                Text(
                    text = entrada.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp).weight(1f)
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_toolbar_favorite),
                    contentDescription = "Favorita",
                    tint = if (entrada.isFav) colorResource(id = R.color.fav_active) else colorResource(id = R.color.fav_inactive),
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(onClick = onToggleFav)
                )
            }

            Text(
                text = entrada.content,
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
                    contentDescription = "Editar entrada",
                    tint = colorResource(id = R.color.shared_social),
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(onClick = onEdit)
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_delete),
                    contentDescription = "Eliminar entrada",
                    tint = colorResource(id = R.color.fav_active),
                    modifier = Modifier
                        .padding(start = 20.dp)
                        .size(22.dp)
                        .clickable(onClick = onDelete)
                )
            }
        }
    }

    private data class AgeFilter(
        val label: String,
        val maxAgeMillis: Long?
    )

    private enum class FavoritoFiltro(val label: String) {
        TODAS("Todas"),
        SOLO_FAVORITAS("Favoritas"),
        SOLO_NO_FAVORITAS("No favoritas")
    }
}
