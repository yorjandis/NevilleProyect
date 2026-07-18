package com.ypg.neville.feature.presence.ui

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.ypg.neville.R
import com.ypg.neville.feature.presence.data.PresenceDayStats
import com.ypg.neville.feature.presence.data.PresenceEventPoint
import com.ypg.neville.feature.presence.data.PresenceMood
import com.ypg.neville.feature.presence.data.PresenceMoodStats
import com.ypg.neville.feature.presence.data.PresenceRepository
import com.ypg.neville.feature.presence.data.PresenceSettings
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import com.ypg.neville.model.preferences.DbPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class FragPresence : Fragment() {
    private val viewModel: PresenceViewModel by viewModels {
        val database = NevilleRoomDatabase.getInstance(requireContext().applicationContext)
        PresenceViewModel.Factory(PresenceRepository(database.presenceEventDao()))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            com.ypg.neville.ui.theme.NevilleTheme {
                PresenceRoot(
                    viewModel = viewModel,
                    onClose = { requireActivity().onBackPressedDispatcher.onBackPressed() }
                )
            }
        }
    }
}

private enum class PresenceStatsCard {
    TodayReturns,
    CurrentStreak,
    WeeklyAverage,
    DominantMood,
    PracticalInsights,
    DailyEvents,
    DailyTimeline,
    Ratio,
    Moods
}

private data class PresenceStatsTheme(
    val screen: Brush,
    val card: Brush,
    val cardStroke: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val rowBackground: Color,
    val gridLine: Color
)

private val DarkStatsTheme = PresenceStatsTheme(
    screen = Brush.verticalGradient(listOf(Color(0xFFF37208C), Color(0xFF238F91))),
    card = Brush.linearGradient(listOf(Color(0xFF2B3347), Color(0xFF1A2133))),
    cardStroke = Color.White.copy(alpha = 0.16f),
    primaryText = Color.White,
    secondaryText = Color.White.copy(alpha = 0.76f),
    rowBackground = Color.White.copy(alpha = 0.08f),
    gridLine = Color.White
)

private val LightStatsTheme = PresenceStatsTheme(
    screen = Brush.verticalGradient(listOf(Color(0xFFDBEDF9), Color(0xFFB3D1E6))),
    card = Brush.linearGradient(listOf(Color(0xFFF5FAFF), Color(0xFFD4E6F5))),
    cardStroke = Color(0xFF2E4D6B).copy(alpha = 0.18f),
    primaryText = Color(0xFF14293D),
    secondaryText = Color(0xFF14293D).copy(alpha = 0.68f),
    rowBackground = Color.White.copy(alpha = 0.42f),
    gridLine = Color(0xFF24425F)
)

@Composable
private fun PresenceRoot(viewModel: PresenceViewModel, onClose: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var showStats by rememberSaveable { mutableStateOf(false) }
    if (showStats) {
        PresenceStatsScreen(
            state = state,
            onBack = { showStats = false },
            onReset = viewModel::resetAll
        )
    } else {
        PresenceMainScreen(
            state = state,
            onClose = onClose,
            onShowStats = { showStats = true },
            onRecord = viewModel::recordPresent,
            onDismissCelebration = viewModel::dismissCelebration,
            onDismissMilestone = viewModel::dismissMilestone
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresenceMainScreen(
    state: PresenceUiState,
    onClose: () -> Unit,
    onShowStats: () -> Unit,
    onRecord: (PresenceMood?) -> Unit,
    onDismissCelebration: () -> Unit,
    onDismissMilestone: () -> Unit
) {
    var showInfo by remember { mutableStateOf(false) }
    var showMoodList by rememberSaveable { mutableStateOf(false) }
    val view = LocalView.current
    val prefs = remember { DbPreferences.default(view.context.applicationContext) }
    val defaultCelebrationPhrase = stringResource(R.string.presence_mood_future)
    val celebrationPhrase = remember(state.showCelebration, defaultCelebrationPhrase) {
        prefs.getString(
            PresenceSettings.CUSTOM_CELEBRATION_PHRASE_KEY,
            defaultCelebrationPhrase
        )?.trim().orEmpty().ifBlank { defaultCelebrationPhrase }
    }

    LaunchedEffect(state.showCelebration) {
        if (state.showCelebration) {
            delay(2300)
            onDismissCelebration()
        }
    }
    LaunchedEffect(state.showMilestone) {
        if (state.showMilestone) {
            delay(3200)
            onDismissMilestone()
        }
    }

    Box(Modifier.fillMaxSize().background(PresenceGradient)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.presence_short_title), color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Rounded.Close, stringResource(R.string.common_close), tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showInfo = true }) {
                            Icon(Icons.Rounded.Info, stringResource(R.string.common_information), tint = Color.White)
                        }
                        IconButton(onClick = onShowStats) {
                            Icon(Icons.Rounded.BarChart, stringResource(R.string.coherence_statistics), tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .scale(if (state.showCelebration) 0.64f else 1f)
                    .alpha(if (state.showCelebration) 0f else 1f)
                    .blur(if (state.showCelebration) 8.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                PresenceHeader(state.todayPresentCount, state.showMilestone)
                PresenceMainAction {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onRecord(null)
                }
                MoodSection(
                    expanded = showMoodList,
                    onExpandedChange = { showMoodList = !showMoodList },
                    onRecordMood = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        onRecord(it)
                    }
                )
            }
        }
        AnimatedVisibility(state.showCelebration, modifier = Modifier.fillMaxSize()) {
            PresenceCelebration(celebrationPhrase)
        }
    }

    if (showInfo) {
        PresenceInfoDialog { showInfo = false }
    }
}

