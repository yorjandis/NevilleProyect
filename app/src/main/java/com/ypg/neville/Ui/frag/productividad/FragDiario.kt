package com.ypg.neville.ui.frag

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.res.stringResource
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
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.db.room.DiarioEntity
import com.ypg.neville.model.db.room.DiarioAttachmentRepository
import com.ypg.neville.model.db.room.DiarioRepository
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import com.ypg.neville.model.db.room.PendingDiarioAttachment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Ajustes visuales manuales de la mascota de la cabecera del Diario.
 *
 * - [menuRowHeightDp]: altura que la cabecera reserva antes del listado de tarjetas.
 * - [sizeDp]: tamaño total de la imagen.
 * - [offsetXDp]: valores positivos desplazan la mascota hacia la derecha.
 * - [offsetYDp]: valores positivos desplazan la mascota hacia abajo.
 *
 * Para evitar que la mascota se superponga al listado, aumenta [menuRowHeightDp]
 * junto con [sizeDp]. Ambos valores son independientes para permitir ajustar
 * manualmente la proporción y la posición visual.
 */
private object DiarioMascotLayout {
    const val menuRowHeightDp = 176
    const val sizeDp = 160
    const val offsetXDp = 10
    const val offsetYDp = 5
}

class FragDiario : Fragment() {

    private val dbExecutor = Executors.newSingleThreadExecutor()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private var screenRefreshTick by mutableStateOf(0)

