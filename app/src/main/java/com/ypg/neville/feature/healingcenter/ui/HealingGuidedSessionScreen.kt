package com.ypg.neville.feature.healingcenter.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Emergency
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ypg.neville.R
import com.ypg.neville.feature.healingcenter.domain.HealingProtocol
import com.ypg.neville.feature.healingcenter.domain.HealingProtocolStep
import com.ypg.neville.feature.healingcenter.domain.HealingSituation
import com.ypg.neville.feature.healingcenter.domain.HealingStepPhase
import kotlinx.coroutines.delay

private enum class SessionFeedback(val icon: ImageVector) {
    HELPED(Icons.Rounded.ThumbUp),
    SOMEWHAT(Icons.Rounded.RemoveCircle),
    NOT_HELPED(Icons.Rounded.ThumbDown)
}

@Composable
internal fun HealingGuidedSessionScreen(
    situation: HealingSituation,
    protocol: HealingProtocol,
    contentPadding: PaddingValues,
    emergencyVisible: Boolean,
    onExit: () -> Unit,
    onEmergency: () -> Unit
) {
    var currentIndex by remember(protocol.id) { mutableIntStateOf(0) }
    var remainingSeconds by remember(protocol.id) { mutableIntStateOf(protocol.steps.firstOrNull()?.durationSeconds ?: 0) }
    var isPaused by remember(protocol.id) { mutableStateOf(false) }
    var finished by remember(protocol.id) { mutableStateOf(false) }
    var feedback by remember(protocol.id) { mutableStateOf<SessionFeedback?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    fun advanceStep() {
        if (currentIndex + 1 < protocol.steps.size) {
            currentIndex += 1
            remainingSeconds = protocol.steps[currentIndex].durationSeconds
        } else {
            finished = true
            isPaused = true
        }
    }

    fun previousStep() {
        if (currentIndex > 0) {
            currentIndex -= 1
            remainingSeconds = protocol.steps[currentIndex].durationSeconds
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) isPaused = true
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(emergencyVisible) {
        if (emergencyVisible) isPaused = true
    }

    LaunchedEffect(currentIndex, isPaused, finished) {
        if (isPaused || finished || protocol.steps.getOrNull(currentIndex) == null) return@LaunchedEffect
        while (remainingSeconds > 0 && !isPaused && !finished) {
            delay(1_000)
            if (isPaused || finished) return@LaunchedEffect
            if (remainingSeconds > 1) remainingSeconds -= 1 else {
                advanceStep()
                return@LaunchedEffect
            }
        }
    }

    if (finished) {
        HealingCompletionView(
            contentPadding = contentPadding,
            feedback = feedback,
            onFeedback = { feedback = it },
            onExit = onExit,
            onEmergency = onEmergency
        )
        return
    }

    val step = protocol.steps.getOrNull(currentIndex) ?: return
    val elapsed = (step.durationSeconds - remainingSeconds).coerceAtLeast(0)
    val stepProgress = elapsed.toFloat() / step.durationSeconds.coerceAtLeast(1).toFloat()
    val progress = (currentIndex.toFloat() + stepProgress) / protocol.steps.size.coerceAtLeast(1).toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = contentPadding.calculateTopPadding() + 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 18.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.healing_step_progress, currentIndex + 1, protocol.steps.size),
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                "%d:%02d".format(remainingSeconds / 60, remainingSeconds % 60),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
            color = Color(0xFF69F0AE),
            trackColor = Color.White.copy(alpha = 0.16f),
            strokeCap = ProgressIndicatorDefaults.LinearStrokeCap
        )

        Spacer(Modifier.weight(1f))
        HealingGuidanceOrb(step, isPaused)

        Text(
            step.title,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            step.instruction,
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 19.sp,
            lineHeight = 25.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp)
        )

        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = ::previousStep,
                enabled = currentIndex > 0,
                modifier = Modifier.size(52.dp),
                contentPadding = PaddingValues(0.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = stringResource(R.string.healing_previous_step))
            }
            Button(
                onClick = { isPaused = !isPaused },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF45B984), contentColor = Color(0xFF07120D)),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, contentDescription = null)
                Spacer(Modifier.size(7.dp))
                Text(if (isPaused) stringResource(R.string.healing_continue) else stringResource(R.string.healing_pause), fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = ::advanceStep,
                modifier = Modifier.size(52.dp),
                contentPadding = PaddingValues(0.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Rounded.SkipNext, contentDescription = stringResource(R.string.healing_next_step))
            }
        }

        Text(
            stringResource(R.string.healing_stop_instruction),
            color = Color.White.copy(alpha = 0.52f),
            fontSize = 11.sp,
            lineHeight = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 13.dp)
        )
    }
}

