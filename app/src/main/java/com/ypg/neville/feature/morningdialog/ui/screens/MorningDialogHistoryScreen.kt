package com.ypg.neville.feature.morningdialog.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ypg.neville.R
import com.ypg.neville.feature.morningdialog.domain.MorningDialogSession
import com.ypg.neville.feature.morningdialog.domain.EveningReview
import com.ypg.neville.feature.morningdialog.ui.components.MorningDialogStyles
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val storedRitualIdentities = listOf(
    "Enfocado", "Calmado", "Disciplinado", "Valiente", "Presente", "Compasivo", "Curioso",
    "Reflexivo", "Proactivo", "Consciente", "Observador", "Intencional", "Constante", "Organizado",
    "Persistente", "Productivo", "Comprometido", "Auténtico", "Visionario", "Autodidacta", "Valiente",
    "Explorador", "Innovador", "Expansivo"
)
private val storedRitualEmotions = listOf(
    "Calma", "Confianza", "Gratitud", "Claridad", "Energía", "Apertura", "Serenidad", "Empatía",
    "Autoestima", "Alegría", "Paz", "Amor", "Compasión", "Esperanza", "Entusiasmo", "Seguridad",
    "Asombro", "Satisfacción", "Fluidez", "Conexión", "Fortaleza", "Resiliencia"
)

@Composable
private fun EveningHistoryList(reviews: List<EveningReview>, onDeleteReview: (Long) -> Unit) {
    var expandedId by remember { mutableStateOf<Long?>(null) }
    var pendingDelete by remember { mutableStateOf<Long?>(null) }

    if (reviews.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.ritual_evening_history_empty), color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.ritual_evening_history_empty_body), color = Color.White.copy(alpha = 0.72f), textAlign = TextAlign.Center)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(reviews, key = { "evening_${it.id}" }) { review ->
            val expanded = expandedId == review.id
            Card(
                modifier = Modifier.fillMaxWidth().clickable { expandedId = if (expanded) null else review.id },
                colors = CardDefaults.cardColors(containerColor = MorningDialogStyles.ritualCardColor),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                LocalDate.ofEpochDay(review.sessionDateEpochDay).format(localizedDateFormatter()),
                                color = MorningDialogStyles.ritualCardText,
                                fontWeight = FontWeight.Bold
                            )
                            Text(stringResource(R.string.ritual_evening), color = MorningDialogStyles.ritualCardText.copy(alpha = 0.68f))
                        }
                        TextButton(onClick = { pendingDelete = review.id }) { Text(stringResource(R.string.common_delete)) }
                    }
                    if (expanded) {
                        EveningHistoryValue(stringResource(R.string.ritual_energy), "${review.energy}/5")
                        EveningHistoryValue(stringResource(R.string.ritual_predominant_emotion), localizedEveningEmotion(review.predominantEmotionId))
                        EveningHistoryValue(stringResource(R.string.ritual_went_well_label), review.whatWentWell)
                        EveningHistoryValue(stringResource(R.string.ritual_learning_label), review.learning)
                        EveningHistoryValue(stringResource(R.string.ritual_autopilot_label), review.autopilotMoment)
                        EveningHistoryValue(stringResource(R.string.ritual_gratitude_label), review.gratitude)
                        EveningHistoryValue(stringResource(R.string.ritual_prepared_tomorrow), review.tomorrowPreparation)
                        EveningHistoryValue(stringResource(R.string.ritual_coherence_label), "${review.identityAlignment}/5")
                        EveningHistoryValue(stringResource(R.string.ritual_footprint), stringResource(R.string.ritual_footprint_value, review.agendaCompletedCount, review.agendaTotalCount, review.goalUnitsCompletedCount, review.presenceReturns))
                        EveningHistoryValue(stringResource(R.string.ritual_improvement_tomorrow), review.suggestion)
                    } else {
                        Text(
                            "${stringResource(R.string.ritual_energy)} ${review.energy}/5 · ${localizedEveningEmotion(review.predominantEmotionId)}\n${review.learning.ifBlank { review.suggestion }}",
                            color = MorningDialogStyles.ritualCardText,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.ritual_delete_evening)) },
            text = { Text(stringResource(R.string.ritual_delete_evening_question)) },
            confirmButton = {
                TextButton(onClick = { pendingDelete = null; onDeleteReview(id) }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.common_cancel)) } }
        )
    }
}

