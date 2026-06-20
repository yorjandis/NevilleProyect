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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ypg.neville.R
import com.ypg.neville.feature.cardiocoherence.domain.MeditationSessionRecord
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
                    text = "Estadísticas de Coherencia",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onRefresh) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_refress),
                        contentDescription = "Recargar",
                        tint = Color.White
                    )
                }
                TextButton(onClick = onClose) {
                    Text("Cerrar", color = Color.White)
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
            title = { Text(info.title) },
            text = { Text(info.message) },
            confirmButton = {
                TextButton(onClick = { infoItem = null }) { Text("Cerrar") }
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
    data class EmotionCount(val label: String, val emoji: String, val count: Int)
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
                .map { EmotionCount(it.key.label, it.key.emoji, it.value) }
                .sortedByDescending { it.count }
                .take(5)
        }

        private fun calculateWeekdayCounts(records: List<MeditationSessionRecord>): List<WeekdayCount> {
            val labels = listOf("D", "L", "M", "X", "J", "V", "S")
            val counts = IntArray(7)
            val cal = Calendar.getInstance()
            records.forEach { record ->
                cal.timeInMillis = startOfDay(record.dateEpochMillis)
                val weekday = cal.get(Calendar.DAY_OF_WEEK)
                counts[weekday - 1]++
            }
            return labels.indices.map { idx -> WeekdayCount(labels[idx], counts[idx]) }
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
    val title: String,
    val message: String
)

@Composable
private fun CardioCoherenceHeadlineCards(
    stats: CardioCoherenceStatsSnapshot,
    onInfo: (CardioCoherenceInfoItem) -> Unit
) {
    val formatter = remember { DecimalFormat("0.0") }
    Column(modifier = Modifier.padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Tu práctica de coherencia",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CardioCoherenceMetricCard(
                title = "Sesiones",
                value = stats.totalSessions.toString(),
                subtitle = "total",
                modifier = Modifier.weight(1f),
                onInfo = {
                    onInfo(CardioCoherenceInfoItem("Sesiones Totales", "Cantidad total de sesiones de coherencia registradas."))
                }
            )
            CardioCoherenceMetricCard(
                title = "Tiempo",
                value = stats.totalMinutes.toString(),
                subtitle = "minutos",
                modifier = Modifier.weight(1f),
                onInfo = {
                    onInfo(CardioCoherenceInfoItem("Tiempo Total", "Minutos acumulados de práctica registrados en Coherencia Cardio-Cerebral."))
                }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CardioCoherenceMetricCard(
                title = "Racha actual",
                value = stats.currentStreak.toString(),
                subtitle = "días",
                modifier = Modifier.weight(1f),
                onInfo = {
                    onInfo(CardioCoherenceInfoItem("Racha Actual", "Días consecutivos recientes con al menos una sesión de coherencia."))
                }
            )
            CardioCoherenceMetricCard(
                title = "Mejor racha",
                value = stats.longestStreak.toString(),
                subtitle = "días",
                modifier = Modifier.weight(1f),
                onInfo = {
                    onInfo(CardioCoherenceInfoItem("Mejor Racha", "Mayor número histórico de días consecutivos con sesiones registradas."))
                }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CardioCoherenceMetricCard(
                title = "Cambio",
                value = signedDecimal(stats.averageScoreDelta, formatter),
                subtitle = "promedio",
                modifier = Modifier.weight(1f),
                onInfo = {
                    onInfo(CardioCoherenceInfoItem("Cambio Promedio", "Promedio de diferencia entre la puntuación posterior y la puntuación inicial de cada sesión."))
                }
            )
            CardioCoherenceMetricCard(
                title = "Promedio",
                value = formatter.format(stats.weeklyAverage),
                subtitle = "por semana",
                modifier = Modifier.weight(1f),
                onInfo = {
                    onInfo(CardioCoherenceInfoItem("Promedio Semanal", "Promedio de sesiones creadas por semana durante el historial disponible."))
                }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CardioCoherenceMetricCard(
                title = "Promedio",
                value = formatter.format(stats.monthlyAverage),
                subtitle = "por mes",
                modifier = Modifier.weight(1f),
                onInfo = {
                    onInfo(CardioCoherenceInfoItem("Promedio Mensual", "Promedio de sesiones creadas por mes considerando todo el historial de coherencia."))
                }
            )
            CardioCoherenceMetricCard(
                title = "Coherencia",
                value = scoreText(stats.averageAfterScore),
                subtitle = "final media",
                modifier = Modifier.weight(1f),
                onInfo = {
                    onInfo(CardioCoherenceInfoItem("Coherencia Final Media", "Promedio de calma/coherencia percibida después de las sesiones."))
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
                contentDescription = "Info",
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
            Text("Frecuencia semanal", color = Color.White, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.weight(1f))
            CardioCoherenceInfoIcon {
                onInfo(CardioCoherenceInfoItem("Frecuencia Semanal", "Cuenta cuántas sesiones se registraron en cada día de la semana."))
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
            Text("Calidad percibida", color = Color.White, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.weight(1f))
            CardioCoherenceInfoIcon {
                onInfo(CardioCoherenceInfoItem("Calidad Percibida", "Promedia la calma/coherencia final, claridad mental y conexión con el corazón."))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            CardioCoherenceRingMetric(
                progress = stats.averageAfterScore / 10.0,
                color = Color(0xFF80CBC4),
                emoji = "😌",
                title = "Coherencia",
                value = scoreText(stats.averageAfterScore),
                modifier = Modifier.weight(1f),
                onInfo = {
                    onInfo(CardioCoherenceInfoItem("Coherencia Percibida", "Promedio de calma/coherencia valorada al terminar la sesión."))
                }
            )
            CardioCoherenceRingMetric(
                progress = stats.averageMentalClarity / 10.0,
                color = Color(0xFF4DD0E1),
                emoji = "✨",
                title = "Claridad",
                value = scoreText(stats.averageMentalClarity),
                modifier = Modifier.weight(1f),
                onInfo = {
                    onInfo(CardioCoherenceInfoItem("Claridad Mental", "Promedio de claridad mental indicada después de cada sesión."))
                }
            )
            CardioCoherenceRingMetric(
                progress = stats.averageHeartConnection / 10.0,
                color = Color(0xFFCE93D8),
                emoji = "💗",
                title = "Corazón",
                value = scoreText(stats.averageHeartConnection),
                modifier = Modifier.weight(1f),
                onInfo = {
                    onInfo(CardioCoherenceInfoItem("Conexión con el Corazón", "Promedio de conexión corporal y emocional percibida al finalizar."))
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
            Text("Emociones predominantes", color = Color.White, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.weight(1f))
            CardioCoherenceInfoIcon {
                onInfo(CardioCoherenceInfoItem("Emociones Predominantes", "Ordena las emociones elegidas al final de la sesión y muestra las 3 más frecuentes."))
            }
        }

        if (stats.topEmotions.isEmpty()) {
            Text("Aún no hay datos emocionales suficientes.", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
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
                        title = emotion.label,
                        value = emotion.count.toString(),
                        modifier = Modifier.weight(1f),
                        onInfo = {
                            onInfo(CardioCoherenceInfoItem("Anillo de Emoción", "Cada anillo representa el porcentaje de una emoción sobre el total de sesiones registradas."))
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
            Text("Actividad de los últimos $selectedDays días", color = Color.White, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.weight(1f))
            CardioCoherenceInfoIcon {
                onInfo(CardioCoherenceInfoItem("Actividad de los últimos $selectedDays Días", "Cada punto representa un día. Puedes cambiar el rango para ver la constancia de tus sesiones."))
            }
        }

        Text("Visualización por puntos al estilo Fitness", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            dayOptions.forEach { days ->
                val selected = selectedDays == days
                Text(
                    text = "${days}d",
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
        contentDescription = "Info",
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
