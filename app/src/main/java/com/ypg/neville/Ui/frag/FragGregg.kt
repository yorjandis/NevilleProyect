package com.ypg.neville.ui.frag

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.ypg.neville.model.subscription.SubscriptionManager
import com.ypg.neville.R
import com.ypg.neville.model.db.utilsDB

class FragGregg : Fragment() {

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                com.ypg.neville.ui.theme.NevilleTheme {
                    val author = getString(R.string.gregg_braden)
                    val authorAssetsFolder = "autores/greggBraden"
                    val context = requireContext()
                    val navController = this@FragGregg.findNavController()
                    val subscriptionState by SubscriptionManager.uiState.collectAsState()
                    val hasPremium = subscriptionState.isActive
                    val biographyAssetPath = remember { loadAuthorBiographyAssetPath(context, authorAssetsFolder) }
                    val teachingSummaryAssetPath = remember { loadAuthorTeachingSummaryAssetPath(context, authorAssetsFolder) }
                    val cards = remember { loadAuthorResourceCards(context, authorAssetsFolder) }
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
                        imageRes = R.drawable.gregg,
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
                            teachingSummaryAssetPath?.let { openAsset(navController, it, isPremiumPreview = false) }
                        },
                        teachingSummaryEnabled = !teachingSummaryAssetPath.isNullOrBlank(),
                        cards = cards,
                        hasResourceAccess = true,
                        onResourceClick = { assetPath ->
                            openAsset(navController, assetPath, isPremiumPreview = !hasPremium)
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
}
