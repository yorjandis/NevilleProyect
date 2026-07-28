package com.ypg.neville.feature.morningdialog.ui.screens

import android.app.DatePickerDialog
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.ypg.neville.R
import com.ypg.neville.feature.morningdialog.domain.EveningDaySnapshot
import com.ypg.neville.feature.morningdialog.domain.EveningReview
import com.ypg.neville.feature.morningdialog.domain.EveningReviewDraft
import com.ypg.neville.feature.morningdialog.domain.EveningRitualRepository
import com.ypg.neville.feature.morningdialog.domain.MorningDialogSession
import com.ypg.neville.feature.morningdialog.domain.hasChangedContext
import com.ypg.neville.feature.morningdialog.ui.viewmodel.RitualDayUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val eveningBrush = Brush.linearGradient(
    listOf(Color(0xFF121A3B), Color(0xFF382364), Color(0xFF753D7D))
)
private val dashboardBrush = Brush.linearGradient(
    listOf(Color(0xFF102A4E), Color(0xFF2B245E), Color(0xFF61305F))
)
private val statsBrush = Brush.linearGradient(
    listOf(Color(0xFF102A4E), Color(0xFF1C527D), Color(0xFFF2763B))
)
private val glass = Color.White.copy(alpha = 0.13f)
private val glassStroke = Color.White.copy(alpha = 0.18f)
private val secondaryWhite = Color.White.copy(alpha = 0.72f)
private val storedCycleIdentities = listOf(
    "Enfocado", "Calmado", "Disciplinado", "Valiente", "Presente", "Compasivo", "Curioso",
    "Reflexivo", "Proactivo", "Consciente", "Observador", "Intencional", "Constante", "Organizado",
    "Persistente", "Productivo", "Comprometido", "Auténtico", "Visionario", "Autodidacta", "Valiente",
    "Explorador", "Innovador", "Expansivo"
)
private val storedCycleEmotions = listOf(
    "Calma", "Confianza", "Gratitud", "Claridad", "Energía", "Apertura", "Serenidad", "Empatía",
    "Autoestima", "Alegría", "Paz", "Amor", "Compasión", "Esperanza", "Entusiasmo", "Seguridad",
    "Asombro", "Satisfacción", "Fluidez", "Conexión", "Fortaleza", "Resiliencia"
)
@Composable
fun RitualPrivacyUnlockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize().background(dashboardBrush), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(Icons.Rounded.Lock, null, tint = Color(0xFF51449B), modifier = Modifier.size(48.dp))
                Text(stringResource(R.string.ritual_reflections_protected), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.ritual_unlock_body),
                    textAlign = TextAlign.Center,
                    color = Color.Black.copy(alpha = 0.68f)
                )
                Button(onClick = {
                    val activity = context as? FragmentActivity
                    if (activity == null) {
                        error = context.getString(R.string.ritual_unlock_error)
                        return@Button
                    }
                    val prompt = BiometricPrompt(
                        activity,
                        ContextCompat.getMainExecutor(context),
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                onUnlocked()
                            }

                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                                ) error = errString.toString()
                            }
                        }
                    )
                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle(context.getString(R.string.ritual_unlock_body))
                        .setAllowedAuthenticators(
                            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                BiometricManager.Authenticators.DEVICE_CREDENTIAL
                        )
                        .build()
                    prompt.authenticate(promptInfo)
                }) { Text(stringResource(R.string.ritual_unlock)) }
                error?.let { Text(it, color = Color(0xFF9D2633), textAlign = TextAlign.Center) }
            }
        }
    }
}

