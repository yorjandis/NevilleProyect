package com.ypg.neville.ui.frag

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.edit
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import com.skydoves.balloon.Balloon
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.db.DatabaseHelper
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.model.utils.QRManager
import com.ypg.neville.model.utils.UiModalWindows
import com.ypg.neville.model.utils.balloon.HelpBalloon
import com.ypg.neville.model.utils.utilsFields

class FragHome : Fragment() {

    private lateinit var textFrase: TextView
    private lateinit var textAutor: TextView
    private lateinit var icFav: AppCompatImageView
    private lateinit var icShared: AppCompatImageView
    private var idFrase: Long = 0
    private lateinit var navController: NavController
    private lateinit var inlineIconsLayout: LinearLayout
    private lateinit var ayudaContextual: ImageButton
    private lateinit var masInfo: ImageView
    private var isFirstLoad = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.frag_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textFrase = view.findViewById(R.id.frag_home_text)
        textAutor = view.findViewById(R.id.frag_home_textautor)
        icFav = view.findViewById(R.id.ic_frase_fav)
        icShared = view.findViewById(R.id.ic_frase_shared)
        inlineIconsLayout = view.findViewById(R.id.layout_fraghome_icons_inlines_frase)
        ayudaContextual = view.findViewById(R.id.frag_home_ayuda)
        masInfo = view.findViewById(R.id.frag_home_frases_img_abajo)

        navController = view.findNavController()

        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        ayudaContextual.visibility = if (prefs.getBoolean("help_inline", true)) View.VISIBLE else View.GONE
        ayudaContextual.setOnClickListener { showAyudaContextual() }

        textFrase.textSize = prefs.getString("fuente_frase", "28")?.toFloat() ?: 28f

        val tempColor = prefs.getInt("color_letra_frases", 0)
        if (tempColor != 0) {
            textFrase.setTextColor(tempColor)
        }

        val hideInlineControls = prefs.getBoolean("hide_frase_controles", false)
        inlineIconsLayout.isInvisible = hideInlineControls
        masInfo.setImageResource(if (hideInlineControls) R.drawable.ic_abajo else R.drawable.ic_arriba)

        if (isFirstLoad) {
            initHome()
            isFirstLoad = false
        } else {
            loadFrases(false)
        }

        masInfo.setOnClickListener {
            val shouldHide = !inlineIconsLayout.isInvisible
            inlineIconsLayout.isInvisible = shouldHide
            masInfo.setImageResource(if (shouldHide) R.drawable.ic_abajo else R.drawable.ic_arriba)
            prefs.edit { putBoolean("hide_frase_controles", shouldHide) }
        }

        textFrase.setOnClickListener {
            val startMode = prefs.getString("list_start_load", "Nada") ?: "Nada"
            loadFrases(startMode.contains("Frase_fav_azar"))
        }

        textFrase.setOnLongClickListener {
            val nota = utilsDB.getFraseNota(requireContext(), textFrase.text.toString())
            UiModalWindows.NotaManager(
                requireContext(),
                nota,
                DatabaseHelper.T_Frases,
                DatabaseHelper.C_frases_frase,
                textFrase.text.toString()
            )
            true
        }

        icFav.setOnClickListener {
            val result = utilsDB.UpdateFavorito(
                requireContext(),
                DatabaseHelper.T_Frases,
                DatabaseHelper.CC_id,
                "",
                idFrase.toInt()
            )
            if (result.isNotEmpty()) {
                setFavColor(result)
            }
        }

