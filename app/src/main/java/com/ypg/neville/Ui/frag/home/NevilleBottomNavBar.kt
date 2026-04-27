package com.ypg.neville.ui.frag

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ypg.neville.R
import com.ypg.neville.ui.theme.ContextMenuShape

@Composable
fun NevilleBottomNavBar(
    activeId: String?,
    tintColor: Color?,
    onConf: () -> Unit,
    onNotas: () -> Unit,
    onHome: () -> Unit,
    onDiario: () -> Unit,
    onLienzo: () -> Unit,
    onMetas: () -> Unit,
    onRecordatorios: () -> Unit,
    onRitual: () -> Unit,
    onResumenSemanal: () -> Unit,
    onVoces: () -> Unit,
    onAnclas: () -> Unit,
    onCalma: () -> Unit,
    onCardio: () -> Unit
) {
    var showProductivityMenu by remember { mutableStateOf(false) }
    val barShape = RoundedCornerShape(30.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 0.dp, bottom = 14.dp)
            .height(60.dp)
            .shadow(elevation = 16.dp, shape = barShape, clip = false)
            .border(width = 1.dp, color = Color(0x88FFFFFF), shape = barShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFB9BFC0),
                        Color(0xFFD4DBE0),
                        Color(0xFFB8C0C7)
                    )
                ),
                shape = barShape
            )
            .clip(barShape)
    ) {
        if (tintColor != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(tintColor.copy(alpha = 0.30f))
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xCCFFFFFF),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.TopCenter)
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavButton(
                activeId = activeId,
                id = "conf",
                icon = R.drawable.ic_conf,
                onClick = onConf,
                modifier = Modifier.weight(1f)
            )
            BottomNavButton(
                activeId = activeId,
                id = "notas",
                icon = R.drawable.ic_note,
                onClick = onNotas,
                modifier = Modifier.weight(1f)
            )
            BottomNavButton(
                activeId = activeId,
                id = "home",
                icon = R.drawable.ic_nav_home,
                onClick = onHome,
                modifier = Modifier.weight(1f)
            )

            BottomNavButton(
                activeId = activeId,
                id = "diario",
                icon = R.drawable.ic_diario_pen_book,
                onClick = onDiario,
                modifier = Modifier.weight(1f)
            )
            Box(modifier = Modifier.weight(1f)) {
                BottomNavButton(
                    activeId = if (activeId in setOf("lienzo", "metas", "recordatorios", "morning_dialog", "weekly_summary", "voces", "anclas", "calma", "cardio")) "productividad" else activeId,
                    id = "productividad",
                    icon = R.drawable.ic_icon_drawer,
                    onClick = { showProductivityMenu = true },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(
                    expanded = showProductivityMenu,
                    onDismissRequest = { showProductivityMenu = false },
                    shape = ContextMenuShape
                ) {
                    DropdownMenuItem(
                        text = { Text("Diario") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_diario_pen_book),
                                contentDescription = "Diario"
                            )
                        },
                        onClick = {
                            showProductivityMenu = false
                            onDiario()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Lienzo") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_edit_note),
                                contentDescription = "Lienzo"
                            )
                        },
                        onClick = {
                            showProductivityMenu = false
                            onLienzo()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Metas") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arriba),
                                contentDescription = "Metas"
                            )
                        },
                        onClick = {
                            showProductivityMenu = false
                            onMetas()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Recordatorios") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_calendar_toggle),
                                contentDescription = "Recordatorios"
                            )
                        },
                        onClick = {
                            showProductivityMenu = false
                            onRecordatorios()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Ritual del día") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_calendar_toggle),
                                contentDescription = "Ritual del día"
                            )
                        },
                        onClick = {
                            showProductivityMenu = false
                            onRitual()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Resumen Semanal") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_item),
                                contentDescription = "Resumen Semanal"
                            )
                        },
                        onClick = {
                            showProductivityMenu = false
                            onResumenSemanal()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Notas de Voz") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_audio),
                                contentDescription = "Notas de Voz"
                            )
                        },
                        onClick = {
                            showProductivityMenu = false
                            onVoces()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Anclas Emocionales") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_tips),
                                contentDescription = "Anclas Emocionales"
                            )
                        },
                        onClick = {
                            showProductivityMenu = false
                            onAnclas()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Espacio de Calma") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_show),
                                contentDescription = "Espacio de Calma"
                            )
                        },
                        onClick = {
                            showProductivityMenu = false
                            onCalma()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Coherencia Cardio-Cerebral") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.BookmarkAdded,
                                contentDescription = "Coherencia Cardio-Cerebral"
                            )
                        },
                        onClick = {
                            showProductivityMenu = false
                            onCardio()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavButton(
    activeId: String?,
    id: String,
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active = activeId == id
    val itemShape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .then(modifier)
            .height(44.dp)
            .padding(horizontal = 2.dp)
            .clip(itemShape)
            .background(
                if (active) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE6EEF3),
                            Color(0xFFC7D2DA)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Transparent)
                    )
                },
                itemShape
            )
            .border(
                width = if (active) 1.dp else 0.dp,
                color = if (active) Color(0x66FFFFFF) else Color.Transparent,
                shape = itemShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = id,
            tint = if (active) Color(0xFF1E2A32) else Color(0xFF2E3B44),
            modifier = Modifier.size(22.dp)
        )
    }
}
