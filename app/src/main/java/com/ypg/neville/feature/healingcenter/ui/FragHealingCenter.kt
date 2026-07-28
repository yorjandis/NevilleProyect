package com.ypg.neville.feature.healingcenter.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Emergency
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.Fragment
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.feature.healingcenter.data.HealingCenterPreferences
import com.ypg.neville.feature.healingcenter.domain.BundledHealingCatalogRepository
import com.ypg.neville.feature.healingcenter.domain.HealingCatalog
import com.ypg.neville.feature.healingcenter.domain.HealingProtocol
import com.ypg.neville.feature.healingcenter.domain.HealingSituation
import com.ypg.neville.model.subscription.SubscriptionManager

class FragHealingCenter : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            com.ypg.neville.ui.theme.NevilleTheme {
                val premiumReason = stringResource(R.string.paywall_reason_healing_center)
                val localeTag = LocalConfiguration.current.locales[0].toLanguageTag()
                val loadResult = remember(localeTag) {
                    runCatching { BundledHealingCatalogRepository(requireContext()).load() }
                }
                HealingCenterRoot(
                    loadResult = loadResult,
                    preferences = remember { HealingCenterPreferences(requireContext().applicationContext) },
                    onClose = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                    onShowPremium = {
                        (requireActivity() as? MainActivity)?.showSubscriptionPaywall(premiumReason)
                    }
                )
            }
        }
    }
}

private sealed interface HealingDestination {
    data object Home : HealingDestination
    data class Detail(val situation: HealingSituation) : HealingDestination
    data class Safety(val situation: HealingSituation, val protocol: HealingProtocol) : HealingDestination
    data class Session(val situation: HealingSituation, val protocol: HealingProtocol) : HealingDestination
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HealingCenterRoot(
    loadResult: Result<HealingCatalog>,
    preferences: HealingCenterPreferences,
    onClose: () -> Unit,
    onShowPremium: () -> Unit
) {
    val stack = remember { mutableStateListOf<HealingDestination>(HealingDestination.Home) }
    val destination = stack.last()
    var favorites by remember { mutableStateOf(preferences.favoriteSituationIds()) }
    var showEmergency by remember { mutableStateOf(false) }
    val subscription by SubscriptionManager.uiState.collectAsState()

    fun navigate(next: HealingDestination) { stack.add(next) }
    fun goBack() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex) else onClose()
    }

    BackHandler(onBack = ::goBack)

    val title = when (destination) {
        HealingDestination.Home -> stringResource(R.string.healing_center_title)
        is HealingDestination.Detail -> destination.situation.title
        is HealingDestination.Safety -> stringResource(R.string.healing_check_title)
        is HealingDestination.Session -> destination.protocol.title
    }
    val backDescription = stringResource(R.string.healing_back)
    val closeDescription = stringResource(R.string.healing_close)
    val addFavoriteDescription = stringResource(R.string.healing_add_favorite)
    val removeFavoriteDescription = stringResource(R.string.healing_remove_favorite)
    val urgentHelpDescription = stringResource(R.string.healing_open_urgent_help)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(title, color = Color.White, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = ::goBack) {
                        Icon(
                            if (stack.size > 1) Icons.AutoMirrored.Rounded.ArrowBack else Icons.Rounded.Close,
                            contentDescription = if (stack.size > 1) backDescription else closeDescription,
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    when (destination) {
                        is HealingDestination.Detail -> {
                            val isFavorite = destination.situation.id in favorites
                            IconButton(onClick = {
                                favorites = preferences.toggleFavorite(destination.situation.id)
                            }) {
                                Icon(
                                    if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                    contentDescription = if (isFavorite) removeFavoriteDescription else addFavoriteDescription,
                                    tint = if (isFavorite) Color(0xFFFFD740) else Color.White
                                )
                            }
                        }
                        is HealingDestination.Session -> IconButton(onClick = {
                            showEmergency = true
                        }) {
                            Icon(Icons.Rounded.Emergency, contentDescription = urgentHelpDescription, tint = Color.White)
                        }
                        else -> Unit
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().background(HealingBackground)) {
            when (destination) {
                HealingDestination.Home -> loadResult.fold(
                    onSuccess = { catalog ->
                        HealingCenterHomeScreen(
                            catalog = catalog,
                            favorites = favorites,
                            hasPremium = subscription.isEntitlementVerified && subscription.isActive,
                            contentPadding = padding,
                            onSituation = { navigate(HealingDestination.Detail(it)) },
                            onEmergency = { showEmergency = true },
                            onShowPremium = onShowPremium
                        )
                    },
                    onFailure = { HealingCatalogErrorScreen(padding) }
                )
                is HealingDestination.Detail -> HealingSituationDetailScreen(
                    situation = destination.situation,
                    catalog = loadResult.getOrNull() ?: return@Box,
                    contentPadding = padding,
                    onProtocol = { navigate(HealingDestination.Safety(destination.situation, it)) },
                    onEmergency = { showEmergency = true }
                )
                is HealingDestination.Safety -> HealingSafetyGateScreen(
                    situation = destination.situation,
                    protocol = destination.protocol,
                    contentPadding = padding,
                    onEmergency = { showEmergency = true },
                    onStart = { navigate(HealingDestination.Session(destination.situation, destination.protocol)) }
                )
                is HealingDestination.Session -> HealingGuidedSessionScreen(
                    situation = destination.situation,
                    protocol = destination.protocol,
                    contentPadding = padding,
                    emergencyVisible = showEmergency,
                    onExit = ::goBack,
                    onEmergency = { showEmergency = true }
                )
            }
        }
    }

    if (showEmergency) {
        Dialog(
            onDismissRequest = { showEmergency = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.healing_urgent_help), color = Color.White) },
                            navigationIcon = {
                                IconButton(onClick = { showEmergency = false }) {
                                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.healing_close), tint = Color.White)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                        )
                    }
                ) { emergencyPadding ->
                    Box(Modifier.fillMaxSize().background(HealingBackground)) {
                        HealingEmergencyResourcesScreen(
                            preferences = preferences,
                            contentPadding = emergencyPadding
                        )
                    }
                }
            }
        }
    }
}
