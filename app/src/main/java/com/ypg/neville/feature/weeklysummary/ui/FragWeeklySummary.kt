@file:SuppressLint("NewApi")

package com.ypg.neville.feature.weeklysummary.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.feature.weeklysummary.data.WeeklySummaryEntity
import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryRepository
import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryTime
import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryViewData
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import kotlin.math.max

class FragWeeklySummary : Fragment() {

    private val dbExecutor = Executors.newSingleThreadExecutor()

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (view as ComposeView).setContent {
            MaterialTheme {
                WeeklySummaryScreen()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        MainActivity.currentInstance()?.icToolsBarFraseAdd?.visibility = View.GONE
        MainActivity.currentInstance()?.icToolsBarNotaAdd?.visibility = View.GONE
        MainActivity.currentInstance()?.icToolsBarFav?.visibility = View.GONE
    }

    @Composable
    private fun WeeklySummaryScreen() {
        val repository = remember {
            WeeklySummaryRepository(
                NevilleRoomDatabase
                    .getInstance(requireContext())
                    .weeklySummaryDao()
            )
        }
        val summaries = remember { mutableStateListOf<WeeklySummaryEntity>() }
        var selected by remember { mutableStateOf<WeeklySummaryEntity?>(null) }
        var tab by remember { mutableStateOf("resumen") }
        var monthCursor by remember { mutableStateOf(LocalDate.now()) }
        var sectionOrderTick by remember { mutableStateOf(0) }

        fun reload() {
            dbExecutor.execute {
                repository.generatePendingSummaries()
                val all = repository.getAllSummariesDesc()
                activity?.runOnUiThread {
                    summaries.clear()
                    summaries.addAll(all)
                    if (selected == null || summaries.none { it.weekStartMillis == selected?.weekStartMillis }) {
                        selected = summaries.firstOrNull()
                    } else {
                        selected = summaries.firstOrNull { it.weekStartMillis == selected?.weekStartMillis }
                    }
                }
            }
        }

        LaunchedEffect(Unit, sectionOrderTick) {
            reload()
        }

        val zone = ZoneId.systemDefault()
        val selectedViewData = selected?.let { repository.toViewData(it) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1D2549),
                            Color(0xFF6951A8),
                            Color(0xFFA974D6)
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Resumen Semanal",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { reload() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_refress),
                            contentDescription = "Recargar",
                            tint = Color.White
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { tab = "resumen" },
                        label = { Text("Resumen") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (tab == "resumen") Color(0xFFE6EEF8) else Color.White.copy(alpha = 0.75f)
                        )
                    )
                    AssistChip(
                        onClick = { tab = "historial" },
                        label = { Text("Historial") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (tab == "historial") Color(0xFFE6EEF8) else Color.White.copy(alpha = 0.75f)
                        )
                    )
                }