    private val emotions = DiarioEmotion.entries
    private val ageFilters = listOf(
        AgeFilter(R.string.diary_age_all, null),
        AgeFilter(R.string.diary_age_one_week, 7L * DAY_MS),
        AgeFilter(R.string.diary_age_fifteen_days, 15L * DAY_MS),
        AgeFilter(R.string.diary_age_one_month, 30L * DAY_MS),
        AgeFilter(R.string.diary_age_three_months, 90L * DAY_MS),
        AgeFilter(R.string.diary_age_six_months, 180L * DAY_MS),
        AgeFilter(R.string.diary_age_one_year, 365L * DAY_MS)
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
        val attachmentCounts = remember { mutableStateMapOf<Long, Int>() }
        val refreshTick = screenRefreshTick
        val unknownErrorMessage = stringResource(R.string.common_unknown_error)
        val attachmentPartialSaveErrorTemplate = stringResource(
            R.string.diary_attachment_partial_save_error,
            "%s"
        )
        var authState by remember { mutableStateOf(DiarioAuthState.CHECKING) }
        var authMessage by remember { mutableStateOf(context.getString(R.string.diary_checking_biometrics)) }

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

        var titleDialogTarget by remember { mutableStateOf<DiarioEntity?>(null) }
        var titleDialogText by remember { mutableStateOf("") }

        var showEditor by remember { mutableStateOf(false) }
        var newEntryDateMillis by remember { mutableStateOf<Long?>(null) }
        var showActionsMenu by remember { mutableStateOf(false) }
        var showFiltersMenu by remember { mutableStateOf(false) }
        var showFilterPanel by remember { mutableStateOf(false) }
        var showStats by remember { mutableStateOf(false) }
        var groupedByChapter by remember { mutableStateOf(false) }

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
        var filtroSoloConAnexos by remember { mutableStateOf(false) }
        var filtroAntiguedad by remember { mutableStateOf(ageFilters.first()) }

        var emotionMenuId by remember { mutableStateOf<Long?>(null) }
        var itemMenuId by remember { mutableStateOf<Long?>(null) }

        fun recargarEntradas() {
            dbExecutor.execute {
                val data = diarioRepository().obtenerTodas()
                val counts = diarioAttachmentRepository().attachmentCounts()
                activity?.runOnUiThread {
                    entradas.clear()
                    entradas.addAll(data)
                    attachmentCounts.clear()
                    attachmentCounts.putAll(counts)
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
            .filter { entry -> !filtroSoloConAnexos || (attachmentCounts[entry.id] ?: 0) > 0 }
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

        val diaryEntryCard: @Composable (DiarioEntity) -> Unit = { entrada ->
            DiarioRow(
                entrada = entrada,
                isExpanded = entradaExpandidaId == entrada.id,
                showEmotionMenu = emotionMenuId == entrada.id,
                showItemMenu = itemMenuId == entrada.id,
                attachmentCount = attachmentCounts[entrada.id] ?: 0,
                allEntries = entradas,
                selectionMode = manualSelectionMode,
                isSelected = entrada.id in entradasSeleccionadas,
                fechaTexto = stringResource(
                    R.string.diary_dates,
                    dateFormat.format(Date(entrada.fechaM)),
                    dateFormat.format(Date(entrada.fecha))
                ),
                onToggleSelection = { toggleBatchSelection(entrada.id) },
                onStartSelection = {
                    itemMenuId = null
                    emotionMenuId = null
                    manualSelectionMode = true
                    if (entrada.id !in entradasSeleccionadas) entradasSeleccionadas.add(entrada.id)
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
                onToggleFav = { toggleFavorite(entrada.id, entrada.isFav) },
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
                },
                onAttachmentsChanged = { recargarEntradas() }
            )
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
                        authMessage = context.getString(R.string.diary_checking_biometrics)
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
                DiarioTopActions(
                    showActionsMenu = showActionsMenu,
                    onShowActionsMenuChange = { showActionsMenu = it },
                    showFiltersMenu = showFiltersMenu,
                    onShowFiltersMenuChange = { showFiltersMenu = it },
                    showCalendar = showCalendarPanel,
                    selectionMode = manualSelectionMode,
                    groupedByChapter = groupedByChapter,
                    favoritesOnly = filtroFav == FavoritoFiltro.SOLO_FAVORITAS,
                    attachmentsOnly = filtroSoloConAnexos,
                    sortMode = sortMode,
                    onStatistics = {
                        showActionsMenu = false
                        showStats = true
                    },
                    onWeeklySummary = {
                        showActionsMenu = false
                        MainActivity.currentInstance()?.openDestinationAsSheet(R.id.frag_weekly_summary)
                    },
                    onToggleSelection = {
                        showActionsMenu = false
                        manualSelectionMode = !manualSelectionMode
                        if (!manualSelectionMode) entradasSeleccionadas.clear()
                    },
                    onToggleCalendar = {
                        showActionsMenu = false
                        showCalendarPanel = !showCalendarPanel
                        if (!showCalendarPanel) selectedCalendarDayMillis = null
                    },
                    onShowAll = {
                        showFiltersMenu = false
                        groupedByChapter = false
                        filtroTitulo = ""
                        filtroContenido = ""
                        filtroCapitulo = ""
                        filtroEmocionKey = "all"
                        filtroFav = FavoritoFiltro.TODAS
                        filtroSoloConAnexos = false
                        filtroAntiguedad = ageFilters.first()
                        selectedCalendarDayMillis = null
                    },
                    onGroupByChapter = {
                        showFiltersMenu = false
                        groupedByChapter = true
                    },
                    onShowFavorites = {
                        showFiltersMenu = false
                        groupedByChapter = false
                        filtroFav = FavoritoFiltro.SOLO_FAVORITAS
                    },
                    onShowAttachments = {
                        showFiltersMenu = false
                        groupedByChapter = false
                        filtroSoloConAnexos = true
                    },
                    onDetailedFilters = {
                        showFiltersMenu = false
                        showFilterPanel = !showFilterPanel
                    },
                    onToggleSort = {
                        showFiltersMenu = false
                        sortMode = if (sortMode == SortMode.CREATION) SortMode.MODIFICATION else SortMode.CREATION
                    },
                    onNewEntry = {
                        entradaEnEdicion = null
                        newEntryDateMillis = null
                        showEditor = true
                    }
                )

                if (selectedCalendarDayMillis != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { selectedCalendarDayMillis = null }) {
                            Text(stringResource(R.string.diary_clear_date_selection), color = Color.Black)
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
                                Toast.makeText(context, context.getString(R.string.diary_future_entry_error), Toast.LENGTH_SHORT).show()
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
                        Text(stringResource(R.string.diary_no_entries), fontSize = 18.sp)
                    }
                } else if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.diary_no_matching_entries), fontSize = 16.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = if (showFilterPanel) 230.dp else 32.dp)
                    ) {
                        if (groupedByChapter) {
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
                                        diaryEntryCard(entrada)
                                    }
                                }
                            }
                        } else {
                            items(filtered, key = { it.id }) { entrada ->
                                diaryEntryCard(entrada)
                            }
                        }
                    }
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
                    filtroSoloConAnexos = filtroSoloConAnexos,
                    onFiltroSoloConAnexosChange = { filtroSoloConAnexos = it },
                    filtroAntiguedad = filtroAntiguedad,
                    filtrosAntiguedad = ageFilters,
                    onFiltroAntiguedadChange = { filtroAntiguedad = it },
                    onClear = {
                        filtroTitulo = ""
                        filtroContenido = ""
                        filtroCapitulo = ""
                        filtroEmocionKey = "all"
                        filtroFav = FavoritoFiltro.TODAS
                        filtroSoloConAnexos = false
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
                allEntries = entradas,
                onAttachmentsChanged = { recargarEntradas() },
                onDismiss = {
                    showEditor = false
                    newEntryDateMillis = null
                },
                onSave = { title, content, emotionKey, capitulo, isFav, pendingAttachments ->
                    if (title.isBlank()) {
                        Toast.makeText(context, context.getString(R.string.diary_title_required), Toast.LENGTH_SHORT).show()
                        false
                    } else {
                        dbExecutor.execute {
                            val existing = entradaEnEdicion
                            val savedId = if (existing == null) {
                                diarioRepository().insertar(
                                    title = title.trim(),
                                    content = content.trim().ifBlank { context.getString(R.string.diary_default_new_content) },
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
                                existing.id
                            }
                            var attachmentError: Throwable? = null
                            pendingAttachments.forEach { pending ->
                                runCatching {
                                    diarioAttachmentRepository().insertPending(savedId, pending)
                                }.onFailure { if (attachmentError == null) attachmentError = it }
                            }
                            activity?.runOnUiThread {
                                attachmentError?.let { error ->
                                    Toast.makeText(
                                        context,
                                        attachmentPartialSaveErrorTemplate.replace(
                                            "%s",
                                            error.message ?: unknownErrorMessage
                                        ),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
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
                title = { Text(stringResource(R.string.diary_change_emotion)) },
                text = {
                    Text(stringResource(R.string.diary_change_selected_emotion_question, emotion.emoji, entradasSeleccionadas.size))
                },
                dismissButton = {
                    TextButton(onClick = { pendingBatchEmotion = null }) {
                        Text(stringResource(R.string.common_cancel))
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
                                Toast.makeText(context, context.getString(R.string.diary_emotion_updated), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Text(stringResource(R.string.common_confirm))
                    }
                }
            )
        }

        if (showBatchChapterPicker) {
            ChangeEntryChapterDialog(
                title = stringResource(R.string.diary_change_chapter),
                message = stringResource(R.string.diary_change_selected_chapter_description, entradasSeleccionadas.size),
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
                            Toast.makeText(context, context.getString(R.string.diary_chapter_updated), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        if (showBatchDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showBatchDeleteConfirmation = false },
                title = { Text(stringResource(R.string.diary_delete_entries)) },
                text = {
                    Text(stringResource(R.string.diary_delete_selected_question, entradasSeleccionadas.size))
                },
                dismissButton = {
                    TextButton(onClick = { showBatchDeleteConfirmation = false }) {
                        Text(stringResource(R.string.common_cancel))
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
                                Toast.makeText(context, context.getString(R.string.diary_entries_deleted), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Text(stringResource(R.string.common_delete))
                    }
                }
            )
        }

        titleDialogTarget?.let { target ->
            AlertDialog(
                onDismissRequest = { titleDialogTarget = null },
                title = { Text(stringResource(R.string.diary_edit_title)) },
                text = {
                    OutlinedTextField(
                        value = titleDialogText,
                        onValueChange = { titleDialogText = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.common_title)) }
                    )
                },
                dismissButton = {
                    TextButton(onClick = { titleDialogTarget = null }) {
                        Text(stringResource(R.string.common_cancel))
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
                        Text(stringResource(R.string.common_save))
                    }
                }
            )
        }

        entradaACambiarCapitulo?.let { target ->
            ChangeEntryChapterDialog(
                title = stringResource(R.string.diary_change_chapter),
                message = stringResource(R.string.diary_change_entry_chapter_description),
                initialChapter = target.capitulo,
                capitulosExistentes = capitulosExistentes,
                onDismiss = { entradaACambiarCapitulo = null },
                onApply = { capitulo ->
                    dbExecutor.execute {
                        diarioRepository().cambiarCapitulo(target.id, capitulo)
                        activity?.runOnUiThread {
                            entradaACambiarCapitulo = null
                            recargarEntradas()
                            Toast.makeText(context, context.getString(R.string.diary_chapter_updated), Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(context, context.getString(R.string.diary_chapter_updated), Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(context, context.getString(R.string.diary_entries_moved), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        capituloAEliminar?.let { capitulo ->
            AlertDialog(
                onDismissRequest = { capituloAEliminar = null },
                title = { Text(stringResource(R.string.diary_delete_entries)) },
                text = { Text(stringResource(R.string.diary_delete_chapter_entries_question, displayChapterName(capitulo))) },
                dismissButton = {
                    TextButton(onClick = { capituloAEliminar = null }) {
                        Text(stringResource(R.string.common_cancel))
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
                                Toast.makeText(context, context.getString(R.string.diary_entries_deleted), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Text(stringResource(R.string.common_delete))
                    }
                }
            )
        }

        entradaAEliminar?.let { target ->
            AlertDialog(
                onDismissRequest = { entradaAEliminar = null },
                title = { Text(stringResource(R.string.diary_delete_entry)) },
                text = { Text(stringResource(R.string.diary_delete_entry_question, target.title)) },
                dismissButton = {
                    TextButton(onClick = { entradaAEliminar = null }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        dbExecutor.execute {
                            diarioRepository().eliminar(target)
                            activity?.runOnUiThread {
                                entradaAEliminar = null
                                recargarEntradas()
                                Toast.makeText(context, context.getString(R.string.diary_entry_deleted), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Text(stringResource(R.string.common_delete))
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
                    Text(if (hideCalendarGrid) stringResource(R.string.common_show) else stringResource(R.string.common_hide))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onPrevMonth) { Text("◀") }
                    Text(monthText, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = onNextMonth) { Text("▶") }
                }
                TextButton(onClick = onToday) { Text(stringResource(R.string.common_today)) }
            }

            if (!hideCalendarGrid) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    diaryWeekdayLabels().forEach { label ->
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
                text = stringResource(R.string.common_selected_count, selectedCount),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(end = 10.dp)
            )
            TextButton(onClick = onChangeEmotion) {
                Text(stringResource(R.string.diary_emotion), color = Color.White)
            }
            TextButton(onClick = onChangeChapter) {
                Text(stringResource(R.string.diary_chapter), color = Color.White)
            }
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.common_delete), color = Color(0xFFFFB4AB))
            }
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.common_close), color = Color.White)
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
            title = { Text(stringResource(R.string.diary_choose_emotion)) },
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
                    Text(stringResource(R.string.common_cancel))
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
        filtroSoloConAnexos: Boolean,
        onFiltroSoloConAnexosChange: (Boolean) -> Unit,
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
                Text(stringResource(R.string.diary_filters), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row {
                    TextButton(onClick = onClear) { Text(stringResource(R.string.common_clear), fontSize = 12.sp, color = Color.White) }
                    TextButton(onClick = onHide) { Text(stringResource(R.string.common_hide), fontSize = 12.sp, color = Color.White) }
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
                    label = { Text(stringResource(R.string.diary_search_title), color = Color.White) },
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
                    label = { Text(stringResource(R.string.diary_search_text), color = Color.White) },
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
                label = { Text(stringResource(R.string.diary_search_chapter), color = Color.White) },
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
                    label = stringResource(R.string.common_all),
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
                        label = option.localizedLabel(),
                        selected = filtroFav == option,
                        onClick = { onFiltroFavChange(option) }
                    )
                }
                FilterChip(
                    label = stringResource(R.string.diary_filter_with_attachments),
                    selected = filtroSoloConAnexos,
                    onClick = { onFiltroSoloConAnexosChange(!filtroSoloConAnexos) }
                )
            }

            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                filtrosAntiguedad.forEach { age ->
                    FilterChip(
                        label = stringResource(age.labelRes),
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
    private fun DiarioTopActions(
        showActionsMenu: Boolean,
        onShowActionsMenuChange: (Boolean) -> Unit,
        showFiltersMenu: Boolean,
        onShowFiltersMenuChange: (Boolean) -> Unit,
        showCalendar: Boolean,
        selectionMode: Boolean,
        groupedByChapter: Boolean,
        favoritesOnly: Boolean,
        attachmentsOnly: Boolean,
        sortMode: SortMode,
        onStatistics: () -> Unit,
        onWeeklySummary: () -> Unit,
        onToggleSelection: () -> Unit,
        onToggleCalendar: () -> Unit,
        onShowAll: () -> Unit,
        onGroupByChapter: () -> Unit,
        onShowFavorites: () -> Unit,
        onShowAttachments: () -> Unit,
        onDetailedFilters: () -> Unit,
        onToggleSort: () -> Unit,
        onNewEntry: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // Esta altura forma parte del layout: el listado comienza debajo
                // de ella y no se superpone a la mascota.
                .height(DiarioMascotLayout.menuRowHeightDp.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.peti_escribe),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(
                        x = DiarioMascotLayout.offsetXDp.dp,
                        y = DiarioMascotLayout.offsetYDp.dp
                    )
                    // requiredSize evita que la altura fija de la barra superior
                    // limite el crecimiento manual de la mascota.
                    .requiredSize(DiarioMascotLayout.sizeDp.dp)
            )

            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    DiarioTopActionButton(
                        icon = Icons.Rounded.MoreHoriz,
                        contentDescription = stringResource(R.string.diary_actions_menu),
                        onClick = { onShowActionsMenuChange(true) }
                    )
                    DropdownMenu(
                        expanded = showActionsMenu,
                        onDismissRequest = { onShowActionsMenuChange(false) }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.diary_statistics)) },
                            leadingIcon = { Icon(Icons.Rounded.BarChart, contentDescription = null) },
                            onClick = onStatistics
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.diary_weekly_summary)) },
                            leadingIcon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null) },
                            onClick = onWeeklySummary
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (selectionMode) stringResource(R.string.common_exit_multi_select)
                                    else stringResource(R.string.common_multi_select)
                                )
                            },
                            leadingIcon = { Icon(Icons.Rounded.Checklist, contentDescription = null) },
                            onClick = onToggleSelection
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (showCalendar) stringResource(R.string.common_hide_calendar)
                                    else stringResource(R.string.common_show_calendar)
                                )
                            },
                            leadingIcon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null) },
                            onClick = onToggleCalendar
                        )
                    }
                }

                Box {
                    DiarioTopActionButton(
                        icon = Icons.Rounded.FilterList,
                        contentDescription = stringResource(R.string.diary_filter_menu),
                        onClick = { onShowFiltersMenuChange(true) }
                    )
                    DropdownMenu(
                        expanded = showFiltersMenu,
                        onDismissRequest = { onShowFiltersMenuChange(false) }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.diary_all_entries)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.ViewList, contentDescription = null) },
                            onClick = onShowAll
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.diary_by_chapters)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Book,
                                    contentDescription = null,
                                    tint = if (groupedByChapter) Color(0xFFE58A00) else Color.Unspecified
                                )
                            },
                            onClick = onGroupByChapter
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.diary_favorites)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Favorite,
                                    contentDescription = null,
                                    tint = if (favoritesOnly) Color(0xFFD32F2F) else Color.Unspecified
                                )
                            },
                            onClick = onShowFavorites
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.diary_filter_with_attachments)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.AttachFile,
                                    contentDescription = null,
                                    tint = if (attachmentsOnly) Color(0xFFE58A00) else Color.Unspecified
                                )
                            },
                            onClick = onShowAttachments
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.diary_detailed_filters)) },
                            leadingIcon = { Icon(Icons.Rounded.Tune, contentDescription = null) },
                            onClick = onDetailedFilters
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (sortMode == SortMode.CREATION) stringResource(R.string.diary_sort_creation)
                                    else stringResource(R.string.diary_sort_modification)
                                )
                            },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = null) },
                            onClick = onToggleSort
                        )
                    }
                }

                DiarioTopActionButton(
                    icon = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.diary_new_entry),
                    accent = true,
                    onClick = onNewEntry
                )
            }
        }
    }

    @Composable
    private fun DiarioTopActionButton(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        contentDescription: String,
        accent: Boolean = false,
        onClick: () -> Unit
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = if (accent) Color(0xFFFF9500) else Color.White.copy(alpha = 0.78f),
                    shape = CircleShape
                )
                .border(1.dp, Color.Black.copy(alpha = 0.24f), CircleShape)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color(0xFF202629),
                modifier = Modifier.size(23.dp)
            )
        }
    }

    @Composable
    private fun DiarioEditorDialog(
        entradaEnEdicion: DiarioEntity?,
        emociones: List<DiarioEmotion>,
        capitulosExistentes: List<String>,
        allEntries: List<DiarioEntity>,
        onAttachmentsChanged: () -> Unit,
        onDismiss: () -> Unit,
        onSave: (String, String, String, String, Boolean, List<PendingDiarioAttachment>) -> Boolean
    ) {
        var titulo by remember(entradaEnEdicion?.id) { mutableStateOf(entradaEnEdicion?.title.orEmpty()) }
        var contenido by remember(entradaEnEdicion?.id) { mutableStateOf(entradaEnEdicion?.content.orEmpty()) }
        var capitulo by remember(entradaEnEdicion?.id) { mutableStateOf(entradaEnEdicion?.capitulo.orEmpty()) }
        var showChapterMenu by remember { mutableStateOf(false) }
        var emocionKey by remember(entradaEnEdicion?.id) {
            mutableStateOf(DiarioEmotion.fromStored(entradaEnEdicion?.emocion)?.key ?: DiarioEmotion.NEUTRAL.key)
        }
        var isFav by remember(entradaEnEdicion?.id) { mutableStateOf(entradaEnEdicion?.isFav ?: false) }
        val pendingAttachments = remember(entradaEnEdicion?.id) { mutableStateListOf<PendingDiarioAttachment>() }

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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 780.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = titulo,
                        onValueChange = { titulo = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(id = R.color.nota_title)
                        ),
                        placeholder = { Text(stringResource(R.string.diary_entry_title_placeholder), color = Color(0xFFE6ECEF)) },
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
                                if (entradaEnEdicion == null) stringResource(R.string.diary_new_content_placeholder) else stringResource(R.string.diary_what_happened_today),
                                color = Color(0xFFE6ECEF)
                            )
                        }
                    )

                    entradaEnEdicion?.let { existingEntry ->
                        DiaryAttachmentsSection(
                            entry = existingEntry,
                            allEntries = allEntries,
                            onChanged = onAttachmentsChanged,
                            modifier = Modifier.fillMaxWidth(),
                            showAddButton = false,
                            darkTheme = true
                        )
                    }

                    PendingDiaryAttachmentsEditor(
                        attachments = pendingAttachments,
                        onAdd = { pendingAttachments.add(it) },
                        onRemove = { id -> pendingAttachments.removeAll { it.id == id } },
                        modifier = Modifier.fillMaxWidth(),
                        showTitle = entradaEnEdicion == null
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = capitulo,
                            onValueChange = { capitulo = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                            label = { Text(stringResource(R.string.diary_chapter), color = Color.White) },
                            placeholder = { Text(stringResource(R.string.diary_chapter_example), color = Color(0xFFE6ECEF)) },
                            trailingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_menu_open),
                                    contentDescription = stringResource(R.string.diary_existing_chapters),
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
                                text = { Text(stringResource(R.string.diary_no_chapter)) },
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

                    Text(text = stringResource(R.string.diary_emotion), fontWeight = FontWeight.SemiBold, color = Color.White)
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
                            contentDescription = stringResource(R.string.diary_favorite),
                            tint = if (isFav) Color(0xFFFF9800) else colorResource(id = R.color.fav_inactive),
                            modifier = Modifier.size(22.dp)
                        )
                        TextButton(onClick = { isFav = !isFav }) {
                            Text(if (isFav) stringResource(R.string.diary_remove_favorite) else stringResource(R.string.diary_mark_favorite), color = Color.Yellow)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Button(onClick = onDismiss, modifier = Modifier.padding(end = 50.dp)) {
                            Text(stringResource(R.string.common_close))
                        }
                        Button(onClick = { onSave(titulo, contenido, emocionKey, capitulo, isFav, pendingAttachments.toList()) }) {
                            Text(stringResource(R.string.common_save))
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
        attachmentCount: Int,
        allEntries: List<DiarioEntity>,
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
        onDelete: () -> Unit,
        onAttachmentsChanged: () -> Unit
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
                                text = { Text("${emo.localizedLabel()} ${emo.emoji}") },
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

            if (isExpanded && !selectionMode) {
                DiaryAttachmentsSection(
                    entry = entrada,
                    allEntries = allEntries,
                    onChanged = onAttachmentsChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )

                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
                    color = Color.Black.copy(alpha = 0.12f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (entrada.capitulo.isNotBlank()) {
                            Text(
                                text = "${stringResource(R.string.diary_chapter)}: ${entrada.capitulo}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4D4D4D),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = fechaTexto,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = Color(0xFF4D4D4D)
                        )
                    }
                    Icon(
                        painter = painterResource(id = R.drawable.ic_toolbar_favorite),
                        contentDescription = stringResource(R.string.diary_favorite),
                        tint = if (entrada.isFav) Color(0xFFD32F2F) else Color(0xFF888888),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(onClick = onToggleFav)
                    )
                    Box(modifier = Modifier.padding(start = 10.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.MoreHoriz,
                            contentDescription = stringResource(R.string.common_options),
                            tint = Color.Black,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable(onClick = onToggleItemMenu)
                        )
                        DropdownMenu(expanded = showItemMenu, onDismissRequest = onToggleItemMenu) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.diary_change_chapter)) }, onClick = onChangeChapter)
                            DropdownMenuItem(text = { Text(stringResource(R.string.common_edit)) }, onClick = onEdit)
                            DropdownMenuItem(text = { Text(stringResource(R.string.common_delete)) }, onClick = onDelete)
                        }
                    }
                }
            }

            if (!selectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (attachmentCount > 0 && !isExpanded) {
                        Text(
                            text = "📎 $attachmentCount",
                            fontSize = 12.sp,
                            color = Color(0xFF4D4D4D),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    Text(
                        text = if (isExpanded) "⌃" else "⌄",
                        color = Color.Black.copy(alpha = 0.48f),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable(onClick = onToggleExpand)
                    )
                }
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
                text = displayChapterName(capitulo),
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
                    contentDescription = stringResource(R.string.diary_chapter_options),
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
                        text = { Text(stringResource(R.string.diary_edit_chapter_name)) },
                        onClick = {
                            showChapterMenu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.diary_move_entries_to_chapter)) },
                        onClick = {
                            showChapterMenu = false
                            onMove()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.diary_delete_chapter_entries)) },
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
            title = { Text(stringResource(R.string.diary_edit_chapter)) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.diary_chapter)) }
                )
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = { onRename(draft) }) {
                    Text(stringResource(R.string.common_save))
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
                            label = { Text(stringResource(R.string.diary_chapter)) },
                            placeholder = { Text(stringResource(R.string.diary_new_chapter)) },
                            trailingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_menu_open),
                                    contentDescription = stringResource(R.string.diary_existing_chapters),
                                    modifier = Modifier.clickable { showChapterMenu = true }
                                )
                            }
                        )
                        DropdownMenu(
                            expanded = showChapterMenu,
                            onDismissRequest = { showChapterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.diary_no_chapter)) },
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
                    Text(stringResource(R.string.common_cancel))
                }
            },
            confirmButton = {
                TextButton(onClick = { onApply(draft.trim()) }) {
                    Text(stringResource(R.string.common_save))
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
            title = { Text(stringResource(R.string.diary_move_entries)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.diary_choose_destination_chapter, displayChapterName(capitulo)))
                    if (capitulosDisponibles.isEmpty()) {
                        Text(stringResource(R.string.diary_no_other_chapter))
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
                    Text(stringResource(R.string.common_cancel))
                }
            },
            confirmButton = {
                TextButton(
                    enabled = selected.isNotBlank(),
                    onClick = { onMove(selected) }
                ) {
                    Text(stringResource(R.string.common_move))
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
                    text = stringResource(R.string.diary_protected),
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
                        Text(stringResource(R.string.diary_retry_biometrics))
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
                onUnavailable(getString(R.string.diary_no_biometric_hardware))
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                onUnavailable(getString(R.string.diary_biometrics_unavailable))
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                onUnavailable(getString(R.string.diary_biometrics_not_configured))
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
                            onFailed(getString(R.string.diary_identity_verification_failed))
                        }
                    }
                )

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(getString(R.string.diary_access_title))
                    .setSubtitle(getString(R.string.diary_access_subtitle))
                    .setAllowedAuthenticators(authenticators)
                    .setNegativeButtonText(getString(R.string.common_cancel))
                    .build()

                prompt.authenticate(promptInfo)
            }
            else -> onUnavailable(getString(R.string.diary_biometrics_initialization_failed))
        }
    }

    private fun diarioRepository(): DiarioRepository {
        val db = NevilleRoomDatabase.getInstance(requireContext().applicationContext)
        return DiarioRepository(db.diarioDao())
    }

    private fun diarioAttachmentRepository(): DiarioAttachmentRepository =
        DiarioAttachmentRepository(requireContext().applicationContext)

    private fun nombreCapitulo(entrada: DiarioEntity): String =
        entrada.capitulo.trim().ifEmpty { SIN_CAPITULO }

    private data class CalendarDay(
        val dayStartMillis: Long,
        val dayNumber: String,
        val count: Int
    )

    private data class AgeFilter(
        val labelRes: Int,
        val maxAgeMillis: Long?
    )

    private enum class FavoritoFiltro {
        TODAS,
        SOLO_FAVORITAS,
        SOLO_NO_FAVORITAS
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

    private enum class DiarioEmotion(val key: String, val emoji: String) {
        FELIZ("feliz", "😊"),
        TRISTE("triste", "🥺"),
        ENFADADO("enfadado", "😤"),
        DESANIMADO("desanimado", "😔"),
        SORPRESA("sorpresa", "😮"),
        DISTRAIDO("distraido", "🙄"),
        NEUTRAL("neutral", "🙂"),
        ENAMORADO("enamorado", "🥰"),
        ENFERMO("enfermo", "🤒"),
        PENSATIVO("pensativo", "🤔"),
        FESTIVO("festivo", "🥳");

        companion object {
            fun fromStored(stored: String?): DiarioEmotion? {
                if (stored.isNullOrBlank()) return NEUTRAL
                val normalized = stored.trim().lowercase(Locale.getDefault())
                return entries.firstOrNull { it.key == normalized || it.emoji == stored }
            }
        }
    }

    @Composable
    private fun FavoritoFiltro.localizedLabel(): String = when (this) {
        FavoritoFiltro.TODAS -> stringResource(R.string.common_all)
        FavoritoFiltro.SOLO_FAVORITAS -> stringResource(R.string.diary_favorites)
        FavoritoFiltro.SOLO_NO_FAVORITAS -> stringResource(R.string.diary_not_favorites)
    }

    @Composable
    private fun DiarioEmotion.localizedLabel(): String = when (this) {
        DiarioEmotion.FELIZ -> stringResource(R.string.diary_emotion_happy)
        DiarioEmotion.TRISTE -> stringResource(R.string.diary_emotion_sad)
        DiarioEmotion.ENFADADO -> stringResource(R.string.diary_emotion_angry)
        DiarioEmotion.DESANIMADO -> stringResource(R.string.diary_emotion_discouraged)
        DiarioEmotion.SORPRESA -> stringResource(R.string.diary_emotion_surprised)
        DiarioEmotion.DISTRAIDO -> stringResource(R.string.diary_emotion_distracted)
        DiarioEmotion.NEUTRAL -> stringResource(R.string.diary_emotion_neutral)
        DiarioEmotion.ENAMORADO -> stringResource(R.string.diary_emotion_loving)
        DiarioEmotion.ENFERMO -> stringResource(R.string.diary_emotion_unwell)
        DiarioEmotion.PENSATIVO -> stringResource(R.string.diary_emotion_thoughtful)
        DiarioEmotion.FESTIVO -> stringResource(R.string.diary_emotion_celebratory)
    }

    @Composable
    private fun displayChapterName(chapter: String): String =
        if (chapter == SIN_CAPITULO) stringResource(R.string.diary_no_chapter) else chapter

    @Composable
    private fun diaryWeekdayLabels(): List<String> = listOf(
        stringResource(R.string.diary_weekday_mon),
        stringResource(R.string.diary_weekday_tue),
        stringResource(R.string.diary_weekday_wed),
        stringResource(R.string.diary_weekday_thu),
        stringResource(R.string.diary_weekday_fri),
        stringResource(R.string.diary_weekday_sat),
        stringResource(R.string.diary_weekday_sun)
    )

    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000
        private const val SIN_CAPITULO = "Sin capítulo"
    }
}

private fun diarioBackgroundBrush(): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            Color(0xFF738080),
            Color(0xFFFF9500)
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
                text = stringResource(R.string.diary_title),
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
                    contentDescription = stringResource(R.string.common_show_calendar),
                    tint = Color(0xFF1F2D36),
                    modifier = Modifier.size(24.dp)
                )
                TextButton(onClick = {}) {
                    Text(stringResource(R.string.diary_clear_date_selection))
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
                    text = stringResource(R.string.common_calendar),
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
                        text = stringResource(R.string.diary_preview_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .weight(1f)
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_menu_open),
                        contentDescription = stringResource(R.string.common_options),
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.diary_preview_content),
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
                        text = stringResource(R.string.diary_dates, "08/04/2026 18:30", "08/04/2026 10:20"),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = Color(0xFF4D4D4D),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_toolbar_favorite),
                        contentDescription = stringResource(R.string.diary_favorite),
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
                contentDescription = stringResource(R.string.diary_menu)
            )
        }
    }
}
