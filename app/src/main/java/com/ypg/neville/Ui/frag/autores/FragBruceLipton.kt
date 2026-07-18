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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.ypg.neville.model.subscription.SubscriptionManager
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.db.DatabaseHelper
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.model.preferences.DbPreferences
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
                    val authorDisplayName = stringResource(R.string.bruce_lipton)
                    val authorAssetsFolder = "autores/bruceLipton"
                    val context = requireContext()
                    val subscriptionState by SubscriptionManager.uiState.collectAsState()
                    val hasPremium = subscriptionState.isActive && subscriptionState.isEntitlementVerified
                    val prefs = remember { DbPreferences.default(context) }
                    val filterFavKey = remember { authorQuoteFilterPrefKey(authorAssetsFolder, "favoritas") }
                    val filterNotesKey = remember { authorQuoteFilterPrefKey(authorAssetsFolder, "con_notas") }
                    val biographyAssetPath = remember { loadAuthorBiographyAssetPath(context, authorAssetsFolder) }
                    val teachingSummaryAssetPath = remember { loadAuthorTeachingSummaryAssetPath(context, authorAssetsFolder) }
                    val cards = remember { bruceResourceCards() }
                    val placeholder = if (hasPremium) {
                        stringResource(R.string.author_quote_placeholder, authorDisplayName)
                    } else {
                        stringResource(R.string.author_quote_premium_placeholder, authorDisplayName)
                    }
                    var quoteFilter by remember {
                        mutableStateOf(
                            AuthorQuoteFilter(
                                onlyFavorites = prefs.getBoolean(filterFavKey, false),
                                onlyWithNotes = prefs.getBoolean(filterNotesKey, false)
                            )
                        )
                    }
                    val initialQuoteItem = remember(hasPremium) {
                        if (hasPremium) {
                            utilsDB.getRandomFraseByAutor(
                                context = context,
                                autor = AUTHOR_DATABASE_NAME,
                                onlyFav = quoteFilter.onlyFavorites,
                                onlyWithNotes = quoteFilter.onlyWithNotes
                            )
                        } else {
                            null
                        }
                    }
                    var quote by remember {
                        mutableStateOf(
                            if (hasPremium) {
                                initialQuoteItem?.frase ?: placeholder
                            } else {
                                placeholder
                            }
                        )
                    }
                    var quoteFavState by remember { mutableStateOf(initialQuoteItem?.fav ?: "") }
                    if (!hasPremium && quote != placeholder) quote = placeholder
                    if (!hasPremium && quoteFavState.isNotEmpty()) quoteFavState = ""
                    AuthorPlaceholderScreen(
                        authorName = authorDisplayName,
                        imageRes = R.drawable.bruce,
                        quote = quote,
                        quoteFilter = quoteFilter,
                        onQuoteClick = {
                            if (hasPremium) {
                                val nextQuoteItem = utilsDB.getRandomFraseByAutor(
                                    context = context,
                                    autor = AUTHOR_DATABASE_NAME,
                                    onlyFav = quoteFilter.onlyFavorites,
                                    onlyWithNotes = quoteFilter.onlyWithNotes
                                )
                                quote = nextQuoteItem?.frase ?: placeholder
                                quoteFavState = nextQuoteItem?.fav ?: ""
                            }
                        },
                        onQuoteFilterChange = { newFilter ->
                            quoteFilter = newFilter
                            prefs.edit {
                                putBoolean(filterFavKey, newFilter.onlyFavorites)
                                putBoolean(filterNotesKey, newFilter.onlyWithNotes)
                            }
                        },
                        favoriteOptionLabel = if (!hasPremium) {
                            null
                        } else {
                            stringResource(
                                if (quoteFavState == "1") {
                                    R.string.author_remove_from_favorites
                                } else {
                                    R.string.author_add_to_favorites
                                }
                            )
                        },
                        onToggleFavorito = if (!hasPremium) {
                            null
                        } else {
                            {
                                if (quote.isNotBlank()) {
                                    val result = utilsDB.UpdateFavorito(
                                        context = context,
                                        tableName = DatabaseHelper.T_Frases,
                                        columnID = DatabaseHelper.C_frases_frase,
                                        id_str = quote,
                                        id_int = -1
                                    )
                                    if (result.isNotEmpty()) {
                                        quoteFavState = result
                                    }
                                }
                            }
                        },
                        onBiographyClick = {
                            biographyAssetPath?.let { openAsset(it, isPremiumPreview = false) }
                        },
                        onTeachingSummaryClick = {
                            teachingSummaryAssetPath?.let { openAsset(it, isPremiumPreview = !hasPremium) }
                        },
                        teachingSummaryEnabled = !teachingSummaryAssetPath.isNullOrBlank(),
                        resourcesSection = {
                            AuthorResourcesSection(
                                cards = cards,
                                hasPremium = hasPremium,
                                onResourceClick = { assetPath ->
                                    openAsset(assetPath, isPremiumPreview = !hasPremium)
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    private fun openAsset(assetPath: String, isPremiumPreview: Boolean) {
        FragContentWebView.elementLoaded = assetPath
        FragContentWebView.isPremiumPreviewMode = isPremiumPreview
        FragContentWebView.urlPath = "file:///android_asset/$assetPath"
        MainActivity.currentInstance()?.openDestinationAsSheet(R.id.frag_content_webview)
    }

    private fun bruceResourceCards(): List<AccessCardPlaceholder> {
        val evolutionMenu = (1..13).map { chapter ->
            AccessCardMenuItem(
                title = getString(R.string.author_chapter_format, chapter),
                assetPath = "autores/bruceLipton/materialBruce/serieEvolucionInterior/bruce_evolucion_interior_${chapter}.txt"
            )
        }
        return listOf(
            AccessCardPlaceholder(
                title = getString(R.string.author_title_biology_belief),
                primaryButton = getString(R.string.author_summary),
                primaryAssetPath = "autores/bruceLipton/libros/LaBiologiaDeLaCreencia/resumen_libro_biologiacreencia.txt",
                secondaryButton = getString(R.string.author_plan),
                secondaryAssetPath = "autores/bruceLipton/libros/LaBiologiaDeLaCreencia/plan_libro_biologiacreencia.txt"
            ),
            AccessCardPlaceholder(
                title = getString(R.string.author_title_inner_evolution_summary),
                primaryButton = getString(R.string.author_chapters),
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
                text = stringResource(R.string.author_resources_section_title),
                color = titleColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            if (!hasPremium) {
                Text(
                    text = stringResource(R.string.author_extended_version_available),
                    color = bodyColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (cards.isEmpty()) {
            Text(
                text = stringResource(R.string.author_no_resources),
                color = bodyColor,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            )
            return
        }

        val evolutionCards = cards.filter { it.menuItems.isNotEmpty() }
        val bookCards = cards.filter { it.menuItems.isEmpty() }

        if (bookCards.isNotEmpty()) {
            Text(
                text = stringResource(R.string.author_book_summary),
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
                text = stringResource(R.string.author_inner_evolution_summary_section),
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

    private companion object {
        const val AUTHOR_DATABASE_NAME = "Bruce Lipton"
    }
}