@Composable
private fun HealingGuidanceOrb(step: HealingProtocolStep, isPaused: Boolean) {
    val targetScale = when {
        isPaused -> 1f
        step.phase == HealingStepPhase.INHALE -> 1.18f
        step.phase == HealingStepPhase.EXHALE -> 0.82f
        step.phase == HealingStepPhase.PRESS -> 0.90f
        step.phase == HealingStepPhase.SOUND -> 1.10f
        else -> 1f
    }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween((step.durationSeconds.coerceAtMost(8) * 1_000)),
        label = "healing_orb_scale"
    )

    Box(modifier = Modifier.height(282.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(250.dp)
                .scale(scale * 1.08f)
                .background(Color(0xFF00BCD4).copy(alpha = 0.09f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(188.dp)
                .scale(scale)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color.White.copy(alpha = 0.72f),
                            Color(0xFF69F0AE).copy(alpha = 0.55f),
                            Color(0xFF00BCD4).copy(alpha = 0.20f),
                            Color(0xFF3F51B5).copy(alpha = 0.10f)
                        )
                    ),
                    CircleShape
                )
                .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isPaused) Icons.Rounded.Pause else healingPhaseIcon(step.phase),
                contentDescription = step.accessibilityCue ?: step.title,
                tint = Color.White,
                modifier = Modifier.size(44.dp)
            )
        }
    }
}

@Composable
private fun HealingCompletionView(
    contentPadding: PaddingValues,
    feedback: SessionFeedback?,
    onFeedback: (SessionFeedback) -> Unit,
    onExit: () -> Unit,
    onEmergency: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 22.dp,
                end = 22.dp,
                top = contentPadding.calculateTopPadding() + 35.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF69F0AE), modifier = Modifier.size(74.dp))
        Text(stringResource(R.string.healing_guide_completed), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(
            stringResource(R.string.healing_completion_note),
            color = Color.White.copy(alpha = 0.76f),
            fontSize = 19.sp,
            lineHeight = 25.sp,
            textAlign = TextAlign.Center
        )

        HealingGlassCard {
            Text(stringResource(R.string.healing_feedback_question), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.healing_feedback_privacy),
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            Row(Modifier.padding(top = 11.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SessionFeedback.entries.forEach { value ->
                    val feedbackTitle = when (value) {
                        SessionFeedback.HELPED -> stringResource(R.string.healing_feedback_helped)
                        SessionFeedback.SOMEWHAT -> stringResource(R.string.healing_feedback_somewhat)
                        SessionFeedback.NOT_HELPED -> stringResource(R.string.healing_feedback_not_helped)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (feedback == value) Color(0xFF4E50A8).copy(alpha = 0.72f) else Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onFeedback(value) }
                            .padding(vertical = 10.dp)
                            ,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(Modifier.fillMaxWidth().background(Color.Transparent).padding(0.dp)) {
                            Column(
                                Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(value.icon, contentDescription = null, tint = Color.White.copy(alpha = if (feedback == value) 1f else 0.72f))
                                Text(feedbackTitle, color = Color.White.copy(alpha = if (feedback == value) 1f else 0.72f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        when (feedback) {
            SessionFeedback.NOT_HELPED -> Text(
                stringResource(R.string.healing_feedback_not_helped_advice),
                color = Color(0xFFFFB45F), textAlign = TextAlign.Center, fontSize = 14.sp
            )
            SessionFeedback.HELPED, SessionFeedback.SOMEWHAT -> Text(
                stringResource(R.string.healing_feedback_helped_advice),
                color = Color(0xFF69F0AE), textAlign = TextAlign.Center, fontSize = 14.sp
            )
            null -> Unit
        }

        HealingPrimaryButton(
            text = stringResource(R.string.healing_exit_guide),
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            color = Color(0xFF4E50A8),
            onClick = onExit
        )
        Button(
            onClick = onEmergency,
            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF6262))
        ) {
            Icon(Icons.Rounded.Emergency, contentDescription = null)
            Spacer(Modifier.size(7.dp))
            Text(stringResource(R.string.healing_need_urgent_help))
        }
    }
}
