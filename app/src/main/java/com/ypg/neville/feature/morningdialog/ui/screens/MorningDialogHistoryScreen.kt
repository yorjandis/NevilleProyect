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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-ES"))

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
            Text("Aún no hay cierres guardados.", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text("Cuando cierres un día, su aprendizaje aparecerá aquí.", color = Color.White.copy(alpha = 0.72f), textAlign = TextAlign.Center)
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
                                LocalDate.ofEpochDay(review.sessionDateEpochDay).format(dateFormatter),
                                color = MorningDialogStyles.ritualCardText,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Cierre consciente", color = MorningDialogStyles.ritualCardText.copy(alpha = 0.68f))
                        }
                        TextButton(onClick = { pendingDelete = review.id }) { Text("Eliminar") }
                    }
                    if (expanded) {
                        EveningHistoryValue("Energía", "${review.energy}/5")
                        EveningHistoryValue("Emoción predominante", com.ypg.neville.feature.morningdialog.domain.EveningRitualRepository.emotionTitle(review.predominantEmotionId))
                        EveningHistoryValue("Lo que salió bien", review.whatWentWell)
                        EveningHistoryValue("Aprendizaje", review.learning)
                        EveningHistoryValue("Piloto automático", review.autopilotMoment)
                        EveningHistoryValue("Gratitud", review.gratitude)
                        EveningHistoryValue("Preparado para mañana", review.tomorrowPreparation)
                        EveningHistoryValue("Coherencia", "${review.identityAlignment}/5")
                        EveningHistoryValue("Huella", "Agenda ${review.agendaCompletedCount}/${review.agendaTotalCount} · Metas ${review.goalUnitsCompletedCount} · Presencia ${review.presenceReturns}")
                        EveningHistoryValue("Mejora para mañana", review.suggestion)
                    } else {
                        Text(
                            "Energía ${review.energy}/5 · ${com.ypg.neville.feature.morningdialog.domain.EveningRitualRepository.emotionTitle(review.predominantEmotionId)}\n${review.learning.ifBlank { review.suggestion }}",
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
            title = { Text("Eliminar cierre") },
            text = { Text("¿Seguro que deseas eliminar este cierre del historial?") },
            confirmButton = {
                TextButton(onClick = { pendingDelete = null; onDeleteReview(id) }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun EveningHistoryValue(title: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = MorningDialogStyles.ritualCardText, fontWeight = FontWeight.SemiBold)
        Text(value.ifBlank { "Sin respuesta" }, color = MorningDialogStyles.ritualCardText.copy(alpha = 0.78f))
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
                    Text(item.title, color = if (kind == item) MorningDialogStyles.ritualCardText else Color.White)
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

private enum class HistoryKind(val title: String) {
    Morning("Ritual Matutino"),
    Evening("Cierre Consciente")
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
                "Aún no hay sesiones guardadas.",
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
                            showCalendar && selectedEpochDay == null -> "Selecciona un día del calendario."
                            showCalendar -> "No hay ritual guardado para este día con el filtro actual."
                            else -> "No hay rituales que coincidan con la búsqueda."
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
                                .format(dateFormatter),
                            style = MaterialTheme.typography.titleMedium,
                            color = MorningDialogStyles.ritualCardText,
                            modifier = Modifier.weight(1f)
                        )
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_menu_open),
                                    contentDescription = "Opciones ritual",
                                    tint = MorningDialogStyles.ritualCardText
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (session.noteText.isBlank()) "Crear nota" else "Editar nota") },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_note),
                                            contentDescription = "Nota",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onNoteClick(session.id)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Exportar al Diario") },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_diario_pen_book),
                                            contentDescription = "Exportar al Diario",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onExportClick(session.id)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Eliminar ritual") },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_delete),
                                            contentDescription = "Eliminar ritual",
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
                            contentDescription = if (isExpanded) "Colapsar" else "Expandir",
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
                    title = { Text("Eliminar ritual") },
                    text = { Text("¿Seguro que deseas eliminar este ritual del historial?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteConfirm = false
                                onDeleteClick(session.id)
                            }
                        ) {
                            Text("Eliminar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) {
                            Text("Cancelar")
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
                    text = "Filtros",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MorningDialogStyles.ritualCardText
                )
                TextButton(onClick = onToggleCalendar) {
                    Text(
                        text = if (showCalendar) "Ocultar calendario" else "Mostrar calendario",
                        color = MorningDialogStyles.ritualCardText
                    )
                }
            }

            OutlinedTextField(
                value = searchText,
                onValueChange = onSearchTextChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Buscar") },
                placeholder = {
                    Text(
                        text = "Metas, emociones o nota",
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
                            Text("Limpiar")
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
                    text = visibleMonth.atDay(1).format(monthFormatter)
                        .replaceFirstChar { char -> char.titlecase(Locale.getDefault()) },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MorningDialogStyles.ritualCardText,
                    textAlign = TextAlign.Center
                )
                TextButton(onClick = onNextMonth) { Text("→") }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("L", "M", "X", "J", "V", "S", "D").forEach { dayName ->
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionLabel("Metas")
        BulletLines(session.goals)

        SectionLabel("Identidad")
        Text(
            text = session.identity.ifBlank { "-" },
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
            color = MorningDialogStyles.ritualCardText
        )

        SectionLabel("Emociones")
        BulletLines(session.emotions)

        SectionLabel("Situaciones y respuestas")
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
                    text = "• Si $trigger, responderé con $response",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
                    color = MorningDialogStyles.ritualCardText
                )
            }
        }

        SectionLabel("Nota del ritual")
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

private fun buildCollapsedPreview(session: MorningDialogSession): String {
    return buildString {
        appendLine("Metas: ${session.goals.joinToString().ifBlank { "-" }}")
        appendLine("Identidad: ${session.identity.ifBlank { "-" }}")
        append("Emociones: ${session.emotions.joinToString().ifBlank { "-" }}")
    }
}

private fun MorningDialogSession.matchesHistoryQuery(query: String): Boolean {
    return goals.any { it.contains(query, ignoreCase = true) } ||
        emotions.any { it.contains(query, ignoreCase = true) } ||
        noteText.contains(query, ignoreCase = true)
}
