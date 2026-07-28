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
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ypg.neville.R

@Composable
fun NevilleBottomNavBar(
    activeId: String?,
    tintColor: Color?,
    onConf: () -> Unit,
    onNotas: () -> Unit,
    onHome: () -> Unit,
    onDiario: () -> Unit,
    onChat: () -> Unit,
    onLienzo: () -> Unit,
    onMetas: () -> Unit,
    onRecordatorios: () -> Unit,
    onAgenda: () -> Unit,
    onRitual: () -> Unit,
    onResumenSemanal: () -> Unit,
    onVoces: () -> Unit,
    onAnclas: () -> Unit,
    onCalma: () -> Unit,
    onCardio: () -> Unit,
    onPresence: () -> Unit
) {
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
                contentDescription = stringResource(R.string.home_nav_lectures),
                onClick = onConf,
                modifier = Modifier.weight(1f)
            )
            BottomNavButton(
                activeId = activeId,
                id = "notas",
                icon = R.drawable.ic_note,
                contentDescription = stringResource(R.string.home_nav_notes),
                onClick = onNotas,
                modifier = Modifier.weight(1f)
            )
            BottomNavButton(
                activeId = activeId,
                id = "home",
                icon = R.drawable.ic_nav_home,
                contentDescription = stringResource(R.string.home_nav_home),
                onClick = onHome,
                modifier = Modifier.weight(1f)
            )

            BottomNavButton(
                activeId = activeId,
                id = "diario",
                icon = R.drawable.ic_diario_pen_book,
                contentDescription = stringResource(R.string.home_nav_diary),
                onClick = onDiario,
                modifier = Modifier.weight(1f)
            )
            BottomNavButton(
                activeId = activeId,
                id = "chat",
                icon = R.drawable.ic_nav_chat,
                contentDescription = stringResource(R.string.nav_chat),
                onClick = onChat,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomNavButton(
    activeId: String?,
    id: String,
    icon: Int,
    contentDescription: String,
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
            contentDescription = contentDescription,
            tint = if (active) Color(0xFF1E2A32) else Color(0xFF2E3B44),
            modifier = Modifier.size(22.dp)
        )
    }
}
