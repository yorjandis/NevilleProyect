package com.ypg.neville.ui.frag

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.db.DatabaseHelper
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.model.utils.FraseContextActions
import com.ypg.neville.model.utils.UiModalWindows
import com.ypg.neville.model.utils.utilsFields

class FragHome : Fragment() {

    private var initialDisplay: HomeDisplay? = null

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initialDisplay = loadInitialState()

        (view as ComposeView).setContent {
            com.ypg.neville.ui.theme.NevilleTheme {
                HomeScreen(initial = initialDisplay)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        runCatching { MainActivity.currentInstance()?.icToolsBarFraseAdd?.visibility = View.VISIBLE }
        runCatching { MainActivity.currentInstance()?.icToolsBarFav?.visibility = View.GONE }
    }

    override fun onStop() {
        super.onStop()
        runCatching { MainActivity.currentInstance()?.icToolsBarFraseAdd?.visibility = View.GONE }
        runCatching { MainActivity.currentInstance()?.icToolsBarFav?.visibility = View.GONE }
        utilsFields.ID_row_ofElementLoad = -1
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun HomeScreen(initial: HomeDisplay?) {
        val context = LocalContext.current
        val activityContext = remember { this@FragHome.requireActivity() }
        val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

        var frase by remember(initial) { mutableStateOf(initial?.frase.orEmpty()) }
        var autor by remember(initial) { mutableStateOf(initial?.autor.orEmpty()) }
        var fuente by remember(initial) { mutableStateOf(initial?.fuente.orEmpty()) }
        var favState by remember(initial) { mutableStateOf(initial?.fav ?: "0") }
        var idFrase by remember(initial) { mutableLongStateOf(initial?.id ?: 0L) }
        var hideInlineControls by remember { mutableStateOf(prefs.getBoolean("hide_frase_controles", false)) }
        var showFraseMenu by remember { mutableStateOf(false) }

        val textSize = (prefs.getString("fuente_frase", "28")?.toFloatOrNull() ?: 28f).coerceIn(16f, 40f)
        val textColor = prefs.getInt("color_letra_frases_home", prefs.getInt("color_letra_frases", 0))
        val bgColorA = prefs.getInt("color_fondo_a", 0xFFF3F5F9.toInt())
        val bgColorB = prefs.getInt("color_fondo_b", 0xFFE2E7F0.toInt())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(bgColorA), Color(bgColorB))
                    )
                )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val noRipple = remember { MutableInteractionSource() }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = frase,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp)
                                .heightIn(max = 420.dp)
                                .verticalScroll(rememberScrollState())
                                .combinedClickable(
                                    interactionSource = noRipple,
                                    indication = null,
                                    onClick = {
                                        val startMode = prefs.getString("list_start_load", "Nada") ?: "Nada"
                                        val loaded = loadFrase(startMode.contains("Frase_fav_azar"))
                                        if (loaded != null) {
                                            frase = loaded.frase
                                            autor = loaded.autor
                                            fuente = loaded.fuente
                                            favState = loaded.fav
                                            idFrase = loaded.id
                                        }
                                    },
                                    onLongClick = {
                                        if (frase.isNotBlank()) {
                                            showFraseMenu = true
                                        }
                                    }
                                ),
                            textAlign = TextAlign.Center,
                            fontSize = textSize.sp,
                            lineHeight = (textSize * 1.38f).sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold,
                            color = if (textColor != 0) Color(textColor) else Color.Black
                        )

                        FraseOptionsMenu(
                            expanded = showFraseMenu,
                            onDismiss = { showFraseMenu = false },
                            onConvertirNota = {
                                val result = FraseContextActions.convertirFraseEnNota(activityContext, frase)
                                if (result.ok) {
                                    Toast.makeText(activityContext, "Nota creada: ${result.titulo}", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(activityContext, "No se pudo crear la nota", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onCargarLienzo = {
                                FraseContextActions.cargarFraseEnLienzo(activityContext, frase)
                            },
                            onCompartirSistema = {
                                FraseContextActions.compartirFraseSistema(
                                    context = activityContext,
                                    frase = frase,
                                    autor = autor,
                                    fuente = fuente
                                )
                            },
                            onAbrirNotaFrase = {
                                FraseContextActions.abrirNotaDeFrase(activityContext, frase)
                            },
                            onCrearNuevaFrase = {
                                UiModalWindows.Add_New_frase(activityContext, null)
                            }
                        )
                    }

                    Text(
                        text = "<$autor>",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 10.dp),
                        textAlign = TextAlign.End,
                        fontSize = 18.sp,
                        fontStyle = FontStyle.Italic
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 10.dp, top = 2.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = if (hideInlineControls) R.drawable.ic_abajo else R.drawable.ic_arriba),
                            contentDescription = getString(R.string.mostrar_ocultar_controles_frase),
                            modifier = Modifier
                                .size(25.dp)
                                .clickable {
                                    hideInlineControls = !hideInlineControls
                                    prefs.edit { putBoolean("hide_frase_controles", hideInlineControls) }
                                }
                        )
                    }

                    if (!hideInlineControls) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 15.dp, end = 20.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val result = utilsDB.UpdateFavorito(
                                        context,
                                        DatabaseHelper.T_Frases,
                                        DatabaseHelper.CC_id,
                                        "",
                                        idFrase.toInt()
                                    )
                                    if (result.isNotEmpty()) {
                                        favState = result
                                    }
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(Color.Transparent)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_toolbar_favorite),
                                    contentDescription = "Favorito",
                                    tint = if (favState == "1") colorResource(id = R.color.fav_active) else colorResource(id = R.color.fav_inactive)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            IconButton(onClick = { showFraseMenu = true }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_menu_open),
                                    contentDescription = "Opciones",
                                    tint = colorResource(id = R.color.shared_social)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private data class HomeDisplay(
        val id: Long,
        val frase: String,
        val autor: String,
        val fuente: String,
        val fav: String
    )

    private fun loadInitialState(): HomeDisplay? {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        var startMode = prefs.getString("list_start_load", "")
        if (startMode.isNullOrEmpty()) {
            prefs.edit { putString("list_start_load", "Frase_azar") }
            startMode = "Frase_azar"
        }

        return when (startMode) {
            "Ultima_frase_vista" -> {
                val idUltimaFrase = prefs.getString(utilsFields.SETTING_KEY_ID_ULTIMA_FRASE, "0")
                val frase = utilsDB.getFraseById(requireContext(), idUltimaFrase?.toLongOrNull() ?: 0)
                frase?.let {
                    utilsFields.ID_row_ofElementLoad = it.id.toInt()
                    utilsFields.ID_Str_row_ofElementLoad = it.frase
                    HomeDisplay(it.id, it.frase, it.autor, it.fuente, it.favState())
                }
            }

            "Frase_azar" -> loadFrase(false)
            "Frase_fav_azar" -> loadFrase(true)
            "Conf_azar" -> {
                loadConfAzar(false)
                null
            }

            "Conf_fav_azar" -> {
                loadConfAzar(true)
                null
            }

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
                    MainActivity.currentInstance()?.openDestinationAsSheet(R.id.frag_content_webview)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Debe cargar al menos una conferencia en Texto",
                        Toast.LENGTH_SHORT
                    ).show()
                    frag_listado.elementLoaded = "autores/neville/conf"
                    MainActivity.currentInstance()?.openDestinationAsSheet(R.id.frag_listado)
                }
                null
            }

            else -> loadFrase(false)
        }
    }

    private fun loadFrase(isFavList: Boolean): HomeDisplay? {
        val frase = utilsDB.getRandomFrase(requireContext(), isFavList)
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        return if (frase != null) {
            utilsFields.ID_row_ofElementLoad = frase.id.toInt()
            utilsFields.ID_Str_row_ofElementLoad = frase.frase
            prefs.edit { putString(utilsFields.SETTING_KEY_ID_ULTIMA_FRASE, frase.id.toString()) }
            HomeDisplay(frase.id, frase.frase, frase.autor, frase.fuente, frase.favState())
        } else {
            val hasAnyFilter = prefs.getBoolean("home_filter_autores", true) ||
                prefs.getBoolean("home_filter_otros", true) ||
                prefs.getBoolean("home_filter_salud", true)
            Toast.makeText(
                requireContext(),
                if (hasAnyFilter) {
                    "No hay frase para mostrar con el filtro actual"
                } else {
                    "No hay categorías activas. Activa al menos una en Ajustes"
                },
                Toast.LENGTH_SHORT
            ).show()
            null
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
            MainActivity.currentInstance()?.openDestinationAsSheet(R.id.frag_content_webview)
        } else {
            Toast.makeText(
                requireContext(),
                "No hay Conferencia favorita para mostrar. Cargando Conferencias inbuilt",
                Toast.LENGTH_SHORT
            ).show()
            prefs.edit { putString("list_start_load", "Conf_azar") }
        }
    }
}
