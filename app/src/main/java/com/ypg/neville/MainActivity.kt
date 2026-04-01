package com.ypg.neville

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.content.edit
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.net.toUri
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import com.google.android.material.navigation.NavigationView
import com.google.zxing.integration.android.IntentIntegrator
import com.ypg.neville.ui.frag.FragContentWebView
import com.ypg.neville.ui.frag.frag_listado
import com.ypg.neville.model.db.DatabaseHelper
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.model.utils.QRManager
import com.ypg.neville.model.utils.UiModalWindows
import com.ypg.neville.model.utils.Utils
import com.ypg.neville.model.utils.myListener_In_App_Update
import com.ypg.neville.model.utils.utilsFields
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {

    lateinit var drawerLayout: DrawerLayout
    lateinit var navigationView: NavigationView
    lateinit var bottomNavigationView: CardView
    lateinit var toolbar: Toolbar
    lateinit var toggle: ActionBarDrawerToggle

    lateinit var fraseBienvenida: TextView
    lateinit var headerImage: ImageView

    lateinit var navController: NavController

    lateinit var ic_toolsBar_nota_add: ImageView
    lateinit var ic_toolsBar_frase_add: ImageView
    lateinit var ic_toolsBar_fav: ImageView
    lateinit var navBtnConf: ImageButton
    lateinit var navBtnNotas: ImageButton
    lateinit var navBtnHome: ImageButton
    lateinit var navBtnDiario: ImageButton
    lateinit var navBtnChat: ImageButton
    lateinit var bottomNavInner: LinearLayout

    val utils = Utils(this)

    lateinit var frag_container: FragmentContainerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setCurrentInstance(this)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (prefs.getBoolean("tema", true)) {
            setTheme(R.style.Theme_NevilleProyect_noche)
        } else {
            setTheme(R.style.Theme_NevilleProyect)
        }

        setContentView(R.layout.activity_main)

        ic_toolsBar_nota_add = findViewById(R.id.ic_toolbar_add_note)
        ic_toolsBar_frase_add = findViewById(R.id.ic_toolbar_add_frase)
        ic_toolsBar_fav = findViewById(R.id.ic_toolbar_fav)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        toolbar = findViewById(R.id.toolsbar)
        bottomNavigationView = findViewById(R.id.bottom_navigation_view)
        navBtnConf = findViewById(R.id.nav_btn_conf)
        navBtnNotas = findViewById(R.id.nav_btn_notas)
        navBtnHome = findViewById(R.id.nav_btn_home)
        navBtnDiario = findViewById(R.id.nav_btn_diario)
        navBtnChat = findViewById(R.id.nav_btn_chat)
        bottomNavInner = findViewById(R.id.bottom_nav_inner)
        frag_container = findViewById(R.id.frag_container)

        val navigationHeader = navigationView.getHeaderView(0)
        fraseBienvenida = navigationHeader.findViewById(R.id.drawer_header_frase)
        headerImage = navigationHeader.findViewById(R.id.drawer_header_imgbutton)

        val temp_Color = prefs.getInt("color_marcos", 0)
        AuxSetColorBar(temp_Color)

        fraseBienvenida.text = prefs.getString("frase", "Imaginar crea la realidad")
        headerImage.clipToOutline = true

        navController = frag_container.findNavController()

        val in_app_update = myListener_In_App_Update(this)
        in_app_update.setMylistener(object : myListener_In_App_Update.In_mylistener {
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

        ic_toolsBar_frase_add.setOnClickListener {
            UiModalWindows.Add_New_frase(this, null)
        }

        ic_toolsBar_nota_add.setOnClickListener {
            UiModalWindows.ApunteManager(this, "", null, false)
        }

        setupBottomNav()

        ic_toolsBar_fav.setOnClickListener {
            var result = ""
            val fragment = frag_container.getFragment<androidx.fragment.app.Fragment>()
            val fragName = fragment.javaClass.simpleName

            if (fragName.contains("FragContentWebView")) {
                result = utilsDB.UpdateFavorito(this, DatabaseHelper.T_Conf, DatabaseHelper.C_conf_title, utilsFields.ID_Str_row_ofElementLoad, -1)
            }

            if (result != "") {
                setFavColor(result)
            }
        }

        setSupportActionBar(toolbar)
        toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.nav_cerrado, R.string.nav_abierto)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener { item ->
            deselecItemBottom()
            when (item.itemId) {
                R.id.drawer_menu_biografia -> {
                    FragContentWebView.elementLoaded = "biografia"
                    FragContentWebView.extension = ".html"
                    FragContentWebView.urlPath = "file:///android_asset/autores/neville/biografia/biografia.txt"
                    navController.navigate(R.id.frag_content_webview)
                }
                R.id.drawer_menu_galeriafotos -> {
                    FragContentWebView.elementLoaded = "galeriafotos"
                    FragContentWebView.extension = ".html"
                    FragContentWebView.urlPath = "file:///android_asset/autores/neville/biografia/gale_Galeria de fotos.html"
                    navController.navigate(R.id.frag_content_webview)
                }
                R.id.drawer_menu_abdullah -> {
                    if (Utils.isConnection(this)) {
                        val intent = Intent(Intent.ACTION_VIEW, "https://www.youtube.com/watch?v=mgbdcv606Rg".toUri())
                        startActivity(intent)
                    }
                }
                R.id.drawer_menu_conferen_texto -> {
                    frag_listado.elementLoaded = "autores/neville/conf"
                    setBottomActive(R.id.nav_btn_conf)
                    navController.navigate(R.id.frag_listado)
                }
                R.id.drawer_menu_preguntas -> {
                    frag_listado.elementLoaded = "preguntas"
                    navController.navigate(R.id.frag_listado)
                }
                R.id.drawer_menu_citas_conf -> {
                    frag_listado.elementLoaded = "citasConferencias"
                    navController.navigate(R.id.frag_listado)
                }
                R.id.drawer_menu_conferen_audio -> {
                    if (Utils.isConnection(this)) {
                        val intent = Intent(Intent.ACTION_VIEW, "https://www.ivoox.com/escuchar-neville-goddard_nq_102778_1.html".toUri())
                        startActivity(intent)
                    }
                }
                R.id.drawer_menu_frases -> {
                    setBottomActive(null)
                    navController.navigate(R.id.frag_home)
                }
                R.id.drawer_menu_books -> {
                    if (Utils.isConnection(this)) {
                        val intent = Intent(Intent.ACTION_VIEW, "https://drive.google.com/file/d/1NjUDZfjSOjdPRd6vsyhfDKmjdDus25YM/view?usp=sharing".toUri())
                        startActivity(intent)
                    }
                }
                R.id.drawer_menu_audiobook -> {
                    if (Utils.isConnection(this)) {
                        val intent = Intent(Intent.ACTION_VIEW, "https://www.ivoox.com/escuchar-neville-goddard_nq_102778_1.html".toUri())
                        startActivity(intent)
                    }
                }
                R.id.drawer_menu_ayudas -> {
                    frag_listado.elementLoaded = "ayudas"
                    navController.navigate(R.id.frag_listado)
                }
                R.id.drawer_menu_neville_autor -> {
                    navController.navigate(R.id.frag_neville_goddard)
                }
                R.id.drawer_menu_gregg -> {
                    navController.navigate(R.id.frag_gregg)
                }
                R.id.drawer_menu_joe -> {
                    navController.navigate(R.id.frag_joe_dispenza)
                }
                R.id.drawer_menu_bruce -> {
                    navController.navigate(R.id.frag_bruce_lipton)
                }
                R.id.drawer_menu_audio_telegram -> {
                    if (Utils.isConnection(this)) {
                        if (Utils.isPackageInstalled("org.telegram.messenger", this)) {
                            val intent = Intent(Intent.ACTION_VIEW, "https://t.me/nevilleGoddardaudios".toUri())
                            intent.setPackage("org.telegram.messenger")
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "Debe estar instalado Telegram", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                R.id.drawer_menu_audio_telegram_ii -> {
                    if (Utils.isConnection(this)) {
                        if (Utils.isPackageInstalled("org.telegram.messenger", this)) {
                            val intent = Intent(Intent.ACTION_VIEW, "https://t.me/NevilleAudiosII".toUri())
                            intent.setPackage("org.telegram.messenger")
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "Debe estar instalado Telegram", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                R.id.drawer_menu_audio_telegram_ypg -> {
                    if (Utils.isConnection(this)) {
                        if (Utils.isPackageInstalled("org.telegram.messenger", this)) {
                            val intent = Intent(Intent.ACTION_VIEW, "https://t.me/+rODRAz2S6nVmMmY0".toUri())
                            intent.setPackage("org.telegram.messenger")
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "Debe estar instalado Telegram", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                R.id.drawer_menu_web_neville_blog -> {
                    if (Utils.isConnection(this)) {
                        val intent = Intent(Intent.ACTION_VIEW, "https://nevilleenespanol.blogspot.com/".toUri())
                        startActivity(intent)
                    }
                }
                R.id.drawer_menu_web_neville_espanol -> {
                    if (Utils.isConnection(this)) {
                        val intent = Intent(Intent.ACTION_VIEW, "https://neville-espanol.com/".toUri())
                        startActivity(intent)
                    }
                }
                R.id.drawer_menu_web_real_neville -> {
                    if (Utils.isConnection(this)) {
                        val intent = Intent(Intent.ACTION_VIEW, "https://realneville.com/".toUri())
                        startActivity(intent)
                    }
                }
            }
            drawerLayout.close()
            false
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (QRManager.Request_Code) {
            val intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            if (intentResult != null) {
                ProcesarQRCode(intentResult.contents)
            } else {
                Toast.makeText(this, "Error al leer el código QR", Toast.LENGTH_SHORT).show()
            }
            QRManager.Request_Code = false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menuprincipal, menu)
        return true
    }

    @SuppressLint("Range")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.main_menu_shared_app -> {
                if (Utils.isConnection(this)) {
                    QRManager.ShowQRDialog(this, "https://play.google.com/store/apps/details?id=com.ypg.neville", "Compartir App Neville", null)
                }
            }
            R.id.main_menu_leerQR -> {
                QRManager.launch_QRRead()
            }
            R.id.main_menu_myinfo -> {
                deselecItemBottom()
                navController.navigate(R.id.frag_list_info)
            }
            R.id.main_menu_setup -> {
                deselecItemBottom()
                navController.navigate(R.id.fragSetting)
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    fun setFavColor(fav_state: String) {
        if (fav_state == "1") {
            ic_toolsBar_fav.setColorFilter(resources.getColor(R.color.fav_active, null))
            animate(ic_toolsBar_fav)
        } else {
            ic_toolsBar_fav.setColorFilter(resources.getColor(R.color.fav_inactive, null))
        }
    }

    private fun animate(view: View) {
        val scaleDown = ObjectAnimator.ofPropertyValuesHolder(view,
            PropertyValuesHolder.ofFloat("scaleX", 1.3f),
            PropertyValuesHolder.ofFloat("scaleY", 1.3f))
        scaleDown.duration = 300
        scaleDown.setAutoCancel(false)
        scaleDown.repeatCount = 3
        scaleDown.repeatMode = ObjectAnimator.REVERSE
        scaleDown.start()
    }

    private fun deselecItemBottom() {
        setBottomActive(null)
    }

    private fun setupBottomNav() {
        navBtnConf.setOnClickListener {
            setBottomActive(R.id.nav_btn_conf)
            frag_listado.elementLoaded = "autores/neville/conf"
            navController.navigate(R.id.frag_listado)
        }

        navBtnNotas.setOnClickListener {
            setBottomActive(R.id.nav_btn_notas)
            navController.navigate(R.id.frag_notas)
        }

        navBtnHome.setOnClickListener {
            setBottomActive(R.id.nav_btn_home)
            Toast.makeText(this, "Inicio próximamente", Toast.LENGTH_SHORT).show()
        }

        navBtnDiario.setOnClickListener {
            setBottomActive(R.id.nav_btn_diario)
            Toast.makeText(this, "Diario próximamente", Toast.LENGTH_SHORT).show()
        }

        navBtnChat.setOnClickListener {
            setBottomActive(R.id.nav_btn_chat)
            Toast.makeText(this, "Chat próximamente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setBottomActive(activeId: Int?) {
        val buttons = listOf(navBtnConf, navBtnNotas, navBtnHome, navBtnDiario, navBtnChat)
        buttons.forEach { button ->
            val isActive = button.id == activeId
            button.background = if (isActive) AppCompatResources.getDrawable(this, R.drawable.bg_nav_item_active) else null
            button.imageAlpha = if (isActive) 255 else 185
            button.scaleX = if (isActive) 1.06f else 1f
            button.scaleY = if (isActive) 1.06f else 1f
        }
    }

    private fun ProcesarQRCode(result: String?) {
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

    fun AuxSetColorBar(color: Int) {
        if (color != 0) {
            toolbar.background?.setColorFilter(
                BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_ATOP)
            )
            bottomNavInner.background?.setColorFilter(
                BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_ATOP)
            )
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