@Composable
private fun EveningHistoryValue(title: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = MorningDialogStyles.ritualCardText, fontWeight = FontWeight.SemiBold)
        Text(value.ifBlank { stringResource(R.string.ritual_no_answer) }, color = MorningDialogStyles.ritualCardText.copy(alpha = 0.78f))
    }
}

@Composable
fun MorningDialogHistoryScreen(
    sessions: List<MorningDialogSession>,
    reviews: List<EveningReview>,
    onNoteClick: (Long) -> Unit,
    onExportClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
    onDeleteReview: (Long) -> Unit
) {
    var kind by remember { mutableStateOf(HistoryKind.Morning) }
    Column(Modifier.fillMaxSize().background(MorningDialogStyles.backgroundBrush)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HistoryKind.entries.forEach { item ->
                TextButton(
                    onClick = { kind = item },
                    modifier = Modifier.weight(1f).background(
                        if (kind == item) MorningDialogStyles.ritualCardColor else Color.White.copy(alpha = 0.14f),
                        RoundedCornerShape(14.dp)
                    )
                ) {
                    Text(
                        if (item == HistoryKind.Morning) stringResource(R.string.ritual_title) else stringResource(R.string.ritual_evening),
                        color = if (kind == item) MorningDialogStyles.ritualCardText else Color.White
                    )
                }
            }
        }
        Box(Modifier.weight(1f)) {
            if (kind == HistoryKind.Morning) {
                MorningHistoryList(sessions, onNoteClick, onExportClick, onDeleteClick)
            } else {
                EveningHistoryList(reviews, onDeleteReview)
            }
        }
    }
}

private enum class HistoryKind {
    Morning,
    Evening
}