        icShared.setOnClickListener {
            val autor = textAutor.text.toString().replace("<", "").replace(">", "")
            QRManager.ShowQRDialog(
                requireContext(),
                getString(R.string.qr_share_frase_payload, textFrase.text, autor),
                "Compartir Frase",
                "Puede utilizar el lector QR para importar frases"
            )
        }
    }

    override fun onStart() {
        super.onStart()
        runCatching { MainActivity.currentInstance()?.ic_toolsBar_frase_add?.visibility = View.VISIBLE }
        runCatching { MainActivity.currentInstance()?.ic_toolsBar_fav?.visibility = View.GONE }
    }

    override fun onStop() {
        super.onStop()
        runCatching { MainActivity.currentInstance()?.ic_toolsBar_frase_add?.visibility = View.GONE }
        runCatching { MainActivity.currentInstance()?.ic_toolsBar_fav?.visibility = View.GONE }
        utilsFields.ID_row_ofElementLoad = -1
    }

    private fun initHome() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        var startMode = prefs.getString("list_start_load", "")
        if (startMode.isNullOrEmpty()) {
            prefs.edit { putString("list_start_load", "Frase_azar") }
            startMode = "Frase_azar"
        }

        when (startMode) {
            "Ultima_frase_vista" -> {
                val idUltimaFrase = prefs.getString(utilsFields.SETTING_KEY_ID_ULTIMA_FRASE, "0")
                val frase = utilsDB.getFraseById(requireContext(), idUltimaFrase?.toLongOrNull() ?: 0)
                if (frase != null) {
                    textFrase.text = frase.frase
                    textAutor.text = getString(R.string.autor_tag_format, frase.autor)
                    idFrase = frase.id
                    utilsFields.ID_row_ofElementLoad = frase.id.toInt()
                    utilsFields.ID_Str_row_ofElementLoad = frase.frase
                    setFavColor(frase.fav)
                }
            }

            "Frase_azar" -> loadFrases(false)
            "Frase_fav_azar" -> loadFrases(true)
            "Conf_azar" -> loadConfAzar(false)
            "Conf_fav_azar" -> loadConfAzar(true)
            "Ultima_conf_vista" -> {
                utilsFields.ID_Str_row_ofElementLoad =
                    prefs.getString(utilsFields.SETTING_KEY_ULTIMA_CONFERENCIA, "") ?: ""

                if (utilsFields.ID_Str_row_ofElementLoad.isNotEmpty()) {
                    FragContentWebView.extension = ".txt"
                    FragContentWebView.urlDirAssets = "autores/neville/conf"
                    val confFileName =
                        FragContentWebView.confAssetFileNameFromTitle(utilsFields.ID_Str_row_ofElementLoad)
                    FragContentWebView.urlPath =
                        "file:///android_asset/${FragContentWebView.urlDirAssets}/$confFileName${FragContentWebView.extension}"
                    navController.navigate(R.id.frag_content_webview)
                } else {
                    Toast.makeText(requireContext(), "Debe cargar al menos una conferencia en Texto", Toast.LENGTH_SHORT).show()
                    frag_listado.elementLoaded = "autores/neville/conf"
                    navController.navigate(R.id.frag_listado)
                }
            }
        }
    }

    private fun loadFrases(isFavList: Boolean) {
        val frase = utilsDB.getRandomFrase(requireContext(), isFavList)
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        if (frase != null) {
            textFrase.text = frase.frase
            textAutor.text = getString(R.string.autor_tag_format, frase.autor)

            idFrase = frase.id
            utilsFields.ID_row_ofElementLoad = frase.id.toInt()
            utilsFields.ID_Str_row_ofElementLoad = frase.frase

            prefs.edit { putString(utilsFields.SETTING_KEY_ID_ULTIMA_FRASE, frase.id.toString()) }
            setFavColor(frase.fav)
        } else {
            Toast.makeText(requireContext(), "No hay frase para mostrar. Cargando frases inbuilt", Toast.LENGTH_SHORT).show()
            prefs.edit { putString("list_start_load", "Frase_azar") }
        }
    }

    private fun loadConfAzar(isFav: Boolean) {
        val conf = utilsDB.getRandomConf(requireContext(), isFav)
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        if (conf != null) {
            utilsFields.ID_Str_row_ofElementLoad = conf.title
            FragContentWebView.extension = ".txt"
            FragContentWebView.urlDirAssets = "autores/neville/conf"
            val confFileName = FragContentWebView.confAssetFileNameFromTitle(conf.title)
            FragContentWebView.urlPath =
                "file:///android_asset/${FragContentWebView.urlDirAssets}/$confFileName${FragContentWebView.extension}"
            navController.navigate(R.id.frag_content_webview)
        } else {
            Toast.makeText(
                requireContext(),
                "No hay Conferencia favorita para mostrar. Cargando Conferencias inbuilt",
                Toast.LENGTH_SHORT
            ).show()
            prefs.edit { putString("list_start_load", "Conf_azar") }
        }
    }

    private fun setFavColor(favState: String) {
        if (favState == "1") {
            icFav.setColorFilter(requireContext().resources.getColor(R.color.fav_active, null))
            animate(icFav)
        } else {
            icFav.setColorFilter(requireContext().resources.getColor(R.color.fav_inactive, null))
        }
    }

    private fun animate(view: View) {
        val scaleDown = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat("scaleX", 1.3f),
            PropertyValuesHolder.ofFloat("scaleY", 1.3f)
        )
        scaleDown.duration = 300
        scaleDown.setAutoCancel(false)
        scaleDown.repeatCount = 3
        scaleDown.repeatMode = ObjectAnimator.REVERSE
        scaleDown.start()
    }

    @SuppressLint("SuspiciousIndentation")
    private fun showAyudaContextual() {
        val helpBalloon = HelpBalloon(requireContext())
        inlineIconsLayout.isInvisible = false

        val balloon1: Balloon = helpBalloon.buildFactory("Añadir un apunte", viewLifecycleOwner)
        val balloon2: Balloon = helpBalloon.buildFactory("Añadir una frase", viewLifecycleOwner)
        val balloon3: Balloon = helpBalloon.buildFactory("toque largo sobre frase para añadir una nota asociada", viewLifecycleOwner)
        val balloon4: Balloon = helpBalloon.buildFactory("Marca la frase como favorita", viewLifecycleOwner)
        val balloon5: Balloon = helpBalloon.buildFactory("Compartir la frase", viewLifecycleOwner)
        val balloon6: Balloon = helpBalloon.buildFactory("Mostrar/ocultar los iconos", viewLifecycleOwner)

        val main = MainActivity.currentInstance() ?: return

        balloon1
            .relayShowAlignBottom(balloon2, main.ic_toolsBar_frase_add)
            .relayShowAlignTop(balloon3, textFrase)
            .relayShowAlignBottom(balloon4, icFav)
            .relayShowAlignBottom(balloon5, icShared)
            .relayShowAlignTop(balloon6, masInfo)

        balloon1.showAlignBottom(main.ic_toolsBar_nota_add)
    }
}
