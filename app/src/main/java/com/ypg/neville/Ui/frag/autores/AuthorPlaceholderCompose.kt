package com.ypg.neville.ui.frag

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ypg.neville.model.preferences.DbPreferences
import com.ypg.neville.model.utils.FraseContextActions
import com.ypg.neville.model.utils.UiModalWindows

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AuthorPlaceholderScreen(
    authorName: String,
    imageRes: Int,
    quote: String,
    onQuoteClick: () -> Unit,
    quoteFilter: AuthorQuoteFilter,
    onQuoteFilterChange: (AuthorQuoteFilter) -> Unit,
    favoriteOptionLabel: String? = null,
    onToggleFavorito: (() -> Unit)? = null,
    onBiographyClick: () -> Unit,
    onTeachingSummaryClick: () -> Unit,
    teachingSummaryEnabled: Boolean,
    resourcesSection: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = DbPreferences.default(context)
    var showFraseMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    val bg = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFCFAF9B),
            Color(0xFFC8B99A),
            Color(0xFFD7D1CD)
        )
    )
    val titleColor = Color(0xFF2A211A)
    val bodyColor = Color(0xFF3A3026)
    val primaryBtn = Color(0xFF5A564E)
    val secondaryBtn = Color(0xFF5A564E)
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
                        .size(80.dp)
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

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = onBiographyClick,
                            modifier = Modifier
                                .weight(1f)
                                .width(60.dp)
                                .height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryBtn),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "Biografía",
                                color = Color.White,
                                maxLines = 1,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Button(
                            onClick = onTeachingSummaryClick,
                            enabled = teachingSummaryEnabled,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = secondaryBtn),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "Resum. Enseñanza",
                                color = Color.White,
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Box {
                    IconButton(
                        onClick = { showFilterMenu = true },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_sort_by_size),
                            contentDescription = "Filtrar frases",
                            tint = bodyColor.copy(alpha = 0.68f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {

                        Text(" Filtrar frases:")
                        val isAllSelected = !quoteFilter.onlyFavorites && !quoteFilter.onlyWithNotes
                        DropdownMenuItem(
                            text = { Text(if (isAllSelected) "✓ Todas" else "Todas") },
                            onClick = {
                                onQuoteFilterChange(AuthorQuoteFilter())
                                onQuoteClick()
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (quoteFilter.onlyFavorites) "✓ Favoritas" else "Favoritas") },
                            onClick = {
                                onQuoteFilterChange(
                                    quoteFilter.copy(onlyFavorites = !quoteFilter.onlyFavorites)
                                )
                                onQuoteClick()
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (quoteFilter.onlyWithNotes) "✓ Con notas" else "Con notas") },
                            onClick = {
                                onQuoteFilterChange(
                                    quoteFilter.copy(onlyWithNotes = !quoteFilter.onlyWithNotes)
                                )
                                onQuoteClick()
                                showFilterMenu = false
                            }
                        )
                    }
                }
            }
        }


        item {
            val noRipple = remember { MutableInteractionSource() }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(300.dp)
                    .combinedClickable(
                        interactionSource = noRipple,
                        indication = null,
                        onClick = { onQuoteClick() },
                        onLongClick = {
                            if (quote.isNotBlank()) {
                                showFraseMenu = true
                            }
                        }
                    ),
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

            FraseOptionsMenu(
                expanded = showFraseMenu,
                onDismiss = { showFraseMenu = false },
                favoriteOptionLabel = favoriteOptionLabel,
                onToggleFavorito = onToggleFavorito,
                onConvertirNota = {
                    val result = FraseContextActions.convertirFraseEnNota(context, quote)
                    if (result.ok) {
                        Toast.makeText(context, "Nota creada: ${result.titulo}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "No se pudo crear la nota", Toast.LENGTH_SHORT).show()
                    }
                },
                onCargarLienzo = {
                    FraseContextActions.cargarFraseEnLienzo(context, quote)
                },
                onCompartirSistema = {
                    FraseContextActions.compartirFraseSistema(
                        context = context,
                        frase = quote,
                        autor = authorName,
                        fuente = ""
                    )
                },
                onAbrirNotaFrase = {
                    FraseContextActions.abrirNotaDeFrase(context, quote)
                },
                onCrearNuevaFrase = {
                    UiModalWindows.Add_New_frase(context, null)
                }
            )
        }

        item { resourcesSection() }
    }
}
