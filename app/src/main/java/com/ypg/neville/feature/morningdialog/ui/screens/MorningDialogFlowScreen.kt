package com.ypg.neville.feature.morningdialog.ui.screens

import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ypg.neville.R
import com.ypg.neville.feature.morningdialog.ui.components.SectionCard
import com.ypg.neville.feature.morningdialog.ui.components.SelectableChip
import com.ypg.neville.feature.morningdialog.ui.components.StepProgress
import com.ypg.neville.feature.morningdialog.ui.components.MorningDialogStyles
import com.ypg.neville.feature.morningdialog.ui.viewmodel.MorningDialogFlowUiState
import com.ypg.neville.feature.morningdialog.ui.viewmodel.TriggerResponseInput
import kotlinx.coroutines.delay
import java.util.Locale

private val identitySuggestions = listOf(
    "Enfocado", "Calmado", "Disciplinado", "Valiente", "Presente",
    "Compasivo","Curioso","Reflexivo","Proactivo","Consciente","Observador",
    "Intencional","Constante","Organizado","Persistente","Productivo","Comprometido",
    "Auténtico","Visionario","Autodidacta","Valiente","Explorador", "Innovador",
    "Expansivo")

private val emotionSuggestions = listOf(
    "Calma", "Confianza", "Gratitud", "Claridad", "Energía", "Apertura",
    "Serenidad","Empatía", "Autoestima","Alegría","Paz", "Amor", "Compasión","Esperanza",
    "Entusiasmo","Seguridad","Asombro","Satisfacción", "Fluidez", "Conexión", "Fortaleza",
    "Resiliencia"
)

@Composable
fun MorningDialogFlowScreen(
    state: MorningDialogFlowUiState,
    onNext: () -> Unit,
    onBack: () -> Boolean,
    onFinish: () -> Unit,
    onGoalChange: (Int, String) -> Unit,
    onAddGoal: () -> Unit,
    onRemoveGoal: (Int) -> Unit,
    onToggleIdentity: (String) -> Unit,
    onCustomIdentityChange: (String) -> Unit,
    onToggleEmotion: (String) -> Unit,
    onCustomEmotionChange: (String) -> Unit,
    onTriggerChange: (Int, String) -> Unit,
    onResponseChange: (Int, String) -> Unit,
    onAddTriggerResponse: () -> Unit,
    onRemoveTriggerResponse: (Int) -> Unit,
    onStepChange: (Int) -> Unit,
    onSetDayRemindersEnabled: (Boolean) -> Unit,
    onAddDayReminderTime: (Int) -> Unit,
    onRemoveDayReminderTime: (Int) -> Unit,
    onCloseAfterFinish: () -> Unit
) {
    BackHandler(enabled = state.step > 1) {
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MorningDialogStyles.backgroundBrush)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StepProgress(currentStep = state.step, totalSteps = 6)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (state.step) {
                1 -> PresenceStep(onStepChange = onStepChange)
                2 -> GoalsStep(
                    goals = state.goals,
                    onGoalChange = onGoalChange,
                    onAddGoal = onAddGoal,
                    onRemoveGoal = onRemoveGoal
                )

                3 -> IdentityStep(
                    identities = state.identities,
                    customIdentity = state.customIdentity,
                    onToggleIdentity = onToggleIdentity,
                    onCustomIdentityChange = onCustomIdentityChange
                )

                4 -> EmotionStep(
                    emotions = state.emotions,
                    customEmotion = state.customEmotion,
                    onToggleEmotion = onToggleEmotion,
                    onCustomEmotionChange = onCustomEmotionChange
                )

                5 -> AnticipationStep(
                    pairs = state.triggerResponses,
                    onTriggerChange = onTriggerChange,
                    onResponseChange = onResponseChange,
                    onAddPair = onAddTriggerResponse,
                    onRemovePair = onRemoveTriggerResponse
                )

                6 -> SummaryStep(
                    state = state,
                    onSetDayRemindersEnabled = onSetDayRemindersEnabled,
                    onAddDayReminderTime = onAddDayReminderTime,
                    onRemoveDayReminderTime = onRemoveDayReminderTime
                )
            }

            state.validationMessage?.let { message ->
                Text(
                    text = localizedFlowValidation(message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (state.isSaving) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onBack() },
                    modifier = Modifier.weight(1f),
                    enabled = state.step > 1,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MorningDialogStyles.buttonColor,
                        contentColor = MorningDialogStyles.buttonTextColor
                    )
                ) {
                    Text(stringResource(R.string.ritual_back))
                }

                Button(
                    onClick = {
                        if (state.step < 6) onNext() else onFinish()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MorningDialogStyles.buttonColor,
                        contentColor = MorningDialogStyles.buttonTextColor
                    )
                ) {
                    Text(if (state.step < 6) stringResource(R.string.ritual_continue) else stringResource(R.string.ritual_finish))
                }
            }

            if (state.isCompleted) {
                Button(
                    onClick = onCloseAfterFinish,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MorningDialogStyles.buttonColor,
                        contentColor = MorningDialogStyles.buttonTextColor
                    )
                ) {
                    Text(stringResource(R.string.ritual_back_home))
                }
            }
        }
    }
}

