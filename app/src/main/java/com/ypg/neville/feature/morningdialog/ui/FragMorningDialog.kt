package com.ypg.neville.feature.morningdialog.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ypg.neville.feature.morningdialog.data.MorningDialogSettingsDataStore
import com.ypg.neville.feature.morningdialog.data.RoomMorningDialogRepository
import com.ypg.neville.feature.morningdialog.ui.navigation.MorningDialogRoutes
import com.ypg.neville.feature.morningdialog.ui.screens.MorningDialogFlowScreen
import com.ypg.neville.feature.morningdialog.ui.screens.MorningDialogHistoryScreen
import com.ypg.neville.feature.morningdialog.ui.screens.MorningDialogHomeScreen
import com.ypg.neville.feature.morningdialog.ui.screens.MorningDialogNoteScreen
import com.ypg.neville.feature.morningdialog.ui.screens.MorningDialogSettingsScreen
import com.ypg.neville.feature.morningdialog.ui.viewmodel.MorningDialogFlowViewModel
import com.ypg.neville.feature.morningdialog.ui.viewmodel.MorningDialogHubViewModel
import com.ypg.neville.feature.morningdialog.notifications.MorningDialogScheduler
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import com.ypg.neville.model.db.room.DiarioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FragMorningDialog : Fragment() {

    private val repository by lazy {
        val db = NevilleRoomDatabase.getInstance(requireContext().applicationContext)
        RoomMorningDialogRepository(db.morningDialogDao())
    }

    private val settingsStore by lazy {
        MorningDialogSettingsDataStore(requireContext().applicationContext)
    }

    private val scheduler by lazy {
        MorningDialogScheduler(requireContext().applicationContext)
    }

    private val hubViewModel: MorningDialogHubViewModel by viewModels {
        MorningDialogHubViewModel.Factory(settingsStore, repository, scheduler)
    }

    private val flowViewModel: MorningDialogFlowViewModel by viewModels {
        MorningDialogFlowViewModel.Factory(repository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                com.ypg.neville.ui.theme.NevilleTheme {
                    MorningDialogRoot(
                        hubViewModel = hubViewModel,
                        flowViewModel = flowViewModel,
                        startInFlow = arguments?.getBoolean(ARG_START_FLOW, false) == true,
                        initialOpenSessionId = arguments?.getLong(ARG_OPEN_SESSION_ID, -1L) ?: -1L,
                        scheduler = scheduler,
                        onExportSessionToDiary = { sessionId -> exportSessionToDiary(sessionId) },
                        onDeleteSession = { sessionId -> deleteSessionFromHistory(sessionId) },
                        onClose = { parentFragmentManager.popBackStack() }
                    )
                }
            }
        }
    }

    companion object {
        const val ARG_START_FLOW = "arg_start_flow"
        const val ARG_OPEN_SESSION_ID = "arg_open_session_id"
    }

    private fun exportSessionToDiary(sessionId: Long) {
        val appContext = requireContext().applicationContext
        lifecycleScope.launch(Dispatchers.IO) {
            val session = repository.getSession(sessionId)
            if (session == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "No se encontró el ritual para exportar.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val dateLabel = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                .format(Date(session.completedAtEpochMillis))
            val dateTimeLabel = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(Date(session.completedAtEpochMillis))

            val title = "Ritual del dia $dateLabel"
            val content = buildString {
                appendLine("Fecha de ritual: $dateTimeLabel")
                appendLine()
                appendLine("Metas:")
                if (session.goals.isEmpty()) appendLine("-")
                else session.goals.forEach { appendLine("- $it") }
                appendLine()
                appendLine("Identidad:")
                appendLine(session.identity.ifBlank { "-" })
                appendLine()
                appendLine("Emociones:")
                if (session.emotions.isEmpty()) appendLine("-")
                else session.emotions.forEach { appendLine("- $it") }
                appendLine()
                appendLine("Situaciones y respuestas:")
                if (session.anticipatedSituations.isEmpty()) {
                    appendLine("-")
                } else {
                    session.anticipatedSituations.forEachIndexed { index, trigger ->
                        val response = session.consciousResponses.getOrElse(index) { "" }
                        appendLine("- Si $trigger, responderé con $response")
                    }
                }
                appendLine()
                appendLine("Nota del ritual:")
                appendLine(session.noteText.ifBlank { "-" })
            }.trim()

            val db = NevilleRoomDatabase.getInstance(appContext)
            val diarioRepository = DiarioRepository(db.diarioDao())
            val exportDao = db.ritualDiaryExportDao()
            var wasUpdated = false

            db.runInTransaction {
                val existingExport = exportDao.getBySessionId(sessionId)
                if (existingExport != null) {
                    diarioRepository.actualizarTituloYContenido(
                        id = existingExport.diarioId,
                        title = title,
                        content = content
                    )
                    wasUpdated = true
                    return@runInTransaction
                }

                val diarioId = diarioRepository.insertar(
                    title = title,
                    content = content,
                    emocion = "\uD83D\uDE0C",
                    isFav = false,
                    fechaCreacionMillis = session.completedAtEpochMillis
                )

                exportDao.insert(
                    com.ypg.neville.feature.morningdialog.data.RitualDiaryExportEntity(
                        sessionId = sessionId,
                        diarioId = diarioId,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    requireContext(),
                    if (wasUpdated) "Entrada de Diario Actualizada"
                    else "Entrada de Diario Creada",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun deleteSessionFromHistory(sessionId: Long) {
        val appContext = requireContext().applicationContext
        lifecycleScope.launch(Dispatchers.IO) {
            val db = NevilleRoomDatabase.getInstance(appContext)
            db.runInTransaction {
                db.ritualDiaryExportDao().deleteBySessionId(sessionId)
                db.morningDialogDao().deleteById(sessionId)
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Ritual eliminado del historial.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MorningDialogRoot(
    hubViewModel: MorningDialogHubViewModel,
    flowViewModel: MorningDialogFlowViewModel,
    startInFlow: Boolean,
    initialOpenSessionId: Long,
    scheduler: MorningDialogScheduler,
    onExportSessionToDiary: (Long) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onClose: () -> Unit
) {
    val navController = rememberNavController()
    val hubState by hubViewModel.uiState.collectAsState()
    val flowState by flowViewModel.uiState.collectAsState()
    val selectedSession by hubViewModel.selectedSession.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    var startNavigationHandled by remember { mutableStateOf(false) }

    LaunchedEffect(startInFlow) {
        if (startInFlow && !startNavigationHandled) {
            startNavigationHandled = true
            val todaySessionId = hubViewModel.getTodayCompletedSessionId()
            if (todaySessionId != null && todaySessionId > 0L) {
                hubViewModel.loadSession(todaySessionId)
                navController.navigate(MorningDialogRoutes.HISTORY) {
                    popUpTo(MorningDialogRoutes.HOME) { inclusive = true }
                }
            } else {
                navController.navigate(MorningDialogRoutes.FLOW) {
                    popUpTo(MorningDialogRoutes.HOME) { inclusive = true }
                }
            }
        }
    }
    LaunchedEffect(initialOpenSessionId) {
        if (initialOpenSessionId > 0L) {
            hubViewModel.loadSession(initialOpenSessionId)
            navController.navigate(MorningDialogRoutes.HISTORY)
        }
    }

    val route = backStackEntry?.destination?.route.orEmpty()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            route.startsWith("morning_note") -> "Nota"
                            route == MorningDialogRoutes.FLOW -> "Diálogo guiado"
                            route == MorningDialogRoutes.HISTORY -> "Historial"
                            route == MorningDialogRoutes.SETTINGS -> "Ajustes"
                            else -> "Ritual Matutino"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!navController.popBackStack()) onClose()
                    }) {
                        Text(
                            text = "←",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = MorningDialogRoutes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(MorningDialogRoutes.HOME) {
                MorningDialogHomeScreen(
                    todayCompleted = hubState.todayCompleted,
                    onStartFlow = {
                        flowViewModel.clearCompletionFlag()
                        flowViewModel.setStep(1)
                        navController.navigate(MorningDialogRoutes.FLOW)
                    },
                    onOpenHistory = { navController.navigate(MorningDialogRoutes.HISTORY) },
                    onOpenSettings = { navController.navigate(MorningDialogRoutes.SETTINGS) }
                )
            }

            composable(MorningDialogRoutes.FLOW) {
                MorningDialogFlowScreen(
                    state = flowState,
                    onNext = { flowViewModel.goNext() },
                    onBack = { flowViewModel.goBack() },
                    onFinish = {
                        flowViewModel.complete { sessionId, remindersEnabled, reminderTimes ->
                            if (sessionId > 0L) {
                                if (remindersEnabled && reminderTimes.isNotEmpty()) {
                                    scheduler.scheduleSessionDayReminders(sessionId, reminderTimes)
                                } else {
                                    scheduler.cancelSessionDayReminders(sessionId)
                                }
                            }
                            navController.navigate(MorningDialogRoutes.HOME) {
                                popUpTo(MorningDialogRoutes.HOME) { inclusive = true }
                            }
                        }
                    },
                    onGoalChange = flowViewModel::updateGoal,
                    onAddGoal = flowViewModel::addGoal,
                    onRemoveGoal = flowViewModel::removeGoal,
                    onToggleIdentity = flowViewModel::toggleIdentity,
                    onCustomIdentityChange = flowViewModel::updateCustomIdentity,
                    onToggleEmotion = flowViewModel::toggleEmotion,
                    onCustomEmotionChange = flowViewModel::updateCustomEmotion,
                    onTriggerChange = flowViewModel::updateTrigger,
                    onResponseChange = flowViewModel::updateResponse,
                    onAddTriggerResponse = flowViewModel::addTriggerResponse,
                    onRemoveTriggerResponse = flowViewModel::removeTriggerResponse,
                    onStepChange = flowViewModel::setStep,
                    onSetDayRemindersEnabled = flowViewModel::setDayRemindersEnabled,
                    onAddDayReminderTime = flowViewModel::addDayReminderTime,
                    onRemoveDayReminderTime = flowViewModel::removeDayReminderTime,
                    onCloseAfterFinish = {
                        navController.navigate(MorningDialogRoutes.HOME) {
                            popUpTo(MorningDialogRoutes.HOME) { inclusive = true }
                        }
                    }
                )
            }

            composable(MorningDialogRoutes.HISTORY) {
                MorningDialogHistoryScreen(
                    sessions = hubState.sessions,
                    onNoteClick = { sessionId ->
                        navController.navigate(MorningDialogRoutes.note(sessionId))
                    },
                    onExportClick = { sessionId ->
                        onExportSessionToDiary(sessionId)
                    },
                    onDeleteClick = { sessionId ->
                        onDeleteSession(sessionId)
                    }
                )
            }

            composable(
                route = MorningDialogRoutes.NOTE,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { entry ->
                val sessionId = entry.arguments?.getLong("sessionId") ?: 0L
                LaunchedEffect(sessionId) {
                    hubViewModel.loadSession(sessionId)
                }
                MorningDialogNoteScreen(
                    initialNote = selectedSession?.noteText.orEmpty(),
                    onSave = { note ->
                        hubViewModel.saveSessionNote(sessionId, note) {
                            navController.popBackStack()
                        }
                    }
                )
            }

            composable(MorningDialogRoutes.SETTINGS) {
                MorningDialogSettingsScreen(
                    settings = hubState.settings,
                    onSaveSettings = { enabled, hour, minute ->
                        hubViewModel.applySettings(enabled, hour, minute)
                    }
                )
            }
        }
    }
}
