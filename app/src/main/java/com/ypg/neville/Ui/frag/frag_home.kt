package com.ypg.neville.Ui.frag

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.content.Context
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
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import com.skydoves.balloon.Balloon
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.db.DBManager
import com.ypg.neville.model.db.DatabaseHelper
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.model.utils.QRManager
import com.ypg.neville.model.utils.UiModalWindows
import com.ypg.neville.model.utils.balloon.HelpBalloon
import com.ypg.neville.model.utils.utilsFields
import java.util.Objects
import java.util.Random

class frag_home : Fragment() {

    private lateinit var text_frase: TextView
    private lateinit var textAutor: TextView
    private lateinit var ic_fav: AppCompatImageView
    private lateinit var ic_shared: AppCompatImageView
    private var id_frase: Long = 0 // Almacena el id de la frase actual
    private lateinit var navController: NavController
    private lateinit var linearLayout_IconosInlineFrases: LinearLayout
    private lateinit var ayudaContextual: ImageButton
    private lateinit var masinfo: ImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.frag_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        text_frase = view.findViewById(R.id.frag_home_text)
        textAutor = view.findViewById(R.id.frag_home_textautor)
        ic_fav = view.findViewById(R.id.ic_frase_fav)
        ic_shared = view.findViewById(R.id.ic_frase_shared)
        linearLayout_IconosInlineFrases = view.findViewById(R.id.layout_fraghome_icons_inlines_frase)
        ayudaContextual = view.findViewById(R.id.frag_home_ayuda)
        masinfo = view.findViewById(R.id.frag_home_frases_img_abajo)

        navController = Navigation.findNavController(view)
        elementLoaded_home = "frases"

        // Setting: Mostrando/Ocultando la Ayuda contextual
        if (PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean("help_inline", true)) {
            ayudaContextual.visibility = View.VISIBLE
        } else {
            ayudaContextual.visibility = View.GONE
        }
        ayudaContextual.setOnClickListener {
            ShowHelpCount = 0
            ShowAyudaContextual(requireContext())
        }

        // Setting: Aplicando tamaño de fuente a frases
        text_frase.textSize = PreferenceManager.getDefaultSharedPreferences(requireContext()).getString("fuente_frase", "28")?.toFloat() ?: 28f

        // Setting: Aplicando el color de fuente de las frases
        val temp_color = PreferenceManager.getDefaultSharedPreferences(requireContext()).getInt("color_letra_frases", 0)
        if (temp_color != 0) {
            text_frase.setTextColor(temp_color)
        }

        // Setting: Chequeando si se muestran/ocultan los iconos inlines en las frases
        if (PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean("hide_frase_controles", false)) {
            linearLayout_IconosInlineFrases.visibility = View.INVISIBLE
            masinfo.setImageResource(R.drawable.ic_abajo)
        } else {
            linearLayout_IconosInlineFrases.visibility = View.VISIBLE
            masinfo.setImageResource(R.drawable.ic_arriba)
        }

        // Setting: determinando si el fragment se ha iniciado la primera vez
        if (isPrimeracarga) {
            Init()
            isPrimeracarga = false // resetea el valor a false
        } else {
            Loadfrases(false)
        }

        // expandir o colapsar la información de la frase
        masinfo.setOnClickListener {
            if (linearLayout_IconosInlineFrases.visibility == View.INVISIBLE) {
                linearLayout_IconosInlineFrases.visibility = View.VISIBLE
                masinfo.setImageResource(R.drawable.ic_arriba)
                PreferenceManager.getDefaultSharedPreferences(requireContext()).edit().putBoolean("hide_frase_controles", false).apply()
            } else {
                linearLayout_IconosInlineFrases.visibility = View.INVISIBLE
                masinfo.setImageResource(R.drawable.ic_abajo)
                PreferenceManager.getDefaultSharedPreferences(requireContext()).edit().putBoolean("hide_frase_controles", true).apply()
            }
        }

        // Onclick del texto en frases: Cambiar el texto de la frase
        text_frase.setOnClickListener {
            if (PreferenceManager.getDefaultSharedPreferences(requireContext()).getString("list_start_load", "Nada")?.contains("Frase_fav_azar") == true) {
                Loadfrases(true) // Carga una frase favorita al azar
            } else {
                Loadfrases(false)
            }
        }