@Composable
private fun PresenceStep(onStepChange: (Int) -> Unit) {
    val sequence = stringArrayResource(R.array.ritual_presence_sequence).toList()
    var sequenceIndex by remember { mutableIntStateOf(0) }
    val sequenceAlpha = remember { Animatable(0f) }

    LaunchedEffect(sequence) {
        while (true) {
            sequenceAlpha.animateTo(
                targetValue = 0.95f,
                animationSpec = tween(durationMillis = STEP1_FADE_IN_MS)
            )
            sequenceAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = STEP1_FADE_OUT_MS)
            )
            sequenceIndex = (sequenceIndex + 1) % sequence.size
            delay(STEP1_BETWEEN_MESSAGES_MS)
        }
    }

    SectionCard(
        title = stringResource(R.string.ritual_step1_title),
        body = stringResource(R.string.ritual_step1_body)
    ) {
        Text(
            text = stringResource(R.string.ritual_present_intro),
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = sequence[sequenceIndex],
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.graphicsLayer(alpha = sequenceAlpha.value),
            color = MorningDialogStyles.ritualCardText
        )

        OutlinedButton(
            onClick = { onStepChange(2) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MorningDialogStyles.buttonColor,
                contentColor = MorningDialogStyles.buttonTextColor
            )
        ) {
            Text(stringResource(R.string.ritual_i_am_present))
        }
    }
}

private const val STEP1_FADE_IN_MS = 3500
private const val STEP1_FADE_OUT_MS = 4000
private const val STEP1_BETWEEN_MESSAGES_MS = 220L

@Composable
private fun GoalsStep(
    goals: List<String>,
    onGoalChange: (Int, String) -> Unit,
    onAddGoal: () -> Unit,
    onRemoveGoal: (Int) -> Unit
) {
    SectionCard(title = stringResource(R.string.ritual_create_today), body = stringResource(R.string.ritual_step2_prompt)) {
        goals.forEachIndexed { index, goal ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = goal,
                    onValueChange = { onGoalChange(index, it) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(R.string.ritual_goal_number, index + 1), color = Color.Black) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black)
                )
                if (goals.size > 1) {
                    OutlinedButton(
                        onClick = { onRemoveGoal(index) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MorningDialogStyles.buttonColor,
                            contentColor = MorningDialogStyles.buttonTextColor
                        )
                    ) { Text(stringResource(R.string.ritual_remove)) }
                }
            }
        }
        if (goals.size < 3) {
            OutlinedButton(
                onClick = onAddGoal,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MorningDialogStyles.buttonColor,
                    contentColor = MorningDialogStyles.buttonTextColor
                )
            ) { Text(stringResource(R.string.ritual_add_goal)) }
        }
    }
}