                if (tab == "resumen") {
                    WeeklySummaryTab(
                        data = selectedViewData,
                        summaries = summaries,
                        onSelect = { selected = it },
                        onMoveSection = { key, moveUp ->
                            dbExecutor.execute {
                                repository.moveSection(key, moveUp)
                                activity?.runOnUiThread { sectionOrderTick++ }
                            }
                        }
                    )
                } else {
                    WeeklyHistoryTab(
                        monthCursor = monthCursor,
                        summaries = summaries,
                        zoneId = zone,
                        onPrevMonth = { monthCursor = monthCursor.minusMonths(1) },
                        onNextMonth = { monthCursor = monthCursor.plusMonths(1) },
                        onSelectSummary = {
                            selected = it
                            tab = "resumen"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklySummaryTab(
    data: WeeklySummaryViewData?,
    summaries: List<WeeklySummaryEntity>,
    onSelect: (WeeklySummaryEntity) -> Unit,
    onMoveSection: (String, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            val nextText = "Los resúmenes se generan automáticamente cada lunes a las 03:00"
            Text(
                text = nextText,
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                summaries.take(6).forEach { summary ->
                    AssistChip(
                        onClick = { onSelect(summary) },
                        label = {
                            val start = Instant.ofEpochMilli(summary.weekStartMillis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            Text(start.toString())
                        },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color.White)
                    )
                }
            }
        }

        if (data == null) {
            item {
                Text(
                    text = "Todavía no hay semanas cerradas con datos para mostrar.",
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
            item {
                WeeklySummaryHeader(data.entity)
            }
            item {
                WeeklySummaryCharts(data.entity)
            }

            items(data.sections, key = { it.key }) { section ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = section.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onMoveSection(section.key, true) }) {
                            Icon(painter = painterResource(id = R.drawable.ic_arriba), contentDescription = "Subir")
                        }
                        IconButton(onClick = { onMoveSection(section.key, false) }) {
                            Icon(painter = painterResource(id = R.drawable.ic_abajo), contentDescription = "Bajar")
                        }
                    }

                    section.metrics.forEach { metric ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = metric.first, fontSize = 20.sp)
                            Text(text = metric.second.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklySummaryHeader(entity: WeeklySummaryEntity) {
    val zone = ZoneId.systemDefault()
    val label = WeeklySummaryTime.weekLabel(entity.weekStartMillis, entity.weekEndMillis, zone)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Text(text = "Semana", fontSize = 20.sp, color = Color(0xFF264E77))
        Text(text = label, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WeeklySummaryCharts(entity: WeeklySummaryEntity) {
    val created = entity.notesCreated + entity.journalCreated + entity.goalsCreated + entity.remindersCreated +
        entity.voiceCreated + entity.emotionalAnchorsCreated + entity.personalPhrasesCreated
    val modified = entity.notesModified + entity.journalModified + entity.remindersModified + entity.personalPhrasesModified
    val deleted = entity.notesDeleted + entity.journalDeleted + entity.remindersDeleted + entity.voiceDeleted + entity.personalPhrasesDeleted

    val bars = listOf(
        "Creado" to created,
        "Modificado" to modified,
        "Eliminado" to deleted,
        "Uso" to (entity.conferencesRead + entity.emotionalAnchorsUsed + entity.encyclopediaAccessed + entity.morningRitualsCompleted)
    )

    val maxValue = max(1, bars.maxOf { it.second })

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Text("Visión General", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        bars.forEachIndexed { index, bar ->
            val fraction = bar.second.toFloat() / maxValue.toFloat()
            val color = when (index) {
                0 -> Color(0xFF0A7C2F)
                1 -> Color(0xFF2563EB)
                2 -> Color(0xFFB91C1C)
                else -> Color(0xFF7C3AED)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = bar.first, fontSize = 18.sp, modifier = Modifier.size(92.dp, 22.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp)
                        .background(Color(0xFFE5E7EB), RoundedCornerShape(9.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(16.dp)
                            .background(color, RoundedCornerShape(9.dp))
                    )
                }
                Text(text = bar.second.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            }
        }

        val total = created + modified + deleted
        val createAngle = if (total > 0) 360f * created / total else 0f
        val modifyAngle = if (total > 0) 360f * modified / total else 0f

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Canvas(modifier = Modifier.size(150.dp)) {
                drawArc(
                    color = Color(0xFF0A7C2F),
                    startAngle = -90f,
                    sweepAngle = createAngle,
                    useCenter = false,
                    style = Stroke(width = 28f, cap = StrokeCap.Butt),
                    topLeft = Offset.Zero,
                    size = Size(size.width, size.height)
                )
                drawArc(
                    color = Color(0xFF2563EB),
                    startAngle = -90f + createAngle,
                    sweepAngle = modifyAngle,
                    useCenter = false,
                    style = Stroke(width = 28f, cap = StrokeCap.Butt),
                    topLeft = Offset.Zero,
                    size = Size(size.width, size.height)
                )
                drawArc(
                    color = Color(0xFFB91C1C),
                    startAngle = -90f + createAngle + modifyAngle,
                    sweepAngle = 360f - createAngle - modifyAngle,
                    useCenter = false,
                    style = Stroke(width = 28f, cap = StrokeCap.Butt),
                    topLeft = Offset.Zero,
                    size = Size(size.width, size.height)
                )
            }
        }
    }
}

@Composable
private fun WeeklyHistoryTab(
    monthCursor: LocalDate,
    summaries: List<WeeklySummaryEntity>,
    zoneId: ZoneId,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectSummary: (WeeklySummaryEntity) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    val weekStarts = WeeklySummaryRepository.weekStartsForMonth(monthCursor.withDayOfMonth(1), zoneId)
    val summaryByWeek = summaries.associateBy { it.weekStartMillis }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onPrevMonth) { Text("◀") }
                Text(
                    text = monthCursor.format(formatter),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNextMonth) { Text("▶") }
            }
        }

        item {
            Text(
                text = "Calendario semanal del mes",
                color = Color.White,
                fontSize = 19.sp,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }

        items(weekStarts) { weekStart ->
            val summary = summaryByWeek[weekStart]
            val monday = Instant.ofEpochMilli(weekStart).atZone(zoneId).toLocalDate()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(16.dp))
                    .padding(10.dp)
                    .border(1.dp, if (summary != null) Color(0xFF3B82F6) else Color.Transparent, RoundedCornerShape(16.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Semana de $monday", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (summary != null) "Resumen disponible" else "Sin resumen generado",
                        color = if (summary != null) Color(0xFF0A7C2F) else Color(0xFF6B7280),
                        fontSize = 17.sp
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        for (d in 0..6) {
                            val day = monday.plusDays(d.toLong())
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        if (d == 0) Color(0xFFE8EEF9) else Color(0xFFF3F4F6),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(day.dayOfMonth.toString(), fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (summary != null) {
                    AssistChip(
                        onClick = { onSelectSummary(summary) },
                        label = { Text("Abrir") },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFDDEBFF))
                    )
                }
            }
        }
    }
}
