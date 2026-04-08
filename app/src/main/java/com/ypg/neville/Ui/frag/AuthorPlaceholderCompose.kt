package com.ypg.neville.ui.frag

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager

@Composable
fun AuthorPlaceholderScreen(
    authorName: String,
    imageRes: Int,
    quote: String,
    onQuoteClick: () -> Unit,
    onBiographyClick: () -> Unit,
    onTeachingSummaryClick: () -> Unit,
    teachingSummaryEnabled: Boolean,
    resourcesSection: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val bg = Color(0xFF91AED7) //Color de fondo
    val titleColor = Color(0xFF2A211A)
    val bodyColor = Color(0xFF3A3026)
    val primaryBtn = Color(0xFF6E84CF)
    val secondaryBtn = Color(0xFF6E84CF)
    val textSize = (prefs.getString("fuente_frase", "28")?.toFloatOrNull() ?: 28f).coerceIn(16f, 40f)
    val textColor = prefs.getInt("color_letra_frases", 0)
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = authorName,
                    modifier = Modifier
                        .size(112.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = authorName,
                        color = titleColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = onBiographyClick,
                        modifier = Modifier.padding(top = 10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBtn)
                    ) {
                        Text("Biografía", color = Color.White)
                    }

                    Button(
                        onClick = onTeachingSummaryClick,
                        enabled = teachingSummaryEnabled,
                        modifier = Modifier.padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = secondaryBtn)
                    ) {
                        Text("Resumen Enseñanza", color = Color.White)
                    }
                }
            }
        }

        item {
            Text(
                text = "Frases",
                color = titleColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp)
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(300.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onQuoteClick() },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = quote,
                    color = if (textColor != 0) Color(textColor) else Color.Black,
                    fontSize = textSize.sp,
                    lineHeight = (textSize * 1.38f).sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 268.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                )
            }
        }

        item { resourcesSection() }
    }
}
