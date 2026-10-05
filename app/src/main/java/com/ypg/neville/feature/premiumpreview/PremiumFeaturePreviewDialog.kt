package com.ypg.neville.feature.premiumpreview

import android.app.Dialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.ViewGroup
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog as ComposeDialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.DialogFragment
import androidx.core.graphics.drawable.toDrawable
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import kotlin.math.roundToInt

class PremiumFeaturePreviewDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val feature = PremiumFeatureId.fromWireName(arguments?.getString(ARG_FEATURE))
            ?: error("Unknown premium feature")
        val host = requireActivity() as? MainActivity

        return Dialog(requireContext(), R.style.Theme_NevilleProyect_CenterDialog).apply {
            setCanceledOnTouchOutside(false)
            setContentView(
                ComposeView(requireContext()).apply {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                    setContent {
                        com.ypg.neville.ui.theme.NevilleTheme {
                            PremiumFeaturePreview(
                                feature = feature,
                                onClose = ::dismiss,
                                onOpenPurchase = {
                                    dismiss()
                                    host?.window?.decorView?.post {
                                        host.showSubscriptionPaywall()
                                    }
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
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())
        }
    }

    companion object {
        private const val ARG_FEATURE = "arg_feature"
        const val TAG = "PremiumFeaturePreviewDialog"

        fun newInstance(feature: PremiumFeatureId): PremiumFeaturePreviewDialog =
            PremiumFeaturePreviewDialog().apply {
                arguments = Bundle().apply { putString(ARG_FEATURE, feature.wireName) }
            }
    }
}

@Composable
private fun PremiumFeaturePreview(
    feature: PremiumFeatureId,
    onClose: () -> Unit,
    onOpenPurchase: () -> Unit
) {
    var fullscreenIndex by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumBackground)
    ) {
        AnimatedBackgroundGlow()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(feature.titleRes),
                modifier = Modifier.padding(horizontal = 38.dp),
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )

            if (feature.screenshotNames.isNotEmpty()) {
                ScreenshotGallery(feature) { fullscreenIndex = it }
            }

            PremiumValueCard(feature)
            PremiumPurchaseCallToAction(onOpenPurchase)
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(34.dp)
                .background(Color.Black.copy(alpha = 0.18f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.premium_preview_close),
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }

    fullscreenIndex?.let { initialIndex ->
        ScreenshotFullscreenDialog(
            feature = feature,
            initialIndex = initialIndex,
            onClose = { fullscreenIndex = null }
        )
    }
}

@Composable
private fun BoxScope.AnimatedBackgroundGlow() {
    val transition = rememberInfiniteTransition(label = "premium-glow")
    val shift by transition.animateFloat(
        initialValue = -36f,
        targetValue = 42f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5_500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "premium-glow-shift"
    )

    Box(
        Modifier
            .size(320.dp)
            .align(Alignment.TopStart)
            .offset {
                IntOffset(
                    x = ((shift - 110f) * density).roundToInt(),
                    y = (-120.dp).roundToPx()
                )
            }
            .blur(70.dp)
            .background(Color.Cyan.copy(alpha = 0.22f), CircleShape)
    )
    Box(
        Modifier
            .size(390.dp)
            .align(Alignment.BottomEnd)
            .offset {
                IntOffset(
                    x = ((110f - shift) * density).roundToInt(),
                    y = 120.dp.roundToPx()
                )
            }
            .blur(85.dp)
            .background(Color(0xFFB060E8).copy(alpha = 0.30f), CircleShape)
    )
}

@Composable
private fun ScreenshotGallery(feature: PremiumFeatureId, onOpen: (Int) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(feature.screenshotNames, key = { _, name -> name }) { index, name ->
            ScreenshotCard(feature, name, index, onOpen)
        }
    }
}

@Composable
private fun ScreenshotCard(
    feature: PremiumFeatureId,
    screenshotName: String,
    index: Int,
    onOpen: (Int) -> Unit
) {
    val title = stringResource(feature.titleRes)
    val description = stringResource(
        R.string.premium_preview_screenshot_accessibility,
        index + 1,
        title
    )
    val image = rememberPremiumScreenshot(feature, screenshotName)

    Box(
        modifier = Modifier
            .width(150.dp)
            .height(270.dp)
            .semantics { contentDescription = description }
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.16f))
            .border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(24.dp))
            .clickable { onOpen(index) },
        contentAlignment = Alignment.Center
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            ScreenshotPlaceholder(feature, index + 1)
        }
    }
}

@Composable
private fun PremiumValueCard(feature: PremiumFeatureId) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.11f), RoundedCornerShape(22.dp))
            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        ValueSection(
            icon = Icons.Rounded.AutoAwesome,
            title = stringResource(R.string.premium_preview_what_it_does),
            body = stringResource(feature.descriptionRes)
        )
        HorizontalDivider(color = Color.White.copy(alpha = 0.18f))
        ValueSection(
            icon = Icons.AutoMirrored.Rounded.TrendingUp,
            title = stringResource(R.string.premium_preview_practical_value),
            body = stringResource(feature.practicalValueRes)
        )
    }
}

