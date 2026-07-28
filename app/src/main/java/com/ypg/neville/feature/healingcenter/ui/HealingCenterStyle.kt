package com.ypg.neville.feature.healingcenter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FrontHand
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ypg.neville.R
import com.ypg.neville.feature.healingcenter.domain.HealingEvidenceLevel
import com.ypg.neville.feature.healingcenter.domain.HealingPalette
import com.ypg.neville.feature.healingcenter.domain.HealingSituation
import com.ypg.neville.feature.healingcenter.domain.HealingStepPhase

internal val HealingBackground = Brush.linearGradient(
    listOf(Color(0xFF091321), Color(0xFF13262E), Color(0xFF1C172E))
)

internal fun healingPaletteColors(palette: HealingPalette): List<Color> = when (palette) {
    HealingPalette.OCEAN -> listOf(Color(0xFF00BCD4), Color(0xFF296BF2))
    HealingPalette.AMBER -> listOf(Color(0xFFFFEB3B), Color(0xFFFF9800))
    HealingPalette.VIOLET -> listOf(Color(0xFF9C27B0), Color(0xFF3F51B5))
    HealingPalette.FOREST -> listOf(Color(0xFF69F0AE), Color(0xFF0D855C))
    HealingPalette.ROSE -> listOf(Color(0xFFE91E63), Color(0xFFE83D3D))
    HealingPalette.SLATE -> listOf(Color(0xFF009688), Color(0xFF4A5782))
}

internal fun evidenceColor(level: HealingEvidenceLevel): Color = when (level) {
    HealingEvidenceLevel.SUPPORTED -> Color(0xFF62D66F)
    HealingEvidenceLevel.PROMISING -> Color(0xFF38D8EE)
    HealingEvidenceLevel.COMPLEMENTARY -> Color(0xFFFFA726)
    HealingEvidenceLevel.EXPERIMENTAL -> Color(0xFFB76DEA)
}

@Composable
internal fun HealingGlassCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.09f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(22.dp))
            .padding(contentPadding),
        content = content
    )
}

@Composable
internal fun HealingEvidenceBadge(level: HealingEvidenceLevel) {
    val color = evidenceColor(level)
    val title = when (level) {
        HealingEvidenceLevel.SUPPORTED -> stringResource(R.string.healing_evidence_supported)
        HealingEvidenceLevel.PROMISING -> stringResource(R.string.healing_evidence_promising)
        HealingEvidenceLevel.COMPLEMENTARY -> stringResource(R.string.healing_evidence_complementary)
        HealingEvidenceLevel.EXPERIMENTAL -> stringResource(R.string.healing_evidence_experimental)
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.CheckCircleOutline, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.size(5.dp))
        Text(title, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun HealingSituationCard(
    situation: HealingSituation,
    isFavorite: Boolean,
    onClick: () -> Unit
) {
    val colors = healingPaletteColors(situation.palette)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(colors.first().copy(alpha = 0.20f), Color.White.copy(alpha = 0.06f))))
            .border(1.dp, colors.first().copy(alpha = 0.33f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(colors)),
            contentAlignment = Alignment.Center
        ) {
            Icon(healingSymbolIcon(situation.symbol), contentDescription = null, tint = Color.White, modifier = Modifier.size(27.dp))
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    situation.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isFavorite) {
                    Spacer(Modifier.size(6.dp))
                    Icon(Icons.Rounded.Star, contentDescription = stringResource(R.string.healing_favorite), tint = Color(0xFFFFD740), modifier = Modifier.size(15.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                situation.subtitle,
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 15.sp,
                lineHeight = 20.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.size(6.dp))
        Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, contentDescription = null, tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.size(15.dp))
    }
}

@Composable
internal fun HealingPrimaryButton(
    text: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = if (color == Color(0xFF69F0AE)) Color(0xFF07120D) else Color.White,
            disabledContainerColor = color.copy(alpha = 0.38f),
            disabledContentColor = Color.White.copy(alpha = 0.52f)
        ),
        contentPadding = PaddingValues(vertical = 13.dp, horizontal = 16.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
        Spacer(Modifier.size(8.dp))
        Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

internal fun healingPhaseIcon(phase: HealingStepPhase): ImageVector = when (phase) {
    HealingStepPhase.PREPARE -> Icons.Rounded.SelfImprovement
    HealingStepPhase.INHALE -> Icons.Rounded.Air
    HealingStepPhase.EXHALE -> Icons.Rounded.Waves
    HealingStepPhase.OBSERVE -> Icons.Rounded.RemoveRedEye
    HealingStepPhase.ORIENT -> Icons.Rounded.Visibility
    HealingStepPhase.MOVE -> Icons.AutoMirrored.Rounded.DirectionsWalk
    HealingStepPhase.PRESS -> Icons.Rounded.TouchApp
    HealingStepPhase.SOUND -> Icons.Rounded.GraphicEq
    HealingStepPhase.REFLECT -> Icons.Rounded.Psychology
    HealingStepPhase.FINISH -> Icons.Rounded.Check
}

internal fun healingSymbolIcon(symbol: String): ImageVector {
    val value = symbol.lowercase()
    return when {
        "heart" in value || "ecg" in value -> Icons.Rounded.Favorite
        "brain" in value || "thought" in value -> Icons.Rounded.Psychology
        "wind" in value || "air" in value || "lungs" in value -> Icons.Rounded.Air
        "hand" in value || "tap" in value || "touch" in value -> Icons.Rounded.FrontHand
        "walk" in value || "figure" in value || "move" in value -> Icons.AutoMirrored.Rounded.DirectionsWalk
        "wave" in value || "sound" in value -> Icons.Rounded.GraphicEq
        "eye" in value || "scope" in value -> Icons.Rounded.Visibility
        "flame" in value || "bolt" in value -> Icons.Rounded.Bolt
        "shield" in value || "cross" in value || "medical" in value -> Icons.Rounded.HealthAndSafety
        "spark" in value || "star" in value -> Icons.Rounded.AutoAwesome
        "light" in value -> Icons.Rounded.Lightbulb
        "warning" in value || "exclamation" in value -> Icons.Rounded.Warning
        "phone" in value || "sos" in value -> Icons.Rounded.Call
        "check" in value -> Icons.Rounded.CheckCircle
        "pause" in value -> Icons.Rounded.Pause
        "search" in value -> Icons.Rounded.Search
        "close" in value -> Icons.Rounded.Close
        "back" in value -> Icons.AutoMirrored.Rounded.ArrowBack
        "circle" in value -> Icons.Rounded.RadioButtonUnchecked
        "fire" in value -> Icons.Rounded.LocalFireDepartment
        "access" in value -> Icons.Rounded.AccessibilityNew
        else -> Icons.Rounded.Spa
    }
}
