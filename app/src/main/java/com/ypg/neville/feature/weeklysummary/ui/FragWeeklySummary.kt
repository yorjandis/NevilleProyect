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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.feature.weeklysummary.data.WeeklySummaryEntity
import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryRepository
import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryViewData
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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
                        text = stringResource(R.string.weekly_summary_title),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { reload() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_refress),
                            contentDescription = stringResource(R.string.common_refresh),
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
                        label = { Text(stringResource(R.string.weekly_summary_tab_summary)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (tab == "resumen") Color(0xFFE6EEF8) else Color.White.copy(alpha = 0.75f)
                        )
                    )
                    AssistChip(
                        onClick = { tab = "historial" },
                        label = { Text(stringResource(R.string.weekly_summary_tab_history)) },
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
            Text(
                text = stringResource(R.string.weekly_summary_generation_notice),
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
                            val locale = LocalConfiguration.current.locales[0]
                            Text(start.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)))
                        },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color.White)
                    )
                }
            }
        }

        if (data == null) {
            item {
                Text(
                    text = stringResource(R.string.weekly_summary_empty),
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
                            text = localizedWeeklySectionTitle(section.key, section.title),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onMoveSection(section.key, true) }) {
                            Icon(painter = painterResource(id = R.drawable.ic_arriba), contentDescription = stringResource(R.string.weekly_summary_move_up))
                        }
                        IconButton(onClick = { onMoveSection(section.key, false) }) {
                            Icon(painter = painterResource(id = R.drawable.ic_abajo), contentDescription = stringResource(R.string.weekly_summary_move_down))
                        }
                    }

                    section.metrics.forEachIndexed { index, metric ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = localizedWeeklyMetricLabel(section.key, index, metric.first), fontSize = 20.sp)
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
    val locale = LocalConfiguration.current.locales[0]
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    val start = Instant.ofEpochMilli(entity.weekStartMillis).atZone(zone).toLocalDate()
    val end = Instant.ofEpochMilli(entity.weekEndMillis - 1L).atZone(zone).toLocalDate()
    val label = "${start.format(formatter)} – ${end.format(formatter)}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Text(text = stringResource(R.string.weekly_summary_week), fontSize = 20.sp, color = Color(0xFF264E77))
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
        stringResource(R.string.weekly_summary_created) to created,
        stringResource(R.string.weekly_summary_modified) to modified,
        stringResource(R.string.weekly_summary_deleted) to deleted,
        stringResource(R.string.weekly_summary_usage) to (
            entity.conferencesRead +
                entity.emotionalAnchorsUsed +
                entity.encyclopediaAccessed +
                entity.morningRitualsCompleted +
                entity.eveningRitualsCompleted +
                entity.cardioCoherenceSessions
            )
    )

    val maxValue = max(1, bars.maxOf { it.second })

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Text(stringResource(R.string.weekly_summary_overview), fontSize = 24.sp, fontWeight = FontWeight.Bold)

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
    val locale = LocalConfiguration.current.locales[0]
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", locale)
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
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
                text = stringResource(R.string.weekly_summary_month_calendar),
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
                    Text(stringResource(R.string.weekly_summary_week_of, monday.format(dateFormatter)), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (summary != null) stringResource(R.string.weekly_summary_available) else stringResource(R.string.weekly_summary_unavailable),
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
                        label = { Text(stringResource(R.string.weekly_summary_open)) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFDDEBFF))
                    )
                }
            }
        }
    }
}

@Composable
private fun localizedWeeklySectionTitle(key: String, fallback: String): String = when (key) {
    WeeklySummaryRepository.SECTION_NOTES -> stringResource(R.string.weekly_section_notes)
    WeeklySummaryRepository.SECTION_JOURNAL -> stringResource(R.string.weekly_section_journal)
    WeeklySummaryRepository.SECTION_CONFERENCES -> stringResource(R.string.weekly_section_conferences)
    WeeklySummaryRepository.SECTION_GOALS -> stringResource(R.string.weekly_section_goals)
    WeeklySummaryRepository.SECTION_REMINDERS -> stringResource(R.string.weekly_section_reminders)
    WeeklySummaryRepository.SECTION_VOICE -> stringResource(R.string.weekly_section_voice)
    WeeklySummaryRepository.SECTION_ANCHORS -> stringResource(R.string.weekly_section_anchors)
    WeeklySummaryRepository.SECTION_MORNING -> stringResource(R.string.weekly_section_morning)
    WeeklySummaryRepository.SECTION_EVENING -> stringResource(R.string.weekly_section_evening)
    WeeklySummaryRepository.SECTION_CARDIO_COHERENCE -> stringResource(R.string.weekly_section_cardio_coherence)
    WeeklySummaryRepository.SECTION_PHRASES -> stringResource(R.string.weekly_section_phrases)
    WeeklySummaryRepository.SECTION_ENCYCLOPEDIA -> stringResource(R.string.weekly_section_encyclopedia)
    else -> fallback
}

@Composable
private fun localizedWeeklyMetricLabel(sectionKey: String, index: Int, fallback: String): String {
    val resourceId = when (sectionKey) {
        WeeklySummaryRepository.SECTION_NOTES,
        WeeklySummaryRepository.SECTION_JOURNAL,
        WeeklySummaryRepository.SECTION_PHRASES -> listOf(
            R.string.weekly_metric_created_feminine,
            R.string.weekly_metric_modified_feminine,
            R.string.weekly_metric_deleted_feminine
        ).getOrNull(index)
        WeeklySummaryRepository.SECTION_CONFERENCES -> R.string.weekly_metric_read
        WeeklySummaryRepository.SECTION_GOALS -> listOf(
            R.string.weekly_metric_created_feminine,
            R.string.weekly_metric_completed_feminine,
            R.string.weekly_metric_in_progress
        ).getOrNull(index)
        WeeklySummaryRepository.SECTION_REMINDERS -> listOf(
            R.string.weekly_metric_created_masculine,
            R.string.weekly_metric_modified_masculine,
            R.string.weekly_metric_deleted_masculine
        ).getOrNull(index)
        WeeklySummaryRepository.SECTION_VOICE -> listOf(
            R.string.weekly_metric_created_feminine,
            R.string.weekly_metric_deleted_feminine
        ).getOrNull(index)
        WeeklySummaryRepository.SECTION_ANCHORS -> listOf(
            R.string.weekly_metric_created_feminine,
            R.string.weekly_metric_used_feminine
        ).getOrNull(index)
        WeeklySummaryRepository.SECTION_MORNING -> R.string.weekly_metric_completed_masculine
        WeeklySummaryRepository.SECTION_EVENING -> listOf(
            R.string.weekly_metric_closures_completed,
            R.string.weekly_metric_ritual_cycles,
            R.string.weekly_metric_average_energy,
            R.string.weekly_metric_average_coherence,
            R.string.weekly_metric_presence_returns,
            R.string.weekly_metric_goal_units
        ).getOrNull(index)
        WeeklySummaryRepository.SECTION_CARDIO_COHERENCE -> listOf(
            R.string.weekly_metric_sessions,
            R.string.weekly_metric_minutes,
            R.string.weekly_metric_net_improvement
        ).getOrNull(index)
        WeeklySummaryRepository.SECTION_ENCYCLOPEDIA -> R.string.weekly_metric_articles_accessed
        else -> null
    }
    return resourceId?.let { stringResource(it) } ?: fallback
}
