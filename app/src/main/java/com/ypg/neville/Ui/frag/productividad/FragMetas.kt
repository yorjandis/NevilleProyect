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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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
import com.ypg.neville.model.metas.GoalCompletionBasis
import com.ypg.neville.model.metas.GoalDayPeriod
import com.ypg.neville.model.metas.GoalScheduleType
import com.ypg.neville.model.metas.HabitPreset
import com.ypg.neville.model.metas.MetasRepository
import com.ypg.neville.model.metas.ProgramaPreestablecido
import com.ypg.neville.model.metas.TimeUnitType
import com.ypg.neville.model.metas.UnitStatus
import com.ypg.neville.model.metas.UnitInfo
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
        var showStats by remember { mutableStateOf(false) }

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
                    text = if (showArchived) stringResource(R.string.goals_archived_title) else stringResource(R.string.goals_title),
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
                    label = { Text(stringResource(R.string.goals_search), color = Color.Black) },
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
                            Text(stringResource(R.string.goals_create))
                        }
                        Button(
                            onClick = {
                                showArchived = true
                                reloadArchived()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.goals_archived))
                        }
                    } else {
                        Button(
                            onClick = {
                                showArchived = false
                                reloadGoals()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.goals_back_to_active))
                        }
                    }
                }
                Button(
                    onClick = {
                        reloadArchived()
                        showStats = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                ) {
                    Text(stringResource(R.string.goals_statistics))
                }

                if (!showArchived) {
                    if (filteredGoals.isEmpty()) {
                        EmptyState(stringResource(R.string.goals_no_active))
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
                        EmptyState(stringResource(R.string.goals_no_archived))
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

        if (showStats) {
            Dialog(
                onDismissRequest = { showStats = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                GoalStatsScreen(
                    goals = goals.toList(),
                    archivedGoals = archivedGoals.toList(),
                    onClose = { showStats = false },
                    onRefresh = {
                        reloadGoals()
                        reloadArchived()
                    }
                )
            }
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
        val context = LocalContext.current
        var expandUnits by remember(state.goal.id) { mutableStateOf(false) }
        var expandNotes by remember(state.goal.id) { mutableStateOf(false) }
        var showEditGoal by remember(state.goal.id) { mutableStateOf(false) }
        var showDeleteConfirm by remember(state.goal.id) { mutableStateOf(false) }
        var showReactivateConfirm by remember(state.goal.id) { mutableStateOf(false) }
        var noteText by remember(state.goal.id, state.goal.descriptionText) { mutableStateOf(state.goal.descriptionText) }
        var notifyOnUnitAvailable by remember(state.goal.id, state.goal.notifyOnUnitAvailable) {
            mutableStateOf(state.goal.notifyOnUnitAvailable)
        }
        var showNotifyHint by remember(state.goal.id) { mutableStateOf(false) }
        var notifyHintText by remember(state.goal.id) { mutableStateOf("") }
        var unitDetail by remember { mutableStateOf<GoalUnitEntity?>(null) }

        val nextUnit = repository.nextPendingUnit(state, nowMs)
        val canCheck = nextUnit?.startDate?.let { nowMs >= it } == true
        val timeText = localizedTimeUntilNextUnit(state, nextUnit, nowMs)
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

                Text(
                    text = "🗓 ${localizedPlanSummary(state)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4D3A2A)
                )

                if (state.isCompleted && state.goal.isStarted) {
                    Text(stringResource(R.string.goals_completed), color = Color(0xFF1E8E3E), fontWeight = FontWeight.Bold)
                } else {
                    if (canCheck && expiration != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.goals_unit_expires_in, formatRemaining(expiration - nowMs)),
                                modifier = Modifier.weight(1f)
                            )
                            Button(onClick = {
                                dbExecutor.execute {
                                    repository.markUnitCompleted(nextUnit.id)
                                    activity?.runOnUiThread { onChanged() }
                                }
                            }) {
                                Text(stringResource(R.string.goals_check_in))
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
                        text = stringResource(R.string.goals_progress_value, state.completedCount, state.goal.totalUnits),
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
                                    context.getString(R.string.goals_notifications_on)
                                } else {
                                    context.getString(R.string.goals_notifications_off)
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
                        TextButton(onClick = { showReactivateConfirm = true }) {
                            Text(stringResource(R.string.goals_reactivate))
                        }
                        TextButton(onClick = {
                            dbExecutor.execute {
                                repository.archiveGoal(state.goal.id)
                                activity?.runOnUiThread { onChanged() }
                            }
                        }) { Text(stringResource(R.string.goals_archive)) }
                    }

                    IconButton(onClick = { expandNotes = !expandNotes; if (expandNotes) expandUnits = false }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_note),
                            contentDescription = stringResource(R.string.goals_notes)
                        )
                    }

                    if (!state.goal.isStarted) {
                        TextButton(onClick = {
                            dbExecutor.execute {
                                repository.startGoal(state.goal.id)
                                activity?.runOnUiThread { onChanged() }
                            }
                        }) { Text(stringResource(R.string.goals_start), color = Color.Blue, fontWeight = FontWeight.Bold) }
                    } else {
                        IconButton(onClick = { expandUnits = !expandUnits; if (expandUnits) expandNotes = false }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_show),
                                contentDescription = stringResource(R.string.goals_progress)
                            )
                        }
                    }

                    IconButton(onClick = { showEditGoal = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit_note),
                            contentDescription = stringResource(R.string.common_edit)
                        )
                    }

                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.common_delete)
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
                    Text(stringResource(R.string.goals_general_notes), fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        label = { Text(stringResource(R.string.goals_descriptive_text), color = Color.Black) },
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
                        Text(stringResource(R.string.common_save), color = Color.Black)
                    }
                }
            }
        }

        if (showDeleteConfirm) {
            ConfirmDialog(
                title = stringResource(R.string.goals_delete_title),
                message = stringResource(R.string.goals_delete_message),
                confirmText = stringResource(R.string.common_delete),
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


        if (showReactivateConfirm) {
            ConfirmDialog(
                title = stringResource(R.string.goals_reactivate_title),
                message = stringResource(R.string.goals_reactivate_completed_message),
                confirmText = stringResource(R.string.goals_reactivate),
                onDismiss = { showReactivateConfirm = false },
                onConfirm = {
                    showReactivateConfirm = false
                    dbExecutor.execute {
                        repository.reactivateCompletedGoal(state.goal.id)
                        activity?.runOnUiThread { onChanged() }
                    }
                }
            )
        }

        if (showEditGoal) {
            EditGoalDialog(
                initialTitle = state.goal.title,
                initialDescription = state.goal.descriptionText,
                initialUnitLabel = state.goal.customUnitLabel,
                initialDayPeriod = state.dayPeriod,
                onDismiss = { showEditGoal = false },
                onSave = { title, desc, unitLabel, period ->
                    showEditGoal = false
                    dbExecutor.execute {
                        repository.updateGoal(state.goal.id, title, desc, unitLabel, period)
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
                Text(stringResource(R.string.goals_completion_value, (state.completionRate * 100).toInt()))
                Spacer(Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${(state.progressRatio * 100).toInt()}%", modifier = Modifier.width(48.dp))
                    GradientProgressBar(progress = state.progressRatio, lostIndexes = state.lostIndexes, total = state.goal.totalUnits)
                }
                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.goals_completed_value, state.completedCount, state.goal.totalUnits), fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { showRestore = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_play_store),
                            contentDescription = stringResource(R.string.goals_reactivate)
                        )
                    }
                    IconButton(onClick = { expandNotes = !expandNotes; if (expandNotes) expandUnits = false }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_note),
                            contentDescription = stringResource(R.string.goals_notes)
                        )
                    }
                    IconButton(onClick = { expandUnits = !expandUnits; if (expandUnits) expandNotes = false }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_show),
                            contentDescription = stringResource(R.string.goals_progress)
                        )
                    }
                    IconButton(onClick = { showDelete = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.common_delete)
                        )
                    }
                }

                if (expandUnits) {
                    Spacer(Modifier.height(8.dp))
                    ArchivedUnitsPanel(state = state, onUnitInfo = { unitDetail = it })
                }

                if (expandNotes) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.goals_general_notes), fontWeight = FontWeight.Bold)
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
                    }) { Text(stringResource(R.string.common_save)) }
                }
            }
        }

        if (showDelete) {
            ConfirmDialog(
                title = stringResource(R.string.goals_delete_archived_title),
                message = stringResource(R.string.goals_delete_archived_message),
                confirmText = stringResource(R.string.common_delete),
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
                title = stringResource(R.string.goals_reactivate_title),
                message = stringResource(R.string.goals_restore_message),
                confirmText = stringResource(R.string.goals_reactivate),
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
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Text(
                            unit.name.ifBlank { stringResource(R.string.goals_unit_number, unit.unitIndex) },
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (unit.note.isNotBlank()) {
                            Text(
                                stringResource(R.string.goals_note_badge),
                                color = Color(0xFF16813B),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    val emoji = when (status) {
                        UnitStatus.COMPLETED -> "🟢"
                        UnitStatus.LOST -> "🟠"
                        UnitStatus.PENDING -> if (locked) "⚪" else "🟢"
                    }
                    Text(emoji, color = Color.Black)
                    if (state.scheduleType == GoalScheduleType.SPECIFIC_DATES ||
                        state.scheduleType == GoalScheduleType.WEEKLY ||
                        state.dayPeriod != GoalDayPeriod.ANYTIME
                    ) {
                        unit.startDate?.let {
                            Text(
                                formatUnitSchedule(it, state.scheduleType, state.dayPeriod),
                                color = Color.DarkGray,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 2
                            )
                        }
                    }
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
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    @Composable
    private fun EditGoalDialog(
        initialTitle: String,
        initialDescription: String,
        initialUnitLabel: String,
        initialDayPeriod: GoalDayPeriod,
        onDismiss: () -> Unit,
        onSave: (String, String, String, GoalDayPeriod) -> Unit
    ) {
        var title by remember(initialTitle) { mutableStateOf(initialTitle) }
        var desc by remember(initialDescription) { mutableStateOf(initialDescription) }
        var unitLabel by remember(initialUnitLabel) { mutableStateOf(initialUnitLabel) }
        var dayPeriod by remember(initialDayPeriod) { mutableStateOf(initialDayPeriod) }

        Dialog(onDismissRequest = onDismiss) {
            Surface(shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(stringResource(R.string.goals_edit_title), fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.common_title)) },
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text(stringResource(R.string.goals_description)) },
                        modifier = Modifier.height(120.dp),
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = unitLabel,
                        onValueChange = { unitLabel = it },
                        label = { Text(stringResource(R.string.goals_optional_unit_name)) },
                        shape = RoundedCornerShape(14.dp)
                    )
                    ConfigSelector(
                        label = stringResource(R.string.goals_time_of_day),
                        options = GoalDayPeriod.entries.map { it to localizedDayPeriod(it) },
                        selected = dayPeriod,
                        onSelected = { dayPeriod = it }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
                        Button(onClick = { onSave(title, desc, unitLabel, dayPeriod) }) { Text(stringResource(R.string.goals_update)) }
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
                        Text(stringResource(R.string.goals_checked_on, formatDate(completedDate)), style = MaterialTheme.typography.bodySmall)
                    }

                    PrimaryTabRow(selectedTabIndex = selectedTab) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(stringResource(R.string.goals_notes)) })
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(stringResource(R.string.goals_info)) })
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
                            text = info.ifBlank { stringResource(R.string.goals_no_information) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .background(Color(0x11000000), RoundedCornerShape(10.dp))
                                .verticalScroll(infoScroll)
                                .padding(8.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) }
                        Button(onClick = { onSaveNote(localNote) }) { Text(stringResource(R.string.common_save)) }
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
        var executionTargetText by remember { mutableStateOf("1") }
        var customUnitLabel by remember { mutableStateOf("") }
        var completionBasis by remember { mutableStateOf(GoalCompletionBasis.EXECUTIONS) }
        var durationValueText by remember { mutableStateOf("30") }
        var durationUnit by remember { mutableStateOf(TimeUnitType.DIAS) }
        var scheduleType by remember { mutableStateOf(GoalScheduleType.INTERVAL) }
        var weeklyDaysText by remember { mutableStateOf("3") }
        var dayPeriod by remember { mutableStateOf(GoalDayPeriod.ANYTIME) }
        var showDescription by remember { mutableStateOf(false) }
        val specificDates = remember { mutableStateListOf<Long>() }
        var notifyOnUnitAvailable by remember { mutableStateOf(false) }
        var habitTitleFilter by remember { mutableStateOf("") }
        var habitContentFilter by remember { mutableStateOf("") }

        var previewPrograma by remember { mutableStateOf<ProgramaPreestablecido?>(null) }

        val habits = remember { mutableStateListOf<HabitPreset>() }
        val groupedProgramas = remember { mutableStateListOf<Pair<String, List<ProgramaPreestablecido>>>() }
        val habitCardColor = Color(0xFFD2F0DF)
        val habitCardContentColor = Color(0xFF173C2A)
        val programCardColor = Color(0xFFFFE0B8)
        val programCardContentColor = Color(0xFF4A2D12)
        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            focusedLabelColor = Color.Black,
            unfocusedLabelColor = Color.Black,
            focusedPlaceholderColor = Color.Black,
            unfocusedPlaceholderColor = Color.Black,
            focusedBorderColor = Color.Black,
            unfocusedBorderColor = Color.Black,
            cursorColor = Color.Black
        )
        val darkFieldColors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedLabelColor = Color.Black,
            unfocusedLabelColor = Color.Black,
            focusedPlaceholderColor = Color.White.copy(alpha = 0.7f),
            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.7f),
            focusedContainerColor = Color.Black.copy(alpha = 0.7f),
            unfocusedContainerColor = Color.Black.copy(alpha = 0.7f),
            cursorColor = Color.White
        )
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

        LaunchedEffect(amountText, scheduleType) {
            if (scheduleType == GoalScheduleType.SPECIFIC_DATES) {
                val amount = (amountText.toIntOrNull() ?: 1).coerceIn(1, 365)
                while (specificDates.size < amount) {
                    val base = specificDates.lastOrNull() ?: System.currentTimeMillis()
                    specificDates.add(java.util.Calendar.getInstance().apply {
                        timeInMillis = base
                        add(java.util.Calendar.DAY_OF_MONTH, 1)
                    }.timeInMillis)
                }
                while (specificDates.size > amount) specificDates.removeAt(specificDates.lastIndex)
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
                                colors = if (selectedTab == 0) {
                                    listOf(Color(0xFFD1F0C7), Color(0xFFBAE5B0), Color(0xFFE0F7D6))
                                } else {
                                    listOf(Color(0xFF59636F), Color(0xFF303943), Color(0xFF171C22))
                                }
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(stringResource(R.string.goals_new_title),
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
                            text = { Text(stringResource(R.string.goals_create_tab)) },
                            selectedContentColor = Color.Black,
                            unselectedContentColor = Color(0xFF2E4158)
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text(stringResource(R.string.goals_habits_tab)) },
                            selectedContentColor = Color.Black,
                            unselectedContentColor = Color(0xFF2E4158)
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text(stringResource(R.string.goals_programs_tab)) },
                            selectedContentColor = Color.Black,
                            unselectedContentColor = Color(0xFF2E4158)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when (selectedTab) {
                        0 -> {
                            Column(
                                modifier = Modifier
                                    .height(500.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(stringResource(R.string.goals_goal_title), fontWeight = FontWeight.Bold, color = Color.Black)
                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text(stringResource(R.string.goals_title_example)) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = darkFieldColors
                                )

                                Text(stringResource(R.string.goals_target_per_session), fontWeight = FontWeight.Bold, color = Color.Black)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = executionTargetText,
                                        onValueChange = { executionTargetText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                                        modifier = Modifier.width(92.dp),
                                        label = { Text(stringResource(R.string.goals_amount)) },
                                        colors = fieldColors,
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = customUnitLabel,
                                        onValueChange = { customUnitLabel = it },
                                        modifier = Modifier.weight(1f),
                                        label = { Text(stringResource(R.string.goals_unit_examples_label)) },
                                        colors = fieldColors,
                                        singleLine = true
                                    )
                                }
                                Text(stringResource(R.string.goals_unit_examples), style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)

                                ConfigSelector(
                                    label = stringResource(R.string.goals_ends_by),
                                    options = GoalCompletionBasis.entries.map { it to localizedCompletionBasis(it) },
                                    selected = completionBasis,
                                    onSelected = {
                                        completionBasis = it
                                        if (it == GoalCompletionBasis.DURATION && scheduleType == GoalScheduleType.SPECIFIC_DATES) {
                                            scheduleType = GoalScheduleType.INTERVAL
                                        }
                                    }
                                )

                                if (completionBasis == GoalCompletionBasis.EXECUTIONS) {
                                    OutlinedTextField(
                                        value = amountText,
                                        onValueChange = { amountText = it.filter(Char::isDigit) },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text(stringResource(R.string.goals_total_sessions)) },
                                        colors = fieldColors,
                                        singleLine = true
                                    )
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = durationValueText,
                                            onValueChange = { durationValueText = it.filter(Char::isDigit) },
                                            modifier = Modifier.width(110.dp),
                                            label = { Text(stringResource(R.string.goals_duration)) },
                                            colors = fieldColors,
                                            singleLine = true
                                        )
                                        ConfigSelector(
                                            label = stringResource(R.string.goals_unit),
                                            options = listOf(TimeUnitType.DIAS, TimeUnitType.SEMANAS, TimeUnitType.MESES, TimeUnitType.ANIOS).map { it to localizedTimeUnit(it, 2) },
                                            selected = durationUnit,
                                            onSelected = { durationUnit = it },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                ConfigSelector(
                                    label = stringResource(R.string.goals_schedule),
                                    options = GoalScheduleType.entries
                                        .filter { completionBasis == GoalCompletionBasis.EXECUTIONS || it != GoalScheduleType.SPECIFIC_DATES }
                                        .map { it to localizedScheduleType(it) },
                                    selected = scheduleType,
                                    onSelected = { scheduleType = it }
                                )

                                when (scheduleType) {
                                    GoalScheduleType.INTERVAL -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = frequencyText,
                                            onValueChange = { frequencyText = it.filter(Char::isDigit) },
                                            modifier = Modifier.width(90.dp),
                                            label = { Text(stringResource(R.string.goals_every)) },
                                            colors = fieldColors,
                                            singleLine = true
                                        )
                                        ConfigSelector(
                                            label = stringResource(R.string.goals_time_unit),
                                            options = TimeUnitType.entries.map { it to localizedTimeUnit(it, 2) },
                                            selected = selectedUnit,
                                            onSelected = { selectedUnit = it },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    GoalScheduleType.WEEKLY -> OutlinedTextField(
                                        value = weeklyDaysText,
                                        onValueChange = { weeklyDaysText = it.filter(Char::isDigit) },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text(stringResource(R.string.goals_days_per_week_input)) },
                                        colors = fieldColors,
                                        singleLine = true
                                    )
                                    GoalScheduleType.SPECIFIC_DATES -> {
                                        Text(stringResource(R.string.goals_session_dates), fontWeight = FontWeight.Bold, color = Color.Black)
                                        specificDates.forEachIndexed { index, date ->
                                            val context = LocalContext.current
                                            Button(onClick = {
                                                val calendar = java.util.Calendar.getInstance().apply { timeInMillis = date }
                                                android.app.DatePickerDialog(
                                                    context,
                                                    { _, year, month, day ->
                                                        specificDates[index] = java.util.Calendar.getInstance().apply {
                                                            set(year, month, day, 0, 0, 0)
                                                            set(java.util.Calendar.MILLISECOND, 0)
                                                        }.timeInMillis
                                                    },
                                                    calendar.get(java.util.Calendar.YEAR),
                                                    calendar.get(java.util.Calendar.MONTH),
                                                    calendar.get(java.util.Calendar.DAY_OF_MONTH)
                                                ).show()
                                            }) {
                                                Text(stringResource(R.string.goals_session_date_value, index + 1, formatDateOnly(date)))
                                            }
                                        }
                                    }
                                }

                                ConfigSelector(
                                    label = stringResource(R.string.goals_time_of_day),
                                    options = GoalDayPeriod.entries.map { it to localizedDayPeriod(it) },
                                    selected = dayPeriod,
                                    onSelected = { dayPeriod = it }
                                )

                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.goals_notify_available), color = Color.Black, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                    Switch(checked = notifyOnUnitAvailable, onCheckedChange = { notifyOnUnitAvailable = it })
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.68f), RoundedCornerShape(14.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.goals_summary),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = buildGoalCreationSummary(
                                            title,
                                            executionTargetText,
                                            customUnitLabel,
                                            completionBasis,
                                            amountText,
                                            durationValueText,
                                            durationUnit,
                                            scheduleType,
                                            frequencyText,
                                            selectedUnit,
                                            weeklyDaysText,
                                            dayPeriod
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Black
                                    )
                                }

                                TextButton(onClick = { showDescription = !showDescription }) {
                                    Text(
                                        if (showDescription) stringResource(R.string.goals_hide_description)
                                        else stringResource(R.string.goals_show_description),
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (showDescription) {
                                    OutlinedTextField(
                                        value = description,
                                        onValueChange = { description = it },
                                        modifier = Modifier.fillMaxWidth().height(130.dp),
                                        label = { Text(stringResource(R.string.goals_description)) },
                                        colors = darkFieldColors
                                    )
                                }
                            }
                        }

                        1 -> {
                            OutlinedTextField(
                                value = habitTitleFilter,
                                onValueChange = { habitTitleFilter = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.goals_filter_title)) },
                                shape = RoundedCornerShape(14.dp),
                                colors = fieldColors,
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = habitContentFilter,
                                onValueChange = { habitContentFilter = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.goals_filter_content)) },
                                shape = RoundedCornerShape(14.dp),
                                colors = fieldColors,
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(
                                modifier = Modifier.height(420.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredHabits) { habit ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = habitCardColor,
                                            contentColor = habitCardContentColor
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(habit.title, fontWeight = FontWeight.Bold)
                                            Text(habit.description, maxLines = 4, overflow = TextOverflow.Ellipsis)
                                            Text(
                                                habitScheduleSummary(habit),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = habitCardContentColor.copy(alpha = 0.75f)
                                            )
                                            Row(modifier = Modifier.fillMaxWidth()) {
                                                Spacer(modifier = Modifier.weight(1f))
                                                Button(onClick = {
                                                    title = habit.title
                                                    description = habit.description
                                                    amountText = habit.noUnidades.toString()
                                                    frequencyText = habit.noFrecuencias.toString()
                                                    selectedUnit = TimeUnitType.DIAS
                                                    scheduleType = habit.scheduleType
                                                    weeklyDaysText = habit.weeklyDaysPerWeek.toString()
                                                    dayPeriod = habit.dayPeriod
                                                    customUnitLabel = habit.customUnitLabel
                                                    executionTargetText = "1"
                                                    completionBasis = GoalCompletionBasis.EXECUTIONS
                                                    notifyOnUnitAvailable = false
                                                    selectedTab = 0
                                                }) {
                                                    Text(stringResource(R.string.goals_load))
                                                }
                                            }
                                        }
                                    }
                                }
                                if (filteredHabits.isEmpty()) {
                                    item {
                                        Text(
                                            text = stringResource(R.string.goals_no_matching_habits),
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
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFEB3B)
                                    )
                                    TextButton(onClick = { selectedProgramaGroup = null }) {
                                        Text(stringResource(R.string.goals_back))
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
                                                .clickable { selectedProgramaGroup = group },
                                            colors = CardDefaults.cardColors(
                                                containerColor = programCardColor,
                                                contentColor = programCardContentColor
                                            )
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
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = programCardColor,
                                                contentColor = programCardContentColor
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(programa.title, fontWeight = FontWeight.Bold)
                                                Text(programa.description, maxLines = 4, overflow = TextOverflow.Ellipsis)
                                                Text(
                                                    programScheduleSummary(programa),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = programCardContentColor.copy(alpha = 0.75f)
                                                )
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Button(onClick = { previewPrograma = programa }) {
                                                        Text(stringResource(R.string.goals_summary))
                                                    }
                                                    Spacer(modifier = Modifier.weight(1f))
                                                    Button(onClick = {
                                                        dbExecutor.execute {
                                                            repository.createProgramGoal(programa)
                                                            activity?.runOnUiThread { onCreated() }
                                                        }
                                                    }) {
                                                        Text(stringResource(R.string.goals_start_program))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    item {
                                        Text(
                                            text = stringResource(R.string.goals_category_value, readableProgramaGroup(group)),
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(top = 4.dp, start = 4.dp, bottom = 2.dp),
                                            color = Color.White
                                        )
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel), color = Color.Black) }
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
                                            notifyOnUnitAvailable = notifyOnUnitAvailable,
                                            scheduleType = scheduleType,
                                            weeklyDaysPerWeek = weeklyDaysText.toIntOrNull() ?: 3,
                                            dayPeriod = dayPeriod,
                                            customUnitLabel = customUnitLabel,
                                            executionTargetValue = executionTargetText.replace(',', '.').toDoubleOrNull() ?: 0.0,
                                            completionBasis = completionBasis,
                                            durationValue = durationValueText.toIntOrNull() ?: 30,
                                            durationUnit = durationUnit,
                                            specificDates = specificDates.toList()
                                        )
                                        activity?.runOnUiThread { onCreated() }
                                    }
                                },
                                enabled = title.trim().isNotEmpty() &&
                                    (amountText.toIntOrNull() ?: 0) > 0 &&
                                    (executionTargetText.replace(',', '.').toDoubleOrNull() ?: 0.0) > 0 &&
                                    (completionBasis != GoalCompletionBasis.DURATION || (durationValueText.toIntOrNull() ?: 0) > 0) &&
                                    (scheduleType != GoalScheduleType.SPECIFIC_DATES || specificDates.size == (amountText.toIntOrNull() ?: 0))
                            ) {
                                Text(stringResource(R.string.goals_create_tab), color = Color.Black)
                            }
                        }
                    }
                }
            }
        }

        previewPrograma?.let { programa ->
            ProgramSummaryDialog(
                programa = programa,
                onDismiss = { previewPrograma = null }
            )
        }
    }

    //Vista de resumen de un programa
    @Composable
    private fun ProgramSummaryDialog(
        programa: ProgramaPreestablecido,
        onDismiss: () -> Unit
    ) {
        val scroll = rememberScrollState()
        val expandedUnits = remember(programa.fileBaseName) { mutableStateListOf<Int>() }

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
                                    Color(0xFFE9F6EC),
                                    Color(0xFFD6EEDC),
                                    Color(0xFFBFE2CB)
                                )
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(14.dp)
                        .verticalScroll(scroll),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(stringResource(R.string.goals_program_summary), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.goals_title_value, programa.title), fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(stringResource(R.string.goals_details_value, programa.detalles.ifBlank { stringResource(R.string.goals_no_details) }), color = Color.Black)
                    Text(stringResource(R.string.goals_description_value, programa.description.ifBlank { stringResource(R.string.goals_no_description) }), color = Color.Black)
                    Text(stringResource(R.string.goals_schedule_value, programScheduleSummary(programa)), color = Color.Black, fontWeight = FontWeight.SemiBold)

                    HorizontalDivider()
                    Text(stringResource(R.string.goals_units), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                    programa.unidadesinfo.forEachIndexed { index, unit ->
                        val unitNumber = index + 1
                        val isExpanded = expandedUnits.contains(index)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isExpanded) expandedUnits.remove(index) else expandedUnits.add(index)
                                },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                val unitTitle = unit.name.ifBlank { stringResource(R.string.goals_unit_number, unitNumber) }
                                Text(
                                    text = "$unitTitle ${if (isExpanded) "▲" else "▼"}",
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (isExpanded) {
                                    Spacer(Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = Color(0xFFE6F2E9),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = unit.info.ifBlank { stringResource(R.string.goals_no_unit_content) },
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close), color = Color.Black) }
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

    @Composable
    private fun <T> ConfigSelector(
        label: String,
        options: List<Pair<T, String>>,
        selected: T,
        onSelected: (T) -> Unit,
        modifier: Modifier = Modifier
    ) {
        var expanded by remember { mutableStateOf(false) }
        val selectedLabel = options.firstOrNull { it.first == selected }?.second.orEmpty()
        Box(modifier = modifier) {
            Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.goals_selector_value, label, selectedLabel), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (value, text) ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            onSelected(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun buildGoalCreationSummary(
        title: String,
        targetText: String,
        unitLabel: String,
        completionBasis: GoalCompletionBasis,
        amountText: String,
        durationText: String,
        durationUnit: TimeUnitType,
        scheduleType: GoalScheduleType,
        frequencyText: String,
        intervalUnit: TimeUnitType,
        weeklyDaysText: String,
        dayPeriod: GoalDayPeriod
    ): String {
        val target = targetText.replace(',', '.').toDoubleOrNull() ?: 0.0
        val number = if (target % 1.0 == 0.0) target.toInt().toString() else targetText
        val action = when {
            unitLabel.isNotBlank() && target == 1.0 -> stringResource(R.string.goals_action_named, unitLabel.trim())
            unitLabel.isNotBlank() -> stringResource(R.string.goals_action_named, "$number ${unitLabel.trim()}")
            target == 1.0 -> stringResource(R.string.goals_action_one)
            else -> stringResource(R.string.goals_action_number, number)
        }
        val frequency = (frequencyText.toIntOrNull() ?: 1).coerceAtLeast(1)
        val cadence = when (scheduleType) {
            GoalScheduleType.INTERVAL -> stringResource(
                R.string.goals_once_every,
                "${if (frequency == 1) "" else "$frequency "}${localizedTimeUnit(intervalUnit, frequency)}"
            )
            GoalScheduleType.WEEKLY -> stringResource(
                R.string.goals_weekly_cadence,
                (weeklyDaysText.toIntOrNull() ?: 3).coerceIn(1, 7)
            )
            GoalScheduleType.SPECIFIC_DATES -> stringResource(
                R.string.goals_selected_dates_cadence,
                amountText.toIntOrNull() ?: 0
            )
        }
        val cadenceWithPeriod = if (dayPeriod == GoalDayPeriod.ANYTIME) cadence else "$cadence · ${localizedDayPeriod(dayPeriod)}"
        val ending = if (completionBasis == GoalCompletionBasis.EXECUTIONS) {
            val amount = amountText.toIntOrNull() ?: 0
            val sessions = stringResource(
                if (amount == 1) R.string.goals_execution_singular else R.string.goals_execution_plural,
                amount
            )
            stringResource(R.string.goals_ending_executions, sessions)
        } else {
            val duration = (durationText.toIntOrNull() ?: 0).coerceAtLeast(0)
            stringResource(R.string.goals_ending_duration, duration, localizedTimeUnit(durationUnit, duration))
        }
        return if (title.isBlank()) {
            stringResource(R.string.goals_creation_summary_unnamed, action, cadenceWithPeriod, ending)
        } else {
            stringResource(R.string.goals_creation_summary_named, title.trim(), action, cadenceWithPeriod, ending)
        }
    }

    @Composable
    private fun habitScheduleSummary(habit: HabitPreset): String {
        val cadence = when (habit.scheduleType) {
            GoalScheduleType.INTERVAL -> stringResource(
                R.string.goals_interval_every,
                "${if (habit.noFrecuencias == 1) "" else "${habit.noFrecuencias} "}${localizedTimeUnit(TimeUnitType.DIAS, habit.noFrecuencias)}"
            )
            GoalScheduleType.WEEKLY -> stringResource(R.string.goals_weekly_cadence, habit.weeklyDaysPerWeek.coerceIn(1, 7))
            GoalScheduleType.SPECIFIC_DATES -> stringResource(R.string.goals_specific_dates)
        }
        return if (habit.dayPeriod == GoalDayPeriod.ANYTIME) cadence else "$cadence · ${localizedDayPeriod(habit.dayPeriod)}"
    }

    @Composable
    private fun programScheduleSummary(program: ProgramaPreestablecido): String {
        val unit = TimeUnitType.fromRaw(program.tipoUnidad)
        val cadence = when (program.scheduleType) {
            GoalScheduleType.INTERVAL -> stringResource(
                R.string.goals_interval_every,
                "${if (program.frecuencia == 1) "" else "${program.frecuencia} "}${localizedTimeUnit(unit, program.frecuencia)}"
            )
            GoalScheduleType.WEEKLY -> stringResource(R.string.goals_weekly_cadence, program.weeklyDaysPerWeek.coerceIn(1, 7))
            GoalScheduleType.SPECIFIC_DATES -> stringResource(R.string.goals_specific_dates)
        }
        return if (program.dayPeriod == GoalDayPeriod.ANYTIME) cadence else "$cadence · ${localizedDayPeriod(program.dayPeriod)}"
    }

    @Composable
    private fun localizedPlanSummary(state: GoalCardState): String {
        val value = state.goal.executionTargetValue.takeIf { it > 0 } ?: 1.0
        val number = if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value).trimEnd('0').trimEnd('.')
        val label = state.goal.customUnitLabel.trim()
        val execution = when {
            label.isNotEmpty() -> stringResource(R.string.goals_per_session, "$number $label")
            value == 1.0 -> stringResource(R.string.goals_one_session)
            else -> stringResource(R.string.goals_per_session, number)
        }
        val cadence = when (state.scheduleType) {
            GoalScheduleType.INTERVAL -> stringResource(
                R.string.goals_interval_every,
                "${if (state.goal.frequency == 1) "" else "${state.goal.frequency} "}${localizedTimeUnit(state.unitType, state.goal.frequency)}"
            )
            GoalScheduleType.WEEKLY -> stringResource(R.string.goals_weekly_cadence, state.goal.weeklyDaysPerWeek.coerceIn(1, 7))
            GoalScheduleType.SPECIFIC_DATES -> stringResource(R.string.goals_specific_dates)
        }.let { if (state.dayPeriod == GoalDayPeriod.ANYTIME) it else "$it · ${localizedDayPeriod(state.dayPeriod)}" }
        val ending = when (state.completionBasis) {
            GoalCompletionBasis.EXECUTIONS -> stringResource(
                if (state.goal.totalUnits == 1) R.string.goals_execution_singular else R.string.goals_execution_plural,
                state.goal.totalUnits
            )
            GoalCompletionBasis.DURATION -> {
                val valueDuration = state.goal.durationValue.coerceAtLeast(1)
                stringResource(
                    R.string.goals_during_duration,
                    valueDuration,
                    localizedTimeUnit(TimeUnitType.fromRaw(state.goal.durationUnit), valueDuration)
                )
            }
        }
        return "$execution · $cadence · $ending"
    }

    @Composable
    private fun localizedTimeUntilNextUnit(state: GoalCardState, next: GoalUnitEntity?, now: Long): String? {
        if (!state.goal.isStarted || next == null) return null
        val start = next.startDate ?: return null
        if (now >= start) return stringResource(R.string.goals_ready)
        val seconds = kotlin.math.ceil((start - now) / 1000.0).toInt().coerceAtLeast(0)
        if (seconds < 60) return stringResource(R.string.goals_next_seconds, seconds)
        val totalMinutes = kotlin.math.ceil((start - now) / 60_000.0).toInt().coerceAtLeast(1)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) stringResource(R.string.goals_next_hours_minutes, hours, minutes)
        else stringResource(R.string.goals_next_minutes, totalMinutes)
    }

    @Composable
    private fun localizedTimeUnit(unit: TimeUnitType, value: Int): String = pluralStringResource(
        when (unit) {
            TimeUnitType.MINUTOS -> R.plurals.goals_minutes
            TimeUnitType.HORAS -> R.plurals.goals_hours
            TimeUnitType.DIAS -> R.plurals.goals_days
            TimeUnitType.SEMANAS -> R.plurals.goals_weeks
            TimeUnitType.MESES -> R.plurals.goals_months
            TimeUnitType.ANIOS -> R.plurals.goals_years
        },
        value.coerceAtLeast(0)
    )

    @Composable
    private fun localizedScheduleType(value: GoalScheduleType): String = stringResource(
        when (value) {
            GoalScheduleType.INTERVAL -> R.string.goals_schedule_interval
            GoalScheduleType.WEEKLY -> R.string.goals_schedule_weekly
            GoalScheduleType.SPECIFIC_DATES -> R.string.goals_schedule_specific
        }
    )

    @Composable
    private fun localizedCompletionBasis(value: GoalCompletionBasis): String = stringResource(
        when (value) {
            GoalCompletionBasis.EXECUTIONS -> R.string.goals_basis_executions
            GoalCompletionBasis.DURATION -> R.string.goals_basis_duration
        }
    )

    @Composable
    private fun localizedDayPeriod(value: GoalDayPeriod): String = stringResource(
        when (value) {
            GoalDayPeriod.ANYTIME -> R.string.goals_period_anytime
            GoalDayPeriod.MORNING -> R.string.goals_period_morning
            GoalDayPeriod.AFTERNOON -> R.string.goals_period_afternoon
            GoalDayPeriod.NIGHT -> R.string.goals_period_night
        }
    )

    private fun formatUnitSchedule(epoch: Long, schedule: GoalScheduleType, period: GoalDayPeriod): String {
        val pattern = if (schedule == GoalScheduleType.SPECIFIC_DATES || schedule == GoalScheduleType.WEEKLY) {
            "EEE, d MMM yyyy"
        } else if (period != GoalDayPeriod.ANYTIME) {
            "EEE HH:mm"
        } else {
            "dd/MM/yyyy HH:mm"
        }
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(epoch))
    }

    private fun formatDateOnly(epoch: Long): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(epoch))

    @Composable
    private fun readableProgramaGroup(raw: String): String {
        val resource = when (raw) {
            "prog_anti_ansiedad" -> R.string.goals_program_category_anti_anxiety
            "prog_dejar_alcohol" -> R.string.goals_program_category_quit_alcohol
            "prog_dejar_fumar" -> R.string.goals_program_category_quit_smoking
            "prog_dieta_semanal" -> R.string.goals_program_category_weekly_diet
            "prog_eliminar_antojos" -> R.string.goals_program_category_eliminate_cravings
            "prog_regulacion_digital_menores" -> R.string.goals_program_category_digital_regulation_minors
            "prog_reset_dopaminergico" -> R.string.goals_program_category_dopamine_reset
            "prog_respiracion_buteyko" -> R.string.goals_program_category_buteyko_breathing
            "prog_visualizacion_creativa_neville" -> R.string.goals_program_category_neville_visualization
            else -> null
        }
        return resource?.let { stringResource(it) } ?: raw.removePrefix("prog_")
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
