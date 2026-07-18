package com.ypg.neville.feature.cardiocoherence.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import com.ypg.neville.R
import com.ypg.neville.feature.cardiocoherence.domain.MeditationSessionRecord
import com.ypg.neville.feature.cardiocoherence.domain.PostSessionEmotion
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun CardioCoherenceStatsScreen(
    records: List<MeditationSessionRecord>,
    onClose: () -> Unit,
    onRefresh: () -> Unit
) {
    val stats = remember(records) { CardioCoherenceStatsSnapshot(records) }
    var infoItem by remember { mutableStateOf<CardioCoherenceInfoItem?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(red = 0.06f, green = 0.13f, blue = 0.26f),
                        Color(red = 0.11f, green = 0.26f, blue = 0.44f),
                        Color(red = 0.95f, green = 0.46f, blue = 0.23f)
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
                    text = stringResource(R.string.coherence_stats_title),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onRefresh) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_refress),
                        contentDescription = stringResource(R.string.common_refresh),
                        tint = Color.White
                    )
                }
                TextButton(onClick = onClose) {
                    Text(stringResource(R.string.common_close), color = Color.White)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    CardioCoherenceHeadlineCards(stats = stats, onInfo = { infoItem = it })
                }
                item {
                    CardioCoherenceWeeklyBarChart(stats = stats, onInfo = { infoItem = it })
                }
                item {
                    CardioCoherenceRingsSection(stats = stats, onInfo = { infoItem = it })
                }
                item {
                    CardioCoherenceEmotionSection(stats = stats, onInfo = { infoItem = it })
                }
                item {
                    CardioCoherenceDotTrendSection(stats = stats, onInfo = { infoItem = it })
                }
            }
        }
    }

    infoItem?.let { info ->
        AlertDialog(
            onDismissRequest = { infoItem = null },
            title = { Text(stringResource(info.titleRes, *info.titleArgs.toTypedArray())) },
            text = { Text(stringResource(info.messageRes)) },
            confirmButton = {
                TextButton(onClick = { infoItem = null }) { Text(stringResource(R.string.common_close)) }
            }
        )
    }
}

