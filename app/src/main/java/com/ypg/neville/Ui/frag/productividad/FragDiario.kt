package com.ypg.neville.ui.frag

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.backup.BackupRestoreSignal
import com.ypg.neville.model.db.room.DiarioEntity
import com.ypg.neville.model.db.room.DiarioRepository
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class FragDiario : Fragment() {

    private val dbExecutor = Executors.newSingleThreadExecutor()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private var screenRefreshTick by mutableStateOf(0)

    private val emotions = DiarioEmotion.entries
    private val ageFilters = listOf(
        AgeFilter("Todo", null),
        AgeFilter("1 semana", 7L * DAY_MS),
        AgeFilter("15 días", 15L * DAY_MS),
        AgeFilter("1 mes", 30L * DAY_MS),
        AgeFilter("3 meses", 90L * DAY_MS),
        AgeFilter("6 meses", 180L * DAY_MS),
        AgeFilter("1 año", 365L * DAY_MS)
    )

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (view as ComposeView).setContent {
            MaterialTheme {
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

    override fun onResume() {
        super.onResume()
        screenRefreshTick++
    }

    @Composable
    private fun DiarioScreen() {
        val context = LocalContext.current
        val entradas = remember { mutableStateListOf<DiarioEntity>() }
        val restoreTick by BackupRestoreSignal.restoreTick.collectAsState()
        val refreshTick = screenRefreshTick
        var authState by remember { mutableStateOf(DiarioAuthState.CHECKING) }
        var authMessage by remember { mutableStateOf("Comprobando biometría...") }

        var entradaEnEdicion by remember { mutableStateOf<DiarioEntity?>(null) }
        var entradaAEliminar by remember { mutableStateOf<DiarioEntity?>(null) }
        var entradaExpandidaId by remember { mutableStateOf<Long?>(null) }

        var titleDialogTarget by remember { mutableStateOf<DiarioEntity?>(null) }
        var titleDialogText by remember { mutableStateOf("") }

        var showEditor by remember { mutableStateOf(false) }
        var showFabMenu by remember { mutableStateOf(false) }
        var showFilterPanel by remember { mutableStateOf(false) }
        var showStats by remember { mutableStateOf(false) }

        var showCalendarPanel by remember { mutableStateOf(false) }
        var hideCalendarGrid by remember { mutableStateOf(false) }
        var currentMonthStartMillis by remember { mutableStateOf(startOfMonth(System.currentTimeMillis())) }
        var selectedCalendarDayMillis by remember { mutableStateOf<Long?>(null) }

        var sortMode by remember { mutableStateOf(SortMode.CREATION) }

        var filtroTitulo by remember { mutableStateOf("") }
        var filtroContenido by remember { mutableStateOf("") }
        var filtroEmocionKey by remember { mutableStateOf("all") }
        var filtroFav by remember { mutableStateOf(FavoritoFiltro.TODAS) }
        var filtroAntiguedad by remember { mutableStateOf(ageFilters.first()) }

        var emotionMenuId by remember { mutableStateOf<Long?>(null) }
        var itemMenuId by remember { mutableStateOf<Long?>(null) }

        fun recargarEntradas() {
            dbExecutor.execute {
                val data = diarioRepository().obtenerTodas()
                activity?.runOnUiThread {
                    entradas.clear()
                    entradas.addAll(data)
                }
            }
        }

        fun updateEntrada(target: DiarioEntity, newTitle: String = target.title, newContent: String = target.content, newEmotionKey: String = target.emocion, newFav: Boolean = target.isFav) {
            dbExecutor.execute {
                diarioRepository().actualizar(
                    id = target.id,
                    title = newTitle,
                    content = newContent,
                    emocion = newEmotionKey,
                    isFav = newFav,
                    fechaOriginal = target.fecha
                )
                activity?.runOnUiThread { recargarEntradas() }
            }
        }

        fun toggleFavorite(entryId: Long, currentFav: Boolean) {
            dbExecutor.execute {
                diarioRepository().cambiarFavorito(entryId, !currentFav)
                activity?.runOnUiThread { recargarEntradas() }
            }
        }

        LaunchedEffect(Unit) {
            recargarEntradas()
            requestDiarioBiometricAccess(
                onSuccess = {
                    authState = DiarioAuthState.AUTHORIZED
                    authMessage = ""
                },
                onUnavailable = { reason ->
                    authState = DiarioAuthState.UNAVAILABLE
                    authMessage = reason
                },
                onFailed = { reason ->
                    authState = DiarioAuthState.DENIED
                    authMessage = reason
                }
            )
        }

        LaunchedEffect(restoreTick) {
            if (restoreTick > 0L) {
                recargarEntradas()
            }
        }

        LaunchedEffect(refreshTick) {
            recargarEntradas()
        }

        val now = System.currentTimeMillis()

        val filtered = entradas
            .asSequence()
            .filter { entry ->
                selectedCalendarDayMillis == null || startOfDay(entry.fecha) == selectedCalendarDayMillis
            }
            .filter { entry ->
                filtroTitulo.isBlank() || entry.title.contains(filtroTitulo.trim(), ignoreCase = true)
            }
            .filter { entry ->
                filtroContenido.isBlank() || entry.content.contains(filtroContenido.trim(), ignoreCase = true)
            }
            .filter { entry ->
                filtroEmocionKey == "all" || DiarioEmotion.fromStored(entry.emocion)?.key == filtroEmocionKey
            }
            .filter { entry ->
                when (filtroFav) {
                    FavoritoFiltro.TODAS -> true
                    FavoritoFiltro.SOLO_FAVORITAS -> entry.isFav
                    FavoritoFiltro.SOLO_NO_FAVORITAS -> !entry.isFav
                }
            }
            .filter { entry ->
                val maxAge = filtroAntiguedad.maxAgeMillis
                maxAge == null || (now - startOfDay(entry.fecha)) <= maxAge
            }
            .toList()
            .sortedByDescending { if (sortMode == SortMode.CREATION) it.fecha else it.fechaM }

        if (entradaExpandidaId != null && filtered.none { it.id == entradaExpandidaId }) {
            entradaExpandidaId = null
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(diarioBackgroundBrush())
        ) {
            if (authState != DiarioAuthState.AUTHORIZED) {
                DiarioAuthGate(
                    state = authState,
                    message = authMessage,
                    onRetry = {
                        authState = DiarioAuthState.CHECKING
                        authMessage = "Comprobando biometría..."
                        requestDiarioBiometricAccess(
                            onSuccess = {
                                authState = DiarioAuthState.AUTHORIZED
                                authMessage = ""
                            },
                            onUnavailable = { reason ->
                                authState = DiarioAuthState.UNAVAILABLE
                                authMessage = reason
                            },
                            onFailed = { reason ->
                                authState = DiarioAuthState.DENIED
                                authMessage = reason
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                )
                return@Box
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "Diario",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_calendar_toggle),
                        contentDescription = if (showCalendarPanel) "Ocultar calendario" else "Mostrar calendario",
                        tint = if (showCalendarPanel) Color(0xFF0F4C6E) else Color(0xFF1F2D36),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(24.dp)
                            .clickable {
                                showCalendarPanel = !showCalendarPanel
                                if (!showCalendarPanel) selectedCalendarDayMillis = null
                            }
                    )
                    if (selectedCalendarDayMillis != null) {
                        TextButton(onClick = { selectedCalendarDayMillis = null }) {
                            Text("Quitar Selección")
                        }
                    }
                }

                if (showCalendarPanel) {
                    DiarioCalendarPanel(
                        currentMonthStartMillis = currentMonthStartMillis,
                        monthText = monthFormat.format(Date(currentMonthStartMillis)).replaceFirstChar { it.uppercase() },
                        hideCalendarGrid = hideCalendarGrid,
                        selectedDayMillis = selectedCalendarDayMillis,
                        entries = entradas,
                        onToggleHide = { hideCalendarGrid = !hideCalendarGrid },
                        onPrevMonth = { currentMonthStartMillis = addMonth(currentMonthStartMillis, -1) },
                        onNextMonth = { currentMonthStartMillis = addMonth(currentMonthStartMillis, 1) },
                        onToday = {
                            currentMonthStartMillis = startOfMonth(System.currentTimeMillis())
                            selectedCalendarDayMillis = null
                        },
                        onSelectDay = { dayMillis ->
                            selectedCalendarDayMillis = dayMillis
                        },
                        onDoubleTapDay = { dayMillis ->
                            val today = startOfDay(System.currentTimeMillis())
                            if (dayMillis > today) {
                                Toast.makeText(context, "No se puede crear una entrada en una fecha futura", Toast.LENGTH_SHORT).show()
                            } else {
                                dbExecutor.execute {
                                    diarioRepository().insertar(
                                        title = "Título",
                                        content = "Nuevo Contenido!",
                                        emocion = DiarioEmotion.NEUTRAL.key,
                                        isFav = false,
                                        fechaCreacionMillis = dayMillis
                                    )
                                    activity?.runOnUiThread {
                                        selectedCalendarDayMillis = dayMillis
                                        recargarEntradas()
                                    }
                                }
                            }
                        }
                    )
                }

                if (entradas.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay entradas todavía", fontSize = 18.sp)
                    }
                } else if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay entradas que coincidan", fontSize = 16.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = if (showFilterPanel) 230.dp else 96.dp)
                    ) {
                        items(filtered, key = { it.id }) { entrada ->
                            DiarioRow(
                                entrada = entrada,
                                isExpanded = entradaExpandidaId == entrada.id,
                                showEmotionMenu = emotionMenuId == entrada.id,
                                showItemMenu = itemMenuId == entrada.id,
                                fechaTexto = "Modificado: ${dateFormat.format(Date(entrada.fechaM))}\nCreado: ${dateFormat.format(Date(entrada.fecha))}",
                                onToggleExpand = {
                                    entradaExpandidaId = if (entradaExpandidaId == entrada.id) null else entrada.id
                                },
                                onContentDoubleTap = {
                                    entradaEnEdicion = entrada
                                    showEditor = true
                                },
                                onTitleDoubleTap = {
                                    titleDialogTarget = entrada
                                    titleDialogText = entrada.title
                                },
                                onToggleFav = {
                                    toggleFavorite(entrada.id, entrada.isFav)
                                },
                                onToggleEmotionMenu = {
                                    emotionMenuId = if (emotionMenuId == entrada.id) null else entrada.id
                                },
                                onChangeEmotion = { emotion ->
                                    emotionMenuId = null
                                    updateEntrada(target = entrada, newEmotionKey = emotion.key)
                                },
                                onToggleItemMenu = {
                                    itemMenuId = if (itemMenuId == entrada.id) null else entrada.id
                                },
                                onEdit = {
                                    itemMenuId = null
                                    entradaEnEdicion = entrada
                                    showEditor = true
                                },
                                onDelete = {
                                    itemMenuId = null
                                    entradaAEliminar = entrada
                                }
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = if (showFilterPanel) 160.dp else 20.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnimatedVisibility(
                    visible = showFabMenu,
                    enter = expandVertically(
                        expandFrom = Alignment.Bottom,
                        animationSpec = tween(durationMillis = 260)
                    ) + fadeIn(animationSpec = tween(durationMillis = 220)),
                    exit = shrinkVertically(
                        shrinkTowards = Alignment.Bottom,
                        animationSpec = tween(durationMillis = 220)
                    ) + fadeOut(animationSpec = tween(durationMillis = 180))
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FabActionItem(
                            label = "Nueva entrada",
                            iconRes = R.drawable.ic_note_add,
                            onClick = {
                                showFabMenu = false
                                entradaEnEdicion = null
                                showEditor = true
                            }
                        )
                        FabActionItem(
                            label = "Estadísticas",
                            iconRes = R.drawable.ic_item,
                            onClick = {
                                showFabMenu = false
                                showStats = true
                            }
                        )
                        FabActionItem(
                            label = "Resumen semanal",
                            iconRes = R.drawable.ic_calendar_toggle,
                            onClick = {
                                showFabMenu = false
                                runCatching { findNavController().navigate(R.id.frag_weekly_summary) }
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
                        FabActionItem(
                            label = if (sortMode == SortMode.CREATION) "Orden: creación" else "Orden: modificación",
                            iconRes = R.drawable.ic_refress,
                            onClick = {
                                showFabMenu = false
                                sortMode = if (sortMode == SortMode.CREATION) SortMode.MODIFICATION else SortMode.CREATION
                            }
                        )
                    }
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
                    filtroEmocionKey = filtroEmocionKey,
                    emociones = emotions,
                    onFiltroEmocionKeyChange = { filtroEmocionKey = it },
                    filtroFav = filtroFav,
                    onFiltroFavChange = { filtroFav = it },
                    filtroAntiguedad = filtroAntiguedad,
                    filtrosAntiguedad = ageFilters,
                    onFiltroAntiguedadChange = { filtroAntiguedad = it },
                    onClear = {
                        filtroTitulo = ""
                        filtroContenido = ""
                        filtroEmocionKey = "all"
                        filtroFav = FavoritoFiltro.TODAS
                        filtroAntiguedad = ageFilters.first()
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
                emociones = emotions,
                onDismiss = { showEditor = false },
                onSave = { title, content, emotionKey, isFav ->
                    if (title.isBlank() || content.isBlank()) {
                        Toast.makeText(context, "Debes escribir título y contenido", Toast.LENGTH_SHORT).show()
                        false
                    } else {
                        dbExecutor.execute {
                            val existing = entradaEnEdicion
                            if (existing == null) {
                                diarioRepository().insertar(
                                    title = title.trim(),
                                    content = content.trim(),
                                    emocion = emotionKey,
                                    isFav = isFav
                                )
                            } else {
                                diarioRepository().actualizar(
                                    id = existing.id,
                                    title = title.trim(),
                                    content = content.trim(),
                                    emocion = emotionKey,
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

        titleDialogTarget?.let { target ->
            AlertDialog(
                onDismissRequest = { titleDialogTarget = null },
                title = { Text("Modificar título") },
                text = {
                    OutlinedTextField(
                        value = titleDialogText,
                        onValueChange = { titleDialogText = it },
                        singleLine = true,
                        label = { Text("Título") }
                    )
                },
                dismissButton = {
                    TextButton(onClick = { titleDialogTarget = null }) {
                        Text(getString(R.string.cancelar))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val newTitle = titleDialogText.trim()
                        if (newTitle.isNotEmpty()) {
                            updateEntrada(target = target, newTitle = newTitle)
                        }
                        titleDialogTarget = null
                    }) {
                        Text(getString(R.string.guardar))
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
                            diarioRepository().eliminar(target)
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

        if (showStats) {
            Dialog(
                onDismissRequest = { showStats = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                DiarioStatsScreen(
                    entries = entradas.toList(),
                    onClose = { showStats = false },
                    onRefresh = { recargarEntradas() }
                )
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun DiarioCalendarPanel(
        currentMonthStartMillis: Long,
        monthText: String,
        hideCalendarGrid: Boolean,
        selectedDayMillis: Long?,
        entries: List<DiarioEntity>,
        onToggleHide: () -> Unit,
        onPrevMonth: () -> Unit,
        onNextMonth: () -> Unit,
        onToday: () -> Unit,
        onSelectDay: (Long) -> Unit,
        onDoubleTapDay: (Long) -> Unit
    ) {
        val monthDays = generateMonthDays(currentMonthStartMillis, entries)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .background(Color(0xFFF3F0EA), RoundedCornerShape(16.dp))
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onToggleHide) {
                    Text(if (hideCalendarGrid) "Mostrar" else "Ocultar")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onPrevMonth) { Text("◀") }
                    Text(monthText, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = onNextMonth) { Text("▶") }
                }
                TextButton(onClick = onToday) { Text("Hoy") }
            }

            if (!hideCalendarGrid) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(monthDays.size) { index ->
                        val day = monthDays[index]
                        if (day == null) {
                            Box(modifier = Modifier.size(40.dp))
                        } else {
                            val isSelected = selectedDayMillis != null && selectedDayMillis == day.dayStartMillis
                            val isFuture = day.dayStartMillis > startOfDay(System.currentTimeMillis())
                            //Circulo del dia
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .combinedClickable(
                                        onClick = { onSelectDay(day.dayStartMillis) },
                                        onDoubleClick = { onDoubleTapDay(day.dayStartMillis) }
                                    )
                                    .background(
                                        if (day.count > 0) Color(0xAA454545) else Color.Transparent,
                                        CircleShape
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) Color(0xFF020202) else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                //Texto del día del mes
                                Text(
                                    text = day.dayNumber,
                                    color = if (day.count > 0) Color.White else if (isFuture) Color(
                                        0xFF5D5E55
                                    ) else Color.Black,
                                    fontSize = 14.sp
                                )
                                if (day.count > 0) {
                                    //texto del contador de entradas:
                                    Text(
                                        text = day.count.toString(),
                                        color = Color(0xFFFFD58B),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .offset(y = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun DiarioFilterPanel(
        filtroTitulo: String,
        onFiltroTituloChange: (String) -> Unit,
        filtroContenido: String,
        onFiltroContenidoChange: (String) -> Unit,
        filtroEmocionKey: String,
        emociones: List<DiarioEmotion>,
        onFiltroEmocionKeyChange: (String) -> Unit,
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
                .background(Color(0xF6323A42), RoundedCornerShape(20.dp))
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
                    TextButton(onClick = onClear) { Text("Limpiar", fontSize = 12.sp, color = Color.White) }
                    TextButton(onClick = onHide) { Text("Ocultar", fontSize = 12.sp, color = Color.White) }
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
                    shape = RoundedCornerShape(18.dp),
                    label = { Text("Buscar en Título", color = Color.White) },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.9f),
                        cursorColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = filtroContenido,
                    onValueChange = onFiltroContenidoChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    label = { Text("Buscar en texto", color = Color.White) },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.9f),
                        cursorColor = Color.White
                    )
                )
            }

            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    label = "Todas",
                    selected = filtroEmocionKey == "all",
                    onClick = { onFiltroEmocionKeyChange("all") }
                )
                emociones.forEach { emotion ->
                    FilterChip(
                        label = emotion.emoji,
                        selected = filtroEmocionKey == emotion.key,
                        onClick = { onFiltroEmocionKeyChange(emotion.key) }
                    )
                }
            }

            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FavoritoFiltro.entries.forEach { option ->
                    FilterChip(
                        label = option.label,
                        selected = filtroFav == option,
                        onClick = { onFiltroFavChange(option) }
                    )
                }
            }

            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
    private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier
                .background(
                    if (selected) Color(0x884E8AA7) else Color(0x664E5E68),
                    RoundedCornerShape(12.dp)
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }

    @Composable
    private fun FabActionItem(label: String, iconRes: Int, onClick: () -> Unit) {
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
        emociones: List<DiarioEmotion>,
        onDismiss: () -> Unit,
        onSave: (String, String, String, Boolean) -> Boolean
    ) {
        var titulo by remember(entradaEnEdicion?.id) { mutableStateOf(entradaEnEdicion?.title.orEmpty()) }
        var contenido by remember(entradaEnEdicion?.id) { mutableStateOf(entradaEnEdicion?.content.orEmpty()) }
        var emocionKey by remember(entradaEnEdicion?.id) {
            mutableStateOf(DiarioEmotion.fromStored(entradaEnEdicion?.emocion)?.key ?: DiarioEmotion.NEUTRAL.key)
        }
        var isFav by remember(entradaEnEdicion?.id) { mutableStateOf(entradaEnEdicion?.isFav ?: false) }

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .background(Brush.verticalGradient(
                        listOf(
                            Color(0xFF292F3A),
                            Color(0xFF2A3440),
                            Color(0xFF343840)
                        )
                    ), RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = titulo,
                        onValueChange = { titulo = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(id = R.color.nota_title)
                        ),
                        placeholder = { Text("Título de la entrada", color = Color(0xFFE6ECEF)) },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = contenido,
                        onValueChange = { contenido = it },
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 20.sp,
                            color = colorResource(id = R.color.light_blue_50)
                        ),
                        placeholder = { Text("¿Qué ocurrió hoy?", color = Color(0xFFE6ECEF)) }
                    )

                    Text(text = "Emoción", fontWeight = FontWeight.SemiBold, color = Color.White)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        emociones.forEach { emotion ->
                            val selected = emocionKey == emotion.key
                            Text(
                                text = emotion.emoji,
                                fontSize = 24.sp,
                                modifier = Modifier
                                    .background(
                                        if (selected) Color(0xFFC4C89D) else Color(0x66585858),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { emocionKey = emotion.key }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_toolbar_favorite),
                            contentDescription = "Favorito",
                            tint = if (isFav) Color(0xFFFF9800) else colorResource(id = R.color.fav_inactive),
                            modifier = Modifier.size(22.dp)
                        )
                        TextButton(onClick = { isFav = !isFav }) {
                            Text(if (isFav) "Quitar favorito" else "Marcar favorito", color = Color.Yellow)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Button(onClick = onDismiss, modifier = Modifier.padding(end = 50.dp)) {
                            Text(getString(R.string.cerrar))
                        }
                        Button(onClick = { onSave(titulo, contenido, emocionKey, isFav) }) {
                            Text(getString(R.string.guardar))
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun DiarioRow(
        entrada: DiarioEntity,
        isExpanded: Boolean,
        showEmotionMenu: Boolean,
        showItemMenu: Boolean,
        fechaTexto: String,
        onToggleExpand: () -> Unit,
        onContentDoubleTap: () -> Unit,
        onTitleDoubleTap: () -> Unit,
        onToggleFav: () -> Unit,
        onToggleEmotionMenu: () -> Unit,
        onChangeEmotion: (DiarioEmotion) -> Unit,
        onToggleItemMenu: () -> Unit,
        onEdit: () -> Unit,
        onDelete: () -> Unit
    ) {
        val emotion = DiarioEmotion.fromStored(entrada.emocion) ?: DiarioEmotion.NEUTRAL

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .background(Color(0xFFF6F3ED), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0x22000000), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Text(
                        text = emotion.emoji,
                        fontSize = 34.sp,
                        modifier = Modifier.clickable(onClick = onToggleEmotionMenu)
                    )
                    DropdownMenu(expanded = showEmotionMenu, onDismissRequest = onToggleEmotionMenu) {
                        DiarioEmotion.entries.forEach { emo ->
                            DropdownMenuItem(
                                text = { Text("${emo.label} ${emo.emoji}") },
                                onClick = { onChangeEmotion(emo) }
                            )
                        }
                    }
                }

                Text(
                    text = entrada.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f)
                        .combinedClickable(onClick = {}, onDoubleClick = onTitleDoubleTap)
                )

                Box {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_menu_open),
                        contentDescription = "Opciones",
                        tint = Color.Black,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(onClick = onToggleItemMenu)
                    )
                    DropdownMenu(expanded = showItemMenu, onDismissRequest = onToggleItemMenu) {
                        DropdownMenuItem(text = { Text("Editar") }, onClick = onEdit)
                        DropdownMenuItem(text = { Text("Eliminar") }, onClick = onDelete)
                    }
                }
            }

            Text(
                text = entrada.content,
                fontSize = 18.sp,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222),
                maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                overflow = if (isExpanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .combinedClickable(onClick = onToggleExpand, onDoubleClick = onContentDoubleTap)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = fechaTexto,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = Color(0xFF4D4D4D),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_toolbar_favorite),
                    contentDescription = "Favorito",
                    tint = if (entrada.isFav) Color(0xFFD32F2F) else Color(0xFF888888),
                    modifier = Modifier.size(24.dp)
                        .clickable(onClick = onToggleFav)
                )
            }
        }
    }

    private fun startOfDay(timeMillis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun startOfMonth(timeMillis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeMillis
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun addMonth(monthStart: Long, delta: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = monthStart
        cal.add(Calendar.MONTH, delta)
        return startOfMonth(cal.timeInMillis)
    }

    private fun generateMonthDays(monthStart: Long, entries: List<DiarioEntity>): List<CalendarDay?> {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = monthStart

        val firstDayWeek = calendar.firstDayOfWeek
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val leadingEmpty = (dayOfWeek - firstDayWeek + 7) % 7

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val totalDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val countsByDay = entries
            .asSequence()
            .filter {
                val c = Calendar.getInstance()
                c.timeInMillis = it.fecha
                c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month
            }
            .groupingBy { startOfDay(it.fecha) }
            .eachCount()

        val list = mutableListOf<CalendarDay?>()
        repeat(leadingEmpty) { list.add(null) }

        for (day in 1..totalDays) {
            val dayCal = Calendar.getInstance()
            dayCal.timeInMillis = monthStart
            dayCal.set(Calendar.DAY_OF_MONTH, day)
            val dayStart = startOfDay(dayCal.timeInMillis)
            list.add(
                CalendarDay(
                    dayStartMillis = dayStart,
                    dayNumber = day.toString(),
                    count = countsByDay[dayStart] ?: 0
                )
            )
        }

        return list
    }

    @Composable
    private fun DiarioAuthGate(
        state: DiarioAuthState,
        message: String,
        onRetry: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Diario Protegido",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D2B34)
                )
                Text(
                    text = message,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF2F3E47)
                )
                if (state != DiarioAuthState.UNAVAILABLE) {
                    Button(onClick = onRetry) {
                        Text("Reintentar biometría")
                    }
                }
            }
        }
    }

    private fun requestDiarioBiometricAccess(
        onSuccess: () -> Unit,
        onUnavailable: (String) -> Unit,
        onFailed: (String) -> Unit
    ) {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK

        val biometricManager = BiometricManager.from(requireContext())
        when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                onUnavailable("Este dispositivo no dispone de biometría para acceder al Diario.")
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                onUnavailable("La biometría no está disponible en este momento.")
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                onUnavailable("No hay biometría configurada en el dispositivo. Actívala para acceder al Diario.")
            BiometricManager.BIOMETRIC_SUCCESS -> {
                val executor = ContextCompat.getMainExecutor(requireContext())
                val prompt = BiometricPrompt(
                    this,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            onSuccess()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            onFailed(errString.toString())
                        }

                        override fun onAuthenticationFailed() {
                            onFailed("No se pudo verificar la identidad. Inténtalo de nuevo.")
                        }
                    }
                )

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Acceso al Diario")
                    .setSubtitle("Bienvenido al Diario. Usa biometría para acceder.")
                    .setAllowedAuthenticators(authenticators)
                    .setNegativeButtonText("Cancelar")
                    .build()

                prompt.authenticate(promptInfo)
            }
            else -> onUnavailable("No se pudo inicializar la biometría en este dispositivo.")
        }
    }

    private fun diarioRepository(): DiarioRepository {
        val db = NevilleRoomDatabase.getInstance(requireContext().applicationContext)
        return DiarioRepository(db.diarioDao())
    }

    private data class CalendarDay(
        val dayStartMillis: Long,
        val dayNumber: String,
        val count: Int
    )

    private data class AgeFilter(
        val label: String,
        val maxAgeMillis: Long?
    )

    private enum class FavoritoFiltro(val label: String) {
        TODAS("Todas"),
        SOLO_FAVORITAS("Favoritas"),
        SOLO_NO_FAVORITAS("No favoritas")
    }

    private enum class SortMode {
        CREATION,
        MODIFICATION
    }

    private enum class DiarioAuthState {
        CHECKING,
        AUTHORIZED,
        DENIED,
        UNAVAILABLE
    }

    private enum class DiarioEmotion(val key: String, val label: String, val emoji: String) {
        FELIZ("feliz", "Feliz", "😊"),
        TRISTE("triste", "Triste", "🥺"),
        ENFADADO("enfadado", "Enfadado", "😤"),
        DESANIMADO("desanimado", "Desanimado", "😔"),
        SORPRESA("sorpresa", "Sorpresa", "😮"),
        DISTRAIDO("distraido", "Distraído", "🙄"),
        NEUTRAL("neutral", "Neutral", "🙂"),
        ENAMORADO("enamorado", "Enamorado", "🥰"),
        ENFERMO("enfermo", "Enfermo", "🤒"),
        PENSATIVO("pensativo", "Pensativo", "🤔"),
        FESTIVO("festivo", "Festivo", "🥳");

        companion object {
            fun fromStored(stored: String?): DiarioEmotion? {
                if (stored.isNullOrBlank()) return NEUTRAL
                val normalized = stored.trim().lowercase(Locale.getDefault())
                return entries.firstOrNull { it.key == normalized || it.emoji == stored }
            }
        }
    }

    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000
    }
}

private fun diarioBackgroundBrush(): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE2B645),
            Color(0xFFF1B882),
            Color(0xFFB3ABA1)
        )
    )
}

//Preview del Diario
@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun DiarioFullPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(diarioBackgroundBrush())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = "Diario",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_calendar_toggle),
                    contentDescription = "Mostrar calendario",
                    tint = Color(0xFF1F2D36),
                    modifier = Modifier.size(24.dp)
                )
                TextButton(onClick = {}) {
                    Text("Quitar Selección")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .background(Color(0xFFF3F0EA), RoundedCornerShape(16.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = "Calendario (Preview)",
                    fontSize = 12.sp,
                    color = Color(0xFF465862)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .background(Color(0xFFF6F3ED), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x22000000), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("🙂", fontSize = 34.sp)
                    Text(
                        text = "Entrada de ejemplo",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .weight(1f)
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_menu_open),
                        contentDescription = "Opciones",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "Contenido de ejemplo para previsualizar colores y contraste del Diario.",
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222),
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Modificado: 08/04/2026 18:30\nCreado: 08/04/2026 10:20",
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = Color(0xFF4D4D4D),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_toolbar_favorite),
                        contentDescription = "Favorito",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {},
            containerColor = colorResource(id = R.color.light_blue_200),
            contentColor = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_menu_open),
                contentDescription = "Menú Diario"
            )
        }
    }
}
