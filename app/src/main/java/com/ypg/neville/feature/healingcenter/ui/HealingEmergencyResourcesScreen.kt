package com.ypg.neville.feature.healingcenter.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Emergency
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.core.net.toUri
import com.ypg.neville.R
import com.ypg.neville.feature.healingcenter.data.HealingCenterPreferences
import com.ypg.neville.feature.healingcenter.domain.HealingEmergencyContact
import com.ypg.neville.feature.healingcenter.domain.HealingEmergencyContactKind
import com.ypg.neville.feature.healingcenter.domain.HealingEmergencyResourceProvider
import com.ypg.neville.feature.healingcenter.domain.HealingSafetyPolicy

@Composable
internal fun HealingEmergencyResourcesScreen(
    preferences: HealingCenterPreferences,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    val provider = remember(context) { HealingEmergencyResourceProvider(context) }
    var selectedRegion by remember {
        mutableStateOf(preferences.emergencyRegion().ifBlank { provider.detectedRegionCode })
    }
    val resources = remember(selectedRegion) { provider.resources(selectedRegion) }
    var showRegions by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(
                start = 18.dp,
                end = 18.dp,
                top = contentPadding.calculateTopPadding() + 14.dp,
                bottom = contentPadding.calculateBottomPadding() + 30.dp
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HealingGlassCard {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFFF6262), modifier = Modifier.size(22.dp))
                Spacer(Modifier.size(9.dp))
                Text(
                    stringResource(R.string.healing_immediate_danger_notice),
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 21.sp
                )
            }
        }

        HealingGlassCard {
            Text(stringResource(R.string.healing_check_signals), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            HealingSafetyPolicy.urgentSignals(context).forEach { signal ->
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Emergency, contentDescription = null, tint = Color(0xFFFF7777), modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(9.dp))
                    Text(signal, color = Color.White.copy(alpha = 0.82f), fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
        }

        HealingGlassCard {
            Text(stringResource(R.string.healing_country_or_region), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .clickable { showRegions = true }
                        .padding(horizontal = 13.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Language, contentDescription = null, tint = Color(0xFF63DDEF), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(9.dp))
                    Text(resources.countryName, color = Color.White, modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.ExpandMore, contentDescription = stringResource(R.string.healing_change_country), tint = Color.White.copy(alpha = 0.72f))
                }
                DropdownMenu(expanded = showRegions, onDismissRequest = { showRegions = false }) {
                    provider.availableRegionCodes.forEach { code ->
                        DropdownMenuItem(
                            text = { Text(provider.countryName(code)) },
                            onClick = {
                                selectedRegion = code
                                preferences.setEmergencyRegion(code)
                                showRegions = false
                            }
                        )
                    }
                }
            }
            Text(resources.note, color = Color.White.copy(alpha = 0.60f), fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 9.dp))
        }

        if (resources.contacts.isEmpty()) {
            HealingGlassCard {
                Text(stringResource(R.string.healing_help), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.healing_local_help_fallback),
                    color = Color.White.copy(alpha = 0.76f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
                HealingDirectoryLink(Modifier.padding(top = 12.dp)) {
                    openExternalUrl(context, HealingEmergencyResourceProvider.INTERNATIONAL_DIRECTORY_URL)
                }
            }
        } else {
            Text(
                stringResource(R.string.healing_contacts_for, resources.countryName),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            resources.contacts.forEach { contact ->
                HealingEmergencyContactCard(
                    contact = contact,
                    onCall = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_DIAL, "tel:${contact.dialableNumber}".toUri()))
                        }
                    },
                    onSource = { contact.sourceUrl?.let { openExternalUrl(context, it) } }
                )
            }
        }

        HealingGlassCard {
            Text(stringResource(R.string.healing_more_countries), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            HealingDirectoryLink(Modifier.padding(top = 10.dp)) {
                openExternalUrl(context, HealingEmergencyResourceProvider.INTERNATIONAL_DIRECTORY_URL)
            }
            Text(
                stringResource(R.string.healing_services_change_notice),
                color = Color.White.copy(alpha = 0.60f),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun HealingEmergencyContactCard(
    contact: HealingEmergencyContact,
    onCall: () -> Unit,
    onSource: () -> Unit
) {
    val isEmergency = contact.kind == HealingEmergencyContactKind.EMERGENCY
    val accent = if (isEmergency) Color(0xFFFF6262) else Color(0xFF55A7FF)
    HealingGlassCard {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                if (isEmergency) Icons.Rounded.Call else Icons.Rounded.Favorite,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(contact.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(contact.detail, color = Color.White.copy(alpha = 0.68f), fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 3.dp))
            }
        }
        Spacer(Modifier.size(12.dp))
        HealingPrimaryButton(
            text = stringResource(R.string.healing_call_number, contact.number),
            icon = Icons.Rounded.Call,
            color = if (isEmergency) Color(0xFFB93232) else Color(0xFF2969B2),
            onClick = onCall
        )
        if (contact.sourceUrl != null) {
            Row(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clickable(onClick = onSource),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.healing_official_source), color = Color(0xFF63DDEF), fontSize = 12.sp)
                Spacer(Modifier.size(5.dp))
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = Color(0xFF63DDEF), modifier = Modifier.size(13.dp))
            }
        }
    }
}

@Composable
private fun HealingDirectoryLink(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Language, contentDescription = null, tint = Color(0xFF63DDEF), modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(8.dp))
        Text(stringResource(R.string.healing_international_directory), color = Color(0xFF63DDEF), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, contentDescription = null, tint = Color(0xFF63DDEF), modifier = Modifier.size(14.dp))
    }
}
