package com.ypg.neville.ui.frag

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.model.utils.Utils
import com.ypg.neville.model.utils.utilsFields
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
        val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
        val listado = remember { mutableStateListOf<String>() }
        var queryTitulo by remember { mutableStateOf("") }
        var queryContenido by remember { mutableStateOf("") }
        var showOptions by remember { mutableStateOf(false) }
        var showFilterMenu by remember { mutableStateOf(false) }
        var filter by remember { mutableStateOf("Todas") }

        val showConfOptions = elementLoaded.equals("autores/neville/conf", ignoreCase = true)
        val showHelp = remember {
            prefs.getBoolean("help_inline", true)
        }
        val listTextSize = (prefs.getString("fuente_listados", "22")?.toFloatOrNull() ?: 22f).coerceIn(12f, 40f)

        LaunchedEffect(elementLoaded) {
            listado.clear()
            listado.addAll(generarListado())
            queryTitulo = ""
            queryContenido = ""
            filter = "Todas"
            showOptions = false
        }

        val visibleItems = listado.filter { item ->
            val displayName = formatListadoDisplayName(item)
            item.contains(queryTitulo, ignoreCase = true) || displayName.contains(queryTitulo, ignoreCase = true)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (showHelp) {
                IconButton(
                    onClick = {
                        Toast.makeText(
                            context,
                            "Filtra y busca elementos. Toca un item para abrirlo.",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_help), contentDescription = "Ayuda")
                }
            }

            if (showConfOptions) {
                Text(
                    text = if (showOptions) getString(R.string.ocultar_opciones) else getString(R.string.mostrar_opciones),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable { showOptions = !showOptions }
                        .padding(bottom = 8.dp)
                )
            }

            if (showConfOptions && showOptions) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(onClick = { showFilterMenu = true }, label = { Text("Filtro: $filter") })
                    DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                        listOf("Todas", "Favoritos", "Con notas").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    showFilterMenu = false
                                    filter = option
                                    listado.clear()
                                    listado.addAll(loadConfByFilter(option))
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = queryTitulo,
                    onValueChange = { queryTitulo = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    label = { Text("Buscar en títulos (${listado.size})") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = queryContenido,
                    onValueChange = { queryContenido = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    label = { Text("Buscar dentro de conferencias") },
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
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemSelected(item, navController) }
                            .padding(vertical = 10.dp, horizontal = 6.dp)
                    )
                }

                if (visibleItems.isEmpty()) {
                    item {
                        Text(
                            text = "No hay elementos para mostrar",
                            fontSize = (listTextSize - 2f).coerceAtLeast(12f).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }

    private fun onItemSelected(selectedItemText: String, navController: NavController) {
        when (elementLoaded) {
            "autores/neville/conf" -> {
                FragContentWebView.elementLoaded = "autores/neville/conf"
                utilsFields.ID_Str_row_ofElementLoad = selectedItemText
                val confFileName = FragContentWebView.confAssetFileNameFromTitle(selectedItemText)
                FragContentWebView.urlPath = "file:///android_asset/autores/neville/conf/$confFileName.txt"
                navController.navigate(R.id.frag_content_webview)
            }

            "preguntas" -> {
                utilsFields.ID_Str_row_ofElementLoad = selectedItemText
                FragContentWebView.urlPath = "file:///android_asset/autores/neville/preg/$selectedItemText.txt"
                navController.navigate(R.id.frag_content_webview)
            }

            "citasConferencias" -> {
                utilsFields.ID_Str_row_ofElementLoad = selectedItemText
                FragContentWebView.urlPath = "file:///android_asset/autores/neville/cita/$selectedItemText.txt"
                navController.navigate(R.id.frag_content_webview)
            }

            "ayudas" -> {
                utilsFields.ID_Str_row_ofElementLoad = selectedItemText
                FragContentWebView.urlPath = "file:///android_asset/ayuda/$selectedItemText.txt"
                navController.navigate(R.id.frag_content_webview)
            }

            "evidenciaCientifica" -> {
                utilsFields.ID_Str_row_ofElementLoad = selectedItemText
                FragContentWebView.urlPath = "file:///android_asset/evidenciaCientifica/$selectedItemText.txt"
                navController.navigate(R.id.frag_content_webview)
            }

            "reflexiones" -> {
                utilsFields.ID_Str_row_ofElementLoad = selectedItemText
                FragContentWebView.urlPath = "file:///android_asset/reflexiones/$selectedItemText.txt"
                navController.navigate(R.id.frag_content_webview)
            }

            "enciclopedia" -> {
                elementLoaded = "enciclopedia/$selectedItemText"
                navController.navigate(R.id.frag_listado)
            }

            else -> {
                if (elementLoaded.startsWith("enciclopedia/")) {
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
