package com.ypg.neville.feature.weeklysummary.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ypg.neville.R
import com.ypg.neville.feature.weeklysummary.data.WeeklySummaryEntity
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

private val StatsBackground = Color(0xFFF7F7FA)
private val StatsSurface = Color.White
private val StatsText = Color(0xFF202124)
private val StatsSecondary = Color(0xFF6B6B73)
private val StatsIndigo = Color(0xFF4F46A5)
private val StatsTeal = Color(0xFF0F8B83)
private val StatsGreen = Color(0xFF218739)
private val StatsPurple = Color(0xFF8A3FA0)
private val StatsOrange = Color(0xFFC66A16)
private val StatsBlue = Color(0xFF3568B8)
private val StatsYellow = Color(0xFFB78305)

@Composable
fun WeeklySummaryStatsView(
    summaries: List<WeeklySummaryEntity>,
    modifier: Modifier = Modifier
) {
    var selectedRange by remember { mutableStateOf(WeeklySummaryStatsRange.TwelveWeeks) }
    val summarySnapshot = summaries.toList()
    val analysis = remember(summarySnapshot, selectedRange) {
        WeeklySummaryStatsAnalysis(
            allRecords = summarySnapshot.sortedBy { it.weekStartMillis },
            range = selectedRange
        )
    }
    val locale = LocalConfiguration.current.locales[0]

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(StatsBackground),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            top = 8.dp,
            end = 20.dp,
            bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            StatsHero(recordCount = summarySnapshot.size)
        }

        if (summarySnapshot.isEmpty()) {
            item {
                StatsEmptyState()
            }
            return@LazyColumn
        }

        item {
            StatsRangePicker(
                selected = selectedRange,
                onSelected = { selectedRange = it }
            )
        }

        item {
            StatsSummaryGrid(analysis = analysis, locale = locale)
        }

        item {
            StatsSectionCard(
                title = stringResource(R.string.weekly_stats_next_action),
                subtitle = stringResource(R.string.weekly_stats_next_action_subtitle),
                icon = Icons.Rounded.TrackChanges,
                accent = StatsIndigo
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = StatsIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = analysis.nextBestAction(),
                        color = StatsText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 21.sp
                    )
                }
            }
        }

        item {
            StatsHighlights(analysis = analysis, locale = locale)
        }

        item {
            StatsBalance(analysis = analysis, locale = locale)
        }

        item {
            StatsTrends(analysis = analysis)
        }

        item {
            StatsInsights(analysis = analysis)
        }

        item {
            StatsPredominantActivity(analysis = analysis)
        }

        item {
            StatsRecentWeeks(analysis = analysis, locale = locale)
        }
    }
}

@Composable
private fun StatsHero(recordCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFE2E0F7), Color(0xFFE1F3F0))
                )
            )
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ShowChart,
                contentDescription = null,
                tint = StatsIndigo,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.weekly_stats_visible_evolution),
                color = StatsIndigo,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = stringResource(R.string.weekly_stats_reviews_summary),
            color = StatsText,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 34.sp
        )
        Text(
            text = if (recordCount == 0) {
                stringResource(R.string.weekly_stats_header_empty)
            } else {
                stringResource(R.string.weekly_stats_saved_reviews, recordCount)
            },
            color = StatsSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun StatsEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFECECF0), RoundedCornerShape(16.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.History,
                contentDescription = null,
                tint = StatsText
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.weekly_stats_no_history),
                color = StatsText,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = stringResource(R.string.weekly_stats_no_history_body),
            color = StatsSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun StatsRangePicker(
    selected: WeeklySummaryStatsRange,
    onSelected: (WeeklySummaryStatsRange) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE7E7EC), RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        WeeklySummaryStatsRange.entries.forEach { range ->
            val isSelected = selected == range
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) StatsSurface else Color.Transparent)
                    .clickable { onSelected(range) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(range.labelRes),
                    color = StatsText,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun StatsSummaryGrid(
    analysis: WeeklySummaryStatsAnalysis,
    locale: Locale
) {
    val format = remember(locale) {
        NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatsSummaryCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.weekly_stats_reviews),
                value = analysis.recent.size.toString(),
                caption = stringResource(
                    if (analysis.range == WeeklySummaryStatsRange.All) {
                        R.string.weekly_stats_saved_closures
                    } else {
                        R.string.weekly_stats_in_range
                    }
                ),
                icon = Icons.Rounded.CheckCircle,
                accent = StatsGreen
            )
            StatsSummaryCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.weekly_section_journal),
                value = format.format(analysis.average { it.journalCreated }),
                caption = stringResource(R.string.weekly_stats_entries_per_week),
                icon = Icons.Rounded.Book,
                accent = StatsPurple
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatsSummaryCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.weekly_stats_presence),
                value = format.format(analysis.average { it.eveningPresenceReturns }),
                caption = stringResource(R.string.weekly_stats_returns_per_week),
                icon = Icons.Rounded.Favorite,
                accent = StatsTeal
            )
            StatsSummaryCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.weekly_stats_coherence),
                value = stringResource(
                    R.string.weekly_stats_minutes_value,
                    analysis.average { it.cardioCoherenceMinutes }.toInt()
                ),
                caption = stringResource(R.string.weekly_stats_weekly_average),
                icon = Icons.Rounded.Waves,
                accent = StatsIndigo
            )
        }
    }
}

