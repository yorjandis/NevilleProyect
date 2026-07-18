package com.ypg.neville

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.edit
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.ypg.neville.model.preferences.DbPreferences
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.model.utils.QRManager
import com.ypg.neville.model.utils.NewsContent
import com.ypg.neville.model.utils.UiModalWindows
import com.ypg.neville.model.utils.Utils
import com.ypg.neville.model.utils.myListener_In_App_Update
import com.ypg.neville.feature.morningdialog.notifications.MorningDialogStartup
import com.ypg.neville.feature.morningdialog.ui.FragMorningDialog
import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryBootstrap
import com.ypg.neville.model.reminders.JournalDailyReminderManager
import com.ypg.neville.model.subscription.SubscriptionManager
import com.ypg.neville.ui.frag.HomeFloatingMenuBottomSheet
import com.ypg.neville.ui.frag.NevilleBottomNavBar
import com.ypg.neville.ui.frag.SheetNavHostBottomSheet
import com.ypg.neville.ui.frag.SubscriptionPaywallDialog
import com.ypg.neville.ui.frag.buildNevilleNavGraph
import com.ypg.neville.ui.frag.FragHome
import com.ypg.neville.ui.frag.frag_listado
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {

    private val toolbarColor = mutableStateOf<Int?>(null)
    private val bottomActive = mutableStateOf<String?>("home")

    private val toolbarAddNoteVisible = mutableStateOf(View.VISIBLE)
    private val toolbarAddFraseVisible = mutableStateOf(View.VISIBLE)
    private val toolbarFavVisible = mutableStateOf(View.GONE)
    private val toolbarFavColor = mutableStateOf(android.graphics.Color.BLACK)
    private val bottomNavVisible = mutableStateOf(true)
    private val homeAlternativeEnabled = mutableStateOf(false)

    lateinit var navController: NavController
    private lateinit var fragContainer: FragmentContainerView

    val icToolsBarNotaAdd = ToolbarIconProxy(toolbarAddNoteVisible, mutableStateOf(android.graphics.Color.BLACK))
    val icToolsBarFraseAdd = ToolbarIconProxy(toolbarAddFraseVisible, mutableStateOf(android.graphics.Color.BLACK))
    val icToolsBarFav = ToolbarIconProxy(toolbarFavVisible, toolbarFavColor)

    private val utils by lazy { Utils(this) }
    private val installMarkerPrefKey = "install_marker_first_install_time"

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = DbPreferences.default(this)
        val isDarkTheme = prefs.getBoolean("tema", true)
        homeAlternativeEnabled.value = prefs.getBoolean(FragHome.PREF_KEY_HOME_ALTERNATIVE_ENABLED, false)
        FragHome.homeAlternativeEnabledState.value = homeAlternativeEnabled.value
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkTheme) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        setTheme(R.style.Theme_NevilleProyect)

        super.onCreate(savedInstanceState)
        setCurrentInstance(this)
        SubscriptionManager.initialize(this)
        JournalDailyReminderManager.initialize(this)
        MorningDialogStartup.sync(this)
        WeeklySummaryBootstrap.initialize(this)

        toolbarColor.value = prefs.getInt("color_marcos", 0).takeIf { it != 0 }

        setContentView(
            ComposeView(this).apply {
                setContent {
                    com.ypg.neville.ui.theme.NevilleTheme {
                        MainScreen()
                    }
                }
            }
        )
        enableImmersiveMode()
        if (savedInstanceState == null) {
            window.decorView.post { handleIncomingShareIntent(intent) }
        }
        window.decorView.post { handleNotificationNavigationIntent(intent) }

        val inAppUpdate = myListener_In_App_Update(this)
        inAppUpdate.setMylistener(object : myListener_In_App_Update.In_mylistener {
            override fun onUpdateAvailable(pUpdateAvailable: Boolean) {
                if (pUpdateAvailable) {
                    val intentNotification = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        utils.show_Notification(getString(R.string.main_update_available), intentNotification)
                    } else {
                        Toast.makeText(this@MainActivity, getString(R.string.main_update_available), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })

        val hadLegacyDatabase = utilsDB.hasLegacyDatabase(this)
        if (hadLegacyDatabase) {
            Toast.makeText(this, getString(R.string.main_migrating_data), Toast.LENGTH_LONG).show()
        }

        if (utilsDB.RestoreDBInfo(this)) {
            if (hadLegacyDatabase) {
                Toast.makeText(this, getString(R.string.main_migration_completed), Toast.LENGTH_SHORT).show()
            }
            recreate()
        } else {
            if (hadLegacyDatabase) {
                Toast.makeText(this, getString(R.string.main_migration_completed), Toast.LENGTH_SHORT).show()
            }
            val currentInstallMarker = currentInstallFirstTime()
            val storedInstallMarker = prefs.getLong(installMarkerPrefKey, -1L)
            val isFirstLaunchForCurrentInstall = currentInstallMarker > 0L && storedInstallMarker != currentInstallMarker
            val shouldShowNews = prefs.getBoolean("Is_primeraVez", true) || isFirstLaunchForCurrentInstall

            if (shouldShowNews) {
                UiModalWindows.showAyudaContectual(
                    this,
                    getString(R.string.settings_whats_new),
                    getString(R.string.main_whats_new_subtitle),
                    NewsContent.buildNewsText(this),
                    false,
                    AppCompatResources.getDrawable(this, R.drawable.neville)
                )
                prefs.edit {
                    putBoolean("Is_primeraVez", false)
                    if (currentInstallMarker > 0L) {
                        putLong(installMarkerPrefKey, currentInstallMarker)
                    }
                }
            }
        }

    }

    private fun currentInstallFirstTime(): Long {
        return runCatching {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            info.firstInstallTime
        }.getOrDefault(-1L)
    }

    override fun onResume() {
        super.onResume()
        SubscriptionManager.refreshStatus()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingShareIntent(intent)
        handleNotificationNavigationIntent(intent)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveMode()
        }
    }

    private fun enableImmersiveMode() {
        // Evita re-layouts verticales al ocultar/mostrar barras del sistema.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    @Composable
    private fun MainScreen() {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidNavHostContainer()
            if (bottomNavVisible.value) {
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    BottomNav()
                }
            }
        }
    }

    fun setBottomNavVisible(visible: Boolean) {
        bottomNavVisible.value = visible
    }

    @Composable
    private fun AndroidNavHostContainer() {
        androidx.compose.ui.viewinterop.AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                FragmentContainerView(context).apply {
                    id = R.id.frag_container
                    fragContainer = this
                    post { ensureNavHostAttached(this) }
                }
            },
            update = {
                ensureNavHostAttached(it)
            }
        )
    }

    private fun ensureNavHostAttached(container: FragmentContainerView) {
        val byId = supportFragmentManager.findFragmentById(container.id) as? NavHostFragment
        val byTag = supportFragmentManager.findFragmentByTag(MAIN_NAV_HOST_TAG) as? NavHostFragment
        val existing = byId ?: byTag

        if (existing != null) {
            val isAttachedToThisContainer = existing.view?.parent === container
            if (isAttachedToThisContainer) {
                navController = existing.navController
                return
            }
        }

        if (!container.isAttachedToWindow) return

        val navHost = NavHostFragment()
        supportFragmentManager.commitNow {
            setReorderingAllowed(true)
            existing?.let { remove(it) }
            replace(container.id, navHost, MAIN_NAV_HOST_TAG)
        }
        navController = navHost.navController
        val graph = buildNevilleNavGraph(navController, R.id.frag_home)
        navController.setGraph(graph, Bundle())
    }

    @Composable
    private fun BottomNav() {
        val tintColor = toolbarColor.value?.let { Color(it) }
        NevilleBottomNavBar(
            activeId = bottomActive.value,
            tintColor = tintColor,
            onConf = {
                bottomActive.value = "conf"
                frag_listado.elementLoaded = "autores/neville/conf"
                openDestinationAsSheet(
                    R.id.frag_listado,
                    Bundle().apply {
                        putBoolean(frag_listado.ARG_RETURN_HOME_ON_BACK, true)
                    }
                )
            },
            onNotas = {
                bottomActive.value = "notas"
                openDestinationAsSheet(R.id.frag_notas)
            },
            onHome = {
                bottomActive.value = "home"
                if (supportFragmentManager.findFragmentByTag(HomeFloatingMenuBottomSheet.TAG) == null) {
                    HomeFloatingMenuBottomSheet().show(supportFragmentManager, HomeFloatingMenuBottomSheet.TAG)
                }
            },
            onDiario = {
                bottomActive.value = "diario"
                openDestinationAsSheet(R.id.frag_diario)
            },
            onLienzo = {
                bottomActive.value = "lienzo"
                openDestinationAsSheet(R.id.frag_lienzo)
            },
            onMetas = {
                bottomActive.value = "metas"
                openDestinationAsSheet(R.id.frag_metas)
            },
            onRecordatorios = {
                bottomActive.value = "recordatorios"
                openDestinationAsSheet(R.id.frag_reminders)
            },
            onAgenda = {
                bottomActive.value = "agenda"
                openDestinationAsSheet(R.id.frag_agenda)
            },
            onRitual = {
                bottomActive.value = "morning_dialog"
                openDestinationAsSheet(R.id.frag_morning_dialog)
            },
            onResumenSemanal = {
                bottomActive.value = "weekly_summary"
                openDestinationAsSheet(R.id.frag_weekly_summary)
            },
            onVoces = {
                bottomActive.value = "voces"
                openDestinationAsSheet(R.id.frag_voice_recordings)
            },
            onAnclas = {
                bottomActive.value = "anclas"
                openDestinationAsSheet(R.id.frag_emotional_anchors)
            },
            onCalma = {
                bottomActive.value = "calma"
                openDestinationAsSheet(R.id.frag_calm_space)
            },
            onCardio = {
                bottomActive.value = "cardio"
                openDestinationAsSheet(R.id.frag_cardio_coherence)
            },
            onPresence = {
                bottomActive.value = "presence"
                openDestinationAsSheet(R.id.frag_presence)
            }
        )
    }

    fun toggleHomeAlternativeMode(): Boolean {
        val enabled = !homeAlternativeEnabled.value
        homeAlternativeEnabled.value = enabled
        FragHome.homeAlternativeEnabledState.value = enabled
        DbPreferences.default(this).edit { putBoolean(FragHome.PREF_KEY_HOME_ALTERNATIVE_ENABLED, enabled) }
        bottomActive.value = "home"
        return enabled
    }

    fun openDestinationAsSheet(destinationId: Int, startArgs: Bundle? = null) {
        if (destinationId == R.id.frag_home) return
        if (destinationId == R.id.frag_notas && shouldRequireNotesBiometricLock()) {
            if (!SubscriptionManager.hasActiveSubscriptionNow()) {
                showSubscriptionPaywall(getString(R.string.paywall_reason_notes_biometric))
                return
            }
            showNotesBiometricPrompt {
                openDestinationAsSheetInternal(destinationId, startArgs)
            }
            return
        }
        if (
            (
                destinationId == R.id.frag_metas ||
                    destinationId == R.id.frag_lienzo ||
                    destinationId == R.id.frag_reminders ||
                    destinationId == R.id.frag_agenda ||
                    destinationId == R.id.frag_morning_dialog ||
                    destinationId == R.id.frag_my_day ||
                    destinationId == R.id.frag_weekly_summary ||
                    destinationId == R.id.frag_voice_recordings ||
                    destinationId == R.id.frag_calm_space ||
                    destinationId == R.id.frag_cardio_coherence ||
                    destinationId == R.id.frag_presence ||
                    destinationId == R.id.frag_emotional_anchors ||
                    destinationId == R.id.frag_emotional_anchor_create ||
                    destinationId == R.id.frag_emotional_anchor_run
                ) &&
            !SubscriptionManager.hasActiveSubscriptionNow()
        ) {
            showSubscriptionPaywall()
            return
        }
        openDestinationAsSheetInternal(destinationId, startArgs)
    }

    private fun openDestinationAsSheetInternal(destinationId: Int, startArgs: Bundle? = null) {
        val tag = "sheet_dest_$destinationId"
        val existing = supportFragmentManager.findFragmentByTag(tag)
        if (existing != null) {
            if (destinationId == R.id.frag_morning_dialog) {
                (existing as? DialogFragment)?.dismissAllowingStateLoss()
            } else {
                return
            }
        }
        SheetNavHostBottomSheet.newInstance(destinationId, startArgs)
            .show(supportFragmentManager, tag)
    }

    private fun handleIncomingShareIntent(incomingIntent: Intent?) {
        val sharedText = extractSharedText(incomingIntent) ?: return
        openSharedTextImportSheet(sharedText)
    }

    private fun handleNotificationNavigationIntent(incomingIntent: Intent?) {
        if (incomingIntent?.getBooleanExtra(EXTRA_OPEN_DIARIO, false) == true) {
            bottomActive.value = "diario"
            openDestinationAsSheet(R.id.frag_diario)
            return
        }
        if (incomingIntent?.getBooleanExtra(EXTRA_OPEN_METAS, false) == true) {
            bottomActive.value = "metas"
            openDestinationAsSheet(R.id.frag_metas)
            return
        }
        if (incomingIntent?.getBooleanExtra(EXTRA_OPEN_MY_DAY, false) == true) {
            if (!SubscriptionManager.hasActiveSubscriptionNow()) {
                showSubscriptionPaywall()
                return
            }
            bottomActive.value = "my_day"
            openDestinationAsSheetInternal(R.id.frag_my_day)
            return
        }
        if (incomingIntent?.getBooleanExtra(EXTRA_OPEN_MORNING_DIALOG, false) == true) {
            if (!SubscriptionManager.hasActiveSubscriptionNow()) {
                showSubscriptionPaywall()
                return
            }
            bottomActive.value = "morning_dialog"
            val args = Bundle().apply { putBoolean(FragMorningDialog.ARG_START_FLOW, true) }
            openDestinationAsSheetInternal(R.id.frag_morning_dialog, args)
            return
        }
        val sessionId = incomingIntent?.getLongExtra(EXTRA_OPEN_MORNING_DIALOG_DETAIL_ID, -1L) ?: -1L
        if (sessionId > 0L) {
            if (!SubscriptionManager.hasActiveSubscriptionNow()) {
                showSubscriptionPaywall()
                return
            }
            bottomActive.value = "morning_dialog"
            val args = Bundle().apply { putLong(FragMorningDialog.ARG_OPEN_SESSION_ID, sessionId) }
            openDestinationAsSheetInternal(R.id.frag_morning_dialog, args)
        }
    }

    private fun extractSharedText(incomingIntent: Intent?): String? {
        if (incomingIntent?.action != Intent.ACTION_SEND) return null
        val type = incomingIntent.type ?: return null
        if (!type.startsWith("text/")) return null
        return incomingIntent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun openSharedTextImportSheet(sharedText: String) {
        val destinationId = R.id.frag_import_shared_text
        val tag = "sheet_dest_$destinationId"
        if (supportFragmentManager.findFragmentByTag(tag) != null) return
        val args = Bundle().apply {
            putString(EXTRA_SHARED_TEXT, sharedText)
        }
        openDestinationAsSheetInternal(destinationId, args)
    }

    private fun shouldRequireNotesBiometricLock(): Boolean {
        val prefs = DbPreferences.default(this)
        return prefs.getBoolean("notes_biometric_lock_enabled", false)
    }

    private fun showNotesBiometricPrompt(onSuccess: () -> Unit) {
        val canAuth = BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(
                this,
                getString(R.string.settings_biometric_unavailable),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        runCatching {
            val executor = ContextCompat.getMainExecutor(this)
            val prompt = BiometricPrompt(
                this,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        Toast.makeText(this@MainActivity, errString, Toast.LENGTH_SHORT).show()
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.main_notes_biometric_title))
                .setSubtitle(getString(R.string.main_notes_biometric_subtitle))
                .setNegativeButtonText(getString(R.string.common_cancel))
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
                .build()

            prompt.authenticate(promptInfo)
        }.onFailure { error ->
            Toast.makeText(
                this,
                error.message ?: getString(R.string.settings_biometric_start_error),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun showSubscriptionPaywall(reason: String? = null) {
        if (supportFragmentManager.findFragmentByTag(SubscriptionPaywallDialog.TAG) != null) return
        SubscriptionPaywallDialog.newInstance(reason)
            .show(supportFragmentManager, SubscriptionPaywallDialog.TAG)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (QRManager.Request_Code) {
            val qrContent = data?.getStringExtra("SCAN_RESULT")
            if (!qrContent.isNullOrEmpty()) {
                procesarQrCode(qrContent)
            } else {
                Toast.makeText(this, getString(R.string.main_qr_read_error), Toast.LENGTH_SHORT).show()
            }
            QRManager.Request_Code = false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean = true

    override fun onOptionsItemSelected(item: MenuItem): Boolean = super.onOptionsItemSelected(item)

    fun setFavColor(favState: String) {
        if (favState == "1") {
            toolbarFavColor.value = resources.getColor(R.color.fav_active, null)
            animateFavIcon()
        } else {
            toolbarFavColor.value = resources.getColor(R.color.fav_inactive, null)
        }
    }

    private fun animateFavIcon() {
        val temp = View(this)
        val scaleDown = ObjectAnimator.ofPropertyValuesHolder(
            temp,
            PropertyValuesHolder.ofFloat("scaleX", 1.3f),
            PropertyValuesHolder.ofFloat("scaleY", 1.3f)
        )
        scaleDown.duration = 300
        scaleDown.setAutoCancel(false)
        scaleDown.repeatCount = 3
        scaleDown.repeatMode = ObjectAnimator.REVERSE
        scaleDown.start()
    }

    private fun procesarQrCode(result: String?) {
        if (TextUtils.isEmpty(result)) {
            Toast.makeText(this, getString(R.string.main_import_empty), Toast.LENGTH_SHORT).show()
            QRManager.Request_Code = false
            return
        }

        val temp = result!!.split("::").toTypedArray()
        if (temp[0].contains("f")) {
            if (temp.size < 4) {
                Toast.makeText(this, getString(R.string.main_import_code_error), Toast.LENGTH_SHORT).show()
                return
            }
            val contentValues = ContentValues()
            contentValues.put("frase", temp[1])
            contentValues.put("autor", temp[2])
            contentValues.put("fuente", temp[3])
            UiModalWindows.Add_New_frase(this, contentValues)
        } else if (temp[0].contains("a")) {
            if (temp.size < 3) {
                Toast.makeText(this, getString(R.string.main_import_code_error), Toast.LENGTH_SHORT).show()
                return
            }
            val contentValues = ContentValues()
            contentValues.put("title", temp[1])
            contentValues.put("apunte", temp[2])
            UiModalWindows.ApunteManager(this, "", contentValues, false)
        }
    }

    fun auxSetColorBar(color: Int) {
        if (color != 0) {
            toolbarColor.value = color
        }
    }

    companion object {
        private const val MAIN_NAV_HOST_TAG = "main_nav_host"
        const val EXTRA_SHARED_TEXT = "extra_shared_text"
        const val EXTRA_OPEN_METAS = "extra_open_metas"
        const val EXTRA_OPEN_DIARIO = "extra_open_diario"
        const val EXTRA_OPEN_MORNING_DIALOG = "extra_open_morning_dialog"
        const val EXTRA_OPEN_MORNING_DIALOG_DETAIL_ID = "extra_open_morning_dialog_detail_id"
        const val EXTRA_OPEN_MY_DAY = "extra_open_my_day"
        private var currentActivityRef: WeakReference<MainActivity>? = null

        @JvmStatic
        fun setCurrentInstance(activity: MainActivity) {
            currentActivityRef = WeakReference(activity)
        }

        @JvmStatic
        fun currentInstance(): MainActivity? = currentActivityRef?.get()

        @JvmStatic
        fun clearCurrentInstance(activity: MainActivity) {
            val current = currentActivityRef?.get()
            if (current === activity) currentActivityRef = null
        }

        @JvmField
        var version = ""
    }

    override fun onDestroy() {
        clearCurrentInstance(this)
        super.onDestroy()
    }
}

class ToolbarIconProxy(
    private val visibilityState: MutableState<Int>,
    private val tintState: MutableState<Int>
) {
    var visibility: Int
        get() = visibilityState.value
        set(value) {
            visibilityState.value = value
        }

    fun setColorFilter(color: Int) {
        tintState.value = color
    }
}
