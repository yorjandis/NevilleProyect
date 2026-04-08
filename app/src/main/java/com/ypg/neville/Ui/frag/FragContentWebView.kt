package com.ypg.neville.ui.frag

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.ypg.neville.MainActivity
import com.ypg.neville.model.db.DatabaseHelper
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.model.utils.utilsFields
import com.ypg.neville.ui.render.BlockType
import com.ypg.neville.ui.render.ContentBlock
import com.ypg.neville.ui.render.DynamicTextRenderer
import com.ypg.neville.ui.render.RenderStyle
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale

class FragContentWebView : Fragment() {

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        visibilidadIconos()

        if (elementLoaded.contains("autores/neville/conf")) {
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .edit {
                    putString(utilsFields.SETTING_KEY_ULTIMA_CONFERENCIA, utilsFields.ID_Str_row_ofElementLoad)
                }
        }

        handlefavState()

        (view as ComposeView).setContent {
            com.ypg.neville.ui.theme.NevilleTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (shouldUseNativeRenderer(urlPath)) {
                            TxtContent(urlPath)
                        } else {
                            WebContent(urlPath)
                        }
                    }

                    AssistChip(
                        onClick = { navigateToPreviousScreen() },
                        label = { androidx.compose.material3.Text("Atrás") },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 12.dp)
                            .height(34.dp)
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        visibilidadIconos()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isPremiumPreviewMode = false
    }

    @Composable
    private fun WebContent(path: String) {
        val textZoom = if (elementLoaded.contains("biografia") || elementLoaded.contains("galeriafotos")) {
            80
        } else {
            PreferenceManager
                .getDefaultSharedPreferences(requireContext())
                .getString("fuente_conf", "170")
                ?.toIntOrNull() ?: 170
        }

        val webViewRef = remember { arrayOfNulls<WebView>(1) }

        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.setSupportZoom(true)
                    settings.textZoom = textZoom
                    loadUrl(path)
                    webViewRef[0] = this

                    if (flag_isPrimeraVez && elementLoaded.contains("autores/neville/conf") &&
                        PreferenceManager.getDefaultSharedPreferences(context)
                            .getString("list_start_load", "")
                            ?.contains("Ultima_conf_vista") == true
                    ) {
                        postDelayed({
                            val pos = PreferenceManager.getDefaultSharedPreferences(context)
                                .getString(utilsFields.SETTING_KEY_CONF_SCROLL_POSITION, "0")
                                ?.toIntOrNull() ?: 0
                            scrollTo(0, pos)
                            flag_isPrimeraVez = false
                        }, 300)
                    }
                }
            },
            update = {
                it.settings.textZoom = textZoom
                if (it.url != path) {
                    it.loadUrl(path)
                }
                webViewRef[0] = it
            }
        )

        DisposableEffect(Unit) {
            onDispose {
                val webView = webViewRef[0] ?: return@onDispose
                PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .edit {
                        putString(utilsFields.SETTING_KEY_CONF_SCROLL_POSITION, webView.scrollY.toString())
                    }
            }
        }
    }

    @Composable
    private fun TxtContent(path: String) {
        val requestedAssetPath = path
            .removePrefix("file:///android_asset/")
            .trimStart('/')
        val assetPath = resolveAssetPath(requestedAssetPath)

        val rawText = remember(assetPath) {
            runCatching {
                requireContext().assets.open(assetPath).bufferedReader().use { it.readText() }
            }.getOrElse {
                "No se pudo cargar el contenido: $assetPath"
            }
        }

        val blocks = remember(assetPath, rawText) {
            buildBlocksForNativeText(assetPath, rawText)
        }

        val fontZoom = PreferenceManager
            .getDefaultSharedPreferences(requireContext())
            .getString("fuente_conf", "170")
            ?.toIntOrNull() ?: 170

        val style = RenderStyle(
            fontSizeSp = ((fontZoom / 100f) * 14f).coerceIn(13f, 30f),
            textColor = MaterialTheme.colorScheme.onBackground
        )

        DynamicTextRenderer(blocks = blocks, style = style)
    }

    private fun shouldUseNativeRenderer(path: String): Boolean {
        return path.endsWith(".txt", ignoreCase = true)
    }

    private fun buildBlocksForNativeText(assetPath: String, rawText: String): List<ContentBlock> {
        if (isPremiumPreviewMode) {
            val fullContent = extractTextForPremiumPreview(assetPath, rawText)
                .replace("\\s+".toRegex(), " ")
                .trim()
            val preview = fullContent.take(350).trimEnd()
            val previewText = if (preview.isBlank()) {
                "..."
            } else {
                "$preview..."
            }
            return listOf(
                ContentBlock(content = BlockType.Text(previewText)),
                ContentBlock(content = BlockType.Text("")),
                ContentBlock(content = BlockType.Text("[Contenido disponible en la Versión extendida]"))
            )
        }

        val reflection = parseReflectionContent(assetPath, rawText)
        if (reflection != null) {
            val blocks = mutableListOf<ContentBlock>()
            blocks.add(ContentBlock(content = BlockType.Text(reflection.titulo)))
            blocks.add(ContentBlock(content = BlockType.Text("Autor: ${reflection.autor}")))
            reflection.contenido
                .filter { it.isNotBlank() }
                .forEach { paragraph ->
                    blocks.add(ContentBlock(content = BlockType.Text(paragraph.trim())))
                }
            return blocks
        }
        return listOf(ContentBlock(content = BlockType.Text(rawText)))
    }

    private fun extractTextForPremiumPreview(assetPath: String, rawText: String): String {
        val reflection = parseReflectionContent(assetPath, rawText)
        if (reflection != null) {
            return reflection.contenido.joinToString(separator = " ")
        }
        return rawText
    }

    private fun parseReflectionContent(assetPath: String, rawText: String): ReflectionContent? {
        val isReflectionFile = assetPath.startsWith("reflexiones/") &&
            assetPath.substringAfterLast('/').startsWith("reflex_")
        if (!isReflectionFile) return null

        return runCatching {
            val json = JSONObject(rawText)
            val titulo = json.optString("titulo").trim()
            val autor = json.optString("autor").trim()
            val contenidoArray = json.optJSONArray("contenido")

            val contenido = buildList {
                if (contenidoArray != null) {
                    for (i in 0 until contenidoArray.length()) {
                        add(contenidoArray.optString(i).trim())
                    }
                }
            }

            if (titulo.isBlank() && autor.isBlank() && contenido.isEmpty()) {
                null
            } else {
                ReflectionContent(
                    titulo = if (titulo.isBlank()) "Reflexión" else titulo,
                    autor = if (autor.isBlank()) "Desconocido" else autor,
                    contenido = contenido
                )
            }
        }.getOrNull()
    }

    private fun resolveAssetPath(path: String): String {
        if (assetExists(path)) return path

        // Resolver títulos de conferencias con variaciones de tildes/espacios/símbolos.
        val confResolved = resolveConfPathByNormalizedTitle(path)
        if (confResolved != null) return confResolved

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

    private fun resolveConfPathByNormalizedTitle(path: String): String? {
        val confPrefix = "autores/neville/conf/"
        if (!path.startsWith(confPrefix)) return null

        val requestedFile = path.removePrefix(confPrefix)
        val requestedBase = requestedFile.removeSuffix(".txt")
        val requestedNoPrefix = requestedBase.removePrefix("conf_")
        val requestedKey = normalizeForAssetMatch(requestedNoPrefix)

        val confFiles = requireContext().assets.list("autores/neville/conf").orEmpty()
            .filter { it.startsWith("conf_") && it.endsWith(".txt", ignoreCase = true) }

        val exact = confFiles.firstOrNull {
            it.removeSuffix(".txt").removePrefix("conf_").equals(requestedNoPrefix, ignoreCase = true)
        }
        if (exact != null) return "$confPrefix$exact"

        val normalized = confFiles.firstOrNull {
            val base = it.removeSuffix(".txt").removePrefix("conf_")
            normalizeForAssetMatch(base) == requestedKey
        }
        return normalized?.let { "$confPrefix$it" }
    }

    private fun normalizeForAssetMatch(raw: String): String {
        val noAccents = Normalizer.normalize(raw, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
        return noAccents
            .lowercase(Locale.ROOT)
            .replace("[^a-z0-9]+".toRegex(), "")
    }

    private data class ReflectionContent(
        val titulo: String,
        val autor: String,
        val contenido: List<String>
    )

    private fun visibilidadIconos() {
        if (elementLoaded.contains("autores/neville/conf")) {
            runCatching {
                MainActivity.currentInstance()?.icToolsBarFav?.visibility = View.VISIBLE
                MainActivity.currentInstance()?.icToolsBarFraseAdd?.visibility = View.VISIBLE
            }
        } else {
            runCatching {
                MainActivity.currentInstance()?.icToolsBarFav?.visibility = View.INVISIBLE
            }
        }
    }

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
        runCatching { MainActivity.currentInstance()?.setFavColor(favState) }
    }

    private fun navigateToPreviousScreen() {
        val navController = runCatching { findNavController() }.getOrNull()
        if (navController?.popBackStack() == true) return
        runCatching { requireActivity().onBackPressedDispatcher.onBackPressed() }
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

        @JvmField
        var isPremiumPreviewMode = false

        @JvmStatic
        fun confAssetFileNameFromTitle(title: String): String {
            val clean = title.removeSuffix(".txt")
            return if (clean.startsWith("conf_")) clean else "conf_$clean"
        }
    }
}