@Composable
private fun PresenceHeader(todayCount: Int, showMilestone: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.presence_back_to_present),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            stringResource(R.string.presence_today_count, todayCount),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.82f)
        )
        AnimatedVisibility(showMilestone) {
            Text(
                stringResource(R.string.presence_milestone),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun PresenceMainAction(onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(300.dp).padding(top = 50.dp, bottom = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        PresenceHaloButton(onClick)
    }
}

@Composable
private fun PresenceHaloButton(onClick: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "presencePulse").animateFloat(
        initialValue = 0.96f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "presencePulseValue"
    )
    Box(
        Modifier.size(218.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(198.dp).scale(pulse).background(Color.White.copy(alpha = 0.16f), CircleShape))
        Box(Modifier.size(174.dp).border(1.4.dp, Color.White.copy(alpha = 0.34f), CircleShape))
        Box(
            Modifier
                .size(154.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFC8B8FF), Color(0xFF7659C7)),
                        center = Offset(42f, 32f),
                        radius = 190f
                    ),
                    CircleShape
                )
                .border(1.dp, Color.White.copy(alpha = 0.32f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Icon(Icons.Rounded.Spa, null, tint = Color.White, modifier = Modifier.size(52.dp))
                Text(stringResource(R.string.presence_here), color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun MoodSection(
    expanded: Boolean,
    onExpandedChange: () -> Unit,
    onRecordMood: (PresenceMood) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onExpandedChange),
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.14f)
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.EmojiEmotions,
                    null,
                    tint = Color.White
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (expanded) stringResource(R.string.presence_hide_mood) else stringResource(R.string.presence_add_mood),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        AnimatedVisibility(expanded) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.18f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(stringResource(R.string.presence_mood), color = Color.White, fontWeight = FontWeight.Bold)
                PresenceMood.common.forEach { mood ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onRecordMood(mood) },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.14f)
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(moodIcon(mood.id), null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Text(localizedPresenceMood(mood.id, mood.title), color = Color.White, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.AddCircle, null, tint = Color.White.copy(alpha = 0.72f))
                        }
                    }
                }
            }
        }
    }
}

