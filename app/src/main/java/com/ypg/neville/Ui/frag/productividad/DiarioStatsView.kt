package com.ypg.neville.ui.frag

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.ypg.neville.model.db.room.DiarioEntity
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

@Composable
fun DiarioStatsScreen(
    entries: List<DiarioEntity>,
    onClose: () -> Unit,
    onRefresh: () -> Unit
) {
    val stats = remember(entries) { DiarioStatsSnapshot(entries) }
    var infoItem by remember { mutableStateOf<DiarioInfoItem?>(null) }

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
                    text = "Estadísticas del Diario",
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
                    DiarioHeadlineCards(stats = stats, onInfo = { infoItem = it })
                }
                item {
                    DiarioWeeklyBarChart(stats = stats, onInfo = { infoItem = it })
                }
                item {
                    DiarioMoodRingsSection(stats = stats, onInfo = { infoItem = it })
                }
                item {
                    DiarioDotTrendSection(stats = stats, onInfo = { infoItem = it })
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

private data class DiarioStatsSnapshot(
    val totalEntries: Int,
    val favoritesCount: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val weeklyAverage: Double,
    val monthlyAverage: Double,
    val weekdayCounts: List<WeekdayCount>,
    val topEmotions: List<EmotionCount>,
    private val dailyCounts: Map<Long, Int>
) {
    data class WeekdayCount(val dayLabel: String, val count: Int)
    data class EmotionCount(val emotion: String, val count: Int) {
        val emoji: String
            get() = emotionToEmoji(emotion)
        val name: String
            get() = emotion.replaceFirstChar { it.uppercase() }
    }

    data class DayPoint(val dayStartMillis: Long, val count: Int)

    constructor(entries: List<DiarioEntity>) : this(
        totalEntries = entries.size,
        favoritesCount = entries.count { it.isFav },
        currentStreak = calculateCurrentStreak(entries),
        longestStreak = calculateLongestStreak(entries),
        weeklyAverage = calculateWeeklyAverage(entries),
        monthlyAverage = calculateMonthlyAverage(entries),
        weekdayCounts = calculateWeekdayCounts(entries),
        topEmotions = calculateTopEmotions(entries),
        dailyCounts = entries.groupingBy { startOfDay(it.fecha) }.eachCount()
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
        private fun calculateTopEmotions(entries: List<DiarioEntity>): List<EmotionCount> {
            return entries
                .groupingBy { normalizeEmotion(it.emocion) }
                .eachCount()
                .map { EmotionCount(it.key, it.value) }
                .sortedByDescending { it.count }
                .take(5)
        }

        private fun calculateWeekdayCounts(entries: List<DiarioEntity>): List<WeekdayCount> {
            val labels = listOf("D", "L", "M", "X", "J", "V", "S")
            val counts = IntArray(7)
            val cal = Calendar.getInstance()
            entries.forEach { e ->
                cal.timeInMillis = startOfDay(e.fecha)
                val weekday = cal.get(Calendar.DAY_OF_WEEK) // 1..7
                counts[weekday - 1]++
            }
            return labels.indices.map { idx -> WeekdayCount(labels[idx], counts[idx]) }
        }

        private fun calculateWeeklyAverage(entries: List<DiarioEntity>): Double {
            if (entries.isEmpty()) return 0.0
            val days = entries.map { startOfDay(it.fecha) }
            val min = days.min()
            val max = days.max()
            val dayRange = max(1, (((max - min) / DAY_MS).toInt()))
            return entries.size / (dayRange / 7.0)
        }

        private fun calculateMonthlyAverage(entries: List<DiarioEntity>): Double {
            if (entries.isEmpty()) return 0.0
            val days = entries.map { startOfDay(it.fecha) }
            val min = days.min()
            val max = days.max()
            val monthRange = max(1, (((max - min) / (30L * DAY_MS)).toInt()))
            return entries.size / monthRange.toDouble()
        }

        private fun calculateCurrentStreak(entries: List<DiarioEntity>): Int {
            if (entries.isEmpty()) return 0
            val set = entries.map { startOfDay(it.fecha) }.toSet()
            var streak = 0
            var cursor = startOfDay(System.currentTimeMillis())

            if (!set.contains(cursor) && set.contains(cursor - DAY_MS)) {
                cursor -= DAY_MS
            }

            while (set.contains(cursor)) {
                streak++
                cursor -= DAY_MS
            }
            return streak
        }

        private fun calculateLongestStreak(entries: List<DiarioEntity>): Int {
            if (entries.isEmpty()) return 0
            val sorted = entries.map { startOfDay(it.fecha) }.distinct().sorted()
            var best = 1
            var current = 1
            for (i in 1 until sorted.size) {
                if (sorted[i] - sorted[i - 1] == DAY_MS) {
                    current++
                    best = max(best, current)
                } else {
                    current = 1
                }
            }
            return best
        }

        private fun normalizeEmotion(raw: String?): String {
            val value = raw?.trim()?.lowercase(Locale.getDefault()).orEmpty()
            return if (value.isBlank()) "neutral" else value
        }

        private fun emotionToEmoji(raw: String): String {
            return when (raw.lowercase(Locale.getDefault())) {
                "feliz" -> "😊"
                "triste" -> "🥺"
                "enfadado" -> "😤"
                "desanimado" -> "😔"
                "sorpresa" -> "😮"
                "distraido" -> "🙄"
                "enamorado" -> "🥰"
                "enfermo" -> "🤒"
                "pensativo" -> "🤔"
                "festivo" -> "🥳"
                else -> "🙂"
            }
        }
    }
}

private const val DAY_MS = 24L * 60 * 60 * 1000

private data class DiarioInfoItem(
    val title: String,
    val message: String
)

@Composable
private fun DiarioHeadlineCards(stats: DiarioStatsSnapshot, onInfo: (DiarioInfoItem) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Tu ritmo de escritura",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DiarioMetricCard(
                title = "Entradas",
                value = stats.totalEntries.toString(),
                subtitle = "Total",
                modifier = Modifier.weight(1f),
                onInfo = {
                    onInfo(DiarioInfoItem("Entradas Totales", "Cantidad total de entradas registradas en el Diario desde el inicio."))
                }
            )
            DiarioMetricCard(
                title = "Favoritas",
                value = stats.favoritesCount.toString(),
                subtitle = "Guardadas",
                modifier = Modifier.weight(1f),
                onInfo = {
                    onInfo(DiarioInfoItem("Entradas Favoritas", "Número de entradas marcadas como favoritas para acceso rápido."))
                }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DiarioMetricCard(
                title = "Racha actual",
                value = stats.currentStreak.toString(),
                subtitle = "días",
                modifier = Modifier.weight(1f),
                onInfo = {
                    onInfo(DiarioInfoItem("Racha Actual", "Días consecutivos recientes con al menos una entrada creada por día."))
                }
            )
            DiarioMetricCard(
                title = "Mejor racha",
                value = stats.longestStreak.toString(),
                subtitle = "días",
                modifier = Modifier.weight(1f),
                onInfo = {
                    onInfo(DiarioInfoItem("Mejor Racha", "Mayor número histórico de días consecutivos con actividad en el Diario."))
                }
            )
        }

        val formatter = remember { DecimalFormat("0.0") }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DiarioMetricCard(
                title = "Promedio",
                value = formatter.format(stats.weeklyAverage),
                subtitle = "por semana",
                modifier = Modifier.weight(1f),
                onInfo = {
                    onInfo(DiarioInfoItem("Promedio Semanal", "Promedio de entradas creadas por semana durante el período de datos disponible."))
                }
            )
            DiarioMetricCard(
                title = "Promedio",
                value = formatter.format(stats.monthlyAverage),
                subtitle = "por mes",
                modifier = Modifier.weight(1f),
                onInfo = {
                    onInfo(DiarioInfoItem("Promedio Mensual", "Promedio de entradas creadas por mes considerando todo el historial del Diario."))
                }
            )
        }
    }
}