private data class CardioCoherenceStatsSnapshot(
    val totalSessions: Int,
    val totalMinutes: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val averageScoreDelta: Double,
    val averageAfterScore: Double,
    val averageMentalClarity: Double,
    val averageHeartConnection: Double,
    val weeklyAverage: Double,
    val monthlyAverage: Double,
    val weekdayCounts: List<WeekdayCount>,
    val topEmotions: List<EmotionCount>,
    private val dailyCounts: Map<Long, Int>
) {
    data class WeekdayCount(val dayLabel: String, val count: Int)
    data class EmotionCount(val emotion: PostSessionEmotion, val emoji: String, val count: Int)
    data class DayPoint(val dayStartMillis: Long, val count: Int)

    constructor(records: List<MeditationSessionRecord>) : this(
        totalSessions = records.size,
        totalMinutes = records.sumOf { it.durationMinutes },
        currentStreak = calculateCurrentStreak(records),
        longestStreak = calculateLongestStreak(records),
        averageScoreDelta = records.averageOf { it.afterScore - it.beforeScore },
        averageAfterScore = records.averageOf { it.afterScore },
        averageMentalClarity = records.averageOf { it.mentalClarityScore },
        averageHeartConnection = records.averageOf { it.heartConnectionScore },
        weeklyAverage = calculateWeeklyAverage(records),
        monthlyAverage = calculateMonthlyAverage(records),
        weekdayCounts = calculateWeekdayCounts(records),
        topEmotions = calculateTopEmotions(records),
        dailyCounts = records.groupingBy { startOfDay(it.dateEpochMillis) }.eachCount()
    )

    fun dayPoints(days: Int): List<DayPoint> {
        val safeDays = max(1, days)
        val cal = Calendar.getInstance()
        val result = mutableListOf<DayPoint>()
        for (offset in (safeDays - 1) downTo 0) {
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.DAY_OF_YEAR, -offset)
            val day = startOfDay(cal.timeInMillis)
            result += DayPoint(dayStartMillis = day, count = dailyCounts[day] ?: 0)
        }
        return result
    }

    companion object {
        private fun calculateTopEmotions(records: List<MeditationSessionRecord>): List<EmotionCount> {
            return records
                .groupingBy { it.predominantEmotion }
                .eachCount()
                .map { EmotionCount(it.key, it.key.emoji, it.value) }
                .sortedByDescending { it.count }
                .take(5)
        }

        private fun calculateWeekdayCounts(records: List<MeditationSessionRecord>): List<WeekdayCount> {
            val counts = IntArray(7)
            val cal = Calendar.getInstance()
            records.forEach { record ->
                cal.timeInMillis = startOfDay(record.dateEpochMillis)
                val weekday = cal.get(Calendar.DAY_OF_WEEK)
                counts[weekday - 1]++
            }
            val labelCalendar = Calendar.getInstance()
            return counts.indices.map { idx ->
                labelCalendar.set(Calendar.DAY_OF_WEEK, idx + 1)
                WeekdayCount(
                    SimpleDateFormat("EEEEE", Locale.getDefault()).format(labelCalendar.time).uppercase(Locale.getDefault()),
                    counts[idx]
                )
            }
        }

        private fun calculateWeeklyAverage(records: List<MeditationSessionRecord>): Double {
            if (records.isEmpty()) return 0.0
            val days = records.map { startOfDay(it.dateEpochMillis) }
            val min = days.min()
            val max = days.max()
            val dayRange = max(1, ((max - min) / CARDIO_STATS_DAY_MS).toInt())
            return records.size / (dayRange / 7.0)
        }

        private fun calculateMonthlyAverage(records: List<MeditationSessionRecord>): Double {
            if (records.isEmpty()) return 0.0
            val days = records.map { startOfDay(it.dateEpochMillis) }
            val min = days.min()
            val max = days.max()
            val monthRange = max(1, ((max - min) / (30L * CARDIO_STATS_DAY_MS)).toInt())
            return records.size / monthRange.toDouble()
        }

        private fun calculateCurrentStreak(records: List<MeditationSessionRecord>): Int {
            if (records.isEmpty()) return 0
            val set = records.map { startOfDay(it.dateEpochMillis) }.toSet()
            var streak = 0
            var cursor = startOfDay(System.currentTimeMillis())

            if (!set.contains(cursor) && set.contains(cursor - CARDIO_STATS_DAY_MS)) {
                cursor -= CARDIO_STATS_DAY_MS
            }

            while (set.contains(cursor)) {
                streak++
                cursor -= CARDIO_STATS_DAY_MS
            }
            return streak
        }

        private fun calculateLongestStreak(records: List<MeditationSessionRecord>): Int {
            if (records.isEmpty()) return 0
            val sorted = records.map { startOfDay(it.dateEpochMillis) }.distinct().sorted()
            var best = 1
            var current = 1
            for (i in 1 until sorted.size) {
                if (sorted[i] - sorted[i - 1] == CARDIO_STATS_DAY_MS) {
                    current++
                    best = max(best, current)
                } else {
                    current = 1
                }
            }
            return best
        }
    }
}

private data class CardioCoherenceInfoItem(
    @StringRes val titleRes: Int,
    @StringRes val messageRes: Int,
    val titleArgs: List<Any> = emptyList()
)

