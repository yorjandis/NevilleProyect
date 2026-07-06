package com.ypg.neville.ui.frag

import android.content.res.Configuration
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import com.ypg.neville.model.preferences.DbPreferences
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.db.DatabaseHelper
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.model.subscription.SubscriptionManager
import com.ypg.neville.model.utils.FraseContextActions
import com.ypg.neville.model.utils.UiModalWindows
import com.ypg.neville.model.utils.utilsFields
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone

class FragHome : Fragment() {

    private var initialDisplay: HomeDisplay? = null
    private var mandalaAssetPath: String = ""

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun AlternativeHomeScreen(
        mandalaPath: String,
        todayStartMillis: Long,
        tomorrowStartMillis: Long,
        todayEpochDay: Long,
        nowMillis: Long,
        agendaCountToday: Int,
        agendaIndicatorHiddenDay: Long,
        onAgendaIndicatorHiddenDayChange: (Long) -> Unit
    ) {
        val context = LocalContext.current
        val prefs = remember { DbPreferences.default(context) }
        val isDark = prefs.getBoolean("tema", true)
        val theme = remember(isDark) { AlternativeHomeTheme(isDark) }
        val mandalaBitmap = remember(mandalaPath) {
            runCatching {
                requireContext().assets.open(mandalaPath).use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
        }
        var phrase by remember { mutableStateOf(alternativePhraseForNow()) }
        var accessIds by remember {
            mutableStateOf(
                normalizeAlternativeAccessIds(
                    prefs.getString(PREF_KEY_HOME_ALTERNATIVE_ACCESS_IDS, "").orEmpty()
                )
            )
        }
        var accessGradientIds by remember {
            mutableStateOf(
                normalizeAlternativeGradientIds(
                    prefs.getString(PREF_KEY_HOME_ALTERNATIVE_ACCESS_GRADIENT_IDS, "").orEmpty(),
                    accessIds
                )
            )
        }
        var showEditor by remember { mutableStateOf(false) }
        var presenceCount by remember { mutableStateOf(0) }
        var diaryCount by remember { mutableStateOf(0) }
        val database = remember(context) { NevilleRoomDatabase.getInstance(context.applicationContext) }
        val activeGoalsCount by remember(database) {
            database.goalDao().observeStartedCount()
        }.collectAsState(initial = 0)
        val readyGoalUnitsCount by remember(database, nowMillis) {
            database.goalUnitDao().observeReadyToCheckCount(nowMillis)
        }.collectAsState(initial = 0)
        LaunchedEffect(Unit) {
            homeAlternativePresenceTotalState.value = prefs
                .getInt(PREF_KEY_HOME_ALTERNATIVE_PRESENCE_TOTAL, HOME_ALTERNATIVE_PRESENCE_TOTAL_DEFAULT)
                .coerceAtLeast(HOME_ALTERNATIVE_PRESENCE_TOTAL_DEFAULT)
            homeAlternativeGoalsTotalState.value = prefs
                .getInt(PREF_KEY_HOME_ALTERNATIVE_GOALS_TOTAL, HOME_ALTERNATIVE_GOALS_TOTAL_DEFAULT)
                .coerceAtLeast(HOME_ALTERNATIVE_GOALS_TOTAL_DEFAULT)
            homeAlternativeDiaryTotalState.value = prefs
                .getInt(PREF_KEY_HOME_ALTERNATIVE_DIARY_TOTAL, HOME_ALTERNATIVE_DIARY_TOTAL_DEFAULT)
                .coerceAtLeast(HOME_ALTERNATIVE_DIARY_TOTAL_DEFAULT)
        }
        val presenceProgressTotal = homeAlternativePresenceTotalState.value
        val goalsProgressTotal = homeAlternativeGoalsTotalState.value
        val diaryProgressTotal = homeAlternativeDiaryTotalState.value
        val gridSpacing = HOME_ALTERNATIVE_GRID_SPACING_DP.dp
        val gridSide = (HOME_ALTERNATIVE_CARD_SIZE_DP * 3 + HOME_ALTERNATIVE_GRID_SPACING_DP * 2).dp

        LaunchedEffect(todayStartMillis, tomorrowStartMillis) {
            while (true) {
                val counts = withContext(Dispatchers.IO) {
                    val presence = database.presenceEventDao()
                        .countByTypeBetween("presente", todayStartMillis, tomorrowStartMillis)
                    val diary = database.diarioDao()
                        .getAll()
                        .count { it.fecha >= todayStartMillis && it.fecha < tomorrowStartMillis }
                    presence to diary
                }
                presenceCount = counts.first
                diaryCount = counts.second
                delay(30_000)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(theme.background))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 18.dp, end = 18.dp, top = 58.dp, bottom = 96.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (mandalaBitmap != null) {
                    Image(
                        bitmap = mandalaBitmap,
                        contentDescription = "Mándala Home",
                        modifier = Modifier
                            .size(HOME_ALTERNATIVE_TOP_IMAGE_SIZE_DP.dp)
                            .clip(RoundedCornerShape(HOME_ALTERNATIVE_TOP_IMAGE_CORNER_DP.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.logo_home2),
                        contentDescription = "Logo Home",
                        modifier = Modifier
                            .size(HOME_ALTERNATIVE_TOP_IMAGE_SIZE_DP.dp)
                            .clip(RoundedCornerShape(HOME_ALTERNATIVE_TOP_IMAGE_CORNER_DP.dp)),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(HOME_ALTERNATIVE_TOP_IMAGE_BOTTOM_SPACING_DP.dp))

                Text(
                    text = phrase,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { phrase = alternativePhraseForNow() },
                            onLongClick = { showEditor = true }
                        ),
                    textAlign = TextAlign.Center,
                    fontSize = 23.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.primaryText
                )

                Spacer(modifier = Modifier.height(20.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .width(gridSide)
                        .height(gridSide),
                    userScrollEnabled = false,
                    horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                    verticalArrangement = Arrangement.spacedBy(gridSpacing),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    items(accessIds.indices.toList()) { index ->
                        val access = HomeAlternativeAccess.entries.firstOrNull { it.id == accessIds[index] } ?: return@items
                        val gradient = homeAlternativeGradientForId(accessGradientIds.getOrNull(index), access)
                        AlternativeAccessCard(
                            access = access,
                            colors = gradient.colors,
                            theme = theme,
                            showBadge = access == HomeAlternativeAccess.Agenda &&
                                agendaCountToday > 0 &&
                                agendaIndicatorHiddenDay != todayEpochDay,
                            badgeText = agendaCountToday.coerceAtMost(99).toString(),
                            showWarning = access == HomeAlternativeAccess.Metas && readyGoalUnitsCount > 0,
                            onClick = {
                                openHomeAlternativeAccess(access)
                            },
                            onLongClick = { showEditor = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(theme.divider)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    AlternativeProgressCard(
                        modifier = Modifier.weight(1f),
                        title = "Presencia",
                        value = "$presenceCount eventos",
                        icon = Icons.Rounded.Favorite,
                        progress = (presenceCount.toFloat() / presenceProgressTotal.toFloat()).coerceIn(0f, 1f),
                        colors = listOf(Color(0xFFFFEEA8), Color(0xFFFFB738), Color(0xFFF46F10)),
                        theme = theme
                    )
                    AlternativeProgressCard(
                        modifier = Modifier.weight(1f),
                        title = "Metas",
                        value = "$activeGoalsCount activas",
                        icon = Icons.Rounded.Checklist,
                        progress = (activeGoalsCount.toFloat() / goalsProgressTotal.toFloat()).coerceIn(0f, 1f),
                        colors = listOf(Color(0xFFC2FFC7), Color(0xFF61D67A), Color(0xFF1A9443)),
                        theme = theme
                    )
                    AlternativeProgressCard(
                        modifier = Modifier.weight(1f),
                        title = "Diario",
                        value = "$diaryCount hoy",
                        icon = Icons.Rounded.MenuBook,
                        progress = (diaryCount.toFloat() / diaryProgressTotal.toFloat()).coerceIn(0f, 1f),
                        colors = listOf(Color(0xFFBCFFF5), Color(0xFF4DD2C7), Color(0xFF007F94)),
                        theme = theme
                    )
                }
            }
        }

        if (showEditor) {
            AlternativeAccessEditorDialog(
                accessIds = accessIds,
                accessGradientIds = accessGradientIds,
                theme = theme,
                onDismiss = { showEditor = false },
                onReset = {
                    val resetAccessIds = HOME_ALTERNATIVE_DEFAULT_ACCESS_IDS
                    val resetGradientIds = defaultAlternativeGradientIds(resetAccessIds)
                    accessIds = resetAccessIds
                    accessGradientIds = resetGradientIds
                    prefs.edit {
                        putString(PREF_KEY_HOME_ALTERNATIVE_ACCESS_IDS, encodeAlternativeAccessIds(resetAccessIds))
                        putString(
                            PREF_KEY_HOME_ALTERNATIVE_ACCESS_GRADIENT_IDS,
                            encodeAlternativeGradientIds(resetGradientIds, resetAccessIds)
                        )
                    }
                },
                onChange = { newIds, newGradientIds ->
                    val nextAccessIds = normalizeAlternativeAccessIds(encodeAlternativeAccessIds(newIds))
                    val nextGradientIds = normalizeAlternativeGradientIds(
                        encodeAlternativeGradientIds(newGradientIds, nextAccessIds),
                        nextAccessIds
                    )
                    accessIds = nextAccessIds
                    accessGradientIds = nextGradientIds
                    prefs.edit {
                        putString(PREF_KEY_HOME_ALTERNATIVE_ACCESS_IDS, encodeAlternativeAccessIds(nextAccessIds))
                        putString(
                            PREF_KEY_HOME_ALTERNATIVE_ACCESS_GRADIENT_IDS,
                            encodeAlternativeGradientIds(nextGradientIds, nextAccessIds)
                        )
                    }
                }
            )
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun AlternativeAccessCard(
        access: HomeAlternativeAccess,
        colors: List<Color>,
        theme: AlternativeHomeTheme,
        showBadge: Boolean,
        badgeText: String,
        showWarning: Boolean,
        onClick: () -> Unit,
        onLongClick: () -> Unit
    ) {
        Box {
            Column(
                modifier = Modifier
                    .size(HOME_ALTERNATIVE_CARD_SIZE_DP.dp)
                    .aspectRatio(1f)
                    .shadow(HOME_ALTERNATIVE_CARD_SHADOW_DP.dp, RoundedCornerShape(HOME_ALTERNATIVE_CARD_CORNER_DP.dp))
                    .clip(RoundedCornerShape(HOME_ALTERNATIVE_CARD_CORNER_DP.dp))
                    .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                    .background(Brush.linearGradient(colors))
                    .border(1.dp, theme.cardStroke, RoundedCornerShape(HOME_ALTERNATIVE_CARD_CORNER_DP.dp))
                    .padding(HOME_ALTERNATIVE_CARD_PADDING_DP.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = access.icon,
                    contentDescription = access.title,
                    tint = theme.cardForeground,
                    modifier = Modifier.size(HOME_ALTERNATIVE_CARD_ICON_SIZE_DP.dp)
                )
                Spacer(modifier = Modifier.height(HOME_ALTERNATIVE_CARD_ICON_TEXT_SPACING_DP.dp))
                Text(
                    text = access.title,
                    maxLines = 1,
                    fontSize = HOME_ALTERNATIVE_CARD_TEXT_SIZE_SP.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.cardForeground,
                    textAlign = TextAlign.Center
                )
            }
            if (showBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = HOME_ALTERNATIVE_BADGE_OFFSET_DP.dp, y = (-HOME_ALTERNATIVE_BADGE_OFFSET_DP).dp)
                        .size(HOME_ALTERNATIVE_BADGE_SIZE_DP.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(badgeText, color = Color.White, fontSize = HOME_ALTERNATIVE_BADGE_TEXT_SIZE_SP.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (showWarning) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = HOME_ALTERNATIVE_BADGE_OFFSET_DP.dp, y = (-HOME_ALTERNATIVE_BADGE_OFFSET_DP).dp)
                        .size(HOME_ALTERNATIVE_BADGE_SIZE_DP.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF9800)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("!", color = Color.White, fontSize = HOME_ALTERNATIVE_WARNING_TEXT_SIZE_SP.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }

    @Composable
    private fun AlternativeProgressCard(
        modifier: Modifier,
        title: String,
        value: String,
        icon: ImageVector,
        progress: Float,
        colors: List<Color>,
        theme: AlternativeHomeTheme
    ) {
        val animatedProgress by animateFloatAsState(progress, label = "alt_progress_$title")
        Column(
            modifier = modifier.padding(HOME_ALTERNATIVE_PROGRESS_CARD_PADDING_DP.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(HOME_ALTERNATIVE_PROGRESS_RING_SIZE_DP.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(HOME_ALTERNATIVE_PROGRESS_RING_SIZE_DP.dp),
                    color = theme.progressTrack,
                    strokeWidth = HOME_ALTERNATIVE_PROGRESS_STROKE_WIDTH_DP.dp
                )
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(HOME_ALTERNATIVE_PROGRESS_RING_SIZE_DP.dp),
                    color = colors[1],
                    strokeWidth = HOME_ALTERNATIVE_PROGRESS_STROKE_WIDTH_DP.dp
                )
                Box(
                    modifier = Modifier
                        .size(HOME_ALTERNATIVE_PROGRESS_INNER_CIRCLE_SIZE_DP.dp)
                        .clip(CircleShape)
                        .background(colors.last().copy(alpha = if (theme.isDark) 0.24f else 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = colors.last(),
                        modifier = Modifier.size(HOME_ALTERNATIVE_PROGRESS_ICON_SIZE_DP.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(HOME_ALTERNATIVE_PROGRESS_TEXT_TOP_SPACING_DP.dp))
            Text(
                title,
                color = theme.primaryText,
                fontSize = HOME_ALTERNATIVE_PROGRESS_TITLE_TEXT_SIZE_SP.sp,
                lineHeight = HOME_ALTERNATIVE_PROGRESS_TITLE_LINE_HEIGHT_SP.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                value,
                color = theme.secondaryText,
                fontSize = HOME_ALTERNATIVE_PROGRESS_VALUE_TEXT_SIZE_SP.sp,
                lineHeight = HOME_ALTERNATIVE_PROGRESS_VALUE_LINE_HEIGHT_SP.sp,
                maxLines = 1
            )
        }
    }

    @Composable
    private fun AlternativeAccessEditorDialog(
        accessIds: List<String>,
        accessGradientIds: List<String>,
        theme: AlternativeHomeTheme,
        onDismiss: () -> Unit,
        onReset: () -> Unit,
        onChange: (List<String>, List<String>) -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                Button(onClick = onDismiss) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = onReset) { Text("Restablecer") }
            },
            title = { Text("Accesos") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accessIds.forEachIndexed { index, id ->
                        val access = HomeAlternativeAccess.entries.firstOrNull { it.id == id } ?: return@forEachIndexed
                        val selectedGradient = homeAlternativeGradientForId(accessGradientIds.getOrNull(index), access)
                        var accessExpanded by remember(id, index) { mutableStateOf(false) }
                        var gradientExpanded by remember(id, selectedGradient.id, index) { mutableStateOf(false) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(access.icon, contentDescription = access.title, tint = theme.primaryText, modifier = Modifier.size(20.dp))
                            Text(access.title, modifier = Modifier.weight(1f), color = theme.primaryText, maxLines = 1)
                            Box {
                                Surface(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .clickable { gradientExpanded = true },
                                    shape = CircleShape,
                                    color = Color.Transparent,
                                    contentColor = Color.White
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Brush.linearGradient(selectedGradient.colors))
                                            .border(1.dp, theme.cardStroke, CircleShape)
                                    )
                                }
                                DropdownMenu(expanded = gradientExpanded, onDismissRequest = { gradientExpanded = false }) {
                                    HomeAlternativeGradient.entries.forEach { gradient ->
                                        DropdownMenuItem(
                                            text = { Text(gradient.title) },
                                            leadingIcon = {
                                                Box(
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .clip(CircleShape)
                                                        .background(Brush.linearGradient(gradient.colors))
                                                        .border(1.dp, theme.divider, CircleShape)
                                                )
                                            },
                                            onClick = {
                                                gradientExpanded = false
                                                onChange(
                                                    accessIds,
                                                    accessGradientIds.toMutableList().also { gradients ->
                                                        while (gradients.size <= index) {
                                                            gradients.add(defaultAlternativeGradientId(accessIds[gradients.size]))
                                                        }
                                                        gradients[index] = gradient.id
                                                    }
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = {
                                if (index > 0) {
                                    val nextAccessIds = accessIds.toMutableList().also {
                                        val moved = it.removeAt(index)
                                        it.add(index - 1, moved)
                                    }
                                    val nextGradientIds = currentAlternativeGradientIds(accessGradientIds, accessIds)
                                        .toMutableList()
                                        .also {
                                            val moved = it.removeAt(index)
                                            it.add(index - 1, moved)
                                        }
                                    onChange(nextAccessIds, nextGradientIds)
                                }
                            }) {
                                Icon(Icons.Rounded.ArrowUpward, contentDescription = "Subir", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {
                                if (index < accessIds.lastIndex) {
                                    val nextAccessIds = accessIds.toMutableList().also {
                                        val moved = it.removeAt(index)
                                        it.add(index + 1, moved)
                                    }
                                    val nextGradientIds = currentAlternativeGradientIds(accessGradientIds, accessIds)
                                        .toMutableList()
                                        .also {
                                            val moved = it.removeAt(index)
                                            it.add(index + 1, moved)
                                        }
                                    onChange(nextAccessIds, nextGradientIds)
                                }
                            }) {
                                Icon(Icons.Rounded.ArrowDownward, contentDescription = "Bajar", modifier = Modifier.size(18.dp))
                            }
                            Box {
                                IconButton(onClick = { accessExpanded = true }) {
                                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Cambiar", modifier = Modifier.size(20.dp))
                                }
                                DropdownMenu(expanded = accessExpanded, onDismissRequest = { accessExpanded = false }) {
                                    HomeAlternativeAccess.entries.forEach { candidate ->
                                        DropdownMenuItem(
                                            text = { Text(candidate.title) },
                                            leadingIcon = { Icon(candidate.icon, contentDescription = null) },
                                            onClick = {
                                                accessExpanded = false
                                                val next = accessIds.toMutableList()
                                                val oldValue = next[index]
                                                val existingIndex = next.indexOf(candidate.id)
                                                if (existingIndex >= 0 && existingIndex != index) {
                                                    next[existingIndex] = oldValue
                                                }
                                                next[index] = candidate.id
                                                onChange(next, currentAlternativeGradientIds(accessGradientIds, accessIds))
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    private fun openHomeAlternativeAccess(access: HomeAlternativeAccess) {
        if (access.requiresPremium && !SubscriptionManager.hasActiveSubscriptionNow()) {
            MainActivity.currentInstance()?.showSubscriptionPaywall()
            return
        }
        access.listElementLoaded?.let { frag_listado.elementLoaded = it }
        MainActivity.currentInstance()?.openDestinationAsSheet(access.destinationId)
    }

    private data class AlternativeHomeTheme(val isDark: Boolean) {
        val background: List<Color> = if (isDark) {
            listOf(Color(0xFF050F2E), Color(0xFF071F4A), Color(0xFF030A24))
        } else {
            listOf(Color(0xFFFCFCF8), Color(0xFFEDF9F9))
        }
        val primaryText: Color = if (isDark) Color.White else Color(0xFF0D0F17)
        val secondaryText: Color = if (isDark) Color.White.copy(alpha = 0.72f) else Color(0xB80D0F17)
        val divider: Color = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.10f)
        val cardStroke: Color = if (isDark) Color.White.copy(alpha = 0.36f) else Color.White.copy(alpha = 0.50f)
        val progressTrack: Color = if (isDark) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.14f)
        val cardForeground: Color = if (isDark) Color(0xFF050816) else Color.White
    }

    private enum class HomeAlternativeGradient(
        val id: String,
        val title: String,
        val colors: List<Color>
    ) {
        Ocean("ocean", "Océano", listOf(Color(0xFF2196F3), Color(0xFF00BCD4))),
        Sunrise("sunrise", "Amanecer", listOf(Color(0xFFFFD95A), Color(0xFFFF9800))),
        Mint("mint", "Menta", listOf(Color(0xFF009688), Color(0xFF6DE0B6))),
        Growth("growth", "Crecimiento", listOf(Color(0xFF4CAF50), Color(0xFF8BE28E))),
        Journal("journal", "Diario", listOf(Color(0xFF9C27B0), Color(0xFFE85BA5))),
        VioletInk("violet_ink", "Tinta violeta", listOf(Color(0xFF3F51B5), Color(0xFF8E44AD))),
        Flame("flame", "Fuego", listOf(Color(0xFFE53935), Color(0xFFFF9800))),
        Ritual("ritual", "Ritual", listOf(Color(0xFFE91E63), Color(0xFFFF8A3D))),
        Summary("summary", "Resumen", listOf(Color(0xFF607D8B), Color(0xFF00BCD4))),
        Voice("voice", "Voz", listOf(Color(0xFF00ACC1), Color(0xFF1976D2))),
        Anchor("anchor", "Ancla", listOf(Color(0xFFFF7A92), Color(0xFFE53935))),
        Coherence("coherence", "Coherencia", listOf(Color(0xFF3F51B5), Color(0xFF26A69A))),
        Notes("notes", "Notas", listOf(Color(0xFF00BCD4), Color(0xFF1976D2))),
        Phrase("phrase", "Frases", listOf(Color(0xFFE91E63), Color(0xFF8E44AD))),
        Encyclopedia("encyclopedia", "Enciclopedia", listOf(Color(0xFF00ACC1), Color(0xFF66D9C7))),
        Reflection("reflection", "Reflexión", listOf(Color(0xFFFFD54F), Color(0xFFE85BA5))),
        Evidence("evidence", "Evidencia", listOf(Color(0xFF7E57C2), Color(0xFF26C6DA))),
        Help("help", "Ayuda", listOf(Color(0xFF26A69A), Color(0xFF1976D2))),
        Neville("neville", "Neville", listOf(Color(0xFF8D6E63), Color(0xFFFF9800))),
        Joe("joe", "JD", listOf(Color(0xFF4DB6AC), Color(0xFF1976D2))),
        Bruce("bruce", "Bruce", listOf(Color(0xFF4CAF50), Color(0xFFFFD54F))),
        Gregg("gregg", "Gregg", listOf(Color(0xFF2196F3), Color(0xFF8E44AD)))
    }

    private enum class HomeAlternativeAccess(
        val id: String,
        val title: String,
        val icon: ImageVector,
        val defaultGradient: HomeAlternativeGradient,
        val destinationId: Int,
        val listElementLoaded: String? = null,
        val requiresPremium: Boolean = false
    ) {
        Calma("calma", "Calma", Icons.Rounded.Spa, HomeAlternativeGradient.Ocean, R.id.frag_calm_space, requiresPremium = true),
        Agenda("agenda", "Agenda", Icons.Rounded.CalendarMonth, HomeAlternativeGradient.Sunrise, R.id.frag_agenda, requiresPremium = true),
        Presencia("presencia", "Presencia", Icons.Rounded.SelfImprovement, HomeAlternativeGradient.Mint, R.id.frag_presence, requiresPremium = true),
        Metas("metas", "Metas", Icons.Rounded.Checklist, HomeAlternativeGradient.Growth, R.id.frag_metas, requiresPremium = true),
        Diario("diario", "Diario", Icons.Rounded.MenuBook, HomeAlternativeGradient.Journal, R.id.frag_diario),
        Lienzo("lienzo", "Lienzo", Icons.Rounded.EditNote, HomeAlternativeGradient.VioletInk, R.id.frag_lienzo, requiresPremium = true),
        Recordatorios("recordatorios", "Recordatorios", Icons.Rounded.Notifications, HomeAlternativeGradient.Flame, R.id.frag_reminders, requiresPremium = true),
        Ritual("ritual", "Ritual", Icons.Rounded.WbSunny, HomeAlternativeGradient.Ritual, R.id.frag_morning_dialog, requiresPremium = true),
        Resumen("resumen", "Resumen", Icons.Rounded.GraphicEq, HomeAlternativeGradient.Summary, R.id.frag_weekly_summary, requiresPremium = true),
        Voces("voces", "Voces", Icons.Rounded.Mic, HomeAlternativeGradient.Voice, R.id.frag_voice_recordings, requiresPremium = true),
        Anclas("anclas", "Anclas", Icons.Rounded.Favorite, HomeAlternativeGradient.Anchor, R.id.frag_emotional_anchors, requiresPremium = true),
        Cardio("cardio", "Coherencia", Icons.Rounded.Favorite, HomeAlternativeGradient.Coherence, R.id.frag_cardio_coherence, requiresPremium = true),
        Notas("notas", "Notas", Icons.Rounded.EditNote, HomeAlternativeGradient.Notes, R.id.frag_notas),
        Frases("frases", "Frases", Icons.Rounded.Favorite, HomeAlternativeGradient.Phrase, R.id.frag_listado_frases),
        Enciclopedia("enciclopedia", "Enciclopedia", Icons.Rounded.MenuBook, HomeAlternativeGradient.Encyclopedia, R.id.frag_listado, "enciclopedia"),
        Reflexiones("reflexiones", "Reflexiones", Icons.Rounded.Checklist, HomeAlternativeGradient.Reflection, R.id.frag_listado, "reflexiones"),
        Evidencia("evidencia", "Evidencia", Icons.Rounded.Science, HomeAlternativeGradient.Evidence, R.id.frag_listado, "evidenciaCientifica"),
        Ayudas("ayudas", "Ayudas", Icons.Rounded.HelpOutline, HomeAlternativeGradient.Help, R.id.frag_listado, "ayudas"),
        AutorNeville("autor_neville", "Neville", Icons.Rounded.Person, HomeAlternativeGradient.Neville, R.id.frag_neville_goddard),
        AutorJoe("autor_jd", "JD", Icons.Rounded.Psychology, HomeAlternativeGradient.Joe, R.id.frag_joe_dispenza),
        AutorBruce("autor_bruce", "Bruce", Icons.Rounded.Spa, HomeAlternativeGradient.Bruce, R.id.frag_bruce_lipton),
        AutorGregg("autor_gregg", "Gregg", Icons.Rounded.GraphicEq, HomeAlternativeGradient.Gregg, R.id.frag_gregg)
    }

    private fun normalizeAlternativeAccessIds(stored: String): List<String> {
        val available = HomeAlternativeAccess.entries.map { it.id }
        val decoded = stored.split(",")
            .map { it.trim() }
            .filter { it in available }
        val result = mutableListOf<String>()
        decoded.forEach { id ->
            if (id !in result) result.add(id)
        }
        (HOME_ALTERNATIVE_DEFAULT_ACCESS_IDS + available).forEach { id ->
            if (result.size < HOME_ALTERNATIVE_GRID_SIZE && id !in result) result.add(id)
        }
        return result.take(HOME_ALTERNATIVE_GRID_SIZE)
    }

    private fun encodeAlternativeAccessIds(ids: List<String>): String {
        return normalizeAlternativeAccessIds(ids.joinToString(",")).joinToString(",")
    }

    private fun defaultAlternativeGradientId(accessId: String): String {
        return HomeAlternativeAccess.entries
            .firstOrNull { it.id == accessId }
            ?.defaultGradient
            ?.id
            ?: HomeAlternativeGradient.Ocean.id
    }

    private fun defaultAlternativeGradientIds(accessIds: List<String>): List<String> {
        return accessIds.map(::defaultAlternativeGradientId)
    }

    private fun normalizeAlternativeGradientIds(stored: String, accessIds: List<String>): List<String> {
        val available = HomeAlternativeGradient.entries.map { it.id }
        val decoded = stored.split(",").map { it.trim() }
        return accessIds.take(HOME_ALTERNATIVE_GRID_SIZE).mapIndexed { index, accessId ->
            decoded.getOrNull(index)
                ?.takeIf { it in available }
                ?: defaultAlternativeGradientId(accessId)
        }
    }

    private fun currentAlternativeGradientIds(gradientIds: List<String>, accessIds: List<String>): List<String> {
        return normalizeAlternativeGradientIds(gradientIds.joinToString(","), accessIds)
    }

    private fun encodeAlternativeGradientIds(gradientIds: List<String>, accessIds: List<String>): String {
        return currentAlternativeGradientIds(gradientIds, accessIds).joinToString(",")
    }

    private fun homeAlternativeGradientForId(
        gradientId: String?,
        access: HomeAlternativeAccess
    ): HomeAlternativeGradient {
        return HomeAlternativeGradient.entries.firstOrNull { it.id == gradientId } ?: access.defaultGradient
    }

    private fun alternativePhraseForNow(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val phrases = when (hour) {
            in 5..11 -> HOME_ALTERNATIVE_MORNING_PHRASES
            in 12..19 -> HOME_ALTERNATIVE_AFTERNOON_PHRASES
            else -> HOME_ALTERNATIVE_NIGHT_PHRASES
        }
        return phrases.random()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initialDisplay = loadInitialState()
        mandalaAssetPath = getOrCreateSessionMandalaAsset()

        (view as ComposeView).setContent {
            com.ypg.neville.ui.theme.NevilleTheme {
                HomeScreen(initial = initialDisplay, mandalaPath = mandalaAssetPath)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        runCatching { MainActivity.currentInstance()?.icToolsBarFraseAdd?.visibility = View.VISIBLE }
        runCatching { MainActivity.currentInstance()?.icToolsBarFav?.visibility = View.GONE }
    }

    override fun onStop() {
        super.onStop()
        runCatching { MainActivity.currentInstance()?.icToolsBarFraseAdd?.visibility = View.GONE }
        runCatching { MainActivity.currentInstance()?.icToolsBarFav?.visibility = View.GONE }
        utilsFields.ID_row_ofElementLoad = -1
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun HomeScreen(initial: HomeDisplay?, mandalaPath: String) {
        val context = LocalContext.current
        val activityContext = remember { this@FragHome.requireActivity() }
        val prefs = remember { DbPreferences.default(context) }
        val configuration = LocalConfiguration.current
        val showTopImage = configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
        val mandalaBitmap = remember(mandalaPath) {
            runCatching {
                requireContext().assets.open(mandalaPath).use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
        }

        var frase by remember(initial) { mutableStateOf(initial?.frase.orEmpty()) }
        var autor by remember(initial) { mutableStateOf(initial?.autor.orEmpty()) }
        var fuente by remember(initial) { mutableStateOf(initial?.fuente.orEmpty()) }
        var favState by remember(initial) { mutableStateOf(initial?.fav ?: "0") }
        var idFrase by remember(initial) { mutableLongStateOf(initial?.id ?: 0L) }
        var hideInlineControls by remember { mutableStateOf(prefs.getBoolean("hide_frase_controles", false)) }
        var showFraseMenu by remember { mutableStateOf(false) }
        var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
        var ritualCompletedToday by remember { mutableStateOf(false) }
        var showAgendaShortcut by remember {
            mutableStateOf(prefs.getBoolean(PREF_KEY_AGENDA_HOME_BUTTON_ENABLED, true))
        }
        var showPresenceShortcut by remember {
            mutableStateOf(prefs.getBoolean(PREF_KEY_PRESENCE_HOME_BUTTON_ENABLED, true))
        }
        var isPlayStoreUpdateAvailable by remember { mutableStateOf(false) }
        var agendaIndicatorHiddenDay by remember {
            mutableLongStateOf(prefs.getLong(PREF_KEY_AGENDA_INDICATOR_HIDDEN_DAY, -1L))
        }

        LaunchedEffect(Unit) {
            runCatching {
                val appUpdateManager = AppUpdateManagerFactory.create(context.applicationContext)
                appUpdateManager.appUpdateInfo
                    .addOnSuccessListener { appUpdateInfo ->
                        isPlayStoreUpdateAvailable =
                            appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    }
                    .addOnFailureListener {
                        isPlayStoreUpdateAvailable = false
                    }
            }.onFailure {
                isPlayStoreUpdateAvailable = false
            }
        }

        LaunchedEffect(Unit) {
            while (true) {
                nowMs = System.currentTimeMillis()
                delay(30_000)
            }
        }

        val textSize = (prefs.getString("fuente_frase", "28")?.toFloatOrNull() ?: 28f).coerceIn(16f, 40f)
        val textColor = prefs.getInt("color_letra_frases_home", prefs.getInt("color_letra_frases", 0))
        val bgColorA = prefs.getInt("color_fondo_a", 0xFFC69FF9.toInt())
        val bgColorB = prefs.getInt("color_fondo_b", 0xFFC4AA8E.toInt())
        val nowMillis = nowMs
        val offsetMillis = TimeZone.getDefault().getOffset(nowMillis).toLong()
        val todayEpochDay = Math.floorDiv(nowMillis + offsetMillis, 86_400_000L)
        val (todayStartMillis, tomorrowStartMillis) = remember(todayEpochDay) {
            java.util.Calendar.getInstance().run {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
                val start = timeInMillis
                add(java.util.Calendar.DAY_OF_MONTH, 1)
                start to timeInMillis
            }
        }
        val agendaCountToday by remember(todayStartMillis, tomorrowStartMillis) {
            NevilleRoomDatabase.getInstance(context.applicationContext)
                .agendaItemDao()
                .observeCountBetween(todayStartMillis, tomorrowStartMillis)
        }.collectAsState(initial = 0)
        val showAgendaIndicator =
            agendaCountToday > 0 && agendaIndicatorHiddenDay != todayEpochDay
        LaunchedEffect(Unit) {
            if (!prefs.getBoolean(PREF_KEY_RITUAL_HIDDEN_DAY_RESET_DONE, false)) {
                prefs.edit {
                    remove(PREF_KEY_RITUAL_BUTTON_HIDDEN_DAY)
                    putBoolean(PREF_KEY_RITUAL_HIDDEN_DAY_RESET_DONE, true)
                }
            }
        }
        val hiddenDay = prefs.getLong(PREF_KEY_RITUAL_BUTTON_HIDDEN_DAY, -1L)
        val isHiddenToday = hiddenDay == todayEpochDay
        val triggerMsToday = remember(todayEpochDay) {
            java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 3)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        LaunchedEffect(todayEpochDay, nowMs) {
            ritualCompletedToday = withContext(Dispatchers.IO) {
                NevilleRoomDatabase.getInstance(context.applicationContext)
                    .morningDialogDao()
                    .getByDay(todayEpochDay)
                    ?.completed == true
            }
        }
        val showRitualShortcut = !isHiddenToday &&
            !ritualCompletedToday &&
            nowMs >= triggerMsToday
        val homeAlternativeEnabled = homeAlternativeEnabledState.value

        Crossfade(
            targetState = homeAlternativeEnabled,
            animationSpec = tween(durationMillis = 360),
            label = "home_mode_crossfade"
        ) { useAlternativeHome ->
            if (useAlternativeHome) {
                AlternativeHomeScreen(
                    mandalaPath = mandalaPath,
                    todayStartMillis = todayStartMillis,
                    tomorrowStartMillis = tomorrowStartMillis,
                    todayEpochDay = todayEpochDay,
                    nowMillis = nowMillis,
                    agendaCountToday = agendaCountToday,
                    agendaIndicatorHiddenDay = agendaIndicatorHiddenDay,
                    onAgendaIndicatorHiddenDayChange = { agendaIndicatorHiddenDay = it }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(bgColorA), Color(bgColorB))
                            )
                        )
                ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (showTopImage) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 56.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (mandalaBitmap != null) {
                            Image(
                                bitmap = mandalaBitmap,
                                contentDescription = "Mándala Home",
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(20.dp)),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.logo_home2),
                                contentDescription = "Logo Home",
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(20.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }

                        if (isPlayStoreUpdateAvailable) {
                            Text(
                                text = getString(R.string.play_store_update_available_home),
                                modifier = Modifier
                                    .padding(top = 10.dp)
                                    .clickable { openPlayStoreListing() },
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0B3D5C)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val noRipple = remember { MutableInteractionSource() }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = frase,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 10.dp, end = 10.dp)
                                .heightIn(max = 420.dp)
                                .verticalScroll(rememberScrollState())
                                .combinedClickable(
                                    interactionSource = noRipple,
                                    indication = null,
                                    onClick = {
                                        val startMode = prefs.getString("list_start_load", "Nada") ?: "Nada"
                                        val loaded = loadFrase(startMode.contains("Frase_fav_azar"))
                                        if (loaded != null) {
                                            frase = loaded.frase
                                            autor = loaded.autor
                                            fuente = loaded.fuente
                                            favState = loaded.fav
                                            idFrase = loaded.id
                                        }
                                    },
                                    onLongClick = {
                                        if (frase.isNotBlank()) {
                                            showFraseMenu = true
                                        }
                                    }
                                ),
                            textAlign = TextAlign.Center,
                            fontSize = textSize.sp,
                            lineHeight = (textSize * 1.38f).sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold,
                            color = if (textColor != 0) Color(textColor) else Color.Black
                        )

                        FraseOptionsMenu(
                            expanded = showFraseMenu,
                            onDismiss = { showFraseMenu = false },
                            favoriteOptionLabel = if (favState == "1") "Quitar de Favoritas" else "Agregar a Favoritas",
                            onToggleFavorito = {
                                if (idFrase > 0) {
                                    val result = utilsDB.UpdateFavorito(
                                        context,
                                        DatabaseHelper.T_Frases,
                                        DatabaseHelper.CC_id,
                                        "",
                                        idFrase.toInt()
                                    )
                                    if (result.isNotEmpty()) {
                                        favState = result
                                    }
                                }
                            },
                            onConvertirNota = {
                                val result = FraseContextActions.convertirFraseEnNota(activityContext, frase)
                                if (result.ok) {
                                    Toast.makeText(activityContext, "Nota creada: ${result.titulo}", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(activityContext, "No se pudo crear la nota", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onCargarLienzo = {
                                FraseContextActions.cargarFraseEnLienzo(activityContext, frase)
                            },
                            onCompartirSistema = {
                                FraseContextActions.compartirFraseSistema(
                                    context = activityContext,
                                    frase = frase,
                                    autor = autor,
                                    fuente = fuente
                                )
                            },
                            onAbrirNotaFrase = {
                                FraseContextActions.abrirNotaDeFrase(activityContext, frase)
                            },
                            onCrearNuevaFrase = {
                                UiModalWindows.Add_New_frase(activityContext, null)
                            }
                        )
                    }

                    Text(
                        text = "<$autor>",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 10.dp),
                        textAlign = TextAlign.End,
                        fontSize = 18.sp,
                        fontStyle = FontStyle.Italic
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 10.dp, top = 2.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = if (hideInlineControls) R.drawable.ic_abajo else R.drawable.ic_arriba),
                            contentDescription = getString(R.string.mostrar_ocultar_controles_frase),
                            modifier = Modifier
                                .size(25.dp)
                                .clickable {
                                    hideInlineControls = !hideInlineControls
                                    prefs.edit { putBoolean("hide_frase_controles", hideInlineControls) }
                                }
                        )
                    }

                    if (!hideInlineControls) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 15.dp, end = 20.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val result = utilsDB.UpdateFavorito(
                                        context,
                                        DatabaseHelper.T_Frases,
                                        DatabaseHelper.CC_id,
                                        "",
                                        idFrase.toInt()
                                    )
                                    if (result.isNotEmpty()) {
                                        favState = result
                                    }
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(Color.Transparent)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_toolbar_favorite),
                                    contentDescription = "Favorito",
                                    tint = if (favState == "1") colorResource(id = R.color.fav_active) else colorResource(id = R.color.fav_inactive)
                                )
                            }

                        }
                    }
                }

                if (showAgendaShortcut || showRitualShortcut || showPresenceShortcut) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = 84.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showAgendaShortcut) {
                            AgendaShortcutButton(
                                agendaCountToday = agendaCountToday,
                                showIndicator = showAgendaIndicator,
                                onOpenAgenda = {
                                    MainActivity.currentInstance()?.openDestinationAsSheet(R.id.frag_agenda)
                                },
                                onToggleIndicatorToday = {
                                    if (showAgendaIndicator) {
                                        agendaIndicatorHiddenDay = todayEpochDay
                                        prefs.edit {
                                            putLong(PREF_KEY_AGENDA_INDICATOR_HIDDEN_DAY, todayEpochDay)
                                        }
                                    } else {
                                        agendaIndicatorHiddenDay = -1L
                                        prefs.edit {
                                            remove(PREF_KEY_AGENDA_INDICATOR_HIDDEN_DAY)
                                        }
                                    }
                                },
                                onHidePermanently = {
                                    showAgendaShortcut = false
                                    prefs.edit { putBoolean(PREF_KEY_AGENDA_HOME_BUTTON_ENABLED, false) }
                                }
                            )
                        }
                        if (showPresenceShortcut) {
                            PresenceShortcutButton(
                                onOpenPresence = {
                                    MainActivity.currentInstance()?.openDestinationAsSheet(R.id.frag_presence)
                                },
                                onHidePermanently = {
                                    showPresenceShortcut = false
                                    prefs.edit { putBoolean(PREF_KEY_PRESENCE_HOME_BUTTON_ENABLED, false) }
                                }
                            )
                        }
                        if (showRitualShortcut) {
                            RitualShortcutButton(
                                onOpenRitual = {
                                    MainActivity.currentInstance()?.openDestinationAsSheet(R.id.frag_morning_dialog)
                                },
                                onHideToday = {
                                    prefs.edit { putLong(PREF_KEY_RITUAL_BUTTON_HIDDEN_DAY, todayEpochDay) }
                                }
                            )
                        }
                    }
                }
            }
        }
            }
        }
    }

    @Composable
    private fun RitualShortcutButton(
        modifier: Modifier = Modifier,
        onOpenRitual: () -> Unit,
        onHideToday: () -> Unit
    ) {
        var showMenu by remember { mutableStateOf(false) }
        val transition = rememberInfiniteTransition(label = "ritual_button_pulse")
        val glow by transition.animateFloat(
            initialValue = 0.82f,
            targetValue = 0.98f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1600),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ritual_button_alpha"
        )

        Box(modifier = modifier) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier
                    .shadow(20.dp, RoundedCornerShape(26.dp)),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .combinedClickable(
                            onClick = onOpenRitual,
                            onLongClick = { showMenu = true }
                        )
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFDDF2FF),
                                    Color(0xFFAEDCF5)
                                )
                            ),
                            shape = RoundedCornerShape(26.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_calendar_toggle),
                        contentDescription = "Ritual del día",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ritual",
                        color = Color.Black,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                DropdownMenuItem(
                    text = { Text("Ocultar hoy") },
                    onClick = {
                        showMenu = false
                        onHideToday()
                    }
                )
            }
        }
    }

    @Composable
    private fun AgendaShortcutButton(
        agendaCountToday: Int,
        showIndicator: Boolean,
        onOpenAgenda: () -> Unit,
        onToggleIndicatorToday: () -> Unit,
        onHidePermanently: () -> Unit
    ) {
        var showMenu by remember { mutableStateOf(false) }

        Box {
            Surface(
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.shadow(14.dp, RoundedCornerShape(26.dp)),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .combinedClickable(
                            onClick = onOpenAgenda,
                            onLongClick = { showMenu = true }
                        )
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFDDF2FF),
                                    Color(0xFFAEDCF5)
                                )
                            ),
                            shape = RoundedCornerShape(26.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_calendar_toggle),
                        contentDescription = "Agenda",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Agenda",
                        color = Color.Black,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (showIndicator) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 7.dp, y = (-7).dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD32F2F)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = agendaCountToday.toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                if (agendaCountToday > 0) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (showIndicator) {
                                    "Ocultar indicador hoy"
                                } else {
                                    "Mostrar indicador hoy"
                                }
                            )
                        },
                        onClick = {
                            showMenu = false
                            onToggleIndicatorToday()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Ocultar permanentemente") },
                    onClick = {
                        showMenu = false
                        onHidePermanently()
                    }
                )
            }
        }
    }