@Composable
private fun MorningHistoryList(
    sessions: List<MorningDialogSession>,
    onNoteClick: (Long) -> Unit,
    onExportClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit
) {
    var expandedSessionId by remember { mutableStateOf<Long?>(null) }
    var showCalendar by remember { mutableStateOf(false) }
    var selectedEpochDay by remember { mutableStateOf<Long?>(null) }
    var visibleMonth by remember { mutableStateOf(YearMonth.now()) }
    var searchText by remember { mutableStateOf("") }

    LaunchedEffect(sessions) {
        val expanded = expandedSessionId
        if (expanded != null && sessions.none { it.id == expanded }) {
            expandedSessionId = null
        }
        val selected = selectedEpochDay
        if (selected != null && sessions.none { it.sessionDateEpochDay == selected }) {
            selectedEpochDay = null
        }
    }

    if (sessions.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MorningDialogStyles.backgroundBrush)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                stringResource(R.string.ritual_history_empty),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }
        return
    }

    val sessionsByDay = remember(sessions) { sessions.associateBy { it.sessionDateEpochDay } }
    val filteredSessions = remember(sessions, searchText, showCalendar, selectedEpochDay) {
        val normalizedQuery = searchText.trim().lowercase()
        val baseSessions = if (showCalendar) {
            selectedEpochDay?.let { day -> sessions.filter { it.sessionDateEpochDay == day } }.orEmpty()
        } else {
            sessions
        }
        if (normalizedQuery.isBlank()) {
            baseSessions
        } else {
            baseSessions.filter { it.matchesHistoryQuery(normalizedQuery) }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MorningDialogStyles.backgroundBrush)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            HistoryFiltersCard(
                searchText = searchText,
                onSearchTextChange = { searchText = it },
                showCalendar = showCalendar,
                onToggleCalendar = {
                    showCalendar = !showCalendar
                    if (!showCalendar) {
                        selectedEpochDay = null
                    } else if (selectedEpochDay == null) {
                        selectedEpochDay = sessions.firstOrNull()?.sessionDateEpochDay
                        selectedEpochDay?.let { visibleMonth = YearMonth.from(LocalDate.ofEpochDay(it)) }
                    }
                }
            )
        }

        if (showCalendar) {
            item {
                RitualCalendar(
                    visibleMonth = visibleMonth,
                    sessionsByDay = sessionsByDay,
                    selectedEpochDay = selectedEpochDay,
                    onPreviousMonth = { visibleMonth = visibleMonth.minusMonths(1) },
                    onNextMonth = { visibleMonth = visibleMonth.plusMonths(1) },
                    onDaySelected = { selectedEpochDay = it }
                )
            }
        }

        if (filteredSessions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MorningDialogStyles.ritualCardColor)
                ) {
                    Text(
                        text = when {
                            showCalendar && selectedEpochDay == null -> stringResource(R.string.ritual_history_select_day)
                            showCalendar -> stringResource(R.string.ritual_history_no_day)
                            else -> stringResource(R.string.ritual_history_no_match)
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
                        color = MorningDialogStyles.ritualCardText,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        items(filteredSessions, key = { it.id }) { session ->
            var showMenu by remember(session.id) { mutableStateOf(false) }
            var showDeleteConfirm by remember(session.id) { mutableStateOf(false) }
            val isExpanded = expandedSessionId == session.id

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MorningDialogStyles.ritualCardColor)
            ) {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = 0.9f,
                                stiffness = 450f
                            )
                        ),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = Instant.ofEpochMilli(session.completedAtEpochMillis)
                                .atZone(ZoneId.systemDefault())
                                .format(localizedDateFormatter()),
                            style = MaterialTheme.typography.titleMedium,
                            color = MorningDialogStyles.ritualCardText,
                            modifier = Modifier.weight(1f)
                        )
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_menu_open),
                                    contentDescription = stringResource(R.string.ritual_options),
                                    tint = MorningDialogStyles.ritualCardText
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (session.noteText.isBlank()) stringResource(R.string.ritual_create_note) else stringResource(R.string.ritual_edit_note)) },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_note),
                                            contentDescription = stringResource(R.string.ritual_note),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onNoteClick(session.id)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.ritual_export_diary)) },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_diario_pen_book),
                                            contentDescription = stringResource(R.string.ritual_export_diary),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onExportClick(session.id)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.ritual_delete)) },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_delete),
                                            contentDescription = stringResource(R.string.ritual_delete),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showDeleteConfirm = true
                                    }
                                )
                            }
                        }
                    }

                    if (isExpanded) {
                        RitualExpandedContent(session = session)
                    } else {
                        Text(
                            text = buildCollapsedPreview(session),
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
                            color = MorningDialogStyles.ritualCardText,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedSessionId = session.id }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (isExpanded) R.drawable.ic_arriba else R.drawable.ic_abajo
                            ),
                            contentDescription = if (isExpanded) stringResource(R.string.ritual_collapse) else stringResource(R.string.ritual_expand),
                            tint = MorningDialogStyles.ritualCardText,
                            modifier = Modifier
                                .clickable {
                                    expandedSessionId = if (isExpanded) null else session.id
                                }
                                .padding(2.dp)
                        )
                    }
                }
            }

            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = { Text(stringResource(R.string.ritual_delete)) },
                    text = { Text(stringResource(R.string.ritual_delete_question)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteConfirm = false
                                onDeleteClick(session.id)
                            }
                        ) {
                            Text(stringResource(R.string.common_delete))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) {
                            Text(stringResource(R.string.common_cancel))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun HistoryFiltersCard(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    showCalendar: Boolean,
    onToggleCalendar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MorningDialogStyles.ritualCardColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.ritual_filters),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MorningDialogStyles.ritualCardText
                )
                TextButton(onClick = onToggleCalendar) {
                    Text(
                        text = if (showCalendar) stringResource(R.string.common_hide_calendar) else stringResource(R.string.common_show_calendar),
                        color = MorningDialogStyles.ritualCardText
                    )
                }
            }

            OutlinedTextField(
                value = searchText,
                onValueChange = onSearchTextChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.ritual_search)) },
                placeholder = {
                    Text(
                        text = stringResource(R.string.ritual_search_hint),
                        color = MorningDialogStyles.ritualCardText
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedPlaceholderColor = MorningDialogStyles.ritualCardText,
                    unfocusedPlaceholderColor = MorningDialogStyles.ritualCardText
                ),
                trailingIcon = {
                    if (searchText.isNotBlank()) {
                        TextButton(onClick = { onSearchTextChange("") }) {
                            Text(stringResource(R.string.common_clear))
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun RitualCalendar(
    visibleMonth: YearMonth,
    sessionsByDay: Map<Long, MorningDialogSession>,
    selectedEpochDay: Long?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDaySelected: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MorningDialogStyles.ritualCardColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onPreviousMonth) { Text("←") }
                Text(
                    text = visibleMonth.atDay(1).format(localizedMonthFormatter())
                        .replaceFirstChar { char -> char.titlecase(Locale.getDefault()) },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MorningDialogStyles.ritualCardText,
                    textAlign = TextAlign.Center
                )
                TextButton(onClick = onNextMonth) { Text("→") }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                localizedCalendarWeekdays().forEach { dayName ->
                    Text(
                        text = dayName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MorningDialogStyles.ritualCardText,
                        textAlign = TextAlign.Center
                    )
                }
            }

            val firstDay = visibleMonth.atDay(1)
            val leadingEmptyDays = firstDay.dayOfWeek.value - 1
            val daysInMonth = visibleMonth.lengthOfMonth()
            val totalCells = ((leadingEmptyDays + daysInMonth + 6) / 7) * 7

            (0 until totalCells).chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { cellIndex ->
                        val dayNumber = cellIndex - leadingEmptyDays + 1
                        val date = if (dayNumber in 1..daysInMonth) visibleMonth.atDay(dayNumber) else null
                        val epochDay = date?.toEpochDay()
                        val hasEntry = epochDay != null && sessionsByDay.containsKey(epochDay)
                        val isSelected = epochDay != null && epochDay == selectedEpochDay

                        CalendarDayCell(
                            dayNumber = date?.dayOfMonth,
                            hasEntry = hasEntry,
                            isSelected = isSelected,
                            onClick = {
                                if (epochDay != null) onDaySelected(epochDay)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayNumber: Int?,
    hasEntry: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> MorningDialogStyles.buttonColor
        hasEntry -> Color(0xFFB9DDF6)
        else -> Color.Transparent
    }
    val shape = RoundedCornerShape(10.dp)
    val clickableModifier = if (dayNumber != null) Modifier.clickable(onClick = onClick) else Modifier

    Box(
        modifier = modifier
            .height(28.dp)
            .padding(1.dp)
            .background(backgroundColor, shape)
            .then(clickableModifier),
        contentAlignment = Alignment.Center
    ) {
        if (dayNumber != null) {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (hasEntry) FontWeight.Bold else FontWeight.Normal
                ),
                color = MorningDialogStyles.ritualCardText,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RitualExpandedContent(session: MorningDialogSession) {
    val localizedIdentities = androidx.compose.ui.res.stringArrayResource(R.array.ritual_identity_suggestions)
    val localizedEmotions = androidx.compose.ui.res.stringArrayResource(R.array.ritual_emotion_suggestions)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionLabel(stringResource(R.string.ritual_goals))
        BulletLines(session.goals)

        SectionLabel(stringResource(R.string.ritual_identity))
        Text(
            text = localizeStoredSuggestion(session.identity, storedRitualIdentities, localizedIdentities).ifBlank { "—" },
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
            color = MorningDialogStyles.ritualCardText
        )

        SectionLabel(stringResource(R.string.ritual_emotions))
        BulletLines(session.emotions.map { localizeStoredSuggestion(it, storedRitualEmotions, localizedEmotions) })

        SectionLabel(stringResource(R.string.ritual_situations_responses))
        if (session.anticipatedSituations.isEmpty()) {
            Text(
                text = "-",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
                color = MorningDialogStyles.ritualCardText
            )
        } else {
            session.anticipatedSituations.forEachIndexed { index, trigger ->
                val response = session.consciousResponses.getOrElse(index) { "" }
                Text(
                    text = "• ${stringResource(R.string.ritual_response_value, trigger, response)}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
                    color = MorningDialogStyles.ritualCardText
                )
            }
        }

        SectionLabel(stringResource(R.string.ritual_note_title))
        Text(
            text = session.noteText.ifBlank { "-" },
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
            color = MorningDialogStyles.ritualCardText
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            lineHeight = 24.sp
        ),
        color = MorningDialogStyles.ritualCardText
    )
}

@Composable
private fun BulletLines(items: List<String>) {
    if (items.isEmpty()) {
        Text(
            text = "-",
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
            color = MorningDialogStyles.ritualCardText
        )
    } else {
        items.forEach { item ->
            Text(
                text = "• $item",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
                color = MorningDialogStyles.ritualCardText
            )
        }
    }
}

@Composable
private fun buildCollapsedPreview(session: MorningDialogSession): String {
    val localizedIdentities = androidx.compose.ui.res.stringArrayResource(R.array.ritual_identity_suggestions)
    val localizedEmotions = androidx.compose.ui.res.stringArrayResource(R.array.ritual_emotion_suggestions)
    return listOf(
        stringResource(R.string.ritual_goals_value, session.goals.joinToString().ifBlank { "—" }),
        stringResource(
            R.string.ritual_identity_value,
            localizeStoredSuggestion(session.identity, storedRitualIdentities, localizedIdentities).ifBlank { "—" }
        ),
        stringResource(
            R.string.ritual_emotions_value,
            session.emotions.joinToString { localizeStoredSuggestion(it, storedRitualEmotions, localizedEmotions) }.ifBlank { "—" }
        )
    ).joinToString("\n")
}

private fun localizeStoredSuggestion(value: String, stored: List<String>, localized: Array<String>): String {
    return value.split(',').joinToString(", ") { rawPart ->
        val part = rawPart.trim()
        val index = stored.indexOf(part)
        if (index >= 0) localized.getOrElse(index) { part } else part
    }
}

@Composable
private fun localizedDateFormatter(): DateTimeFormatter {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { DateTimeFormatter.ofPattern("dd MMM yyyy", locale) }
}

@Composable
private fun localizedMonthFormatter(): DateTimeFormatter {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { DateTimeFormatter.ofPattern("MMMM yyyy", locale) }
}

@Composable
private fun localizedCalendarWeekdays(): List<String> = listOf(
    stringResource(R.string.goal_stats_monday_short),
    stringResource(R.string.goal_stats_tuesday_short),
    stringResource(R.string.goal_stats_wednesday_short),
    stringResource(R.string.goal_stats_thursday_short),
    stringResource(R.string.goal_stats_friday_short),
    stringResource(R.string.goal_stats_saturday_short),
    stringResource(R.string.goal_stats_sunday_short)
)

@Composable
private fun localizedEveningEmotion(id: String): String = stringResource(
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

private fun MorningDialogSession.matchesHistoryQuery(query: String): Boolean {
    return goals.any { it.contains(query, ignoreCase = true) } ||
        emotions.any { it.contains(query, ignoreCase = true) } ||
        noteText.contains(query, ignoreCase = true)
}
