package com.ypg.neville.ui.frag

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.ypg.neville.model.preferences.DbPreferences
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryEventLogger
import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryEventType
import com.ypg.neville.model.backup.BackupRestoreSignal
import com.ypg.neville.model.db.DatabaseHelper
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import com.ypg.neville.model.db.room.ReflexionRepository
import com.ypg.neville.model.subscription.SubscriptionManager
import com.ypg.neville.model.utils.Utils
import com.ypg.neville.model.utils.utilsFields
import com.ypg.neville.ui.theme.ContextMenuShape
import java.io.IOException
import java.util.Locale
import org.json.JSONObject

class frag_listado : Fragment() {

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navController = view.findNavController()
        (view as ComposeView).setContent {
            com.ypg.neville.ui.theme.NevilleTheme {
                ListadoScreen(navController)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        MainActivity.currentInstance()?.let {
            it.icToolsBarFav.setColorFilter(requireContext().resources.getColor(R.color.black, null))
            it.icToolsBarFav.visibility = if (elementLoaded.equals("autores/neville/conf", ignoreCase = true)) View.VISIBLE else View.GONE
            it.icToolsBarFraseAdd.visibility = View.VISIBLE
        }
    }

    @Composable
    private fun ListadoScreen(navController: NavController) {
        val context = LocalContext.current
        val subscriptionState by SubscriptionManager.uiState.collectAsState()
        val restoreTick by BackupRestoreSignal.restoreTick.collectAsState()
        val hasPremium = subscriptionState.isActive && subscriptionState.isEntitlementVerified
        val prefs = remember { DbPreferences.default(context) }
        val listado = remember { mutableStateListOf<String>() }
        val reflexionRepository = remember {
            ReflexionRepository(NevilleRoomDatabase.getInstance(context).reflexionDao())
        }
        val reflexiones = remember { mutableStateListOf<ReflexionListItem>() }
        val ayudasContenido = remember { mutableStateMapOf<String, String>() }
        var queryTitulo by remember { mutableStateOf("") }
        var queryContenido by remember { mutableStateOf("") }
        var showOptionsMenu by remember { mutableStateOf(false) }
        var showSearchPanel by remember { mutableStateOf(false) }
        var filter by remember { mutableStateOf("Todas") }
        var conferenceEditingTitle by remember { mutableStateOf<String?>(null) }
        var conferenceEditingFav by remember { mutableStateOf(false) }
        var conferenceEditingNota by remember { mutableStateOf("") }
        var reflexionEnEdicion by remember { mutableStateOf<ReflexionListItem?>(null) }
        var showReflexionEditor by remember { mutableStateOf(false) }
        var showReflexionesFabMenu by remember { mutableStateOf(false) }
        var showReflexionesFilterPanel by remember { mutableStateOf(false) }
        var filtroReflexionTitulo by remember { mutableStateOf("") }
        var filtroReflexionContenido by remember { mutableStateOf("") }
        var filtroReflexionFavorito by remember { mutableStateOf(ReflexionFavoritoFiltro.TODAS) }
        var showAyudasFilterPanel by remember { mutableStateOf(false) }
        var filtroAyudasTitulo by remember { mutableStateOf("") }
        var filtroAyudasContenido by remember { mutableStateOf("") }

        val showConfOptions = elementLoaded.equals("autores/neville/conf", ignoreCase = true)
        val isReflexionesView = elementLoaded == "reflexiones"
        val isAyudasView = elementLoaded == "ayudas"
        val isEnciclopediaTopicsView = elementLoaded.startsWith("enciclopedia/") && elementLoaded != "enciclopedia"
        val isNevilleAuthorListView = elementLoaded == "autores/neville/conf" ||
            elementLoaded == "citasConferencias" ||
            elementLoaded == "preguntas"
        val listPrimaryColor = when {
            showConfOptions -> Color.Black
            isReflexionesView -> Color.Black
            isAyudasView -> Color.Black
            else -> MaterialTheme.colorScheme.onBackground
        }
        val listSecondaryColor = when {
            showConfOptions -> Color.Black.copy(alpha = 0.75f)
            isAyudasView -> Color.Black.copy(alpha = 0.75f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        val listTextSize = (prefs.getString("fuente_listados", "22")?.toFloatOrNull() ?: 22f).coerceIn(12f, 40f)

        fun recargarReflexiones() {
            val personalizadas = reflexionRepository.obtenerTodas().map { entity ->
                ReflexionListItem(
                    id = entity.id,
                    titulo = entity.titulo,
                    contenido = entity.contenido,
                    favorito = entity.favorito,
                    nota = entity.nota,
                    fechaCreacion = entity.fechaCreacion,
                    fechaModificacion = entity.fechaModificacion,
                    isCustom = true,
                    rawAssetKey = null
                )
            }
            val assets = loadReflexionesDesdeAssets()
            reflexiones.clear()
            reflexiones.addAll(personalizadas + assets)
        }

        LaunchedEffect(elementLoaded, restoreTick) {
            if (isReflexionesView) {
                recargarReflexiones()
                filtroReflexionTitulo = ""
                filtroReflexionContenido = ""
                filtroReflexionFavorito = ReflexionFavoritoFiltro.TODAS
                showReflexionesFabMenu = false
                showReflexionesFilterPanel = false
            } else {
                listado.clear()
                listado.addAll(generarListado())
                if (isAyudasView) {
                    ayudasContenido.clear()
                    listado.forEach { key ->
                        val raw = runCatching {
                            requireContext().assets.open("ayudas/$key.txt").bufferedReader().use { it.readText() }
                        }.getOrDefault("")
                        ayudasContenido[key] = extractReflexionText(raw)
                    }
                    filtroAyudasTitulo = ""
                    filtroAyudasContenido = ""
                    showAyudasFilterPanel = false
                }
            }
            queryTitulo = ""
            queryContenido = ""
            filter = "Todas"
            showSearchPanel = false
            showOptionsMenu = false
        }

        val visibleItems = listado.filter { item ->
            val displayName = formatListadoDisplayName(item)
            item.contains(queryTitulo, ignoreCase = true) || displayName.contains(queryTitulo, ignoreCase = true)
        }
        val ayudasFiltradas = listado.filter { item ->
            val displayName = formatListadoDisplayName(item)
            val cumpleTitulo = filtroAyudasTitulo.isBlank() ||
                item.contains(filtroAyudasTitulo.trim(), ignoreCase = true) ||
                displayName.contains(filtroAyudasTitulo.trim(), ignoreCase = true)
            val cumpleContenido = filtroAyudasContenido.isBlank() ||
                (ayudasContenido[item] ?: "").contains(filtroAyudasContenido.trim(), ignoreCase = true)
            cumpleTitulo && cumpleContenido
        }
        val reflexionesFiltradas = reflexiones.filter { reflexion ->
            val cumpleTitulo = filtroReflexionTitulo.isBlank() ||
                reflexion.titulo.contains(filtroReflexionTitulo.trim(), ignoreCase = true)
            val cumpleContenido = filtroReflexionContenido.isBlank() ||
                reflexion.contenido.contains(filtroReflexionContenido.trim(), ignoreCase = true) ||
                reflexion.nota.contains(filtroReflexionContenido.trim(), ignoreCase = true)
            val cumpleFavorito = when (filtroReflexionFavorito) {
                ReflexionFavoritoFiltro.TODAS -> true
                ReflexionFavoritoFiltro.SOLO_FAVORITAS -> reflexion.favorito
                ReflexionFavoritoFiltro.SOLO_NO_FAVORITAS -> !reflexion.favorito
            }
            cumpleTitulo && cumpleContenido && cumpleFavorito
        }

        fun openConferenceEditor(title: String) {
            conferenceEditingTitle = title
            conferenceEditingFav =
                utilsDB.readFavState(context, DatabaseHelper.T_Conf, DatabaseHelper.C_conf_title, title) == "1"
            conferenceEditingNota = utilsDB.getConfNota(context, title)
        }

        fun reloadCurrentConferenceFilter() {
            if (!showConfOptions) return
            listado.clear()
            listado.addAll(loadConfByFilter(filter))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (showConfOptions) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFE7EAED),
                                Color(0xFFD6DBDF),
                                Color(0xFFC5CCD1)
                            )
                        )
                    } else if (isReflexionesView) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFF3B04F),
                                Color(0xFFD6AF78),
                                Color(0xFFA19470)
                            )
                        )
                    } else if (isAyudasView) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFF3B04F),
                                Color(0xFFD6AF78),
                                Color(0xFFA19470)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Transparent)
                        )
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (showConfOptions) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        IconButton(
                            onClick = { showOptionsMenu = true },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_menu_open),
                                contentDescription = "Opciones"
                            )
                        }
                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false },
                            shape = ContextMenuShape
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (showSearchPanel) "Ocultar búsqueda" else "Mostrar búsqueda") },
                                onClick = {
                                    showOptionsMenu = false
                                    showSearchPanel = !showSearchPanel
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Filtro: Todas") },
                                onClick = {
                                    showOptionsMenu = false
                                    filter = "Todas"
                                    listado.clear()
                                    listado.addAll(loadConfByFilter(filter))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Filtro: Favoritos") },
                                onClick = {
                                    showOptionsMenu = false
                                    filter = "Favoritos"
                                    listado.clear()
                                    listado.addAll(loadConfByFilter(filter))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Filtro: Con notas") },
                                onClick = {
                                    showOptionsMenu = false
                                    filter = "Con notas"
                                    listado.clear()
                                    listado.addAll(loadConfByFilter(filter))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Restablecer") },
                                onClick = {
                                    showOptionsMenu = false
                                    filter = "Todas"
                                    queryTitulo = ""
                                    queryContenido = ""
                                    listado.clear()
                                    listado.addAll(loadConfByFilter(filter))
                                }
                            )
                        }
                    }
                }

                if (isAyudasView) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        IconButton(
                            onClick = { showAyudasFilterPanel = !showAyudasFilterPanel },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                painter = painterResource(id = if (showAyudasFilterPanel) R.drawable.ic_abajo else R.drawable.ic_menu_open),
                                contentDescription = if (showAyudasFilterPanel) "Ocultar filtros" else "Mostrar filtros"
                            )
                        }
                    }
                }

                if (showConfOptions && showSearchPanel) {
                    OutlinedTextField(
                        value = queryTitulo,
                        onValueChange = { queryTitulo = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        label = { Text("Buscar en títulos (${listado.size})") },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = queryContenido,
                        onValueChange = { queryContenido = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        label = { Text("Buscar dentro de conferencias") },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = {
                            if (queryContenido.isNotBlank()) {
                                try {
                                    listado.clear()
                                    listado.addAll(Utils.searchInConf(context, queryContenido))
                                } catch (_: IOException) {
                                    Toast.makeText(context, "No se pudo realizar la búsqueda", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Text("Buscar")
                        }

                        Button(onClick = {
                            listado.clear()
                            listado.addAll(loadConfByFilter(filter))
                            queryContenido = ""
                        }) {
                            Text("Restablecer")
                        }
                    }
                }

                if (isAyudasView && showAyudasFilterPanel) {
                    OutlinedTextField(
                        value = filtroAyudasTitulo,
                        onValueChange = { filtroAyudasTitulo = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        label = { Text("Buscar en titulos (${listado.size})") },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedLabelColor = Color.Black,
                            unfocusedLabelColor = Color.Black.copy(alpha = 0.85f),
                            cursorColor = Color.Black,
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.Black.copy(alpha = 0.55f)
                        )
                    )

                    OutlinedTextField(
                        value = filtroAyudasContenido,
                        onValueChange = { filtroAyudasContenido = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        label = { Text("Buscar en contenido") },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedLabelColor = Color.Black,
                            unfocusedLabelColor = Color.Black.copy(alpha = 0.85f),
                            cursorColor = Color.Black,
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.Black.copy(alpha = 0.55f)
                        )
                    )
                }

                Spacer(modifier = Modifier.padding(top = 8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isReflexionesView) {
                        items(reflexionesFiltradas, key = { "${if (it.isCustom) "db" else "asset"}-${it.id ?: it.rawAssetKey}" }) { item ->
                            val titlePrefix = if (item.isCustom) "[Personal] " else ""
                            val titleSuffix = if (item.favorito) " (Favorita)" else ""
                            Text(
                                text = "$titlePrefix${item.titulo}$titleSuffix",
                                fontSize = listTextSize.sp,
                                lineHeight = (listTextSize * 1.24f).sp,
                                color = listPrimaryColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (item.isCustom) {
                                                reflexionEnEdicion = item
                                                showReflexionEditor = true
                                            } else {
                                                onItemSelected(item.rawAssetKey.orEmpty(), navController)
                                            }
                                        },
                                        onLongClick = {
                                            if (item.isCustom) {
                                                reflexionEnEdicion = item
                                                showReflexionEditor = true
                                            }
                                        }
                                    )
                                    .padding(vertical = 10.dp, horizontal = 6.dp)
                            )
                        }
                    } else {
                        val filteredGeneral = if (isAyudasView) ayudasFiltradas else visibleItems
                        items(filteredGeneral) { item ->
                            Text(
                                text = formatListadoDisplayName(item),
                                fontSize = listTextSize.sp,
                                lineHeight = (listTextSize * 1.24f).sp,
                                color = listPrimaryColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { onItemSelected(item, navController) },
                                        onLongClick = {
                                            if (showConfOptions) {
                                                openConferenceEditor(item)
                                            }
                                        }
                                    )
                                    .padding(vertical = 10.dp, horizontal = 6.dp)
                            )
                        }
                    }

                    val noGeneralItems = if (isAyudasView) ayudasFiltradas.isEmpty() else visibleItems.isEmpty()
                    if ((isReflexionesView && reflexionesFiltradas.isEmpty()) || (!isReflexionesView && noGeneralItems)) {
                        item {
                            Text(
                                text = "No hay elementos para mostrar",
                                fontSize = (listTextSize - 2f).coerceAtLeast(12f).sp,
                                color = listSecondaryColor,
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            if (isEnciclopediaTopicsView) {
                AssistChip(
                    onClick = {
                        if (!navController.popBackStack()) {
                            elementLoaded = "enciclopedia"
                            navController.navigate(R.id.frag_listado)
                        }
                    },
                    label = { Text("Atrás") },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 12.dp)
                        .height(34.dp)
                )
            }

            if (isNevilleAuthorListView) {
                AssistChip(
                    onClick = {
                        if (!navController.popBackStack()) {
                            navController.navigate(R.id.frag_neville_goddard)
                        }
                    },
                    label = { Text("Atrás") },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 12.dp)
                        .height(34.dp)
                )
            }

            if (isReflexionesView) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = if (showReflexionesFilterPanel) 130.dp else 20.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (showReflexionesFabMenu) {
                        FabActionItem(
                            label = "Crear reflexion",
                            iconRes = R.drawable.ic_note_add,
                            onClick = {
                                showReflexionesFabMenu = false
                                reflexionEnEdicion = null
                                showReflexionEditor = true
                            }
                        )
                        FabActionItem(
                            label = if (showReflexionesFilterPanel) "Ocultar filtros" else "Mostrar filtros",
                            iconRes = R.drawable.ic_show,
                            onClick = {
                                showReflexionesFabMenu = false
                                showReflexionesFilterPanel = !showReflexionesFilterPanel
                            }
                        )
                    }

                    FloatingActionButton(
                        onClick = { showReflexionesFabMenu = !showReflexionesFabMenu },
                        containerColor = Color(0xFF062D48),
                        contentColor = Color.White
                    ) {
                        Icon(
                            painter = painterResource(id = if (showReflexionesFabMenu) R.drawable.ic_abajo else R.drawable.ic_menu_open),
                            tint = Color.White,
                            contentDescription = "Menu Reflexiones"
                        )
                    }
                }

                if (showReflexionesFilterPanel) {
                    ReflexionFilterPanel(
                        filtroTitulo = filtroReflexionTitulo,
                        onFiltroTituloChange = { filtroReflexionTitulo = it },
                        filtroContenido = filtroReflexionContenido,
                        onFiltroContenidoChange = { filtroReflexionContenido = it },
                        filtroFav = filtroReflexionFavorito,
                        onFiltroFavChange = { filtroReflexionFavorito = it },
                        onClear = {
                            filtroReflexionTitulo = ""
                            filtroReflexionContenido = ""
                            filtroReflexionFavorito = ReflexionFavoritoFiltro.TODAS
                        },
                        onHide = { showReflexionesFilterPanel = false },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                    )
                }
            }
        }

        conferenceEditingTitle?.let { title ->
            ConferenceEditDialog(
                conferenceTitle = formatListadoDisplayName(title),
                isFavorite = conferenceEditingFav,
                note = conferenceEditingNota,
                onFavoriteChange = { conferenceEditingFav = it },
                onNoteChange = { conferenceEditingNota = it },
                onDismiss = { conferenceEditingTitle = null },
                onSave = {
                    val currentFav = utilsDB.readFavState(
                        context,
                        DatabaseHelper.T_Conf,
                        DatabaseHelper.C_conf_title,
                        title
                    ) == "1"
                    if (currentFav != conferenceEditingFav) {
                        utilsDB.UpdateFavorito(
                            context,
                            DatabaseHelper.T_Conf,
                            DatabaseHelper.C_conf_title,
                            title,
                            0
                        )
                    }
                    utilsDB.updateNota(
                        context,
                        DatabaseHelper.T_Conf,
                        DatabaseHelper.C_conf_title,
                        title,
                        conferenceEditingNota.trim()
                    )
                    conferenceEditingTitle = null
                    reloadCurrentConferenceFilter()
                    Toast.makeText(context, "Conferencia actualizada", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showReflexionEditor) {
            ReflexionEditorDialog(
                reflexion = reflexionEnEdicion,
                onDismiss = { showReflexionEditor = false },
                onSave = { titulo, contenido, favorito, nota ->
                    if (titulo.isBlank() || contenido.isBlank()) {
                        Toast.makeText(context, "Debes escribir titulo y contenido", Toast.LENGTH_SHORT).show()
                        false
                    } else {
                        val existing = reflexionEnEdicion
                        if (existing?.id == null) {
                            reflexionRepository.insertar(
                                titulo = titulo.trim(),
                                contenido = contenido.trim(),
                                favorito = favorito,
                                nota = nota.trim()
                            )
                        } else {
                            reflexionRepository.actualizar(
                                id = existing.id,
                                titulo = titulo.trim(),
                                contenido = contenido.trim(),
                                favorito = favorito,
                                nota = nota.trim(),
                                fechaCreacionOriginal = existing.fechaCreacion
                            )
                        }
                        recargarReflexiones()
                        showReflexionEditor = false
                        Toast.makeText(context, "Reflexion guardada", Toast.LENGTH_SHORT).show()
                        true
                    }
                }
            )
        }
    }

    @Composable
    private fun ConferenceEditDialog(
        conferenceTitle: String,
        isFavorite: Boolean,
        note: String,
        onFavoriteChange: (Boolean) -> Unit,
        onNoteChange: (String) -> Unit,
        onDismiss: () -> Unit,
        onSave: () -> Unit
    ) {
        Dialog(onDismissRequest = onDismiss) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E334F), RoundedCornerShape(22.dp))
                    .padding(7.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = conferenceTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )

                    Button(
                        onClick = { onFavoriteChange(!isFavorite) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFavorite) Color(0xFF9AC4F8) else Color(0xFFCAD5DF),
                            contentColor = Color(0xFF13212C)
                        )
                    ) {
                        Text(if (isFavorite) "Favorita: Sí" else "Favorita: No")
                    }

                    OutlinedTextField(
                        value = note,
                        onValueChange = onNoteChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp),
                        label = { Text("Nota de la conferencia", color = Color.White) },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6E84CF),
                            unfocusedBorderColor = Color(0xFFA5B4C3)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBAC4CF))
                        ) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = onSave,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.padding(start = 10.dp)
                        ) {
                            Text("Guardar")
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ReflexionFilterPanel(
        filtroTitulo: String,
        onFiltroTituloChange: (String) -> Unit,
        filtroContenido: String,
        onFiltroContenidoChange: (String) -> Unit,
        filtroFav: ReflexionFavoritoFiltro,
        onFiltroFavChange: (ReflexionFavoritoFiltro) -> Unit,
        onClear: () -> Unit,
        onHide: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(Color(0xE61F323D), RoundedCornerShape(20.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Filtros", color = Color.White)
                Row {
                    TextButton(onClick = onClear) {
                        Text("Limpiar", color = Color.White)
                    }
                    TextButton(onClick = onHide) {
                        Text("Ocultar", color = Color.White)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = filtroTitulo,
                    onValueChange = onFiltroTituloChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Buscar en titulo", color = Color.White) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.Gray
                    )
                )
                OutlinedTextField(
                    value = filtroContenido,
                    onValueChange = onFiltroContenidoChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Buscar en contenido", color = Color.White) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.Gray
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ReflexionFavoritoFiltro.entries.forEach { option ->
                    MiniFilterChip(
                        label = option.label,
                        selected = filtroFav == option,
                        onClick = { onFiltroFavChange(option) }
                    )
                }
            }
        }
    }

    @Composable
    private fun MiniFilterChip(
        label: String,
        selected: Boolean,
        onClick: () -> Unit
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFF11232E) else Color(0xFFE6EDF1),
            fontSize = 12.sp,
            modifier = Modifier
                .background(
                    if (selected) Color(0xFFD2E4EE) else Color(0x664E5E68),
                    RoundedCornerShape(12.dp)
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }

    @Composable
    private fun FabActionItem(
        label: String,
        iconRes: Int,
        onClick: () -> Unit
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                color = Color(0xFFEAF2F7),
                fontSize = 12.sp,
                modifier = Modifier
                    .background(Color(0xC2343E46), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
            FloatingActionButton(
                onClick = onClick,
                containerColor = Color(0xFFCFDAE3),
                contentColor = Color(0xFF1A2A33),
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label
                )
            }
        }
    }

    @Composable
    private fun ReflexionEditorDialog(
        reflexion: ReflexionListItem?,
        onDismiss: () -> Unit,
        onSave: (String, String, Boolean, String) -> Boolean
    ) {
        var titulo by remember(reflexion?.id) { mutableStateOf(reflexion?.titulo.orEmpty()) }
        var contenido by remember(reflexion?.id) { mutableStateOf(reflexion?.contenido.orEmpty()) }
        var nota by remember(reflexion?.id) { mutableStateOf(reflexion?.nota.orEmpty()) }
        var favorita by remember(reflexion?.id) { mutableStateOf(reflexion?.favorito ?: false) }

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .background(Color(0xFF1E334F), RoundedCornerShape(22.dp))
                    .padding(12.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (reflexion == null) "Nueva reflexion" else "Editar reflexion",
                        color = Color.White
                    )
                    OutlinedTextField(
                        value = titulo,
                        onValueChange = { titulo = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp),
                        label = { Text("Titulo", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold) },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFFF9800),
                            unfocusedTextColor = Color(0xFFFF9800),
                            cursorColor = Color.White,
                            focusedBorderColor = Color(0xFFFF9800),
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    OutlinedTextField(
                        value = contenido,
                        onValueChange = { contenido = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp),
                        label = { Text("Contenido", color = Color.White) },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White,
                            focusedBorderColor = Color(0xFFFF9800),
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    OutlinedTextField(
                        value = nota,
                        onValueChange = { nota = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nota", color = Color.White) },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.Gray
                        )
                    )

                    Button(
                        onClick = { favorita = !favorita },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (favorita) Color(0xFF9AC4F8) else Color(0xFFCAD5DF),
                            contentColor = Color(0xFF13212C)
                        )
                    ) {
                        Text(if (favorita) "Favorita: Si" else "Favorita: No")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBAC4CF))
                        ) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = { onSave(titulo, contenido, favorita, nota) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.padding(start = 10.dp)
                        ) {
                            Text("Guardar")
                        }
                    }
                }
            }
        }
    }

    private fun onItemSelected(selectedItemText: String, navController: NavController) {
        val hasPremium = SubscriptionManager.hasActiveSubscription(requireContext())
        when (elementLoaded) {
            "autores/neville/conf" -> {
                FragContentWebView.isPremiumPreviewMode = false
                FragContentWebView.elementLoaded = "autores/neville/conf"
                utilsFields.ID_Str_row_ofElementLoad = selectedItemText
                val confFileName = FragContentWebView.confAssetFileNameFromTitle(selectedItemText)
                FragContentWebView.urlPath = "file:///android_asset/autores/neville/conf/$confFileName.txt"
                navController.navigate(R.id.frag_content_webview)
            }

            "preguntas" -> {
                FragContentWebView.isPremiumPreviewMode = false
                utilsFields.ID_Str_row_ofElementLoad = selectedItemText
                FragContentWebView.urlPath = "file:///android_asset/autores/neville/preg/$selectedItemText.txt"
                navController.navigate(R.id.frag_content_webview)
            }

            "citasConferencias" -> {
                FragContentWebView.isPremiumPreviewMode = false
                utilsFields.ID_Str_row_ofElementLoad = selectedItemText
                FragContentWebView.urlPath = "file:///android_asset/autores/neville/cita/$selectedItemText.txt"
                navController.navigate(R.id.frag_content_webview)
            }

            "ayudas" -> {
                val premiumAyudas = setOf(
                    "ayud_estructura_no_emocion",
                    "ayud_arquitectura_del_cambio"
                )
                FragContentWebView.isPremiumPreviewMode = !hasPremium && selectedItemText in premiumAyudas
                utilsFields.ID_Str_row_ofElementLoad = selectedItemText
                FragContentWebView.urlPath = "file:///android_asset/ayudas/$selectedItemText.txt"
                navController.navigate(R.id.frag_content_webview)
            }

            "evidenciaCientifica" -> {
                FragContentWebView.isPremiumPreviewMode = !hasPremium
                utilsFields.ID_Str_row_ofElementLoad = selectedItemText
                FragContentWebView.urlPath = "file:///android_asset/evidenciaCientifica/$selectedItemText.txt"
                navController.navigate(R.id.frag_content_webview)
            }

            "reflexiones" -> {
                FragContentWebView.isPremiumPreviewMode = false
                utilsFields.ID_Str_row_ofElementLoad = selectedItemText
                FragContentWebView.urlPath = "file:///android_asset/reflexiones/$selectedItemText.txt"
                navController.navigate(R.id.frag_content_webview)
            }

            "enciclopedia" -> {
                FragContentWebView.isPremiumPreviewMode = false
                elementLoaded = "enciclopedia/$selectedItemText"
                navController.navigate(R.id.frag_listado)
            }

            else -> {
                if (elementLoaded.startsWith("enciclopedia/")) {
                    FragContentWebView.isPremiumPreviewMode = !hasPremium
                    utilsFields.ID_Str_row_ofElementLoad = selectedItemText
                    val section = elementLoaded.removePrefix("enciclopedia/")
                    WeeklySummaryEventLogger.log(
                        WeeklySummaryEventType.ENCYCLOPEDIA_ACCESSED,
                        targetKey = "$section/$selectedItemText"
                    )
                    FragContentWebView.urlPath = "file:///android_asset/enciclopedia/$section/$selectedItemText.txt"
                    navController.navigate(R.id.frag_content_webview)
                }
            }
        }
    }

    private fun loadConfByFilter(option: String): List<String> {
        val fromDb = when (option) {
            "Favoritos" -> utilsDB.getListadoTitles(requireContext(), "Conferencias favoritas")
            "Con notas" -> utilsDB.getListadoTitles(requireContext(), "Conferencias con notas")
            else -> utilsDB.getAllConfTitles(requireContext())
        }
        return if (fromDb.isNotEmpty()) fromDb else loadConfTitlesFromAssets()
    }

    private fun generarListado(): List<String> {
        val utils = Utils(requireContext())
        val listado = mutableListOf<String>()
        when {
            elementLoaded == "autores/neville/conf" -> {
                val dbItems = utilsDB.loadConferenciaList(requireContext())
                if (dbItems.isNotEmpty()) {
                    listado.addAll(dbItems)
                } else {
                    listado.addAll(loadConfTitlesFromAssets())
                }
            }
            elementLoaded == "preguntas" -> runCatching { utils.listFilesInAssets("autores/neville/preg", listado) }
            elementLoaded == "citasConferencias" -> runCatching { utils.listFilesInAssets("autores/neville/cita", listado) }
            elementLoaded == "ayudas" -> runCatching { utils.listFilesInAssets("ayudas", listado) }
            elementLoaded == "evidenciaCientifica" -> runCatching { utils.listFilesInAssets("evidenciaCientifica", listado) }
            elementLoaded == "reflexiones" -> runCatching { utils.listFilesInAssets("reflexiones", listado) }
            elementLoaded == "enciclopedia" -> {
                val folders = requireContext().assets.list("enciclopedia") ?: emptyArray()
                listado.addAll(folders)
            }

            elementLoaded.startsWith("enciclopedia/") -> {
                val section = elementLoaded.removePrefix("enciclopedia/")
                runCatching { utils.listFilesInAssets("enciclopedia/$section", listado) }
            }
        }
        return listado
    }

    private fun loadConfTitlesFromAssets(): List<String> {
        return runCatching {
            val files = requireContext().assets.list("autores/neville/conf").orEmpty()
            files
                .filter { it.startsWith("conf_") && it.endsWith(".txt", ignoreCase = true) }
                .map { it.removePrefix("conf_").removeSuffix(".txt") }
                .sorted()
        }.getOrDefault(emptyList())
    }

    private fun formatListadoDisplayName(rawName: String): String {
        if (elementLoaded.equals("autores/neville/conf", ignoreCase = true)) {
            val locale = Locale.getDefault()
            return rawName
                .trim()
                .split(Regex("\\s+"))
                .joinToString(" ") { word ->
                    word.lowercase(locale).replaceFirstChar { first ->
                        if (first.isLowerCase()) first.titlecase(locale) else first.toString()
                    }
                }
        }

        val prefixToRemove = when {
            elementLoaded.startsWith("enciclopedia") -> "enc_"
            elementLoaded == "evidenciaCientifica" -> "evi_"
            elementLoaded == "reflexiones" -> "reflex_"
            elementLoaded == "ayudas" -> "ayud_"
            else -> return rawName
        }

        val cleaned = rawName
            .removePrefix(prefixToRemove)
            .replace('_', ' ')
            .trim()

        if (cleaned.isEmpty()) return rawName

        val locale = Locale.getDefault()
        return cleaned
            .split(Regex("\\s+"))
            .joinToString(" ") { word ->
                word.lowercase(locale).replaceFirstChar { first ->
                    if (first.isLowerCase()) first.titlecase(locale) else first.toString()
                }
            }
    }

    private fun loadReflexionesDesdeAssets(): List<ReflexionListItem> {
        val utils = Utils(requireContext())
        val keys = mutableListOf<String>()
        runCatching { utils.listFilesInAssets("reflexiones", keys) }

        return keys.map { key ->
            val content = runCatching {
                requireContext().assets.open("reflexiones/$key.txt").bufferedReader().use { it.readText() }
            }.getOrDefault("")

            val normalizedContent = extractReflexionText(content)

            ReflexionListItem(
                id = null,
                titulo = formatListadoDisplayName(key),
                contenido = normalizedContent,
                favorito = false,
                nota = "",
                fechaCreacion = 0L,
                fechaModificacion = 0L,
                isCustom = false,
                rawAssetKey = key
            )
        }
    }

    private fun extractReflexionText(raw: String): String {
        val jsonContent = runCatching {
            val json = JSONObject(raw)
            val contenidoArray = json.optJSONArray("contenido")
            val contenido = buildList {
                if (contenidoArray != null) {
                    for (i in 0 until contenidoArray.length()) {
                        add(contenidoArray.optString(i).trim())
                    }
                }
            }
            contenido.filter { it.isNotBlank() }.joinToString("\n\n")
        }.getOrNull()

        return if (jsonContent.isNullOrBlank()) raw else jsonContent
    }

    private data class ReflexionListItem(
        val id: Long?,
        val titulo: String,
        val contenido: String,
        val favorito: Boolean,
        val nota: String,
        val fechaCreacion: Long,
        val fechaModificacion: Long,
        val isCustom: Boolean,
        val rawAssetKey: String?
    )

    private enum class ReflexionFavoritoFiltro(val label: String) {
        TODAS("Todas"),
        SOLO_FAVORITAS("Favoritas"),
        SOLO_NO_FAVORITAS("No favoritas")
    }

    companion object {
        @JvmField
        var elementLoaded = ""
    }
}