        // onLong click sobre el texto para abrir el cuadro de adicionar una nota asociada
        text_frase.setOnLongClickListener {
            val dbManager = DBManager(requireContext()).open()
            val query = "SELECT nota FROM ${DatabaseHelper.T_Frases} WHERE ${DatabaseHelper.C_frases_frase}='${text_frase.text}';"
            val cursor = dbManager.ejectSQLRawQuery(query)

            if (cursor.moveToFirst()) {
                UiModalWindows.NotaManager(requireContext(), cursor.getString(0), DatabaseHelper.T_Frases, DatabaseHelper.C_frases_frase, text_frase.text.toString())
            }
            cursor.close()
            dbManager.close()
            true
        }

        ic_fav.setOnClickListener {
            val result = utilsDB.UpdateFavorito(requireContext(), DatabaseHelper.T_Frases, DatabaseHelper.CC_id, "", id_frase.toInt())
            if (result != "") {
                setFavColor(result)
            }
        }

        // Comparte el texto de una frase
        ic_shared.setOnClickListener {
            var autor = textAutor.text.toString().replace("<", "")
            autor = autor.replace(">", "")

            QRManager.ShowQRDialog(requireContext(), "f::${text_frase.text}::$autor:: ",
                "Compartir Frase", "Puede utilizar el lector QR para importar frases")
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            MainActivity.mainActivityThis?.ic_toolsBar_frase_add?.visibility = View.VISIBLE
        } catch (ignored: Exception) {
        }
        try {
            MainActivity.mainActivityThis?.ic_toolsBar_fav?.visibility = View.GONE
        } catch (ignored: Exception) {
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            MainActivity.mainActivityThis?.ic_toolsBar_frase_add?.visibility = View.GONE
        } catch (ignored: Exception) {
        }
        try {
            MainActivity.mainActivityThis?.ic_toolsBar_fav?.visibility = View.GONE
        } catch (ignored: Exception) {
        }
        // resetear las variables antes de abandonar el fragment
        utilsFields.ID_row_ofElementLoad = -1
    }

    private fun Init() {
        var temp = PreferenceManager.getDefaultSharedPreferences(requireContext()).getString("list_start_load", "")
        if (temp.isNullOrEmpty()) {
            PreferenceManager.getDefaultSharedPreferences(requireContext()).edit().putString("list_start_load", "Frase_azar").apply()
            temp = "Frase_azar"
        }

        when (temp) {
            "Ultima_frase_vista" -> {
                val idUltimaFrase = PreferenceManager.getDefaultSharedPreferences(requireContext()).getString(utilsFields.SETTING_KEY_ID_ULTIMA_FRASE, "0")
                val sql = "SELECT * FROM ${DatabaseHelper.T_Frases} WHERE ${DatabaseHelper.CC_id}=${idUltimaFrase?.toInt() ?: 0}"
                val dbManager = DBManager(requireContext()).open()
                val cursor = dbManager.ejectSQLRawQuery(sql)

                if (cursor.moveToFirst()) {
                    text_frase.text = cursor.getString(1)
                    textAutor.text = "<${cursor.getString(2)}>"
                    id_frase = cursor.getInt(0).toLong()
                    utilsFields.ID_row_ofElementLoad = cursor.getInt(0)
                    utilsFields.ID_Str_row_ofElementLoad = cursor.getString(1)
                    setFavColor(cursor.getString(4))
                }
                cursor.close()
                dbManager.close()
            }
            "Frase_azar" -> Loadfrases(false)
            "Frase_fav_azar" -> Loadfrases(true)
            "Conf_azar" -> LoadConfAzar(false)
            "Conf_fav_azar" -> LoadConfAzar(true)
            "Ultima_conf_vista" -> {
                utilsFields.ID_Str_row_ofElementLoad = PreferenceManager.getDefaultSharedPreferences(requireContext()).getString(utilsFields.SETTING_KEY_ULTIMA_CONFERENCIA, "") ?: ""
                if (utilsFields.ID_Str_row_ofElementLoad.isNotEmpty()) {
                    frag_content_WebView.extension = ".txt"
                    frag_content_WebView.urlDirAssets = "conf"
                    frag_content_WebView.urlPath = "file:///android_asset/${frag_content_WebView.urlDirAssets}/${utilsFields.ID_Str_row_ofElementLoad}${frag_content_WebView.extension}"
                    navController.navigate(R.id.frag_content_webview)
                } else {
                    Toast.makeText(requireContext(), "Debe cargar al menos una conferencia en Texto", Toast.LENGTH_SHORT).show()
                    frag_listado.elementLoaded = "conf"
                    navController.navigate(R.id.frag_listado)
                }
            }
        }
    }

    private fun Loadfrases(isfavList: Boolean) {
        val dbManager = DBManager(requireContext()).open()
        val cursor = if (isfavList) {
            dbManager.getListado("Frases favoritas")
        } else {
            dbManager.getListado("Todas las frases")
        }

        if (cursor.moveToFirst()) {
            var randomNumber = 0
            val random = Random()
            if (cursor.count > 1) {
                randomNumber = random.nextInt(cursor.count)
            }

            cursor.move(randomNumber)
            text_frase.text = cursor.getString(1)
            textAutor.text = "<${cursor.getString(2)}>"

            id_frase = cursor.getInt(0).toLong()
            utilsFields.ID_row_ofElementLoad = cursor.getInt(0)
            utilsFields.ID_Str_row_ofElementLoad = cursor.getString(1)

            PreferenceManager.getDefaultSharedPreferences(requireContext()).edit().putString(utilsFields.SETTING_KEY_ID_ULTIMA_FRASE, cursor.getInt(0).toString()).apply()
            setFavColor(cursor.getString(4))
            cursor.close()
        } else {
            Toast.makeText(requireContext(), "No hay frase para mostrar. Cargando frases inbuilt", Toast.LENGTH_SHORT).show()
            PreferenceManager.getDefaultSharedPreferences(requireContext()).edit().putString("list_start_load", "Frase_azar").apply()
        }
        dbManager.close()
    }

    private fun LoadConfAzar(isfav: Boolean) {
        val dbManager = DBManager(requireContext()).open()
        val cursor = if (isfav) {
            dbManager.getListado("Conferencias favoritas")
        } else {
            dbManager.getListado("Todas las conf")
        }

        if (cursor.moveToFirst()) {
            var randomNumber = 0
            val random = Random()
            if (cursor.count > 1) {
                randomNumber = random.nextInt(cursor.count)
            }

            cursor.move(randomNumber)

            utilsFields.ID_Str_row_ofElementLoad = cursor.getString(1)
            frag_content_WebView.extension = ".txt"
            frag_content_WebView.urlDirAssets = "conf"

            frag_content_WebView.urlPath = "file:///android_asset/${frag_content_WebView.urlDirAssets}/${cursor.getString(1)}${frag_content_WebView.extension}"
            navController.navigate(R.id.frag_content_webview)
        } else {
            Toast.makeText(requireContext(), "No hay Conferencia favorita para mostrar. Cargando Conferencias inbuilt", Toast.LENGTH_SHORT).show()
            PreferenceManager.getDefaultSharedPreferences(requireContext()).edit().putString("list_start_load", "Conf_azar").apply()
        }
        cursor.close()
        dbManager.close()
    }

    private fun setFavColor(fav_state: String) {
        if (fav_state == "1") {
            ic_fav.setColorFilter(requireContext().resources.getColor(R.color.fav_active, null))
            animate(ic_fav)
        } else {
            ic_fav.setColorFilter(requireContext().resources.getColor(R.color.fav_inactive, null))
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

    @SuppressLint("SuspiciousIndentation")
    private fun ShowAyudaContextual(context: Context) {
        val helpBalloon = HelpBalloon(requireContext())
        linearLayout_IconosInlineFrases.visibility = View.VISIBLE
        val balloon1: Balloon
        val balloon2: Balloon
        val balloon3: Balloon
        val balloon4: Balloon
        val balloon5: Balloon
        val balloon6: Balloon

        balloon1 = helpBalloon.buildFactory("Añadir un apunte", viewLifecycleOwner)
        balloon2 = helpBalloon.buildFactory("Añadir una frase", viewLifecycleOwner)
        balloon3 = helpBalloon.buildFactory("toque largo sobre frase para añadir una nota asociada", viewLifecycleOwner)
        balloon4 = helpBalloon.buildFactory("Marca la frase como favorita", viewLifecycleOwner)
        balloon5 = helpBalloon.buildFactory("Compartir la frase", viewLifecycleOwner)
        balloon6 = helpBalloon.buildFactory("Mostrar/ocultar los iconos", viewLifecycleOwner)

        balloon1
            .relayShowAlignBottom(balloon2, MainActivity.mainActivityThis!!.ic_toolsBar_frase_add)
            .relayShowAlignTop(balloon3, text_frase)
            .relayShowAlignBottom(balloon4, ic_fav)
            .relayShowAlignBottom(balloon5, ic_shared)
            .relayShowAlignTop(balloon6, masinfo)

        balloon1.showAlignBottom(MainActivity.mainActivityThis!!.ic_toolsBar_nota_add)
    }

    companion object {
        @JvmField
        var isPrimeracarga = true
        @JvmField
        var frag_home_this: frag_home? = null
        @JvmField
        var elementLoaded_home = ""
        @JvmField
        var ShowHelpCount = 0
    }
}
