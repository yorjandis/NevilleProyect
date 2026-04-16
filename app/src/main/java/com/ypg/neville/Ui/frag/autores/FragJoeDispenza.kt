package com.ypg.neville.ui.frag

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.ypg.neville.model.subscription.SubscriptionManager
import com.ypg.neville.R
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.ui.theme.ContextMenuShape

class FragJoeDispenza : Fragment() {

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                com.ypg.neville.ui.theme.NevilleTheme {
                    val author = getString(R.string.joe_dispenza)
                    val authorAssetsFolder = "autores/joeDispenza"
                    val context = requireContext()
                    val navController = this@FragJoeDispenza.findNavController()
                    val subscriptionState by SubscriptionManager.uiState.collectAsState()
                    val hasPremium = subscriptionState.isActive && subscriptionState.isEntitlementVerified
                    val biographyAssetPath = remember { loadAuthorBiographyAssetPath(context, authorAssetsFolder) }
                    val teachingSummaryAssetPath = remember { loadAuthorTeachingSummaryAssetPath(context, authorAssetsFolder) }
                    val cards = remember { joeResourceCards() }
                    val placeholder = if (hasPremium) {
                        getString(R.string.author_quote_placeholder, author)
                    } else {
                        getString(R.string.author_quote_premium_placeholder, author)
                    }
                    var quote by remember {
                        mutableStateOf(
                            if (hasPremium) {
                                utilsDB.getRandomFraseByAutor(requireContext(), author)?.frase ?: placeholder
                            } else {
                                placeholder
                            }
                        )
                    }
                    if (!hasPremium && quote != placeholder) quote = placeholder
                    AuthorPlaceholderScreen(
                        authorName = author,
                        imageRes = R.drawable.jd,
                        quote = quote,
                        onQuoteClick = {
                            if (hasPremium) {
                                quote = utilsDB.getRandomFraseByAutor(requireContext(), author)?.frase ?: placeholder
                            }
                        },
                        onBiographyClick = {
                            biographyAssetPath?.let { openAsset(navController, it, isPremiumPreview = false) }
                        },
                        onTeachingSummaryClick = {
                            teachingSummaryAssetPath?.let { openAsset(navController, it, isPremiumPreview = !hasPremium) }
                        },
                        teachingSummaryEnabled = !teachingSummaryAssetPath.isNullOrBlank(),
                        resourcesSection = {
                            AuthorResourcesSection(
                                cards = cards,
                                hasPremium = hasPremium,
                                onResourceClick = { assetPath ->
                                    openAsset(navController, assetPath, isPremiumPreview = !hasPremium)
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    private fun openAsset(navController: NavController, assetPath: String, isPremiumPreview: Boolean) {
        FragContentWebView.elementLoaded = assetPath
        FragContentWebView.isPremiumPreviewMode = isPremiumPreview
        FragContentWebView.urlPath = "file:///android_asset/$assetPath"
        navController.navigate(R.id.frag_content_webview)
    }

    private fun joeResourceCards(): List<AccessCardPlaceholder> {
        val formulaMenu = (1..12).map { episode ->
            AccessCardMenuItem(
                title = "Episodio $episode",
                assetPath = "autores/joeDispenza/Serie_La_Formula/episodio_${episode}_resumen.txt"
            )
        }
        return listOf(
            AccessCardPlaceholder(
                title = "Desarrolla Tu Cerebro",
                primaryButton = "Resumen",
                primaryAssetPath = "autores/joeDispenza/libros/DesarrollaTuCerebro/resumen_libro_desarrollatucerebro.txt",
                secondaryButton = "Plan",
                secondaryAssetPath = "autores/joeDispenza/libros/DesarrollaTuCerebro/plan_libro_desarrollatucerebro.txt"
            ),
            AccessCardPlaceholder(
                title = "Deja De Ser Tu",
                primaryButton = "Resumen",
                primaryAssetPath = "autores/joeDispenza/libros/dejaDeSerTu/resumen_libro_dejadesertu.txt",
                secondaryButton = "Plan",
                secondaryAssetPath = "autores/joeDispenza/libros/dejaDeSerTu/plan_libro_dejadesertu.txt"
            ),
            AccessCardPlaceholder(
                title = "El Placebo Eres Tu",
                primaryButton = "Resumen",
                primaryAssetPath = "autores/joeDispenza/libros/elPlaceboEresTu/resumen_libro_elplaceboerestu.txt",
                secondaryButton = "Plan",
                secondaryAssetPath = "autores/joeDispenza/libros/elPlaceboEresTu/plan_libro_elplaceboerestu.txt"
            ),
            AccessCardPlaceholder(
                title = "Supernatural",
                primaryButton = "Resumen",
                primaryAssetPath = "autores/joeDispenza/libros/supernatural/resumen_libro_supernatural.txt",
                secondaryButton = "Plan",
                secondaryAssetPath = "autores/joeDispenza/libros/supernatural/plan_libro_supernatural.txt"
            ),
            AccessCardPlaceholder(
                title = "Resumen Serie La Formula",
                primaryButton = "Episodios",
                primaryAssetPath = formulaMenu.first().assetPath,
                menuItems = formulaMenu
            )
        )
    }

    @Composable
    private fun AuthorResourcesSection(
        cards: List<AccessCardPlaceholder>,
        hasPremium: Boolean,
        onResourceClick: (String) -> Unit
    ) {
        val titleColor = Color(0xFF2A211A)
        val bodyColor = Color(0xFF3A3026)
        val primaryBtn = Color(0xFF5A564E)
        val secondaryBtn = Color(0xFF5A564E)

       
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Recursos",
                color = titleColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            if (!hasPremium) {
                Text(
                    text = "Disponible en la Versión Extendida",
                    color = bodyColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (cards.isEmpty()) {
            Text(
                text = "No hay recursos disponibles",
                color = bodyColor,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            )
            return
        }

        val seriesCards = cards.filter { it.title.contains("Serie La Formula", ignoreCase = true) }
        val bookCards = cards.filterNot { it.title.contains("Serie La Formula", ignoreCase = true) }

        if (bookCards.isNotEmpty()) {
            Text(
                text = "Resumen de Libros",
                color = bodyColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            )
            ResourceCardsRow(
                cards = bookCards,
                bodyColor = bodyColor,
                primaryBtn = primaryBtn,
                secondaryBtn = secondaryBtn,
                onResourceClick = onResourceClick
            )
        }

        if (seriesCards.isNotEmpty()) {
            Text(
                text = "Resumen: Serie La Fórmula",
                color = bodyColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            )
            ResourceCardsRow(
                cards = seriesCards,
                bodyColor = bodyColor,
                primaryBtn = primaryBtn,
                secondaryBtn = secondaryBtn,
                onResourceClick = onResourceClick
            )
        }
    }

    @Composable
    private fun ResourceCardsRow(
        cards: List<AccessCardPlaceholder>,
        bodyColor: Color,
        primaryBtn: Color,
        secondaryBtn: Color,
        onResourceClick: (String) -> Unit
    ) {
        LazyRow(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(cards) { card ->
                var showChapterMenu by remember(card.title) { mutableStateOf(false) }
                val cardWidth = remember(card.title) {
                    val extraChars = (card.title.length - 18).coerceAtLeast(0)
                    (220 + (extraChars * 7)).coerceIn(220, 420)
                }
                Card(
                    modifier = Modifier
                        .width(cardWidth.dp)
                        .height(150.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = card.title,
                            color = bodyColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (card.menuItems.isNotEmpty()) {
                                    showChapterMenu = true
                                } else {
                                    onResourceClick(card.primaryAssetPath)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryBtn)
                        ) {
                            Text(card.primaryButton, color = Color.White)
                        }
                        if (card.menuItems.isNotEmpty()) {
                            DropdownMenu(
                                expanded = showChapterMenu,
                                onDismissRequest = { showChapterMenu = false },
                                shape = ContextMenuShape
                            ) {
                                card.menuItems.forEach { menuItem ->
                                    DropdownMenuItem(
                                        text = { Text(menuItem.title) },
                                        onClick = {
                                            showChapterMenu = false
                                            onResourceClick(menuItem.assetPath)
                                        }
                                    )
                                }
                            }
                        }
                        if (!card.secondaryButton.isNullOrBlank() && !card.secondaryAssetPath.isNullOrBlank()) {
                            Button(
                                onClick = { onResourceClick(card.secondaryAssetPath) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .height(40.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = secondaryBtn)
                            ) {
                                Text(card.secondaryButton, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