@Composable
fun EveningReviewScreen(
    state: RitualDayUiState,
    weeklyClosures: Int,
    onDateChange: (Long) -> Unit,
    onSave: (EveningReviewDraft) -> Unit,
    onOpenDiary: () -> Unit,
    forceEditing: Boolean = false
) {
    var step by remember(state.epochDay) { mutableIntStateOf(0) }
    var editing by remember(state.epochDay) { mutableStateOf(forceEditing) }
    var draft by remember(state.epochDay) { mutableStateOf(state.review.toDraft()) }
    var saveRequested by remember(state.epochDay) { mutableStateOf(false) }

    LaunchedEffect(state.review?.completedAtEpochMillis) {
        if (state.review != null) draft = state.review.toDraft()
    }
    LaunchedEffect(forceEditing) {
        if (forceEditing) editing = true
    }
    LaunchedEffect(state.review?.completedAtEpochMillis, state.saving, state.errorMessage) {
        if (saveRequested && !state.saving && state.review != null && state.errorMessage == null) {
            editing = false
            saveRequested = false
        }
    }

    Box(Modifier.fillMaxSize().background(eveningBrush)) {
        when {
            state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
            state.review != null && !editing -> CompletedReview(
                review = state.review,
                morning = state.morningSession,
                snapshot = state.snapshot,
                weeklyClosures = weeklyClosures,
                onEdit = {
                    draft = state.review.toDraft()
                    step = 0
                    editing = true
                },
                onOpenDiary = onOpenDiary
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    EveningHeader(step, state.epochDay, onDateChange)
                }
                item {
                    when (step) {
                        0 -> PulseStep(draft) { draft = it }
                        1 -> ReflectionStep(draft) { draft = it }
                        2 -> IntegrationStep(draft, state.morningSession) { draft = it }
                        else -> DaySynthesisStep(state.snapshot, draft) { draft = it }
                    }
                }
                state.errorMessage?.let { message ->
                    item { Text(localizedCycleError(message), color = Color(0xFFFFD5D5)) }
                }
                item {
                    StepButtons(
                        step = step,
                        saving = state.saving,
                        createDiary = draft.createJournalEntry,
                        editing = state.review != null,
                        onBack = { step = (step - 1).coerceAtLeast(0) },
                        onNext = {
                            if (step < 3) step++ else {
                                saveRequested = true
                                onSave(draft)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EveningHeader(step: Int, epochDay: Long, onDateChange: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.NightsStay, null, tint = Color.White)
            Text(stringResource(R.string.ritual_evening_header), color = Color.White, fontWeight = FontWeight.SemiBold)
        }
        Text(
            stringArrayResource(R.array.ritual_evening_step_titles)[step],
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringArrayResource(R.array.ritual_evening_step_bodies)[step],
            color = secondaryWhite
        )
        RitualDateButton(epochDay, onDateChange)
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            repeat(4) { index ->
                Box(
                    Modifier.weight(1f).height(5.dp).background(
                        if (index <= step) Color.White else Color.White.copy(alpha = 0.22f),
                        RoundedCornerShape(99.dp)
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PulseStep(draft: EveningReviewDraft, update: (EveningReviewDraft) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GlassCard(stringResource(R.string.ritual_energy_question), Icons.Rounded.Timeline) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                (1..5).forEach { value ->
                    SelectBox(
                        text = value.toString(),
                        selected = draft.energy == value,
                        modifier = Modifier.weight(1f)
                    ) { update(draft.copy(energy = value)) }
                }
            }
            Text(stringArrayResource(R.array.ritual_energy_labels)[(draft.energy - 1).coerceIn(0, 4)], color = secondaryWhite)
        }
        GlassCard(stringResource(R.string.ritual_emotion_question), Icons.Rounded.Favorite) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("sereno", "alegre", "ansioso", "triste", "enfadado", "cansado", "agradecido").forEach { id ->
                    SelectBox(
                        text = localizedEveningEmotionTitle(id),
                        selected = draft.predominantEmotionId == id
                    ) { update(draft.copy(predominantEmotionId = id)) }
                }
            }
        }
    }
}

@Composable
private fun ReflectionStep(draft: EveningReviewDraft, update: (EveningReviewDraft) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AnswerCard(stringResource(R.string.ritual_went_well), stringResource(R.string.ritual_went_well_hint), draft.whatWentWell) {
            update(draft.copy(whatWentWell = it))
        }
        AnswerCard(stringResource(R.string.ritual_learning), stringResource(R.string.ritual_learning_hint), draft.learning) {
            update(draft.copy(learning = it))
        }
        AnswerCard(stringResource(R.string.ritual_autopilot), stringResource(R.string.ritual_autopilot_hint), draft.autopilotMoment) {
            update(draft.copy(autopilotMoment = it))
        }
    }
}

@Composable
private fun IntegrationStep(
    draft: EveningReviewDraft,
    morning: MorningDialogSession?,
    update: (EveningReviewDraft) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AnswerCard(stringResource(R.string.ritual_gratitude), stringResource(R.string.ritual_gratitude_hint), draft.gratitude) {
            update(draft.copy(gratitude = it))
        }
        AnswerCard(stringResource(R.string.ritual_prepare_tomorrow), stringResource(R.string.ritual_tomorrow_hint), draft.tomorrowPreparation) {
            update(draft.copy(tomorrowPreparation = it))
        }
        GlassCard(stringResource(R.string.ritual_identity_alignment), Icons.Rounded.WbSunny) {
            morning?.identity?.takeIf { it.isNotBlank() }?.let {
                Text(stringResource(R.string.ritual_chosen_identity_value, it), color = secondaryWhite)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                (1..5).forEach { value ->
                    SelectBox(value.toString(), draft.identityAlignment == value, Modifier.weight(1f)) {
                        update(draft.copy(identityAlignment = value))
                    }
                }
            }
            Text(
                stringArrayResource(R.array.ritual_alignment_labels)[(draft.identityAlignment - 1).coerceIn(0, 4)],
                color = secondaryWhite
            )
        }
    }
}

@Composable
private fun DaySynthesisStep(
    snapshot: EveningDaySnapshot,
    draft: EveningReviewDraft,
    update: (EveningReviewDraft) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassCard(stringResource(R.string.ritual_day_data), Icons.Rounded.BarChart) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SnapshotMetric("${snapshot.agendaCompleted.size}/${snapshot.agendaTotalCount}", stringResource(R.string.ritual_agenda), Modifier.weight(1f))
                SnapshotMetric(snapshot.completedGoalUnits.size.toString(), stringResource(R.string.ritual_goals), Modifier.weight(1f))
                SnapshotMetric(snapshot.presenceReturns.toString(), stringResource(R.string.ritual_presence), Modifier.weight(1f))
            }
            Text(
                snapshot.coherenceAverageAfterScore?.let {
                    stringResource(R.string.ritual_coherence_day_value, snapshot.coherenceSessionsCount, it)
                } ?: stringResource(R.string.ritual_no_coherence_today),
                color = secondaryWhite
            )
            if (snapshot.automaticPilotEvents > 0) {
                Text(stringResource(R.string.ritual_autopilot_count, snapshot.automaticPilotEvents), color = Color(0xFFFFE082))
            }
        }
        GlassCard(stringResource(R.string.ritual_completed_today_section), Icons.Rounded.CheckCircle) {
            if (snapshot.agendaCompleted.isEmpty() && snapshot.completedGoalUnits.isEmpty()) {
                Text(stringResource(R.string.ritual_nothing_marked), color = secondaryWhite)
            }
            snapshot.agendaCompleted.take(3).forEach { Text("• ${it.title}", color = Color.White) }
            snapshot.completedGoalUnits.take(3).forEach { Text("• ${it.goalTitle} · ${it.unitName}", color = Color.White) }
        }
        GlassCard(stringResource(R.string.ritual_save_diary), Icons.Rounded.MenuBook) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.ritual_save_diary_body), Modifier.weight(1f), color = secondaryWhite)
                Switch(
                    checked = draft.createJournalEntry,
                    onCheckedChange = { update(draft.copy(createJournalEntry = it)) }
                )
            }
            Text(stringResource(R.string.ritual_diary_includes), color = secondaryWhite)
        }
    }
}

