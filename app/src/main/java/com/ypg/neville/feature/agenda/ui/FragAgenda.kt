package com.ypg.neville.feature.agenda.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.Fragment
import com.ypg.neville.feature.agenda.data.AgendaItemEntity
import com.ypg.neville.feature.agenda.data.AgendaPriority
import com.ypg.neville.feature.agenda.data.AgendaRepository
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import com.ypg.neville.model.migration.CanonicalRecord
import com.ypg.neville.model.migration.MigrationFormat
import com.ypg.neville.model.migration.MyAppMigrationService
import com.ypg.neville.model.migration.NevilleMigrationRoomBridge
import com.ypg.neville.model.reminders.ReminderFrequency
import com.ypg.neville.model.reminders.ReminderRepository
import com.ypg.neville.model.reminders.ReminderScheduler
import com.ypg.neville.model.subscription.SubscriptionManager
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.Executors
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class FragAgenda : Fragment() {

    private val dbExecutor = Executors.newSingleThreadExecutor()
    private lateinit var createSelectedMigrationExportLauncher: ActivityResultLauncher<String>
    private var pendingSelectedMigrationPassword: CharArray? = null
    private var pendingSelectedMigrationRecords: List<CanonicalRecord> = emptyList()

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
                        "Exportadas ${export.countsByType.values.sum()} entrada(s) de agenda",
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
        val db = NevilleRoomDatabase.getInstance(requireContext())
        val agendaRepository = AgendaRepository(db.agendaItemDao())
        val reminderRepository = ReminderRepository(db.reminderDao())

        (view as ComposeView).setContent {
            com.ypg.neville.ui.theme.NevilleTheme {
                AgendaScreen(agendaRepository, reminderRepository)
            }
        }
    }

    @Composable
    private fun AgendaScreen(
        repository: AgendaRepository,
        reminderRepository: ReminderRepository
    ) {
        val context = LocalContext.current
        val hasPremium = SubscriptionManager.hasActiveSubscriptionNow()
        val items = remember { mutableStateListOf<AgendaItemEntity>() }
        val selectedIds = remember { mutableStateListOf<String>() }
        val expandedIds = remember { mutableStateListOf<String>() }

        var selectedDate by remember { mutableLongStateOf(startOfDay(System.currentTimeMillis())) }
        var displayedMonth by remember { mutableLongStateOf(monthStart(System.currentTimeMillis())) }
        var quickFilter by remember { mutableStateOf(QuickFilter.HOY) }
        var calendarExpanded by remember { mutableStateOf(false) }
        var multiSelectionMode by remember { mutableStateOf(false) }
        var editorItem by remember { mutableStateOf<AgendaItemEntity?>(null) }
        var showReminderManager by remember { mutableStateOf(false) }
        var menuExpanded by remember { mutableStateOf(false) }
        var deleteTarget by remember { mutableStateOf<List<AgendaItemEntity>>(emptyList()) }
        var alertMessage by remember { mutableStateOf<String?>(null) }
        var showSelectedExportDialog by remember { mutableStateOf(false) }
        var selectedExportPassword by remember { mutableStateOf("") }

        fun reload() {
            dbExecutor.execute {
                val list = repository.load()
                activity?.runOnUiThread {
                    items.clear()
                    items.addAll(list)
                }
            }
        }

        fun deleteAgendaItem(item: AgendaItemEntity) {
            dbExecutor.execute {
                item.reminderId?.let { ReminderScheduler.cancel(requireContext(), reminderRepository, it) }
                repository.deleteById(item.id)
                activity?.runOnUiThread { reload() }
            }
        }

        fun saveAgendaItem(item: AgendaItemEntity) {
            dbExecutor.execute {
                val previous = repository.get(item.id)
                var updated = item.copy(updatedAt = System.currentTimeMillis())
                if (updated.reminderActive) {
                    val scheduledAt = mergedDateTime(updated.activityDateMillis, updated.activityTimeMillis)
                    if (scheduledAt <= System.currentTimeMillis()) {
                        activity?.runOnUiThread {
                            alertMessage = "La hora seleccionada ya pasó. Ajusta la fecha u hora del recordatorio a un momento futuro para poder activarlo."
                        }
                        return@execute
                    }
                    previous?.reminderId?.let { ReminderScheduler.cancel(requireContext(), reminderRepository, it) }
                    val reminder = reminderRepository.create(
                        updated.title,
                        updated.content.ifBlank { updated.note },
                        ReminderFrequency.DateOnce(scheduledAt)
                    )
                    ReminderScheduler.schedule(requireContext(), reminder)
                    updated = updated.copy(reminderActive = true, reminderId = reminder.id)
                } else if (previous?.reminderId != null) {
                    ReminderScheduler.cancel(requireContext(), reminderRepository, previous.reminderId)
                    updated = updated.copy(reminderId = null)
                }
                repository.save(updated)
                activity?.runOnUiThread {
                    editorItem = null
                    reload()
                }
            }
        }

        fun updateItem(item: AgendaItemEntity) {
            dbExecutor.execute {
                repository.save(item.copy(updatedAt = System.currentTimeMillis()))
                activity?.runOnUiThread { reload() }
            }
        }

        fun toggleReminder(item: AgendaItemEntity) {
            saveAgendaItem(item.copy(reminderActive = !item.reminderActive))
        }

        LaunchedEffect(Unit) { reload() }

        val listedItems = remember(items.toList(), quickFilter, selectedDate, calendarExpanded) {
            currentListedItems(items, quickFilter, selectedDate, calendarExpanded)
        }
        val collapsedSections = remember(items.toList()) { collapsedSectionsCurrentMonth(items) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF1E88E5), Color(0xFF64B5F6))))
                .padding(10.dp)
        ) {
            if (!hasPremium) {
                SubscriptionLockedState()
            } else {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Agenda",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { showReminderManager = true }) {
                            Text("Recordatorios", color = Color.Black)
                        }
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "Más opciones", tint = Color.Black)
                            }
                            AgendaOptionsMenu(
                                expanded = menuExpanded,
                                multiSelectionMode = multiSelectionMode,
                                onDismiss = { menuExpanded = false },
                                onDeleteMonth = {
                                    deleteTarget = itemsInCurrentMonth(items)
                                    menuExpanded = false
                                },
                                onDeleteWeek = {
                                    deleteTarget = itemsInCurrentWeek(items)
                                    menuExpanded = false
                                },
                                onToggleSelection = {
                                    multiSelectionMode = !multiSelectionMode
                                    if (!multiSelectionMode) selectedIds.clear()
                                    menuExpanded = false
                                },
                                onDeleteSelected = {
                                    deleteTarget = listedItems.filter { it.id in selectedIds }
                                    menuExpanded = false
                                },
                                onExportSelected = {
                                    if (selectedIds.isEmpty()) {
                                        alertMessage = "Selecciona al menos una actividad para exportar."
                                    } else {
                                        selectedExportPassword = ""
                                        showSelectedExportDialog = true
                                    }
                                    menuExpanded = false
                                },
                                onMarkSelected = {
                                    listedItems.filter { it.id in selectedIds }.forEach { updateItem(it.copy(completed = true)) }
                                    menuExpanded = false
                                },
                                onClearSelectedCheck = {
                                    listedItems.filter { it.id in selectedIds }.forEach { updateItem(it.copy(completed = null)) }
                                    menuExpanded = false
                                },
                                onActivateSelectedReminders = {
                                    val selected = listedItems.filter { it.id in selectedIds }
                                    val invalid = selected.any { mergedDateTime(it.activityDateMillis, it.activityTimeMillis) <= System.currentTimeMillis() && !it.reminderActive }
                                    if (invalid) {
                                        alertMessage = "No se activó ningún recordatorio: al menos una actividad seleccionada tiene una hora pasada. Ajusta esas horas a futuro y vuelve a intentarlo."
                                    } else {
                                        selected.filter { !it.reminderActive }.forEach { toggleReminder(it) }
                                    }
                                    menuExpanded = false
                                },
                                onDeactivateSelectedReminders = {
                                    listedItems.filter { it.id in selectedIds && it.reminderActive }.forEach { toggleReminder(it) }
                                    menuExpanded = false
                                }
                            )
                        }
                        IconButton(onClick = { editorItem = repository.create(selectedDate, System.currentTimeMillis()) }) {
                            Icon(Icons.Rounded.Add, contentDescription = "Nueva actividad", tint = Color.Black)
                        }
                    }

                    if (multiSelectionMode) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Seleccionadas: ${selectedIds.size}", color = Color.Black.copy(alpha = 0.75f), modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                multiSelectionMode = false
                                selectedIds.clear()
                            }) { Text("Cancelar", color = Color.Black) }
                        }
                    }

                    FilterTabs(quickFilter) {
                        quickFilter = it
                        if (it == QuickFilter.HOY) {
                            selectedDate = startOfDay(System.currentTimeMillis())
                            displayedMonth = monthStart(System.currentTimeMillis())
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Calendario", color = Color.Black.copy(alpha = 0.75f), modifier = Modifier.weight(1f))
                        IconButton(onClick = { calendarExpanded = !calendarExpanded }) {
                            Icon(
                                if (calendarExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (calendarExpanded) "Ocultar calendario" else "Mostrar calendario",
                                tint = Color.Black.copy(alpha = 0.65f)
                            )
                        }
                    }

                    AnimatedVisibility(calendarExpanded) {
                        CalendarPanel(
                            displayedMonth = displayedMonth,
                            selectedDate = selectedDate,
                            items = items,
                            onPreviousMonth = { displayedMonth = addMonths(displayedMonth, -1) },
                            onNextMonth = { displayedMonth = addMonths(displayedMonth, 1) },
                            onSelectDate = {
                                selectedDate = it
                                quickFilter = QuickFilter.TODOS
                            },
                            onCreateAtDate = {
                                selectedDate = it
                                quickFilter = QuickFilter.TODOS
                                editorItem = repository.create(it, System.currentTimeMillis())
                            }
                        )
                    }

                    if (calendarExpanded) {
                        AgendaItemList(
                            items = listedItems,
                            selectedIds = selectedIds,
                            expandedIds = expandedIds,
                            multiSelectionMode = multiSelectionMode,
                            fixedHeight = true,
                            onSelect = { toggleSelection(selectedIds, it.id) },
                            onEdit = { editorItem = it },
                            onDelete = { deleteTarget = listOf(it) },
                            onToggleCheck = {
                                val next = when (it.completed) {
                                    null -> true
                                    true -> false
                                    false -> null
                                }
                                updateItem(it.copy(completed = next))
                            },
                            onToggleReminder = { toggleReminder(it) },
                            onChangePriority = { item, priority ->
                                updateItem(item.copy(priority = priority.name.lowercase()))
                            }
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(collapsedSections, key = { it.dateMillis }) { section ->
                                Text(sectionTitle(section.dateMillis), color = Color.Black, fontWeight = FontWeight.Bold)
                                section.items.forEach { item ->
                                    AgendaCard(
                                        item = item,
                                        selected = item.id in selectedIds,
                                        expanded = item.id in expandedIds,
                                        multiSelectionMode = multiSelectionMode,
                                        fixedHeight = false,
                                        onExpand = { toggleSelection(expandedIds, item.id) },
                                        onSelect = { toggleSelection(selectedIds, item.id) },
                                        onEdit = { editorItem = item },
                                        onDelete = { deleteTarget = listOf(item) },
                                        onToggleCheck = {
                                            val next = when (item.completed) {
                                                null -> true
                                                true -> false
                                                false -> null
                                            }
                                            updateItem(item.copy(completed = next))
                                        },
                                        onToggleReminder = { toggleReminder(item) },
                                        onChangePriority = { priority ->
                                            updateItem(item.copy(priority = priority.name.lowercase()))
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        editorItem?.let { item ->
            AgendaEditorDialog(
                initial = item,
                onDismiss = { editorItem = null },
                onSave = { saveAgendaItem(it) }
            )
        }

        if (showReminderManager) {
            AgendaReminderDialog(
                items = items.filter { it.reminderActive }.sortedBy { mergedDateTime(it.activityDateMillis, it.activityTimeMillis) },
                onDismiss = { showReminderManager = false },
                onDeactivate = { toggleReminder(it) }
            )
        }

        if (deleteTarget.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { deleteTarget = emptyList() },
                title = { Text(if (deleteTarget.size == 1) "¿Eliminar actividad?" else "¿Eliminar actividades?") },
                text = { Text("También se eliminarán sus recordatorios. Esta acción no se puede deshacer.") },
                confirmButton = {
                    TextButton(onClick = {
                        val target = deleteTarget
                        deleteTarget = emptyList()
                        selectedIds.removeAll(target.map { it.id }.toSet())
                        target.forEach { deleteAgendaItem(it) }
                    }) { Text("Eliminar") }
                },
                dismissButton = { TextButton(onClick = { deleteTarget = emptyList() }) { Text("Cancelar") } }
            )
        }

        if (showSelectedExportDialog) {
            AlertDialog(
                onDismissRequest = {
                    selectedExportPassword = ""
                    showSelectedExportDialog = false
                },
                title = { Text("Exportar agenda seleccionada") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Se creará un archivo ${MigrationFormat.FILE_EXTENSION} solo con las actividades seleccionadas.")
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
                        val selected = listedItems.filter { it.id in selectedIds }
                        if (selected.isEmpty()) {
                            alertMessage = "Selecciona al menos una actividad para exportar."
                            return@TextButton
                        }
                        if (selectedExportPassword.isBlank()) {
                            alertMessage = "Introduce una contraseña para el archivo."
                            return@TextButton
                        }
                        val db = NevilleRoomDatabase.getInstance(context.applicationContext)
                        pendingSelectedMigrationRecords = NevilleMigrationRoomBridge(db).exportSelectedRecords(agendaEntries = selected)
                        pendingSelectedMigrationPassword = selectedExportPassword.toCharArray()
                        selectedExportPassword = ""
                        selectedIds.clear()
                        multiSelectionMode = false
                        showSelectedExportDialog = false
                        createSelectedMigrationExportLauncher.launch("agenda-${System.currentTimeMillis()}${MigrationFormat.FILE_EXTENSION}")
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

        alertMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { alertMessage = null },
                title = { Text("Recordatorio") },
                text = { Text(message) },
                confirmButton = { TextButton(onClick = { alertMessage = null }) { Text("Aceptar") } }
            )
        }
    }

    @Composable
    private fun SubscriptionLockedState() {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 4.dp) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Agenda es una función premium", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Activa la suscripción anual para usar este módulo.")
                }
            }
        }
    }

    @Composable
    private fun FilterTabs(selected: QuickFilter, onSelect: (QuickFilter) -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.38f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            QuickFilter.entries.forEach { filter ->
                val active = filter == selected
                Text(
                    text = filter.label,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) Color(0xFFF2AB5E) else Color.Transparent)
                        .clickable { onSelect(filter) }
                        .padding(vertical = 9.dp, horizontal = 3.dp)
                )
            }
        }
    }

    @Composable
    private fun CalendarPanel(
        displayedMonth: Long,
        selectedDate: Long,
        items: List<AgendaItemEntity>,
        onPreviousMonth: () -> Unit,
        onNextMonth: () -> Unit,
        onSelectDate: (Long) -> Unit,
        onCreateAtDate: (Long) -> Unit
    ) {
        val calendarDays = remember(displayedMonth) { daysForDisplayedMonth(displayedMonth) }
        val weekRows = (calendarDays.size + 6) / 7
        val calendarGridHeight = (22 + weekRows * 30).dp

        Surface(color = Color.White.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onPreviousMonth) { Text("<", color = Color.Black) }
                    Text(
                        monthTitle(displayedMonth),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onNextMonth) { Text(">", color = Color.Black) }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    userScrollEnabled = false,
                    modifier = Modifier.height(calendarGridHeight)
                ) {
                    items(weekdaySymbols()) { day ->
                        Text(
                            day,
                            color = Color.Black.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.height(22.dp)
                        )
                    }
                    items(calendarDays) { date ->
                        if (date == null) {
                            Spacer(modifier = Modifier.height(30.dp))
                        } else {
                            DayCell(
                                date = date,
                                selected = isSameDay(date, selectedDate),
                                hasActivity = items.any { isSameDay(it.activityDateMillis, date) },
                                onClick = { onSelectDate(date) },
                                onDoubleClick = { onCreateAtDate(date) }
                            )
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun DayCell(
        date: Long,
        selected: Boolean,
        hasActivity: Boolean,
        onClick: () -> Unit,
        onDoubleClick: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .height(30.dp)
                .clip(CircleShape)
                .background(if (selected) Color(0xFFF2AB5E) else Color.Transparent)
                .combinedClickable(
                    onClick = onClick,
                    onDoubleClick = onDoubleClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = Calendar.getInstance().apply { timeInMillis = date }.get(Calendar.DAY_OF_MONTH).toString(),
                color = if (selected) Color.Black else if (hasActivity) Color(0xFF1565C0) else Color.Black,
                fontWeight = if (hasActivity) FontWeight.Bold else FontWeight.Normal
            )
        }
    }

    @Composable
    private fun AgendaItemList(
        items: List<AgendaItemEntity>,
        selectedIds: List<String>,
        expandedIds: MutableList<String>,
        multiSelectionMode: Boolean,
        fixedHeight: Boolean,
        onSelect: (AgendaItemEntity) -> Unit,
        onEdit: (AgendaItemEntity) -> Unit,
        onDelete: (AgendaItemEntity) -> Unit,
        onToggleCheck: (AgendaItemEntity) -> Unit,
        onToggleReminder: (AgendaItemEntity) -> Unit,
        onChangePriority: (AgendaItemEntity, AgendaPriority) -> Unit
    ) {
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay actividades", color = Color.Black.copy(alpha = 0.75f))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items, key = { it.id }) { item ->
                    AgendaCard(
                        item = item,
                        selected = item.id in selectedIds,
                        expanded = item.id in expandedIds,
                        multiSelectionMode = multiSelectionMode,
                        fixedHeight = fixedHeight,
                        onExpand = { toggleSelection(expandedIds, item.id) },
                        onSelect = { onSelect(item) },
                        onEdit = { onEdit(item) },
                        onDelete = { onDelete(item) },
                        onToggleCheck = { onToggleCheck(item) },
                        onToggleReminder = { onToggleReminder(item) },
                        onChangePriority = { priority -> onChangePriority(item, priority) }
                    )
                }
            }
        }
    }

    @Composable
    private fun AgendaCard(
        item: AgendaItemEntity,
        selected: Boolean,
        expanded: Boolean,
        multiSelectionMode: Boolean,
        fixedHeight: Boolean,
        onExpand: () -> Unit,
        onSelect: () -> Unit,
        onEdit: () -> Unit,
        onDelete: () -> Unit,
        onToggleCheck: () -> Unit,
        onToggleReminder: () -> Unit,
        onChangePriority: (AgendaPriority) -> Unit
    ) {
        var actionsExpanded by remember(item.id) { mutableStateOf(false) }
        var priorityMenuExpanded by remember(item.id) { mutableStateOf(false) }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fixedHeight) Modifier.height(132.dp) else Modifier)
                .clickable { if (multiSelectionMode) onSelect() },
            color = priorityBackground(item.priority),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Box(modifier = if (fixedHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 44.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (multiSelectionMode) {
                            Icon(
                                if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = "Seleccionar",
                                tint = if (selected) Color(0xFF1565C0) else Color.Black.copy(alpha = 0.55f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                        }
                        AgendaCheckIndicator(item.completed)
                        Text(item.title, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        if (item.reminderActive) {
                            Icon(Icons.Rounded.Notifications, contentDescription = "Recordatorio activo", tint = Color.Black, modifier = Modifier.size(16.dp))
                        }
                        Text(formatTime(item.activityTimeMillis), color = Color.Black.copy(alpha = 0.7f), modifier = Modifier.padding(start = 8.dp))
                    }
                    if (item.place.isNotBlank()) {
                        Text("Lugar: ${item.place}", color = Color.Black.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    val detail = cardDetailText(item)
                    if (detail.isNotBlank()) {
                        Text(
                            detail,
                            color = Color.Black.copy(alpha = 0.82f),
                            maxLines = if (expanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onExpand)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 4.dp, bottom = 2.dp)
                ) {
                    IconButton(onClick = { actionsExpanded = true }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Acciones", tint = Color.Black.copy(alpha = 0.72f))
                    }
                    DropdownMenu(
                        expanded = actionsExpanded,
                        onDismissRequest = { actionsExpanded = false },
                        shape = RoundedCornerShape(12.dp),
                        containerColor = Color.White,
                        tonalElevation = 0.dp,
                        shadowElevation = 6.dp
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar", color = Color.Black) },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = Color.Black) },
                            onClick = {
                                actionsExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(checkActionTitle(item.completed), color = Color.Black) },
                            leadingIcon = { Icon(checkActionIcon(item.completed), contentDescription = null, tint = Color.Black) },
                            onClick = {
                                actionsExpanded = false
                                onToggleCheck()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (item.reminderActive) "Quitar recordatorio" else "Recordatorio", color = Color.Black) },
                            leadingIcon = { Icon(Icons.Rounded.Alarm, contentDescription = null, tint = Color.Black) },
                            onClick = {
                                actionsExpanded = false
                                onToggleReminder()
                            }
                        )
                        Box {
                            DropdownMenuItem(
                                text = { Text("Prioridad", color = Color.Black) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(priorityTint(item.priority))
                                    )
                                },
                                trailingIcon = { Text("›", color = Color.Black, fontWeight = FontWeight.Bold) },
                                onClick = { priorityMenuExpanded = true }
                            )
                            DropdownMenu(
                                expanded = priorityMenuExpanded,
                                onDismissRequest = { priorityMenuExpanded = false },
                                shape = RoundedCornerShape(12.dp),
                                containerColor = Color.White,
                                tonalElevation = 0.dp,
                                shadowElevation = 6.dp
                            ) {
                                AgendaPriority.entries.forEach { option ->
                                    val isCurrent = AgendaPriority.fromRaw(item.priority) == option
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                option.title,
                                                color = Color.Black,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(priorityChipBackground(option, selected = isCurrent))
                                                    .border(
                                                        width = if (isCurrent) 2.dp else 1.dp,
                                                        color = if (isCurrent) priorityChipBorder(option) else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                            )
                                        },
                                        onClick = {
                                            priorityMenuExpanded = false
                                            actionsExpanded = false
                                            onChangePriority(option)
                                        }
                                    )
                                }
                            }
                        }
                        DropdownMenuItem(
                            text = { Text("Eliminar", color = Color.Black) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color.Black) },
                            onClick = {
                                actionsExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun AgendaCheckIndicator(completed: Boolean?) {
        when (completed) {
            null -> Spacer(modifier = Modifier.size(0.dp))
            true -> {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Checkeado",
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(18.dp)
                )
            }
            false -> {
                Icon(
                    Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = "Activa",
                    tint = Color.Black.copy(alpha = 0.62f),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(18.dp)
                )
            }
        }
    }

    @Composable
    private fun AgendaEditorDialog(
        initial: AgendaItemEntity,
        onDismiss: () -> Unit,
        onSave: (AgendaItemEntity) -> Unit
    ) {
        val context = LocalContext.current
        var title by remember(initial.id) { mutableStateOf(initial.title) }
        var dateMillis by remember(initial.id) { mutableLongStateOf(initial.activityDateMillis) }
        var timeMillis by remember(initial.id) { mutableLongStateOf(initial.activityTimeMillis) }
        var place by remember(initial.id) { mutableStateOf(initial.place) }
        var note by remember(initial.id) { mutableStateOf(initial.note) }
        var content by remember(initial.id) { mutableStateOf(initial.content) }
        var priority by remember(initial.id) { mutableStateOf(AgendaPriority.fromRaw(initial.priority)) }
        var completed by remember(initial.id) { mutableStateOf(initial.completed) }
        var reminderActive by remember(initial.id) { mutableStateOf(initial.reminderActive) }
        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            focusedLabelColor = Color.Black,
            unfocusedLabelColor = Color.Black.copy(alpha = 0.72f),
            cursorColor = Color.Black,
            focusedBorderColor = Color.Black.copy(alpha = 0.62f),
            unfocusedBorderColor = Color.Black.copy(alpha = 0.36f),
            focusedContainerColor = Color.White.copy(alpha = 0.48f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.36f)
        )
        val dateButtonColors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.58f),
            contentColor = Color.Black
        )

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFDDF2FF),
                                    Color(0xFFC8E8FA),
                                    Color(0xFFEAF8FF)
                                )
                            )
                        )
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (initial.title.isBlank()) "Nueva actividad" else "Editar actividad",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        item {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Título", color = Color.Black.copy(alpha = 0.72f)) },
                                singleLine = true,
                                colors = fieldColors,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                                        DatePickerDialog(
                                            context,
                                            { _, year, month, day ->
                                                dateMillis = Calendar.getInstance().apply {
                                                    set(year, month, day, 0, 0, 0)
                                                    set(Calendar.MILLISECOND, 0)
                                                }.timeInMillis
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                colors = dateButtonColors,
                                modifier = Modifier.weight(1f)
                            ) { Text(formatDate(dateMillis), color = Color.Black) }
                            Button(
                                onClick = {
                                    val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
                                        TimePickerDialog(
                                            context,
                                            { _, hour, minute ->
                                                timeMillis = Calendar.getInstance().apply {
                                                    set(Calendar.HOUR_OF_DAY, hour)
                                                    set(Calendar.MINUTE, minute)
                                                    set(Calendar.SECOND, 0)
                                                    set(Calendar.MILLISECOND, 0)
                                                }.timeInMillis
                                            },
                                            cal.get(Calendar.HOUR_OF_DAY),
                                            cal.get(Calendar.MINUTE),
                                        true
                                    ).show()
                                },
                                colors = dateButtonColors,
                                modifier = Modifier.weight(1f)
                            ) { Text(formatTime(timeMillis), color = Color.Black) }
                        }
                    }
                        item {
                            OutlinedTextField(
                                value = place,
                                onValueChange = { place = it },
                                label = { Text("Lugar", color = Color.Black.copy(alpha = 0.72f)) },
                                colors = fieldColors,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = note,
                                onValueChange = { note = it },
                                label = { Text("Nota", color = Color.Black.copy(alpha = 0.72f)) },
                                minLines = 2,
                                colors = fieldColors,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = content,
                                onValueChange = { content = it },
                                label = { Text("Contenido", color = Color.Black.copy(alpha = 0.72f)) },
                                minLines = 2,
                                colors = fieldColors,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                AgendaPriority.entries.forEach { option ->
                                Text(
                                    option.title,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(priorityChipBackground(option, selected = priority == option))
                                        .border(
                                            width = if (priority == option) 2.dp else 1.dp,
                                            color = if (priority == option) priorityChipBorder(option) else Color.White.copy(alpha = 0.42f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { priority = option }
                                        .padding(vertical = 8.dp)
                                )
                                }
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(null to "No aplicar", false to "Activa", true to "Completada").forEach { (value, label) ->
                                    Text(
                                        label,
                                        color = Color.Black,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (completed == value) Color(0xFFF2AB5E) else Color.Black.copy(alpha = 0.08f))
                                            .clickable { completed = value }
                                            .padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Activar recordatorio", color = Color.Black, modifier = Modifier.weight(1f))
                                Switch(checked = reminderActive, onCheckedChange = { reminderActive = it })
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Black) }
                        TextButton(
                            enabled = title.trim().isNotBlank(),
                            onClick = {
                                onSave(
                                    initial.copy(
                                        title = title.trim(),
                                        activityDateMillis = startOfDay(dateMillis),
                                        activityTimeMillis = timeMillis,
                                        place = place,
                                        note = note,
                                        content = content,
                                        priority = priority.name.lowercase(),
                                        completed = completed,
                                        reminderActive = reminderActive
                                    )
                                )
                            }
                        ) { Text("Guardar", color = Color.Black) }
                    }
                }
            }
        }
    }

    @Composable
    private fun AgendaReminderDialog(
        items: List<AgendaItemEntity>,
        onDismiss: () -> Unit,
        onDeactivate: (AgendaItemEntity) -> Unit
    ) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFDDF2FF),
                                    Color(0xFFC8E8FA),
                                    Color(0xFFEAF8FF)
                                )
                            )
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Recordatorios Agenda",
                        color = Color.Black,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (items.isEmpty()) {
                        Text("No hay recordatorios activos.", color = Color.Black.copy(alpha = 0.72f))
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            items(items, key = { it.id }) { item ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = 0.52f),
                                    tonalElevation = 0.dp,
                                    shadowElevation = 0.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.title, color = Color.Black, fontWeight = FontWeight.Bold)
                                            Text(
                                                "${formatDate(item.activityDateMillis)} · ${formatTime(item.activityTimeMillis)}",
                                                color = Color.Black.copy(alpha = 0.7f)
                                            )
                                        }
                                        TextButton(onClick = { onDeactivate(item) }) {
                                            Text("Desactivar", color = Color.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) {
                            Text("Cerrar", color = Color.Black)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun AgendaOptionsMenu(
        expanded: Boolean,
        multiSelectionMode: Boolean,
        onDismiss: () -> Unit,
        onDeleteMonth: () -> Unit,
        onDeleteWeek: () -> Unit,
        onToggleSelection: () -> Unit,
        onDeleteSelected: () -> Unit,
        onExportSelected: () -> Unit,
        onMarkSelected: () -> Unit,
        onClearSelectedCheck: () -> Unit,
        onActivateSelectedReminders: () -> Unit,
        onDeactivateSelectedReminders: () -> Unit
    ) {
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
            DropdownMenuItem(text = { Text("Eliminar mes actual") }, onClick = onDeleteMonth)
            DropdownMenuItem(text = { Text("Eliminar semana actual") }, onClick = onDeleteWeek)
            DropdownMenuItem(text = { Text(if (multiSelectionMode) "Salir selección múltiple" else "Selección múltiple") }, onClick = onToggleSelection)
            if (multiSelectionMode) {
                DropdownMenuItem(text = { Text("Exportar seleccionadas") }, onClick = onExportSelected)
                DropdownMenuItem(text = { Text("Eliminar seleccionadas") }, onClick = onDeleteSelected)
                DropdownMenuItem(text = { Text("Marcar seleccionadas completadas") }, onClick = onMarkSelected)
                DropdownMenuItem(text = { Text("Quitar modo check seleccionadas") }, onClick = onClearSelectedCheck)
                DropdownMenuItem(text = { Text("Activar recordatorios seleccionadas") }, onClick = onActivateSelectedReminders)
                DropdownMenuItem(text = { Text("Desactivar recordatorios seleccionadas") }, onClick = onDeactivateSelectedReminders)
            }
        }
    }

    private enum class QuickFilter(val label: String) {
        HOY("Hoy"),
        SIETE_DIAS("Próx. 7 días"),
        CON_RECORDATORIO("Con recordatorio"),
        TODOS("Todos")
    }

    private data class AgendaSection(val dateMillis: Long, val items: List<AgendaItemEntity>)

    private fun currentListedItems(
        items: List<AgendaItemEntity>,
        filter: QuickFilter,
        selectedDate: Long,
        calendarExpanded: Boolean
    ): List<AgendaItemEntity> {
        val today = startOfDay(System.currentTimeMillis())
        val sevenDays = addDays(today, 7)
        val filtered = when (filter) {
            QuickFilter.HOY -> items.filter { isSameDay(it.activityDateMillis, today) }
            QuickFilter.SIETE_DIAS -> items.filter { startOfDay(it.activityDateMillis) >= today && startOfDay(it.activityDateMillis) < sevenDays }
            QuickFilter.CON_RECORDATORIO -> items.filter { it.reminderActive }
            QuickFilter.TODOS -> if (calendarExpanded) items.filter { isSameDay(it.activityDateMillis, selectedDate) } else items
        }
        return filtered.sortedWith(compareBy({ it.activityDateMillis }, { timeSortValue(it.activityTimeMillis) }))
    }

    private fun collapsedSectionsCurrentMonth(items: List<AgendaItemEntity>): List<AgendaSection> {
        val now = System.currentTimeMillis()
        val start = monthStart(now)
        val end = addMonths(start, 1)
        val grouped = items.filter { it.activityDateMillis >= start && it.activityDateMillis < end }
            .groupBy { startOfDay(it.activityDateMillis) }
        val pivot = startOfDay(now)
        return grouped.keys.sortedWith { lhs, rhs ->
            val lhsOffset = if (lhs < pivot) 10_000_000_000L + (pivot - lhs) else lhs - pivot
            val rhsOffset = if (rhs < pivot) 10_000_000_000L + (pivot - rhs) else rhs - pivot
            lhsOffset.compareTo(rhsOffset)
        }.map { day ->
            AgendaSection(day, grouped[day].orEmpty().sortedWith(compareBy({ it.activityDateMillis }, { timeSortValue(it.activityTimeMillis) })))
        }
    }

    private fun itemsInCurrentMonth(items: List<AgendaItemEntity>): List<AgendaItemEntity> {
        val start = monthStart(System.currentTimeMillis())
        val end = addMonths(start, 1)
        return items.filter { it.activityDateMillis >= start && it.activityDateMillis < end }
    }

    private fun itemsInCurrentWeek(items: List<AgendaItemEntity>): List<AgendaItemEntity> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 7)
        val end = cal.timeInMillis
        return items.filter { it.activityDateMillis >= start && it.activityDateMillis < end }
    }
}

