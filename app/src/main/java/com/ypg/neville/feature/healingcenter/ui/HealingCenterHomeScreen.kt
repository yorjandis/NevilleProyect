package com.ypg.neville.feature.healingcenter.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Emergency
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.ypg.neville.R
import com.ypg.neville.feature.healingcenter.domain.HealingCatalog
import com.ypg.neville.feature.healingcenter.domain.HealingSituation

@Composable
internal fun HealingCenterHomeScreen(
    catalog: HealingCatalog,
    favorites: Set<String>,
    hasPremium: Boolean,
    contentPadding: PaddingValues,
    onSituation: (HealingSituation) -> Unit,
    onEmergency: () -> Unit,
    onShowPremium: () -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    val situations = remember(catalog, searchText, favorites) {
        catalog.situations
            .filter { it.matches(searchText) }
            .sortedWith(
                compareByDescending<HealingSituation> { it.id in favorites }
                    .thenBy { catalog.situations.indexOf(it) }
            )
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = contentPadding.calculateTopPadding() + 14.dp,
            bottom = contentPadding.calculateBottomPadding() + 36.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF69F0AE).copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.HealthAndSafety, contentDescription = null, tint = Color(0xFF69F0AE), modifier = Modifier.size(27.dp))
                    }
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text(stringResource(R.string.healing_home_support_title), color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.healing_home_support_subtitle), color = Color.White.copy(alpha = 0.70f), fontSize = 15.sp)
                    }
                }
                Text(
                    stringResource(R.string.healing_home_intro),
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFB93232).copy(alpha = 0.82f), RoundedCornerShape(19.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(19.dp))
                    .clickable(onClick = onEmergency)
                    .padding(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Emergency, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.healing_home_emergency_question), color = Color.White, fontSize = 18.sp)
                    Text(stringResource(R.string.healing_home_emergency_detail), color = Color.White.copy(alpha = 0.82f), fontSize = 14.sp)
                }
                Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }

        if (hasPremium) {
            item {
                HealingGlassCard {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = Color(0xFF38D8EE), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(9.dp))
                        Text(
                            "${stringResource(R.string.healing_guide_not_diagnosis)}\n\n${catalog.disclaimer}",
                            color = Color(0xFF38D8EE),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.healing_search_hint), color = Color.White.copy(alpha = 0.54f)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = { searchText = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.healing_clear_search))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38D8EE),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.20f),
                        focusedLeadingIconColor = Color(0xFF38D8EE),
                        unfocusedLeadingIconColor = Color.White.copy(alpha = 0.62f),
                        focusedTrailingIconColor = Color.White,
                        unfocusedTrailingIconColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.07f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.07f)
                    )
                )
            }
            item {
                Text(
                    if (searchText.isEmpty()) stringResource(R.string.healing_what_is_happening) else stringResource(R.string.healing_results),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (situations.isEmpty()) {
                item {
                    HealingGlassCard {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.72f))
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.healing_no_matches), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.healing_search_suggestion),
                            color = Color.White.copy(alpha = 0.68f),
                            modifier = Modifier.padding(top = 5.dp)
                        )
                    }
                }
            } else {
                items(situations, key = { it.id }) { situation ->
                    HealingSituationCard(
                        situation = situation,
                        isFavorite = situation.id in favorites,
                        onClick = { onSituation(situation) }
                    )
                }
            }
        } else {
            item {
                HealingGlassCard {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color(0xFFFFD740).copy(alpha = 0.16f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color(0xFFFFD740), modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.healing_premium_guides), color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            Text(
                                stringResource(R.string.healing_premium_explanation),
                                color = Color.White.copy(alpha = 0.78f),
                                fontSize = 15.sp,
                                lineHeight = 21.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    HealingPrimaryButton(
                        text = stringResource(R.string.healing_unlock),
                        icon = Icons.Rounded.Star,
                        color = Color(0xFF4E50A8),
                        onClick = onShowPremium
                    )
                }
            }
        }
    }
}

@Composable
internal fun HealingCatalogErrorScreen(contentPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(contentPadding)
            .padding(22.dp),
        contentAlignment = Alignment.Center
    ) {
        HealingGlassCard {
            Icon(Icons.Rounded.Spa, contentDescription = null, tint = Color(0xFFFFA726), modifier = Modifier.size(34.dp))
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.healing_unavailable), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.healing_load_error),
                color = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