@Composable
private fun CompletedReview(
    review: EveningReview,
    morning: MorningDialogSession?,
    snapshot: EveningDaySnapshot,
    weeklyClosures: Int,
    onEdit: () -> Unit,
    onOpenDiary: () -> Unit
) {
    val contextChanged = review.hasChangedContext(snapshot)
    val localizedIdentities = stringArrayResource(R.array.ritual_identity_suggestions)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFFFFE066), modifier = Modifier.size(58.dp))
            Text(stringResource(R.string.ritual_day_closed), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(
                if (review.journalEntryCreated) stringResource(R.string.ritual_saved_with_diary)
                else stringResource(R.string.ritual_saved_without_diary),
                color = secondaryWhite,
                textAlign = TextAlign.Center
            )
            Text(stringResource(R.string.ritual_cycle_integrated, weeklyClosures), color = Color(0xFFFFE082), textAlign = TextAlign.Center)
        }
        item {
            GlassCard(stringResource(R.string.ritual_intention_experience), Icons.Rounded.AutoAwesome) {
                Text(
                    morning?.identity?.takeIf { it.isNotBlank() }?.let {
                        stringResource(R.string.ritual_chosen_identity_value, localizeCycleValues(it, storedCycleIdentities, localizedIdentities))
                    } ?: stringResource(R.string.ritual_no_identity_today),
                    color = Color.White
                )
                Text(stringResource(R.string.ritual_perceived_alignment, review.identityAlignment), color = secondaryWhite)
            }
        }
        item {
            GlassCard(stringResource(R.string.ritual_improvement_tomorrow), Icons.Rounded.ArrowForward) {
                Text(review.suggestion, color = Color.White)
            }
        }
        item {
            GlassCard(stringResource(R.string.ritual_footprint), Icons.Rounded.BarChart) {
                Text(stringResource(R.string.ritual_footprint_value, review.agendaCompletedCount, review.agendaTotalCount, review.goalUnitsCompletedCount, review.presenceReturns), color = Color.White)
                Text(if (review.journalEntryCreated) stringResource(R.string.ritual_diary_entry_created) else stringResource(R.string.ritual_no_diary_entry), color = secondaryWhite)
            }
        }
        if (review.journalEntryCreated) {
            item {
                Button(onClick = onOpenDiary, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF3E3A78))) {
                    Icon(Icons.Rounded.MenuBook, null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.ritual_open_diary))
                }
            }
        }
        if (contextChanged) {
            item {
                OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Color(0xFFFFE082))) {
                    Text(stringResource(R.string.ritual_day_changed_summary), color = Color(0xFFFFE082))
                }
            }
        }
        item {
            OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Edit, null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.ritual_edit_evening))
            }
        }
    }
}

