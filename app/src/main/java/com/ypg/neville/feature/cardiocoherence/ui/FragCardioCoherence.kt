package com.ypg.neville.feature.cardiocoherence.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.ypg.neville.R
import com.ypg.neville.feature.cardiocoherence.data.CardioCoherenceRepository
import com.ypg.neville.feature.cardiocoherence.domain.BreathingRhythmOption
import com.ypg.neville.feature.cardiocoherence.domain.InitialEmotionalState
import com.ypg.neville.feature.cardiocoherence.domain.MeditationPhase
import com.ypg.neville.feature.cardiocoherence.domain.PostSessionEmotion
import com.ypg.neville.feature.cardiocoherence.domain.SessionDurationOption
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

class FragCardioCoherence : Fragment() {

    private val viewModel: CardioCoherenceViewModel by viewModels {
        val db = NevilleRoomDatabase.getInstance(requireContext().applicationContext)
        CardioCoherenceViewModel.Factory(
            repository = CardioCoherenceRepository(db.meditationSessionRecordDao()),
            initialBreathingRhythm = requireContext().savedCardioBreathingRhythm()
        )
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
                    CardioCoherenceRoot(
                        viewModel = viewModel,
                        onClose = { requireActivity().onBackPressedDispatcher.onBackPressed() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onPause()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardioCoherenceRoot(
    viewModel: CardioCoherenceViewModel,
    onClose: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current
    val backgroundAssets = remember(context) { context.cardioCoherenceBackgroundAssets() }
    var selectedBackgroundAsset by remember { mutableStateOf<String?>(null) }
    var showStats by remember { mutableStateOf(false) }

    LaunchedEffect(backgroundAssets) {
        selectedBackgroundAsset = context.initialCardioCoherenceBackgroundAsset(backgroundAssets)
    }

    fun selectNextBackground() {
        val nextAsset = context.nextCardioCoherenceBackgroundAsset(
            assets = backgroundAssets,
            currentAsset = selectedBackgroundAsset
        )
        selectedBackgroundAsset = nextAsset
    }

    LaunchedEffect(state.currentPhaseIndex, state.stage) {
        if (state.stage == CardioCoherenceStage.SESSION && state.elapsedSeconds > 0) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(backgroundAssets, selectedBackgroundAsset) {
                detectTapGestures(onDoubleTap = { selectNextBackground() })
            }
    ) {
        CardioCoherenceBackground(assetPath = selectedBackgroundAsset)
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Coherencia Cardio-Cerebral") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = BackgroundTextColor,
                        navigationIconContentColor = BackgroundTextColor,
                        actionIconContentColor = BackgroundTextColor
                    ),
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Rounded.Close, contentDescription = "Cerrar")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showStats = true }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_item),
                                contentDescription = "Estadísticas"
                            )
                        }
                    }
                )
            }
        ) { padding ->
            CompositionLocalProvider(LocalContentColor provides TextPrimary) {
                AnimatedContent(
                    targetState = state.stage,
                    label = "cardio-stage",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) { stage ->
                    when (stage) {
                        CardioCoherenceStage.SETUP -> SetupScreen(
                            state = state,
                            onSelectState = viewModel::selectInitialState,
                            onSelectDuration = viewModel::selectDuration,
                            onSelectBreathingRhythm = {
                                context.persistCardioBreathingRhythm(it)
                                viewModel.selectBreathingRhythm(it)
                            },
                            onIntention = viewModel::updateIntention,
                            onBeforeScore = viewModel::updateBeforeScore,
                            onStart = viewModel::startSession
                        )

                        CardioCoherenceStage.SESSION -> SessionScreen(
                            state = state,
                            onPause = viewModel::pause,
                            onResume = viewModel::resume,
                            onFinish = viewModel::finishSession
                        )

                    CardioCoherenceStage.EVALUATION -> EvaluationScreen(
                        state = state,
                        onAfterScore = viewModel::updateAfterScore,
                        onMentalClarityScore = viewModel::updateMentalClarityScore,
                        onHeartConnectionScore = viewModel::updateHeartConnectionScore,
                        onPredominantEmotion = viewModel::updatePredominantEmotion,
                        onClosingWord = viewModel::updateClosingWord,
                        onSave = viewModel::saveEvaluation
                    )

                        CardioCoherenceStage.SUMMARY -> SummaryScreen(
                            state = state,
                            onRestart = viewModel::resetFlow,
                            onClose = onClose
                        )
                    }
                }
            }
        }
    }

    if (showStats) {
        Dialog(
            onDismissRequest = { showStats = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            CardioCoherenceStatsScreen(
                records = state.records,
                onClose = { showStats = false },
                onRefresh = viewModel::refreshRecords
            )
        }
    }
}

