package com.ypg.neville.feature.healingcenter.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Emergency
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.ypg.neville.R
import com.ypg.neville.feature.healingcenter.domain.HealingCatalog
import com.ypg.neville.feature.healingcenter.domain.HealingProtocol
import com.ypg.neville.feature.healingcenter.domain.HealingSituation

@Composable
internal fun HealingSituationDetailScreen(
    situation: HealingSituation,
    catalog: HealingCatalog,
    contentPadding: PaddingValues,
    onProtocol: (HealingProtocol) -> Unit,
    onEmergency: () -> Unit
) {
    val colors = healingPaletteColors(situation.palette)
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(
                start = 18.dp,
                end = 18.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 40.dp
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Brush.linearGradient(colors), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(healingSymbolIcon(situation.symbol), contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(situation.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    situation.subtitle,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
        }

        HealingGlassCard {
            HealingSectionLabel(stringResource(R.string.healing_essential_now), Icons.Rounded.Bolt, colors.first())
            Text(situation.immediateExplanation, color = Color.White, fontSize = 16.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 7.dp))
            Text(situation.reassurance, color = Color.White.copy(alpha = 0.76f), fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 7.dp))
        }

        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text(stringResource(R.string.healing_choose_practical_help), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            situation.protocols.forEach { protocol ->
                HealingProtocolCard(protocol, colors, onClick = { onProtocol(protocol) })
            }
        }

        HealingDisclosureCard(
            title = stringResource(R.string.healing_small_resources),
            collapsedHint = stringResource(R.string.healing_small_resources_hint),
            expandedHint = stringResource(R.string.healing_hide_tips),
            icon = Icons.Rounded.Lightbulb,
            accent = Color(0xFFFFD740)
        ) {
            Text(
                stringResource(R.string.healing_complementary_resources_note),
                color = Color.White.copy(alpha = 0.64f),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(14.dp))
            situation.practicalTips.forEach { tip ->
                Row(Modifier.padding(bottom = 14.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color(0xFFFFD740), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.size(11.dp))
                    Column {
                        Text(tip.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(tip.detail, color = Color.White.copy(alpha = 0.72f), fontSize = 14.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }
            Text(
                stringResource(R.string.healing_practical_tip_warning),
                color = Color(0xFFFFB45F),
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }

        HealingDisclosureCard(
            title = stringResource(R.string.healing_what_may_be_happening),
            collapsedHint = stringResource(R.string.healing_body_mind_explanation_hint),
            expandedHint = stringResource(R.string.healing_hide_explanation),
            icon = Icons.Rounded.Psychology,
            accent = Color(0xFF69F0AE)
        ) {
            Text(situation.biologicalExplanation, color = Color.White.copy(alpha = 0.84f), fontSize = 15.sp, lineHeight = 22.sp)
            HorizontalDivider(color = Color.White.copy(alpha = 0.14f), modifier = Modifier.padding(vertical = 14.dp))
            HealingSectionLabel(stringResource(R.string.healing_body_learns), Icons.Rounded.Sync, Color(0xFF69F0AE))
            Text(
                stringResource(R.string.healing_neuroplasticity_note),
                color = Color.White.copy(alpha = 0.70f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        HealingDisclosureCard(
            title = stringResource(R.string.healing_when_to_seek_help),
            collapsedHint = stringResource(R.string.healing_warning_signs_hint),
            expandedHint = stringResource(R.string.healing_hide_warning_signs),
            icon = Icons.Rounded.SupportAgent,
            accent = Color(0xFFFFA726)
        ) {
            situation.whenToSeekHelp.forEach { item ->
                Row(Modifier.padding(bottom = 12.dp), verticalAlignment = Alignment.Top) {
                    Text("•", color = Color.White.copy(alpha = 0.80f), fontSize = 16.sp)
                    Spacer(Modifier.size(9.dp))
                    Text(item, color = Color.White.copy(alpha = 0.80f), fontSize = 15.sp, lineHeight = 21.sp)
                }
            }
            HealingPrimaryButton(
                text = stringResource(R.string.healing_view_local_urgent_help),
                icon = Icons.Rounded.Emergency,
                color = Color(0xFFB93232),
                onClick = onEmergency
            )
        }

        HealingDisclosureCard(
            title = stringResource(R.string.healing_sources_and_evidence),
            collapsedHint = stringResource(R.string.healing_sources_hint),
            expandedHint = stringResource(R.string.healing_hide_sources),
            icon = Icons.AutoMirrored.Rounded.MenuBook,
            accent = Color(0xFF38D8EE)
        ) {
            situation.sources.forEach { source ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openExternalUrl(context, source.url) }
                        .padding(vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(source.title, color = Color(0xFF63DDEF), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = stringResource(R.string.healing_open_source), tint = Color(0xFF63DDEF), modifier = Modifier.size(15.dp))
                    }
                    Text(source.organization, color = Color.White.copy(alpha = 0.62f), fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                    Text(source.note, color = Color.White.copy(alpha = 0.74f), fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 3.dp))
                }
                Spacer(Modifier.height(5.dp))
            }
        }

        Column(Modifier.padding(horizontal = 4.dp)) {
            Text(stringResource(R.string.healing_informational_disclaimer), color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp, lineHeight = 15.sp)
            Text(
                stringResource(R.string.healing_content_reviewed, catalog.contentVersion, catalog.reviewedAt),
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 5.dp)
            )
        }
    }
}

@Composable
private fun HealingProtocolCard(protocol: HealingProtocol, colors: List<Color>, onClick: () -> Unit) {
    val totalSeconds = protocol.durationSeconds
    val duration = when {
        totalSeconds < 60 -> stringResource(R.string.healing_duration_seconds, totalSeconds)
        totalSeconds % 60 == 0 -> stringResource(R.string.healing_duration_minutes, totalSeconds / 60)
        else -> stringResource(R.string.healing_duration_minutes_seconds, totalSeconds / 60, totalSeconds % 60)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(colors.first().copy(alpha = 0.16f), Color.White.copy(alpha = 0.09f), colors.last().copy(alpha = 0.10f))
                ),
                RoundedCornerShape(19.dp)
            )
            .border(1.dp, colors.first().copy(alpha = 0.38f), RoundedCornerShape(19.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Brush.linearGradient(listOf(colors.first().copy(alpha = 0.82f), colors.last().copy(alpha = 0.62f))), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(healingSymbolIcon(protocol.symbol), contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(protocol.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(protocol.summary, color = Color.White.copy(alpha = 0.68f), fontSize = 14.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 3.dp))
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, contentDescription = null, tint = Color.White.copy(alpha = 0.50f), modifier = Modifier.size(14.dp))
        }
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            HealingEvidenceBadge(protocol.evidence)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = Color.White.copy(alpha = 0.68f), modifier = Modifier.size(14.dp))
            Spacer(Modifier.size(5.dp))
            Text(duration, color = Color.White.copy(alpha = 0.68f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun HealingDisclosureCard(
    title: String,
    collapsedHint: String,
    expandedHint: String,
    icon: ImageVector,
    accent: Color,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    HealingGlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = accent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    if (expanded) expandedHint else collapsedHint,
                    color = Color.White.copy(alpha = 0.58f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            Icon(
                if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) stringResource(R.string.healing_collapse) else stringResource(R.string.healing_expand),
                tint = accent
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(Modifier.padding(top = 12.dp)) { content() }
        }
    }
}

@Composable
private fun HealingSectionLabel(text: String, icon: ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(7.dp))
        Text(text, color = color, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

internal fun openExternalUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
}
