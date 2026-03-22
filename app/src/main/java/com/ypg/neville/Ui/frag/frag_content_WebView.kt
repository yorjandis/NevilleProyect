package com.ypg.neville.Ui.frag

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.db.DatabaseHelper
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.model.utils.utilsFields
import java.util.Timer
import java.util.TimerTask

class frag_content_WebView : Fragment() {

    private var webView: WebView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.frag_content, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        visibilidadIconos() // maneja la visibilidad de los iconos en la toolbar

        webView = view.findViewById(R.id.frag_content_webview)
        webView?.webViewClient = WebViewClient()
        webView?.settings?.setSupportZoom(true)

        // Ajustando el tamaño de fuente adecuadamente, segun sea texto o html
        if (elementLoaded.contains("biografia") || elementLoaded.contains("galeriafotos")) {
            webView?.settings?.textZoom = 80
        } else {
            webView?.settings?.textZoom = PreferenceManager.getDefaultSharedPreferences(requireContext()).getString("fuente_conf", "170")?.toInt() ?: 170
        }

        webView?.visibility = View.VISIBLE
        webView?.loadUrl(urlPath)

        // Restablece la posición de la barra de desplazamiento
        if (flag_isPrimeraVez && elementLoaded.contains("conf") &&
            PreferenceManager.getDefaultSharedPreferences(requireContext()).getString("list_start_load", "")?.contains("Ultima_conf_vista") == true) {

            val t = Timer(false)
            t.schedule(object : TimerTask() {
                override fun run() {
                    activity?.runOnUiThread {
                        val i = PreferenceManager.getDefaultSharedPreferences(requireContext()).getString(utilsFields.SETTING_KEY_CONF_SCROLL_POSITION, "0")?.toInt() ?: 0
                        webView?.scrollY = i
                        flag_isPrimeraVez = false
                    }
                }
            }, 300) // 300 ms delay before scrolling
        }

        // Almacenando el path de la ultima conferencia cargada
        if (elementLoaded.contains("conf")) {
            PreferenceManager.getDefaultSharedPreferences(requireContext()).edit().putString(utilsFields.SETTING_KEY_ULTIMA_CONFERENCIA, utilsFields.ID_Str_row_ofElementLoad).apply()
        }

        // Comprobando y cargando el estado de favorito para el elemento cargado
        handlefavState()
    }

    override fun onStart() {
        super.onStart()
        visibilidadIconos()
    }

    override fun onStop() {
        super.onStop()
        // Almacenando la posición del scroll
        try {
            PreferenceManager.getDefaultSharedPreferences(requireContext()).edit().putString(utilsFields.SETTING_KEY_CONF_SCROLL_POSITION, webView?.scrollY.toString()).apply()
        } catch (ignored: Exception) {
        }
    }

    // Controla la visibilidad de los iconos
    private fun visibilidadIconos() {
        if (elementLoaded.contains("conf")) {
            try {
                MainActivity.mainActivityThis?.ic_toolsBar_fav?.visibility = View.VISIBLE
                MainActivity.mainActivityThis?.ic_toolsBar_frase_add?.visibility = View.VISIBLE
            } catch (ignored: Exception) {
            }
        } else {
            try {
                MainActivity.mainActivityThis?.ic_toolsBar_fav?.visibility = View.INVISIBLE
            } catch (ignored: Exception) {
            }
        }
    }

    // lee y actualiza el estado de favorito de un elemento cargado
    private fun handlefavState() {
        var favState = ""
        if (elementLoaded == "conf") {
            favState = utilsDB.readFavState(requireContext(), DatabaseHelper.T_Conf, DatabaseHelper.C_conf_title, utilsFields.ID_Str_row_ofElementLoad)
        }
        try {
            MainActivity.mainActivityThis?.setFavColor(favState)
        } catch (ignored: Exception) {
        }
    }

    companion object {
        @JvmField
        var urlPath = ""
        @JvmField
        var extension = ""
        @JvmField
        var urlDirAssets = ""
        @JvmField
        var flag_isPrimeraVez = true
        @JvmField
        var elementLoaded = ""
    }
}