    @Composable
    private fun PresenceShortcutButton(
        onOpenPresence: () -> Unit,
        onHidePermanently: () -> Unit
    ) {
        var showMenu by remember { mutableStateOf(false) }

        Box {
            Surface(
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.shadow(14.dp, RoundedCornerShape(26.dp)),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .combinedClickable(
                            onClick = onOpenPresence,
                            onLongClick = { showMenu = true }
                        )
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFDDF2FF),
                                    Color(0xFFAEDCF5)
                                )
                            ),
                            shape = RoundedCornerShape(26.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_show),
                        contentDescription = "Presencia",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Presencia",
                        color = Color.Black,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                DropdownMenuItem(
                    text = { Text("Ocultar permanentemente") },
                    onClick = {
                        showMenu = false
                        onHidePermanently()
                    }
                )
            }
        }
    }

    private data class HomeDisplay(
        val id: Long,
        val frase: String,
        val autor: String,
        val fuente: String,
        val fav: String
    )

    private fun loadInitialState(): HomeDisplay? {
        val prefs = DbPreferences.default(requireContext())
        var startMode = prefs.getString("list_start_load", "")
        if (startMode.isNullOrEmpty()) {
            prefs.edit { putString("list_start_load", "Frase_azar") }
            startMode = "Frase_azar"
        }

        return when (startMode) {
            "Ultima_frase_vista" -> {
                val idUltimaFrase = prefs.getString(utilsFields.SETTING_KEY_ID_ULTIMA_FRASE, "0")
                val frase = utilsDB.getFraseById(requireContext(), idUltimaFrase?.toLongOrNull() ?: 0)
                frase?.let {
                    utilsFields.ID_row_ofElementLoad = it.id.toInt()
                    utilsFields.ID_Str_row_ofElementLoad = it.frase
                    HomeDisplay(it.id, it.frase, it.autor, it.fuente, it.favState())
                }
            }

            "Frase_azar" -> loadFrase(false)
            "Frase_fav_azar" -> loadFrase(true)
            "Conf_azar" -> {
                loadConfAzar(false)
                null
            }

            "Conf_fav_azar" -> {
                loadConfAzar(true)
                null
            }

            "Ultima_conf_vista" -> {
                utilsFields.ID_Str_row_ofElementLoad =
                    prefs.getString(utilsFields.SETTING_KEY_ULTIMA_CONFERENCIA, "") ?: ""

                if (utilsFields.ID_Str_row_ofElementLoad.isNotEmpty()) {
                    FragContentWebView.extension = ".txt"
                    FragContentWebView.urlDirAssets = "autores/neville/conf"
                    val confFileName =
                        FragContentWebView.confAssetFileNameFromTitle(utilsFields.ID_Str_row_ofElementLoad)
                    FragContentWebView.urlPath =
                        "file:///android_asset/${FragContentWebView.urlDirAssets}/$confFileName${FragContentWebView.extension}"
                    MainActivity.currentInstance()?.openDestinationAsSheet(R.id.frag_content_webview)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Debe cargar al menos una conferencia en Texto",
                        Toast.LENGTH_SHORT
                    ).show()
                    frag_listado.elementLoaded = "autores/neville/conf"
                    MainActivity.currentInstance()?.openDestinationAsSheet(R.id.frag_listado)
                }
                null
            }

            else -> loadFrase(false)
        }
    }

    private fun loadFrase(isFavList: Boolean): HomeDisplay? {
        val frase = utilsDB.getRandomFrase(requireContext(), isFavList)
        val prefs = DbPreferences.default(requireContext())

        return if (frase != null) {
            utilsFields.ID_row_ofElementLoad = frase.id.toInt()
            utilsFields.ID_Str_row_ofElementLoad = frase.frase
            prefs.edit { putString(utilsFields.SETTING_KEY_ID_ULTIMA_FRASE, frase.id.toString()) }
            HomeDisplay(frase.id, frase.frase, frase.autor, frase.fuente, frase.favState())
        } else {
            val hasAnyFilter = prefs.getBoolean("home_filter_autores", true) ||
                prefs.getBoolean("home_filter_otros", true) ||
                prefs.getBoolean("home_filter_salud", true)
            Toast.makeText(
                requireContext(),
                if (hasAnyFilter) {
                    "No hay frase para mostrar con el filtro actual"
                } else {
                    "No hay categorías activas. Activa al menos una en Ajustes"
                },
                Toast.LENGTH_SHORT
            ).show()
            null
        }
    }

    private fun loadConfAzar(isFav: Boolean) {
        val conf = utilsDB.getRandomConf(requireContext(), isFav)
        val prefs = DbPreferences.default(requireContext())

        if (conf != null) {
            utilsFields.ID_Str_row_ofElementLoad = conf.title
            FragContentWebView.extension = ".txt"
            FragContentWebView.urlDirAssets = "autores/neville/conf"
            val confFileName = FragContentWebView.confAssetFileNameFromTitle(conf.title)
            FragContentWebView.urlPath =
                "file:///android_asset/${FragContentWebView.urlDirAssets}/$confFileName${FragContentWebView.extension}"
            MainActivity.currentInstance()?.openDestinationAsSheet(R.id.frag_content_webview)
        } else {
            Toast.makeText(
                requireContext(),
                "No hay Conferencia favorita para mostrar. Cargando Conferencias inbuilt",
                Toast.LENGTH_SHORT
            ).show()
            prefs.edit { putString("list_start_load", "Conf_azar") }
        }
    }

    private fun getOrCreateSessionMandalaAsset(): String {
        sessionMandalaAssetPath?.let { return it }
        val randomIndex = (1..14).random()
        val selected = "mandalas/m_$randomIndex.jpg"
        sessionMandalaAssetPath = selected
        return selected
    }

    private fun openPlayStoreListing() {
        val packageName = requireContext().packageName
        val marketIntent = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
        val fallbackIntent = Intent(
            Intent.ACTION_VIEW,
            "https://play.google.com/store/apps/details?id=$packageName".toUri()
        )

        runCatching {
            startActivity(marketIntent)
        }.onFailure {
            startActivity(fallbackIntent)
        }
    }

    companion object {
        private var sessionMandalaAssetPath: String? = null
        val homeAlternativeEnabledState = mutableStateOf(false)
        val homeAlternativePresenceTotalState = mutableStateOf(HOME_ALTERNATIVE_PRESENCE_TOTAL_DEFAULT)
        val homeAlternativeGoalsTotalState = mutableStateOf(HOME_ALTERNATIVE_GOALS_TOTAL_DEFAULT)
        val homeAlternativeDiaryTotalState = mutableStateOf(HOME_ALTERNATIVE_DIARY_TOTAL_DEFAULT)
        const val PREF_KEY_AGENDA_HOME_BUTTON_ENABLED = "agenda_home_button_enabled"
        const val PREF_KEY_PRESENCE_HOME_BUTTON_ENABLED = "presence_home_button_enabled"
        const val PREF_KEY_HOME_ALTERNATIVE_ENABLED = "home_alternative_enabled"
        const val PREF_KEY_HOME_ALTERNATIVE_PRESENCE_TOTAL = "home_alternative_presence_total"
        const val PREF_KEY_HOME_ALTERNATIVE_GOALS_TOTAL = "home_alternative_goals_total"
        const val PREF_KEY_HOME_ALTERNATIVE_DIARY_TOTAL = "home_alternative_diary_total"
        const val HOME_ALTERNATIVE_PRESENCE_TOTAL_DEFAULT = 5
        const val HOME_ALTERNATIVE_GOALS_TOTAL_DEFAULT = 1
        const val HOME_ALTERNATIVE_DIARY_TOTAL_DEFAULT = 1
        private const val PREF_KEY_HOME_ALTERNATIVE_ACCESS_IDS = "home_alternative_access_ids"
        private const val PREF_KEY_HOME_ALTERNATIVE_ACCESS_GRADIENT_IDS = "home_alternative_access_gradient_ids"
        private const val PREF_KEY_AGENDA_INDICATOR_HIDDEN_DAY = "agenda_indicator_hidden_day"
        private const val PREF_KEY_RITUAL_BUTTON_HIDDEN_DAY = "morning_ritual_button_hidden_day"
        private const val PREF_KEY_RITUAL_HIDDEN_DAY_RESET_DONE = "morning_ritual_hidden_day_reset_done"
        private const val HOME_ALTERNATIVE_GRID_SIZE = 9
        //Imagen top
        private const val HOME_ALTERNATIVE_TOP_IMAGE_SIZE_DP = 80
        private const val HOME_ALTERNATIVE_TOP_IMAGE_CORNER_DP = 12
        private const val HOME_ALTERNATIVE_TOP_IMAGE_BOTTOM_SPACING_DP = 35
        //Cuadrícula de accesos
        private const val HOME_ALTERNATIVE_CARD_SIZE_DP = 85
        private const val HOME_ALTERNATIVE_GRID_SPACING_DP = 25
        private const val HOME_ALTERNATIVE_CARD_CORNER_DP = 14
        private const val HOME_ALTERNATIVE_CARD_SHADOW_DP = 5
        private const val HOME_ALTERNATIVE_CARD_PADDING_DP = 5
        private const val HOME_ALTERNATIVE_CARD_ICON_SIZE_DP = 34
        private const val HOME_ALTERNATIVE_CARD_ICON_TEXT_SPACING_DP = 5
        private const val HOME_ALTERNATIVE_CARD_TEXT_SIZE_SP = 12
        private const val HOME_ALTERNATIVE_BADGE_SIZE_DP = 25
        private const val HOME_ALTERNATIVE_BADGE_OFFSET_DP = 0
        private const val HOME_ALTERNATIVE_BADGE_TEXT_SIZE_SP = 10
        private const val HOME_ALTERNATIVE_WARNING_TEXT_SIZE_SP = 10
        //Indicadores de Progreso
        private const val HOME_ALTERNATIVE_PROGRESS_CARD_PADDING_DP = 10
        private const val HOME_ALTERNATIVE_PROGRESS_RING_SIZE_DP = 70
        private const val HOME_ALTERNATIVE_PROGRESS_STROKE_WIDTH_DP = 4
        private const val HOME_ALTERNATIVE_PROGRESS_INNER_CIRCLE_SIZE_DP = 45
        private const val HOME_ALTERNATIVE_PROGRESS_ICON_SIZE_DP = 25
        private const val HOME_ALTERNATIVE_PROGRESS_TEXT_TOP_SPACING_DP = 7
        private const val HOME_ALTERNATIVE_PROGRESS_TITLE_TEXT_SIZE_SP = 18
        private const val HOME_ALTERNATIVE_PROGRESS_TITLE_LINE_HEIGHT_SP = 15
        private const val HOME_ALTERNATIVE_PROGRESS_VALUE_TEXT_SIZE_SP = 13
        private const val HOME_ALTERNATIVE_PROGRESS_VALUE_LINE_HEIGHT_SP = 13
        private val HOME_ALTERNATIVE_DEFAULT_ACCESS_IDS = listOf(
            "calma",
            "agenda",
            "presencia",
            "metas",
            "diario",
            "lienzo",
            "recordatorios",
            "ritual",
            "cardio"
        )
        private val HOME_ALTERNATIVE_MORNING_PHRASES = listOf(
            "Meditar en la mañana organiza tu energía",
            "Hoy despiertas en un nuevo estado",
            "Empieza el día desde la versión que eliges ser",
            "Tu mañana obedece a la historia que aceptas",
            "Asume temprano lo que deseas vivir",
            "Tu atención abre el camino del día",
            "Respira, elige y crea desde adentro",
            "Este día responde a tu nueva identidad",
            "Tu cuerpo escucha la intención con la que comienzas",
            "Entra al día como quien ya lo logró",
            "La primera imagen interna dirige tus pasos",
            "Hoy practicas el futuro que quieres habitar",
            "Cada amanecer puede reeducar tu mente",
            "Empieza en calma y el mundo se ordena",
            "Tu percepción de hoy cambia tu biología",
            "El día nace desde el estado que sostienes",
            "Imagina con fe antes de actuar",
            "Tu energía de inicio marca la dirección",
            "Hoy eliges presencia antes que pasado",
            "Declara internamente quién eres ahora",
            "La mañana es tu primer acto creador",
            "Empieza desde tu mejor versión",
            "Hoy eliges quién ser",
            "Tu momento es ahora",
            "Avanza hoy con intención",
            "Recuerda, todo comienza en ti",
            "Hoy dirige tu energía sabiamente",
            "Hoy siembras tu futuro",
            "Actúa desde tu visión futura",
            "Comienza este día con propósito",
            "Hoy eliges tu estado interior",
            "Hoy construyes desde la calma",
            "Hoy lideras tu experiencia",
            "Honra este nuevo comienzo",
            "Hoy conviertes intención en acción"
        )
        private val HOME_ALTERNATIVE_AFTERNOON_PHRASES = listOf(
            "Vuelve al estado que elegiste al comenzar",
            "A mitad del día también puedes reiniciar",
            "Tu atención puede cambiar el rumbo ahora",
            "Respira y regresa a tu versión elevada",
            "Cada pausa es una puerta a otro estado",
            "Sostén la visión mientras actúas",
            "Tu cuerpo aprende de la emoción que repites",
            "Elige coherencia en medio del movimiento",
            "Lo externo no manda sobre tu estado",
            "Ahora puedes pensar desde el resultado",
            "Tu tarde se transforma con una nueva percepción",
            "La intención se fortalece con presencia",
            "Haz una cosa desde tu identidad elegida",
            "No negocies con el viejo hábito",
            "Tu mundo cambia cuando vuelves a ti",
            "Siente el resultado antes de perseguirlo",
            "Una emoción elevada reorganiza el día",
            "Actúa como quien ya recuerda su poder",
            "La tarde aún tiene espacio para crear",
            "Tu siguiente elección también cuenta",
            "Vuelve al momento presente",
            "Has una pausa, respira y continúa",
            "Regresa a tu centro",
            "Mantén viva tu intención",
            "Elige calma otra vez",
            "Este instante también importa",
            "Observa antes de reaccionar",
            "Sigue creando conscientemente",
            "Tu poder sigue aquí, contigo",
            "Vuelve a lo esencial",
            "Permanece presente",
            "Conecta con tu propósito",
            "Una pausa puede cambiarlo todo",
            "Recuerda quién estás siendo",
            "Aún puedes elegir",
            "Vuelve a sentir plenitud",
            "Crea desde este instante",
            "Recupera tu enfoque",
            "Sigue alineado contigo"
        )
        private val HOME_ALTERNATIVE_NIGHT_PHRASES = listOf(
            "Cierra el día aceptando tu nuevo estado",
            "La noche integra lo que decides creer",
            "Antes de dormir, habita el resultado",
            "Tu imaginación prepara el mañana",
            "Suelta el día y conserva la visión",
            "Descansa en la identidad que estás creando",
            "La calma nocturna reeduca el cuerpo",
            "Tu subconsciente escucha lo que sientes real",
            "Duerme como quien ya recibió",
            "La gratitud sella una nueva percepción",
            "Esta noche no repites pasado, eliges futuro",
            "Permite que tu cuerpo memorice paz",
            "La quietud también es creación",
            "Revisa el día desde compasión y poder",
            "Antes del sueño, vuelve a la imagen cumplida",
            "Tu descanso puede fortalecer tu intención",
            "Entrega la duda y conserva la certeza",
            "La noche convierte práctica en integración",
            "Imagina suavemente lo que deseas vivir",
            "Mañana empieza en el estado que duermes hoy",
            "Agradece lo vivido hoy",
            "Integra las lecciones del día",
            "Descansa en confianza",
            "Honra tu progreso",
            "Suelta lo que ya pasó",
            "Conserva lo aprendido",
            "Termina el día en paz",
            "Observa con compasión",
            "Reconoce tu crecimiento",
            "Deja espacio para la calma",
            "Agradece y descansa",
            "Permite que todo se asiente",
            "Encuentra sentido en la experiencia",
            "Libera el peso del día",
            "Abraza lo que descubriste",
            "Descansa en tu nueva identidad",
            "Cierra el día conscientemente",
            "Todo aprendizaje suma",
            "Mañana continúa la creación",
            "Duerme en coherencia",
            "Relájate y disfruta tu descanso",
            "Cada noche es una oportunidad de crear",
            "Recupera e integra las experiencias del día",
            "Bendice este día",
            "Descansa en la certeza de tu poder creativo",
            "No dejes psar este día sin bendecirte",
            "Agradece, todo está en su justo lugar"
        )
    }
}