private fun toggleSelection(list: MutableList<String>, id: String) {
    if (id in list) list.remove(id) else list.add(id)
}

private fun priorityTint(raw: String): Color {
    return when (AgendaPriority.fromRaw(raw)) {
        AgendaPriority.NEUTRAL -> Color.Black.copy(alpha = 0.35f)
        AgendaPriority.BAJA -> Color(0xFF8CB884)
        AgendaPriority.MEDIA -> Color(0xFFDEBD7A)
        AgendaPriority.ALTA -> Color(0xFFD48778)
    }
}

private fun priorityBackground(raw: String): Color {
    return when (AgendaPriority.fromRaw(raw)) {
        AgendaPriority.NEUTRAL -> Color(0xFFF4F7F8)
        AgendaPriority.BAJA -> Color(0xFFE6F3E4)
        AgendaPriority.MEDIA -> Color(0xFFF8EED3)
        AgendaPriority.ALTA -> Color(0xFFF7DFDC)
    }
}

private fun priorityChipBackground(priority: AgendaPriority, selected: Boolean): Color {
    val alpha = if (selected) 0.95f else 0.68f
    return when (priority) {
        AgendaPriority.NEUTRAL -> Color(0xFFE8EDF0).copy(alpha = alpha)
        AgendaPriority.BAJA -> Color(0xFFD9EED7).copy(alpha = alpha)
        AgendaPriority.MEDIA -> Color(0xFFF2E4C1).copy(alpha = alpha)
        AgendaPriority.ALTA -> Color(0xFFF1D0CB).copy(alpha = alpha)
    }
}

