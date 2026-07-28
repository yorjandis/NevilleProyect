package com.ypg.neville.feature.healingcenter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Emergency
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ypg.neville.R
import com.ypg.neville.feature.healingcenter.domain.HealingProtocol
import com.ypg.neville.feature.healingcenter.domain.HealingSafetyPolicy
import com.ypg.neville.feature.healingcenter.domain.HealingSituation

@Composable
internal fun HealingSafetyGateScreen(
    situation: HealingSituation,
    protocol: HealingProtocol,
    contentPadding: PaddingValues,
    onEmergency: () -> Unit,
    onStart: () -> Unit
) {
    var checked by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val signals = remember(situation, context) { HealingSafetyPolicy.combinedSignals(context, situation) }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(
                start = 18.dp,
                end = 18.dp,
                top = contentPadding.calculateTopPadding() + 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 28.dp
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = Color(0xFFFFA726), modifier = Modifier.size(44.dp))
        Text(stringResource(R.string.healing_before_starting), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(
            stringResource(R.string.healing_safety_intro),
            color = Color.White.copy(alpha = 0.74f),
            fontSize = 16.sp,
            lineHeight = 22.sp
        )

        HealingGlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFFF6262), modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(7.dp))
                Text(stringResource(R.string.healing_seek_immediate_help), color = Color(0xFFFF6262), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(8.dp))
            signals.forEach { signal ->
                Row(Modifier.padding(top = 7.dp), verticalAlignment = Alignment.Top) {
                    Text("•", color = Color.White.copy(alpha = 0.84f))
                    Spacer(Modifier.size(9.dp))
                    Text(signal, color = Color.White.copy(alpha = 0.84f), fontSize = 15.sp, lineHeight = 21.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            HealingPrimaryButton(
                text = stringResource(R.string.healing_unsure_need_help),
                icon = Icons.Rounded.Emergency,
                color = Color(0xFFB93232),
                onClick = onEmergency
            )
        }

        HealingGlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(healingSymbolIcon(protocol.symbol), contentDescription = null, tint = Color(0xFF38D8EE), modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(7.dp))
                Text(protocol.title, color = Color(0xFF38D8EE), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(protocol.summary, color = Color.White.copy(alpha = 0.78f), fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 7.dp))
            protocol.caution?.let { caution ->
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = Color(0xFFFFA726), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(7.dp))
                    Text(caution, color = Color(0xFFFFB45F), fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                .padding(11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { checked = it },
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF69F0AE), checkmarkColor = Color(0xFF07120D))
            )
            Text(
                stringResource(R.string.healing_safety_confirmation),
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }

        HealingPrimaryButton(
            text = stringResource(R.string.healing_start_guide),
            icon = Icons.Rounded.PlayArrow,
            color = Color(0xFF69F0AE),
            enabled = checked,
            onClick = onStart
        )

        Text(
            stringResource(R.string.healing_stop_instruction),
            color = Color.White.copy(alpha = 0.60f),
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}