@Composable
private fun CardioCoherenceHeadlineCards(
    stats: CardioCoherenceStatsSnapshot,
    onInfo: (CardioCoherenceInfoItem) -> Unit
) {
    val formatter = remember { DecimalFormat("0.0") }
    Column(modifier = Modifier.padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.coherence_your_practice),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CardioCoherenceMetricCard(
                title = stringResource(R.string.coherence_sessions),
                value = stats.totalSessions.toString(),
                subtitle = stringResource(R.string.coherence_total),
                modifier = Modifier.weight(1f),
                onInfo = {
                    onInfo(CardioCoherenceInfoItem(R.string.coherence_info_total_sessions_title, R.string.coherence_info_total_sessions))
                }
            )
            CardioCoherenceMetricCard(
                title = stringResource(R.string.coherence_time),
                value = stats.totalMinutes.toString(),
                subtitle = stringResource(R.string.coherence_minutes),
                modifier = Modifier.weight(1f),
                onInfo = {
                    onInfo(CardioCoherenceInfoItem(R.string.coherence_info_total_time_title, R.string.coherence_info_total_time))
                }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CardioCoherenceMetricCard(
                title = stringResource(R.string.coherence_current_streak),
                value = stats.currentStreak.toString(),
                subtitle = stringResource(R.string.coherence_days),
                modifier = Modifier.weight(1f),
                onInfo = {
                    onInfo(CardioCoherenceInfoItem(R.string.coherence_current_streak, R.string.coherence_info_current_streak))
                }
            )
            CardioCoherenceMetricCard(
                title = stringResource(R.string.coherence_best_streak),
                value = stats.longestStreak.toString(),
                subtitle = stringResource(R.string.coherence_days),
                modifier = Modifier.weight(1f),
                onInfo = {
                    onInfo(CardioCoherenceInfoItem(R.string.coherence_best_streak, R.string.coherence_info_best_streak))
                }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CardioCoherenceMetricCard(
                title = stringResource(R.string.coherence_change),
                value = signedDecimal(stats.averageScoreDelta, formatter),
                subtitle = stringResource(R.string.coherence_average),
                modifier = Modifier.weight(1f),
                onInfo = {
                    onInfo(CardioCoherenceInfoItem(R.string.coherence_change, R.string.coherence_info_average_change))
                }
            )
            CardioCoherenceMetricCard(
                title = stringResource(R.string.coherence_average),
                value = formatter.format(stats.weeklyAverage),
                subtitle = stringResource(R.string.coherence_per_week),
                modifier = Modifier.weight(1f),
                onInfo = {
                    onInfo(CardioCoherenceInfoItem(R.string.coherence_card_weekly_average_title, R.string.coherence_info_weekly_average))
                }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CardioCoherenceMetricCard(
                title = stringResource(R.string.coherence_average),
                value = formatter.format(stats.monthlyAverage),
                subtitle = stringResource(R.string.coherence_per_month),
                modifier = Modifier.weight(1f),
                onInfo = {
                    onInfo(CardioCoherenceInfoItem(R.string.coherence_card_monthly_average_title, R.string.coherence_info_monthly_average))
                }
            )
            CardioCoherenceMetricCard(
                title = stringResource(R.string.coherence_label),
                value = scoreText(stats.averageAfterScore),
                subtitle = stringResource(R.string.coherence_final_average),
                modifier = Modifier.weight(1f),
                onInfo = {
                    onInfo(CardioCoherenceInfoItem(R.string.coherence_final_average, R.string.coherence_info_final_average))
                }
            )
        }
    }
}

@Composable
private fun CardioCoherenceMetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onInfo: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = title, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            Box(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(id = R.drawable.ic_help),
                contentDescription = stringResource(R.string.coherence_info),
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(16.dp).clickable(onClick = onInfo)
            )
        }
        Text(text = value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(text = subtitle, color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
    }
}

