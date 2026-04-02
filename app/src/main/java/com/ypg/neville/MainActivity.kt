package com.ypg.neville

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceManager
import com.google.zxing.integration.android.IntentIntegrator
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.model.utils.QRManager
import com.ypg.neville.model.utils.UiModalWindows
import com.ypg.neville.model.utils.Utils
import com.ypg.neville.model.utils.myListener_In_App_Update
import com.ypg.neville.ui.frag.HomeFloatingMenuBottomSheet
import com.ypg.neville.ui.frag.SheetNavHostBottomSheet
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

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (prefs.getBoolean("tema", true)) {
            setTheme(R.style.Theme_NevilleProyect_noche)
        } else {
            setTheme(R.style.Theme_NevilleProyect)
        }

        super.onCreate(savedInstanceState)
        setCurrentInstance(this)

        toolbarColor.value = prefs.getInt("color_marcos", 0).takeIf { it != 0 }

        setContentView(
            ComposeView(this).apply {
                setContent {
                    MaterialTheme {
                        MainScreen()
                    }
                }
            }
        )

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
            if (prefs.getBoolean("Is_primeraVez", true)) {
                UiModalWindows.showAyudaContectual(
                    this,
                    "Novedades",
                    "Que hay de nuevo?",
                    getString(R.string.news),
                    false,
                    AppCompatResources.getDrawable(this, R.drawable.neville)
                )
                prefs.edit { putBoolean("Is_primeraVez", false) }
            }
        }

        if (prefs.getBoolean("updateFrases", true)) {
            utilsDB.CorrectOrtogFrases(this)
            prefs.edit { putBoolean("updateFrases", false) }
        }
    }

    @Composable
    private fun MainScreen() {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                AndroidNavHostContainer()
            }

            BottomNav()
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
        val existing = supportFragmentManager.findFragmentById(container.id) as? NavHostFragment
        if (existing != null) {
            navController = existing.navController
            return
        }

        if (!container.isAttachedToWindow) return

        val navHost = NavHostFragment.create(R.navigation.nav_graf)
        supportFragmentManager.commitNow {
            replace(container.id, navHost)
        }
        navController = navHost.navController
    }

    @Composable
    private fun BottomNav() {
        val barShape = RoundedCornerShape(30.dp)
        val tintColor = toolbarColor.value?.let { Color(it) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 0.dp, bottom = 14.dp)
                .height(68.dp)
                .shadow(elevation = 16.dp, shape = barShape, clip = false)
                .border(width = 1.dp, color = Color(0x88FFFFFF), shape = barShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFB9BFC0),
                            Color(0xFFD4DBE0),
                            Color(0xFFB8C0C7)
                        )
                    ),
                    shape = barShape
                )
                .clip(barShape)
        ) {
            if (tintColor != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(tintColor.copy(alpha = 0.30f))
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xCCFFFFFF),
                                Color.Transparent
                            )
                        )
                    )
                    .align(Alignment.TopCenter)
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavButton("conf", R.drawable.ic_conf) {
                    bottomActive.value = "conf"
                    frag_listado.elementLoaded = "autores/neville/conf"
                    openDestinationAsSheet(R.id.frag_listado)
                }
                BottomNavButton("notas", R.drawable.ic_note) {
                    bottomActive.value = "notas"
                    openDestinationAsSheet(R.id.frag_notas)
                }
                BottomNavButton("home", R.drawable.ic_nav_home) {
                    bottomActive.value = "home"
                    if (supportFragmentManager.findFragmentByTag(HomeFloatingMenuBottomSheet.TAG) == null) {
                        HomeFloatingMenuBottomSheet().show(supportFragmentManager, HomeFloatingMenuBottomSheet.TAG)
                    }
                }
                BottomNavButton("diario", R.drawable.ic_nav_journal) {
                    bottomActive.value = "diario"
                    Toast.makeText(this@MainActivity, "Diario próximamente", Toast.LENGTH_SHORT).show()
                }
                BottomNavButton("chat", R.drawable.ic_nav_chat) {
                    bottomActive.value = "chat"
                    Toast.makeText(this@MainActivity, "Chat próximamente", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun openDestinationAsSheet(destinationId: Int) {
        if (destinationId == R.id.frag_home) return
        val tag = "sheet_dest_$destinationId"
        if (supportFragmentManager.findFragmentByTag(tag) != null) return
        SheetNavHostBottomSheet.newInstance(destinationId)
            .show(supportFragmentManager, tag)
    }

    @Composable
    private fun RowScope.BottomNavButton(id: String, icon: Int, onClick: () -> Unit) {
        val active = bottomActive.value == id
        val itemShape = RoundedCornerShape(18.dp)
        Box(
            modifier = Modifier
                .height(44.dp)
                .weight(1f)
                .padding(horizontal = 2.dp)
                .clip(itemShape)
                .background(
                    if (active) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFE6EEF3),
                                Color(0xFFC7D2DA)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Transparent)
                        )
                    },
                    itemShape
                )
                .border(
                    width = if (active) 1.dp else 0.dp,
                    color = if (active) Color(0x66FFFFFF) else Color.Transparent,
                    shape = itemShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = id,
                tint = if (active) Color(0xFF1E2A32) else Color(0xFF2E3B44),
                modifier = Modifier.size(22.dp)
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (QRManager.Request_Code) {
            val intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            if (intentResult != null) {
                procesarQrCode(intentResult.contents)
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
