package com.ypg.neville.ui.frag

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.db.DatabaseHelper
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.model.utils.utilsFields
import com.ypg.neville.ui.render.BlockType
import com.ypg.neville.ui.render.ContentBlock
import com.ypg.neville.ui.render.DynamicTextRenderer
import com.ypg.neville.ui.render.RenderStyle
import java.util.Timer
import java.util.TimerTask

class FragContentWebView : Fragment() {

    private var webView: WebView? = null
    private var composeView: ComposeView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.frag_content, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        visibilidadIconos() // maneja la visibilidad de los iconos en la toolbar

        webView = view.findViewById(R.id.frag_content_webview)
        composeView = view.findViewById(R.id.frag_content_compose)

        if (shouldUseNativeRenderer(urlPath)) {
            renderTxtWithCompose(urlPath)
        } else {
            renderUrlInWebView(urlPath)
        }

        // Almacenando el path de la ultima conferencia cargada
        if (elementLoaded.contains("autores/neville/conf")) {
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .edit()
                .putString(utilsFields.SETTING_KEY_ULTIMA_CONFERENCIA, utilsFields.ID_Str_row_ofElementLoad)
                .apply()
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
        // Almacenando la posición del scroll solo cuando el contenido se muestra en WebView
        if (webView?.visibility == View.VISIBLE) {
            try {
                PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .edit()
                    .putString(utilsFields.SETTING_KEY_CONF_SCROLL_POSITION, webView?.scrollY.toString())
                    .apply()
            } catch (ignored: Exception) {
            }
        }
    }

    private fun renderUrlInWebView(path: String) {
        composeView?.visibility = View.GONE
        webView?.visibility = View.VISIBLE

        webView?.webViewClient = WebViewClient()
        webView?.settings?.setSupportZoom(true)

        // Ajustando el tamaño de fuente adecuadamente, segun sea texto o html
        if (elementLoaded.contains("biografia") || elementLoaded.contains("galeriafotos")) {
            webView?.settings?.textZoom = 80
        } else {
            webView?.settings?.textZoom = PreferenceManager
                .getDefaultSharedPreferences(requireContext())
                .getString("fuente_conf", "170")
                ?.toInt() ?: 170
        }

        webView?.loadUrl(path)

        // Restablece la posición de la barra de desplazamiento
        if (flag_isPrimeraVez && elementLoaded.contains("autores/neville/conf") &&
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString("list_start_load", "")
                ?.contains("Ultima_conf_vista") == true
        ) {
            val t = Timer(false)
            t.schedule(object : TimerTask() {
                override fun run() {
                    activity?.runOnUiThread {
                        val i = PreferenceManager.getDefaultSharedPreferences(requireContext())
                            .getString(utilsFields.SETTING_KEY_CONF_SCROLL_POSITION, "0")
                            ?.toInt() ?: 0
                        webView?.scrollY = i
                        flag_isPrimeraVez = false
                    }
                }
            }, 300)
        }
    }

    private fun renderTxtWithCompose(path: String) {
        webView?.visibility = View.GONE
        composeView?.visibility = View.VISIBLE

        val requestedAssetPath = path
            .removePrefix("file:///android_asset/")
            .trimStart('/')
        val assetPath = resolveAssetPath(requestedAssetPath)

        val rawText = runCatching {
            requireContext().assets.open(assetPath).bufferedReader().use { it.readText() }
        }.getOrElse {
            "No se pudo cargar el contenido: $assetPath"
        }

        val blocks = listOf(ContentBlock(content = BlockType.Text(rawText)))

        val fontZoom = PreferenceManager
            .getDefaultSharedPreferences(requireContext())
            .getString("fuente_conf", "170")
            ?.toIntOrNull() ?: 170

        val style = RenderStyle(
            fontSizeSp = ((fontZoom / 100f) * 14f).coerceIn(13f, 30f),
            textColor = Color.Black
        )

        composeView?.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        composeView?.setContent {
            MaterialTheme {
                DynamicTextRenderer(
                    blocks = blocks,
                    style = style
                )
            }
        }
    }

    private fun shouldUseNativeRenderer(path: String): Boolean {
        return path.endsWith(".txt", ignoreCase = true)
    }

    private fun resolveAssetPath(path: String): String {
        if (assetExists(path)) return path

        val legacyCandidates = listOf(
            path.replaceFirst("conf/", "autores/neville/conf/"),
            path.replaceFirst("preg/", "autores/neville/preg/"),
            path.replaceFirst("cita/", "autores/neville/cita/"),
            path.replaceFirst("ayuda/", "ayuda/")
        )

        return legacyCandidates.firstOrNull { assetExists(it) } ?: path
    }

    private fun assetExists(path: String): Boolean {
        return runCatching {
            requireContext().assets.open(path).close()
            true
        }.getOrDefault(false)
    }

    // Controla la visibilidad de los iconos
    private fun visibilidadIconos() {
        if (elementLoaded.contains("autores/neville/conf")) {
            try {
                MainActivity.currentInstance()?.ic_toolsBar_fav?.visibility = View.VISIBLE
                MainActivity.currentInstance()?.ic_toolsBar_frase_add?.visibility = View.VISIBLE
            } catch (ignored: Exception) {
            }
        } else {
            try {
                MainActivity.currentInstance()?.ic_toolsBar_fav?.visibility = View.INVISIBLE
            } catch (ignored: Exception) {
            }
        }
    }

    // lee y actualiza el estado de favorito de un elemento cargado
    private fun handlefavState() {
        var favState = ""
        if (elementLoaded == "autores/neville/conf") {
            favState = utilsDB.readFavState(
                requireContext(),
                DatabaseHelper.T_Conf,
                DatabaseHelper.C_conf_title,
                utilsFields.ID_Str_row_ofElementLoad
            )
        }
        try {
            MainActivity.currentInstance()?.setFavColor(favState)
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

        @JvmStatic
        fun confAssetFileNameFromTitle(title: String): String {
            val clean = title.removeSuffix(".txt")
            return if (clean.startsWith("conf_")) clean else "conf_$clean"
        }
    }
}