@Composable
private fun CardioCoherenceWeeklyBarChart(
    stats: CardioCoherenceStatsSnapshot,
    onInfo: (CardioCoherenceInfoItem) -> Unit
) {
    val maxCount = max(1, stats.weekdayCounts.maxOfOrNull { it.count } ?: 1)
    CardioCoherenceStatsPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.coherence_weekly_frequency), color = Color.White, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.weight(1f))
            CardioCoherenceInfoIcon {
                onInfo(CardioCoherenceInfoItem(R.string.coherence_weekly_frequency, R.string.coherence_info_weekly_frequency))
            }
        }

        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            stats.weekdayCounts.forEach { item ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Box(contentAlignment = Alignment.BottomCenter) {
                        Box(
                            modifier = Modifier
                                .width(30.dp)
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                        )
                        Box(
                            modifier = Modifier
                                .width(30.dp)
                                .height((item.count.toFloat() / maxCount.toFloat() * 120f).dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.verticalGradient(colors = listOf(Color(0xFFB3E5FC), Color(0xFF5B63D6))))
                        )
                    }
                    Text(item.dayLabel, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                    Text(item.count.toString(), color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun CardioCoherenceRingsSection(
    stats: CardioCoherenceStatsSnapshot,
    onInfo: (CardioCoherenceInfoItem) -> Unit
) {
    CardioCoherenceStatsPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.coherence_perceived_quality), color = Color.White, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.weight(1f))
            CardioCoherenceInfoIcon {
                onInfo(CardioCoherenceInfoItem(R.string.coherence_perceived_quality, R.string.coherence_info_perceived_quality))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            CardioCoherenceRingMetric(
                progress = stats.averageAfterScore / 10.0,
                color = Color(0xFF80CBC4),
                emoji = "😌",
                title = stringResource(R.string.coherence_label),
                value = scoreText(stats.averageAfterScore),
                modifier = Modifier.weight(1f),
                onInfo = {
                    onInfo(CardioCoherenceInfoItem(R.string.coherence_label, R.string.coherence_info_perceived_coherence))
                }
            )
            CardioCoherenceRingMetric(
                progress = stats.averageMentalClarity / 10.0,
                color = Color(0xFF4DD0E1),
                emoji = "✨",
                title = stringResource(R.string.coherence_clarity),
                value = scoreText(stats.averageMentalClarity),
                modifier = Modifier.weight(1f),
                onInfo = {
                    onInfo(CardioCoherenceInfoItem(R.string.coherence_mental_clarity, R.string.coherence_info_mental_clarity))
                }
            )
            CardioCoherenceRingMetric(
                progress = stats.averageHeartConnection / 10.0,
                color = Color(0xFFCE93D8),
                emoji = "💗",
                title = stringResource(R.string.coherence_heart),
                value = scoreText(stats.averageHeartConnection),
                modifier = Modifier.weight(1f),
                onInfo = {
                    onInfo(CardioCoherenceInfoItem(R.string.coherence_heart_connection, R.string.coherence_info_heart_connection))
                }
            )
        }
    }
}

