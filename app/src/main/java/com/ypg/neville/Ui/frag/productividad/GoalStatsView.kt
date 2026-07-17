package com.ypg.neville.ui.frag

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ypg.neville.R
import com.ypg.neville.model.metas.ArchivedGoalCardState
import com.ypg.neville.model.metas.GoalCardState
import com.ypg.neville.model.metas.TimeUnitType
import com.ypg.neville.model.metas.UnitStatus
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

@Composable
fun GoalStatsScreen(
    goals: List<GoalCardState>,
    archivedGoals: List<ArchivedGoalCardState>,
    onClose: () -> Unit,
    onRefresh: () -> Unit
) {
    val stats = remember(goals, archivedGoals) { GoalStatsSnapshot(goals, archivedGoals) }
    var infoItem by remember { mutableStateOf<GoalInfoItem?>(null) }

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
                    text = "Estadísticas de Metas",
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
                item { GoalHeadlineCards(stats = stats, onInfo = { infoItem = it }) }
                item { GoalWeeklyBarChart(stats = stats, onInfo = { infoItem = it }) }
                item { GoalStatusRingsSection(stats = stats, onInfo = { infoItem = it }) }
                item { GoalFocusSection(stats = stats, onInfo = { infoItem = it }) }
                item { GoalDotTrendSection(stats = stats, onInfo = { infoItem = it }) }
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

private data class GoalStatsSnapshot(
    val activeGoals: Int,
    val completedGoals: Int,
    val actionReadyGoals: Int,
    val totalUnits: Int,
    val completedUnits: Int,
    val lostUnits: Int,
    val pendingUnits: Int,
    val completionRate: Double,
    val averageProgress: Double,
    val currentStreak: Int,
    val longestStreak: Int,
    val weekdayCounts: List<WeekdayCount>,
    val statusShares: List<StatusShare>,
    val topUnitTypes: List<UnitTypeCount>,
    private val dailyCounts: Map<Long, Int>
) {
    data class WeekdayCount(val dayLabel: String, val count: Int)
    data class StatusShare(val title: String, val value: Int, val color: Color, val symbol: String)
    data class UnitTypeCount(val title: String, val count: Int)
    data class DayPoint(val dayStartMillis: Long, val count: Int)

    constructor(goals: List<GoalCardState>, archivedGoals: List<ArchivedGoalCardState>) : this(
        activeGoals = goals.count { it.goal.isStarted && !it.isCompleted },
        completedGoals = archivedGoals.size + goals.count { it.goal.isStarted && it.isCompleted },
        actionReadyGoals = goals.count { state ->
            state.goal.isStarted && state.units.any { unit ->
                UnitStatus.fromRaw(unit.status) == UnitStatus.PENDING &&
                    (unit.startDate ?: Long.MAX_VALUE) <= System.currentTimeMillis()
            }
        },
        totalUnits = goals.sumOf { it.units.size } + archivedGoals.sumOf { it.units.size },
        completedUnits = goals.sumOf { state -> state.units.count { UnitStatus.fromRaw(it.status) == UnitStatus.COMPLETED } } +
            archivedGoals.sumOf { state -> state.units.count { UnitStatus.fromRaw(it.status) == UnitStatus.COMPLETED } },
        lostUnits = goals.sumOf { state -> state.units.count { UnitStatus.fromRaw(it.status) == UnitStatus.LOST } } +
            archivedGoals.sumOf { state -> state.units.count { UnitStatus.fromRaw(it.status) == UnitStatus.LOST } },
        pendingUnits = goals.sumOf { state -> state.units.count { UnitStatus.fromRaw(it.status) == UnitStatus.PENDING } } +
            archivedGoals.sumOf { state -> state.units.count { UnitStatus.fromRaw(it.status) == UnitStatus.PENDING } },
        completionRate = calculateCompletionRate(goals, archivedGoals),
        averageProgress = goals.map { it.progressRatio }.let { values ->
            if (values.isEmpty()) 0.0 else values.sum() / values.size.toDouble()
        },
        currentStreak = calculateCurrentStreak(completedDays(goals, archivedGoals)),
        longestStreak = calculateLongestStreak(completedDays(goals, archivedGoals)),
        weekdayCounts = calculateWeekdayCounts(completedDays(goals, archivedGoals)),
        statusShares = buildStatusShares(goals, archivedGoals),
        topUnitTypes = calculateTopUnitTypes(goals, archivedGoals),
        dailyCounts = completedDays(goals, archivedGoals).groupingBy { it }.eachCount()
    )

    fun dayPoints(days: Int): List<DayPoint> {
        val safeDays = max(1, days)
        val cal = Calendar.getInstance()
        val result = mutableListOf<DayPoint>()
        for (offset in (safeDays - 1) downTo 0) {
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.DAY_OF_YEAR, -offset)
            val day = goalStartOfDay(cal.timeInMillis)
            result += DayPoint(dayStartMillis = day, count = dailyCounts[day] ?: 0)
        }
        return result
    }

    companion object {
        private fun calculateCompletionRate(
            goals: List<GoalCardState>,
            archivedGoals: List<ArchivedGoalCardState>
        ): Double {
            val completed = goals.sumOf { it.completedCount } + archivedGoals.sumOf { it.completedCount }
            val total = goals.sumOf { it.units.size } + archivedGoals.sumOf { it.units.size }
            return if (total == 0) 0.0 else completed.toDouble() / total.toDouble()
        }

        private fun buildStatusShares(
            goals: List<GoalCardState>,
            archivedGoals: List<ArchivedGoalCardState>
        ): List<StatusShare> {
            val liveUnits = goals.flatMap { it.units.map { unit -> UnitStatus.fromRaw(unit.status) } }
            val archivedUnits = archivedGoals.flatMap { it.units.map { unit -> UnitStatus.fromRaw(unit.status) } }
            val allUnits = liveUnits + archivedUnits
            return listOf(
                StatusShare("Fichadas", allUnits.count { it == UnitStatus.COMPLETED }, Color(0xFF66BB6A), "OK"),
                StatusShare("Pendientes", allUnits.count { it == UnitStatus.PENDING }, Color(0xFF4DD0E1), "..."),
                StatusShare("Perdidas", allUnits.count { it == UnitStatus.LOST }, Color(0xFFFFB74D), "!")
            )
        }

        private fun calculateTopUnitTypes(
            goals: List<GoalCardState>,
            archivedGoals: List<ArchivedGoalCardState>
        ): List<UnitTypeCount> {
            val unitTypes = goals.map { it.goal.unitType } + archivedGoals.map { it.goal.unitType }
            return unitTypes
                .groupingBy { TimeUnitType.fromRaw(it).descriptionFor(2).replaceFirstChar { c -> c.uppercase() } }
                .eachCount()
                .map { UnitTypeCount(it.key, it.value) }
                .sortedByDescending { it.count }
        }

        private fun calculateWeekdayCounts(days: List<Long>): List<WeekdayCount> {
            val labels = listOf("D", "L", "M", "X", "J", "V", "S")
            val counts = IntArray(7)
            val cal = Calendar.getInstance()
            days.forEach { day ->
                cal.timeInMillis = day
                counts[cal.get(Calendar.DAY_OF_WEEK) - 1]++
            }
            return labels.indices.map { idx -> WeekdayCount(labels[idx], counts[idx]) }
        }

        private fun calculateCurrentStreak(days: List<Long>): Int {
            if (days.isEmpty()) return 0
            val set = days.toSet()
            var streak = 0
            var cursor = goalStartOfDay(System.currentTimeMillis())
            if (!set.contains(cursor) && set.contains(cursor - GOAL_DAY_MS)) {
                cursor -= GOAL_DAY_MS
            }
            while (set.contains(cursor)) {
                streak++
                cursor -= GOAL_DAY_MS
            }
            return streak
        }

        private fun calculateLongestStreak(days: List<Long>): Int {
            val sorted = days.distinct().sorted()
            if (sorted.isEmpty()) return 0
            var best = 1
            var current = 1
            for (idx in 1 until sorted.size) {
                if (sorted[idx] - sorted[idx - 1] == GOAL_DAY_MS) {
                    current++
                    best = max(best, current)
                } else {
                    current = 1
                }
            }
            return best
        }

        private fun completedDays(
            goals: List<GoalCardState>,
            archivedGoals: List<ArchivedGoalCardState>
        ): List<Long> {
            val liveDays = goals.flatMap { state ->
                state.units.mapNotNull { unit ->
                    unit.completedDate?.takeIf { UnitStatus.fromRaw(unit.status) == UnitStatus.COMPLETED }
                }
            }
            val archivedDays = archivedGoals.flatMap { state ->
                state.units.mapNotNull { unit ->
                    unit.completedDate?.takeIf { UnitStatus.fromRaw(unit.status) == UnitStatus.COMPLETED }
                }
            }
            return (liveDays + archivedDays).map { goalStartOfDay(it) }
        }
    }
}

