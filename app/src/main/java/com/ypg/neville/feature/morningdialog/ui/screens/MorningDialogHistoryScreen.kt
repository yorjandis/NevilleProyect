package com.ypg.neville.feature.morningdialog.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ypg.neville.R
import com.ypg.neville.feature.morningdialog.domain.MorningDialogSession
import com.ypg.neville.feature.morningdialog.ui.components.MorningDialogStyles
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

@Composable
fun MorningDialogHistoryScreen(
    sessions: List<MorningDialogSession>,
    onNoteClick: (Long) -> Unit,
    onExportClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit
) {
    var expandedSessionId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(sessions) {
        val expanded = expandedSessionId
        if (expanded != null && sessions.none { it.id == expanded }) {
            expandedSessionId = null
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MorningDialogStyles.backgroundBrush)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(sessions, key = { it.id }) { session ->
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