@Composable
private fun CardioCoherenceEmotionSection(
    stats: CardioCoherenceStatsSnapshot,
    onInfo: (CardioCoherenceInfoItem) -> Unit
) {
    CardioCoherenceStatsPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.coherence_predominant_emotions), color = Color.White, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.weight(1f))
            CardioCoherenceInfoIcon {
                onInfo(CardioCoherenceInfoItem(R.string.coherence_predominant_emotions, R.string.coherence_info_predominant_emotions))
            }
        }

        if (stats.topEmotions.isEmpty()) {
            Text(stringResource(R.string.coherence_not_enough_emotion_data), color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                stats.topEmotions.take(3).forEachIndexed { idx, emotion ->
                    CardioCoherenceRingMetric(
                        progress = emotion.count.toDouble() / max(1, stats.totalSessions).toDouble(),
                        color = when (idx) {
                            0 -> Color(0xFFFFF176)
                            1 -> Color(0xFF80DEEA)
                            else -> Color(0xFFB39DDB)
                        },
                        emoji = emotion.emoji,
                        title = localizedPostSessionEmotionLabel(emotion.emotion),
                        value = emotion.count.toString(),
                        modifier = Modifier.weight(1f),
                        onInfo = {
                            onInfo(CardioCoherenceInfoItem(R.string.coherence_predominant_emotions, R.string.coherence_info_emotion_ring))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CardioCoherenceRingMetric(
    progress: Double,
    color: Color,
    emoji: String,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    onInfo: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            CardioCoherenceInfoIcon(sizeDp = 14, onClick = onInfo)
        }
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(76.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = Color.White.copy(alpha = 0.18f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 10f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = (progress.coerceIn(0.0, 1.0) * 360.0).toFloat(),
                    useCenter = false,
                    style = Stroke(width = 10f, cap = StrokeCap.Round)
                )
            }
            Text(emoji, fontSize = 20.sp)
        }
        Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
        Text(value, color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
    }
}

@Composable
private fun CardioCoherenceDotTrendSection(
    stats: CardioCoherenceStatsSnapshot,
    onInfo: (CardioCoherenceInfoItem) -> Unit
) {
    var selectedDays by remember { mutableStateOf(30) }
    val dayOptions = listOf(7, 15, 30, 45, 60, 90)
    val points = remember(stats, selectedDays) { stats.dayPoints(selectedDays) }
    val dayLabelFormat = remember { SimpleDateFormat("d", Locale.getDefault()) }
    val rows = max(1, (points.size + 6) / 7)
    val gridHeight = (rows * 44 + (rows - 1) * 12).dp

    CardioCoherenceStatsPanel(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.coherence_last_days_activity, selectedDays), color = Color.White, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.weight(1f))
            CardioCoherenceInfoIcon {
                onInfo(CardioCoherenceInfoItem(R.string.coherence_last_days_activity, R.string.coherence_info_activity, listOf(selectedDays)))
            }
        }

        Text(stringResource(R.string.coherence_fitness_dots), color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            dayOptions.forEach { days ->
                val selected = selectedDays == days
                Text(
                    text = stringResource(R.string.coherence_days_short, days),
                    color = if (selected) Color.Black else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) Color.White else Color.White.copy(alpha = 0.16f))
                        .border(1.dp, Color.White.copy(alpha = if (selected) 0f else 0.35f), RoundedCornerShape(20.dp))
                        .clickable { selectedDays = days }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = false
        ) {
            items(points.size) { idx ->
                val point = points[idx]
                val dotSize = when {
                    point.count == 0 -> 10.dp
                    point.count == 1 -> 16.dp
                    point.count in 2..3 -> 20.dp
                    else -> 24.dp
                }
                val dotColor = when {
                    point.count == 0 -> Color.White.copy(alpha = 0.2f)
                    point.count == 1 -> Color(0xFF80CBC4)
                    point.count in 2..3 -> Color(0xFFFFF176)
                    else -> Color(0xFFFFB74D)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(dotSize)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        if (point.count > 0) {
                            Text(
                                point.count.toString(),
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = dayLabelFormat.format(Date(point.dayStartMillis)),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CardioCoherenceStatsPanel(
    modifier: Modifier = Modifier.padding(horizontal = 14.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.18f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
private fun CardioCoherenceInfoIcon(
    sizeDp: Int = 18,
    onClick: () -> Unit
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_help),
        contentDescription = stringResource(R.string.coherence_info),
        tint = Color.White.copy(alpha = 0.9f),
        modifier = Modifier
            .size(sizeDp.dp)
            .clickable(onClick = onClick)
    )
}

private fun List<MeditationSessionRecord>.averageOf(selector: (MeditationSessionRecord) -> Int): Double {
    if (isEmpty()) return 0.0
    return sumOf(selector).toDouble() / size.toDouble()
}

private fun signedDecimal(value: Double, formatter: DecimalFormat): String {
    val formatted = formatter.format(value)
    return if (value > 0.0) "+$formatted" else formatted
}

private fun scoreText(value: Double): String {
    if (value <= 0.0) return "0"
    val rounded = (value * 10.0).roundToInt() / 10.0
    return DecimalFormat("0.0").format(rounded)
}

@Composable
private fun localizedPostSessionEmotionLabel(emotion: PostSessionEmotion): String = stringResource(
    when (emotion) {
        PostSessionEmotion.CALM -> R.string.coherence_emotion_calm
        PostSessionEmotion.GRATITUDE -> R.string.coherence_emotion_gratitude
        PostSessionEmotion.LOVE -> R.string.coherence_emotion_love
        PostSessionEmotion.PEACE -> R.string.coherence_emotion_peace
        PostSessionEmotion.JOY -> R.string.coherence_emotion_joy
        PostSessionEmotion.CLARITY -> R.string.coherence_emotion_clarity
        PostSessionEmotion.HOPE -> R.string.coherence_emotion_hope
        PostSessionEmotion.NEUTRAL -> R.string.coherence_emotion_neutral
    }
)

private fun startOfDay(timeMillis: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = timeMillis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private const val CARDIO_STATS_DAY_MS = 24L * 60 * 60 * 1000