@Composable
private fun StepButtons(
    step: Int,
    saving: Boolean,
    createDiary: Boolean,
    editing: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (step > 0) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(0.36f)) {
                Icon(Icons.Rounded.ArrowBack, null)
                Text(stringResource(R.string.ritual_back))
            }
        }
        Button(
            onClick = onNext,
            enabled = !saving,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF443A86))
        ) {
            Text(
                when {
                    saving -> stringResource(R.string.ritual_saving)
                    step < 2 -> stringResource(R.string.ritual_continue)
                    step == 2 -> stringResource(R.string.ritual_view_day_summary)
                    editing -> stringResource(R.string.ritual_save_changes)
                    createDiary -> stringResource(R.string.ritual_save_and_create)
                    else -> stringResource(R.string.ritual_save_evening)
                },
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
                maxLines = 1
            )
            Icon(Icons.Rounded.ArrowForward, null)
        }
    }
}

@Composable
fun MyDayScreen(
    state: RitualDayUiState,
    onDateChange: (Long) -> Unit,
    onOpenMorning: () -> Unit,
    onOpenAgenda: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenPresence: () -> Unit,
    onOpenCoherence: () -> Unit,
    onOpenReview: () -> Unit
) {
    val localizedIdentities = stringArrayResource(R.array.ritual_identity_suggestions)
    Box(Modifier.fillMaxSize().background(dashboardBrush)) {
        if (state.loading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
            return@Box
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(stringResource(R.string.ritual_my_day), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.ritual_my_day_body), color = secondaryWhite)
                Spacer(Modifier.height(10.dp))
                RitualDateButton(state.epochDay, onDateChange)
            }
            item {
                DashboardCard(stringResource(R.string.ritual_intention), Icons.Rounded.WbSunny, onOpenMorning) {
                    Text(
                        state.morningSession?.identity?.takeIf { it.isNotBlank() }?.let {
                            localizeCycleValues(it, storedCycleIdentities, localizedIdentities)
                        } ?: stringResource(R.string.ritual_no_morning),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    val goals = state.morningSession?.goals.orEmpty()
                    Text(if (goals.isEmpty()) stringResource(R.string.ritual_register_intention) else goals.joinToString(" · "), color = secondaryWhite)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DashboardMetric(stringResource(R.string.ritual_agenda), "${state.snapshot.agendaCompleted.size}/${state.snapshot.agendaTotalCount}", Icons.Rounded.CheckCircle, Modifier.weight(1f), onOpenAgenda)
                    DashboardMetric(stringResource(R.string.ritual_goals), state.snapshot.completedGoalUnits.size.toString(), Icons.Rounded.Flag, Modifier.weight(1f), onOpenGoals)
                    DashboardMetric(stringResource(R.string.ritual_presence), state.snapshot.presenceReturns.toString(), Icons.Rounded.AutoAwesome, Modifier.weight(1f), onOpenPresence)
                }
            }
            item {
                DashboardCard(stringResource(R.string.ritual_agenda), Icons.Rounded.CalendarMonth, onOpenAgenda) {
                    if (state.snapshot.agendaCompleted.isEmpty()) Text(stringResource(R.string.ritual_no_agenda_completed), color = secondaryWhite)
                    state.snapshot.agendaCompleted.take(4).forEach { Text("• ${it.title}", color = Color.White) }
                }
            }
            item {
                DashboardCard(stringResource(R.string.ritual_goals), Icons.Rounded.Flag, onOpenGoals) {
                    if (state.snapshot.completedGoalUnits.isEmpty()) Text(stringResource(R.string.ritual_no_goals_completed), color = secondaryWhite)
                    state.snapshot.completedGoalUnits.take(4).forEach { Text("• ${it.unitName} · ${it.goalTitle}", color = Color.White) }
                }
            }
            item {
                DashboardCard(stringResource(R.string.ritual_presence), Icons.Rounded.AutoAwesome, onOpenPresence) {
                    Text(stringResource(R.string.ritual_conscious_returns, state.snapshot.presenceReturns), color = Color.White)
                    Text(stringResource(R.string.ritual_automatic_pilot, state.snapshot.automaticPilotEvents), color = secondaryWhite)
                }
            }
            item {
                DashboardCard(stringResource(R.string.ritual_coherence), Icons.Rounded.Favorite, onOpenCoherence) {
                    Text(state.snapshot.coherenceAverageAfterScore?.let { stringResource(R.string.ritual_average_score, it) } ?: stringResource(R.string.ritual_no_coherence), color = Color.White)
                    Text(stringResource(R.string.ritual_sessions_today, state.snapshot.coherenceSessionsCount), color = secondaryWhite)
                }
            }
            item {
                DashboardCard(stringResource(R.string.ritual_learning_section), Icons.Rounded.NightsStay, onOpenReview) {
                    val review = state.review
                    if (review == null) {
                        Text(stringResource(R.string.ritual_day_not_closed), color = Color.White)
                    } else {
                        Text(stringResource(R.string.ritual_energy_alignment_value, review.energy, review.identityAlignment), color = Color.White)
                        Text(review.suggestion, color = secondaryWhite)
                    }
                }
            }
            state.errorMessage?.let { message -> item { Text(localizedCycleError(message), color = Color(0xFFFFD5D5)) } }
        }
    }
}