private const val GOAL_DAY_MS = 24L * 60 * 60 * 1000

private data class GoalInfoItem(val title: String, val message: String)

@Composable
private fun GoalHeadlineCards(stats: GoalStatsSnapshot, onInfo: (GoalInfoItem) -> Unit) {
    val percentFormatter = remember { DecimalFormat("0%") }
    Column(modifier = Modifier.padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Tu avance en metas", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GoalMetricCard("Activas", stats.activeGoals.toString(), "en curso", Modifier.weight(1f)) {
                onInfo(GoalInfoItem("Metas Activas", "Metas iniciadas que todavía tienen unidades pendientes."))
            }
            GoalMetricCard("Completadas", stats.completedGoals.toString(), "histórico", Modifier.weight(1f)) {
                onInfo(GoalInfoItem("Metas Completadas", "Metas terminadas, incluyendo las que ya fueron archivadas."))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GoalMetricCard("Listas", stats.actionReadyGoals.toString(), "para fichar", Modifier.weight(1f)) {
                onInfo(GoalInfoItem("Metas Listas", "Metas con al menos una unidad pendiente cuyo tiempo de inicio ya llegó."))
            }
            GoalMetricCard("Efectividad", percentFormatter.format(stats.completionRate), "unidades", Modifier.weight(1f)) {
                onInfo(GoalInfoItem("Efectividad", "Porcentaje de unidades fichadas sobre el total de unidades creadas."))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GoalMetricCard("Racha actual", stats.currentStreak.toString(), "días", Modifier.weight(1f)) {
                onInfo(GoalInfoItem("Racha Actual", "Días consecutivos recientes con al menos una unidad completada."))
            }
            GoalMetricCard("Mejor racha", stats.longestStreak.toString(), "días", Modifier.weight(1f)) {
                onInfo(GoalInfoItem("Mejor Racha", "Mayor número histórico de días consecutivos completando unidades."))
            }
        }
    }
}

