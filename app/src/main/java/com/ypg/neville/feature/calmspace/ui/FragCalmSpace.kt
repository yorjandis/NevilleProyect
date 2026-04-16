package com.ypg.neville.feature.calmspace.ui

import android.graphics.BitmapFactory
import android.graphics.Paint
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.ypg.neville.MainActivity
import org.json.JSONArray
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class CalmSphere(
    val id: Int,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    val phrase: String,
    val tint: Color,
    val bornAtMs: Long
)

data class BurstParticle(
    val id: Int,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    val lifeMs: Long,
    val bornAtMs: Long,
    val color: Color
)

data class PhraseReveal(
    val id: Int,
    val phrase: String,
    val x: Float,
    val y: Float,
    val bornAtMs: Long,
    val revealDelayMs: Long = 450L
)

data class SphereDeformation(
    val angleRad: Float,
    val strength: Float,
    val bornAtMs: Long
)

private class CalmPreferences(private val fragment: Fragment) {
    private val prefs = com.ypg.neville.model.preferences.DbPreferences.named(
        fragment.requireContext(),
        "calm_space_prefs"
    )

    fun sphereCount(): Int = prefs.getInt(KEY_SPHERES, 10).coerceIn(3, 24)

    fun saveSphereCount(value: Int) {
        prefs.edit().putInt(KEY_SPHERES, value.coerceIn(3, 24)).apply()
    }

    fun useFixedBackground(): Boolean = prefs.getBoolean(KEY_USE_FIXED_BG, false)

    fun saveUseFixedBackground(value: Boolean) {
        prefs.edit().putBoolean(KEY_USE_FIXED_BG, value).apply()
    }

    fun fixedBackgroundName(): String? = prefs.getString(KEY_FIXED_BG_NAME, null)

    fun saveFixedBackgroundName(value: String) {
        prefs.edit().putString(KEY_FIXED_BG_NAME, value).apply()
    }

    fun useFixedMusic(): Boolean = prefs.getBoolean(KEY_USE_FIXED_MUSIC, true)

    fun saveUseFixedMusic(value: Boolean) {
        prefs.edit().putBoolean(KEY_USE_FIXED_MUSIC, value).apply()
    }

    fun fixedMusicName(): String? = prefs.getString(KEY_FIXED_MUSIC_NAME, DEFAULT_MUSIC_ASSET)

    fun saveFixedMusicName(value: String) {
        prefs.edit().putString(KEY_FIXED_MUSIC_NAME, value).apply()
    }

    companion object {
        private const val KEY_SPHERES = "calm_spheres"
        private const val KEY_USE_FIXED_BG = "calm_use_fixed_background"
        private const val KEY_FIXED_BG_NAME = "calm_fixed_background_name"
        private const val KEY_USE_FIXED_MUSIC = "calm_use_fixed_music"
        private const val KEY_FIXED_MUSIC_NAME = "calm_fixed_music_name"
        private const val DEFAULT_MUSIC_ASSET = "calma_musica_3.mp3"
    }
}

