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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.db.DatabaseHelper
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.model.subscription.SubscriptionManager
import com.ypg.neville.model.utils.Utils
import com.ypg.neville.model.utils.utilsFields
import com.ypg.neville.ui.theme.ContextMenuShape
import java.io.IOException
import java.util.Locale

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
        val hasPremium = subscriptionState.isActive
        val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
        val listado = remember { mutableStateListOf<String>() }
        var queryTitulo by remember { mutableStateOf("") }
        var queryContenido by remember { mutableStateOf("") }
        var showOptionsMenu by remember { mutableStateOf(false) }
        var showSearchPanel by remember { mutableStateOf(false) }
        var filter by remember { mutableStateOf("Todas") }
        var conferenceEditingTitle by remember { mutableStateOf<String?>(null) }
        var conferenceEditingFav by remember { mutableStateOf(false) }
        var conferenceEditingNota by remember { mutableStateOf("") }

        val showConfOptions = elementLoaded.equals("autores/neville/conf", ignoreCase = true)
        val isEnciclopediaTopicsView = elementLoaded.startsWith("enciclopedia/") && elementLoaded != "enciclopedia"
        val listPrimaryColor = if (showConfOptions) Color.Black else MaterialTheme.colorScheme.onBackground
        val listSecondaryColor = if (showConfOptions) Color.Black.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
        val listTextSize = (prefs.getString("fuente_listados", "22")?.toFloatOrNull() ?: 22f).coerceIn(12f, 40f)

        LaunchedEffect(elementLoaded) {
            listado.clear()
            listado.addAll(generarListado())
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

                Spacer(modifier = Modifier.padding(top = 8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visibleItems) { item ->
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

                    if (visibleItems.isEmpty()) {
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
                FragContentWebView.isPremiumPreviewMode = false
                utilsFields.ID_Str_row_ofElementLoad = selectedItemText
                FragContentWebView.urlPath = "file:///android_asset/ayuda/$selectedItemText.txt"
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
            elementLoaded == "ayudas" -> runCatching { utils.listFilesInAssets("ayuda", listado) }
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

    companion object {
        @JvmField
        var elementLoaded = ""
    }
}
