package com.ypg.neville

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import com.google.android.material.navigation.NavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.zxing.integration.android.IntentIntegrator
import com.ismaeldivita.chipnavigation.ChipNavigationBar
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.ypg.neville.Ui.frag.frag_content_WebView
import com.ypg.neville.Ui.frag.frag_home
import com.ypg.neville.Ui.frag.frag_listado
import com.ypg.neville.model.db.DatabaseHelper
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.model.utils.QRManager
import com.ypg.neville.model.utils.UiModalWindows
import com.ypg.neville.model.utils.Utils
import com.ypg.neville.model.utils.myListener_In_App_Update
import com.ypg.neville.model.utils.utilsFields
import com.ypg.neville.services.serviceStreaming

class MainActivity : AppCompatActivity() {

    var mservise: serviceStreaming? = null

    lateinit var drawerLayout: DrawerLayout
    lateinit var navigationView: NavigationView
    lateinit var bottomNavigationView: ChipNavigationBar
    lateinit var toolbar: Toolbar
    lateinit var toggle: ActionBarDrawerToggle

    lateinit var fraseBienvenida: TextView
    lateinit var headerImage: ImageView

    private var firebaseAnalytics: FirebaseAnalytics? = null

    lateinit var navController: NavController

    lateinit var ic_toolsBar_nota_add: ImageView
    lateinit var ic_toolsBar_frase_add: ImageView
    lateinit var ic_toolsBar_fav: ImageView
    lateinit var fabTestNotas: FloatingActionButton

    val utils = Utils(this)

    lateinit var frag_container: FragmentContainerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainActivityThis = this

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
        fabTestNotas = findViewById(R.id.fab_test_notas)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        toolbar = findViewById(R.id.toolsbar)
        bottomNavigationView = findViewById(R.id.bottom_navigation_view)
        frag_container = findViewById(R.id.frag_container)

        val navigationHeader = navigationView.getHeaderView(0)
        fraseBienvenida = navigationHeader.findViewById(R.id.drawer_header_frase)
        headerImage = navigationHeader.findViewById(R.id.drawer_header_imgbutton)

        val temp_Color = prefs.getInt("color_marcos", 0)
        AuxSetColorBar(temp_Color)

        fraseBienvenida.text = prefs.getString("frase", "Imaginar crea la realidad")
        headerImage.clipToOutline = true

        navController = Navigation.findNavController(frag_container)

