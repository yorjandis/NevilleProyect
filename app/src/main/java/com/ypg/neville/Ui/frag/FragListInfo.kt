package com.ypg.neville.ui.frag

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.db.DatabaseHelper
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.model.utils.UiModalWindows
import com.ypg.neville.model.utils.utilsFields

class FragListInfo : Fragment() {

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
                ListInfoScreen(navController)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        runCatching { MainActivity.currentInstance()?.icToolsBarFraseAdd?.visibility = View.VISIBLE }
        runCatching { MainActivity.currentInstance()?.icToolsBarFav?.visibility = View.GONE }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    @Suppress("ASSIGNED_VALUE_IS_NEVER_READ")
    private fun ListInfoScreen(navController: NavController) {
        val context = LocalContext.current
        val listado = remember { mutableStateListOf<String>() }
        val filtros = remember {
            listOf(
                "Frases inbuilt",
                "Frases inbuilt favoritas",
                "Frases inbuilt con notas",
                "Frases personales",
                "Frases personales favoritas",
                "Frases personales con notas",
                "Conferencias favoritas",
                "Conferencias con notas",
                "Apuntes"
            )
        }

        var filtroActual by remember { mutableStateOf(filtros.first()) }
        var showFiltroMenu by remember { mutableStateOf(false) }
        var itemPendienteQuitarFav by remember { mutableStateOf<String?>(null) }

        val showHelp = remember {
            PreferenceManager.getDefaultSharedPreferences(context).getBoolean("help_inline", true)
        }

        LaunchedEffect(filtroActual) {
            utilsFields.spinnerListInfoItemSelected = filtroActual
            listado.clear()
            listado.addAll(utilsDB.getListadoTitles(context, filtroActual))
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
                            "Selecciona un filtro y toca un elemento para abrirlo. Mantén pulsado para gestionar nota.",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_help), contentDescription = "Ayuda")
                }
            }

            Button(onClick = { showFiltroMenu = true }) {
                Text("Filtro: $filtroActual")
            }

            DropdownMenu(expanded = showFiltroMenu, onDismissRequest = { showFiltroMenu = false }) {
                filtros.forEach { filtro ->
                    DropdownMenuItem(
                        text = { Text(filtro) },
                        onClick = {
                            showFiltroMenu = false
                            filtroActual = filtro
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listado) { itemText ->
                    Text(
                        text = itemText,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    when (filtroActual) {
                                        "Frases inbuilt favoritas" -> {
                                            itemPendienteQuitarFav = itemText
                                        }

                                        "Frases personales", "Frases inbuilt con notas", "Frases personales con notas" -> {
                                            UiModalWindows.NotaManager(
                                                context,
                                                utilsDB.getFraseNota(context, itemText),
                                                DatabaseHelper.T_Frases,
                                                DatabaseHelper.C_frases_frase,
                                                itemText
                                            )
                                        }

                                        "Conferencias favoritas", "Conferencias con notas" -> {
                                            utilsFields.ID_Str_row_ofElementLoad = itemText
                                            FragContentWebView.extension = ".txt"
                                            FragContentWebView.urlDirAssets = "autores/neville/conf"
                                            val confFileName = FragContentWebView.confAssetFileNameFromTitle(itemText)
                                            FragContentWebView.urlPath =
                                                "file:///android_asset/${FragContentWebView.urlDirAssets}/$confFileName${FragContentWebView.extension}"
                                            navController.navigate(R.id.frag_content_webview)
                                        }

                                        "Apuntes" -> {
                                            UiModalWindows.ApunteManager(context, itemText, null, true)
                                        }
                                    }
                                },
                                onLongClick = {
                                    when (filtroActual) {
                                        "Frases inbuilt favoritas", "Frases personales favoritas", "Frases inbuilt" -> {
                                            UiModalWindows.NotaManager(
                                                context,
                                                utilsDB.getFraseNota(context, itemText),
                                                DatabaseHelper.T_Frases,
                                                DatabaseHelper.C_frases_frase,
                                                itemText
                                            )
                                        }

                                        "Conferencias favoritas", "Conferencias con notas" -> {
                                            UiModalWindows.NotaManager(
                                                context,
                                                utilsDB.getConfNota(context, itemText),
                                                DatabaseHelper.T_Conf,
                                                DatabaseHelper.C_conf_title,
                                                itemText
                                            )
                                        }
                                    }
                                }
                            )
                            .padding(vertical = 10.dp, horizontal = 6.dp)
                    )
                }
            }
        }

        if (itemPendienteQuitarFav != null) {
            AlertDialog(
                onDismissRequest = { itemPendienteQuitarFav = null },
                title = { Text("¿Dejar de ser favorita?") },
                text = { Text(itemPendienteQuitarFav ?: "") },
                confirmButton = {
                    Button(onClick = {
                        val item = itemPendienteQuitarFav ?: return@Button
                        utilsDB.UpdateFavorito(
                            context,
                            DatabaseHelper.T_Frases,
                            DatabaseHelper.C_frases_frase,
                            item,
                            -1
                        )
                        listado.clear()
                        listado.addAll(utilsDB.getListadoTitles(context, filtroActual))
                        itemPendienteQuitarFav = null
                    }) {
                        Text("Quitar favorito")
                    }
                },
                dismissButton = {
                    Button(onClick = { itemPendienteQuitarFav = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}