@Composable
fun RitualSummaryScreen(sessions: List<MorningDialogSession>, reviews: List<EveningReview>) {
    val stats = remember(sessions, reviews) { RitualStats(sessions, reviews) }
    val localizedIdentities = stringArrayResource(R.array.ritual_identity_suggestions)
    val localizedEmotions = stringArrayResource(R.array.ritual_emotion_suggestions)
    Box(Modifier.fillMaxSize().background(statsBrush)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Text(stringResource(R.string.ritual_stats_title), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
            item {
                Text(stringResource(R.string.ritual_stats_general), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatMetric(stringResource(R.string.ritual_mornings), stats.totalRituals.toString(), stringResource(R.string.ritual_rituals_lower), Modifier.weight(1f))
                    StatMetric(stringResource(R.string.ritual_active_days), stats.daysWithRitual.toString(), stringResource(R.string.ritual_with_ritual), Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatMetric(stringResource(R.string.ritual_current_streak), stats.currentStreak.toString(), stringResource(R.string.ritual_days_lower), Modifier.weight(1f))
                    StatMetric(stringResource(R.string.ritual_best_streak), stats.longestStreak.toString(), stringResource(R.string.ritual_days_lower), Modifier.weight(1f))
                }
            }
            item {
                GlassCard(stringResource(R.string.ritual_morning_evening_cycle), Icons.Rounded.Timeline) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatMetric(stringResource(R.string.ritual_evenings), stats.totalReviews.toString(), stringResource(R.string.ritual_registered), Modifier.weight(1f))
                        StatMetric(stringResource(R.string.ritual_complete_cycles), "${(stats.closureRate * 100).roundToInt()}%", stringResource(R.string.ritual_with_intention), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatMetric(stringResource(R.string.ritual_average_energy), stats.averageEnergy.formatScore(), stringResource(R.string.ritual_out_of_five), Modifier.weight(1f))
                        StatMetric(stringResource(R.string.ritual_coherence), stats.averageAlignment.formatScore(), stringResource(R.string.ritual_out_of_five), Modifier.weight(1f))
                    }
                    Text(stringResource(stats.insightRes), color = secondaryWhite)
                }
            }
            item {
                GlassCard(stringResource(R.string.ritual_weekly_distribution), Icons.Rounded.BarChart) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
                        val max = stats.weekdayCounts.maxOrNull()?.coerceAtLeast(1) ?: 1
                        stats.weekdayCounts.forEachIndexed { index, count ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                                Box(Modifier.fillMaxWidth().height((12 + 58 * count / max).dp).background(Color.White.copy(alpha = 0.82f), RoundedCornerShape(5.dp)))
                                Text(localizedRitualWeekdays()[index], color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            item {
                GlassCard(stringResource(R.string.ritual_frequent_focuses), Icons.Rounded.AutoAwesome) {
                    Text(stringResource(R.string.ritual_identities), color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(
                        stats.topIdentities.ifEmpty { listOf(stringResource(R.string.ritual_no_data)) }
                            .joinToString("  ·  ") { localizeCountedCycleValue(it, storedCycleIdentities, localizedIdentities) },
                        color = secondaryWhite
                    )
                    Text(stringResource(R.string.ritual_emotions), color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(
                        stats.topEmotions.ifEmpty { listOf(stringResource(R.string.ritual_no_data)) }
                            .joinToString("  ·  ") { localizeCountedCycleValue(it, storedCycleEmotions, localizedEmotions) },
                        color = secondaryWhite
                    )
                }
            }
        }
    }
}

@Composable
private fun RitualDateButton(epochDay: Long, onDateChange: (Long) -> Unit) {
    val context = LocalContext.current
    val date = LocalDate.ofEpochDay(epochDay)
    OutlinedButton(
        onClick = {
            DatePickerDialog(
                context,
                { _, year, month, day -> onDateChange(LocalDate.of(year, month + 1, day).toEpochDay()) },
                date.year,
                date.monthValue - 1,
                date.dayOfMonth
            ).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
        },
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f))
    ) {
        Icon(Icons.Rounded.CalendarMonth, null, tint = Color.White)
        Spacer(Modifier.size(8.dp))
        val locale = LocalConfiguration.current.locales[0]
        Text(date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", locale)), color = Color.White)
    }
}

@Composable
private fun GlassCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = glass),
        border = BorderStroke(1.dp, glassStroke)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, tint = Color(0xFFFFE082))
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            content()
        }
    }
}