@Composable
private fun IdentityStep(
    identities: List<String>,
    customIdentity: String,
    onToggleIdentity: (String) -> Unit,
    onCustomIdentityChange: (String) -> Unit
) {
    val localizedSuggestions = stringArrayResource(R.array.ritual_identity_suggestions)
    SectionCard(title = stringResource(R.string.ritual_intentional_identity), body = stringResource(R.string.ritual_step3_prompt)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            identitySuggestions.forEachIndexed { index, suggestion ->
                SelectableChip(
                    text = localizedSuggestions.getOrElse(index) { suggestion },
                    selected = identities.contains(suggestion),
                    onClick = { onToggleIdentity(suggestion) }
                )
            }
        }
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = customIdentity,
            onValueChange = onCustomIdentityChange,
            singleLine = true,
            label = { Text(stringResource(R.string.ritual_custom_identity)) },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black)
        )
    }
}

@Composable
private fun EmotionStep(
    emotions: List<String>,
    customEmotion: String,
    onToggleEmotion: (String) -> Unit,
    onCustomEmotionChange: (String) -> Unit
) {
    val localizedSuggestions = stringArrayResource(R.array.ritual_emotion_suggestions)
    SectionCard(title = stringResource(R.string.ritual_chosen_emotion), body = stringResource(R.string.ritual_step4_prompt)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            emotionSuggestions.forEachIndexed { index, suggestion ->
                SelectableChip(
                    text = localizedSuggestions.getOrElse(index) { suggestion },
                    selected = emotions.contains(suggestion),
                    onClick = { onToggleEmotion(suggestion) }
                )
            }
        }
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = customEmotion,
            onValueChange = onCustomEmotionChange,
            singleLine = true,
            label = { Text(stringResource(R.string.ritual_custom_emotion)) },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black)
        )
    }
}

@Composable
private fun AnticipationStep(
    pairs: List<TriggerResponseInput>,
    onTriggerChange: (Int, String) -> Unit,
    onResponseChange: (Int, String) -> Unit,
    onAddPair: () -> Unit,
    onRemovePair: (Int) -> Unit
) {
    SectionCard(
        title = stringResource(R.string.ritual_conscious_anticipation),
        body = stringResource(R.string.ritual_step5_prompt)
    ) {
        pairs.forEachIndexed { index, pair ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pair.trigger,
                    onValueChange = { onTriggerChange(index, it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.ritual_trigger_hint)) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black)
                )
                OutlinedTextField(
                    value = pair.response,
                    onValueChange = { onResponseChange(index, it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.ritual_response_hint)) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black)
                )
                if (pairs.size > 1) {
                    OutlinedButton(
                        onClick = { onRemovePair(index) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MorningDialogStyles.buttonColor,
                            contentColor = MorningDialogStyles.buttonTextColor
                        )
                    ) {
                        Text(stringResource(R.string.ritual_remove_situation))
                    }
                }
            }
        }
        if (pairs.size < 3) {
            OutlinedButton(
                onClick = onAddPair,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MorningDialogStyles.buttonColor,
                    contentColor = MorningDialogStyles.buttonTextColor
                )
            ) {
                Text(stringResource(R.string.ritual_add_situation))
            }
        }
    }
}

