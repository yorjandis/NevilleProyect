package com.ypg.neville.ui.frag

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

class FragBruceLipton : Fragment() {

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                com.ypg.neville.ui.theme.NevilleTheme {
                    val author = getString(R.string.bruce_lipton)
                    val authorAssetsFolder = "autores/bruceLipton"
                    val context = requireContext()
                    val navController = this@FragBruceLipton.findNavController()
                    val subscriptionState by SubscriptionManager.uiState.collectAsState()
                    val hasPremium = subscriptionState.isActive && subscriptionState.isEntitlementVerified
                    val biographyAssetPath = remember { loadAuthorBiographyAssetPath(context, authorAssetsFolder) }
                    val teachingSummaryAssetPath = remember { loadAuthorTeachingSummaryAssetPath(context, authorAssetsFolder) }
                    val cards = remember { bruceResourceCards() }
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
                        imageRes = R.drawable.bruce,
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

    private fun bruceResourceCards(): List<AccessCardPlaceholder> {
        val evolutionMenu = (1..13).map { chapter ->
            AccessCardMenuItem(
                title = "Capítulo $chapter",
                assetPath = "autores/bruceLipton/materialBruce/serieEvolucionInterior/bruce_evolucion_interior_${chapter}.txt"
            )
        }
        return listOf(
            AccessCardPlaceholder(
                title = "La Biologia De La Creencia",
                primaryButton = "Resumen",
                primaryAssetPath = "autores/bruceLipton/libros/LaBiologiaDeLaCreencia/resumen_libro_biologiacreencia.txt",
                secondaryButton = "Plan",
                secondaryAssetPath = "autores/bruceLipton/libros/LaBiologiaDeLaCreencia/plan_libro_biologiacreencia.txt"
            ),
            AccessCardPlaceholder(
                title = "Resumen Serie Evolución Interior",
                primaryButton = "Capítulos",
                primaryAssetPath = evolutionMenu.first().assetPath,
                menuItems = evolutionMenu
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

        val evolutionCards = cards.filter { it.title.contains("Evolución Interior", ignoreCase = true) }
        val bookCards = cards.filterNot { it.title.contains("Evolución Interior", ignoreCase = true) }

        if (bookCards.isNotEmpty()) {
            Text(
                text = "Resumen de Libro",
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

        if (evolutionCards.isNotEmpty()) {
            Text(
                text = "Resumen Evolución Interior",
                color = bodyColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            )
            ResourceCardsRow(
                cards = evolutionCards,
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
