package com.ypg.neville.feature.calmspace.ui

import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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

enum class CalmParticleMode {
    SPHERE,
    FIREFLY,
    BOTH
}

data class CalmFirefly(
    val id: Int,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val phase: Float,
    val noiseA: Float,
    val noiseB: Float,
    val wanderFactor: Float,
    val glowFactor: Float,
    val blinkRate: Float,
    val blinkPhase: Float,
    val phrase: String,
    val bornAtMs: Long,
    val disturbedUntilMs: Long = 0L
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

    fun fireflyCount(): Int = prefs.getInt(KEY_FIREFLIES, 14).coerceIn(4, 36)

    fun saveFireflyCount(value: Int) {
        prefs.edit().putInt(KEY_FIREFLIES, value.coerceIn(4, 36)).apply()
    }

    fun particleMode(): CalmParticleMode {
        val raw = prefs.getString(KEY_PARTICLE_MODE, CalmParticleMode.BOTH.name).orEmpty()
        return runCatching { CalmParticleMode.valueOf(raw) }.getOrDefault(CalmParticleMode.BOTH)
    }

    fun saveParticleMode(value: CalmParticleMode) {
        prefs.edit().putString(KEY_PARTICLE_MODE, value.name).apply()
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

    fun nominalReleaseSpeed(): Float = prefs.getFloat(KEY_NOMINAL_RELEASE_SPEED, 34f).coerceIn(12f, 120f)

    fun saveNominalReleaseSpeed(value: Float) {
        prefs.edit().putFloat(KEY_NOMINAL_RELEASE_SPEED, value.coerceIn(12f, 120f)).apply()
    }

    fun keepMusicInBackground(): Boolean = prefs.getBoolean(KEY_KEEP_MUSIC_BACKGROUND, false)

    fun saveKeepMusicInBackground(value: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_MUSIC_BACKGROUND, value).apply()
    }

    fun keepScreenOn(): Boolean = prefs.getBoolean(KEY_KEEP_SCREEN_ON, false)

    fun saveKeepScreenOn(value: Boolean) {
        prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, value).apply()
    }

    companion object {
        private const val KEY_SPHERES = "calm_spheres"
        private const val KEY_FIREFLIES = "calm_fireflies"
        private const val KEY_PARTICLE_MODE = "calm_particle_mode"
        private const val KEY_USE_FIXED_BG = "calm_use_fixed_background"
        private const val KEY_FIXED_BG_NAME = "calm_fixed_background_name"
        private const val KEY_USE_FIXED_MUSIC = "calm_use_fixed_music"
        private const val KEY_FIXED_MUSIC_NAME = "calm_fixed_music_name"
        private const val KEY_NOMINAL_RELEASE_SPEED = "calm_nominal_release_speed"
        private const val KEY_KEEP_MUSIC_BACKGROUND = "calm_keep_music_background"
        private const val KEY_KEEP_SCREEN_ON = "calm_keep_screen_on"
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
        applyKeepScreenOn(false)
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
        var targetFireflyCount by remember { mutableIntStateOf(prefs.fireflyCount()) }
        var particleMode by remember { mutableStateOf(prefs.particleMode()) }
        var useFixedBackground by remember { mutableStateOf(prefs.useFixedBackground()) }
        var fixedBackgroundName by remember { mutableStateOf(prefs.fixedBackgroundName()) }
        var useFixedMusic by remember { mutableStateOf(prefs.useFixedMusic()) }
        var fixedMusicName by remember { mutableStateOf(prefs.fixedMusicName()) }
        var nominalReleaseSpeed by remember { mutableStateOf(prefs.nominalReleaseSpeed()) }
        var keepMusicInBackground by remember { mutableStateOf(prefs.keepMusicInBackground()) }
        var keepScreenOn by remember { mutableStateOf(prefs.keepScreenOn()) }
        val phraseBag = remember { mutableStateListOf<String>() }

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
        val fireflies = remember { mutableStateListOf<CalmFirefly>() }
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
        var showParticleModeMenu by remember { mutableStateOf(false) }
        var showSettings by remember { mutableStateOf(false) }
        var draggingSphereId by remember { mutableIntStateOf(-1) }
        var draggingFireflyId by remember { mutableIntStateOf(-1) }
        var lastDragMs by remember { mutableLongStateOf(0L) }
        var lastDragPoint by remember { mutableStateOf<Offset?>(null) }
        var dragVelocityX by remember { mutableStateOf(0f) }
        var dragVelocityY by remember { mutableStateOf(0f) }

        fun spawnSphere(): CalmSphere {
            val radius = random.nextInt(34, 78).toFloat()
            val x = random.nextFloat().coerceIn(0.05f, 0.95f) * max(viewportWidth, radius * 2f)
            val y = random.nextFloat().coerceIn(0.10f, 0.90f) * max(viewportHeight, radius * 2f)
            val speed = (nominalReleaseSpeed * 0.85f) + random.nextFloat() * (nominalReleaseSpeed * 0.30f)
            val angle = random.nextFloat() * (2f * PI.toFloat())
            val phrase = nextNonRepeatingPhrase(calmPhrases, phraseBag, random)
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

        fun spawnFirefly(disturbedUntilMs: Long = 0L): CalmFirefly {
            val x = random.nextFloat().coerceIn(0.05f, 0.95f) * max(viewportWidth, 40f)
            val y = random.nextFloat().coerceIn(0.10f, 0.90f) * max(viewportHeight, 40f)
            val speed = (nominalReleaseSpeed * 0.42f) + random.nextFloat() * 16f
            val angle = random.nextFloat() * (2f * PI.toFloat())
            val phrase = nextNonRepeatingPhrase(calmPhrases, phraseBag, random)
            return CalmFirefly(
                id = nextSphereId++,
                x = x,
                y = y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                phase = random.nextFloat() * (2f * PI.toFloat()),
                noiseA = random.nextFloat() * 0.95f + 0.55f,
                noiseB = random.nextFloat() * 1.30f + 0.70f,
                wanderFactor = random.nextFloat() * 0.55f + 0.78f,
                glowFactor = random.nextFloat() * 0.40f + 0.82f,
                blinkRate = random.nextFloat() * 0.45f + 0.70f,
                blinkPhase = random.nextFloat() * (2f * PI.toFloat()),
                phrase = phrase,
                bornAtMs = System.currentTimeMillis(),
                disturbedUntilMs = disturbedUntilMs
            )
        }

        fun ensureSphereTarget() {
            if (viewportWidth <= 0f || viewportHeight <= 0f) return
            val targetSpheres = desiredSphereTarget(particleMode, targetSphereCount)
            val targetFireflies = desiredFireflyTarget(particleMode, targetFireflyCount)
            while (spheres.size < targetSpheres) {
                spheres.add(spawnSphere())
            }
            while (spheres.size > targetSpheres) {
                spheres.removeLastOrNull()
            }
            while (fireflies.size < targetFireflies) {
                fireflies.add(spawnFirefly())
            }
            while (fireflies.size > targetFireflies) {
                fireflies.removeLastOrNull()
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

        fun pickNextBackgroundNonRepeating() {
            if (backgroundAssets.isEmpty()) return
            val candidates = backgroundAssets.filter { it != selectedBackground }
            val picked = if (candidates.isNotEmpty()) candidates.random(random) else backgroundAssets.first()
            selectedBackground = picked
            fixedBackgroundName = picked
            prefs.saveFixedBackgroundName(picked)
            useFixedBackground = true
            prefs.saveUseFixedBackground(true)
        }

        fun findSphereAt(offset: Offset): CalmSphere? {
            return spheres
                .asReversed()
                .firstOrNull { sphere ->
                    val dx = sphere.x - offset.x
                    val dy = sphere.y - offset.y
                    (dx * dx + dy * dy) <= sphere.radius * sphere.radius
                }
        }

        fun findFireflyAt(offset: Offset): CalmFirefly? {
            return fireflies
                .asReversed()
                .firstOrNull { firefly ->
                    val dx = firefly.x - offset.x
                    val dy = firefly.y - offset.y
                    (dx * dx + dy * dy) <= (FIREFLY_TOUCH_RADIUS * FIREFLY_TOUCH_RADIUS)
                }
        }

        fun updateSpherePosition(id: Int, x: Float, y: Float) {
            val index = spheres.indexOfFirst { it.id == id }
            if (index == -1) return
            val sphere = spheres[index]
            spheres[index] = sphere.copy(
                x = x.coerceIn(sphere.radius, viewportWidth - sphere.radius),
                y = y.coerceIn(sphere.radius, viewportHeight - sphere.radius),
                vx = 0f,
                vy = 0f
            )
        }

        fun updateFireflyPosition(id: Int, x: Float, y: Float) {
            val index = fireflies.indexOfFirst { it.id == id }
            if (index == -1) return
            val item = fireflies[index]
            fireflies[index] = item.copy(
                x = x.coerceIn(FIREFLY_MARGIN, viewportWidth - FIREFLY_MARGIN),
                y = y.coerceIn(FIREFLY_MARGIN, viewportHeight - FIREFLY_MARGIN),
                vx = 0f,
                vy = 0f
            )
        }

        fun applyReleaseVelocity(id: Int, vx: Float, vy: Float) {
            val index = spheres.indexOfFirst { it.id == id }
            if (index == -1) return
            val sphere = spheres[index]
            val mag = sqrt(vx * vx + vy * vy)
            if (mag <= 0.001f) return
            val reducedMag = (mag * DRAG_RELEASE_DAMPING).coerceIn(10f, MAX_DRAG_RELEASE_SPEED)
            val dirX = vx / mag
            val dirY = vy / mag
            spheres[index] = sphere.copy(
                vx = dirX * reducedMag,
                vy = dirY * reducedMag
            )
        }

        fun applyFireflyRelease(id: Int, vx: Float, vy: Float) {
            val index = fireflies.indexOfFirst { it.id == id }
            if (index == -1) return
            val item = fireflies[index]
            val mag = sqrt(vx * vx + vy * vy).coerceAtLeast(0.001f)
            val dirX = vx / mag
            val dirY = vy / mag
            val chaoticBoost = (mag * 0.55f).coerceIn(18f, 72f)
            val erraticAngle = (Random.nextFloat() - 0.5f) * 0.9f
            val angle = atan2(dirY, dirX) + erraticAngle
            fireflies[index] = item.copy(
                vx = cos(angle) * chaoticBoost,
                vy = sin(angle) * chaoticBoost,
                disturbedUntilMs = nowMs + FIREFLY_DISTURBED_MS
            )
        }

        fun retuneAllSphereSpeeds(targetSpeed: Float) {
            val safeTarget = targetSpeed.coerceIn(12f, 120f)
            for (i in spheres.indices) {
                val item = spheres[i]
                if (item.id == draggingSphereId) continue
                val speed = sqrt(item.vx * item.vx + item.vy * item.vy)
                val blendedTarget = speed + ((safeTarget - speed) * 0.55f)
                val angle = if (speed > 0.001f) atan2(item.vy, item.vx) else Random.nextFloat() * (2f * PI.toFloat())
                spheres[i] = item.copy(
                    vx = cos(angle) * blendedTarget,
                    vy = sin(angle) * blendedTarget
                )
            }
        }

        fun retuneAllFireflySpeeds(targetSpeed: Float) {
            val safeTarget = (targetSpeed * 0.42f).coerceIn(10f, 52f)
            for (i in fireflies.indices) {
                val item = fireflies[i]
                if (item.id == draggingFireflyId) continue
                val speed = sqrt(item.vx * item.vx + item.vy * item.vy)
                val blendedTarget = speed + ((safeTarget - speed) * 0.62f)
                val angle = if (speed > 0.001f) atan2(item.vy, item.vx) else Random.nextFloat() * (2f * PI.toFloat())
                fireflies[i] = item.copy(
                    vx = cos(angle) * blendedTarget,
                    vy = sin(angle) * blendedTarget
                )
            }
        }

        val backgroundBitmap = remember(selectedBackground) {
            val assetName = selectedBackground ?: return@remember null
            runCatching {
                context.assets.open("Calma_ImagenesFondo/$assetName").use { input ->
                    BitmapFactory.decodeStream(input)?.asImageBitmap()
                }
            }.getOrNull()
        }

        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(keepScreenOn) {
            applyKeepScreenOn(keepScreenOn)
        }
        DisposableEffect(selectedMusic, keepMusicInBackground, lifecycleOwner) {
            val music = selectedMusic
            if (music.isNullOrBlank()) return@DisposableEffect onDispose { }
            var resumedAfterStop = false

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

            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> {
                        if (!keepMusicInBackground && player?.isPlaying == true) {
                            runCatching { player.pause() }
                            resumedAfterStop = true
                        }
                    }
                    Lifecycle.Event.ON_START -> {
                        if (!keepMusicInBackground && resumedAfterStop) {
                            runCatching { player?.start() }
                            resumedAfterStop = false
                        }
                    }
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
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

            LaunchedEffect(widthPx, heightPx, targetSphereCount, targetFireflyCount, particleMode) {
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
                        if (item.id == draggingSphereId) continue
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

                        val adjusted = enforceMinimumSpeed(nvx, nvy, minInertialSpeedFromNominal(nominalReleaseSpeed))
                        val regulated = regulateSpeedToNominal(
                            adjusted.first,
                            adjusted.second,
                            nominalReleaseSpeed,
                            response = 0.24f,
                            minFactor = 0.76f,
                            maxFactor = 1.22f
                        )
                        spheres[i] = item.copy(x = nx, y = ny, vx = regulated.first, vy = regulated.second)
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
                            if (a.id == draggingSphereId || b.id == draggingSphereId) continue
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

                            val adjustedA = enforceMinimumSpeed(avx, avy, minInertialSpeedFromNominal(nominalReleaseSpeed))
                            val regulatedA = regulateSpeedToNominal(
                                adjustedA.first,
                                adjustedA.second,
                                nominalReleaseSpeed,
                                response = 0.24f,
                                minFactor = 0.76f,
                                maxFactor = 1.22f
                            )
                            spheres[i] = a.copy(
                                x = (a.x - nx * shiftA).coerceIn(a.radius, viewportWidth - a.radius),
                                y = (a.y - ny * shiftA).coerceIn(a.radius, viewportHeight - a.radius),
                                vx = regulatedA.first,
                                vy = regulatedA.second
                            )
                            val adjustedB = enforceMinimumSpeed(bvx, bvy, minInertialSpeedFromNominal(nominalReleaseSpeed))
                            val regulatedB = regulateSpeedToNominal(
                                adjustedB.first,
                                adjustedB.second,
                                nominalReleaseSpeed,
                                response = 0.24f,
                                minFactor = 0.76f,
                                maxFactor = 1.22f
                            )
                            spheres[j] = b.copy(
                                x = (b.x + nx * shiftB).coerceIn(b.radius, viewportWidth - b.radius),
                                y = (b.y + ny * shiftB).coerceIn(b.radius, viewportHeight - b.radius),
                                vx = regulatedB.first,
                                vy = regulatedB.second
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

                    for (i in fireflies.indices) {
                        val item = fireflies[i]
                        if (item.id == draggingFireflyId) continue
                        val isDisturbed = nowMs < item.disturbedUntilMs
                        val timeS = nowMs * 0.001f

                        // Organic wander with unique frequencies per firefly to avoid synchronized motion.
                        val baseAx =
                            sin((timeS * item.noiseA * 1.9f) + item.phase) * (FIREFLY_ORGANIC_ACCEL * item.wanderFactor) +
                            cos((timeS * item.noiseB * 1.1f) + (item.phase * 0.61f)) * (FIREFLY_ORGANIC_ACCEL * 0.62f * item.wanderFactor)
                        val baseAy =
                            cos((timeS * item.noiseA * 1.37f) + (item.phase * 0.83f)) * (FIREFLY_ORGANIC_ACCEL * item.wanderFactor) +
                            sin((timeS * item.noiseB * 1.73f) + (item.phase * 0.29f)) * (FIREFLY_ORGANIC_ACCEL * 0.64f * item.wanderFactor)

                        // Elliptic drift to bias translation toward curved, lively paths.
                        val ellipticAx =
                            sin((timeS * item.noiseA * 0.86f) + item.phase) * (FIREFLY_ELLIPTIC_ACCEL * item.wanderFactor)
                        val ellipticAy =
                            cos((timeS * item.noiseB * 0.72f) + (item.phase * 0.74f)) * (FIREFLY_ELLIPTIC_ACCEL * 1.18f * item.wanderFactor)

                        val speedForCurve = sqrt((item.vx * item.vx) + (item.vy * item.vy)).coerceAtLeast(0.001f)
                        val curveWave = sin((timeS * (item.noiseA + item.noiseB) * 0.58f) + item.phase)
                        val curveAx = (-item.vy / speedForCurve) * FIREFLY_CURVE_STRENGTH * curveWave
                        val curveAy = (item.vx / speedForCurve) * FIREFLY_CURVE_STRENGTH * curveWave

                        val vibration = sin((timeS * (item.noiseB * 5.6f + 5f)) + item.phase)
                        val vibAx = (-item.vy / speedForCurve) * FIREFLY_TRANSLATION_VIBRATION * vibration
                        val vibAy = (item.vx / speedForCurve) * FIREFLY_TRANSLATION_VIBRATION * vibration

                        val disturbanceMultiplier = if (isDisturbed) FIREFLY_DISTURBANCE_ACCEL_FACTOR else 1f
                        var organicAx = (baseAx + ellipticAx + curveAx + vibAx) * disturbanceMultiplier
                        var organicAy = (baseAy + ellipticAy + curveAy + vibAy) * disturbanceMultiplier

                        // Local separation prevents clusters from drifting in one shared direction.
                        var separationX = 0f
                        var separationY = 0f
                        for (j in fireflies.indices) {
                            if (j == i) continue
                            val other = fireflies[j]
                            val dx = item.x - other.x
                            val dy = item.y - other.y
                            val distSq = (dx * dx) + (dy * dy)
                            if (distSq <= 0.0001f) continue
                            if (distSq < (FIREFLY_AVOID_DISTANCE * FIREFLY_AVOID_DISTANCE)) {
                                val dist = sqrt(distSq)
                                val force = ((FIREFLY_AVOID_DISTANCE - dist) / FIREFLY_AVOID_DISTANCE).coerceIn(0f, 1f)
                                val normalizedX = dx / dist
                                val normalizedY = dy / dist
                                separationX += normalizedX * force * FIREFLY_AVOID_FORCE
                                separationY += normalizedY * force * FIREFLY_AVOID_FORCE
                            }
                        }
                        organicAx += separationX
                        organicAy += separationY

                        // Soft edge steering to avoid getting trapped near borders.
                        val leftProximity = ((FIREFLY_EDGE_SOFT_ZONE - item.x) / FIREFLY_EDGE_SOFT_ZONE).coerceIn(0f, 1f)
                        val rightProximity = ((item.x - (viewportWidth - FIREFLY_EDGE_SOFT_ZONE)) / FIREFLY_EDGE_SOFT_ZONE).coerceIn(0f, 1f)
                        val topProximity = ((FIREFLY_EDGE_SOFT_ZONE - item.y) / FIREFLY_EDGE_SOFT_ZONE).coerceIn(0f, 1f)
                        val bottomProximity = ((item.y - (viewportHeight - FIREFLY_EDGE_SOFT_ZONE)) / FIREFLY_EDGE_SOFT_ZONE).coerceIn(0f, 1f)
                        organicAx += (leftProximity - rightProximity) * FIREFLY_EDGE_PUSH
                        organicAy += (topProximity - bottomProximity) * FIREFLY_EDGE_PUSH

                        // Gentle center attraction to keep broad travel distribution.
                        val centerX = viewportWidth * 0.5f
                        val centerY = viewportHeight * 0.5f
                        organicAx += ((centerX - item.x) / viewportWidth) * FIREFLY_CENTER_PULL
                        organicAy += ((centerY - item.y) / viewportHeight) * FIREFLY_CENTER_PULL

                        var nvx = item.vx + organicAx * dt
                        var nvy = item.vy + organicAy * dt

                        val targetSpeed = if (isDisturbed) {
                            (nominalReleaseSpeed * 1.05f).coerceIn(20f, 84f)
                        } else {
                            (nominalReleaseSpeed * FIREFLY_NOMINAL_SPEED_FACTOR).coerceIn(9f, 48f)
                        }
                        val regulated = regulateSpeedToNominal(
                            nvx,
                            nvy,
                            max(targetSpeed, FIREFLY_MIN_TRAVEL_SPEED),
                            response = 0.16f,
                            minFactor = 0.50f,
                            maxFactor = 1.62f
                        )
                        nvx = regulated.first
                        nvy = regulated.second

                        var nx = item.x + nvx * dt
                        var ny = item.y + nvy * dt
                        if (nx < FIREFLY_MARGIN) {
                            nx = FIREFLY_MARGIN
                            nvx = abs(nvx) * 0.92f
                        } else if (nx > viewportWidth - FIREFLY_MARGIN) {
                            nx = viewportWidth - FIREFLY_MARGIN
                            nvx = -abs(nvx) * 0.92f
                        }
                        if (ny < FIREFLY_MARGIN) {
                            ny = FIREFLY_MARGIN
                            nvy = abs(nvy) * 0.92f
                        } else if (ny > viewportHeight - FIREFLY_MARGIN) {
                            ny = viewportHeight - FIREFLY_MARGIN
                            nvy = -abs(nvy) * 0.92f
                        }
                        fireflies[i] = item.copy(x = nx, y = ny, vx = nvx, vy = nvy)
                    }

                    var mergePairIndices: Pair<Int, Int>? = null
                    var mergeDistance = Float.MAX_VALUE
                    loop@ for (i in 0 until fireflies.size) {
                        for (j in i + 1 until fireflies.size) {
                            val a = fireflies[i]
                            val b = fireflies[j]
                            if (a.id == draggingFireflyId || b.id == draggingFireflyId) continue
                            val dx = b.x - a.x
                            val dy = b.y - a.y
                            val distSq = (dx * dx) + (dy * dy)
                            if (distSq <= (FIREFLY_MERGE_DISTANCE * FIREFLY_MERGE_DISTANCE)) {
                                mergePairIndices = i to j
                                mergeDistance = sqrt(distSq)
                                break@loop
                            }
                        }
                    }

                    if (mergePairIndices != null) {
                        val (ia, ib) = mergePairIndices
                        val a = fireflies.getOrNull(ia)
                        val b = fireflies.getOrNull(ib)
                        if (a == null || b == null) {
                            // no-op
                        } else {
                        val midX = (a.x + b.x) * 0.5f
                        val midY = (a.y + b.y) * 0.5f

                            val finalizeDistance = FIREFLY_MERGE_DISTANCE * FIREFLY_MERGE_FINALIZE_FACTOR
                            if (mergeDistance > finalizeDistance) {
                                // First stage: smoothly pull both fireflies together before merging.
                                val pull = (1f - FIREFLY_FUSION_SMOOTHING).coerceIn(0.04f, 0.40f)
                                fireflies[ia] = a.copy(
                                    x = a.x + ((midX - a.x) * pull),
                                    y = a.y + ((midY - a.y) * pull),
                                    vx = a.vx * (1f - (pull * 0.20f)),
                                    vy = a.vy * (1f - (pull * 0.20f))
                                )
                                fireflies[ib] = b.copy(
                                    x = b.x + ((midX - b.x) * pull),
                                    y = b.y + ((midY - b.y) * pull),
                                    vx = b.vx * (1f - (pull * 0.20f)),
                                    vy = b.vy * (1f - (pull * 0.20f))
                                )
                            } else {
                                val merged = CalmFirefly(
                                    id = nextSphereId++,
                                    x = midX,
                                    y = midY,
                                    vx = (a.vx + b.vx) * (0.35f + (FIREFLY_FUSION_SMOOTHING * 0.28f)),
                                    vy = (a.vy + b.vy) * (0.35f + (FIREFLY_FUSION_SMOOTHING * 0.28f)),
                                    phase = (a.phase + b.phase) * 0.5f,
                                    noiseA = (a.noiseA + b.noiseA) * 0.5f,
                                    noiseB = (a.noiseB + b.noiseB) * 0.5f,
                                    wanderFactor = ((a.wanderFactor + b.wanderFactor) * 0.5f).coerceIn(0.65f, 1.42f),
                                    glowFactor = ((a.glowFactor + b.glowFactor) * 0.5f).coerceIn(0.72f, 1.35f),
                                    blinkRate = ((a.blinkRate + b.blinkRate) * 0.5f).coerceIn(0.65f, 1.25f),
                                    blinkPhase = (a.blinkPhase + b.blinkPhase) * 0.5f,
                                    phrase = nextNonRepeatingPhrase(calmPhrases, phraseBag, random),
                                    bornAtMs = nowMs
                                )
                                fireflies.removeAll { it.id == a.id || it.id == b.id }
                                fireflies.add(merged)
                                if (desiredFireflyTarget(particleMode, targetFireflyCount) > 0) {
                                    fireflies.add(spawnFirefly())
                                }
                                repeat(8) {
                                    val angle = Random.nextFloat() * (2f * PI.toFloat())
                                    val speed = Random.nextFloat() * 78f + 20f
                                    particles.add(
                                        BurstParticle(
                                            id = nextFxId++,
                                            x = midX,
                                            y = midY,
                                            vx = cos(angle) * speed,
                                            vy = sin(angle) * speed,
                                            radius = Random.nextFloat() * 3.5f + 1.2f,
                                            lifeMs = Random.nextLong(250L, 500L),
                                            bornAtMs = nowMs,
                                            color = Color(0xFFFFF6A8).copy(alpha = (0.72f + (FIREFLY_GLOW_INTENSITY * 0.20f)).coerceIn(0.72f, 0.98f))
                                        )
                                    )
                                }
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
                Crossfade(
                    targetState = backgroundBitmap,
                    animationSpec = tween(durationMillis = 1200),
                    label = "calm_background_crossfade"
                ) { currentBitmap ->
                    if (currentBitmap != null) {
                        Image(
                            bitmap = currentBitmap,
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
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.16f))
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(spheres.size, fireflies.size, viewportWidth, viewportHeight) {
                            detectTapGestures(
                                onDoubleTap = { tapOffset ->
                                    if (findSphereAt(tapOffset) == null && findFireflyAt(tapOffset) == null) {
                                        pickNextBackgroundNonRepeating()
                                    }
                                },
                                onTap = { offset ->
                                    val hit = findSphereAt(offset)
                                    val fireHit = if (hit == null) findFireflyAt(offset) else null
                                    if (hit != null || fireHit != null) {
                                        val phrase = hit?.phrase ?: fireHit?.phrase.orEmpty()
                                        val px = hit?.x ?: fireHit?.x ?: offset.x
                                        val py = hit?.y ?: fireHit?.y ?: offset.y
                                        hit?.let { spheres.remove(it) }
                                        fireHit?.let { fireflies.remove(it) }
                                        phraseReveals.add(
                                            PhraseReveal(
                                                id = nextFxId++,
                                                phrase = phrase,
                                                x = px,
                                                y = py,
                                                bornAtMs = System.currentTimeMillis(),
                                                revealDelayMs = PHRASE_REVEAL_DELAY_MS
                                            )
                                        )
                                        val burstCount = if (hit != null) 18 else 12
                                        repeat(burstCount) {
                                            val angle = Random.nextFloat() * (2f * PI.toFloat())
                                            val speed = if (hit != null) {
                                                Random.nextFloat() * 220f + 70f
                                            } else {
                                                Random.nextFloat() * 120f + 30f
                                            }
                                            particles.add(
                                                BurstParticle(
                                                    id = nextFxId++,
                                                    x = px,
                                                    y = py,
                                                    vx = cos(angle) * speed,
                                                    vy = sin(angle) * speed,
                                                    radius = if (hit != null) Random.nextFloat() * 7f + 2f else Random.nextFloat() * 4f + 1.2f,
                                                    lifeMs = if (hit != null) Random.nextLong(500L, 1100L) else Random.nextLong(350L, 700L),
                                                    bornAtMs = System.currentTimeMillis(),
                                                    color = (hit?.tint ?: Color(0xFFFFF5A6)).copy(alpha = 0.9f)
                                                )
                                            )
                                        }
                                        playAssetSoundOnce("Calma_EfectosSonido/efecto-burbuja.mp3")
                                        if (viewportWidth > 0f && viewportHeight > 0f) {
                                            if (hit != null && spheres.size < desiredSphereTarget(particleMode, targetSphereCount)) {
                                                spheres.add(spawnSphere())
                                            }
                                            if (fireHit != null && fireflies.size < desiredFireflyTarget(particleMode, targetFireflyCount)) {
                                                fireflies.add(spawnFirefly())
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        .pointerInput(spheres.size, fireflies.size, viewportWidth, viewportHeight) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val hit = findSphereAt(offset)
                                    val fireHit = if (hit == null) findFireflyAt(offset) else null
                                    if (hit != null || fireHit != null) {
                                        draggingSphereId = hit?.id ?: -1
                                        draggingFireflyId = fireHit?.id ?: -1
                                        lastDragPoint = offset
                                        lastDragMs = System.currentTimeMillis()
                                        dragVelocityX = 0f
                                        dragVelocityY = 0f
                                    }
                                },
                                onDragEnd = {
                                    val id = draggingSphereId
                                    if (id != -1 && draggingFireflyId == -1) {
                                        applyReleaseVelocity(id, dragVelocityX, dragVelocityY)
                                    }
                                    val fireId = draggingFireflyId
                                    if (fireId != -1) {
                                        applyFireflyRelease(fireId, dragVelocityX, dragVelocityY)
                                    }
                                    draggingSphereId = -1
                                    draggingFireflyId = -1
                                    lastDragPoint = null
                                    dragVelocityX = 0f
                                    dragVelocityY = 0f
                                },
                                onDragCancel = {
                                    val id = draggingSphereId
                                    if (id != -1 && draggingFireflyId == -1) {
                                        applyReleaseVelocity(id, dragVelocityX, dragVelocityY)
                                    }
                                    val fireId = draggingFireflyId
                                    if (fireId != -1) {
                                        applyFireflyRelease(fireId, dragVelocityX, dragVelocityY)
                                    }
                                    draggingSphereId = -1
                                    draggingFireflyId = -1
                                    lastDragPoint = null
                                    dragVelocityX = 0f
                                    dragVelocityY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    val id = draggingSphereId
                                    val fireId = draggingFireflyId
                                    if (id == -1 && fireId == -1) return@detectDragGestures
                                    change.consume()
                                    if (id != -1) {
                                        val sphere = spheres.firstOrNull { it.id == id } ?: return@detectDragGestures
                                        val newX = sphere.x + dragAmount.x
                                        val newY = sphere.y + dragAmount.y
                                        updateSpherePosition(id, newX, newY)
                                    } else if (fireId != -1) {
                                        val fly = fireflies.firstOrNull { it.id == fireId } ?: return@detectDragGestures
                                        val newX = fly.x + dragAmount.x
                                        val newY = fly.y + dragAmount.y
                                        updateFireflyPosition(fireId, newX, newY)
                                    }

                                    val now = System.currentTimeMillis()
                                    val previousPoint = lastDragPoint
                                    val dtMs = (now - lastDragMs).coerceAtLeast(1L)
                                    if (previousPoint != null) {
                                        dragVelocityX = ((change.position.x - previousPoint.x) / dtMs.toFloat()) * 1000f
                                        dragVelocityY = ((change.position.y - previousPoint.y) / dtMs.toFloat()) * 1000f
                                    }
                                    lastDragPoint = change.position
                                    lastDragMs = now
                                }
                            )
                        }
                ) {
                    fireflies.forEach { firefly ->
                        val center = Offset(firefly.x, firefly.y)
                        val appearProgress = ((nowMs - firefly.bornAtMs).toFloat() / FIREFLY_APPEAR_MS.toFloat())
                            .coerceIn(0f, 1f)
                        val easedAppear = appearProgress * appearProgress * (3f - (2f * appearProgress))
                        val t = nowMs * 0.001f
                        val slowPulse = ((sin((t * firefly.blinkRate * 1.34f) + firefly.blinkPhase) + 1f) * 0.5f)
                        val supportPulse = ((sin((t * (firefly.blinkRate * 0.72f + 0.24f)) + (firefly.phase * 0.63f)) + 1f) * 0.5f)
                        val blended = (slowPulse * 0.90f) + (supportPulse * 0.10f)
                        // Gate creates a true smooth on/off breathing instead of nearly constant glow.
                        val gated = ((blended - 0.18f) / 0.82f).coerceIn(0f, 1f)
                        val glowPulse = gated * gated * (3f - (2f * gated))
                        val sizeScale = FIREFLY_SIZE_SCALE.coerceIn(0.6f, 2.6f)
                        val coreRadius = ((1.2f + (glowPulse * 2.6f)) * sizeScale) * easedAppear.coerceAtLeast(0.08f)
                        val haloRadius = ((8f + (glowPulse * 18f * FIREFLY_GLOW_INTENSITY * firefly.glowFactor)) * sizeScale) * easedAppear.coerceAtLeast(0.08f)
                        val haloAlpha = (0.02f + (0.28f * glowPulse)) * FIREFLY_GLOW_INTENSITY * firefly.glowFactor

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFF9D4).copy(alpha = (0.10f + (0.74f * glowPulse * firefly.glowFactor)).coerceIn(0.08f, 0.90f)),
                                    Color(0xFFFFEFA6).copy(alpha = haloAlpha.coerceIn(0.01f, 0.56f)),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = haloRadius
                            ),
                            radius = haloRadius,
                            center = center
                        )
                        drawCircle(
                            color = Color(0xFFFFF7C8).copy(alpha = (0.06f + (0.88f * glowPulse * firefly.glowFactor)).coerceIn(0.05f, 0.94f)),
                            radius = coreRadius,
                            center = center
                        )
                    }

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
                    particleMode = particleMode,
                    onParticleModeChange = {
                        particleMode = it
                        prefs.saveParticleMode(it)
                        ensureSphereTarget()
                    },
                    showParticleModeMenu = showParticleModeMenu,
                    onParticleModeMenuChange = { showParticleModeMenu = it },
                    sphereCount = targetSphereCount,
                    onSphereCountChange = {
                        targetSphereCount = it
                        prefs.saveSphereCount(it)
                        ensureSphereTarget()
                    },
                    fireflyCount = targetFireflyCount,
                    onFireflyCountChange = {
                        targetFireflyCount = it
                        prefs.saveFireflyCount(it)
                        ensureSphereTarget()
                    },
                    nominalReleaseSpeed = nominalReleaseSpeed,
                    onNominalReleaseSpeedChange = {
                        nominalReleaseSpeed = it
                        prefs.saveNominalReleaseSpeed(it)
                        retuneAllSphereSpeeds(it)
                        retuneAllFireflySpeeds(it)
                    },
                    keepMusicInBackground = keepMusicInBackground,
                    onKeepMusicInBackgroundChange = {
                        keepMusicInBackground = it
                        prefs.saveKeepMusicInBackground(it)
                    },
                    keepScreenOn = keepScreenOn,
                    onKeepScreenOnChange = {
                        keepScreenOn = it
                        prefs.saveKeepScreenOn(it)
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
        particleMode: CalmParticleMode,
        onParticleModeChange: (CalmParticleMode) -> Unit,
        showParticleModeMenu: Boolean,
        onParticleModeMenuChange: (Boolean) -> Unit,
        sphereCount: Int,
        onSphereCountChange: (Int) -> Unit,
        fireflyCount: Int,
        onFireflyCountChange: (Int) -> Unit,
        nominalReleaseSpeed: Float,
        onNominalReleaseSpeedChange: (Float) -> Unit,
        keepMusicInBackground: Boolean,
        onKeepMusicInBackgroundChange: (Boolean) -> Unit,
        keepScreenOn: Boolean,
        onKeepScreenOnChange: (Boolean) -> Unit,
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
                            .background(Color(0x77202A33))
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
                            text = "Partículas",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = when (particleMode) {
                                    CalmParticleMode.SPHERE -> "Esferas"
                                    CalmParticleMode.FIREFLY -> "Luciérnagas"
                                    CalmParticleMode.BOTH -> "Esferas + Luciérnagas"
                                },
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .clickable { onParticleModeMenuChange(true) }
                                    .padding(horizontal = 10.dp, vertical = 10.dp)
                            )
                            DropdownMenu(
                                expanded = showParticleModeMenu,
                                onDismissRequest = { onParticleModeMenuChange(false) }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Esferas") },
                                    onClick = {
                                        onParticleModeChange(CalmParticleMode.SPHERE)
                                        onParticleModeMenuChange(false)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Luciérnagas") },
                                    onClick = {
                                        onParticleModeChange(CalmParticleMode.FIREFLY)
                                        onParticleModeMenuChange(false)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Esferas + Luciérnagas") },
                                    onClick = {
                                        onParticleModeChange(CalmParticleMode.BOTH)
                                        onParticleModeMenuChange(false)
                                    }
                                )
                            }
                        }
                        if (particleMode == CalmParticleMode.SPHERE || particleMode == CalmParticleMode.BOTH) {
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
                        }
                        if (particleMode == CalmParticleMode.FIREFLY || particleMode == CalmParticleMode.BOTH) {
                            Text(
                                text = "Luciérnagas: $fireflyCount",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                            Slider(
                                value = fireflyCount.toFloat(),
                                onValueChange = { onFireflyCountChange(it.toInt()) },
                                valueRange = MIN_FIREFLIES.toFloat()..MAX_FIREFLIES.toFloat(),
                                steps = (MAX_FIREFLIES - MIN_FIREFLIES - 1)
                            )
                        }
                        Text(
                            text = "Velocidad inercial nominal: ${nominalReleaseSpeed.toInt()}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                        Slider(
                            value = nominalReleaseSpeed,
                            onValueChange = { onNominalReleaseSpeedChange(it) },
                            valueRange = 12f..120f
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Música en segundo plano", color = Color.White)
                            Switch(
                                checked = keepMusicInBackground,
                                onCheckedChange = onKeepMusicInBackgroundChange
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Mantener pantalla encendida", color = Color.White)
                            Switch(
                                checked = keepScreenOn,
                                onCheckedChange = onKeepScreenOnChange
                            )
                        }
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
//Ajustes del desarrollador: Yorjandis PG
        private const val PHRASES_ASSET_PATH = "Calma_Frases/frases_esferas.json"
        private const val MIN_SPHERES = 3
        private const val MAX_SPHERES = 24
        private const val MIN_FIREFLIES = 4
        private const val MAX_FIREFLIES = 36
        private const val PHRASE_LIFETIME_MS = 3000L
        private const val PHRASE_REVEAL_DELAY_MS = 450L
        private const val SPHERE_APPEAR_MS = 520L
        private const val FIREFLY_APPEAR_MS = 420L
        private const val DEFORMATION_MS = 360L
        private const val WALL_RESTITUTION = 0.985f
        private const val SPHERE_RESTITUTION = 0.978f
        private const val DRAG_RELEASE_DAMPING = 0.42f
        private const val MAX_DRAG_RELEASE_SPEED = 140f
        // Radio táctil efectivo para detectar toque/arrastre de luciérnagas.
        private const val FIREFLY_TOUCH_RADIUS = 42f
        // Margen mínimo respecto a bordes para posicionamiento de luciérnagas.
        private const val FIREFLY_MARGIN = 12f
        // Distancia a partir de la cual dos luciérnagas entran en estado de fusión.
        private const val FIREFLY_MERGE_DISTANCE = 40f
        // Factor del umbral final de fusión (más bajo = deben acercarse más antes de unirse).
        private const val FIREFLY_MERGE_FINALIZE_FACTOR = 0.52f
        // Suavidad del acercamiento previo a la fusión (más alto = transición más suave/lenta).
        private const val FIREFLY_FUSION_SMOOTHING = 0.98f
        // Intensidad global del brillo de halo/núcleo de luciérnagas.
        private const val FIREFLY_GLOW_INTENSITY = 0.8f
        // Escala relativa del tamaño visual de luciérnagas (núcleo + halo).
        private const val FIREFLY_SIZE_SCALE = 1.50f
        // Aceleración base del movimiento orgánico (wander) de luciérnagas.
        private const val FIREFLY_ORGANIC_ACCEL = 24f
        // Aceleración de deriva elíptica para trayectorias más curvadas y amplias.
        private const val FIREFLY_ELLIPTIC_ACCEL = 11f
        // Fuerza de curvatura aplicada perpendicular a la velocidad de traslación.
        private const val FIREFLY_CURVE_STRENGTH = 14f
        // Vibración transversal suave durante el desplazamiento para efecto vital/orgánico.
        private const val FIREFLY_TRANSLATION_VIBRATION = 4.6f
        // Multiplicador de aceleración durante estado "molestada" tras soltar.
        private const val FIREFLY_DISTURBANCE_ACCEL_FACTOR = 3.0f
        // Proporción de la velocidad nominal aplicada al crucero normal de luciérnagas.
        private const val FIREFLY_NOMINAL_SPEED_FACTOR = 0.38f
        // Velocidad mínima de traslación para evitar que queden lentas/atrapadas.
        private const val FIREFLY_MIN_TRAVEL_SPEED = 16f
        // Franja cercana al borde donde se activa empuje suave hacia el interior.
        private const val FIREFLY_EDGE_SOFT_ZONE = 86f
        // Intensidad del empuje interno al acercarse a bordes.
        private const val FIREFLY_EDGE_PUSH = 92f
        // Atracción suave al centro para mejorar distribución espacial general.
        private const val FIREFLY_CENTER_PULL = 8f
        // Distancia de separación entre luciérnagas para evitar que se agrupen en bloque.
        private const val FIREFLY_AVOID_DISTANCE = 56f
        // Fuerza de separación aplicada dentro de la zona de evitación.
        private const val FIREFLY_AVOID_FORCE = 20f
        // Duración (ms) del comportamiento errático tras soltar una luciérnaga.
        private const val FIREFLY_DISTURBED_MS = 1800L
    }

    private fun applyKeepScreenOn(enabled: Boolean) {
        val window = activity?.window ?: return
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun nextNonRepeatingPhrase(
        allPhrases: List<String>,
        bag: MutableList<String>,
        random: Random
    ): String {
        if (allPhrases.isEmpty()) return DEFAULT_CALM_PHRASES.first()
        if (bag.isEmpty()) {
            val shuffled = allPhrases.shuffled(random).toMutableList()
            bag.addAll(shuffled)
        }
        return bag.removeAt(0)
    }

    private fun minInertialSpeedFromNominal(nominal: Float): Float {
        return (nominal * 0.42f).coerceIn(8f, 42f)
    }

    private fun regulateSpeedToNominal(
        vx: Float,
        vy: Float,
        nominal: Float,
        response: Float = 0.20f,
        minFactor: Float = 0.72f,
        maxFactor: Float = 1.30f
    ): Pair<Float, Float> {
        val current = sqrt(vx * vx + vy * vy)
        if (current <= 0.001f) return vx to vy
        val target = nominal.coerceIn(12f, 120f)
        val clamped = current.coerceIn(target * minFactor, target * maxFactor)
        val smoothed = clamped + ((target - clamped) * response.coerceIn(0.04f, 0.7f))
        val angle = atan2(vy, vx)
        return cos(angle) * smoothed to sin(angle) * smoothed
    }

    private fun desiredSphereTarget(mode: CalmParticleMode, count: Int): Int {
        return when (mode) {
            CalmParticleMode.SPHERE, CalmParticleMode.BOTH -> count
            CalmParticleMode.FIREFLY -> 0
        }
    }

    private fun desiredFireflyTarget(mode: CalmParticleMode, count: Int): Int {
        return when (mode) {
            CalmParticleMode.FIREFLY, CalmParticleMode.BOTH -> count
            CalmParticleMode.SPHERE -> 0
        }
    }

    private fun enforceMinimumSpeed(vx: Float, vy: Float, minSpeed: Float): Pair<Float, Float> {
        val speed = sqrt(vx * vx + vy * vy)
        if (speed >= minSpeed) return vx to vy
        val angle = if (speed > 0.001f) atan2(vy, vx) else Random.nextFloat() * (2f * PI.toFloat())
        return cos(angle) * minSpeed to sin(angle) * minSpeed
    }
}