@Composable
private fun SummaryStep(
    state: MorningDialogFlowUiState,
    onSetDayRemindersEnabled: (Boolean) -> Unit,
    onAddDayReminderTime: (Int) -> Unit,
    onRemoveDayReminderTime: (Int) -> Unit
) {
    val context = LocalContext.current
    val localizedIdentities = stringArrayResource(R.array.ritual_identity_suggestions)
    val localizedEmotions = stringArrayResource(R.array.ritual_emotion_suggestions)

    val goals = state.goals.map { it.trim() }.filter { it.isNotEmpty() }
    val identities = buildList {
        addAll(state.identities)
        state.customIdentity.trim().takeIf { it.isNotEmpty() }?.let { add(it) }
    }.distinct()
    val emotions = buildList {
        addAll(state.emotions)
        state.customEmotion.trim().takeIf { it.isNotEmpty() }?.let { add(it) }
    }.distinct()

    SectionCard(title = stringResource(R.string.ritual_closing_visualization)) {
        Text(stringResource(R.string.ritual_goals_value, if (goals.isEmpty()) "—" else goals.joinToString()))
        Text(
            stringResource(
                R.string.ritual_identity_value,
                if (identities.isEmpty()) "—" else identities.joinToString { value ->
                    identitySuggestions.indexOf(value).takeIf { it >= 0 }?.let { localizedIdentities.getOrElse(it) { value } } ?: value
                }
            )
        )
        Text(
            stringResource(
                R.string.ritual_emotions_value,
                if (emotions.isEmpty()) "—" else emotions.joinToString { value ->
                    emotionSuggestions.indexOf(value).takeIf { it >= 0 }?.let { localizedEmotions.getOrElse(it) { value } } ?: value
                }
            )
        )

        val pairs = state.triggerResponses
            .map { it.copy(trigger = it.trigger.trim(), response = it.response.trim()) }
            .filter { it.trigger.isNotEmpty() && it.response.isNotEmpty() }
        if (pairs.isEmpty()) {
            Text(stringResource(R.string.ritual_responses_empty))
        } else {
            pairs.forEach {
                Text(stringResource(R.string.ritual_response_value, it.trigger, it.response))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.ritual_day_reminders))
            Switch(
                checked = state.dayRemindersEnabled,
                onCheckedChange = onSetDayRemindersEnabled
            )
        }

        if (state.dayRemindersEnabled) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.dayReminderTimes.sorted().forEach { minuteOfDay ->
                    SelectableChip(
                        text = formatMinuteOfDay(minuteOfDay),
                        selected = true,
                        onClick = { onRemoveDayReminderTime(minuteOfDay) }
                    )
                }
            }

            val canAddMore = state.dayReminderTimes.size < 6
            if (canAddMore) {
                OutlinedButton(
                    onClick = {
                        val nowHour = 11
                        val nowMinute = 0
                        TimePickerDialog(
                            context,
                            { _, selectedHour, selectedMinute ->
                                onAddDayReminderTime((selectedHour * 60) + selectedMinute)
                            },
                            nowHour,
                            nowMinute,
                            false
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MorningDialogStyles.buttonColor,
                        contentColor = MorningDialogStyles.buttonTextColor
                    )
                ) {
                    Text(stringResource(R.string.ritual_add_time))
                }
            }

            if (state.dayReminderTimes.isEmpty()) {
                Text(
                    text = stringResource(R.string.ritual_no_times),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = stringResource(R.string.ritual_tap_remove_time),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = stringResource(R.string.ritual_final_lines),
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFFF9800)
        )
    }
}

private fun formatMinuteOfDay(value: Int): String {
    val hour24 = (value / 60).coerceIn(0, 23)
    val minute = (value % 60).coerceIn(0, 59)
    val calendar = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, hour24)
        set(java.util.Calendar.MINUTE, minute)
    }
    return java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, Locale.getDefault()).format(calendar.time)
}

@Composable
private fun localizedFlowValidation(message: String): String = stringResource(
    when (message) {
        "Añade al menos 1 meta para hoy." -> R.string.ritual_validation_goal_required
        "Solo puedes guardar hasta 3 metas." -> R.string.ritual_validation_goal_limit
        "Elige al menos una identidad o añade una personalizada." -> R.string.ritual_validation_identity
        "Elige al menos una emoción o intención para cultivar hoy." -> R.string.ritual_validation_emotion
        "Completa al menos una situación y tu respuesta consciente." -> R.string.ritual_validation_situation
        "Añade al menos una hora o desactiva los recordatorios del día." -> R.string.ritual_validation_time
        else -> R.string.common_unknown_error
    }
)
