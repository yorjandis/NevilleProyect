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
import com.ypg.neville.model.reminders.JournalDailyReminderManager
import com.ypg.neville.model.subscription.SubscriptionManager
import com.ypg.neville.ui.frag.HomeFloatingMenuBottomSheet
import com.ypg.neville.ui.frag.NevilleBottomNavBar
import com.ypg.neville.ui.frag.SheetNavHostBottomSheet
import com.ypg.neville.ui.frag.SubscriptionPaywallDialog
import com.ypg.neville.ui.frag.frag_listado
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {

    private val toolbarColor = mutableStateOf<Int?>(null)
    private val bottomActive = mutableStateOf<String?>("conf")

    private val toolbarAddNoteVisible = mutableStateOf(View.VISIBLE)
    private val toolbarAddFraseVisible = mutableStateOf(View.VISIBLE)
    private val toolbarFavVisible = mutableStateOf(View.GONE)
    private val toolbarFavColor = mutableStateOf(android.graphics.Color.BLACK)

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
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkTheme) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        setTheme(R.style.Theme_NevilleProyect)

        super.onCreate(savedInstanceState)
        setCurrentInstance(this)
        SubscriptionManager.initialize(this)
        JournalDailyReminderManager.initialize(this)

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
                        utils.show_Notification("Nueva actualización disponible!", intentNotification)
                    } else {
                        Toast.makeText(this@MainActivity, "Nueva actualización disponible!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })

        val hadLegacyDatabase = utilsDB.hasLegacyDatabase(this)
        if (hadLegacyDatabase) {
            Toast.makeText(this, "Estamos migrando tus datos a la nueva base de datos. Por favor espera...", Toast.LENGTH_LONG).show()
        }

        if (utilsDB.RestoreDBInfo(this)) {
            if (hadLegacyDatabase) {
                Toast.makeText(this, "Migración de datos completada.", Toast.LENGTH_SHORT).show()
            }
            recreate()
        } else {
            if (hadLegacyDatabase) {
                Toast.makeText(this, "Migración de datos completada.", Toast.LENGTH_SHORT).show()
            }
            val currentInstallMarker = currentInstallFirstTime()
            val storedInstallMarker = prefs.getLong(installMarkerPrefKey, -1L)
            val isFirstLaunchForCurrentInstall = currentInstallMarker > 0L && storedInstallMarker != currentInstallMarker
            val shouldShowNews = prefs.getBoolean("Is_primeraVez", true) || isFirstLaunchForCurrentInstall

            if (shouldShowNews) {
                UiModalWindows.showAyudaContectual(
                    this,
                    "Novedades",
                    "Que hay de nuevo?",
                    NewsContent.buildNewsText(),
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

        if (prefs.getBoolean("updateFrases", true)) {
            utilsDB.CorrectOrtogFrases(this)
            prefs.edit { putBoolean("updateFrases", false) }
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
        controller.hide(WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    @Composable
    private fun MainScreen() {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidNavHostContainer()
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                BottomNav()
            }
        }
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

        val navHost = NavHostFragment.create(R.navigation.nav_graf)
        supportFragmentManager.commitNow {
            setReorderingAllowed(true)
            existing?.let { remove(it) }
            replace(container.id, navHost, MAIN_NAV_HOST_TAG)
        }
        navController = navHost.navController
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
                openDestinationAsSheet(R.id.frag_listado)
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
            onVoces = {
                bottomActive.value = "voces"
                openDestinationAsSheet(R.id.frag_voice_recordings)
            },
            onAnclas = {
                bottomActive.value = "anclas"
                openDestinationAsSheet(R.id.frag_emotional_anchors)
            }
        )
    }

    fun openDestinationAsSheet(destinationId: Int) {
        if (destinationId == R.id.frag_home) return
        if (destinationId == R.id.frag_notas && shouldRequireNotesBiometricLock()) {
            if (!SubscriptionManager.hasActiveSubscriptionNow()) {
                showSubscriptionPaywall("La protección biométrica de Notas forma parte de la suscripción anual.")
                return
            }
            showNotesBiometricPrompt {
                openDestinationAsSheetInternal(destinationId)
            }
            return
        }
        if (
            (
                destinationId == R.id.frag_metas ||
                    destinationId == R.id.frag_lienzo ||
                    destinationId == R.id.frag_reminders ||
                    destinationId == R.id.frag_voice_recordings ||
                    destinationId == R.id.frag_emotional_anchors ||
                    destinationId == R.id.frag_emotional_anchor_create ||
                    destinationId == R.id.frag_emotional_anchor_run
                ) &&
            !SubscriptionManager.hasActiveSubscriptionNow()
        ) {
            showSubscriptionPaywall()
            return
        }
        openDestinationAsSheetInternal(destinationId)
    }

    private fun openDestinationAsSheetInternal(destinationId: Int, startArgs: Bundle? = null) {
        val tag = "sheet_dest_$destinationId"
        if (supportFragmentManager.findFragmentByTag(tag) != null) return
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
                "No hay biometría disponible/configurada en este dispositivo",
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
                .setTitle("Acceso a Notas")
                .setSubtitle("Autentícate para abrir tu lista de notas")
                .setNegativeButtonText("Cancelar")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
                .build()

            prompt.authenticate(promptInfo)
        }.onFailure { error ->
            Toast.makeText(
                this,
                error.message ?: "No se pudo iniciar autenticación biométrica",
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
                Toast.makeText(this, "Error al leer el código QR", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "No se puede importar un texto vacío", Toast.LENGTH_SHORT).show()
            QRManager.Request_Code = false
            return
        }

        val temp = result!!.split("::").toTypedArray()
        if (temp[0].contains("f")) {
            if (temp.size < 4) {
                Toast.makeText(this, "No se pudo importar el código", Toast.LENGTH_SHORT).show()
                return
            }
            val contentValues = ContentValues()
            contentValues.put("frase", temp[1])
            contentValues.put("autor", temp[2])
            contentValues.put("fuente", temp[3])
            UiModalWindows.Add_New_frase(this, contentValues)
        } else if (temp[0].contains("a")) {
            if (temp.size < 3) {
                Toast.makeText(this, "No se pudo importar el código", Toast.LENGTH_SHORT).show()
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