@Composable
private fun GoalMetricCard(
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
private fun GoalWeeklyBarChart(stats: GoalStatsSnapshot, onInfo: (GoalInfoItem) -> Unit) {
    val maxCount = max(1, stats.weekdayCounts.maxOfOrNull { it.count } ?: 1)
    GoalPanel(title = "Frecuencia semanal", onInfo = {
        onInfo(GoalInfoItem("Frecuencia Semanal", "Cuenta cuántas unidades fueron completadas en cada día de la semana."))
    }) {
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
                                .background(Brush.verticalGradient(colors = listOf(Color(0xFFFFC107), Color(0xFFFF9800))))
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
private fun GoalStatusRingsSection(stats: GoalStatsSnapshot, onInfo: (GoalInfoItem) -> Unit) {
    GoalPanel(title = "Estado de unidades", onInfo = {
        onInfo(GoalInfoItem("Estado de Unidades", "Reparte todas las unidades entre fichadas, pendientes y perdidas."))
    }) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            stats.statusShares.forEach { share ->
                GoalRingMetric(
                    progress = share.value.toDouble() / max(1, stats.totalUnits).toDouble(),
                    color = share.color,
                    symbol = share.symbol,
                    title = share.title,
                    value = share.value,
                    modifier = Modifier.weight(1f),
                    onInfo = {
                        onInfo(GoalInfoItem("Anillo de Estado", "Cada anillo muestra el porcentaje de ese estado sobre el total de unidades."))
                    }
                )
            }
        }
    }
}

@Composable
private fun GoalRingMetric(
    progress: Double,
    color: Color,
    symbol: String,
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
            Text(symbol, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value.toString(), color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
    }
}

@Composable
private fun GoalFocusSection(stats: GoalStatsSnapshot, onInfo: (GoalInfoItem) -> Unit) {
    val percentFormatter = remember { DecimalFormat("0%") }
    GoalPanel(title = "Enfoque de práctica", onInfo = {
        onInfo(GoalInfoItem("Enfoque de Práctica", "Muestra el progreso medio de metas activas y los tipos de unidad más utilizados."))
    }) {
        GoalMetricCard(
            title = "Progreso medio",
            value = percentFormatter.format(stats.averageProgress),
            subtitle = "metas activas",
            modifier = Modifier.fillMaxWidth(),
            onInfo = {
                onInfo(GoalInfoItem("Progreso Medio", "Promedio del avance de las metas activas según unidades completadas o perdidas."))
            }
        )

        if (stats.topUnitTypes.isEmpty()) {
            Text("Aún no hay tipos de unidad suficientes.", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                stats.topUnitTypes.forEach { type ->
                    Column(
                        modifier = Modifier
                            .width(128.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .padding(12.dp)
                    ) {
                        Text(type.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${type.count} metas", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalDotTrendSection(stats: GoalStatsSnapshot, onInfo: (GoalInfoItem) -> Unit) {
    var selectedDays by remember { mutableStateOf(30) }
    val dayOptions = listOf(7, 15, 30, 45, 60, 90)
    val points = remember(stats, selectedDays) { stats.dayPoints(selectedDays) }
    val dayLabelFormat = remember { SimpleDateFormat("d", Locale.getDefault()) }
    val rows = max(1, (points.size + 6) / 7)
    val gridHeight = (rows * 44 + (rows - 1) * 12).dp

    GoalPanel(title = "Actividad de los últimos $selectedDays días", onInfo = {
        onInfo(GoalInfoItem("Actividad de los últimos $selectedDays Días", "Cada punto representa un día y su tamaño indica cuántas unidades fueron fichadas."))
    }, bottomPadding = 8.dp) {
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
                        Box(modifier = Modifier.size(dotSize).clip(CircleShape).background(dotColor))
                        if (point.count > 0) {
                            Text(point.count.toString(), color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
private fun GoalPanel(
    title: String,
    onInfo: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .padding(bottom = bottomPadding)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.18f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(id = R.drawable.ic_help),
                contentDescription = "Info",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(18.dp).clickable(onClick = onInfo)
            )
        }
        content()
    }
}

private fun goalStartOfDay(timeMillis: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = timeMillis
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