@Composable
private fun ValueSection(icon: ImageVector, title: String, body: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(11.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(Color.Cyan.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.Cyan, modifier = Modifier.size(19.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(
                body,
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 21.sp
            )
        }
    }
}

@Composable
private fun PremiumPurchaseCallToAction(onOpenPurchase: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(18.dp, RoundedCornerShape(19.dp), ambientColor = Color.Cyan, spotColor = Color.Cyan)
                .clip(RoundedCornerShape(19.dp))
                .background(
                    Brush.linearGradient(listOf(Color.White, Color(0xFFD6F0FF)))
                )
                .clickable(onClick = onOpenPurchase)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = PremiumButtonText)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.premium_preview_cta),
                    color = PremiumButtonText,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    stringResource(R.string.premium_preview_cta_detail),
                    color = PremiumButtonText.copy(alpha = 0.76f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = PremiumButtonText)
        }

        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.size(17.dp)
            )
            Text(
                stringResource(R.string.premium_preview_annual_note),
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ScreenshotFullscreenDialog(
    feature: PremiumFeatureId,
    initialIndex: Int,
    onClose: () -> Unit
) {
    ComposeDialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val pagerState = rememberPagerState(
            initialPage = initialIndex.coerceIn(feature.screenshotNames.indices),
            pageCount = { feature.screenshotNames.size }
        )
        val title = stringResource(feature.titleRes)

        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val image = rememberPremiumScreenshot(feature, feature.screenshotNames[page])
                val description = stringResource(
                    R.string.premium_preview_fullscreen_accessibility,
                    page + 1,
                    feature.screenshotNames.size,
                    title
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = description }
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (image != null) {
                        Image(
                            bitmap = image,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(0.86f)
                                .aspectRatio(0.77f)
                                .clip(RoundedCornerShape(26.dp))
                        ) {
                            ScreenshotPlaceholder(feature, page + 1)
                        }
                    }
                }
            }

            if (feature.screenshotNames.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 22.dp)
                        .background(Color.Black.copy(alpha = 0.42f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    feature.screenshotNames.indices.forEach { index ->
                        Box(
                            Modifier
                                .size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                                .background(
                                    if (index == pagerState.currentPage) Color.White else Color.White.copy(alpha = 0.34f),
                                    CircleShape
                                )
                        )
                    }
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(18.dp)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.72f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.50f), CircleShape)
            ) {
                Icon(Icons.Rounded.Close, stringResource(R.string.premium_preview_close), tint = Color.White)
            }
        }
    }
}

@Composable
private fun ScreenshotPlaceholder(feature: PremiumFeatureId, position: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.20f),
                        Color.Cyan.copy(alpha = 0.20f),
                        Color(0xFF9C4DCC).copy(alpha = 0.28f)
                    )
                )
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { dot ->
                Box(
                    Modifier
                        .size(7.dp)
                        .background(
                            if (dot == 0) Color(0xFFFF6FAF).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.38f),
                            CircleShape
                        )
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Icon(featureIcon(feature), contentDescription = null, tint = Color.White.copy(alpha = 0.92f), modifier = Modifier.size(42.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(feature.titleRes),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(7.dp))
        Text(
            stringResource(R.string.premium_preview_placeholder),
            color = Color.White.copy(alpha = 0.64f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.weight(1f))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            repeat(3) { item ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(
                            Color.White.copy(alpha = if (item == position % 3) 0.58f else 0.19f),
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun rememberPremiumScreenshot(feature: PremiumFeatureId, screenshotName: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(feature, screenshotName) {
        val basePath = "${feature.assetPath}/$screenshotName"
        listOf("png", "webp", "jpg", "jpeg")
            .firstNotNullOfOrNull { extension ->
                runCatching {
                    context.assets.open("$basePath.$extension").use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }.getOrNull()
            }
    }
}

private fun featureIcon(feature: PremiumFeatureId): ImageVector = when (feature) {
    PremiumFeatureId.AGENDA -> Icons.Rounded.CalendarMonth
    PremiumFeatureId.HEALING_CENTER -> Icons.Rounded.HealthAndSafety
    PremiumFeatureId.CARDIO_COHERENCE -> Icons.Rounded.Favorite
    PremiumFeatureId.CALM_SPACE -> Icons.Rounded.Spa
    PremiumFeatureId.INTEGRATED_AI -> Icons.Rounded.Psychology
    PremiumFeatureId.CREATIVE_CANVAS -> Icons.Rounded.Palette
    PremiumFeatureId.GOALS -> Icons.Rounded.Flag
    PremiumFeatureId.PROTECTED_NOTES -> Icons.Rounded.Lock
    PremiumFeatureId.CONSCIOUS_PRESENCE -> Icons.Rounded.SelfImprovement
    PremiumFeatureId.TRANSFORMATION_PROTOCOL -> Icons.Rounded.Psychology
    PremiumFeatureId.SMART_REMINDERS -> Icons.Rounded.Notifications
    PremiumFeatureId.WEEKLY_REVIEW -> Icons.Rounded.CalendarMonth
    PremiumFeatureId.CONSCIOUS_DAILY_CYCLE -> Icons.Rounded.WbSunny
    PremiumFeatureId.SMART_COPIED_TEXT -> Icons.Rounded.ContentPaste
    PremiumFeatureId.EXTENDED_CONTENT -> Icons.AutoMirrored.Rounded.MenuBook
}

private val PremiumBackground = Brush.linearGradient(
    listOf(Color(0xFF12337A), Color(0xFF3040AD), Color(0xFF6E45B8))
)
private val PremiumButtonText = Color(0xFF1A296B)