private fun moodIcon(id: String): ImageVector = when (id) {
    "sientoMiFuturoAhora" -> Icons.Rounded.AutoAwesome
    "sereno" -> Icons.Rounded.Spa
    "agradecido" -> Icons.Rounded.FavoriteBorder
    else -> Icons.Rounded.Face
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresenceStatsScreen(
    state: PresenceUiState,
    onBack: () -> Unit,
    onReset: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("presence_stats_display", android.content.Context.MODE_PRIVATE)
    }
    var isDark by rememberSaveable {
        mutableStateOf(preferences.getBoolean("dark", true))
    }
    var visibleCards by rememberSaveable {
        val stored = preferences.getString("cards", null)
        mutableStateOf(
            stored?.split(",")?.mapNotNull { value ->
                PresenceStatsCard.entries.firstOrNull { it.name == value }
            }?.toSet() ?: PresenceStatsCard.entries.toSet()
        )
    }
    var visibilityMenuOpen by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var dailyEventsRange by rememberSaveable { mutableStateOf(14) }
    var practicalRange by rememberSaveable { mutableStateOf(14) }
    var ratioRange by rememberSaveable { mutableStateOf(14) }
    var moodRange by rememberSaveable { mutableStateOf(14) }
    var dominantMoodRange by rememberSaveable { mutableStateOf(14) }
    val theme = if (isDark) DarkStatsTheme else LightStatsTheme

    fun persistCards(cards: Set<PresenceStatsCard>) {
        visibleCards = cards
        preferences.edit().putString("cards", cards.joinToString(",") { it.name }).apply()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.presence_short_title), color = theme.primaryText) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.presence_back), tint = theme.primaryText)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { visibilityMenuOpen = true }) {
                            Icon(Icons.Rounded.GridView, stringResource(R.string.presence_select_visible_cards), tint = theme.primaryText)
                        }
                        DropdownMenu(
                            expanded = visibilityMenuOpen,
                            onDismissRequest = { visibilityMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.presence_show_all)) },
                                leadingIcon = { Icon(Icons.Rounded.GridView, null) },
                                onClick = {
                                    persistCards(PresenceStatsCard.entries.toSet())
                                    visibilityMenuOpen = false
                                }
                            )
                            PresenceStatsCard.entries.forEach { card ->
                                DropdownMenuItem(
                                    text = { Text(card.localizedTitle()) },
                                    leadingIcon = {
                                        Icon(
                                            if (card in visibleCards) Icons.Rounded.CheckCircle
                                            else Icons.Rounded.RadioButtonUnchecked,
                                            null
                                        )
                                    },
                                    onClick = {
                                        persistCards(
                                            if (card in visibleCards) visibleCards - card else visibleCards + card
                                        )
                                    }
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = {
                            isDark = !isDark
                            preferences.edit().putBoolean("dark", isDark).apply()
                        }
                    ) {
                        Icon(
                            if (isDark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                            stringResource(R.string.presence_change_appearance),
                            tint = theme.primaryText
                        )
                    }
                    IconButton(onClick = { showResetConfirmation = true }) {
                        Icon(Icons.Rounded.Delete, stringResource(R.string.presence_reset_stats), tint = theme.primaryText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        modifier = Modifier.background(theme.screen)
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.presence_short_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = theme.primaryText
                )
                Text(
                    stringResource(R.string.presence_today_count, state.todayPresentCount),
                    style = MaterialTheme.typography.titleMedium,
                    color = theme.secondaryText
                )
            }

            InsightCards(
                state = state,
                theme = theme,
                visibleCards = visibleCards,
                dominantMoodRange = dominantMoodRange,
                onDominantRangeChange = { dominantMoodRange = it }
            )
            if (PresenceStatsCard.PracticalInsights in visibleCards) {
                PracticalInsightsCard(
                    stats = state.dayStats.takeLast(practicalRange),
                    events = state.eventPoints.inLastDays(practicalRange),
                    range = practicalRange,
                    onRangeChange = { practicalRange = it },
                    theme = theme
                )
            }
            if (PresenceStatsCard.DailyEvents in visibleCards) {
                DailyEventsCard(
                    state.dayStats.takeLast(dailyEventsRange),
                    dailyEventsRange,
                    { dailyEventsRange = it },
                    theme
                )
            }
            if (PresenceStatsCard.DailyTimeline in visibleCards) {
                DailyTimelineCard(
                    state.eventPoints.filter {
                        it.dayStartMillis == PresenceRepository.startOfDay(System.currentTimeMillis())
                    },
                    theme
                )
            }
            if (PresenceStatsCard.Ratio in visibleCards) {
                RatioCard(
                    state.dayStats.takeLast(ratioRange),
                    ratioRange,
                    { ratioRange = it },
                    theme
                )
            }
            if (PresenceStatsCard.Moods in visibleCards) {
                MoodStatsCard(
                    moodStats(state.eventPoints.inLastDays(moodRange)),
                    moodRange,
                    { moodRange = it },
                    theme
                )
            }
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text(stringResource(R.string.presence_reset_title)) },
            text = { Text(stringResource(R.string.presence_reset_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirmation = false
                    onReset()
                }) { Text(stringResource(R.string.presence_delete_history)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}

@Composable
private fun InsightCards(
    state: PresenceUiState,
    theme: PresenceStatsTheme,
    visibleCards: Set<PresenceStatsCard>,
    dominantMoodRange: Int,
    onDominantRangeChange: (Int) -> Unit
) {
    val week = currentWeekStats(state.dayStats)
    val dominant = moodStats(state.eventPoints.inLastDays(dominantMoodRange)).firstOrNull()
    val dominantTotal = moodStats(state.eventPoints.inLastDays(dominantMoodRange)).sumOf { it.count }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (PresenceStatsCard.TodayReturns in visibleCards) {
                MetricCard(
                    icon = Icons.Rounded.Today,
                    title = stringResource(R.string.presence_today_returns),
                    value = state.todayPresentCount.toString(),
                    subtitle = if (state.todayPresentCount >= 10) stringResource(R.string.presence_keep_going) else stringResource(R.string.presence_return_when_notice),
                    accent = Color(0xFF9C68E8),
                    theme = theme,
                    modifier = Modifier.weight(1f)
                )
            }
            if (PresenceStatsCard.CurrentStreak in visibleCards) {
                MetricCard(
                    icon = Icons.Rounded.LocalFireDepartment,
                    title = stringResource(R.string.presence_current_streak),
                    value = state.streakStats.currentDays.toString(),
                    subtitle = stringResource(R.string.presence_consecutive_days),
                    accent = Color(0xFFFF9E42),
                    theme = theme,
                    modifier = Modifier.weight(1f),
                    footer = {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(7) { index ->
                                Box(
                                    Modifier
                                        .size(11.dp)
                                        .border(1.dp, Color(0xFFFF9E42), CircleShape)
                                        .background(
                                            if (index < state.streakStats.currentDays.coerceAtMost(7)) Color(0xFFFF9E42)
                                            else Color.Transparent,
                                            CircleShape
                                        )
                                )
                            }
                        }
                    }
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (PresenceStatsCard.WeeklyAverage in visibleCards) {
                MetricCard(
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    title = stringResource(R.string.presence_week_average),
                    value = (week.sumOf { it.presentes }.toDouble() / 7.0).roundToInt().toString(),
                    subtitle = stringResource(R.string.presence_returns),
                    accent = Color(0xFF43C77B),
                    theme = theme,
                    modifier = Modifier.weight(1f),
                    footer = { WeeklyBars(week, theme) }
                )
            }
            if (PresenceStatsCard.DominantMood in visibleCards) {
                StatsCard(
                    Color(0xFF4D9DE0),
                    theme,
                    Modifier.weight(1f).height(218.dp),
                    cornerRadius = 16.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MetricIcon(Icons.Rounded.FavoriteBorder, Color(0xFF4D9DE0))
                        Spacer(Modifier.weight(1f))
                        CompactRangeMenu(dominantMoodRange, onDominantRangeChange, theme)
                    }
                    Text(
                        stringResource(R.string.presence_dominant_mood),
                        color = theme.primaryText,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        if (dominant == null) Icons.Rounded.Face else Icons.Rounded.EmojiEmotions,
                        null,
                        tint = Color(0xFF4D9DE0),
                        modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally)
                    )
                    Text(
                        dominant?.let { localizedPresenceMood(it.moodId, it.title) } ?: stringResource(R.string.presence_no_records),
                        color = Color(0xFF4D9DE0),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        if (dominant == null) stringResource(R.string.presence_record_mood)
                        else stringResource(R.string.presence_record_percentage, (dominant.count.toFloat() / dominantTotal.coerceAtLeast(1) * 100).roundToInt()),
                        color = theme.secondaryText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    theme: PresenceStatsTheme,
    modifier: Modifier,
    footer: @Composable ColumnScope.() -> Unit = {}
) {
    StatsCard(accent, theme, modifier.height(218.dp), cornerRadius = 16.dp) {
        MetricIcon(icon, accent)
        Text(title, color = theme.primaryText, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.weight(1f))
        Text(
            value,
            color = accent,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Text(
            subtitle,
            color = theme.secondaryText,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        footer()
    }
}

@Composable
private fun MetricIcon(icon: ImageVector, accent: Color) {
    Box(Modifier.size(44.dp).background(accent.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = accent)
    }
}

@Composable
private fun WeeklyBars(stats: List<PresenceDayStats>, theme: PresenceStatsTheme) {
    val max = stats.maxOfOrNull { it.presentes }?.coerceAtLeast(1) ?: 1
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
        stats.forEach { day ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    Modifier
                        .width(8.dp)
                        .height(((day.presentes.toFloat() / max) * 28f).coerceAtLeast(if (day.presentes == 0) 5f else 9f).dp)
                        .background(Color(0xFF43C77B).copy(alpha = if (day.presentes == 0) 0.28f else 0.68f), RoundedCornerShape(3.dp))
                )
                Text(weekdayLetter(day.dateMillis), color = theme.secondaryText, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun PracticalInsightsCard(
    stats: List<PresenceDayStats>,
    events: List<PresenceEventPoint>,
    range: Int,
    onRangeChange: (Int) -> Unit,
    theme: PresenceStatsTheme
) {
    val bestDay = stats.maxByOrNull { it.presentes }?.takeIf { it.presentes > 0 }
    val average = stats.sumOf { it.presentes }.toDouble() / stats.size.coerceAtLeast(1)
    val currentWeek = stats.takeLast(7).sumOf { it.presentes }
    val previousWeek = stats.dropLast(7).takeLast(7).sumOf { it.presentes }
    val automaticEvents = events.filter { it.isAutomaticPilot }
    val criticalWindow = (0..21 step 3)
        .map { start -> start to automaticEvents.count { hourOf(it.createdAtMillis) in start until start + 3 } }
        .maxByOrNull { it.second }
        ?.takeIf { it.second > 0 }
    val trend = when {
        currentWeek == 0 && previousWeek == 0 -> stringResource(R.string.presence_no_trend)
        previousWeek == 0 -> stringResource(R.string.presence_new_activity)
        else -> {
            val percentage = ((currentWeek - previousWeek).toDouble() / previousWeek * 100).roundToInt()
            if (percentage == 0) stringResource(R.string.presence_same_as_last_week)
            else stringResource(
                R.string.presence_trend_change,
                if (percentage > 0) "+" else "",
                kotlin.math.abs(percentage),
                if (percentage > 0) stringResource(R.string.presence_more) else stringResource(R.string.presence_less)
            )
        }
    }

    StatsCard(Color(0xFF42C8D2), theme) {
        Text(stringResource(R.string.presence_practical_data), color = theme.primaryText, fontWeight = FontWeight.Bold)
        RangePicker(range, onRangeChange, theme)
        InsightRow(
            Icons.Rounded.CalendarMonth,
            stringResource(R.string.presence_best_day),
            bestDay?.let { stringResource(R.string.presence_best_day_value, weekdayName(it.dateMillis)) }
                ?: stringResource(R.string.presence_no_best_day),
            Color(0xFF71D7B3),
            theme
        )
        InsightRow(Icons.Rounded.AutoAwesome, stringResource(R.string.presence_daily_average), stringResource(R.string.presence_per_day, formatAverage(average)), Color(0xFF45C7DD), theme)
        InsightRow(Icons.AutoMirrored.Rounded.TrendingUp, stringResource(R.string.presence_weekly_trend), trend, Color(0xFF43C77B), theme)
        InsightRow(
            Icons.Rounded.Warning,
            stringResource(R.string.presence_critical_window),
            criticalWindow?.let { stringResource(R.string.presence_critical_window_value, hourText(it.first), hourText(it.first + 3)) }
                ?: stringResource(R.string.presence_no_critical_window),
            Color(0xFFFF9E42),
            theme
        )
        InsightRow(
            Icons.Rounded.AutoAwesome,
            stringResource(R.string.presence_contextual_suggestion),
            contextualSuggestion(stats, events),
            Color(0xFF71D7B3),
            theme
        )
    }
}

@Composable
private fun InsightRow(
    icon: ImageVector,
    title: String,
    value: String,
    accent: Color,
    theme: PresenceStatsTheme
) {
    Row(
        Modifier.fillMaxWidth().background(theme.rowBackground, RoundedCornerShape(8.dp)).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = theme.secondaryText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Text(value, color = theme.primaryText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun DailyEventsCard(
    stats: List<PresenceDayStats>,
    range: Int,
    onRangeChange: (Int) -> Unit,
    theme: PresenceStatsTheme
) {
    val max = stats.maxOfOrNull { maxOf(it.presentes, it.inconscientes) }?.coerceAtLeast(1) ?: 1
    StatsCard(Color(0xFF71D7B3), theme) {
        Text(stringResource(R.string.presence_card_daily_events), color = theme.primaryText, fontWeight = FontWeight.Bold)
        RangePicker(range, onRangeChange, theme)
        Row(
            Modifier.horizontalScroll(rememberScrollState()).height(180.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            stats.forEach { day ->
                Column(
                    Modifier.width(16.dp).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    StatBar(day.presentes, max, Color(0xFF71D7B3))
                    StatBar(day.inconscientes, max, Color(0xFFFFA94D))
                    Text(dayLabel(day.dateMillis), color = theme.secondaryText, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        StatsLegend(theme)
    }
}

@Composable
private fun StatBar(value: Int, max: Int, color: Color) {
    Box(
        Modifier
            .width(7.dp)
            .height(((value.toFloat() / max) * 70f).coerceAtLeast(if (value == 0) 3f else 8f).dp)
            .background(color.copy(alpha = if (value == 0) 0.2f else 0.9f), RoundedCornerShape(3.dp))
    )
}

@Composable
private fun DailyTimelineCard(events: List<PresenceEventPoint>, theme: PresenceStatsTheme) {
    val timelineEvents = events.filter { it.isPresentReturn || it.isAutomaticPilot }
    val width = 960.dp
    StatsCard(Color(0xFF45C7DD), theme) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.presence_today_moments), color = theme.primaryText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(timelineEvents.size.toString(), color = theme.secondaryText, style = MaterialTheme.typography.labelMedium)
        }
        if (timelineEvents.isEmpty()) {
            Text(
                stringResource(R.string.presence_no_events_today),
                color = theme.secondaryText,
                modifier = Modifier.height(90.dp)
            )
        } else {
            Column(Modifier.horizontalScroll(rememberScrollState())) {
                Canvas(Modifier.width(width).height(116.dp)) {
                    val centerY = size.height / 2f
                    for (hour in 0..24) {
                        val x = size.width * hour / 24f
                        drawLine(
                            theme.gridLine.copy(alpha = if (hour == 0 || hour == 12 || hour == 24) 0.22f else 0.1f),
                            Offset(x, 18f),
                            Offset(x, size.height - 18f),
                            1f
                        )
                    }
                    timelineEvents.forEachIndexed { index, event ->
                        val calendar = Calendar.getInstance().apply { timeInMillis = event.createdAtMillis }
                        val minute = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
                        val x = size.width * minute / (24f * 60f)
                        val y = centerY + ((index % 3) - 1) * 15f
                        drawCircle(
                            if (event.isPresentReturn) Color(0xFF71D7B3) else Color(0xFFFFA94D),
                            7f,
                            Offset(x, y)
                        )
                    }
                }
                Row(Modifier.width(width)) {
                    repeat(24) { hour ->
                        Text(
                            "%02d".format(hour),
                            color = theme.secondaryText,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(40.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            StatsLegend(theme)
        }
    }
}

@Composable
private fun RatioCard(
    stats: List<PresenceDayStats>,
    range: Int,
    onRangeChange: (Int) -> Unit,
    theme: PresenceStatsTheme
) {
    val chartWidth = maxOf(320, stats.size * 25).dp
    StatsCard(Color(0xFF45C7DD), theme) {
        Text(stringResource(R.string.presence_ratio_title), color = theme.primaryText, fontWeight = FontWeight.Bold)
        RangePicker(range, onRangeChange, theme)
        Box(Modifier.horizontalScroll(rememberScrollState())) {
            Canvas(Modifier.width(chartWidth).height(130.dp)) {
                drawLine(
                    theme.gridLine.copy(alpha = 0.18f),
                    Offset(0f, size.height / 2f),
                    Offset(size.width, size.height / 2f),
                    1f
                )
                if (stats.isNotEmpty()) {
                    val step = size.width / stats.size
                    var previous: Offset? = null
                    stats.forEachIndexed { index, day ->
                        val point = Offset(
                            step * index + step / 2f,
                            size.height - day.ratioPresencia * (size.height - 18f) - 9f
                        )
                        previous?.let {
                            drawLine(Color(0xFF45C7DD).copy(alpha = 0.55f), it, point, 2f, StrokeCap.Round)
                        }
                        drawCircle(
                            if (day.total == 0) theme.secondaryText.copy(alpha = 0.28f) else Color(0xFF45C7DD),
                            6f,
                            point
                        )
                        previous = point
                    }
                }
            }
        }
        Text(
            stringResource(R.string.presence_ratio_explanation),
            color = theme.secondaryText,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun MoodStatsCard(
    stats: List<PresenceMoodStats>,
    range: Int,
    onRangeChange: (Int) -> Unit,
    theme: PresenceStatsTheme
) {
    StatsCard(Color(0xFFE66AA5), theme) {
        Text(stringResource(R.string.presence_moods_title), color = theme.primaryText, fontWeight = FontWeight.Bold)
        RangePicker(range, onRangeChange, theme)
        if (stats.isEmpty()) {
            Text(stringResource(R.string.presence_no_moods), color = theme.secondaryText)
        } else {
            stats.take(8).forEach { item ->
                Row(
                    Modifier.fillMaxWidth().background(theme.rowBackground, RoundedCornerShape(8.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(localizedPresenceMood(item.moodId, item.title), color = theme.primaryText, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.count.toString(), color = theme.primaryText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RangePicker(range: Int, onRangeChange: (Int) -> Unit, theme: PresenceStatsTheme) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(theme.rowBackground),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf(14, 30, 90).forEach { days ->
            Surface(
                modifier = Modifier.weight(1f).clickable { onRangeChange(days) },
                color = if (range == days) theme.primaryText.copy(alpha = 0.16f) else Color.Transparent,
                shape = RoundedCornerShape(7.dp)
            ) {
                Text(
                    stringResource(R.string.presence_days_format, days),
                    color = theme.primaryText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (range == days) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactRangeMenu(range: Int, onRangeChange: (Int) -> Unit, theme: PresenceStatsTheme) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Text(stringResource(R.string.presence_days_short, range), color = theme.secondaryText, style = MaterialTheme.typography.labelMedium)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            listOf(14, 30, 90).forEach {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.presence_days_format, it)) },
                    onClick = {
                        onRangeChange(it)
                        open = false
                    }
                )
            }
        }
    }
}

@Composable
private fun StatsCard(
    accent: Color,
    theme: PresenceStatsTheme,
    modifier: Modifier = Modifier.fillMaxWidth(),
    cornerRadius: androidx.compose.ui.unit.Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(theme.card)
            .border(1.dp, theme.cardStroke, RoundedCornerShape(cornerRadius))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
private fun StatsLegend(theme: PresenceStatsTheme) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Legend(stringResource(R.string.presence_present), Color(0xFF71D7B3), theme)
        Legend(stringResource(R.string.presence_autopilot), Color(0xFFFFA94D), theme)
    }
}

@Composable
private fun Legend(text: String, color: Color, theme: PresenceStatsTheme) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(9.dp).background(color, CircleShape))
        Text(text, color = theme.secondaryText, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PresenceCelebration(phrase: String) {
    val glow by rememberInfiniteTransition(label = "presenceGlow").animateFloat(
        initialValue = 0.92f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(720, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "presenceGlowValue"
    )
    Box(Modifier.fillMaxSize().background(PresenceCelebrationGradient), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color.White.copy(alpha = 0.14f), 230f * glow, center)
            drawCircle(Color.White.copy(alpha = 0.22f), 140f * glow, center, style = Stroke(2f))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Icon(Icons.Rounded.SelfImprovement, null, tint = Color.White, modifier = Modifier.size(92.dp))
            Text(
                phrase,
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 28.dp)
            )
        }
    }
}

@Composable
private fun PresenceInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.presence_title)) },
        text = {
            Text(stringResource(R.string.presence_info_body))
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) } }
    )
}

private fun List<PresenceEventPoint>.inLastDays(days: Int): List<PresenceEventPoint> {
    val start = PresenceRepository.startOfDay(System.currentTimeMillis()) - (days - 1) * DAY_MILLIS
    return filter { it.dayStartMillis >= start }
}

private fun moodStats(events: List<PresenceEventPoint>): List<PresenceMoodStats> =
    events.mapNotNull { it.moodId?.takeIf(String::isNotBlank) }
        .groupingBy { it }
        .eachCount()
        .map { PresenceMoodStats(it.key, it.value) }
        .sortedByDescending { it.count }

private fun currentWeekStats(stats: List<PresenceDayStats>): List<PresenceDayStats> {
    val calendar = Calendar.getInstance()
    val today = PresenceRepository.startOfDay(System.currentTimeMillis())
    calendar.timeInMillis = today
    val monday = today - ((calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7) * DAY_MILLIS
    return (0..6).map { offset ->
        val date = monday + offset * DAY_MILLIS
        stats.firstOrNull { it.dateMillis == date } ?: PresenceDayStats(date, 0, 0, 0, 0)
    }
}

@Composable
private fun contextualSuggestion(stats: List<PresenceDayStats>, events: List<PresenceEventPoint>): String {
    val present = stats.sumOf { it.presentes }
    val automatic = stats.sumOf { it.inconscientes }
    val total = present + automatic
    val activeDays = stats.count { it.total > 0 }
    if (total == 0) return stringResource(R.string.presence_suggestion_no_data)
    if (activeDays <= maxOf(2, stats.size / 6)) return stringResource(R.string.presence_suggestion_few_days)
    if (present == 0) return stringResource(R.string.presence_suggestion_no_presence)
    if (automatic > present) {
        val automaticEvents = events.filter { it.isAutomaticPilot }
        val window = (0..21 step 3)
            .map { start -> start to automaticEvents.count { hourOf(it.createdAtMillis) in start until start + 3 } }
            .maxByOrNull { it.second }
        if (window != null && window.second >= 2) {
            return stringResource(R.string.presence_suggestion_window, hourText(window.first), hourText(window.first + 3))
        }
        return stringResource(R.string.presence_suggestion_more_autopilot)
    }
    if (automatic == 0) return stringResource(R.string.presence_suggestion_no_autopilot)
    return stringResource(R.string.presence_suggestion_balanced)
}

private fun weekdayName(millis: Long): String =
    SimpleDateFormat("EEEE", Locale.getDefault()).format(millis)

private fun weekdayLetter(millis: Long): String =
    SimpleDateFormat("EEEEE", Locale.getDefault()).format(millis).uppercase(Locale.getDefault())

@Composable
private fun PresenceStatsCard.localizedTitle(): String = stringResource(
    when (this) {
        PresenceStatsCard.TodayReturns -> R.string.presence_card_today
        PresenceStatsCard.CurrentStreak -> R.string.presence_card_streak
        PresenceStatsCard.WeeklyAverage -> R.string.presence_card_weekly_average
        PresenceStatsCard.DominantMood -> R.string.presence_card_dominant_mood
        PresenceStatsCard.PracticalInsights -> R.string.presence_card_practical
        PresenceStatsCard.DailyEvents -> R.string.presence_card_daily_events
        PresenceStatsCard.DailyTimeline -> R.string.presence_card_today_moments
        PresenceStatsCard.Ratio -> R.string.presence_card_ratio
        PresenceStatsCard.Moods -> R.string.presence_card_moods
    }
)

@Composable
private fun localizedPresenceMood(id: String, fallback: String): String {
    val resourceId = when (id) {
        "sientoMiFuturoAhora" -> R.string.presence_mood_future
        "pilotoAutomatico" -> R.string.presence_mood_autopilot
        "distraido" -> R.string.presence_mood_distracted
        "sereno" -> R.string.presence_mood_serene
        "alegre" -> R.string.presence_mood_happy
        "ansioso" -> R.string.presence_mood_anxious
        "triste" -> R.string.presence_mood_sad
        "enfadado" -> R.string.presence_mood_angry
        "cansado" -> R.string.presence_mood_tired
        "agradecido" -> R.string.presence_mood_grateful
        else -> null
    }
    return resourceId?.let { stringResource(it) } ?: fallback
}

private fun dayLabel(millis: Long): String =
    SimpleDateFormat("d", Locale.getDefault()).format(millis)

private fun hourOf(millis: Long): Int =
    Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.HOUR_OF_DAY)

private fun hourText(hour: Int): String = "%02d:00".format(hour.coerceAtMost(24))

private fun formatAverage(value: Double): String =
    if (value.roundToInt().toDouble() == value) value.roundToInt().toString() else "%.1f".format(value)

private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

private val PresenceGradient = Brush.verticalGradient(
    listOf(Color(0xFF37208C), Color(0xFF0E7A93), Color(0xFF20B79B))
)

private val PresenceCelebrationGradient = Brush.verticalGradient(
    listOf(Color(0xFF5730C7), Color(0xFF147AD9), Color(0xFF2BDEBF))
)