        val in_app_update = myListener_In_App_Update(this)
        in_app_update.setMylistener(object : myListener_In_App_Update.In_mylistener {
            override fun onUpdateAvailable(pUpdateAvailable: Boolean) {
                if (pUpdateAvailable) {
                    val intentNotification = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                    utils.show_Notification("Nueva actualización disponible!", intentNotification)
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
            val intent = intent
            finish()
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        } else {
            if (hadLegacyDatabase) {
                Toast.makeText(this, "Migración de datos completada.", Toast.LENGTH_SHORT).show()
            }
            if (prefs.getBoolean("Is_primeraVez", true)) {
                UiModalWindows.showAyudaContectual(this, "Novedades", "Que hay de nuevo?", getString(R.string.news), false, getDrawable(R.drawable.neville))
                prefs.edit().putBoolean("Is_primeraVez", false).apply()
            }
        }

        if (prefs.getBoolean("updateFrases", true)) {
            utilsDB.CorrectOrtogFrases(this)
            prefs.edit().putBoolean("updateFrases", false).apply()
        }

        ic_toolsBar_frase_add.setOnClickListener {
            UiModalWindows.Add_New_frase(this, null)
        }

        ic_toolsBar_nota_add.setOnClickListener {
            UiModalWindows.ApunteManager(this, "", null, false)
        }

        fabTestNotas.setOnClickListener {
            deselecItemBottom()
            navController.navigate(R.id.frag_notas)
        }

        ic_toolsBar_fav.setOnClickListener {
            var result = ""
            val fragment = frag_container.getFragment<androidx.fragment.app.Fragment>()
            val fragName = fragment.javaClass.simpleName

            if (fragName.contains("frag_content_WebView")) {
                result = utilsDB.UpdateFavorito(this, DatabaseHelper.T_Conf, DatabaseHelper.C_conf_title, utilsFields.ID_Str_row_ofElementLoad, -1)
            } else if (fragName.contains("frag_listado")) {
                when (frag_listado.elementLoaded) {
                    "video_conf", "video_book", "video_gredd" -> {
                        result = utilsDB.UpdateFavorito(this, DatabaseHelper.T_Videos, DatabaseHelper.C_videos_title, utilsFields.ID_Str_row_ofElementLoad, -1)
                    }
                    "video_ext", "audio_ext" -> {
                        result = utilsDB.UpdateFavorito(this, DatabaseHelper.T_Repo, DatabaseHelper.C_repo_title, utilsFields.ID_Str_row_ofElementLoad, -1)
                    }
                }
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
                    frag_content_WebView.elementLoaded = "biografia"
                    frag_content_WebView.extension = ".html"
                    frag_content_WebView.urlPath = "file:///android_asset/biog_quien es neville goddard.html"
                    navController.navigate(R.id.frag_content_webview)
                }
                R.id.drawer_menu_galeriafotos -> {
                    frag_content_WebView.elementLoaded = "galeriafotos"
                    frag_content_WebView.extension = ".html"
                    frag_content_WebView.urlPath = "file:///android_asset/gale_Galeria de fotos.html"
                    navController.navigate(R.id.frag_content_webview)
                }
                R.id.drawer_menu_abdullah -> {
                    if (Utils.isConnection(this)) {
                        frag_listado.elementLoaded = "play_youtube"
                        frag_listado.urlPath = "mgbdcv606Rg"
                        navController.navigate(R.id.frag_listado)
                    }
                }
                R.id.drawer_menu_conferen_texto -> {
                    frag_listado.elementLoaded = "conf"
                    bottomNavigationView.setItemSelected(R.id.bottom_menu_conf, true)
                    navController.navigate(R.id.frag_listado)
                }
                R.id.drawer_menu_offline_videos -> {
                    obtenerPermisos()
                    frag_listado.elementLoaded = "video_ext"
                    navController.navigate(R.id.frag_listado)
                }
                R.id.drawer_menu_offline_audios -> {
                    obtenerPermisos()
                    frag_listado.elementLoaded = "audio_ext"
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
                R.id.drawer_menu_conferen_video -> {
                    if (Utils.isConnection(this)) {
                        frag_listado.elementLoaded = "video_conf"
                        bottomNavigationView.setItemSelected(R.id.bottom_menu_videos, true)
                        navController.navigate(R.id.frag_listado)
                    }
                }
                R.id.drawer_menu_conferen_audio -> {
                    if (Utils.isConnection(this)) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.ivoox.com/escuchar-neville-goddard_nq_102778_1.html"))
                        startActivity(intent)
                    }
                }
                R.id.drawer_menu_frases -> {
                    frag_home.elementLoaded_home = "frases"
                    bottomNavigationView.setItemSelected(R.id.bottom_menu_citas, true)
                    navController.navigate(R.id.frag_home)
                }
                R.id.drawer_menu_books -> {
                    if (Utils.isConnection(this)) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/file/d/1NjUDZfjSOjdPRd6vsyhfDKmjdDus25YM/view?usp=sharing"))
                        startActivity(intent)
                    }
                }
                R.id.drawer_menu_audiobook -> {
                    if (Utils.isConnection(this)) {
                        frag_listado.elementLoaded = "video_book"
                        bottomNavigationView.setItemSelected(R.id.bottom_menu_books, true)
                        navController.navigate(R.id.frag_listado)
                    }
                }
                R.id.drawer_menu_ayudas -> {
                    frag_listado.elementLoaded = "ayudas"
                    navController.navigate(R.id.frag_listado)
                }
                R.id.drawer_menu_gregg -> {
                    navController.navigate(R.id.frag_gregg)
                }
                R.id.drawer_menu_audio_telegram -> {
                    if (Utils.isConnection(this)) {
                        if (Utils.isPackageInstalled("org.telegram.messenger", this)) {
                            val webpage = Uri.parse("https://t.me/nevilleGoddardaudios")
                            val intent = Intent(Intent.ACTION_VIEW, webpage)
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
                            val webpage2 = Uri.parse("https://t.me/NevilleAudiosII")
                            val intent = Intent(Intent.ACTION_VIEW, webpage2)
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
                            val webpage3 = Uri.parse("https://t.me/+rODRAz2S6nVmMmY0")
                            val intent = Intent(Intent.ACTION_VIEW, webpage3)
                            intent.setPackage("org.telegram.messenger")
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "Debe estar instalado Telegram", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                R.id.drawer_menu_web_neville_blog -> {
                    if (Utils.isConnection(this)) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://nevilleenespanol.blogspot.com/"))
                        startActivity(intent)
                    }
                }
                R.id.drawer_menu_web_neville_espanol -> {
                    if (Utils.isConnection(this)) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://neville-espanol.com/"))
                        startActivity(intent)
                    }
                }
                R.id.drawer_menu_web_real_neville -> {
                    if (Utils.isConnection(this)) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://realneville.com/"))
                        startActivity(intent)
                    }
                }
            }
            drawerLayout.close()
            false
        }

        bottomNavigationView.setOnItemSelectedListener(object : ChipNavigationBar.OnItemSelectedListener {
            override fun onItemSelected(i: Int) {
                when (i) {
                    R.id.bottom_menu -> {
                        drawerLayout.open()
                        bottomNavigationView.setItemSelected(R.id.bottom_menu, false)
                    }
                    R.id.bottom_menu_conf -> {
                        frag_listado.elementLoaded = "conf"
                        navController.navigate(R.id.frag_listado)
                    }
                    R.id.bottom_menu_videos -> {
                        if (Utils.isConnection(this@MainActivity)) {
                            frag_listado.elementLoaded = "video_conf"
                            navController.navigate(R.id.frag_listado)
                        }
                    }
                    R.id.bottom_menu_books -> {
                        if (Utils.isConnection(this@MainActivity)) {
                            frag_listado.elementLoaded = "video_book"
                            navController.navigate(R.id.frag_listado)
                        }
                    }
                    R.id.bottom_menu_citas -> {
                        frag_listado.elementLoaded = "frases"
                        navController.navigate(R.id.frag_home)
                    }
                }
            }
        })
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

    private fun obtenerPermisos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            Dexter.withContext(this)
                .withPermissions(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport) {}
                    override fun onPermissionRationaleShouldBeShown(permissions: List<PermissionRequest>, token: PermissionToken) {}
                }).check()
        } else {
            Utils.CrearDirectoriosRepo(this)
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
        bottomNavigationView.setItemSelected(R.id.bottom_menu_conf, false)
        bottomNavigationView.setItemSelected(R.id.bottom_menu_books, false)
        bottomNavigationView.setItemSelected(R.id.bottom_menu_citas, false)
        bottomNavigationView.setItemSelected(R.id.bottom_menu_videos, false)
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
        val background = toolbar.background
        if (color != 0) {
            background.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_ATOP))
            val backgroundBottom = bottomNavigationView.background
            backgroundBottom.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_ATOP))
        }
    }

    companion object {
        @JvmField
        var mainActivityThis: MainActivity? = null
        @JvmField
        var version = ""
        @JvmField
        var prefijo = ""
    }
}
