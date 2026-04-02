package com.ypg.neville.ui.frag

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.ypg.neville.R
import com.ypg.neville.model.utils.ColorPickerManager
import com.ypg.neville.model.utils.GetFromRepo
import com.ypg.neville.model.utils.UiModalWindows

class frag_Setting : Fragment() {

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    SettingsScreen()
                }
            }
        }
    }

    @Composable
    private fun SettingsScreen() {
        val context = LocalContext.current
        val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

        var temaNoche by remember { mutableStateOf(prefs.getBoolean("tema", true)) }
        var fuenteFrase by remember { mutableStateOf(prefs.getString("fuente_frase", "28") ?: "28") }
        var fuenteConf by remember { mutableStateOf(prefs.getString("fuente_conf", "170") ?: "170") }

        val acciones = listOf(
            "color_marcos" to "Color de Marcos",
            "color_letra_frases" to "Color de letra en frases",
            "donar" to "Donar",
            "update_frases_from_web" to "Actualizar frases desde web",
            "write_comment" to "Escribir comentario",
            "web_site" to "Sitio web",
            "resena_app" to "Escribir reseña",
            "show_news" to "Ver novedades"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tema oscuro")
                    Switch(
                        checked = temaNoche,
                        onCheckedChange = {
                            temaNoche = it
                            prefs.edit { putBoolean("tema", it) }
                            activity?.recreate()
                        }
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fuenteFrase,
                        onValueChange = {
                            fuenteFrase = it.filter { c -> c.isDigit() }.ifBlank { "28" }.take(2)
                            val value = fuenteFrase.toIntOrNull()?.let { n -> if (n >= 40) 28 else n } ?: 28
                            prefs.edit { putString("fuente_frase", value.toString()) }
                        },
                        label = { Text("Tamaño fuente frases") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = fuenteConf,
                        onValueChange = {
                            fuenteConf = it.filter { c -> c.isDigit() }.ifBlank { "170" }
                            prefs.edit { putString("fuente_conf", fuenteConf) }
                        },
                        label = { Text("Zoom fuente contenido") },
                        singleLine = true
                    )
                }
            }

            items(acciones) { (id, label) ->
                Text(
                    text = label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            when (id) {
                                "color_marcos" -> ColorPickerManager.showColorPicker(context, 0, "color_marcos", "Color de Marcos")
                                "color_letra_frases" -> ColorPickerManager.showColorPicker(context, 0, "color_letra_frases", "Color de letra en frases")
                                "donar" -> startActivity(Intent(Intent.ACTION_VIEW, "https://projectsypg.mozello.com/donar/".toUri()))
                                "update_frases_from_web" -> {
                                    GetFromRepo.getFrasesFromWeb(context)
                                    Toast.makeText(context, "El compendio de frases se esta actualizando", Toast.LENGTH_SHORT).show()
                                }
                                "write_comment" -> startActivity(Intent(Intent.ACTION_VIEW, "https://projectsypg.mozello.com/contacto/".toUri()))
                                "web_site" -> startActivity(Intent(Intent.ACTION_VIEW, "https://projectsypg.mozello.com/".toUri()))
                                "resena_app" -> {
                                    val uri = "market://details?id=${context.packageName}".toUri()
                                    val myAppLinkToMarket = Intent(Intent.ACTION_VIEW, uri)
                                    try {
                                        startActivity(myAppLinkToMarket)
                                    } catch (_: ActivityNotFoundException) {
                                        Toast.makeText(context, "unable to find market app", Toast.LENGTH_LONG).show()
                                    }
                                }
                                "show_news" -> {
                                    UiModalWindows.showAyudaContectual(
                                        context,
                                        "Novedades",
                                        "Que hay de nuevo?",
                                        getString(R.string.news),
                                        false,
                                        AppCompatResources.getDrawable(context, R.drawable.neville)
                                    )
                                }
                            }
                        }
                        .padding(vertical = 10.dp)
                )
            }
        }
    }
}
