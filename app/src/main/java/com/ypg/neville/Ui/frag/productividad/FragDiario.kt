package com.ypg.neville.ui.frag

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.backup.BackupRestoreSignal
import com.ypg.neville.model.db.room.DiarioEntity
import com.ypg.neville.model.db.room.DiarioRepository
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import com.ypg.neville.model.migration.CanonicalRecord
import com.ypg.neville.model.migration.MigrationFormat
import com.ypg.neville.model.migration.MyAppMigrationService
import com.ypg.neville.model.migration.NevilleMigrationRoomBridge
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlinx.coroutines.launch

class FragDiario : Fragment() {

    private val dbExecutor = Executors.newSingleThreadExecutor()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private var screenRefreshTick by mutableStateOf(0)
    private lateinit var createSelectedMigrationExportLauncher: ActivityResultLauncher<String>
    private var pendingSelectedMigrationPassword: CharArray? = null
    private var pendingSelectedMigrationRecords: List<CanonicalRecord> = emptyList()

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createSelectedMigrationExportLauncher = registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/octet-stream")
        ) { uri ->
            val password = pendingSelectedMigrationPassword
            val records = pendingSelectedMigrationRecords
            pendingSelectedMigrationPassword = null
            pendingSelectedMigrationRecords = emptyList()
            if (uri == null || password == null) {
                password?.fill('\u0000')
                return@registerForActivityResult
            }
            lifecycleScope.launch {
                val result = MyAppMigrationService(requireContext().applicationContext)
                    .exportSelectedToUri(uri, password, records)
                password.fill('\u0000')
                result.onSuccess { export ->
                    Toast.makeText(
                        requireContext(),
                        "Exportadas ${export.countsByType.values.sum()} entrada(s) de diario",
                        Toast.LENGTH_LONG
                    ).show()
                }.onFailure { error ->
                    Toast.makeText(
                        requireContext(),
                        "Error al exportar: ${error.message ?: "desconocido"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

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
        var entradaACambiarCapitulo by remember { mutableStateOf<DiarioEntity?>(null) }
        var entradaExpandidaId by remember { mutableStateOf<Long?>(null) }
        var capituloARenombrar by remember { mutableStateOf<String?>(null) }
        var capituloAMover by remember { mutableStateOf<String?>(null) }
        var capituloAEliminar by remember { mutableStateOf<String?>(null) }
        val capitulosPlegados = remember { mutableStateListOf<String>() }
        var capitulosColapsadosInicialmente by remember { mutableStateOf(false) }
        val entradasSeleccionadas = remember { mutableStateListOf<Long>() }
        var manualSelectionMode by remember { mutableStateOf(false) }
        var showBatchEmotionPicker by remember { mutableStateOf(false) }
        var pendingBatchEmotion by remember { mutableStateOf<DiarioEmotion?>(null) }
        var showBatchChapterPicker by remember { mutableStateOf(false) }
        var showBatchDeleteConfirmation by remember { mutableStateOf(false) }
        var showSelectedExportDialog by remember { mutableStateOf(false) }
        var selectedExportPassword by remember { mutableStateOf("") }

        var titleDialogTarget by remember { mutableStateOf<DiarioEntity?>(null) }
        var titleDialogText by remember { mutableStateOf("") }

        var showEditor by remember { mutableStateOf(false) }
        var newEntryDateMillis by remember { mutableStateOf<Long?>(null) }
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
        var filtroCapitulo by remember { mutableStateOf("") }
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

        fun updateEntrada(
            target: DiarioEntity,
            newTitle: String = target.title,
            newContent: String = target.content,
            newEmotionKey: String = target.emocion,
            newChapter: String = target.capitulo,
            newFav: Boolean = target.isFav
        ) {
            dbExecutor.execute {
                diarioRepository().actualizar(
                    id = target.id,
                    title = newTitle,
                    content = newContent,
                    emocion = newEmotionKey,
                    capitulo = newChapter,
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

        fun toggleBatchSelection(entryId: Long) {
            if (entryId in entradasSeleccionadas) {
                entradasSeleccionadas.remove(entryId)
            } else {
                entradasSeleccionadas.add(entryId)
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
                filtroCapitulo.isBlank() || nombreCapitulo(entry).contains(filtroCapitulo.trim(), ignoreCase = true)
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

        val capitulosExistentes = entradas
            .map { it.capitulo.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)

        val entradasAgrupadas = filtered
            .groupBy(::nombreCapitulo)
            .toList()
            .sortedByDescending { (_, entries) ->
                entries.maxOfOrNull { if (sortMode == SortMode.CREATION) it.fecha else it.fechaM } ?: 0L
            }
        val capitulosVisibles = entradasAgrupadas.map { it.first }

        LaunchedEffect(capitulosVisibles) {
            if (!capitulosColapsadosInicialmente && capitulosVisibles.isNotEmpty()) {
                capitulosPlegados.clear()
                capitulosPlegados.addAll(capitulosVisibles)
                capitulosColapsadosInicialmente = true
            }
        }

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
                                selectedCalendarDayMillis = dayMillis
                                entradaEnEdicion = null
                                newEntryDateMillis = dayMillis
                                showEditor = true
                            }
                        }
                    )
                }

                if (manualSelectionMode) {
                    BatchSelectionBar(
                        selectedCount = entradasSeleccionadas.size,
                        onExport = {
                            selectedExportPassword = ""
                            showSelectedExportDialog = true
                        },
                        onChangeEmotion = { showBatchEmotionPicker = true },
                        onChangeChapter = { showBatchChapterPicker = true },
                        onDelete = { showBatchDeleteConfirmation = true },
                        onCancel = {
                            manualSelectionMode = false
                            entradasSeleccionadas.clear()
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
                        entradasAgrupadas.forEach { (capitulo, entradasCapitulo) ->
                            item(key = "capitulo-$capitulo") {
                                ChapterHeader(
                                    capitulo = capitulo,
                                    cantidad = entradasCapitulo.size,
                                    isCollapsed = capitulo in capitulosPlegados,
                                    onToggle = {
                                        if (capitulo in capitulosPlegados) {
                                            capitulosPlegados.remove(capitulo)
                                        } else {
                                            capitulosPlegados.add(capitulo)
                                        }
                                    },
                                    onRename = { capituloARenombrar = capitulo },
                                    onMove = { capituloAMover = capitulo },
                                    onDelete = { capituloAEliminar = capitulo }
                                )
                            }
                            if (capitulo !in capitulosPlegados) {
                                items(entradasCapitulo, key = { it.id }) { entrada ->
                                    DiarioRow(
                                        entrada = entrada,
                                        isExpanded = entradaExpandidaId == entrada.id,
                                        showEmotionMenu = emotionMenuId == entrada.id,
                                        showItemMenu = itemMenuId == entrada.id,
                                        selectionMode = manualSelectionMode,
                                        isSelected = entrada.id in entradasSeleccionadas,
                                        fechaTexto = "Modificado: ${dateFormat.format(Date(entrada.fechaM))}\nCreado: ${dateFormat.format(Date(entrada.fecha))}",
                                        onToggleSelection = { toggleBatchSelection(entrada.id) },
                                        onStartSelection = {
                                            itemMenuId = null
                                            emotionMenuId = null
                                            manualSelectionMode = true
                                            if (entrada.id !in entradasSeleccionadas) {
                                                entradasSeleccionadas.add(entrada.id)
                                            }
                                        },
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
                                        onChangeChapter = {
                                            itemMenuId = null
                                            entradaACambiarCapitulo = entrada
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
                                newEntryDateMillis = null
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
                                MainActivity.currentInstance()?.openDestinationAsSheet(R.id.frag_weekly_summary)
                            }
                        )
                        FabActionItem(
                            label = if (manualSelectionMode) "Salir selección múltiple" else "Selección múltiple",
                            iconRes = R.drawable.ic_list,
                            onClick = {
                                showFabMenu = false
                                manualSelectionMode = !manualSelectionMode
                                if (!manualSelectionMode) {
                                    entradasSeleccionadas.clear()
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
                    filtroCapitulo = filtroCapitulo,
                    onFiltroCapituloChange = { filtroCapitulo = it },
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
                        filtroCapitulo = ""
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
                capitulosExistentes = capitulosExistentes,
                onDismiss = {
                    showEditor = false
                    newEntryDateMillis = null
                },
                onSave = { title, content, emotionKey, capitulo, isFav ->
                    if (title.isBlank()) {
                        Toast.makeText(context, "Debes escribir un título", Toast.LENGTH_SHORT).show()
                        false
                    } else {
                        dbExecutor.execute {
                            val existing = entradaEnEdicion
                            if (existing == null) {
                                diarioRepository().insertar(
                                    title = title.trim(),
                                    content = content.trim().ifBlank { DEFAULT_NEW_CONTENT },
                                    emocion = emotionKey,
                                    capitulo = capitulo,
                                    isFav = isFav,
                                    fechaCreacionMillis = newEntryDateMillis ?: System.currentTimeMillis()
                                )
                            } else {
                                diarioRepository().actualizar(
                                    id = existing.id,
                                    title = title.trim(),
                                    content = content.trim(),
                                    emocion = emotionKey,
                                    capitulo = capitulo,
                                    isFav = isFav,
                                    fechaOriginal = existing.fecha
                                )
                            }
                            activity?.runOnUiThread {
                                showEditor = false
                                newEntryDateMillis = null
                                recargarEntradas()
                            }
                        }
                        true
                    }
                }
            )
        }

        if (showBatchEmotionPicker) {
            EmotionPickerDialog(
                emotions = emotions,
                onDismiss = { showBatchEmotionPicker = false },
                onSelect = { emotion ->
                    showBatchEmotionPicker = false
                    pendingBatchEmotion = emotion
                }
            )
        }

        pendingBatchEmotion?.let { emotion ->
            AlertDialog(
                onDismissRequest = { pendingBatchEmotion = null },
                title = { Text("Cambiar emoción") },
                text = {
                    Text("¿Cambiar a ${emotion.emoji} la emoción de ${entradasSeleccionadas.size} entradas seleccionadas?")
                },
                dismissButton = {
                    TextButton(onClick = { pendingBatchEmotion = null }) {
                        Text(getString(R.string.cancelar))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val selectedEntries = entradas.filter { it.id in entradasSeleccionadas }
                        dbExecutor.execute {
                            selectedEntries.forEach { entry ->
                                diarioRepository().actualizar(
                                    id = entry.id,
                                    title = entry.title,
                                    content = entry.content,
                                    emocion = emotion.key,
                                    capitulo = entry.capitulo,
                                    isFav = entry.isFav,
                                    fechaOriginal = entry.fecha
                                )
                            }
                            activity?.runOnUiThread {
                                pendingBatchEmotion = null
                                entradasSeleccionadas.clear()
                                manualSelectionMode = false
                                recargarEntradas()
                                Toast.makeText(context, "Emoción actualizada", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Text("Confirmar")
                    }
                }
            )
        }

        if (showBatchChapterPicker) {
            ChangeEntryChapterDialog(
                title = "Cambiar capítulo",
                message = "Elige un capítulo existente o escribe uno nuevo para ${entradasSeleccionadas.size} entradas seleccionadas.",
                initialChapter = "",
                capitulosExistentes = capitulosExistentes,
                onDismiss = { showBatchChapterPicker = false },
                onApply = { capitulo ->
                    val selectedEntries = entradas.filter { it.id in entradasSeleccionadas }
                    dbExecutor.execute {
                        selectedEntries.forEach { entry ->
                            diarioRepository().cambiarCapitulo(entry.id, capitulo)
                        }
                        activity?.runOnUiThread {
                            showBatchChapterPicker = false
                            entradasSeleccionadas.clear()
                            manualSelectionMode = false
                            recargarEntradas()
                            Toast.makeText(context, "Capítulo actualizado", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        if (showBatchDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showBatchDeleteConfirmation = false },
                title = { Text("Eliminar entradas") },
                text = {
                    Text("¿Seguro que quieres eliminar ${entradasSeleccionadas.size} entradas seleccionadas?")
                },
                dismissButton = {
                    TextButton(onClick = { showBatchDeleteConfirmation = false }) {
                        Text(getString(R.string.cancelar))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val selectedEntries = entradas.filter { it.id in entradasSeleccionadas }
                        dbExecutor.execute {
                            selectedEntries.forEach(diarioRepository()::eliminar)
                            activity?.runOnUiThread {
                                showBatchDeleteConfirmation = false
                                entradasSeleccionadas.clear()
                                manualSelectionMode = false
                                recargarEntradas()
                                Toast.makeText(context, "Entradas eliminadas", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Text(getString(R.string.eliminar))
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

        entradaACambiarCapitulo?.let { target ->
            ChangeEntryChapterDialog(
                title = "Cambiar capítulo",
                message = "Elige un capítulo existente o escribe uno nuevo para esta entrada.",
                initialChapter = target.capitulo,
                capitulosExistentes = capitulosExistentes,
                onDismiss = { entradaACambiarCapitulo = null },
                onApply = { capitulo ->
                    dbExecutor.execute {
                        diarioRepository().cambiarCapitulo(target.id, capitulo)
                        activity?.runOnUiThread {
                            entradaACambiarCapitulo = null
                            recargarEntradas()
                            Toast.makeText(context, "Capítulo actualizado", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        capituloARenombrar?.let { capitulo ->
            RenameChapterDialog(
                capitulo = capitulo,
                onDismiss = { capituloARenombrar = null },
                onRename = { nuevoCapitulo ->
                    val normalizado = nuevoCapitulo.trim()
                    dbExecutor.execute {
                        val afectadas = entradas.filter { nombreCapitulo(it) == capitulo }
                        afectadas.forEach { diarioRepository().cambiarCapitulo(it.id, normalizado) }
                        activity?.runOnUiThread {
                            capitulosPlegados.remove(capitulo)
                            if (normalizado.isNotEmpty()) {
                                capitulosPlegados.add(normalizado)
                            }
                            capituloARenombrar = null
                            recargarEntradas()
                            Toast.makeText(context, "Capítulo actualizado", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        capituloAMover?.let { capitulo ->
            MoveChapterDialog(
                capitulo = capitulo,
                capitulosDisponibles = capitulosExistentes.filter { it != capitulo },
                onDismiss = { capituloAMover = null },
                onMove = { destino ->
                    val normalizado = destino.trim()
                    dbExecutor.execute {
                        val afectadas = entradas.filter { nombreCapitulo(it) == capitulo }
                        afectadas.forEach { diarioRepository().cambiarCapitulo(it.id, normalizado) }
                        activity?.runOnUiThread {
                            capitulosPlegados.remove(capitulo)
                            capituloAMover = null
                            recargarEntradas()
                            Toast.makeText(context, "Entradas movidas", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        capituloAEliminar?.let { capitulo ->
            AlertDialog(
                onDismissRequest = { capituloAEliminar = null },
                title = { Text("Eliminar entradas") },
                text = { Text("¿Eliminar todas las entradas de \"$capitulo\"? Esta acción no se puede deshacer.") },
                dismissButton = {
                    TextButton(onClick = { capituloAEliminar = null }) {
                        Text(getString(R.string.cancelar))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        dbExecutor.execute {
                            val afectadas = entradas.filter { nombreCapitulo(it) == capitulo }
                            afectadas.forEach(diarioRepository()::eliminar)
                            activity?.runOnUiThread {
                                capitulosPlegados.remove(capitulo)
                                capituloAEliminar = null
                                recargarEntradas()
                                Toast.makeText(context, "Entradas eliminadas", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Text(getString(R.string.eliminar))
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

        if (showSelectedExportDialog) {
            AlertDialog(
                onDismissRequest = {
                    selectedExportPassword = ""
                    showSelectedExportDialog = false
                },
                title = { Text("Exportar entradas seleccionadas") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Se creará un archivo ${MigrationFormat.FILE_EXTENSION} solo con las entradas seleccionadas.")
                        OutlinedTextField(
                            value = selectedExportPassword,
                            onValueChange = { selectedExportPassword = it },
                            label = { Text("Contraseña del archivo") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val selected = entradas.filter { it.id in entradasSeleccionadas }
                        if (selected.isEmpty()) {
                            Toast.makeText(context, "Selecciona al menos una entrada", Toast.LENGTH_LONG).show()
                            return@TextButton
                        }
                        if (selectedExportPassword.isBlank()) {
                            Toast.makeText(context, "Introduce una contraseña", Toast.LENGTH_LONG).show()
                            return@TextButton
                        }
                        val db = NevilleRoomDatabase.getInstance(context.applicationContext)
                        pendingSelectedMigrationRecords = NevilleMigrationRoomBridge(db).exportSelectedRecords(diaryEntries = selected)
                        pendingSelectedMigrationPassword = selectedExportPassword.toCharArray()
                        selectedExportPassword = ""
                        entradasSeleccionadas.clear()
                        manualSelectionMode = false
                        showSelectedExportDialog = false
                        createSelectedMigrationExportLauncher.launch("diario-${System.currentTimeMillis()}${MigrationFormat.FILE_EXTENSION}")
                    }) {
                        Text("Exportar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        selectedExportPassword = ""
                        showSelectedExportDialog = false
                    }) {
                        Text("Cancelar")
                    }
                }
            )
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
                Row(modifier = Modifier.fillMaxWidth()) {
                    WEEKDAY_LABELS.forEach { label ->
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF4B5960),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

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
                            val todayStart = startOfDay(System.currentTimeMillis())
                            val isToday = day.dayStartMillis == todayStart
                            val isFuture = day.dayStartMillis > todayStart
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
                                        width = when {
                                            isSelected -> 3.dp
                                            isToday -> 2.dp
                                            else -> 0.dp
                                        },
                                        color = when {
                                            isSelected -> Color(0xFF020202)
                                            isToday -> Color(0xFF1976A3)
                                            else -> Color.Transparent
                                        },
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
    private fun BatchSelectionBar(
        selectedCount: Int,
        onExport: () -> Unit,
        onChangeEmotion: () -> Unit,
        onChangeChapter: () -> Unit,
        onDelete: () -> Unit,
        onCancel: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .background(Color(0xE8323A42), RoundedCornerShape(14.dp))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$selectedCount seleccionadas",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(end = 10.dp)
            )
            TextButton(onClick = onExport) {
                Text("Exportar", color = Color.White)
            }
            TextButton(onClick = onChangeEmotion) {
                Text("Emoción", color = Color.White)
            }
            TextButton(onClick = onChangeChapter) {
                Text("Capítulo", color = Color.White)
            }
            TextButton(onClick = onDelete) {
                Text("Eliminar", color = Color(0xFFFFB4AB))
            }
            TextButton(onClick = onCancel) {
                Text("Cerrar", color = Color.White)
            }
        }
    }

    @Composable
    private fun EmotionPickerDialog(
        emotions: List<DiarioEmotion>,
        onDismiss: () -> Unit,
        onSelect: (DiarioEmotion) -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Elegir emoción") },
            text = {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    emotions.forEach { emotion ->
                        Text(
                            text = emotion.emoji,
                            fontSize = 28.sp,
                            modifier = Modifier
                                .background(Color(0xFFE8E2D9), RoundedCornerShape(10.dp))
                                .clickable { onSelect(emotion) }
                                .padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(getString(R.string.cancelar))
                }
            }
        )
    }

    @Composable
    private fun DiarioFilterPanel(
        filtroTitulo: String,
        onFiltroTituloChange: (String) -> Unit,
        filtroContenido: String,
        onFiltroContenidoChange: (String) -> Unit,
        filtroCapitulo: String,
        onFiltroCapituloChange: (String) -> Unit,
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

            OutlinedTextField(
                value = filtroCapitulo,
                onValueChange = onFiltroCapituloChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                label = { Text("Buscar por capítulo", color = Color.White) },
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
        capitulosExistentes: List<String>,
        onDismiss: () -> Unit,
        onSave: (String, String, String, String, Boolean) -> Boolean
    ) {
        var titulo by remember(entradaEnEdicion?.id) { mutableStateOf(entradaEnEdicion?.title.orEmpty()) }
        var contenido by remember(entradaEnEdicion?.id) { mutableStateOf(entradaEnEdicion?.content.orEmpty()) }
        var capitulo by remember(entradaEnEdicion?.id) { mutableStateOf(entradaEnEdicion?.capitulo.orEmpty()) }
        var showChapterMenu by remember { mutableStateOf(false) }
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
                        placeholder = {
                            Text(
                                if (entradaEnEdicion == null) DEFAULT_NEW_CONTENT else "¿Qué ocurrió hoy?",
                                color = Color(0xFFE6ECEF)
                            )
                        }
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = capitulo,
                            onValueChange = { capitulo = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                            label = { Text("Capítulo", color = Color.White) },
                            placeholder = { Text("Ej. Mis recuerdos del pasado verano", color = Color(0xFFE6ECEF)) },
                            trailingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_menu_open),
                                    contentDescription = "Capítulos existentes",
                                    tint = Color.White,
                                    modifier = Modifier.clickable { showChapterMenu = true }
                                )
                            }
                        )
                        DropdownMenu(
                            expanded = showChapterMenu,
                            onDismissRequest = { showChapterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(SIN_CAPITULO) },
                                onClick = {
                                    capitulo = ""
                                    showChapterMenu = false
                                }
                            )
                            capitulosExistentes.forEach { existente ->
                                DropdownMenuItem(
                                    text = { Text(existente) },
                                    onClick = {
                                        capitulo = existente
                                        showChapterMenu = false
                                    }
                                )
                            }
                        }
                    }

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
                        Button(onClick = { onSave(titulo, contenido, emocionKey, capitulo, isFav) }) {
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
        selectionMode: Boolean,
        isSelected: Boolean,
        fechaTexto: String,
        onToggleSelection: () -> Unit,
        onStartSelection: () -> Unit,
        onToggleExpand: () -> Unit,
        onContentDoubleTap: () -> Unit,
        onTitleDoubleTap: () -> Unit,
        onToggleFav: () -> Unit,
        onToggleEmotionMenu: () -> Unit,
        onChangeEmotion: (DiarioEmotion) -> Unit,
        onToggleItemMenu: () -> Unit,
        onChangeChapter: () -> Unit,
        onEdit: () -> Unit,
        onDelete: () -> Unit
    ) {
        val emotion = DiarioEmotion.fromStored(entrada.emocion) ?: DiarioEmotion.NEUTRAL

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .background(
                    if (isSelected) Color(0xFFD7E8F0) else Color(0xFFF6F3ED),
                    RoundedCornerShape(12.dp)
                )
                .border(
                    if (isSelected) 3.dp else 1.dp,
                    if (isSelected) Color(0xFF1976A3) else Color(0x22000000),
                    RoundedCornerShape(12.dp)
                )
                .combinedClickable(
                    onClick = {
                        if (selectionMode) onToggleSelection()
                    },
                    onLongClick = onStartSelection
                )
                .padding(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Text(
                        text = emotion.emoji,
                        fontSize = 34.sp,
                        modifier = Modifier.clickable {
                            if (selectionMode) onToggleSelection() else onToggleEmotionMenu()
                        }
                    )
                    DropdownMenu(expanded = showEmotionMenu && !selectionMode, onDismissRequest = onToggleEmotionMenu) {
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
                        .combinedClickable(
                            onClick = { if (selectionMode) onToggleSelection() },
                            onDoubleClick = { if (!selectionMode) onTitleDoubleTap() },
                            onLongClick = onStartSelection
                        )
                )

                if (selectionMode) {
                    Text(
                        text = if (isSelected) "✓" else "○",
                        color = Color(0xFF0F5F85),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onToggleSelection)
                    )
                } else {
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
                            DropdownMenuItem(text = { Text("Cambiar capítulo") }, onClick = onChangeChapter)
                            DropdownMenuItem(text = { Text("Editar") }, onClick = onEdit)
                            DropdownMenuItem(text = { Text("Eliminar") }, onClick = onDelete)
                        }
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
                    .combinedClickable(
                        onClick = { if (selectionMode) onToggleSelection() else onToggleExpand() },
                        onDoubleClick = { if (!selectionMode) onContentDoubleTap() },
                        onLongClick = onStartSelection
                    )
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
                        .clickable {
                            if (selectionMode) onToggleSelection() else onToggleFav()
                        }
                )
            }
        }
    }

    @Composable
    private fun ChapterHeader(
        capitulo: String,
        cantidad: Int,
        isCollapsed: Boolean,
        onToggle: () -> Unit,
        onRename: () -> Unit,
        onMove: () -> Unit,
        onDelete: () -> Unit
    ) {
        var showChapterMenu by remember(capitulo) { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .background(Color(0xE82F3840), RoundedCornerShape(12.dp))
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isCollapsed) "▶" else "▼",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = capitulo,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = cantidad.toString(),
                color = Color(0xFFFFD58B),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            Box {
                Icon(
                    painter = painterResource(id = R.drawable.ic_menu_open),
                    contentDescription = "Opciones de capítulo",
                    tint = Color.White,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { showChapterMenu = true }
                )
                DropdownMenu(
                    expanded = showChapterMenu,
                    onDismissRequest = { showChapterMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Editar nombre del capítulo") },
                        onClick = {
                            showChapterMenu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Pasar entradas a otro capítulo") },
                        onClick = {
                            showChapterMenu = false
                            onMove()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar entradas de este capítulo") },
                        onClick = {
                            showChapterMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun RenameChapterDialog(
        capitulo: String,
        onDismiss: () -> Unit,
        onRename: (String) -> Unit
    ) {
        var draft by remember(capitulo) {
            mutableStateOf(if (capitulo == SIN_CAPITULO) "" else capitulo)
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Editar capítulo") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    label = { Text("Capítulo") }
                )
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(getString(R.string.cancelar))
                }
            },
            confirmButton = {
                TextButton(onClick = { onRename(draft) }) {
                    Text(getString(R.string.guardar))
                }
            }
        )
    }

    @Composable
    private fun ChangeEntryChapterDialog(
        title: String,
        message: String,
        initialChapter: String,
        capitulosExistentes: List<String>,
        onDismiss: () -> Unit,
        onApply: (String) -> Unit
    ) {
        var draft by remember(initialChapter) { mutableStateOf(initialChapter) }
        var showChapterMenu by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(message)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Capítulo") },
                            placeholder = { Text("Nuevo capítulo") },
                            trailingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_menu_open),
                                    contentDescription = "Capítulos existentes",
                                    modifier = Modifier.clickable { showChapterMenu = true }
                                )
                            }
                        )
                        DropdownMenu(
                            expanded = showChapterMenu,
                            onDismissRequest = { showChapterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(SIN_CAPITULO) },
                                onClick = {
                                    draft = ""
                                    showChapterMenu = false
                                }
                            )
                            capitulosExistentes.forEach { existente ->
                                DropdownMenuItem(
                                    text = { Text(existente) },
                                    onClick = {
                                        draft = existente
                                        showChapterMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(getString(R.string.cancelar))
                }
            },
            confirmButton = {
                TextButton(onClick = { onApply(draft.trim()) }) {
                    Text(getString(R.string.guardar))
                }
            }
        )
    }

    @Composable
    private fun MoveChapterDialog(
        capitulo: String,
        capitulosDisponibles: List<String>,
        onDismiss: () -> Unit,
        onMove: (String) -> Unit
    ) {
        var selected by remember(capitulo, capitulosDisponibles) {
            mutableStateOf(capitulosDisponibles.firstOrNull().orEmpty())
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Mover entradas") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Elige el capítulo de destino para las entradas de \"$capitulo\".")
                    if (capitulosDisponibles.isEmpty()) {
                        Text("No hay otro capítulo existente.")
                    } else {
                        capitulosDisponibles.forEach { destino ->
                            Text(
                                text = if (selected == destino) "✓ $destino" else destino,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (selected == destino) Color(0x223B6E8F) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selected = destino }
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(getString(R.string.cancelar))
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

        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val leadingEmpty = (dayOfWeek - Calendar.MONDAY + 7) % 7

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

    private fun nombreCapitulo(entrada: DiarioEntity): String =
        entrada.capitulo.trim().ifEmpty { SIN_CAPITULO }

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
        private const val DEFAULT_NEW_CONTENT = "Nuevo Contenido!"
        private const val SIN_CAPITULO = "Sin capítulo"
        private val WEEKDAY_LABELS = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
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