@Composable
private fun CardioCoherenceBackground(assetPath: String?) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBrush())
    ) {
        Crossfade(
            targetState = assetPath,
            animationSpec = tween(durationMillis = CARDIO_BACKGROUND_CROSSFADE_MILLIS),
            label = "cardio-background-crossfade",
            modifier = Modifier.fillMaxSize()
        ) { targetAssetPath ->
            val imageBitmap = remember(context, targetAssetPath) {
                targetAssetPath?.let { context.loadCardioCoherenceBackgroundBitmap(it) }
            }

            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.38f),
                            Color(0xFF160D32).copy(alpha = 0.30f),
                            Color.Black.copy(alpha = 0.48f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun SetupScreen(
    state: CardioCoherenceUiState,
    onSelectState: (InitialEmotionalState) -> Unit,
    onSelectDuration: (SessionDurationOption) -> Unit,
    onSelectBreathingRhythm: (BreathingRhythmOption) -> Unit,
    onIntention: (String) -> Unit,
    onBeforeScore: (Int) -> Unit,
    onStart: () -> Unit
) {
    val context = LocalContext.current
    var showRhythmInfo by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .padding(bottom = 86.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Respira en ritmo, atiende al corazón y eleva tu estado interno.",
                style = MaterialTheme.typography.titleSmall,
                color = BackgroundTextColor.copy(alpha = 0.92f)
            )

            CalmPanel {
                SectionTitle("Estado emocional actual")
                Spacer(modifier = Modifier.height(8.dp))
                EmotionalStateDropdown(
                    selected = state.selectedState,
                    onSelected = onSelectState
                )
            }

            CalmPanel {
                SectionTitle("Tiempo disponible")
                Spacer(modifier = Modifier.height(8.dp))
                DurationDropdown(
                    selected = state.durationOption,
                    onSelected = onSelectDuration
                )
            }

            CalmPanel {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle("Ritmo respiratorio")
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { showRhythmInfo = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "Información sobre ritmo respiratorio",
                            tint = TextPrimary.copy(alpha = 0.62f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                BreathingRhythmDropdown(
                    selected = state.breathingRhythm,
                    onSelected = onSelectBreathingRhythm
                )
            }

            CalmPanel {
                SectionTitle("Intención")
                OutlinedTextField(
                    value = state.intention,
                    onValueChange = onIntention,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                    placeholder = { Text("Opcional", color = TextPrimary.copy(alpha = 0.55f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = TextPrimary,
                        unfocusedLabelColor = TextPrimary,
                        cursorColor = TextPrimary
                    )
                )
            }

            CalmPanel {
                ScoreSelector(
                    title = "Estrés / calma antes",
                    value = state.beforeScore,
                    onValueChange = onBeforeScore
                )
            }
        }

        Button(
            onClick = onStart,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
                .height(58.dp)
                .width(196.dp)
                .border(1.dp, SoftButtonBorder, CircleShape),
            shape = CircleShape,
            colors = softButtonColors()
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Iniciar sesión", fontWeight = FontWeight.Medium)
        }

        if (showRhythmInfo) {
            BreathingRhythmInfoDialog(
                onDismiss = { showRhythmInfo = false },
                onOpenStudy = {
                    context.openExternalUrl(BREATHING_RHYTHM_STUDY_URL)
                }
            )
        }
    }
}

@Composable
private fun SessionScreen(
    state: CardioCoherenceUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit
) {
    val session = state.session ?: return
    val phase = session.phases.getOrNull(state.currentPhaseIndex) ?: session.phases.first()
    val context = LocalContext.current
    val vibrator = remember(context) { context.cardioCoherenceVibrator() }
    val totalProgress = if (state.totalSeconds == 0) 0f else state.elapsedSeconds / state.totalSeconds.toFloat()
    val phaseProgress = if (phase.durationSeconds == 0) 0f else state.currentPhaseElapsedSeconds / phase.durationSeconds.toFloat()
    val currentGuidance = phase.currentGuidanceText(phaseProgress)

    LaunchedEffect(phase.kind, state.isPaused, state.isPreparing, vibrator) {
        if (state.isPaused || state.isPreparing) {
            vibrator?.cancel()
        } else {
            runBreathingVibrationPattern(vibrator, phase)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimerPill(Icons.Rounded.Timer, formatSeconds(state.remainingSeconds))
            LinearProgressIndicator(
                progress = { totalProgress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = AccentIndigo.copy(alpha = 0.72f),
                trackColor = Color.White.copy(alpha = 0.30f)
            )
            TimerPill(Icons.Rounded.Favorite, formatSeconds(state.currentPhaseRemainingSeconds))
        }

        Spacer(modifier = Modifier.height(22.dp))
        BreathingOrb(
            phase = phase,
            paused = state.isPaused,
            preparing = state.isPreparing,
            modifier = Modifier.size(240.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))
        BreathingCue(
            phase = phase,
            paused = state.isPaused,
            preparing = state.isPreparing,
            preparationRemainingSeconds = state.preparationRemainingSeconds
        )

        Spacer(modifier = Modifier.height(18.dp))
        SessionInfoPanel {
            Crossfade(
                targetState = SessionGuidanceDisplay(
                    title = phase.title,
                    guidance = currentGuidance,
                    emotionPrompt = phase.emotionCue?.prompt
                ),
                animationSpec = tween(durationMillis = SESSION_PHASE_CROSSFADE_MILLIS),
                label = "session-phase-guidance-crossfade"
            ) { guidanceDisplay ->
                SessionGuidanceContent(guidanceDisplay)
            }
            Spacer(modifier = Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { phaseProgress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = AccentViolet,
                trackColor = Color.White.copy(alpha = 0.56f)
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = if (state.isPaused) onResume else onPause,
                modifier = Modifier.weight(1f),
                shape = CircleShape,
                colors = softTonalButtonColors()
            ) {
                Icon(
                    imageVector = if (state.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (state.isPaused) "Reanudar" else "Pausar", fontWeight = FontWeight.Medium)
            }
            Button(
                onClick = onFinish,
                modifier = Modifier.weight(1f),
                shape = CircleShape,
                colors = softButtonColors()
            ) {
                Icon(Icons.Rounded.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Finalizar", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun EvaluationScreen(
    state: CardioCoherenceUiState,
    onAfterScore: (Int) -> Unit,
    onMentalClarityScore: (Int) -> Unit,
    onHeartConnectionScore: (Int) -> Unit,
    onPredominantEmotion: (PostSessionEmotion) -> Unit,
    onClosingWord: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CalmPanel {
            Text(
                text = "Evaluación final",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))
            ScoreSelector(
                title = "Calma / coherencia percibida",
                value = state.afterScore,
                onValueChange = onAfterScore
            )
            Spacer(modifier = Modifier.height(10.dp))
            ScoreSelector(
                title = "Claridad mental",
                value = state.mentalClarityScore,
                onValueChange = onMentalClarityScore
            )
            Spacer(modifier = Modifier.height(10.dp))
            ScoreSelector(
                title = "Conexión con el corazón",
                value = state.heartConnectionScore,
                onValueChange = onHeartConnectionScore
            )
            Spacer(modifier = Modifier.height(12.dp))
            SectionTitle("Emoción predominante después")
            Spacer(modifier = Modifier.height(8.dp))
            PostSessionEmotionDropdown(
                selected = state.predominantEmotion,
                onSelected = onPredominantEmotion
            )
            Spacer(modifier = Modifier.height(12.dp))
            SectionTitle("Palabra de cierre")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.closingWord,
                onValueChange = onClosingWord,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                placeholder = { Text("Ej. paz, confianza, gratitud", color = TextPrimary.copy(alpha = 0.55f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedLabelColor = TextPrimary,
                    unfocusedLabelColor = TextPrimary,
                    cursorColor = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onSave,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SoftButtonBorder, CircleShape),
                shape = CircleShape,
                colors = softButtonColors()
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (state.isSaving) "Guardando..." else "Guardar resultado",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SummaryScreen(
    state: CardioCoherenceUiState,
    onRestart: () -> Unit,
    onClose: () -> Unit
) {
    val completed = state.session?.phases
        ?.filter { phase -> phase.kind in completedKinds(state) }
        ?.joinToString(", ") { it.title }
        .orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CalmPanel {
            Text(
                text = "Sesión registrada",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Antes ${state.session?.userState?.beforeScore ?: state.beforeScore}/10 · Después ${state.afterScore}/10",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Claridad ${state.mentalClarityScore}/10 · Corazón ${state.heartConnectionScore}/10 · ${state.predominantEmotion.emoji} ${state.predominantEmotion.label}",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = TextPrimary.copy(alpha = 0.78f),
                modifier = Modifier.fillMaxWidth()
            )
            if (state.closingWord.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Palabra: ${state.closingWord.trim()}",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = TextPrimary.copy(alpha = 0.78f),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (completed.isBlank()) "Fases iniciadas: preparación de coherencia"
                else "Fases completadas: $completed",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = TextPrimary.copy(alpha = 0.78f),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    onClick = onRestart,
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, SoftButtonBorder, CircleShape),
                    shape = CircleShape,
                    colors = softTonalButtonColors()
                ) {
                    Icon(Icons.Rounded.Replay, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nueva", fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, SoftButtonBorder, CircleShape),
                    shape = CircleShape,
                    colors = softButtonColors()
                ) {
                    Text("Cerrar", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

private data class SessionGuidanceDisplay(
    val title: String,
    val guidance: String,
    val emotionPrompt: String?
)

@Composable
private fun SessionGuidanceContent(display: SessionGuidanceDisplay) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = display.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = BackgroundTextColor,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = display.guidance,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = BackgroundTextColor.copy(alpha = 0.90f),
            modifier = Modifier.fillMaxWidth()
        )
        display.emotionPrompt?.let { prompt ->
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = Color.White.copy(alpha = 0.18f),
                shape = RoundedCornerShape(8.dp),
                contentColor = BackgroundTextColor
            ) {
                Text(
                    text = prompt,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BackgroundTextColor.copy(alpha = 0.92f)
                )
            }
        }
    }
}

@Composable
private fun BreathingOrb(
    phase: MeditationPhase,
    paused: Boolean,
    preparing: Boolean,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(0.78f) }

    LaunchedEffect(phase.kind, paused, preparing) {
        if (preparing) {
            scale.animateTo(
                targetValue = 0.82f,
                animationSpec = tween(durationMillis = 420, easing = LinearEasing)
            )
            return@LaunchedEffect
        }
        while (isActive) {
            if (paused) {
                delay(180)
            } else {
                scale.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(
                        durationMillis = phase.breathingPattern.inhaleMillis,
                        easing = LinearEasing
                    )
                )
                delay(BREATHING_HAPTIC_TOP_PAUSE_MILLIS)
                scale.animateTo(
                    targetValue = 0.78f,
                    animationSpec = tween(
                        durationMillis = phase.breathingPattern.exhaleMillis,
                        easing = LinearEasing
                    )
                )
                delay(BREATHING_HAPTIC_TOP_PAUSE_MILLIS)
            }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val baseRadius = min(size.width, size.height) * 0.34f
            val radius = baseRadius * scale.value
            val expansionProgress = ((scale.value - 0.78f) / 0.22f).coerceIn(0f, 1f)
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        OrbCenterInnerColor.copy(alpha = ORB_CENTER_INNER_ALPHA),
                        OrbCenterOuterColor.copy(alpha = ORB_CENTER_OUTER_ALPHA),
                        OrbCenterEdgeColor.copy(alpha = ORB_CENTER_EDGE_ALPHA)
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            drawOverlappingRosettePattern(
                center = center,
                radius = radius * ROSETTE_PATTERN_RADIUS_FRACTION,
                cycleProgress = expansionProgress
            )
            drawCircle(
                color = AccentIndigo,
                radius = radius * 1.12f,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}

@Composable
private fun BreathingCue(
    phase: MeditationPhase,
    paused: Boolean,
    preparing: Boolean,
    preparationRemainingSeconds: Int
) {
    var breathLabel by remember(phase.kind) { mutableStateOf("Inhala") }

    LaunchedEffect(phase.kind, paused, preparing) {
        if (preparing) {
            breathLabel = "Prepárate"
            return@LaunchedEffect
        }
        while (isActive) {
            if (paused) {
                delay(180)
            } else {
                breathLabel = "Inhala"
                delay(phase.breathingPattern.inhaleMillis.toLong() + BREATHING_HAPTIC_TOP_PAUSE_MILLIS)
                breathLabel = "Exhala"
                delay(phase.breathingPattern.exhaleMillis.toLong() + BREATHING_HAPTIC_TOP_PAUSE_MILLIS)
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(210.dp)
                .height(30.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = breathLabel,
                modifier = Modifier.fillMaxWidth(),
                style = if (preparing) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = BackgroundTextColor.copy(alpha = if (preparing) 0.72f else 0.38f),
                maxLines = 1
            )
        }
        Text(
            text = if (preparing) {
                "La respiración empieza en $preparationRemainingSeconds"
            } else {
                phase.breathingPattern.displayLabel
            },
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = BackgroundTextColor.copy(alpha = 0.42f)
        )
    }
}

@Composable
private fun BreathingRhythmInfoDialog(
    onDismiss: () -> Unit,
    onOpenStudy: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DropdownContainerColor,
        titleContentColor = TextPrimary,
        textContentColor = TextPrimary,
        title = {
            Text(
                text = "Ritmo respiratorio",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Estos patrones lentos acercan la respiración a unas 4.5-6 respiraciones por minuto. " +
                    "En entrenamiento HRV se usan para favorecer oscilaciones amplias del ritmo cardíaco, " +
                    "estimular el baro reflejo y facilitar un estado de regulación.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenStudy) {
                Text("Ver estudio", color = AccentIndigo, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = TextPrimary)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmotionalStateDropdown(
    selected: InitialEmotionalState,
    onSelected: (InitialEmotionalState) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = "${selected.emoji}  ${selected.label}",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            ),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = DropdownShape,
            colors = compactDropdownTextFieldColors(),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = DropdownShape,
            containerColor = DropdownContainerColor
        ) {
            InitialEmotionalState.entries.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${item.emoji}  ${item.label}",
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    onClick = {
                        onSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DurationDropdown(
    selected: SessionDurationOption,
    onSelected: (SessionDurationOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = "${selected.minutes} minutos",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            ),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = DropdownShape,
            colors = compactDropdownTextFieldColors(),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = DropdownShape,
            containerColor = DropdownContainerColor
        ) {
            SessionDurationOption.entries.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${item.minutes} minutos",
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    onClick = {
                        onSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BreathingRhythmDropdown(
    selected: BreathingRhythmOption,
    onSelected: (BreathingRhythmOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            ),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = DropdownShape,
            colors = compactDropdownTextFieldColors(),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = DropdownShape,
            containerColor = DropdownContainerColor
        ) {
            BreathingRhythmOption.entries.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = item.label,
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    onClick = {
                        onSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostSessionEmotionDropdown(
    selected: PostSessionEmotion,
    onSelected: (PostSessionEmotion) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = "${selected.emoji}  ${selected.label}",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            ),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = DropdownShape,
            colors = compactDropdownTextFieldColors(),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = DropdownShape,
            containerColor = DropdownContainerColor
        ) {
            PostSessionEmotion.entries.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${item.emoji}  ${item.label}",
                            color = TextPrimary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    onClick = {
                        onSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun compactDropdownTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedContainerColor = Color.White.copy(alpha = 0.90f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.90f),
    focusedBorderColor = AccentIndigo,
    unfocusedBorderColor = Color.Black.copy(alpha = 0.22f),
    cursorColor = TextPrimary
)

@Composable
private fun ScoreSelector(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionTitle(title)
        Text(
            text = "$value/10",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
    Slider(
        value = value.toFloat(),
        onValueChange = { onValueChange(it.roundToInt()) },
        valueRange = 1f..10f,
        steps = 8
    )
}

@Composable
private fun SessionInfoPanel(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SessionGuideShape,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.34f),
            contentColor = BackgroundTextColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.22f), SessionGuideShape)
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun CalmPanel(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.82f),
            contentColor = TextPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.38f), RoundedCornerShape(8.dp))
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
    )
}

@Composable
private fun TimerPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Surface(
        color = Color.White.copy(alpha = 0.38f),
        shape = CircleShape,
        contentColor = TextPrimary.copy(alpha = 0.68f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = TextPrimary.copy(alpha = 0.62f), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = text,
                fontWeight = FontWeight.Medium,
                color = TextPrimary.copy(alpha = 0.68f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun softButtonColors() = ButtonDefaults.buttonColors(
    containerColor = SoftButtonContainer,
    contentColor = SoftButtonContent,
    disabledContainerColor = SoftButtonContainer.copy(alpha = 0.14f),
    disabledContentColor = SoftButtonContent.copy(alpha = 0.38f)
)

@Composable
private fun softTonalButtonColors() = ButtonDefaults.filledTonalButtonColors(
    containerColor = SoftButtonContainer.copy(alpha = 0.20f),
    contentColor = SoftButtonContent,
    disabledContainerColor = SoftButtonContainer.copy(alpha = 0.12f),
    disabledContentColor = SoftButtonContent.copy(alpha = 0.38f)
)

private fun screenBrush(): Brush {
    return Brush.linearGradient(
        colors = listOf(
            Color(0xFF08032D),
            Color(0xFF1F1F4D),
            Color(0xFF2F2839)
        )
    )
}

private fun Context.cardioCoherenceBackgroundAssets(): List<String> {
    return runCatching {
        assets.list(CARDIO_BACKGROUND_ASSET_DIR)
            ?.filter { it.isSupportedCardioBackgroundImage() }
            ?.map { "$CARDIO_BACKGROUND_ASSET_DIR/$it" }
            ?.sorted()
            .orEmpty()
    }.getOrDefault(emptyList())
}

private fun Context.initialCardioCoherenceBackgroundAsset(assets: List<String>): String? {
    if (assets.isEmpty()) return null
    val preferences = getSharedPreferences(CARDIO_BACKGROUND_PREFS, Context.MODE_PRIVATE)
    val persistedAsset = preferences
        .getString(CARDIO_BACKGROUND_LAST_ASSET_KEY, null)
    if (persistedAsset in assets) {
        persistCardioCoherenceBackgroundAsset(persistedAsset, assets)
        return persistedAsset
    }

    val defaultAsset = "$CARDIO_BACKGROUND_ASSET_DIR/$CARDIO_BACKGROUND_DEFAULT_ASSET"
        .takeIf { it in assets }
        ?: assets.random(Random.Default)
    persistCardioCoherenceBackgroundAsset(defaultAsset, assets)
    return defaultAsset
}

private fun Context.nextCardioCoherenceBackgroundAsset(
    assets: List<String>,
    currentAsset: String?
): String? {
    if (assets.isEmpty()) return null
    val seenAssets = cardioCoherenceSeenBackgroundAssets(assets)
    val currentSeenAssets = if (currentAsset in assets) {
        seenAssets + currentAsset.orEmpty()
    } else {
        seenAssets
    }
    val unseenCandidates = assets.filter { it !in currentSeenAssets }
    val candidates = unseenCandidates.ifEmpty {
        assets.filter { it != currentAsset }.ifEmpty { assets }
    }
    val nextAsset = candidates.random(Random.Default)
    val nextSeenAssets = if (unseenCandidates.isEmpty()) {
        setOfNotNull(currentAsset?.takeIf { it in assets }, nextAsset)
    } else {
        currentSeenAssets + nextAsset
    }

    persistCardioCoherenceBackgroundAsset(nextAsset, assets, nextSeenAssets)
    return nextAsset
}

private fun Context.persistCardioCoherenceBackgroundAsset(
    assetPath: String?,
    assets: List<String>,
    seenAssets: Set<String> = cardioCoherenceSeenBackgroundAssets(assets) + listOfNotNull(assetPath)
) {
    getSharedPreferences(CARDIO_BACKGROUND_PREFS, Context.MODE_PRIVATE).edit {
        if (assetPath == null) {
            remove(CARDIO_BACKGROUND_LAST_ASSET_KEY)
            remove(CARDIO_BACKGROUND_SEEN_ASSETS_KEY)
        } else {
            putString(CARDIO_BACKGROUND_LAST_ASSET_KEY, assetPath)
            putStringSet(
                CARDIO_BACKGROUND_SEEN_ASSETS_KEY,
                seenAssets.filter { it in assets }.toSet()
            )
        }
    }
}

private fun Context.cardioCoherenceSeenBackgroundAssets(assets: List<String>): Set<String> {
    return getSharedPreferences(CARDIO_BACKGROUND_PREFS, Context.MODE_PRIVATE)
        .getStringSet(CARDIO_BACKGROUND_SEEN_ASSETS_KEY, emptySet())
        .orEmpty()
        .filter { it in assets }
        .toSet()
}

private fun Context.loadCardioCoherenceBackgroundBitmap(assetPath: String) = runCatching {
    assets.open(assetPath).use { input ->
        BitmapFactory.decodeStream(input)?.asImageBitmap()
    }
}.getOrNull()

private fun MeditationPhase.currentGuidanceText(phaseProgress: Float): String {
    return attentionCues
        .filter { phaseProgress >= it.startFraction }
        .maxByOrNull { it.startFraction }
        ?.text
        ?: guidance
}

private fun String.isSupportedCardioBackgroundImage(): Boolean {
    val extension = substringAfterLast('.', missingDelimiterValue = "")
        .lowercase()
    return extension in CARDIO_BACKGROUND_SUPPORTED_EXTENSIONS
}

private fun Context.savedCardioBreathingRhythm(): BreathingRhythmOption {
    val savedName = getSharedPreferences(CARDIO_BREATHING_PREFS, Context.MODE_PRIVATE)
        .getString(CARDIO_BREATHING_RHYTHM_KEY, null)
    return savedName
        ?.let { runCatching { BreathingRhythmOption.valueOf(it) }.getOrNull() }
        ?: DEFAULT_CARDIO_BREATHING_RHYTHM
}

private fun Context.persistCardioBreathingRhythm(value: BreathingRhythmOption) {
    getSharedPreferences(CARDIO_BREATHING_PREFS, Context.MODE_PRIVATE).edit {
        putString(CARDIO_BREATHING_RHYTHM_KEY, value.name)
    }
}

private fun Context.openExternalUrl(url: String) {
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
}

private val TextPrimary = Color.Black
private val BackgroundTextColor = Color.White
private val DropdownContainerColor = Color(0xFFF7F4FF)
private val DropdownShape = RoundedCornerShape(14.dp)
private val SessionGuideShape = RoundedCornerShape(18.dp)
private val AccentIndigo = Color(0xFF5B63D6)
private val AccentViolet = Color(0xFF8D63C7)
private val SoftButtonContainer = Color.White.copy(alpha = 0.26f)
private val SoftButtonContent = Color.Black.copy(alpha = 0.68f)
private val SoftButtonBorder = Color.White.copy(alpha = 0.24f)
private val OrbCenterInnerColor = Color(0xFF210921)
private val OrbCenterOuterColor = Color(0xFF221E28)
private val OrbCenterEdgeColor = Color(0xFF140B32)

private const val CARDIO_BACKGROUND_ASSET_DIR = "CoherenciaCCImagenes"
private const val CARDIO_BACKGROUND_DEFAULT_ASSET = "cc_5.JPG"
private const val CARDIO_BACKGROUND_PREFS = "cardio_coherence_background_prefs"
private const val CARDIO_BACKGROUND_LAST_ASSET_KEY = "last_background_asset"
private const val CARDIO_BACKGROUND_SEEN_ASSETS_KEY = "seen_background_assets"
private const val CARDIO_BACKGROUND_CROSSFADE_MILLIS = 850
private const val SESSION_PHASE_CROSSFADE_MILLIS = 650
private const val CARDIO_BREATHING_PREFS = "cardio_coherence_breathing_prefs"
private const val CARDIO_BREATHING_RHYTHM_KEY = "breathing_rhythm"
private val DEFAULT_CARDIO_BREATHING_RHYTHM = BreathingRhythmOption.FIVE_HALF_FIVE_HALF
private const val BREATHING_RHYTHM_STUDY_URL = "https://pmc.ncbi.nlm.nih.gov/articles/PMC7578229/"
private val CARDIO_BACKGROUND_SUPPORTED_EXTENSIONS = setOf(
    "jpg",
    "jpeg",
    "png",
    "webp",
    "gif",
    "bmp",
    "heic",
    "heif",
    "avif"
)

// Colores ajustables del patron tipo imagen de referencia.
private val RosetteColorA = Color(0xFF49C6A9)
private val RosetteColorB = Color(0xFF49C6A9)
private val RosetteColorC = Color(0xFF49C6A9)
private val RosetteBorderColor = Color(0xFF30816F)

// Porcentaje del radio del orbe ocupado por la roseta.
private const val ROSETTE_PATTERN_RADIUS_FRACTION = 0.98f
// Cantidad de pétalos/círculos exteriores superpuestos.
private const val ROSETTE_OUTER_PETAL_COUNT = 12
// Cantidad de pétalos interiores claros.
private const val ROSETTE_INNER_PETAL_COUNT = 4
// Cantidad de pétalos solapados en el centro para evitar huecos visuales.
private const val ROSETTE_CENTER_OVERLAP_PETAL_COUNT = 8
// Escala de la roseta al inicio/bajo del ciclo respiratorio.
private const val ROSETTE_CLOSED_SCALE = 0.22f
// Escala de la roseta al abrirse por completo.
private const val ROSETTE_OPEN_SCALE = 1.5f
// Radio de cada petalo exterior respecto al radio de la roseta.
private const val ROSETTE_OUTER_PETAL_RADIUS_FRACTION = 0.33f
// Longitud minima del pétalo exterior al cerrar la roseta.
private const val ROSETTE_OUTER_PETAL_CLOSED_LENGTH_SCALE = 0.72f
// Longitud maxima del pétalo exterior al abrir la roseta.
private const val ROSETTE_OUTER_PETAL_OPEN_LENGTH_SCALE = 1.0f
// Anchura minima del pétalo exterior cuando la roseta está cerrada.
private const val ROSETTE_OUTER_PETAL_CLOSED_WIDTH_SCALE = 0.62f
// Anchura maxima del pétalo exterior cuando la roseta esta abierta.
private const val ROSETTE_OUTER_PETAL_OPEN_WIDTH_SCALE = 1.04f
// Distancia del centro de cada pétalo exterior respecto al centro.
private const val ROSETTE_OUTER_CENTER_DISTANCE_FRACTION = 0.34f
// Radio de cada pétalo interior respecto al radio de la roseta.
private const val ROSETTE_INNER_PETAL_RADIUS_FRACTION = 0.20f
// Longitud minima del pétalo interior al cerrar la roseta.
private const val ROSETTE_INNER_PETAL_CLOSED_LENGTH_SCALE = 0.76f
// Longitud maxima del pétalo interior al abrir la roseta.
private const val ROSETTE_INNER_PETAL_OPEN_LENGTH_SCALE = 1.0f
// Anchura minima del pétalo interior cuando la roseta está cerrada.
private const val ROSETTE_INNER_PETAL_CLOSED_WIDTH_SCALE = 0.58f
// Anchura maxima del pétalo interior cuando la roseta está abierta.
private const val ROSETTE_INNER_PETAL_OPEN_WIDTH_SCALE = 0.98f
// Distancia del centro de cada pétalo interior respecto al centro.
private const val ROSETTE_INNER_CENTER_DISTANCE_FRACTION = 0.21f
// Longitud de los pétalos centrales respecto al radio actual de la roseta.
private const val ROSETTE_CENTER_OVERLAP_LENGTH_FRACTION = 0.26f
// Anchura de los pétalos centrales respecto a su longitud.
private const val ROSETTE_CENTER_OVERLAP_WIDTH_SCALE = 0.44f
// Opacidad de los pétalos centrales solapados.
private const val ROSETTE_CENTER_OVERLAP_ALPHA = 0.58f
// Rotacion suave de la roseta durante el ciclo respiratorio.
private const val ROSETTE_ROTATION_DEGREES = 90f
// Curva de expansion del ancho del pétalo; mayor valor retrasa el crecimiento y acelera el final.
private const val ROSETTE_PETAL_WIDTH_ACCELERATION_EXPONENT = 4.35f
// Mezcla de suavizado para que la aceleración no se perciba brusca; 0 es curva pura, 1 es más uniforme.
private const val ROSETTE_PETAL_WIDTH_SMOOTH_BLEND = 0.36f
// Variación sutil vinculada a la rotación para que el ancho no cambie de forma mecánica.
private const val ROSETTE_PETAL_ROTATION_WIDTH_WAVE = 0.06f
// Opacidad de los pétalos exteriores.
private const val ROSETTE_OUTER_ALPHA = 0.66f
// Opacidad de los pétalos interiores claros.
private const val ROSETTE_INNER_ALPHA = 0.54f
// Opacidad del brillo central.
private const val ROSETTE_CORE_ALPHA = 0.24f
// Grosor ajustable del borde exterior de cada pétalo.
private const val ROSETTE_BORDER_WIDTH_DP = 0.9f
// Opacidad del borde exterior de cada pétalo.
private const val ROSETTE_BORDER_ALPHA = 0.72f

// Opacidad del color central del orbe; sube este valor para un nucleo más sólido.
private const val ORB_CENTER_INNER_ALPHA = 1f
// Opacidad del degradado medio del centro del orbe.
private const val ORB_CENTER_OUTER_ALPHA = 0.22f
// Opacidad del borde interno de color del centro del orbe.
private const val ORB_CENTER_EDGE_ALPHA = 0.10f

// Frecuencia base del tren de micropulsos hápticos por segundo.
private const val BREATHING_HAPTIC_PULSES_PER_SECOND = 12f
// Duracion de cada micropulso vibratorio.
private const val BREATHING_HAPTIC_PULSE_ON_MILLIS = 12L
// Amplitud minima de la vibración al inicio de una rampa.
private const val BREATHING_HAPTIC_MIN_AMPLITUDE = 18
// Amplitud maxima de la vibración al final de la inhalación.
private const val BREATHING_HAPTIC_MAX_AMPLITUDE = 84
// Minimo de micro-pulsos por tramo respiratorio.
private const val BREATHING_HAPTIC_MIN_PULSES_PER_RAMP = 32
// Maximo de micro-pulsos por tramo respiratorio.
private const val BREATHING_HAPTIC_MAX_PULSES_PER_RAMP = 96
// Pausa sin vibración en el punto alto y bajo del ciclo.
private const val BREATHING_HAPTIC_TOP_PAUSE_MILLIS = 1100L
// Cuanto se separan los micropulsos al acercarse al punto alto.
private const val BREATHING_HAPTIC_SPACING_EXPANSION = 1.35f

private fun formatSeconds(value: Int): String {
    val minutes = value / 60
    val seconds = value % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun DrawScope.drawOverlappingRosettePattern(
    center: Offset,
    radius: Float,
    cycleProgress: Float
) {
    if (radius <= 0f) return

    val breathProgress = smoothBreathProgress(cycleProgress)
    val rosetteRadius = radius * (ROSETTE_CLOSED_SCALE + ((ROSETTE_OPEN_SCALE - ROSETTE_CLOSED_SCALE) * breathProgress))
    val rotation = degreesToRadians(ROSETTE_ROTATION_DEGREES * (breathProgress - 0.5f))
    val outerPetalRadius = rosetteRadius * ROSETTE_OUTER_PETAL_RADIUS_FRACTION
    val outerDistance = rosetteRadius * ROSETTE_OUTER_CENTER_DISTANCE_FRACTION
    val innerPetalRadius = rosetteRadius * ROSETTE_INNER_PETAL_RADIUS_FRACTION
    val innerDistance = rosetteRadius * ROSETTE_INNER_CENTER_DISTANCE_FRACTION
    val borderWidth = ROSETTE_BORDER_WIDTH_DP.dp.toPx()

    repeat(ROSETTE_OUTER_PETAL_COUNT) { petal ->
        val angle = rotation + ((2f * PI.toFloat() * petal) / ROSETTE_OUTER_PETAL_COUNT)
        val petalCenter = polarOffset(center, outerDistance, angle)
        val petalColor = rosetteColor(
            position = breathProgress + (petal * 0.13f),
            alpha = ROSETTE_OUTER_ALPHA
        )
        val widthScale = rosettePetalWidthScale(
            breathProgress = breathProgress,
            petalAngle = angle,
            closedScale = ROSETTE_OUTER_PETAL_CLOSED_WIDTH_SCALE,
            openScale = ROSETTE_OUTER_PETAL_OPEN_WIDTH_SCALE
        )
        val lengthScale = rosettePetalLengthScale(
            breathProgress = breathProgress,
            closedScale = ROSETTE_OUTER_PETAL_CLOSED_LENGTH_SCALE,
            openScale = ROSETTE_OUTER_PETAL_OPEN_LENGTH_SCALE
        )
        val petalLengthRadius = outerPetalRadius * lengthScale
        val petalWidthRadius = outerPetalRadius * widthScale

        drawRotatingRosettePetal(
            center = petalCenter,
            angleRadians = angle,
            brush = Brush.radialGradient(
                colors = listOf(
                    petalColor.copy(alpha = ROSETTE_OUTER_ALPHA * 1.08f),
                    petalColor.copy(alpha = ROSETTE_OUTER_ALPHA * 0.86f),
                    RosetteColorA.copy(alpha = ROSETTE_OUTER_ALPHA * 0.68f)
                ),
                center = petalCenter,
                radius = petalLengthRadius
            ),
            lengthRadius = petalLengthRadius,
            widthRadius = petalWidthRadius
        )

        if (borderWidth > 0f) {
            drawRotatingRosettePetal(
                center = petalCenter,
                angleRadians = angle,
                color = RosetteBorderColor.copy(alpha = ROSETTE_BORDER_ALPHA),
                lengthRadius = petalLengthRadius,
                widthRadius = petalWidthRadius,
                style = Stroke(width = borderWidth)
            )
        }
    }

    repeat(ROSETTE_INNER_PETAL_COUNT) { petal ->
        val angle = rotation +
            (PI.toFloat() / ROSETTE_INNER_PETAL_COUNT) +
            ((2f * PI.toFloat() * petal) / ROSETTE_INNER_PETAL_COUNT)
        val petalCenter = polarOffset(center, innerDistance, angle)
        val petalColor = rosetteColor(
            position = breathProgress + 0.22f + (petal * 0.11f),
            alpha = ROSETTE_INNER_ALPHA
        )
        val widthScale = rosettePetalWidthScale(
            breathProgress = breathProgress,
            petalAngle = angle,
            closedScale = ROSETTE_INNER_PETAL_CLOSED_WIDTH_SCALE,
            openScale = ROSETTE_INNER_PETAL_OPEN_WIDTH_SCALE
        )
        val lengthScale = rosettePetalLengthScale(
            breathProgress = breathProgress,
            closedScale = ROSETTE_INNER_PETAL_CLOSED_LENGTH_SCALE,
            openScale = ROSETTE_INNER_PETAL_OPEN_LENGTH_SCALE
        )
        val petalLengthRadius = innerPetalRadius * lengthScale

        drawRotatingRosettePetal(
            center = petalCenter,
            angleRadians = angle,
            brush = Brush.radialGradient(
                colors = listOf(
                    RosetteColorC.copy(alpha = ROSETTE_INNER_ALPHA * 1.15f),
                    petalColor.copy(alpha = ROSETTE_INNER_ALPHA),
                    Color.Transparent
                ),
                center = petalCenter,
                radius = petalLengthRadius
            ),
            lengthRadius = petalLengthRadius,
            widthRadius = innerPetalRadius * widthScale
        )
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                RosetteColorC.copy(alpha = ROSETTE_CORE_ALPHA),
                RosetteColorB.copy(alpha = ROSETTE_CORE_ALPHA * 0.42f),
                Color.Transparent
            ),
            center = center,
            radius = rosetteRadius * 0.34f
        ),
        radius = rosetteRadius * 0.34f,
        center = center
    )

    drawRosetteCenterOverlapPetals(
        center = center,
        rosetteRadius = rosetteRadius,
        rotation = rotation,
        breathProgress = breathProgress
    )
}

private fun DrawScope.drawRosetteCenterOverlapPetals(
    center: Offset,
    rosetteRadius: Float,
    rotation: Float,
    breathProgress: Float
) {
    val lengthScale = rosettePetalLengthScale(
        breathProgress = breathProgress,
        closedScale = ROSETTE_INNER_PETAL_CLOSED_LENGTH_SCALE,
        openScale = ROSETTE_INNER_PETAL_OPEN_LENGTH_SCALE
    )
    val lengthRadius = rosetteRadius * ROSETTE_CENTER_OVERLAP_LENGTH_FRACTION * lengthScale
    val widthRadius = lengthRadius * ROSETTE_CENTER_OVERLAP_WIDTH_SCALE

    repeat(ROSETTE_CENTER_OVERLAP_PETAL_COUNT) { petal ->
        val angle = (rotation * 1.18f) + ((2f * PI.toFloat() * petal) / ROSETTE_CENTER_OVERLAP_PETAL_COUNT)
        val petalColor = rosetteColor(
            position = breathProgress + 0.36f + (petal * 0.07f),
            alpha = ROSETTE_CENTER_OVERLAP_ALPHA
        )

        drawRotatingRosettePetal(
            center = center,
            angleRadians = angle,
            brush = Brush.radialGradient(
                colors = listOf(
                    petalColor.copy(alpha = ROSETTE_CENTER_OVERLAP_ALPHA * 1.08f),
                    RosetteColorC.copy(alpha = ROSETTE_CENTER_OVERLAP_ALPHA * 0.92f),
                    RosetteColorB.copy(alpha = ROSETTE_CENTER_OVERLAP_ALPHA * 0.58f)
                ),
                center = center,
                radius = lengthRadius
            ),
            lengthRadius = lengthRadius,
            widthRadius = widthRadius
        )
    }
}

private fun DrawScope.drawRotatingRosettePetal(
    center: Offset,
    angleRadians: Float,
    brush: Brush,
    lengthRadius: Float,
    widthRadius: Float
) {
    rotate(
        degrees = radiansToDegrees(angleRadians),
        pivot = center
    ) {
        drawOval(
            brush = brush,
            topLeft = Offset(
                x = center.x - lengthRadius,
                y = center.y - widthRadius
            ),
            size = Size(
                width = lengthRadius * 2f,
                height = widthRadius * 2f
            )
        )
    }
}

private fun DrawScope.drawRotatingRosettePetal(
    center: Offset,
    angleRadians: Float,
    color: Color,
    lengthRadius: Float,
    widthRadius: Float,
    style: Stroke
) {
    rotate(
        degrees = radiansToDegrees(angleRadians),
        pivot = center
    ) {
        drawOval(
            color = color,
            topLeft = Offset(
                x = center.x - lengthRadius,
                y = center.y - widthRadius
            ),
            size = Size(
                width = lengthRadius * 2f,
                height = widthRadius * 2f
            ),
            style = style
        )
    }
}

private fun rosettePetalWidthScale(
    breathProgress: Float,
    petalAngle: Float,
    closedScale: Float,
    openScale: Float
): Float {
    val widthProgress = rosettePetalWidthProgress(breathProgress)
    val base = closedScale + ((openScale - closedScale) * widthProgress)
    val rotationalWave = sin(petalAngle + (widthProgress * PI.toFloat())) *
        ROSETTE_PETAL_ROTATION_WIDTH_WAVE *
        widthProgress
    return (base + rotationalWave).coerceAtLeast(0.1f)
}

private fun rosettePetalWidthProgress(breathProgress: Float): Float {
    val clamped = breathProgress.coerceIn(0f, 1f)
    val accelerated = clamped.pow(ROSETTE_PETAL_WIDTH_ACCELERATION_EXPONENT)
    val softened = smoothBreathProgress(clamped)
    val blend = ROSETTE_PETAL_WIDTH_SMOOTH_BLEND.coerceIn(0f, 1f)
    return (accelerated + ((softened - accelerated) * blend)).coerceIn(0f, 1f)
}

private fun rosettePetalLengthScale(
    breathProgress: Float,
    closedScale: Float,
    openScale: Float
): Float {
    val lengthProgress = smoothBreathProgress(breathProgress)
    return (closedScale + ((openScale - closedScale) * lengthProgress)).coerceAtLeast(0.1f)
}

private fun polarOffset(
    center: Offset,
    radius: Float,
    angleRadians: Float
): Offset {
    return Offset(
        x = center.x + (cos(angleRadians) * radius),
        y = center.y + (sin(angleRadians) * radius)
    )
}

private fun degreesToRadians(value: Float): Float {
    return (value * PI / 180.0).toFloat()
}

private fun radiansToDegrees(value: Float): Float {
    return (value * 180.0 / PI).toFloat()
}

private fun smoothBreathProgress(value: Float): Float {
    val clamped = value.coerceIn(0f, 1f)
    return clamped * clamped * (3f - (2f * clamped))
}

private fun rosetteColor(
    position: Float,
    alpha: Float
): Color {
    val palette = listOf(
        RosetteColorA,
        RosetteColorB,
        RosetteColorC,
        RosetteColorA
    )
    val normalized = ((position % 1f) + 1f) % 1f
    val scaled = normalized * (palette.size - 1)
    val index = scaled.toInt().coerceIn(0, palette.size - 2)
    val fraction = scaled - index
    return lerpColor(palette[index], palette[index + 1], fraction)
        .copy(alpha = alpha.coerceIn(0f, 1f))
}

private fun lerpColor(
    start: Color,
    end: Color,
    fraction: Float
): Color {
    val t = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + ((end.red - start.red) * t),
        green = start.green + ((end.green - start.green) * t),
        blue = start.blue + ((end.blue - start.blue) * t),
        alpha = start.alpha + ((end.alpha - start.alpha) * t)
    )
}

private fun Context.cardioCoherenceVibrator(): Vibrator? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }?.takeIf { it.hasVibrator() }
}

private suspend fun runBreathingVibrationPattern(
    vibrator: Vibrator?,
    phase: MeditationPhase
) {
    if (vibrator == null) return
    try {
        while (currentCoroutineContext().isActive) {
            runBreathingPulseRamp(
                vibrator = vibrator,
                durationMillis = phase.breathingPattern.inhaleMillis.toLong(),
                increasing = true
            )
            delay(BREATHING_HAPTIC_TOP_PAUSE_MILLIS)
            runBreathingPulseRamp(
                vibrator = vibrator,
                durationMillis = phase.breathingPattern.exhaleMillis.toLong(),
                increasing = false
            )
            delay(BREATHING_HAPTIC_TOP_PAUSE_MILLIS)
        }
    } finally {
        vibrator.cancel()
    }
}

private suspend fun runBreathingPulseRamp(
    vibrator: Vibrator,
    durationMillis: Long,
    increasing: Boolean
) {
    val pulseCount = (durationMillis * BREATHING_HAPTIC_PULSES_PER_SECOND / 1000f)
        .roundToInt()
        .coerceIn(
            BREATHING_HAPTIC_MIN_PULSES_PER_RAMP,
            BREATHING_HAPTIC_MAX_PULSES_PER_RAMP
        )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(createBreathingRampEffect(vibrator, durationMillis, pulseCount, increasing))
        delay(durationMillis)
        return
    }

    val intervalMillis = (durationMillis / pulseCount).coerceAtLeast(BREATHING_HAPTIC_PULSE_ON_MILLIS + 1L)
    repeat(pulseCount) {
        @Suppress("DEPRECATION")
        vibrator.vibrate(BREATHING_HAPTIC_PULSE_ON_MILLIS)
        delay(intervalMillis)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun createBreathingRampEffect(
    vibrator: Vibrator,
    durationMillis: Long,
    pulseCount: Int,
    increasing: Boolean
): VibrationEffect {
    val timings = mutableListOf<Long>()
    val amplitudes = mutableListOf<Int>()
    val pulseWeights = buildBreathingPulseWeights(pulseCount, increasing)
    val totalWeight = pulseWeights.sum().coerceAtLeast(1f)
    var consumedMillis = 0L

    repeat(pulseCount) { index ->
        val remainingMillis = (durationMillis - consumedMillis).coerceAtLeast(0L)
        if (remainingMillis <= 0L) return@repeat

        val weightedPeriodMillis = ((durationMillis * pulseWeights[index]) / totalWeight)
            .roundToInt()
            .toLong()
            .coerceAtLeast(BREATHING_HAPTIC_PULSE_ON_MILLIS + 1L)
        val pulseMillis = BREATHING_HAPTIC_PULSE_ON_MILLIS
            .coerceAtMost(remainingMillis)
            .coerceAtMost(weightedPeriodMillis - 1L)
        val gapMillis = (weightedPeriodMillis - pulseMillis)
            .coerceAtLeast(1L)
            .coerceAtMost((durationMillis - consumedMillis - pulseMillis).coerceAtLeast(1L))
        val rawProgress = if (pulseCount == 1) 1f else index / (pulseCount - 1).toFloat()
        val progress = if (increasing) rawProgress else 1f - rawProgress
        val amplitude = BREATHING_HAPTIC_MIN_AMPLITUDE +
            ((BREATHING_HAPTIC_MAX_AMPLITUDE - BREATHING_HAPTIC_MIN_AMPLITUDE) * progress).roundToInt()
        val safeAmplitude = if (vibrator.hasAmplitudeControl()) {
            amplitude.coerceIn(1, 255)
        } else {
            VibrationEffect.DEFAULT_AMPLITUDE
        }

        timings.add(pulseMillis)
        amplitudes.add(safeAmplitude)
        consumedMillis += pulseMillis

        if (consumedMillis < durationMillis) {
            timings.add(gapMillis)
            amplitudes.add(0)
            consumedMillis += gapMillis
        }
    }

    return VibrationEffect.createWaveform(
        timings.toLongArray(),
        amplitudes.toIntArray(),
        -1
    )
}

private fun buildBreathingPulseWeights(
    pulseCount: Int,
    increasing: Boolean
): List<Float> {
    return List(pulseCount) { index ->
        val rawProgress = if (pulseCount == 1) 1f else index / (pulseCount - 1).toFloat()
        val spacingProgress = if (increasing) rawProgress else 1f - rawProgress
        1f + (BREATHING_HAPTIC_SPACING_EXPANSION * spacingProgress)
    }
}

private fun completedKinds(state: CardioCoherenceUiState): Set<com.ypg.neville.feature.cardiocoherence.domain.MeditationPhaseKind> {
    val session = state.session ?: return emptySet()
    val result = mutableSetOf<com.ypg.neville.feature.cardiocoherence.domain.MeditationPhaseKind>()
    var remaining = state.elapsedSeconds
    session.phases.forEach { phase ->
        if (remaining >= phase.durationSeconds) result.add(phase.kind)
        remaining -= phase.durationSeconds
    }
    return result
}
