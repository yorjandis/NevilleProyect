package com.ypg.neville.ui.frag

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.ypg.neville.R
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.model.utils.ColorPickerManager
import com.ypg.neville.model.utils.UiModalWindows

class frag_Setting : Fragment() {

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                com.ypg.neville.ui.theme.NevilleTheme {
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
        var fuenteFrase by remember { mutableStateOf((prefs.getString("fuente_frase", "28")?.toIntOrNull() ?: 28).coerceIn(14, 40)) }
        var fuenteListados by remember { mutableStateOf((prefs.getString("fuente_listados", "22")?.toIntOrNull() ?: 22).coerceIn(12, 40)) }
        var fuenteConf by remember { mutableStateOf((prefs.getString("fuente_conf", "170")?.toIntOrNull() ?: 170).coerceIn(100, 250)) }
        var filterAutorNeville by remember { mutableStateOf(prefs.getBoolean("home_filter_author_neville", true)) }
        var filterAutorJoe by remember { mutableStateOf(prefs.getBoolean("home_filter_author_joe", true)) }
        var filterAutorGregg by remember { mutableStateOf(prefs.getBoolean("home_filter_author_gregg", true)) }
        var filterAutorBruce by remember { mutableStateOf(prefs.getBoolean("home_filter_author_bruce", true)) }
        var filterOtros by remember { mutableStateOf(prefs.getBoolean("home_filter_otros", true)) }
        var filterSalud by remember { mutableStateOf(prefs.getBoolean("home_filter_salud", true)) }

        fun isAnyFilterEnabled(
            nev: Boolean = filterAutorNeville,
            joe: Boolean = filterAutorJoe,
            gregg: Boolean = filterAutorGregg,
            bruce: Boolean = filterAutorBruce,
            otros: Boolean = filterOtros,
            salud: Boolean = filterSalud
        ): Boolean = nev || joe || gregg || bruce || otros || salud

        fun saveAuthorsAggregate() {
            prefs.edit {
                putBoolean(
                    "home_filter_autores",
                    filterAutorNeville || filterAutorJoe || filterAutorGregg || filterAutorBruce
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Ajustes",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Personaliza apariencia, tamaños y contenido de inicio.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                SettingSection(
                    title = "Apariencia",
                    subtitle = "Tema y colores del contenido"
                ) {
                    SwitchField(
                        title = "Tema oscuro",
                        description = "Activa o desactiva el modo oscuro de la aplicación",
                        checked = temaNoche,
                        onCheckedChange = {
                            temaNoche = it
                            prefs.edit { putBoolean("tema", it) }
                            AppCompatDelegate.setDefaultNightMode(
                                if (it) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                            )
                            activity?.recreate()
                        }
                    )
                    FieldDivider()
                    ActionField(
                        title = "Color de fuente en Inicio",
                        description = "Color del texto de frase en FragHome"
                    ) {
                        ColorPickerManager.showColorPicker(
                            context,
                            prefs.getInt("color_letra_frases_home", prefs.getInt("color_letra_frases", 0xFF1F2A37.toInt())),
                            "color_letra_frases_home",
                            "Color de fuente en Inicio"
                        )
                    }
                    FieldDivider()
                    ActionField(
                        title = "Color de Fondo A",
                        description = "Primer color del degradado en Inicio"
                    ) {
                        ColorPickerManager.showColorPicker(
                            context,
                            prefs.getInt("color_fondo_a", 0xFFF3F5F9.toInt()),
                            "color_fondo_a",
                            "Color de Fondo A"
                        )
                    }
                    FieldDivider()
                    ActionField(
                        title = "Color de Fondo B",
                        description = "Segundo color del degradado en Inicio"
                    ) {
                        ColorPickerManager.showColorPicker(
                            context,
                            prefs.getInt("color_fondo_b", 0xFFE2E7F0.toInt()),
                            "color_fondo_b",
                            "Color de Fondo B"
                        )
                    }
                }
            }

            item {
                SettingSection(
                    title = "Tamaño de Letra",
                    subtitle = "Ajuste independiente por tipo de contenido"
                ) {
                    SliderField(
                        title = "Texto de Frases",
                        description = "Inicio y vistas de autor",
                        value = fuenteFrase,
                        range = 14..40
                    ) {
                        fuenteFrase = it
                        prefs.edit { putString("fuente_frase", it.toString()) }
                    }
                    FieldDivider()
                    SliderField(
                        title = "Listados",
                        description = "Conferencias, Enciclopedia, Evidencia y Reflexiones",
                        value = fuenteListados,
                        range = 12..40
                    ) {
                        fuenteListados = it
                        prefs.edit { putString("fuente_listados", it.toString()) }
                    }
                    FieldDivider()
                    SliderField(
                        title = "Zoom de contenido",
                        description = "Visor interno de texto",
                        value = fuenteConf,
                        range = 100..250
                    ) {
                        fuenteConf = it
                        prefs.edit { putString("fuente_conf", it.toString()) }
                    }
                }
            }

            item {
                SettingSection(
                    title = "Frases en Inicio",
                    subtitle = "Selecciona exactamente qué categorías y autores mostrar"
                ) {
                    Text(
                        text = "Autores",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    SwitchField(
                        title = "Neville Goddard",
                        description = "Mostrar frases de Neville en Inicio",
                        checked = filterAutorNeville
                    ) { newValue ->
                        if (!isAnyFilterEnabled(nev = newValue)) {
                            Toast.makeText(context, "Debe quedar al menos un filtro activo", Toast.LENGTH_SHORT).show()
                            return@SwitchField
                        }
                        filterAutorNeville = newValue
                        prefs.edit { putBoolean("home_filter_author_neville", newValue) }
                        saveAuthorsAggregate()
                    }
                    FieldDivider()
                    SwitchField(
                        title = "Joe Dispenza",
                        description = "Mostrar frases de Joe en Inicio",
                        checked = filterAutorJoe
                    ) { newValue ->
                        if (!isAnyFilterEnabled(joe = newValue)) {
                            Toast.makeText(context, "Debe quedar al menos un filtro activo", Toast.LENGTH_SHORT).show()
                            return@SwitchField
                        }
                        filterAutorJoe = newValue
                        prefs.edit { putBoolean("home_filter_author_joe", newValue) }
                        saveAuthorsAggregate()
                    }
                    FieldDivider()
                    SwitchField(
                        title = "Gregg Braden",
                        description = "Mostrar frases de Gregg en Inicio",
                        checked = filterAutorGregg
                    ) { newValue ->
                        if (!isAnyFilterEnabled(gregg = newValue)) {
                            Toast.makeText(context, "Debe quedar al menos un filtro activo", Toast.LENGTH_SHORT).show()
                            return@SwitchField
                        }
                        filterAutorGregg = newValue
                        prefs.edit { putBoolean("home_filter_author_gregg", newValue) }
                        saveAuthorsAggregate()
                    }
                    FieldDivider()
                    SwitchField(
                        title = "Bruce Lipton",
                        description = "Mostrar frases de Bruce en Inicio",
                        checked = filterAutorBruce
                    ) { newValue ->
                        if (!isAnyFilterEnabled(bruce = newValue)) {
                            Toast.makeText(context, "Debe quedar al menos un filtro activo", Toast.LENGTH_SHORT).show()
                            return@SwitchField
                        }
                        filterAutorBruce = newValue
                        prefs.edit { putBoolean("home_filter_author_bruce", newValue) }
                        saveAuthorsAggregate()
                    }

                    FieldDivider(padding = 8.dp)
                    Text(
                        text = "Categorías",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    SwitchField(
                        title = "Otros autores",
                        description = "Incluir frases de autores diversos",
                        checked = filterOtros
                    ) { newValue ->
                        if (!isAnyFilterEnabled(otros = newValue)) {
                            Toast.makeText(context, "Debe quedar al menos un filtro activo", Toast.LENGTH_SHORT).show()
                            return@SwitchField
                        }
                        filterOtros = newValue
                        prefs.edit { putBoolean("home_filter_otros", newValue) }
                    }
                    FieldDivider()
                    SwitchField(
                        title = "Salud",
                        description = "Incluir tips y frases de salud",
                        checked = filterSalud
                    ) { newValue ->
                        if (!isAnyFilterEnabled(salud = newValue)) {
                            Toast.makeText(context, "Debe quedar al menos un filtro activo", Toast.LENGTH_SHORT).show()
                            return@SwitchField
                        }
                        filterSalud = newValue
                        prefs.edit { putBoolean("home_filter_salud", newValue) }
                    }
                }
            }

            item {
                SettingSection(
                    title = "Contenido",
                    subtitle = "Sincronización y mantenimiento"
                ) {
                    ActionField(
                        title = "Actualizar frases desde archivos",
                        description = "Reimporta las frases desde assets/frases"
                    ) {
                        val updated = utilsDB.forceRefreshFrasesFromAssets(context)
                        Toast.makeText(
                            context,
                            if (updated) "Frases actualizadas desde assets" else "No hubo cambios en frases",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            item {
                SettingSection(
                    title = "Proyecto y Soporte",
                    subtitle = "Acciones de contacto, reseña y novedades"
                ) {
                    ActionField("Enviar comentario", "Contactar con el desarrollador") {
                        startActivity(Intent(Intent.ACTION_VIEW, "https://projectsypg.mozello.com/contacto/".toUri()))
                    }
                    FieldDivider()
                    ActionField("Sitio web del proyecto", "Abrir web oficial") {
                        startActivity(Intent(Intent.ACTION_VIEW, "https://projectsypg.mozello.com/".toUri()))
                    }
                    FieldDivider()
                    ActionField("Escribir reseña", "Valorar la app en Google Play") {
                        val uri = "market://details?id=${context.packageName}".toUri()
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        try {
                            startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(context, "No se encontró la app de tienda", Toast.LENGTH_LONG).show()
                        }
                    }
                    FieldDivider()
                    ActionField("Ver novedades", "Consultar cambios de versión") {
                        UiModalWindows.showAyudaContectual(
                            context,
                            "Novedades",
                            "Que hay de nuevo?",
                            getString(R.string.news),
                            false,
                            AppCompatResources.getDrawable(context, R.drawable.neville)
                        )
                    }
                    FieldDivider()
                    ActionField("Donar", "Apoyar el crecimiento del proyecto") {
                        startActivity(Intent(Intent.ACTION_VIEW, "https://projectsypg.mozello.com/donar/".toUri()))
                    }
                }
            }
        }
    }

    @Composable
    private fun SettingSection(
        title: String,
        subtitle: String,
        content: @Composable ColumnScope.() -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider()
                content()
            }
        }
    }

    @Composable
    private fun SwitchField(
        title: String,
        description: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }

    @Composable
    private fun SliderField(
        title: String,
        description: String,
        value: Int,
        range: IntRange,
        onValueChange: (Int) -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt().coerceIn(range.first, range.last)) },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                steps = (range.last - range.first - 1).coerceAtLeast(0)
            )
        }
    }

    @Composable
    private fun ActionField(
        title: String,
        description: String,
        onClick: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    @Composable
    private fun FieldDivider(padding: androidx.compose.ui.unit.Dp = 6.dp) {
        HorizontalDivider(modifier = Modifier.padding(vertical = padding))
    }
}
