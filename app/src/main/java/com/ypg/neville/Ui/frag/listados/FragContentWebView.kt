package com.ypg.neville.ui.frag

import android.R
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ypg.neville.model.preferences.DbPreferences
import com.ypg.neville.MainActivity
import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryEventLogger
import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryEventType
import com.ypg.neville.model.db.DatabaseHelper
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.model.utils.FraseContextActions
import com.ypg.neville.model.utils.UiModalWindows
import com.ypg.neville.model.utils.utilsFields
import com.ypg.neville.model.subscription.SubscriptionManager
import com.ypg.neville.ui.render.BlockType
import com.ypg.neville.ui.render.ContentBlock
import com.ypg.neville.ui.render.DynamicTextRenderer
import com.ypg.neville.ui.render.RenderStyle
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.flow.distinctUntilChanged

class FragContentWebView : Fragment() {
    private var clipboardManager: ClipboardManager? = null
    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener? = null
    private var currentContentForCopyDetection: String = ""
    private var copiedTextFromContent by mutableStateOf<String?>(null)
    private var showPasteMenu by mutableStateOf(false)

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
            DbPreferences.default(requireContext())
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
                        label = { Text("Atrás", color = Color.Black) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFFE3E8EF).copy(alpha = 0.96f)
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 12.dp)
                            .height(34.dp)

                    )

                    val copiedText = copiedTextFromContent
                    if (!copiedText.isNullOrBlank()) {
                        val hasPremium = SubscriptionManager.hasActiveSubscription(requireContext())
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 12.dp, top = 12.dp)
                        ) {
                            AssistChip(
                                onClick = {
                                    if (hasPremium) {
                                        showPasteMenu = true
                                    } else {
                                        MainActivity.currentInstance()?.showSubscriptionPaywall()
                                    }
                                },
                                label = {
                                    Text(
                                        if (hasPremium) "Pegar en" else "Pegar en (Premium)",
                                        color = Color.Black
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color(0xFFE3E8EF).copy(alpha = 0.96f)
                                ),
                                modifier = Modifier.height(34.dp)
                            )

                            DropdownMenu(
                                expanded = hasPremium && showPasteMenu,
                                onDismissRequest = { showPasteMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Pegar en Notas") },
                                    onClick = {
                                        showPasteMenu = false
                                        val result = FraseContextActions.convertirFraseEnNota(requireContext(), copiedText)
                                        if (result.ok) {
                                            Toast.makeText(requireContext(), "Nota creada: ${result.titulo}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(requireContext(), "No se pudo crear la nota", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Pegar en Lienzo") },
                                    onClick = {
                                        showPasteMenu = false
                                        FraseContextActions.cargarFraseEnLienzo(requireContext(), copiedText)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Convertir en frase personal") },
                                    onClick = {
                                        showPasteMenu = false
                                        val values = ContentValues().apply {
                                            put("frase", copiedText)
                                            put("autor", "")
                                            put("fuente", "")
                                        }
                                        UiModalWindows.Add_New_frase(requireContext(), values)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        visibilidadIconos()
        clipboardManager = requireContext().getSystemService(ClipboardManager::class.java)
        val listener = ClipboardManager.OnPrimaryClipChangedListener {
            updateCopiedTextState()
        }
        clipboardListener = listener
        clipboardManager?.addPrimaryClipChangedListener(listener)
        updateCopiedTextState()
    }

    override fun onStop() {
        clipboardListener?.let { listener ->
            clipboardManager?.removePrimaryClipChangedListener(listener)
        }
        clipboardListener = null
        clipboardManager = null
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isPremiumPreviewMode = false
        currentContentForCopyDetection = ""
        copiedTextFromContent = null
        showPasteMenu = false
    }

    @Composable
    private fun WebContent(path: String) {
        val textZoom = if (elementLoaded.contains("biografia") || elementLoaded.contains("galeriafotos")) {
            80
        } else {
            DbPreferences
                .default(requireContext())
                .getString("fuente_conf", "170")
                ?.toIntOrNull() ?: 170
        }

        val webViewRef = remember { arrayOfNulls<WebView>(1) }
        var conferenceReadLogged by remember(path) { mutableStateOf(false) }

        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.setSupportZoom(true)
                    settings.textZoom = textZoom
                    loadUrl(path)
                    webViewRef[0] = this
                    setOnScrollChangeListener { v, _, scrollY, _, _ ->
                        if (!conferenceReadLogged && elementLoaded.contains("autores/neville/conf")) {
                            val web = v as? WebView ?: return@setOnScrollChangeListener
                            val fullHeight = (web.contentHeight * web.scale).toInt()
                            val maxScroll = (fullHeight - web.height).coerceAtLeast(1)
                            val progress = scrollY.toFloat() / maxScroll.toFloat()
                            if (progress >= 0.33f) {
                                WeeklySummaryEventLogger.log(
                                    WeeklySummaryEventType.CONFERENCE_READ,
                                    targetKey = utilsFields.ID_Str_row_ofElementLoad.ifBlank { path }
                                )
                                conferenceReadLogged = true
                            }
                        }
                    }

                    if (flag_isPrimeraVez && elementLoaded.contains("autores/neville/conf") &&
                        DbPreferences.default(context)
                            .getString("list_start_load", "")
                            ?.contains("Ultima_conf_vista") == true
                    ) {
                        postDelayed({
                            val pos = DbPreferences.default(context)
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
                DbPreferences.default(requireContext())
                    .edit {
                        putString(utilsFields.SETTING_KEY_CONF_SCROLL_POSITION, webView.scrollY.toString())
                    }
            }
        }
    }

    @Composable
    private fun TxtContent(path: String) {
        val prefs = DbPreferences.default(requireContext())
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

        val contentForCopyDetection = remember(assetPath, rawText) {
            extractTextForPremiumPreview(assetPath, rawText)
        }
        LaunchedEffect(assetPath, contentForCopyDetection) {
            currentContentForCopyDetection = contentForCopyDetection
            updateCopiedTextState()
        }

        val listState = rememberLazyListState()
        var conferenceReadLogged by remember(assetPath) { mutableStateOf(false) }

        val blocks = remember(assetPath, rawText) {
            buildBlocksForNativeText(assetPath, rawText)
        }

        LaunchedEffect(assetPath, listState, blocks.size) {
            if (!elementLoaded.contains("autores/neville/conf")) return@LaunchedEffect
            snapshotFlow {
                val total = listState.layoutInfo.totalItemsCount
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                if (total <= 0) 0f else (lastVisible + 1).toFloat() / total.toFloat()
            }
                .distinctUntilChanged()
                .collect { fraction ->
                    if (!conferenceReadLogged && fraction >= 0.33f) {
                        WeeklySummaryEventLogger.log(
                            WeeklySummaryEventType.CONFERENCE_READ,
                            targetKey = utilsFields.ID_Str_row_ofElementLoad.ifBlank { assetPath }
                        )
                        conferenceReadLogged = true
                    }
                }
        }

        val fontZoom = DbPreferences
            .default(requireContext())
            .getString("fuente_conf", "170")
            ?.toIntOrNull() ?: 170
        val textColor = Color(
            prefs.getInt("color_lectura_texto", 0xFF2B2115.toInt())
        )
        val backgroundA = Color(
            prefs.getInt("color_lectura_fondo_a", 0xFFF8F4EA.toInt())
        )
        val backgroundB = Color(
            prefs.getInt("color_lectura_fondo_b", 0xFFECE3D3.toInt())
        )

        val style = RenderStyle(
            fontSizeSp = ((fontZoom / 100f) * 14f).coerceIn(13f, 30f),
            textColor = textColor
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(backgroundA, backgroundB)))
        ) {
            DynamicTextRenderer(
                blocks = blocks,
                style = style,
                listState = listState,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    private fun shouldUseNativeRenderer(path: String): Boolean {
        return path.endsWith(".txt", ignoreCase = true)
    }

    private fun buildBlocksForNativeText(assetPath: String, rawText: String): List<ContentBlock> {
        if (isPremiumPreviewMode) {
            val fullContent = extractTextForPremiumPreview(assetPath, rawText)
                .replace("\r\n", "\n")
                .replace('\r', '\n')
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
            return reflection.contenido.joinToString(separator = "\n\n")
        }
        return rawText
    }

    private fun parseReflectionContent(assetPath: String, rawText: String): ReflectionContent? {
        val fileName = assetPath.substringAfterLast('/')
        val isStructuredResourceFile =
            (assetPath.startsWith("reflexiones/") && fileName.startsWith("reflex_")) ||
                (assetPath.startsWith("ayudas/") && fileName.startsWith("ayud_"))
        if (!isStructuredResourceFile) return null

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
                    titulo = if (titulo.isBlank()) "Recurso" else titulo,
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
            path.replaceFirst("ayuda/", "ayudas/"),
            path.replaceFirst("ayudas/", "ayudas/")
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

    private fun updateCopiedTextState() {
        val clipText = clipboardManager
            ?.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(requireContext())
            ?.toString()
            ?.trim()
            .orEmpty()

        if (clipText.isBlank() || currentContentForCopyDetection.isBlank()) {
            copiedTextFromContent = null
            showPasteMenu = false
            return
        }

        val normalizedContent = normalizeCopyText(currentContentForCopyDetection)
        val normalizedClip = normalizeCopyText(clipText)
        val isFromCurrentContent = normalizedClip.length >= 4 && normalizedContent.contains(normalizedClip)

        copiedTextFromContent = if (isFromCurrentContent) clipText else null
        if (!isFromCurrentContent) {
            showPasteMenu = false
        }
    }

    private fun normalizeCopyText(text: String): String {
        return text
            .replace("\\s+".toRegex(), " ")
            .trim()
            .lowercase(Locale.ROOT)
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