@Composable
private fun AnswerCard(title: String, prompt: String, value: String, onChange: (String) -> Unit) {
    GlassCard(title, Icons.Rounded.Edit) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().height(118.dp),
            placeholder = { Text(prompt, color = Color.White.copy(alpha = 0.45f)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color.Black.copy(alpha = 0.18f),
                unfocusedContainerColor = Color.Black.copy(alpha = 0.18f),
                focusedBorderColor = Color.White.copy(alpha = 0.38f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
                cursorColor = Color.White
            )
        )
    }
}

@Composable
private fun SelectBox(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.clickable(onClick = onClick).background(
            if (selected) Color.White else Color.White.copy(alpha = 0.12f),
            RoundedCornerShape(14.dp)
        ).padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) Color(0xFF443A86) else Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun SnapshotMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Color.White.copy(alpha = 0.09f), RoundedCornerShape(13.dp)).padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = secondaryWhite, fontSize = 11.sp)
    }
}

@Composable
private fun DashboardCard(title: String, icon: ImageVector, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = glass),
        border = BorderStroke(1.dp, glassStroke),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Color(0xFFFFE082))
                Spacer(Modifier.size(8.dp))
                Text(title, Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Bold)
                Box(Modifier.size(24.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ArrowForward, null, tint = Color(0xFF463E7B), modifier = Modifier.size(14.dp))
                }
            }
            content()
        }
    }
}

