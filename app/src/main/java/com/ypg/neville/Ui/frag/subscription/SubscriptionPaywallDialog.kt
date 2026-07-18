package com.ypg.neville.ui.frag

import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import androidx.fragment.app.DialogFragment
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.subscription.SubscriptionManager

class SubscriptionPaywallDialog : DialogFragment() {

    private data class PremiumFeature(
        val icon: ImageVector,
        @param:StringRes val titleRes: Int,
        @param:StringRes val descriptionRes: Int
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val reason = arguments?.getString(ARG_REASON).orEmpty()
        val host = requireActivity() as? MainActivity

        return Dialog(requireContext(), R.style.Theme_NevilleProyect_CenterDialog).apply {
            setCanceledOnTouchOutside(true)
            setContentView(
                ComposeView(requireContext()).apply {
                    setContent {
                        com.ypg.neville.ui.theme.NevilleTheme {
                            PaywallContent(
                                reason = reason,
                                onClose = { dismiss() },
                                onSubscribe = {
                                    val launched = host?.let {
                                        SubscriptionManager.launchPurchase(it)
                                    } == true
                                    if (!launched) {
                                        Toast.makeText(
                                            requireContext(),
                                            R.string.paywall_purchase_start_failed,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                onRestore = {
                                    SubscriptionManager.restorePurchases()
                                    Toast.makeText(
                                        requireContext(),
                                        R.string.paywall_restoring_purchases,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    }
                }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    @Composable
    private fun PaywallContent(
        reason: String,
        onClose: () -> Unit,
        onSubscribe: () -> Unit,
        onRestore: () -> Unit
    ) {
        val state by SubscriptionManager.uiState.collectAsState()
        val hasPremium = state.isActive
        val hasTrial = state.hasIntroductoryTrial
        val price = state.productPrice ?: stringResource(R.string.paywall_price_fallback)

        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFD7EFFA),
                                Color(0xFFEBE4F7),
                                Color(0xFFC2DDF3)
                            )
                        )
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 430.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(
                            if (hasPremium) {
                                R.string.paywall_subscription_active
                            } else {
                                R.string.paywall_extended_version_title
                            }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF082F3B)
                    )
                    Text(
                        text = if (hasPremium) {
                            stringResource(R.string.paywall_active_subtitle)
                        } else {
                            stringResource(R.string.paywall_access_all_premium)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    if (!hasPremium) {
                        Text(
                            text = stringResource(R.string.paywall_first_week_free),
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .background(
                                    Color.White.copy(alpha = 0.58f),
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF083B3B)
                        )
                        Text(
                            text = stringResource(R.string.paywall_intro_offer_eligibility),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF173455)
                        )
                    }

                    if (reason.isNotBlank()) {
                        Text(
                            text = reason,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color.White.copy(alpha = 0.38f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF173455)
                        )
                    }

                    premiumFeatures.forEachIndexed { index, feature ->
                        FeatureCard(
                            feature = feature,
                            backgroundColor = featureCardColors[index % featureCardColors.size]
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0x6622406B), RoundedCornerShape(14.dp))
                        .background(
                            Color.White.copy(alpha = 0.28f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(vertical = 10.dp, horizontal = 12.dp)
                ) {
                    Text(
                        text = if (hasTrial) {
                            stringResource(R.string.paywall_trial_price_format, price)
                        } else {
                            stringResource(R.string.paywall_annual_price_format, price)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF173455)
                    )
                }

                Button(
                    onClick = onSubscribe,
                    enabled = !hasPremium,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1678D3))
                ) {
                    Text(
                        text = when {
                            hasPremium -> stringResource(R.string.paywall_subscription_active)
                            hasTrial -> stringResource(R.string.paywall_start_free_trial)
                            else -> stringResource(R.string.paywall_subscribe_now)
                        },
                        color = if (hasPremium) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!hasPremium) {
                    Text(
                        text = if (hasTrial) {
                            stringResource(R.string.paywall_trial_renewal_format, price)
                        } else {
                            stringResource(R.string.paywall_annual_renewal)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF173455)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onRestore, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.paywall_restore), color = Color.Black)
                    }
                    OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.paywall_close), color = Color.Black)
                    }
                }

                Text(
                    text = stringResource(R.string.paywall_neville_remains_free),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    @Composable
    private fun FeatureCard(feature: PremiumFeature, backgroundColor: Color) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor, RoundedCornerShape(10.dp))
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.55f),
                    RoundedCornerShape(10.dp)
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = Color(0xFF0D5757),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(feature.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0A3B39)
                )
            }
            Text(
                text = stringResource(feature.descriptionRes),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black.copy(alpha = 0.82f)
            )
        }
    }

    companion object {
        private val featureCardColors = listOf(
            Color(0xFFDCEFD8),
            Color(0xFFD4E9D1),
            Color(0xFFE4F2DC),
            Color(0xFFCFE5D3)
        )

        private val premiumFeatures = listOf(
            PremiumFeature(
                Icons.Outlined.Star,
                R.string.premium_exclusive_content_title,
                R.string.premium_exclusive_content_description
            ),
            PremiumFeature(
                Icons.Outlined.CheckCircle,
                R.string.premium_goals_title,
                R.string.premium_goals_description
            ),
            PremiumFeature(
                Icons.Outlined.Palette,
                R.string.premium_canvas_title,
                R.string.premium_canvas_description
            ),
            PremiumFeature(
                Icons.Outlined.WbSunny,
                R.string.premium_morning_ritual_title,
                R.string.premium_morning_ritual_description
            ),
            PremiumFeature(
                Icons.Outlined.Event,
                R.string.premium_evening_ritual_title,
                R.string.premium_evening_ritual_description
            ),
            PremiumFeature(
                Icons.Outlined.CalendarMonth,
                R.string.premium_agenda_title,
                R.string.premium_agenda_description
            ),
            PremiumFeature(
                Icons.Outlined.Favorite,
                R.string.premium_coherence_title,
                R.string.premium_coherence_description
            ),
            PremiumFeature(
                Icons.Outlined.Spa,
                R.string.premium_presence_title,
                R.string.premium_presence_description
            ),
            PremiumFeature(
                Icons.Outlined.Spa,
                R.string.premium_calm_space_title,
                R.string.premium_calm_space_description
            ),
            PremiumFeature(
                Icons.Outlined.Spa,
                R.string.premium_healing_center_title,
                R.string.premium_healing_center_description
            ),
            PremiumFeature(
                Icons.Outlined.Notifications,
                R.string.premium_reminders_title,
                R.string.premium_reminders_description
            ),
            PremiumFeature(
                Icons.AutoMirrored.Outlined.MenuBook,
                R.string.premium_encyclopedia_title,
                R.string.premium_encyclopedia_description
            ),
            PremiumFeature(
                Icons.Outlined.Science,
                R.string.premium_scientific_evidence_title,
                R.string.premium_scientific_evidence_description
            ),
            PremiumFeature(
                Icons.Outlined.Psychology,
                R.string.premium_emotional_anchors_title,
                R.string.premium_emotional_anchors_description
            ),
            PremiumFeature(
                Icons.Outlined.Mic,
                R.string.premium_voice_notes_title,
                R.string.premium_voice_notes_description
            ),
            PremiumFeature(
                Icons.Outlined.Event,
                R.string.premium_weekly_summary_title,
                R.string.premium_weekly_summary_description
            ),
            PremiumFeature(
                Icons.Outlined.Lock,
                R.string.premium_protected_notes_title,
                R.string.premium_protected_notes_description
            ),
            PremiumFeature(
                Icons.Outlined.ContentPaste,
                R.string.premium_paste_into_title,
                R.string.premium_paste_into_description
            ),
            PremiumFeature(
                Icons.Outlined.Edit,
                R.string.premium_health_quotes_title,
                R.string.premium_health_quotes_description
            )
        )

        private const val ARG_REASON = "arg_reason"
        const val TAG = "SubscriptionPaywallDialog"

        fun newInstance(reason: String?): SubscriptionPaywallDialog {
            return SubscriptionPaywallDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_REASON, reason)
                }
            }
        }
    }
}