private fun priorityChipBorder(priority: AgendaPriority): Color {
    return when (priority) {
        AgendaPriority.NEUTRAL -> Color(0xFF62727B)
        AgendaPriority.BAJA -> Color(0xFF6F9F69)
        AgendaPriority.MEDIA -> Color(0xFFC49B42)
        AgendaPriority.ALTA -> Color(0xFFC17469)
    }
}

private fun checkActionTitle(completed: Boolean?): String {
    return when (completed) {
        null -> "Activar check"
        true -> "Reactivar"
        false -> "Quitar check"
    }
}

private fun checkActionIcon(completed: Boolean?) = when (completed) {
    null -> Icons.Rounded.CheckCircle
    true -> Icons.Rounded.RadioButtonUnchecked
    false -> Icons.Rounded.Close
}

private fun cardDetailText(item: AgendaItemEntity): String {
    val content = item.content.trim()
    val note = item.note.trim()
    return when {
        content.isNotEmpty() && note.isNotEmpty() -> "$content · $note"
        content.isNotEmpty() -> content
        else -> note
    }
}

private fun mergedDateTime(dateMillis: Long, timeMillis: Long): Long {
    val date = Calendar.getInstance().apply { timeInMillis = dateMillis }
    val time = Calendar.getInstance().apply { timeInMillis = timeMillis }
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, date.get(Calendar.YEAR))
        set(Calendar.MONTH, date.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, date.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, time.get(Calendar.HOUR_OF_DAY))
        set(Calendar.MINUTE, time.get(Calendar.MINUTE))
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun startOfDay(millis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun monthStart(millis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun addDays(millis: Long, days: Int): Long {
    return Calendar.getInstance().apply {
        timeInMillis = millis
        add(Calendar.DAY_OF_MONTH, days)
    }.timeInMillis
}

private fun addMonths(millis: Long, months: Int): Long {
    return Calendar.getInstance().apply {
        timeInMillis = millis
        add(Calendar.MONTH, months)
    }.timeInMillis
}

private fun isSameDay(a: Long, b: Long): Boolean {
    val left = Calendar.getInstance().apply { timeInMillis = a }
    val right = Calendar.getInstance().apply { timeInMillis = b }
    return left.get(Calendar.YEAR) == right.get(Calendar.YEAR) &&
        left.get(Calendar.DAY_OF_YEAR) == right.get(Calendar.DAY_OF_YEAR)
}

private fun timeSortValue(millis: Long): Int {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
}

private fun daysForDisplayedMonth(displayedMonth: Long): List<Long?> {
    val cal = Calendar.getInstance().apply { timeInMillis = monthStart(displayedMonth) }
    val firstWeekday = cal.firstDayOfWeek
    val weekday = cal.get(Calendar.DAY_OF_WEEK)
    val leadingEmpty = (weekday - firstWeekday + 7) % 7
    val days = mutableListOf<Long?>()
    repeat(leadingEmpty) { days.add(null) }
    val max = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    repeat(max) { index ->
        days.add(Calendar.getInstance().apply {
            timeInMillis = monthStart(displayedMonth)
            add(Calendar.DAY_OF_MONTH, index)
        }.timeInMillis)
    }
    return days
}

private fun weekdaySymbols(): List<String> {
    val symbols = DateFormatSymbols(Locale.getDefault()).shortWeekdays.filter { it.isNotBlank() }
    val firstDay = Calendar.getInstance().firstDayOfWeek
    return (0 until 7).map { offset -> symbols[(firstDay - 1 + offset) % 7] }
}

private fun monthTitle(millis: Long): String {
    return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(millis).replaceFirstChar { it.titlecase(Locale.getDefault()) }
}

private fun sectionTitle(millis: Long): String {
    val title = SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(millis).replaceFirstChar { it.titlecase(Locale.getDefault()) }
    return if (isSameDay(millis, System.currentTimeMillis())) "Hoy · $title" else title
}

private fun formatDate(millis: Long): String = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(millis)

private fun formatTime(millis: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(millis)