@Composable
private fun StatsSummaryCard(
    title: String,
    value: String,
    caption: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(124.dp)
            .background(Color(0xFFECECF0), RoundedCornerShape(15.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = title,
                color = accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = value,
            color = StatsText,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = caption,
            color = StatsSecondary,
            fontSize = 12.sp,
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun StatsHighlights(
    analysis: WeeklySummaryStatsAnalysis,
    locale: Locale
) {
    StatsSectionCard(
        title = stringResource(R.string.weekly_stats_highlighted_weeks),
        subtitle = stringResource(R.string.weekly_stats_highlighted_weeks_subtitle),
        icon = Icons.Rounded.EmojiEvents,
        accent = StatsYellow
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            analysis.bestIntegratedWeek?.let {
                StatsHighlightRow(
                    title = stringResource(R.string.weekly_stats_most_integrated_week),
                    value = weekTitle(it, locale),
                    caption = analysis.balanceLabel(it),
                    icon = Icons.Rounded.Stars
                )
            }
            analysis.strongestPresenceWeek?.let {
                StatsHighlightRow(
                    title = stringResource(R.string.weekly_stats_highest_presence),
                    value = stringResource(R.string.weekly_stats_returns_value, it.eveningPresenceReturns),
                    caption = weekTitle(it, locale),
                    icon = Icons.Rounded.Favorite
                )
            }
            analysis.strongestCoherenceWeek?.let {
                StatsHighlightRow(
                    title = stringResource(R.string.weekly_stats_more_coherence),
                    value = stringResource(R.string.weekly_stats_minutes_value, it.cardioCoherenceMinutes),
                    caption = weekTitle(it, locale),
                    icon = Icons.Rounded.Waves
                )
            }
        }
    }
}

@Composable
private fun StatsHighlightRow(
    title: String,
    value: String,
    caption: String,
    icon: ImageVector
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = StatsYellow,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = title, color = StatsText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = value, color = StatsText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = caption, color = StatsSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun StatsBalance(
    analysis: WeeklySummaryStatsAnalysis,
    locale: Locale
) {
    val latest = analysis.recent.lastOrNull()
    StatsSectionCard(
        title = stringResource(R.string.weekly_stats_weekly_balance),
        subtitle = stringResource(R.string.weekly_stats_weekly_balance_subtitle),
        icon = Icons.Rounded.GridView,
        accent = StatsTeal
    ) {
        if (latest != null) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Rounded.GridView,
                    contentDescription = null,
                    tint = StatsTeal,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = analysis.balanceLabel(latest),
                        color = StatsText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.weekly_stats_last_review, weekTitle(latest, locale)),
                        color = StatsSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatsBalanceChip(
                        Modifier.weight(1f),
                        stringResource(R.string.weekly_section_goals),
                        latest.goalActivity > 0
                    )
                    StatsBalanceChip(
                        Modifier.weight(1f),
                        stringResource(R.string.weekly_stats_agenda),
                        latest.agendaActivity > 0
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatsBalanceChip(
                        Modifier.weight(1f),
                        stringResource(R.string.weekly_section_journal),
                        latest.journalCreated > 0
                    )
                    StatsBalanceChip(
                        Modifier.weight(1f),
                        stringResource(R.string.weekly_stats_presence),
                        latest.eveningPresenceReturns > 0
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatsBalanceChip(
                        Modifier.weight(1f),
                        stringResource(R.string.weekly_stats_coherence),
                        latest.cardioCoherenceMinutes > 0
                    )
                    StatsBalanceChip(
                        Modifier.weight(1f),
                        stringResource(R.string.weekly_stats_ritual),
                        latest.ritualActivity > 0
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsBalanceChip(
    modifier: Modifier,
    title: String,
    isActive: Boolean
) {
    Row(
        modifier = modifier
            .background(
                if (isActive) Color(0xFFF0F6F1) else Color(0xFFF6F6F7),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isActive) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isActive) StatsGreen else StatsSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = title,
            color = if (isActive) StatsGreen else StatsSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatsTrends(analysis: WeeklySummaryStatsAnalysis) {
    StatsSectionCard(
        title = stringResource(analysis.range.labelRes),
        subtitle = stringResource(R.string.weekly_stats_trends_subtitle),
        icon = Icons.Rounded.BarChart,
        accent = StatsBlue
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatsTrendRow(
                stringResource(R.string.weekly_section_goals),
                analysis.sum { it.goalActivity },
                analysis.maxWindowSum { it.goalActivity }
            )
            StatsTrendRow(
                stringResource(R.string.weekly_stats_agenda),
                analysis.sum { it.agendaActivity },
                analysis.maxWindowSum { it.agendaActivity }
            )
            StatsTrendRow(
                stringResource(R.string.weekly_section_journal),
                analysis.sum { it.journalCreated },
                analysis.maxWindowSum { it.journalCreated }
            )
            StatsTrendRow(
                stringResource(R.string.weekly_stats_presence),
                analysis.sum { it.eveningPresenceReturns },
                analysis.maxWindowSum { it.eveningPresenceReturns }
            )
            StatsTrendRow(
                stringResource(R.string.weekly_stats_coherence),
                analysis.sum { it.cardioCoherenceMinutes },
                analysis.maxWindowSum { it.cardioCoherenceMinutes }
            )
            StatsTrendRow(
                stringResource(R.string.weekly_stats_rituals),
                analysis.sum { it.ritualActivity },
                analysis.maxWindowSum { it.ritualActivity }
            )
        }
    }
}

@Composable
private fun StatsTrendRow(title: String, value: Int, maxValue: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Text(text = title, color = StatsText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(text = value.toString(), color = StatsSecondary, fontSize = 14.sp)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(Color(0xFFE8E8EB), RoundedCornerShape(6.dp))
        ) {
            if (value > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((value.toFloat() / max(maxValue, 1)).coerceIn(0f, 1f))
                        .height(10.dp)
                        .background(
                            Brush.horizontalGradient(listOf(StatsIndigo, Color(0xFF7167C7))),
                            RoundedCornerShape(6.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun StatsInsights(analysis: WeeklySummaryStatsAnalysis) {
    val insights = analysis.insights()
    StatsSectionCard(
        title = stringResource(R.string.weekly_stats_useful_reading),
        subtitle = stringResource(R.string.weekly_stats_useful_reading_subtitle),
        icon = Icons.Rounded.Insights,
        accent = StatsOrange
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            insights.forEach { insight ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        tint = StatsOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = insight,
                        color = StatsText,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsPredominantActivity(analysis: WeeklySummaryStatsAnalysis) {
    StatsSectionCard(
        title = stringResource(R.string.weekly_stats_predominant_areas),
        subtitle = stringResource(R.string.weekly_stats_predominant_areas_subtitle),
        icon = Icons.Rounded.AutoAwesome,
        accent = StatsPurple
    ) {
        val activities = listOf(
            stringResource(R.string.weekly_section_goals) to analysis.sum { it.goalActivity },
            stringResource(R.string.weekly_stats_agenda) to analysis.sum { it.agendaActivity },
            stringResource(R.string.weekly_section_journal) to analysis.sum { it.journalCreated },
            stringResource(R.string.weekly_stats_presence) to analysis.sum { it.eveningPresenceReturns },
            stringResource(R.string.weekly_stats_coherence) to analysis.sum { it.cardioCoherenceMinutes },
            stringResource(R.string.weekly_stats_rituals) to analysis.sum { it.ritualActivity }
        ).sortedByDescending { it.second }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            activities.forEach { (title, count) ->
                Row {
                    Text(text = title, color = StatsText, fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = count.toString(),
                        color = StatsSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsRecentWeeks(
    analysis: WeeklySummaryStatsAnalysis,
    locale: Locale
) {
    StatsSectionCard(
        title = stringResource(R.string.weekly_stats_recent_weeks),
        subtitle = stringResource(R.string.weekly_stats_recent_weeks_subtitle),
        icon = Icons.Rounded.History,
        accent = StatsGreen
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            analysis.recent.asReversed().take(5).forEach { summary ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Rounded.Flag,
                        contentDescription = null,
                        tint = StatsGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = weekTitle(summary, locale),
                            color = StatsText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = analysis.balanceLabel(summary),
                            color = StatsSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsSectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(StatsSurface, RoundedCornerShape(18.dp))
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                color = accent,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 23.sp
            )
        }
        Text(
            text = subtitle,
            color = StatsSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        content()
    }
}

private enum class WeeklySummaryStatsRange(
    @param:StringRes val labelRes: Int,
    val limit: Int?
) {
    FourWeeks(R.string.weekly_stats_range_four, 4),
    TwelveWeeks(R.string.weekly_stats_range_twelve, 12),
    TwentyFourWeeks(R.string.weekly_stats_range_twenty_four, 24),
    All(R.string.weekly_stats_range_all, null)
}

private class WeeklySummaryStatsAnalysis(
    val allRecords: List<WeeklySummaryEntity>,
    val range: WeeklySummaryStatsRange
) {
    val recent: List<WeeklySummaryEntity> =
        range.limit?.let { allRecords.takeLast(it) } ?: allRecords

    private val previous: List<WeeklySummaryEntity> = range.limit?.let { limit ->
        val end = max(allRecords.size - limit, 0)
        val start = max(end - limit, 0)
        allRecords.subList(start, end)
    } ?: emptyList()

    val bestIntegratedWeek: WeeklySummaryEntity? =
        recent.maxByOrNull(::balanceScore)

    val strongestPresenceWeek: WeeklySummaryEntity? =
        recent.maxByOrNull { it.eveningPresenceReturns }

    val strongestCoherenceWeek: WeeklySummaryEntity? =
        recent.maxByOrNull { it.cardioCoherenceMinutes }

    fun average(selector: (WeeklySummaryEntity) -> Int): Double {
        if (recent.isEmpty()) return 0.0
        return recent.sumOf(selector).toDouble() / recent.size
    }

    fun sum(selector: (WeeklySummaryEntity) -> Int): Int = recent.sumOf(selector)

    fun maxWindowSum(selector: (WeeklySummaryEntity) -> Int): Int {
        val window = max(recent.size, 1)
        return allRecords
            .chunked(window)
            .maxOfOrNull { chunk -> chunk.sumOf(selector) }
            ?.coerceAtLeast(1)
            ?: 1
    }

    @Composable
    fun balanceLabel(summary: WeeklySummaryEntity): String {
        val score = balanceScore(summary)
        return when {
            score >= 5 -> stringResource(R.string.weekly_stats_balance_integrated)
            summary.goalActivity > 0 && summary.agendaActivity > 0 ->
                stringResource(R.string.weekly_stats_balance_action)
            summary.journalCreated > 0 && summary.eveningPresenceReturns > 0 ->
                stringResource(R.string.weekly_stats_balance_inner_presence)
            summary.cardioCoherenceMinutes > 0 || summary.ritualActivity > 0 ->
                stringResource(R.string.weekly_stats_balance_regulation)
            score > 0 -> stringResource(R.string.weekly_stats_balance_continuity)
            else -> stringResource(R.string.weekly_stats_balance_recovery)
        }
    }

    @Composable
    fun nextBestAction(): String {
        val recentDiary = average { it.journalCreated }
        val previousDiary = previous.averageOf { it.journalCreated }
        val recentPresence = average { it.eveningPresenceReturns }
        val previousPresence = previous.averageOf { it.eveningPresenceReturns }
        val recentCoherence = average { it.cardioCoherenceMinutes }
        val previousCoherence = previous.averageOf { it.cardioCoherenceMinutes }
        val stagnantWeeks = recent.count { it.goalsInProgress > 0 && it.goalActivity == 0 }

        return when {
            stagnantWeeks > max(1, recent.size / 3) ->
                stringResource(R.string.weekly_stats_action_goal)
            previous.isNotEmpty() && recentDiary + 0.2 < previousDiary ->
                stringResource(R.string.weekly_stats_action_diary)
            previous.isNotEmpty() && recentPresence + 0.2 < previousPresence ->
                stringResource(R.string.weekly_stats_action_presence)
            previous.isNotEmpty() && recentCoherence + 1.0 < previousCoherence ->
                stringResource(R.string.weekly_stats_action_coherence)
            recent.lastOrNull()?.ritualActivity == 0 ->
                stringResource(R.string.weekly_stats_action_ritual)
            else -> stringResource(R.string.weekly_stats_action_maintain)
        }
    }

    @Composable
    fun insights(): List<String> {
        val result = mutableListOf<String>()
        if (previous.isNotEmpty()) {
            result += trendText(
                stringResource(R.string.weekly_section_journal),
                average { it.journalCreated },
                previous.averageOf { it.journalCreated }
            )
            result += trendText(
                stringResource(R.string.weekly_stats_presence),
                average { it.eveningPresenceReturns },
                previous.averageOf { it.eveningPresenceReturns }
            )
            result += trendText(
                stringResource(R.string.weekly_stats_coherence),
                average { it.cardioCoherenceMinutes },
                previous.averageOf { it.cardioCoherenceMinutes }
            )
        }

        val stagnantWeeks = recent.count { it.goalsInProgress > 0 && it.goalActivity == 0 }
        result += if (stagnantWeeks > max(1, recent.size / 3)) {
            stringResource(
                R.string.weekly_stats_insight_stagnant_goals,
                stagnantWeeks,
                recent.size
            )
        } else {
            stringResource(R.string.weekly_stats_insight_goals_moving)
        }

        if (result.isEmpty()) {
            result += stringResource(R.string.weekly_stats_insight_more_weeks)
        }
        return result.take(5)
    }

    private fun balanceScore(summary: WeeklySummaryEntity): Int {
        var score = 0
        if (summary.goalActivity > 0) score++
        if (summary.agendaActivity > 0) score++
        if (summary.journalCreated > 0) score++
        if (summary.eveningPresenceReturns > 0) score++
        if (summary.cardioCoherenceMinutes > 0) score++
        if (summary.ritualActivity > 0) score++
        return score
    }
}

private val WeeklySummaryEntity.goalActivity: Int
    get() = goalsCompleted + eveningGoalUnitsCompleted

private val WeeklySummaryEntity.agendaActivity: Int
    get() = remindersCreated + remindersModified

private val WeeklySummaryEntity.ritualActivity: Int
    get() = morningRitualsCompleted + eveningRitualsCompleted

private fun List<WeeklySummaryEntity>.averageOf(
    selector: (WeeklySummaryEntity) -> Int
): Double {
    if (isEmpty()) return 0.0
    return sumOf(selector).toDouble() / size
}

@Composable
private fun trendText(
    title: String,
    current: Double,
    previous: Double
): String {
    val delta = current - previous
    return when {
        abs(delta) < 0.2 -> stringResource(R.string.weekly_stats_trend_stable, title)
        delta > 0 -> stringResource(R.string.weekly_stats_trend_up, title)
        else -> stringResource(R.string.weekly_stats_trend_down, title)
    }
}

private fun weekTitle(summary: WeeklySummaryEntity, locale: Locale): String {
    val formatter = DateTimeFormatter.ofPattern("d MMM", locale)
    val zone = ZoneId.systemDefault()
    val start = Instant.ofEpochMilli(summary.weekStartMillis).atZone(zone).toLocalDate()
    val end = Instant.ofEpochMilli(summary.weekEndMillis - 1L).atZone(zone).toLocalDate()
    return "${start.format(formatter)} – ${end.format(formatter)}"
}
