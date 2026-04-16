package com.ypg.neville.ui.frag

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.Fragment
import com.ypg.neville.R
import com.ypg.neville.model.db.room.ArchivedUnitEntity
import com.ypg.neville.model.db.room.GoalUnitEntity
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import com.ypg.neville.model.metas.ArchivedGoalCardState
import com.ypg.neville.model.metas.GoalCardState
import com.ypg.neville.model.metas.HabitPreset
import com.ypg.neville.model.metas.MetasRepository
import com.ypg.neville.model.metas.ProgramaPreestablecido
import com.ypg.neville.model.metas.TimeUnitType
import com.ypg.neville.model.metas.UnitStatus
import com.ypg.neville.model.metas.UnitInfo
import com.ypg.neville.ui.theme.ContextMenuShape
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class FragMetas : Fragment() {

    private val dbExecutor = Executors.newSingleThreadExecutor()

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
        val repository = MetasRepository(requireContext(), db)

        (view as ComposeView).setContent {
            com.ypg.neville.ui.theme.NevilleTheme {
                MetasScreen(repository)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MetasScreen(repository: MetasRepository) {
        val goals = remember { mutableStateListOf<GoalCardState>() }
        val archivedGoals = remember { mutableStateListOf<ArchivedGoalCardState>() }

        var searchText by remember { mutableStateOf("") }
        var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

        var showCreate by remember { mutableStateOf(false) }
        var showArchived by remember { mutableStateOf(false) }

        fun reloadGoals() {
            dbExecutor.execute {
                val list = repository.loadGoals()
                activity?.runOnUiThread {
                    goals.clear()
                    goals.addAll(list)
                }
            }
        }

        fun reloadArchived() {
            dbExecutor.execute {
                val list = repository.loadArchivedGoals()
                activity?.runOnUiThread {
                    archivedGoals.clear()
                    archivedGoals.addAll(list)
                }
            }
        }

        fun refreshExpired() {
            dbExecutor.execute {
                val current = repository.loadGoals()
                var changed = false
                current.forEach { state ->
                    changed = repository.refreshLostUnits(state.goal.id) || changed
                }
                if (changed) {
                    val refreshed = repository.loadGoals()
                    activity?.runOnUiThread {
                        goals.clear()
                        goals.addAll(refreshed)
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            reloadGoals()
        }

        LaunchedEffect(Unit) {
            while (true) {
                delay(1000)
                nowMs = System.currentTimeMillis()
            }
        }

        LaunchedEffect(nowMs) {
            if (nowMs % 15000L < 1000L) {
                refreshExpired()
            }
        }

        val filteredGoals = goals.filter { it.titleMatches(searchText) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFCCC8BC),
                            Color(0xFF9E9681),
                            Color(0xFF4F4D4A)
                        )
                    )
                )
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = if (showArchived) "Metas Archivadas" else "Metas",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(6.dp)
                )

                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    label = { Text("Buscar meta por título", color = Color.Black) },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!showArchived) {
                        Button(onClick = { showCreate = true }, modifier = Modifier.weight(1f)) {
                            Text("Crear")
                        }
                        Button(
                            onClick = {
                                showArchived = true
                                reloadArchived()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Archivadas")
                        }
                    } else {
                        Button(
                            onClick = {
                                showArchived = false
                                reloadGoals()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Volver a activas")
                        }
                    }
                }

                if (!showArchived) {
                    if (filteredGoals.isEmpty()) {
                        EmptyState("No hay metas activas")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredGoals, key = { it.goal.id }) { state ->
                                GoalCard(
                                    state = state,
                                    nowMs = nowMs,
                                    repository = repository,
                                    onChanged = { reloadGoals(); reloadArchived() }
                                )
                            }
                        }
                    }
                } else {
                    val archivedFiltered = archivedGoals.filter { it.titleMatches(searchText) }
                    if (archivedFiltered.isEmpty()) {
                        EmptyState("No hay metas archivadas")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(archivedFiltered, key = { it.goal.id }) { state ->
                                ArchivedGoalCard(
                                    state = state,
                                    repository = repository,
                                    onChanged = { reloadArchived(); reloadGoals() }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showCreate) {
            CreateGoalDialog(
                repository = repository,
                onDismiss = { showCreate = false },
                onCreated = {
                    showCreate = false
                    reloadGoals()
                }
            )
        }
    }

    @Composable
    private fun EmptyState(text: String) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = text, style = MaterialTheme.typography.titleMedium)
        }
    }

    @Composable
    private fun GoalCard(
        state: GoalCardState,
        nowMs: Long,
        repository: MetasRepository,
        onChanged: () -> Unit
    ) {
        var expandUnits by remember(state.goal.id) { mutableStateOf(false) }
        var expandNotes by remember(state.goal.id) { mutableStateOf(false) }
        var showEditGoal by remember(state.goal.id) { mutableStateOf(false) }
        var showDeleteConfirm by remember(state.goal.id) { mutableStateOf(false) }
        var noteText by remember(state.goal.id, state.goal.descriptionText) { mutableStateOf(state.goal.descriptionText) }
        var notifyOnUnitAvailable by remember(state.goal.id, state.goal.notifyOnUnitAvailable) {
            mutableStateOf(state.goal.notifyOnUnitAvailable)
        }
        var showNotifyHint by remember(state.goal.id) { mutableStateOf(false) }
        var notifyHintText by remember(state.goal.id) { mutableStateOf("") }
        var unitDetail by remember { mutableStateOf<GoalUnitEntity?>(null) }

        val timeText = repository.timeUntilNextUnit(state, nowMs)
        val nextUnit = repository.nextPendingUnit(state, nowMs)
        val canCheck = timeText == "Listo" && nextUnit != null
        val expiration = repository.nextExpirationDate(state, nowMs)

        LaunchedEffect(showNotifyHint) {
            if (showNotifyHint) {
                delay(2000)
                showNotifyHint = false
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFF4E8),
                                Color(0xFFFFE3C9),
                                Color(0xFFF5C999)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = state.goal.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (state.isCompleted && state.goal.isStarted) {
                    Text("Completado", color = Color(0xFF1E8E3E), fontWeight = FontWeight.Bold)
                } else {
                    if (canCheck && expiration != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Esta unidad vence en: ${formatRemaining(expiration - nowMs)}",
                                modifier = Modifier.weight(1f)
                            )
                            Button(onClick = {
                                dbExecutor.execute {
                                    repository.markUnitCompleted(nextUnit.id)
                                    activity?.runOnUiThread { onChanged() }
                                }
                            }) {
                                Text("Fichar")
                            }
                        }
                    } else if (!timeText.isNullOrBlank()) {
                        Text(timeText)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${(state.progressRatio * 100).toInt()}%", modifier = Modifier.width(48.dp))
                    GradientProgressBar(progress = state.progressRatio, lostIndexes = state.lostIndexes, total = state.goal.totalUnits)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Progreso: ${state.completedCount}/${state.goal.totalUnits}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    Box {
                        Switch(
                            checked = notifyOnUnitAvailable,
                            onCheckedChange = { enabled ->
                                notifyOnUnitAvailable = enabled
                                notifyHintText = if (enabled) {
                                    "Notificaciones activadas"
                                } else {
                                    "Notificaciones desactivadas"
                                }
                                showNotifyHint = true
                                dbExecutor.execute {
                                    repository.updateGoalUnitNotifications(state.goal.id, enabled)
                                    activity?.runOnUiThread { onChanged() }
                                }
                            }
                        )
                        if (showNotifyHint) {
                            Surface(
                                color = Color(0xFF2D2D2D),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(y = (-38).dp)
                            ) {
                                Text(
                                    text = notifyHintText,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (state.isCompleted && state.goal.isStarted) {
                        TextButton(onClick = {
                            dbExecutor.execute {
                                repository.archiveGoal(state.goal.id)
                                activity?.runOnUiThread { onChanged() }
                            }
                        }) { Text("Archivar") }
                    }

                    IconButton(onClick = { expandNotes = !expandNotes; if (expandNotes) expandUnits = false }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_note),
                            contentDescription = "Notas"
                        )
                    }

                    if (!state.goal.isStarted) {
                        TextButton(onClick = {
                            dbExecutor.execute {
                                repository.startGoal(state.goal.id)
                                activity?.runOnUiThread { onChanged() }
                            }
                        }) { Text("Iniciar", color = Color.Blue, fontWeight = FontWeight.Bold) }
                    } else {
                        IconButton(onClick = { expandUnits = !expandUnits; if (expandUnits) expandNotes = false }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_show),
                                contentDescription = "Progreso"
                            )
                        }
                    }

                    IconButton(onClick = { showEditGoal = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit_note),
                            contentDescription = "Editar"
                        )
                    }

                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = "Eliminar"
                        )
                    }
                }

                if (expandUnits) {
                    GoalUnitsPanel(
                        state = state,
                        nowMs = nowMs,
                        canComplete = { unit -> repository.canBeCompleted(unit, nowMs) },
                        onUnitTapped = { unit ->
                            dbExecutor.execute {
                                repository.markUnitCompleted(unit.id)
                                activity?.runOnUiThread { onChanged() }
                            }
                        },
                        onUnitInfo = { unit -> unitDetail = unit }
                    )
                }

                if (expandNotes) {
                    Text("Notas Generales", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        label = { Text("Texto descriptivo", color = Color.Black) },
                        shape = RoundedCornerShape(14.dp),
                        textStyle = MaterialTheme.typography.titleMedium.copy(color = Color.Black),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            disabledTextColor = Color.Black
                        )
                    )
                    Button(onClick = {
                        dbExecutor.execute {
                            repository.updateGoalDescription(state.goal.id, noteText)
                            activity?.runOnUiThread { onChanged() }
                        }
                    }) {
                        Text("Guardar", color = Color.Black)
                    }
                }
            }
        }

        if (showDeleteConfirm) {
            ConfirmDialog(
                title = "Eliminar objetivo",
                message = "¿Quieres eliminar esta meta y su progreso?",
                confirmText = "Eliminar",
                onDismiss = { showDeleteConfirm = false },
                onConfirm = {
                    showDeleteConfirm = false
                    dbExecutor.execute {
                        repository.deleteGoal(state.goal.id)
                        activity?.runOnUiThread { onChanged() }
                    }
                }
            )
        }

        if (showEditGoal) {
            EditGoalDialog(
                initialTitle = state.goal.title,
                initialDescription = state.goal.descriptionText,
                onDismiss = { showEditGoal = false },
                onSave = { title, desc ->
                    showEditGoal = false
                    dbExecutor.execute {
                        repository.updateGoal(state.goal.id, title, desc)
                        activity?.runOnUiThread { onChanged() }
                    }
                }
            )
        }

        unitDetail?.let { unit ->
            UnitDetailDialog(
                title = unit.name,
                info = unit.info,
                note = unit.note,
                completedDate = unit.completedDate,
                onDismiss = { unitDetail = null },
                onSaveNote = { note ->
                    dbExecutor.execute {
                        repository.updateUnitNote(unit.id, note)
                        activity?.runOnUiThread { onChanged(); unitDetail = null }
                    }
                }
            )
        }
    }

    @Composable
    private fun ArchivedGoalCard(
        state: ArchivedGoalCardState,
        repository: MetasRepository,
        onChanged: () -> Unit
    ) {
        var expandUnits by remember(state.goal.id) { mutableStateOf(false) }
        var expandNotes by remember(state.goal.id) { mutableStateOf(false) }
        var showDelete by remember(state.goal.id) { mutableStateOf(false) }
        var showRestore by remember(state.goal.id) { mutableStateOf(false) }
        var noteText by remember(state.goal.id, state.goal.descriptionText) { mutableStateOf(state.goal.descriptionText) }
        var unitDetail by remember { mutableStateOf<ArchivedUnitEntity?>(null) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFF4E8),
                                Color(0xFFFFE3C9),
                                Color(0xFFF5C999)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(state.goal.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Cumplimiento: ${(state.completionRate * 100).toInt()}%")
                Spacer(Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${(state.progressRatio * 100).toInt()}%", modifier = Modifier.width(48.dp))
                    GradientProgressBar(progress = state.progressRatio, lostIndexes = state.lostIndexes, total = state.goal.totalUnits)
                }
                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Completado: ${state.completedCount}/${state.goal.totalUnits}", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { showRestore = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_play_store),
                            contentDescription = "Reactivar"
                        )
                    }
                    IconButton(onClick = { expandNotes = !expandNotes; if (expandNotes) expandUnits = false }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_note),
                            contentDescription = "Notas"
                        )
                    }
                    IconButton(onClick = { expandUnits = !expandUnits; if (expandUnits) expandNotes = false }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_show),
                            contentDescription = "Progreso"
                        )
                    }
                    IconButton(onClick = { showDelete = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = "Eliminar"
                        )
                    }
                }

                if (expandUnits) {
                    Spacer(Modifier.height(8.dp))
                    ArchivedUnitsPanel(state = state, onUnitInfo = { unitDetail = it })
                }

                if (expandNotes) {
                    Spacer(Modifier.height(8.dp))
                    Text("Notas Generales", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = RoundedCornerShape(14.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.Black),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            disabledTextColor = Color.Black
                        )
                    )
                    Button(onClick = {
                        dbExecutor.execute {
                            repository.updateArchivedGoalDescription(state.goal.id, noteText)
                            activity?.runOnUiThread { onChanged() }
                        }
                    }) { Text("Guardar") }
                }
            }
        }

        if (showDelete) {
            ConfirmDialog(
                title = "Eliminar meta archivada",
                message = "¿Eliminar esta meta permanentemente del historial?",
                confirmText = "Eliminar",
                onDismiss = { showDelete = false },
                onConfirm = {
                    showDelete = false
                    dbExecutor.execute {
                        repository.deleteArchivedGoal(state.goal.id)
                        activity?.runOnUiThread { onChanged() }
                    }
                }
            )
        }

        if (showRestore) {
            ConfirmDialog(
                title = "Reactivar Meta",
                message = "La meta se cargará como activa. ¿Continuar?",
                confirmText = "Reactivar",
                onDismiss = { showRestore = false },
                onConfirm = {
                    showRestore = false
                    dbExecutor.execute {
                        repository.restoreArchivedGoal(state.goal.id)
                        activity?.runOnUiThread { onChanged() }
                    }
                }
            )
        }

        unitDetail?.let { unit ->
            UnitDetailDialog(
                title = unit.name,
                info = unit.info,
                note = unit.note,
                completedDate = unit.completedDate,
                onDismiss = { unitDetail = null },
                onSaveNote = { note ->
                    dbExecutor.execute {
                        repository.updateArchivedUnitNote(unit.id, note)
                        activity?.runOnUiThread { onChanged(); unitDetail = null }
                    }
                }
            )
        }
    }

    @Composable
    private fun GoalUnitsPanel(
        state: GoalCardState,
        nowMs: Long,
        canComplete: (GoalUnitEntity) -> Boolean,
        onUnitTapped: (GoalUnitEntity) -> Unit,
        onUnitInfo: (GoalUnitEntity) -> Unit
    ) {
        val sorted = state.units.sortedBy { it.unitIndex }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sorted, key = { it.id }) { unit ->
                val status = UnitStatus.fromRaw(unit.status)
                val locked = status == UnitStatus.PENDING && nowMs < (unit.startDate ?: Long.MAX_VALUE)
                val canCompleteNow = canComplete(unit)

                val color = when (status) {
                    UnitStatus.COMPLETED -> Color(0xFFB2F2BB)
                    UnitStatus.LOST -> Color(0xFFFFD9B3)
                    UnitStatus.PENDING -> if (locked) Color(0xFFE0E0E0) else Color(0xFFD7EBFF)
                }

                Column(
                    modifier = Modifier
                        .background(color, RoundedCornerShape(10.dp))
                        .clickable {
                            if (canCompleteNow) {
                                onUnitTapped(unit)
                            } else {
                                onUnitInfo(unit)
                            }
                        }
                        .padding(8.dp)
                ) {
                    Text(
                        unit.name,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val emoji = when (status) {
                        UnitStatus.COMPLETED -> "🟢"
                        UnitStatus.LOST -> "🟠"
                        UnitStatus.PENDING -> if (locked) "⚪" else "🟢"
                    }
                    Text(emoji, color = Color.Black)
                }
            }
        }
    }

    @Composable
    private fun ArchivedUnitsPanel(
        state: ArchivedGoalCardState,
        onUnitInfo: (ArchivedUnitEntity) -> Unit
    ) {
        val sorted = state.units.sortedBy { it.unitIndex }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sorted, key = { it.id }) { unit ->
                val status = UnitStatus.fromRaw(unit.status)
                val color = when (status) {
                    UnitStatus.COMPLETED -> Color(0xFFB2F2BB)
                    UnitStatus.LOST -> Color(0xFFFFD9B3)
                    UnitStatus.PENDING -> Color(0xFFE0E0E0)
                }

                Column(
                    modifier = Modifier
                        .background(color, RoundedCornerShape(10.dp))
                        .clickable { onUnitInfo(unit) }
                        .padding(8.dp)
                ) {
                    Text(
                        unit.name,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(if (status == UnitStatus.LOST) "🟠" else "🟢", color = Color.Black)
                }
            }
        }
    }

    @Composable
    private fun ConfirmDialog(
        title: String,
        message: String,
        confirmText: String,
        onDismiss: () -> Unit,
        onConfirm: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = onConfirm) { Text(confirmText) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        )
    }

    @Composable
    private fun EditGoalDialog(
        initialTitle: String,
        initialDescription: String,
        onDismiss: () -> Unit,
        onSave: (String, String) -> Unit
    ) {
        var title by remember(initialTitle) { mutableStateOf(initialTitle) }
        var desc by remember(initialDescription) { mutableStateOf(initialDescription) }

        Dialog(onDismissRequest = onDismiss) {
            Surface(shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Modificar Meta", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título") },
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Descripción") },
                        modifier = Modifier.height(120.dp),
                        shape = RoundedCornerShape(14.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss) { Text("Cancelar") }
                        Button(onClick = { onSave(title, desc) }) { Text("Actualizar") }
                    }
                }
            }
        }
    }

    @Composable
    private fun UnitDetailDialog(
        title: String,
        info: String,
        note: String,
        completedDate: Long?,
        onDismiss: () -> Unit,
        onSaveNote: (String) -> Unit
    ) {
        var selectedTab by remember { mutableIntStateOf(0) }
        var localNote by remember(note) { mutableStateOf(note) }
        val notesScroll = rememberScrollState()
        val infoScroll = rememberScrollState()

        Dialog(onDismissRequest = onDismiss) {
            Surface(shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(title, fontWeight = FontWeight.Bold)
                    if (completedDate != null) {
                        Text("Fichado: ${formatDate(completedDate)}", style = MaterialTheme.typography.bodySmall)
                    }

                    PrimaryTabRow(selectedTabIndex = selectedTab) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Notas") })
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Info") })
                    }

                    if (selectedTab == 0) {
                        OutlinedTextField(
                            value = localNote,
                            onValueChange = { localNote = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .verticalScroll(notesScroll),
                            shape = RoundedCornerShape(14.dp)
                        )
                    } else {
                        Text(
                            text = info.ifBlank { "Sin información" },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .background(Color(0x11000000), RoundedCornerShape(10.dp))
                                .verticalScroll(infoScroll)
                                .padding(8.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss) { Text("Cerrar") }
                        Button(onClick = { onSaveNote(localNote) }) { Text("Guardar") }
                    }
                }
            }
        }
    }

    @Composable
    private fun CreateGoalDialog(
        repository: MetasRepository,
        onDismiss: () -> Unit,
        onCreated: () -> Unit
    ) {
        var selectedTab by remember { mutableIntStateOf(0) }

        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var amountText by remember { mutableStateOf("21") }
        var frequencyText by remember { mutableStateOf("1") }
        var selectedUnit by remember { mutableStateOf(TimeUnitType.DIAS) }
        var notifyOnUnitAvailable by remember { mutableStateOf(false) }
        var habitTitleFilter by remember { mutableStateOf("") }
        var habitContentFilter by remember { mutableStateOf("") }

        var showUnitMenu by remember { mutableStateOf(false) }

        val habits = remember { mutableStateListOf<HabitPreset>() }
        val groupedProgramas = remember { mutableStateListOf<Pair<String, List<ProgramaPreestablecido>>>() }
        var selectedProgramaGroup by remember { mutableStateOf<String?>(null) }
        val filteredHabits = habits.filter { habit ->
            val titleOk = habitTitleFilter.isBlank() ||
                habit.title.contains(habitTitleFilter.trim(), ignoreCase = true)
            val contentOk = habitContentFilter.isBlank() ||
                habit.description.contains(habitContentFilter.trim(), ignoreCase = true)
            titleOk && contentOk
        }

        LaunchedEffect(Unit) {
            dbExecutor.execute {
                val localHabits = repository.loadHabitPresets()
                val localProgramas = repository.loadProgramasAgrupados()
                activity?.runOnUiThread {
                    habits.clear(); habits.addAll(localHabits)
                    groupedProgramas.clear(); groupedProgramas.addAll(localProgramas)
                }
            }
        }

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1E3E62),
                                    Color(0xFF404865),
                                    Color(0xFF8E8F9E)
                                )
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text("Nueva Meta",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp)),
                        containerColor = Color(0x88AFC1D8)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Crear Meta") },
                            selectedContentColor = Color.Black,
                            unselectedContentColor = Color(0xFF2E4158)
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Hábitos") },
                            selectedContentColor = Color.Black,
                            unselectedContentColor = Color(0xFF2E4158)
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Programas") },
                            selectedContentColor = Color.Black,
                            unselectedContentColor = Color(0xFF2E4158)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when (selectedTab) {
                        0 -> {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Título") },
                                shape = RoundedCornerShape(14.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = amountText,
                                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("Unidades") },
                                    shape = RoundedCornerShape(14.dp)
                                )
                                OutlinedTextField(
                                    value = frequencyText,
                                    onValueChange = { frequencyText = it.filter { c -> c.isDigit() } },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("Frecuencia") },
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Box {
                                Button(onClick = { showUnitMenu = true }) {
                                    Text("Tipo: ${selectedUnit.raw}")
                                }
                                DropdownMenu(
                                    expanded = showUnitMenu,
                                    onDismissRequest = { showUnitMenu = false },
                                    shape = ContextMenuShape
                                ) {
                                    TimeUnitType.entries.forEach { unit ->
                                        DropdownMenuItem(
                                            text = { Text(unit.raw) },
                                            onClick = {
                                                selectedUnit = unit
                                                showUnitMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                label = { Text("Descripción") },
                                shape = RoundedCornerShape(14.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Notificar unidades listas para fichar",
                                    color = Color(0xFFCDDC39),
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Switch(
                                    checked = notifyOnUnitAvailable,
                                    onCheckedChange = { notifyOnUnitAvailable = it }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Resumen: Meta a completar en ${amountText.ifBlank { "0" }} ${if (amountText == "1") "unidad" else "unidades"}. " +
                                    "Cada unidad deberá realizarse cada ${frequencyText.ifBlank { "1" }} ${selectedUnit.descriptionFor(frequencyText.toIntOrNull() ?: 1)}",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        1 -> {
                            OutlinedTextField(
                                value = habitTitleFilter,
                                onValueChange = { habitTitleFilter = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Filtrar por título") },
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = habitContentFilter,
                                onValueChange = { habitContentFilter = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Filtrar por contenido") },
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(
                                modifier = Modifier.height(420.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredHabits) { habit ->
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(habit.title, fontWeight = FontWeight.Bold)
                                            Text(habit.description, maxLines = 4, overflow = TextOverflow.Ellipsis)
                                            Row(modifier = Modifier.fillMaxWidth()) {
                                                Spacer(modifier = Modifier.weight(1f))
                                                Button(onClick = {
                                                    title = habit.title
                                                    description = habit.description
                                                    amountText = habit.noUnidades.toString()
                                                    frequencyText = habit.noFrecuencias.toString()
                                                    selectedUnit = TimeUnitType.DIAS
                                                    notifyOnUnitAvailable = false
                                                    selectedTab = 0
                                                }) {
                                                    Text("Cargar")
                                                }
                                            }
                                        }
                                    }
                                }
                                if (filteredHabits.isEmpty()) {
                                    item {
                                        Text(
                                            text = "No hay hábitos que coincidan con los filtros",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        2 -> {
                            val selectedPair = groupedProgramas.firstOrNull { it.first == selectedProgramaGroup }
                            if (selectedPair != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = readableProgramaGroup(selectedPair.first),
                                        fontWeight = FontWeight.Bold
                                    )
                                    TextButton(onClick = { selectedProgramaGroup = null }) {
                                        Text("Volver")
                                    }
                                }
                            }

                            LazyColumn(
                                modifier = Modifier.height(420.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (selectedPair == null) {
                                    items(groupedProgramas, key = { it.first }) { (group, programas) ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedProgramaGroup = group }
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = readableProgramaGroup(group),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    val group = selectedPair.first
                                    val programas = selectedPair.second
                                    items(programas, key = { it.fileBaseName }) { programa ->
                                        Card(modifier = Modifier.fillMaxWidth()) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(programa.title, fontWeight = FontWeight.Bold)
                                                Text(programa.description, maxLines = 4, overflow = TextOverflow.Ellipsis)
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End
                                                ) {
                                                    Button(onClick = {
                                                        dbExecutor.execute {
                                                            repository.createProgramGoal(programa)
                                                            activity?.runOnUiThread { onCreated() }
                                                        }
                                                    }) {
                                                        Text("Comenzar Programa")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    item {
                                        Text(
                                            text = "Categoría: ${readableProgramaGroup(group)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                        )
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Black) }
                        if (selectedTab == 0) {
                            Button(
                                onClick = {
                                    dbExecutor.execute {
                                        repository.createGoal(
                                            title = title,
                                            description = description,
                                            totalUnits = amountText.toIntOrNull() ?: 0,
                                            unitType = selectedUnit,
                                            frequency = frequencyText.toIntOrNull() ?: 1,
                                            unitsInfo = emptyList<UnitInfo>(),
                                            notifyOnUnitAvailable = notifyOnUnitAvailable
                                        )
                                        activity?.runOnUiThread { onCreated() }
                                    }
                                },
                                enabled = title.trim().isNotEmpty() && (amountText.toIntOrNull() ?: 0) > 0
                            ) {
                                Text("Crear Meta", color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun GradientProgressBar(progress: Double, lostIndexes: List<Int>, total: Int) {
        val safeProgress = progress.coerceIn(0.0, 1.0)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(Color(0x22000000), RoundedCornerShape(30.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = safeProgress.toFloat())
                    .height(24.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF66D9A7), Color(0xFF54C2D9), Color(0xFF4A8BDE))
                        ),
                        shape = RoundedCornerShape(30.dp)
                    )
            )

            if (total > 0) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    lostIndexes.forEach { idx ->
                        val leftFraction = (idx + 0.5f) / total.toFloat()
                        Box(modifier = Modifier.fillMaxWidth(leftFraction))
                        Text("•", color = Color.Black)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProgressMarker("25%", safeProgress >= 0.25)
                ProgressMarker("50%", safeProgress >= 0.50)
                ProgressMarker("75%", safeProgress >= 0.75)
                ProgressMarker("100%", safeProgress >= 1.0)
            }
        }
    }

    @Composable
    private fun ProgressMarker(text: String, active: Boolean) {
        Text(
            text = text,
            color = if (active) Color.Black else Color.White,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
    }

    private fun readableProgramaGroup(raw: String): String {
        return raw.removePrefix("prog_")
            .replace("_", " ")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    private fun formatDate(epoch: Long): String {
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(epoch))
    }

    private fun formatRemaining(ms: Long): String {
        val secs = (ms / 1000L).coerceAtLeast(0)
        val hours = secs / 3600
        val minutes = (secs % 3600) / 60
        val seconds = secs % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
}