class FragCalmSpace : Fragment() {

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (view as ComposeView).setContent {
            com.ypg.neville.ui.theme.NevilleTheme {
                CalmSpaceScreen()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        MainActivity.currentInstance()?.icToolsBarFraseAdd?.visibility = View.GONE
        MainActivity.currentInstance()?.icToolsBarNotaAdd?.visibility = View.GONE
        MainActivity.currentInstance()?.icToolsBarFav?.visibility = View.GONE
        MainActivity.currentInstance()?.setBottomNavVisible(false)
    }

    override fun onStop() {
        super.onStop()
        MainActivity.currentInstance()?.icToolsBarFraseAdd?.visibility = View.VISIBLE
        MainActivity.currentInstance()?.icToolsBarNotaAdd?.visibility = View.VISIBLE
        MainActivity.currentInstance()?.setBottomNavVisible(true)
    }

    @Composable
    private fun CalmSpaceScreen() {
        val context = LocalContext.current
        val prefs = remember { CalmPreferences(this) }
        val random = remember { Random(System.currentTimeMillis()) }
        val calmPhrases = remember { loadCalmPhrasesFromAsset() }

        val backgroundAssets = remember { listAssetsByPattern("Calma_ImagenesFondo", Regex("""calma_fondo_(\d+)\.(jpg|jpeg|png|webp)""")) }
        val musicAssets = remember { listAssetsByPattern("Calma_MusicaFondo", Regex("""calma_musica_(\d+)\.(mp3|ogg|wav|m4a)""")) }

        var targetSphereCount by remember { mutableIntStateOf(prefs.sphereCount()) }
        var useFixedBackground by remember { mutableStateOf(prefs.useFixedBackground()) }
        var fixedBackgroundName by remember { mutableStateOf(prefs.fixedBackgroundName()) }
        var useFixedMusic by remember { mutableStateOf(prefs.useFixedMusic()) }
        var fixedMusicName by remember { mutableStateOf(prefs.fixedMusicName()) }

        var selectedBackground by remember {
            mutableStateOf(
                resolveSelectedAsset(
                    allAssets = backgroundAssets,
                    useFixed = useFixedBackground,
                    fixedAssetName = fixedBackgroundName,
                    random = random
                )
            )
        }
        var selectedMusic by remember {
            mutableStateOf(
                resolveSelectedAsset(
                    allAssets = musicAssets,
                    useFixed = useFixedMusic,
                    fixedAssetName = fixedMusicName,
                    random = random
                )
            )
        }

        val spheres = remember { mutableStateListOf<CalmSphere>() }
        val particles = remember { mutableStateListOf<BurstParticle>() }
        val phraseReveals = remember { mutableStateListOf<PhraseReveal>() }
        val deformations = remember { mutableStateMapOf<Int, SphereDeformation>() }

        var nextSphereId by remember { mutableIntStateOf(1) }
        var nextFxId by remember { mutableIntStateOf(1) }
        var viewportWidth by remember { mutableStateOf(0f) }
        var viewportHeight by remember { mutableStateOf(0f) }
        var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

        var showBackgroundMenu by remember { mutableStateOf(false) }
        var showMusicMenu by remember { mutableStateOf(false) }
        var showSettings by remember { mutableStateOf(false) }

        fun spawnSphere(): CalmSphere {
            val radius = random.nextInt(34, 78).toFloat()
            val x = random.nextFloat().coerceIn(0.05f, 0.95f) * max(viewportWidth, radius * 2f)
            val y = random.nextFloat().coerceIn(0.10f, 0.90f) * max(viewportHeight, radius * 2f)
            val speed = random.nextFloat() * 70f + 30f
            val angle = random.nextFloat() * (2f * PI.toFloat())
            val phrase = calmPhrases.random(random)
            val color = Color.hsv(
                hue = random.nextFloat() * 180f + 170f,
                saturation = random.nextFloat() * 0.24f + 0.20f,
                value = 1f,
                alpha = 0.42f
            )
            return CalmSphere(
                id = nextSphereId++,
                x = x,
                y = y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                radius = radius,
                phrase = phrase,
                tint = color,
                bornAtMs = System.currentTimeMillis()
            )
        }

        fun ensureSphereTarget() {
            if (viewportWidth <= 0f || viewportHeight <= 0f) return
            while (spheres.size < targetSphereCount) {
                spheres.add(spawnSphere())
            }
            while (spheres.size > targetSphereCount) {
                spheres.removeLastOrNull()
            }
        }

        fun restartMusicForSelection() {
            selectedMusic = resolveSelectedAsset(
                allAssets = musicAssets,
                useFixed = useFixedMusic,
                fixedAssetName = fixedMusicName,
                random = random
            )
        }

        val backgroundBitmap = remember(selectedBackground) {
            val assetName = selectedBackground ?: return@remember null
            runCatching {
                context.assets.open("Calma_ImagenesFondo/$assetName").use { input ->
                    BitmapFactory.decodeStream(input)?.asImageBitmap()
                }
            }.getOrNull()
        }

        DisposableEffect(selectedMusic) {
            val music = selectedMusic
            if (music.isNullOrBlank()) return@DisposableEffect onDispose { }

            val player = runCatching {
                val afd = context.assets.openFd("Calma_MusicaFondo/$music")
                MediaPlayer().apply {
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    isLooping = true
                    setVolume(0.38f, 0.38f)
                    prepare()
                    start()
                }
            }.getOrNull()

            onDispose {
                runCatching {
                    player?.stop()
                    player?.release()
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }

            LaunchedEffect(widthPx, heightPx, targetSphereCount) {
                viewportWidth = widthPx
                viewportHeight = heightPx
                ensureSphereTarget()
            }

            LaunchedEffect(Unit) {
                var lastFrameNs = 0L
                while (isActive) {
                    val frameNs = withFrameNanos { it }
                    nowMs = System.currentTimeMillis()
                    if (lastFrameNs == 0L) {
                        lastFrameNs = frameNs
                        continue
                    }

                    val dt = ((frameNs - lastFrameNs) / 1_000_000_000f).coerceIn(0.008f, 0.030f)
                    lastFrameNs = frameNs

                    if (viewportWidth <= 0f || viewportHeight <= 0f) continue

                    for (i in spheres.indices) {
                        val item = spheres[i]
                        var nx = item.x + item.vx * dt
                        var ny = item.y + item.vy * dt
                        var nvx = item.vx
                        var nvy = item.vy
                        var wallImpactAngleRad: Float? = null
                        var wallImpactSpeed = 0f

                        if (nx - item.radius < 0f) {
                            wallImpactSpeed = max(wallImpactSpeed, abs(nvx))
                            wallImpactAngleRad = (PI.toFloat() * 0.5f)
                            nx = item.radius
                            nvx = abs(nvx) * WALL_RESTITUTION
                        } else if (nx + item.radius > viewportWidth) {
                            wallImpactSpeed = max(wallImpactSpeed, abs(nvx))
                            wallImpactAngleRad = (PI.toFloat() * 0.5f)
                            nx = viewportWidth - item.radius
                            nvx = -abs(nvx) * WALL_RESTITUTION
                        }

                        if (ny - item.radius < 0f) {
                            wallImpactSpeed = max(wallImpactSpeed, abs(nvy))
                            wallImpactAngleRad = 0f
                            ny = item.radius
                            nvy = abs(nvy) * WALL_RESTITUTION
                        } else if (ny + item.radius > viewportHeight) {
                            wallImpactSpeed = max(wallImpactSpeed, abs(nvy))
                            wallImpactAngleRad = 0f
                            ny = viewportHeight - item.radius
                            nvy = -abs(nvy) * WALL_RESTITUTION
                        }

                        val adjusted = enforceMinimumSpeed(nvx, nvy)
                        spheres[i] = item.copy(x = nx, y = ny, vx = adjusted.first, vy = adjusted.second)
                        if (wallImpactAngleRad != null && wallImpactSpeed > 18f) {
                            val strength = (wallImpactSpeed / 300f).coerceIn(0.02f, 0.08f)
                            deformations[item.id] = SphereDeformation(
                                angleRad = wallImpactAngleRad,
                                strength = strength,
                                bornAtMs = nowMs
                            )
                        }
                    }

                    for (i in 0 until spheres.size) {
                        for (j in i + 1 until spheres.size) {
                            val a = spheres[i]
                            val b = spheres[j]
                            val dx = b.x - a.x
                            val dy = b.y - a.y
                            val dist = sqrt(dx * dx + dy * dy)
                            val minDist = a.radius + b.radius
                            if (dist <= 0.001f || dist >= minDist) continue

                            val nx = dx / dist
                            val ny = dy / dist
                            val tx = -ny
                            val ty = nx

                            val m1 = a.radius * a.radius
                            val m2 = b.radius * b.radius

                            val v1n = a.vx * nx + a.vy * ny
                            val v1t = a.vx * tx + a.vy * ty
                            val v2n = b.vx * nx + b.vy * ny
                            val v2t = b.vx * tx + b.vy * ty
                            val impactSpeed = abs(v1n - v2n)

                            val v1nAfter = ((v1n * (m1 - m2)) + (2f * m2 * v2n)) / (m1 + m2)
                            val v2nAfter = ((v2n * (m2 - m1)) + (2f * m1 * v1n)) / (m1 + m2)

                            val correctedV1n = v1nAfter * SPHERE_RESTITUTION
                            val correctedV2n = v2nAfter * SPHERE_RESTITUTION

                            val avx = correctedV1n * nx + v1t * tx
                            val avy = correctedV1n * ny + v1t * ty
                            val bvx = correctedV2n * nx + v2t * tx
                            val bvy = correctedV2n * ny + v2t * ty

                            val overlap = (minDist - dist).coerceAtLeast(0f)
                            val totalMass = m1 + m2
                            val shiftA = overlap * (m2 / totalMass)
                            val shiftB = overlap * (m1 / totalMass)

                            val adjustedA = enforceMinimumSpeed(avx, avy)
                            spheres[i] = a.copy(
                                x = (a.x - nx * shiftA).coerceIn(a.radius, viewportWidth - a.radius),
                                y = (a.y - ny * shiftA).coerceIn(a.radius, viewportHeight - a.radius),
                                vx = adjustedA.first,
                                vy = adjustedA.second
                            )
                            val adjustedB = enforceMinimumSpeed(bvx, bvy)
                            spheres[j] = b.copy(
                                x = (b.x + nx * shiftB).coerceIn(b.radius, viewportWidth - b.radius),
                                y = (b.y + ny * shiftB).coerceIn(b.radius, viewportHeight - b.radius),
                                vx = adjustedB.first,
                                vy = adjustedB.second
                            )
                            if (impactSpeed > 26f) {
                                val stretchAngle = (atan2(ny, nx) + (PI.toFloat() * 0.5f))
                                val strength = (impactSpeed / 360f).coerceIn(0.03f, 0.10f)
                                deformations[a.id] = SphereDeformation(
                                    angleRad = stretchAngle,
                                    strength = strength,
                                    bornAtMs = nowMs
                                )
                                deformations[b.id] = SphereDeformation(
                                    angleRad = stretchAngle,
                                    strength = strength,
                                    bornAtMs = nowMs
                                )
                            }
                        }
                    }

                    if (particles.isNotEmpty()) {
                        val current = nowMs
                        val updated = particles.mapNotNull { particle ->
                            val age = current - particle.bornAtMs
                            if (age >= particle.lifeMs) return@mapNotNull null
                            val gravity = 45f
                            particle.copy(
                                x = particle.x + particle.vx * dt,
                                y = particle.y + particle.vy * dt,
                                vy = particle.vy + gravity * dt
                            )
                        }
                        particles.clear()
                        particles.addAll(updated)
                    }

                    if (phraseReveals.isNotEmpty()) {
                        val current = nowMs
                        phraseReveals.removeAll { current - it.bornAtMs > (it.revealDelayMs + PHRASE_LIFETIME_MS) }
                    }
                    if (deformations.isNotEmpty()) {
                        val current = nowMs
                        val expired = deformations.filterValues { current - it.bornAtMs > DEFORMATION_MS }.keys
                        expired.forEach { deformations.remove(it) }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (backgroundBitmap != null) {
                    Image(
                        bitmap = backgroundBitmap,
                        contentDescription = "Fondo de calma",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF7CA7B7), Color(0xFFB2D3C7), Color(0xFFCDE5E9))
                                )
                            )
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.16f))
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(spheres.size, viewportWidth, viewportHeight) {
                            detectTapGestures { offset ->
                                val hit = spheres
                                    .asReversed()
                                    .firstOrNull { sphere ->
                                        val dx = sphere.x - offset.x
                                        val dy = sphere.y - offset.y
                                        (dx * dx + dy * dy) <= sphere.radius * sphere.radius
                                    }

                                if (hit != null) {
                                    spheres.remove(hit)
                                    phraseReveals.add(
                                        PhraseReveal(
                                            id = nextFxId++,
                                            phrase = hit.phrase,
                                            x = hit.x,
                                            y = hit.y,
                                            bornAtMs = System.currentTimeMillis(),
                                            revealDelayMs = PHRASE_REVEAL_DELAY_MS
                                        )
                                    )
                                    repeat(18) {
                                        val angle = Random.nextFloat() * (2f * PI.toFloat())
                                        val speed = Random.nextFloat() * 220f + 70f
                                        particles.add(
                                            BurstParticle(
                                                id = nextFxId++,
                                                x = hit.x,
                                                y = hit.y,
                                                vx = cos(angle) * speed,
                                                vy = sin(angle) * speed,
                                                radius = Random.nextFloat() * 7f + 2f,
                                                lifeMs = Random.nextLong(500L, 1100L),
                                                bornAtMs = System.currentTimeMillis(),
                                                color = hit.tint.copy(alpha = 0.9f)
                                            )
                                        )
                                    }
                                    playAssetSoundOnce("Calma_EfectosSonido/efecto-burbuja.mp3")
                                    if (viewportWidth > 0f && viewportHeight > 0f) {
                                        spheres.add(spawnSphere())
                                    }
                                }
                            }
                        }
                ) {
                    spheres.forEach { sphere ->
                        val center = Offset(sphere.x, sphere.y)
                        val appearProgress = ((nowMs - sphere.bornAtMs).toFloat() / SPHERE_APPEAR_MS.toFloat())
                            .coerceIn(0f, 1f)
                        val easedAppear = appearProgress * appearProgress * (3f - (2f * appearProgress))
                        val radius = sphere.radius * easedAppear.coerceAtLeast(0.02f)
                        val deformation = deformations[sphere.id]
                        val elapsed = deformation?.let { (nowMs - it.bornAtMs).toFloat() } ?: 0f
                        val deformProgress = (elapsed / DEFORMATION_MS.toFloat()).coerceIn(0f, 1f)
                        val deformEnvelope = kotlin.math.sin(deformProgress * PI.toFloat()).coerceIn(0f, 1f)
                        val deformStrength = (deformation?.strength ?: 0f) * deformEnvelope
                        val scaleMajor = 1f + (deformStrength * 0.65f)
                        val scaleMinor = (1f - (deformStrength * 0.35f)).coerceAtLeast(0.86f)
                        val rotationDeg = ((deformation?.angleRad ?: 0f) * 180f / PI.toFloat())

                        withTransform({
                            if (deformStrength > 0.001f) {
                                rotate(degrees = rotationDeg, pivot = center)
                                scale(scaleX = scaleMajor, scaleY = scaleMinor, pivot = center)
                            }
                        }) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.70f),
                                        sphere.tint,
                                        sphere.tint.copy(alpha = 0.24f)
                                    ),
                                    center = center,
                                    radius = radius * 1.2f
                                ),
                                radius = radius,
                                center = center
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.45f),
                                radius = radius,
                                center = center,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.6f)
                            )
                        }

                        val phraseText = sphere.phrase
                        drawIntoCanvas { canvas ->
                            val native = canvas.nativeCanvas
                            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = android.graphics.Color.argb(225, 255, 255, 255)
                                textAlign = Paint.Align.CENTER
                                textSize = sphere.radius * 0.24f
                                isFakeBoldText = true
                            }

                            val maxWidth = sphere.radius * 1.35f
                            val ellipsis = trimText(phraseText, textPaint, maxWidth)
                            native.drawText(
                                ellipsis,
                                sphere.x,
                                sphere.y + (textPaint.textSize * 0.28f),
                                textPaint
                            )
                        }
                    }

                    if (particles.isNotEmpty()) {
                        val current = nowMs
                        particles.forEach { p ->
                            val alpha = (1f - ((current - p.bornAtMs).toFloat() / p.lifeMs.toFloat()))
                                .coerceIn(0f, 1f)
                            drawCircle(
                                color = p.color.copy(alpha = alpha),
                                radius = p.radius,
                                center = Offset(p.x, p.y)
                            )
                        }
                    }
                }

                phraseReveals.forEach { reveal ->
                    val age = nowMs - reveal.bornAtMs
                    if (age < reveal.revealDelayMs) return@forEach
                    val delayedAge = age - reveal.revealDelayMs
                    val progress = (delayedAge.toFloat() / PHRASE_LIFETIME_MS.toFloat()).coerceIn(0f, 1f)
                    val alpha = when {
                        progress < 0.20f -> progress / 0.20f
                        progress > 0.75f -> (1f - progress) / 0.25f
                        else -> 1f
                    }.coerceIn(0f, 1f)

                    val cardWidth = min(360f, max(220f, viewportWidth * 0.78f))
                    val rawX = reveal.x - cardWidth / 2f
                    val safeX = rawX.coerceIn(16f, max(16f, viewportWidth - cardWidth - 16f))
                    val estimatedCardHeight = 96f
                    val topMargin = 24f
                    val bottomMargin = 28f
                    val preferredY = when {
                        reveal.y < viewportHeight * 0.30f -> reveal.y + 30f
                        reveal.y > viewportHeight * 0.75f -> reveal.y - 126f
                        else -> reveal.y - 96f
                    }
                    val safeY = preferredY.coerceIn(
                        topMargin,
                        max(topMargin, viewportHeight - estimatedCardHeight - bottomMargin)
                    )

                    Card(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    x = safeX.toInt(),
                                    y = safeY.toInt()
                                )
                            }
                            .width(with(LocalDensity.current) { cardWidth.toDp() })
                            .clip(RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.5f * alpha),
                                            Color.Black.copy(alpha = 0.90f * alpha)
                                        )
                                    )
                                )
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White.copy(alpha = 0.03f * alpha))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = reveal.phrase,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White, //Color.White.copy(alpha = alpha),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                ControlsCard(
                    showSettings = showSettings,
                    onToggleSettings = { showSettings = !showSettings },
                    sphereCount = targetSphereCount,
                    onSphereCountChange = {
                        targetSphereCount = it
                        prefs.saveSphereCount(it)
                        ensureSphereTarget()
                    },
                    useFixedBackground = useFixedBackground,
                    onUseFixedBackgroundChange = { enabled ->
                        useFixedBackground = enabled
                        prefs.saveUseFixedBackground(enabled)
                        selectedBackground = resolveSelectedAsset(
                            allAssets = backgroundAssets,
                            useFixed = enabled,
                            fixedAssetName = fixedBackgroundName,
                            random = random
                        )
                    },
                    currentBackground = selectedBackground,
                    showBackgroundMenu = showBackgroundMenu,
                    onBackgroundMenuChange = { showBackgroundMenu = it },
                    backgroundAssets = backgroundAssets,
                    onBackgroundSelect = { bgName ->
                        fixedBackgroundName = bgName
                        prefs.saveFixedBackgroundName(bgName)
                        useFixedBackground = true
                        prefs.saveUseFixedBackground(true)
                        selectedBackground = bgName
                        showBackgroundMenu = false
                    },
                    onRandomBackgroundNow = {
                        val picked = if (backgroundAssets.isEmpty()) null else backgroundAssets.random(random)
                        if (!picked.isNullOrBlank()) {
                            selectedBackground = picked
                            fixedBackgroundName = picked
                            prefs.saveFixedBackgroundName(picked)
                            useFixedBackground = true
                            prefs.saveUseFixedBackground(true)
                        }
                    },
                    useFixedMusic = useFixedMusic,
                    onUseFixedMusicChange = { enabled ->
                        useFixedMusic = enabled
                        prefs.saveUseFixedMusic(enabled)
                        restartMusicForSelection()
                    },
                    currentMusic = selectedMusic,
                    showMusicMenu = showMusicMenu,
                    onMusicMenuChange = { showMusicMenu = it },
                    musicAssets = musicAssets,
                    onMusicSelect = { musicName ->
                        fixedMusicName = musicName
                        prefs.saveFixedMusicName(musicName)
                        useFixedMusic = true
                        prefs.saveUseFixedMusic(true)
                        selectedMusic = musicName
                        showMusicMenu = false
                    },
                    onRandomMusicNow = {
                        val picked = if (musicAssets.isEmpty()) null else musicAssets.random(random)
                        if (!picked.isNullOrBlank()) {
                            selectedMusic = picked
                            fixedMusicName = picked
                            prefs.saveFixedMusicName(picked)
                            useFixedMusic = true
                            prefs.saveUseFixedMusic(true)
                        }
                    }
                )
            }
        }
    }

    @Composable
    private fun ControlsCard(
        showSettings: Boolean,
        onToggleSettings: () -> Unit,
        sphereCount: Int,
        onSphereCountChange: (Int) -> Unit,
        useFixedBackground: Boolean,
        onUseFixedBackgroundChange: (Boolean) -> Unit,
        currentBackground: String?,
        showBackgroundMenu: Boolean,
        onBackgroundMenuChange: (Boolean) -> Unit,
        backgroundAssets: List<String>,
        onBackgroundSelect: (String) -> Unit,
        onRandomBackgroundNow: () -> Unit,
        useFixedMusic: Boolean,
        onUseFixedMusicChange: (Boolean) -> Unit,
        currentMusic: String?,
        showMusicMenu: Boolean,
        onMusicMenuChange: (Boolean) -> Unit,
        musicAssets: List<String>,
        onMusicSelect: (String) -> Unit,
        onRandomMusicNow: () -> Unit
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 14.dp, end = 12.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.31f))
                    .clickable { onToggleSettings() }
                    .size(30.dp)
            )

            AnimatedVisibility(
                visible = showSettings,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 56.dp, end = 10.dp),
                enter = fadeIn(animationSpec = tween(260)) + scaleIn(initialScale = 0.96f, animationSpec = tween(260)),
                exit = fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.97f, animationSpec = tween(180))
                ) {
                Card(
                    modifier = Modifier.widthIn(max = 360.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x33202A33))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Espacio de Calma",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Toca una esfera para liberar su frase.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "Esferas: $sphereCount",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                        Slider(
                            value = sphereCount.toFloat(),
                            onValueChange = { onSphereCountChange(it.toInt()) },
                            valueRange = MIN_SPHERES.toFloat()..MAX_SPHERES.toFloat(),
                            steps = (MAX_SPHERES - MIN_SPHERES - 1)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Fondo fijo", color = Color.White)
                            Switch(checked = useFixedBackground, onCheckedChange = onUseFixedBackgroundChange)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth(0.64f)) {
                                Text(
                                    text = currentBackground ?: "Sin fondo",
                                    color = Color.White,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .clickable { onBackgroundMenuChange(true) }
                                        .padding(horizontal = 10.dp, vertical = 10.dp)
                                )
                                DropdownMenu(
                                    expanded = showBackgroundMenu,
                                    onDismissRequest = { onBackgroundMenuChange(false) },
                                    modifier = Modifier.heightIn(max = 280.dp)
                                ) {
                                    backgroundAssets.forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text(item) },
                                            onClick = { onBackgroundSelect(item) }
                                        )
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .width(118.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .clickable { onRandomBackgroundNow() }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "Aleatorio", color = Color.White)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Música fija", color = Color.White)
                            Switch(checked = useFixedMusic, onCheckedChange = onUseFixedMusicChange)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth(0.64f)) {
                                Text(
                                    text = currentMusic ?: "Sin música",
                                    color = Color.White,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .clickable { onMusicMenuChange(true) }
                                        .padding(horizontal = 10.dp, vertical = 10.dp)
                                )
                                DropdownMenu(
                                    expanded = showMusicMenu,
                                    onDismissRequest = { onMusicMenuChange(false) },
                                    modifier = Modifier
                                        .heightIn(max = 280.dp)
                                        .widthIn(min = 220.dp)
                                ) {
                                    musicAssets.forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text(item) },
                                            onClick = { onMusicSelect(item) }
                                        )
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .width(118.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .clickable { onRandomMusicNow() }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "Aleatoria", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun listAssetsByPattern(folder: String, regex: Regex): List<String> {
        val context = requireContext()
        return context.assets.list(folder)
            .orEmpty()
            .filter { regex.matches(it.lowercase()) }
            .sortedBy { name ->
                regex.find(name.lowercase())?.groupValues?.getOrNull(1)?.toIntOrNull() ?: Int.MAX_VALUE
            }
    }

    private fun resolveSelectedAsset(
        allAssets: List<String>,
        useFixed: Boolean,
        fixedAssetName: String?,
        random: Random
    ): String? {
        if (allAssets.isEmpty()) return null
        if (useFixed) {
            val fixed = fixedAssetName?.takeIf { it in allAssets }
            if (!fixed.isNullOrBlank()) return fixed
        }
        return allAssets.random(random)
    }

    private fun playAssetSoundOnce(assetPath: String) {
        val context = requireContext()
        runCatching {
            val afd = context.assets.openFd(assetPath)
            val player = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                isLooping = false
                setVolume(0.65f, 0.65f)
                setOnCompletionListener {
                    it.release()
                }
                prepare()
                start()
            }
            player
        }
    }

    private fun trimText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        val fallback = text.trim()
        if (fallback.isEmpty()) return ""
        val words = fallback.split(" ")
        if (words.isEmpty()) return fallback.take(8)
        val first = words.firstOrNull().orEmpty()
        if (paint.measureText(first) <= maxWidth) {
            return "$first…"
        }
        var lo = 1
        var hi = min(10, fallback.length)
        var best = fallback.take(1)
        while (lo <= hi) {
            val mid = (lo + hi) / 2
            val candidate = fallback.take(mid) + "…"
            if (paint.measureText(candidate) <= maxWidth) {
                best = candidate
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return best
    }

    private fun loadCalmPhrasesFromAsset(): List<String> {
        val context = requireContext()
        val parsed = runCatching {
            context.assets.open(PHRASES_ASSET_PATH).bufferedReader().use { reader ->
                val raw = reader.readText()
                val array = JSONArray(raw)
                buildList {
                    for (i in 0 until array.length()) {
                        val value = array.optString(i).trim()
                        if (value.isNotEmpty()) add(value)
                    }
                }
            }
        }.getOrDefault(emptyList())
        return parsed.ifEmpty { DEFAULT_CALM_PHRASES }
    }

    companion object {
        private val DEFAULT_CALM_PHRASES = listOf(
            "Respira suave, todo esta bien.",
            "Tu paz interior guia cada paso.",
            "Mente serena, corazon fuerte.",
            "Hoy eliges calma y claridad.",
            "Suelta tension, abraza presencia.",
            "Cada respiracion te renueva.",
            "Estas a salvo en este momento.",
            "La quietud tambien transforma.",
            "Tu enfoque crea equilibrio.",
            "Con calma, todo fluye mejor.",
            "Confia en tu ritmo natural.",
            "Tu energia vuelve a su centro.",
            "Respira, observa, suelta y continua.",
            "Dentro de ti hay serenidad.",
            "Eres mas grande que el ruido.",
            "Lo simple trae paz profunda.",
            "Tu atencion crea bienestar.",
            "Calma por dentro, fuerza por fuera.",
            "Un instante presente lo cambia todo.",
            "La paz que buscas ya esta aqui."
        )

        private const val PHRASES_ASSET_PATH = "Calma_Frases/frases_esferas.json"
        private const val MIN_SPHERES = 3
        private const val MAX_SPHERES = 24
        private const val PHRASE_LIFETIME_MS = 3000L
        private const val PHRASE_REVEAL_DELAY_MS = 450L
        private const val SPHERE_APPEAR_MS = 520L
        private const val DEFORMATION_MS = 360L
        private const val MIN_SPEED = 26f
        private const val WALL_RESTITUTION = 0.985f
        private const val SPHERE_RESTITUTION = 0.978f
    }

    private fun enforceMinimumSpeed(vx: Float, vy: Float): Pair<Float, Float> {
        val speed = sqrt(vx * vx + vy * vy)
        if (speed >= MIN_SPEED) return vx to vy
        val angle = if (speed > 0.001f) atan2(vy, vx) else Random.nextFloat() * (2f * PI.toFloat())
        return cos(angle) * MIN_SPEED to sin(angle) * MIN_SPEED
    }
}