@Composable
private fun DashboardMetric(title: String, value: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clickable(onClick = onClick).background(glass, RoundedCornerShape(16.dp)).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icon, null, tint = Color(0xFFFFE082))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
        Text(title, color = secondaryWhite, fontSize = 11.sp)
    }
}

@Composable
private fun StatMetric(title: String, value: String, subtitle: String, modifier: Modifier) {
    Column(modifier.background(Color.White.copy(alpha = 0.11f), RoundedCornerShape(13.dp)).padding(11.dp)) {
        Text(title, color = secondaryWhite, fontSize = 12.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(subtitle, color = secondaryWhite, fontSize = 11.sp)
    }
}

private fun EveningReview?.toDraft() = this?.let {
    EveningReviewDraft(
        energy = it.energy,
        predominantEmotionId = it.predominantEmotionId,
        whatWentWell = it.whatWentWell,
        learning = it.learning,
        autopilotMoment = it.autopilotMoment,
        gratitude = it.gratitude,
        tomorrowPreparation = it.tomorrowPreparation,
        identityAlignment = it.identityAlignment,
        createJournalEntry = it.journalEntryRequested
    )
} ?: EveningReviewDraft()

private data class RitualStats(
    val totalRituals: Int,
    val daysWithRitual: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val totalReviews: Int,
    val closureRate: Double,
    val averageEnergy: Double,
    val averageAlignment: Double,
    val weekdayCounts: List<Int>,
    val topIdentities: List<String>,
    val topEmotions: List<String>,
    val insightRes: Int
) {
    constructor(sessions: List<MorningDialogSession>, reviews: List<EveningReview>) : this(
        totalRituals = sessions.size,
        daysWithRitual = sessions.map { it.sessionDateEpochDay }.toSet().size,
        currentStreak = currentStreak(sessions.map { it.sessionDateEpochDay }.toSet()),
        longestStreak = longestStreak(sessions.map { it.sessionDateEpochDay }.toSet()),
        totalReviews = reviews.size,
        closureRate = if (sessions.isEmpty()) 0.0 else reviews.map { it.sessionDateEpochDay }.toSet().intersect(sessions.map { it.sessionDateEpochDay }.toSet()).size.toDouble() / sessions.map { it.sessionDateEpochDay }.toSet().size,
        averageEnergy = reviews.map { it.energy }.averageOrZero(),
        averageAlignment = reviews.map { it.identityAlignment }.averageOrZero(),
        weekdayCounts = (1..7).map { day -> sessions.count { LocalDate.ofEpochDay(it.sessionDateEpochDay).dayOfWeek.value == day } },
        topIdentities = topValues(sessions.flatMap { it.identity.split(',') }),
        topEmotions = topValues(sessions.flatMap { it.emotions }),
        insightRes = when {
            reviews.isEmpty() -> R.string.ritual_insight_empty
            sessions.isNotEmpty() && reviews.map { it.sessionDateEpochDay }.toSet().intersect(sessions.map { it.sessionDateEpochDay }.toSet()).size.toDouble() / sessions.map { it.sessionDateEpochDay }.toSet().size < 0.5 -> R.string.ritual_insight_closure
            reviews.map { it.identityAlignment }.average() < 3 -> R.string.ritual_insight_alignment
            reviews.map { it.energy }.average() < 3 -> R.string.ritual_insight_energy
            else -> R.string.ritual_insight_consistent
        }
    )

    companion object {
        private fun currentStreak(days: Set<Long>): Int {
            var cursor = LocalDate.now().toEpochDay()
            if (cursor !in days && cursor - 1 in days) cursor--
            var count = 0
            while (cursor in days) { count++; cursor-- }
            return count
        }

        private fun longestStreak(days: Set<Long>): Int {
            var best = 0
            var current = 0
            var previous: Long? = null
            days.sorted().forEach { day ->
                current = if (previous != null && day == previous!! + 1) current + 1 else 1
                best = maxOf(best, current)
                previous = day
            }
            return best
        }

        private fun topValues(values: List<String>): List<String> = values.map(String::trim).filter(String::isNotBlank)
            .groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(6).map { "${it.key} ${it.value}" }
    }
}

private fun List<Int>.averageOrZero() = if (isEmpty()) 0.0 else average()
private fun Double.formatScore() = if (this == 0.0 || isNaN()) "—" else String.format(Locale.getDefault(), "%.1f", this)

@Composable
private fun localizedEveningEmotionTitle(id: String): String = stringResource(
    when (id.lowercase(Locale.ROOT)) {
        "alegre" -> R.string.ritual_emotion_happy
        "ansioso" -> R.string.ritual_emotion_anxious
        "triste" -> R.string.ritual_emotion_sad
        "enfadado" -> R.string.ritual_emotion_angry
        "cansado" -> R.string.ritual_emotion_tired
        "agradecido" -> R.string.ritual_emotion_grateful
        else -> R.string.ritual_emotion_serene
    }
)

@Composable
private fun localizedRitualWeekdays(): List<String> = listOf(
    stringResource(R.string.goal_stats_monday_short),
    stringResource(R.string.goal_stats_tuesday_short),
    stringResource(R.string.goal_stats_wednesday_short),
    stringResource(R.string.goal_stats_thursday_short),
    stringResource(R.string.goal_stats_friday_short),
    stringResource(R.string.goal_stats_saturday_short),
    stringResource(R.string.goal_stats_sunday_short)
)

@Composable
private fun localizedCycleError(message: String): String = stringResource(
    when (message) {
        "No se pudo preparar la información del día." -> R.string.ritual_day_load_error
        "No se pudo guardar el cierre. Inténtalo de nuevo." -> R.string.ritual_evening_save_error
        else -> R.string.common_unknown_error
    }
)

private fun localizeCycleValues(value: String, stored: List<String>, localized: Array<String>): String =
    value.split(',').joinToString(", ") { rawPart ->
        val part = rawPart.trim()
        val index = stored.indexOf(part)
        if (index >= 0) localized.getOrElse(index) { part } else part
    }

private fun localizeCountedCycleValue(value: String, stored: List<String>, localized: Array<String>): String {
    val separator = value.lastIndexOf(' ')
    if (separator <= 0) return localizeCycleValues(value, stored, localized)
    val base = value.substring(0, separator)
    val count = value.substring(separator + 1)
    return "${localizeCycleValues(base, stored, localized)} $count"
}