@Composable
private fun DiarioMetricCard(
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
private fun DiarioWeeklyBarChart(stats: DiarioStatsSnapshot, onInfo: (DiarioInfoItem) -> Unit) {
    val maxCount = max(1, stats.weekdayCounts.maxOfOrNull { it.count } ?: 1)
    Column(
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.18f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Frecuencia semanal", color = Color.White, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(id = R.drawable.ic_help),
                contentDescription = "Info",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(18.dp).clickable {
                    onInfo(
                        DiarioInfoItem(
                            "Frecuencia Semanal",
                            "Cuenta cuántas entradas fueron creadas en cada día de la semana usando la fecha de creación."
                        )
                    )
                }
            )
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
                                .background(
                                    Brush.verticalGradient(colors = listOf(Color(0xFFFFC107), Color(0xFFFF9800)))
                                )
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
private fun DiarioMoodRingsSection(stats: DiarioStatsSnapshot, onInfo: (DiarioInfoItem) -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.18f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Sentimientos predominantes", color = Color.White, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(id = R.drawable.ic_help),
                contentDescription = "Info",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(18.dp).clickable {
                    onInfo(
                        DiarioInfoItem(
                            "Sentimientos Predominantes",
                            "Ordena las emociones por frecuencia y visualiza las 3 más frecuentes con su peso relativo en el Diario."
                        )
                    )
                }
            )
        }

        if (stats.topEmotions.isEmpty()) {
            Text("Aún no hay datos emocionales suficientes.", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                stats.topEmotions.take(3).forEachIndexed { idx, emotion ->
                    DiarioRingMetric(
                        progress = emotion.count.toDouble() / max(1, stats.totalEntries).toDouble(),
                        color = when (idx) {
                            0 -> Color(0xFF80CBC4)
                            1 -> Color(0xFF4DD0E1)
                            else -> Color(0xFF26C6DA)
                        },
                        emoji = emotion.emoji,
                        title = emotion.name,
                        value = emotion.count,
                        modifier = Modifier.weight(1f),
                        onInfo = {
                            onInfo(
                                DiarioInfoItem(
                                    "Anillo de Emoción",
                                    "Cada anillo representa el porcentaje de una emoción sobre el total de entradas registradas."
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DiarioRingMetric(
    progress: Double,
    color: Color,
    emoji: String,
    title: String,
    value: Int,
    modifier: Modifier = Modifier,
    onInfo: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Icon(
                painter = painterResource(id = R.drawable.ic_help),
                contentDescription = "Info",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(14.dp).clickable(onClick = onInfo)
            )
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
        Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(value.toString(), color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
    }
}

@Composable
private fun DiarioDotTrendSection(stats: DiarioStatsSnapshot, onInfo: (DiarioInfoItem) -> Unit) {
    var selectedDays by remember { mutableStateOf(30) }
    val dayOptions = listOf(7, 15, 30, 45, 60, 90)
    val points = remember(stats, selectedDays) { stats.dayPoints(selectedDays) }
    val dayLabelFormat = remember { SimpleDateFormat("d", Locale.getDefault()) }
    val rows = max(1, (points.size + 6) / 7)
    val gridHeight = (rows * 44 + (rows - 1) * 12).dp

    Column(
        modifier = Modifier
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.18f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Actividad de los últimos $selectedDays días", color = Color.White, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(id = R.drawable.ic_help),
                contentDescription = "Info",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(18.dp).clickable {
                    onInfo(
                        DiarioInfoItem(
                            "Actividad de los últimos $selectedDays Días",
                            "Cada punto representa un día. Puedes cambiar el rango a 7, 15, 30, 45, 60 o 90 días para analizar tu ritmo de escritura."
                        )
                    )
                }
            )
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
            modifier = Modifier.fillMaxWidth().height(gridHeight),
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
                    point.count == 1 -> Color(0xFF4CAF50)
                    point.count in 2..3 -> Color(0xFFFFEB3B)
                    else -> Color(0xFFFF9800)
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

private fun startOfDay(timeMillis: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = timeMillis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
