package com.ypg.neville.ui.frag

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.fragment.app.Fragment
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.preferences.DbPreferences
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

class FragLienzo : Fragment() {

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (view as ComposeView).setContent {
            com.ypg.neville.ui.theme.NevilleTheme {
                LienzoScreen()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        MainActivity.currentInstance()?.icToolsBarFraseAdd?.visibility = View.GONE
        MainActivity.currentInstance()?.icToolsBarNotaAdd?.visibility = View.GONE
        MainActivity.currentInstance()?.icToolsBarFav?.visibility = View.GONE
    }

    override fun onStop() {
        super.onStop()
        MainActivity.currentInstance()?.icToolsBarFraseAdd?.visibility = View.VISIBLE
        MainActivity.currentInstance()?.icToolsBarNotaAdd?.visibility = View.VISIBLE
    }
}

private enum class LienzoPosicion {
    ARRIBA, ABAJO, DERECHA, IZQUIERDA, CENTRO
}

private enum class LienzoTab {
    FONDO, TEXTO, IMAGEN, EXPORTAR
}

private data class LienzoUiState(
    val textoPrincipal: String = "Imaginar crea la realidad",
    val textoSecundario: String = "Si asumes el sentimiento del deseo cumplido, ningún poder puede impedir su manifestación.",
    val tamanoTextoPrincipal: Float = 24f,
    val tamanoTextoSecundario: Float = 20f,
    val colorTextoPrincipal: Int = android.graphics.Color.BLACK,
    val colorTextoSecundario: Int = android.graphics.Color.BLACK,
    val posicionTextoPrincipal: LienzoPosicion = LienzoPosicion.DERECHA,
    val posicionTextoSecundario: LienzoPosicion = LienzoPosicion.DERECHA,
    val visibilidadTextoSecundario: Boolean = true,
    val tamanoImagenPrincipal: Float = 120f,
    val tamanoImagenSecundaria: Float = 120f,
    val posicionImagenPrincipal: LienzoPosicion = LienzoPosicion.IZQUIERDA,
    val posicionImagenSecundaria: LienzoPosicion = LienzoPosicion.IZQUIERDA,
    val visibilidadImagenPrincipal: Boolean = true,
    val visibilidadImagenSecundaria: Boolean = true,
    val colorFondo1: Int = android.graphics.Color.rgb(242, 237, 235),
    val colorFondo2: Int = android.graphics.Color.rgb(235, 166, 102),
    val colorCustom1: Int = android.graphics.Color.rgb(242, 237, 235),
    val colorCustom2: Int = android.graphics.Color.rgb(235, 166, 102),
    val usarImagenDeFondo: Boolean = false,
    val expandirImagenFondo: Boolean = false
)

private class LienzoStore(private val context: Context) {
    private val prefs = DbPreferences.named(context, "lienzo_prefs")
    private val imageMainFile = File(context.filesDir, "lienzo_imagen_principal.png")
    private val imageSecondFile = File(context.filesDir, "lienzo_imagen_secundaria.png")
    private val imageBgFile = File(context.filesDir, "lienzo_fondo.png")

    fun loadState(): LienzoUiState {
        return LienzoUiState(
            textoPrincipal = prefs.getString("textoPrincipal", "Imaginar crea la realidad").orEmpty(),
            textoSecundario = prefs.getString(
                "textoSecundario",
                "Si asumes el sentimiento del deseo cumplido, ningún poder puede impedir su manifestación."
            ).orEmpty(),
            tamanoTextoPrincipal = prefs.getFloat("tamTextoPrincipal", 24f),
            tamanoTextoSecundario = prefs.getFloat("tamTextoSecundario", 20f),
            colorTextoPrincipal = prefs.getInt("colorTextoPrincipal", android.graphics.Color.BLACK),
            colorTextoSecundario = prefs.getInt("colorTextoSecundario", android.graphics.Color.BLACK),
            posicionTextoPrincipal = prefs.getString("posTextoPrincipal", LienzoPosicion.DERECHA.name)
                ?.let { runCatching { LienzoPosicion.valueOf(it) }.getOrNull() } ?: LienzoPosicion.DERECHA,
            posicionTextoSecundario = prefs.getString("posTextoSecundario", LienzoPosicion.DERECHA.name)
                ?.let { runCatching { LienzoPosicion.valueOf(it) }.getOrNull() } ?: LienzoPosicion.DERECHA,
            visibilidadTextoSecundario = prefs.getBoolean("visTextoSecundario", true),
            tamanoImagenPrincipal = prefs.getFloat("tamImagenPrincipal", 120f),
            tamanoImagenSecundaria = prefs.getFloat("tamImagenSecundaria", 120f),
            posicionImagenPrincipal = prefs.getString("posImagenPrincipal", LienzoPosicion.IZQUIERDA.name)
                ?.let { runCatching { LienzoPosicion.valueOf(it) }.getOrNull() } ?: LienzoPosicion.IZQUIERDA,
            posicionImagenSecundaria = prefs.getString("posImagenSecundaria", LienzoPosicion.IZQUIERDA.name)
                ?.let { runCatching { LienzoPosicion.valueOf(it) }.getOrNull() } ?: LienzoPosicion.IZQUIERDA,
            visibilidadImagenPrincipal = prefs.getBoolean("visImagenPrincipal", true),
            visibilidadImagenSecundaria = prefs.getBoolean("visImagenSecundaria", true),
            colorFondo1 = prefs.getInt("colorFondo1", android.graphics.Color.rgb(242, 237, 235)),
            colorFondo2 = prefs.getInt("colorFondo2", android.graphics.Color.rgb(235, 166, 102)),
            colorCustom1 = prefs.getInt("colorCustom1", android.graphics.Color.rgb(242, 237, 235)),
            colorCustom2 = prefs.getInt("colorCustom2", android.graphics.Color.rgb(235, 166, 102)),
            usarImagenDeFondo = prefs.getBoolean("usarImagenDeFondo", false),
            expandirImagenFondo = prefs.getBoolean("expandirImagenFondo", false)
        )
    }

    fun saveState(state: LienzoUiState) {
        prefs.edit()
            .putString("textoPrincipal", state.textoPrincipal)
            .putString("textoSecundario", state.textoSecundario)
            .putFloat("tamTextoPrincipal", state.tamanoTextoPrincipal)
            .putFloat("tamTextoSecundario", state.tamanoTextoSecundario)
            .putInt("colorTextoPrincipal", state.colorTextoPrincipal)
            .putInt("colorTextoSecundario", state.colorTextoSecundario)
            .putString("posTextoPrincipal", state.posicionTextoPrincipal.name)
            .putString("posTextoSecundario", state.posicionTextoSecundario.name)
            .putBoolean("visTextoSecundario", state.visibilidadTextoSecundario)
            .putFloat("tamImagenPrincipal", state.tamanoImagenPrincipal)
            .putFloat("tamImagenSecundaria", state.tamanoImagenSecundaria)
            .putString("posImagenPrincipal", state.posicionImagenPrincipal.name)
            .putString("posImagenSecundaria", state.posicionImagenSecundaria.name)
            .putBoolean("visImagenPrincipal", state.visibilidadImagenPrincipal)
            .putBoolean("visImagenSecundaria", state.visibilidadImagenSecundaria)
            .putInt("colorFondo1", state.colorFondo1)
            .putInt("colorFondo2", state.colorFondo2)
            .putInt("colorCustom1", state.colorCustom1)
            .putInt("colorCustom2", state.colorCustom2)
            .putBoolean("usarImagenDeFondo", state.usarImagenDeFondo)
            .putBoolean("expandirImagenFondo", state.expandirImagenFondo)
            .apply()
    }

    fun loadPrincipalImageOrDefault(): ImageBitmap {
        val loaded = loadBitmap(imageMainFile)
        if (loaded != null) return loaded.asImageBitmap()
        return BitmapFactory.decodeResource(context.resources, R.drawable.nev_min).asImageBitmap()
    }

    fun loadSecondaryImageOrDefault(): ImageBitmap {
        val loaded = loadBitmap(imageSecondFile)
        if (loaded != null) return loaded.asImageBitmap()
        return BitmapFactory.decodeResource(context.resources, R.drawable.nev_min).asImageBitmap()
    }

    fun loadBackgroundImage(): ImageBitmap? = loadBitmap(imageBgFile)?.asImageBitmap()

    fun savePrincipalImage(bitmap: Bitmap): ImageBitmap {
        saveBitmap(imageMainFile, bitmap)
        return bitmap.asImageBitmap()
    }

    fun saveSecondaryImage(bitmap: Bitmap): ImageBitmap {
        saveBitmap(imageSecondFile, bitmap)
        return bitmap.asImageBitmap()
    }

    fun saveBackgroundImage(bitmap: Bitmap): ImageBitmap {
        saveBitmap(imageBgFile, bitmap)
        return bitmap.asImageBitmap()
    }

    private fun saveBitmap(file: File, bitmap: Bitmap) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    private fun loadBitmap(file: File): Bitmap? {
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }
}

@Composable
private fun LienzoScreen() {
    val context = LocalContext.current
    val store = remember { LienzoStore(context) }
    var state by remember { mutableStateOf(store.loadState()) }
    var tab by remember { mutableStateOf(LienzoTab.FONDO) }

    var imagenPrincipal by remember { mutableStateOf(store.loadPrincipalImageOrDefault()) }
    var imagenSecundaria by remember { mutableStateOf(store.loadSecondaryImageOrDefault()) }
    var imagenFondo by remember { mutableStateOf(store.loadBackgroundImage()) }

    fun update(newState: LienzoUiState) {
        state = newState
        store.saveState(newState)
    }

    val pickImagenPrincipal = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val bmp = uri?.toBitmap(context)
        if (bmp != null) {
            imagenPrincipal = store.savePrincipalImage(bmp)
        }
    }
    val pickImagenSecundaria = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val bmp = uri?.toBitmap(context)
        if (bmp != null) {
            imagenSecundaria = store.saveSecondaryImage(bmp)
        }
    }
    val pickImagenFondo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val bmp = uri?.toBitmap(context)
        if (bmp != null) {
            imagenFondo = store.saveBackgroundImage(bmp)
            update(state.copy(usarImagenDeFondo = true))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        Text(
            text = "Lienzo",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            LienzoPreview(
                state = state,
                imagenPrincipal = imagenPrincipal,
                imagenSecundaria = imagenSecundaria,
                imagenFondo = imagenFondo
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LienzoTabButton("Fondo", tab == LienzoTab.FONDO) { tab = LienzoTab.FONDO }
            LienzoTabButton("Texto", tab == LienzoTab.TEXTO) { tab = LienzoTab.TEXTO }
            LienzoTabButton("Imagen", tab == LienzoTab.IMAGEN) { tab = LienzoTab.IMAGEN }
            LienzoTabButton("Exportar", tab == LienzoTab.EXPORTAR) { tab = LienzoTab.EXPORTAR }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 4.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (tab) {
                LienzoTab.FONDO -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Usar imagen de fondo", modifier = Modifier.weight(1f))
                        Switch(
                            checked = state.usarImagenDeFondo,
                            onCheckedChange = { update(state.copy(usarImagenDeFondo = it)) }
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Expandir imagen a todo el lienzo", modifier = Modifier.weight(1f))
                        Switch(
                            checked = state.expandirImagenFondo,
                            onCheckedChange = { update(state.copy(expandirImagenFondo = it)) }
                        )
                    }
                    Button(onClick = { pickImagenFondo.launch("image/*") }) {
                        Text("Cargar imagen de fondo")
                    }
                    Text("Plantillas de degradado")
                    val presets = remember {
                        listOf(
                            // Tonalidades suaves
                            android.graphics.Color.rgb(242, 237, 235) to android.graphics.Color.rgb(235, 166, 102),
                            android.graphics.Color.rgb(245, 247, 250) to android.graphics.Color.rgb(220, 230, 240),
                            android.graphics.Color.rgb(255, 245, 235) to android.graphics.Color.rgb(255, 220, 190),
                            android.graphics.Color.rgb(232, 246, 240) to android.graphics.Color.rgb(178, 226, 214),
                            android.graphics.Color.rgb(240, 238, 255) to android.graphics.Color.rgb(210, 201, 245),
                            android.graphics.Color.rgb(252, 239, 244) to android.graphics.Color.rgb(240, 205, 220),
                            android.graphics.Color.rgb(230, 246, 255) to android.graphics.Color.rgb(182, 224, 246),
                            android.graphics.Color.rgb(248, 245, 235) to android.graphics.Color.rgb(232, 216, 170),
                            android.graphics.Color.rgb(235, 244, 235) to android.graphics.Color.rgb(201, 224, 190),
                            android.graphics.Color.rgb(250, 238, 228) to android.graphics.Color.rgb(236, 198, 178),
                            // Tonalidades vibrantes
                            android.graphics.Color.rgb(38, 178, 255) to android.graphics.Color.rgb(204, 0, 153),
                            android.graphics.Color.rgb(255, 153, 0) to android.graphics.Color.rgb(204, 0, 76),
                            android.graphics.Color.rgb(8, 242, 204) to android.graphics.Color.rgb(25, 153, 242),
                            android.graphics.Color.rgb(76, 76, 76) to android.graphics.Color.rgb(38, 38, 38),
                            android.graphics.Color.rgb(255, 51, 128) to android.graphics.Color.rgb(255, 230, 0),
                            android.graphics.Color.rgb(114, 40, 255) to android.graphics.Color.rgb(255, 64, 129),
                            android.graphics.Color.rgb(0, 204, 255) to android.graphics.Color.rgb(0, 122, 255),
                            android.graphics.Color.rgb(80, 255, 120) to android.graphics.Color.rgb(0, 190, 110),
                            android.graphics.Color.rgb(255, 80, 80) to android.graphics.Color.rgb(255, 0, 120),
                            android.graphics.Color.rgb(255, 220, 0) to android.graphics.Color.rgb(255, 120, 0),
                            android.graphics.Color.rgb(28, 214, 180) to android.graphics.Color.rgb(17, 87, 240)
                        )
                    }
                    presets.chunked(2).forEach { rowPresets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowPresets.forEach { pair ->
                                val c1 = Color(pair.first)
                                val c2 = Color(pair.second)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(brush = Brush.horizontalGradient(listOf(c1, c2)))
                                        .clickable {
                                            update(
                                                state.copy(
                                                    colorFondo1 = pair.first,
                                                    colorFondo2 = pair.second,
                                                    usarImagenDeFondo = false
                                                )
                                            )
                                        }
                                )
                            }
                            if (rowPresets.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    Text("Colores personalizados")
                    Text("Color 1")
                    ColorPaletteRow(
                        selectedColor = state.colorCustom1,
                        onColorSelected = { c -> update(state.copy(colorCustom1 = c)) }
                    )
                    Text("Color 2")
                    ColorPaletteRow(
                        selectedColor = state.colorCustom2,
                        onColorSelected = { c -> update(state.copy(colorCustom2 = c)) }
                    )
                    Button(onClick = {
                        update(
                            state.copy(
                                colorFondo1 = state.colorCustom1,
                                colorFondo2 = state.colorCustom2,
                                usarImagenDeFondo = false
                            )
                        )
                    }) {
                        Text("Aplicar colores personalizados")
                    }
                }

                LienzoTab.TEXTO -> {
                    OutlinedTextField(
                        value = state.textoPrincipal,
                        onValueChange = { update(state.copy(textoPrincipal = it)) },
                        label = { Text("Texto principal") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    Text("Tamaño texto principal")
                    Slider(
                        value = state.tamanoTextoPrincipal,
                        valueRange = 10f..80f,
                        onValueChange = { update(state.copy(tamanoTextoPrincipal = it)) }
                    )
                    Text("Posición texto principal")
                    PositionSelector(
                        selected = state.posicionTextoPrincipal,
                        onSelected = { update(state.copy(posicionTextoPrincipal = it)) }
                    )
                    Text("Color texto principal")
                    ColorPaletteRow(
                        selectedColor = state.colorTextoPrincipal,
                        onColorSelected = { update(state.copy(colorTextoPrincipal = it)) }
                    )

                    HorizontalDivider()

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Mostrar texto secundario", modifier = Modifier.weight(1f))
                        Switch(
                            checked = state.visibilidadTextoSecundario,
                            onCheckedChange = { update(state.copy(visibilidadTextoSecundario = it)) }
                        )
                    }
                    OutlinedTextField(
                        value = state.textoSecundario,
                        onValueChange = { update(state.copy(textoSecundario = it)) },
                        label = { Text("Texto secundario") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    Text("Tamaño texto secundario")
                    Slider(
                        value = state.tamanoTextoSecundario,
                        valueRange = 10f..80f,
                        onValueChange = { update(state.copy(tamanoTextoSecundario = it)) }
                    )
                    Text("Posición texto secundario")
                    PositionSelector(
                        selected = state.posicionTextoSecundario,
                        onSelected = { update(state.copy(posicionTextoSecundario = it)) }
                    )
                    Text("Color texto secundario")
                    ColorPaletteRow(
                        selectedColor = state.colorTextoSecundario,
                        onColorSelected = { update(state.copy(colorTextoSecundario = it)) }
                    )
                }

                LienzoTab.IMAGEN -> {
                    Text("Imagen principal", fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = { pickImagenPrincipal.launch("image/*") }, modifier = Modifier.weight(1f)) {
                            Text("Cambiar")
                        }
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Visible", modifier = Modifier.weight(1f))
                            Switch(
                                checked = state.visibilidadImagenPrincipal,
                                onCheckedChange = { update(state.copy(visibilidadImagenPrincipal = it)) }
                            )
                        }
                    }
                    Text("Tamaño imagen principal")
                    Slider(
                        value = state.tamanoImagenPrincipal,
                        valueRange = 40f..220f,
                        onValueChange = { update(state.copy(tamanoImagenPrincipal = it)) }
                    )
                    Text("Posición imagen principal")
                    PositionSelector(
                        selected = state.posicionImagenPrincipal,
                        onSelected = { update(state.copy(posicionImagenPrincipal = it)) }
                    )

                    HorizontalDivider()
                    Text("Imagen secundaria", fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = { pickImagenSecundaria.launch("image/*") }, modifier = Modifier.weight(1f)) {
                            Text("Cambiar")
                        }
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Visible", modifier = Modifier.weight(1f))
                            Switch(
                                checked = state.visibilidadImagenSecundaria,
                                onCheckedChange = { update(state.copy(visibilidadImagenSecundaria = it)) }
                            )
                        }
                    }
                    Text("Tamaño imagen secundaria")
                    Slider(
                        value = state.tamanoImagenSecundaria,
                        valueRange = 40f..220f,
                        onValueChange = { update(state.copy(tamanoImagenSecundaria = it)) }
                    )
                    Text("Posición imagen secundaria")
                    PositionSelector(
                        selected = state.posicionImagenSecundaria,
                        onSelected = { update(state.copy(posicionImagenSecundaria = it)) }
                    )
                }

                LienzoTab.EXPORTAR -> {
                    Text("Exportar imagen del lienzo", fontWeight = FontWeight.SemiBold)
                    Button(onClick = {
                        val bitmap = renderLienzoBitmap(
                            state = state,
                            imagenPrincipal = imagenPrincipal.asAndroidBitmap(),
                            imagenSecundaria = imagenSecundaria.asAndroidBitmap(),
                            imagenFondo = imagenFondo?.asAndroidBitmap()
                        )
                        val uri = saveBitmapToGallery(context, bitmap, "lienzo_neville")
                        if (uri != null) {
                            Toast.makeText(context, "Imagen guardada en galería", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "No se pudo guardar la imagen", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("Guardar en galería")
                    }
                    Button(onClick = {
                        val bitmap = renderLienzoBitmap(
                            state = state,
                            imagenPrincipal = imagenPrincipal.asAndroidBitmap(),
                            imagenSecundaria = imagenSecundaria.asAndroidBitmap(),
                            imagenFondo = imagenFondo?.asAndroidBitmap()
                        )
                        val uri = saveBitmapToCacheAndGetUri(context, bitmap)
                        if (uri != null) {
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "image/png"
                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                android.content.Intent.createChooser(shareIntent, "Compartir lienzo")
                            )
                        } else {
                            Toast.makeText(context, "No se pudo preparar la imagen para compartir", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("Compartir")
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun LienzoPreview(
    state: LienzoUiState,
    imagenPrincipal: ImageBitmap,
    imagenSecundaria: ImageBitmap,
    imagenFondo: ImageBitmap?
) {
    val topItems = rememberPreviewItems(state, LienzoPosicion.ARRIBA)
    val leftItems = rememberPreviewItems(state, LienzoPosicion.IZQUIERDA)
    val centerItems = rememberPreviewItems(state, LienzoPosicion.CENTRO)
    val rightItems = rememberPreviewItems(state, LienzoPosicion.DERECHA)
    val bottomItems = rememberPreviewItems(state, LienzoPosicion.ABAJO)

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.usarImagenDeFondo && imagenFondo != null) {
            Image(
                bitmap = imagenFondo,
                contentDescription = null,
                contentScale = if (state.expandirImagenFondo) ContentScale.Crop else ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            listOf(Color(state.colorFondo1), Color(state.colorFondo2))
                        )
                    )
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                LienzoItemList(topItems, state, imagenPrincipal, imagenSecundaria)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leftItems.isNotEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LienzoItemList(leftItems, state, imagenPrincipal, imagenSecundaria)
                    }
                }
                if (leftItems.isNotEmpty() && (centerItems.isNotEmpty() || rightItems.isNotEmpty())) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (centerItems.isNotEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LienzoItemList(centerItems, state, imagenPrincipal, imagenSecundaria)
                    }
                }
                if (centerItems.isNotEmpty() && rightItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (rightItems.isNotEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LienzoItemList(rightItems, state, imagenPrincipal, imagenSecundaria)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                LienzoItemList(bottomItems, state, imagenPrincipal, imagenSecundaria)
            }
        }
    }
}

@Composable
private fun LienzoItemList(
    items: List<String>,
    state: LienzoUiState,
    imagenPrincipal: ImageBitmap,
    imagenSecundaria: ImageBitmap
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        items.forEach { key ->
            when (key) {
                "img1" -> Image(
                    bitmap = imagenPrincipal,
                    contentDescription = null,
                    modifier = Modifier
                        .size(state.tamanoImagenPrincipal.dp.coerceIn(36.dp, 160.dp))
                        .clip(RoundedCornerShape(8.dp))
                )

                "img2" -> Image(
                    bitmap = imagenSecundaria,
                    contentDescription = null,
                    modifier = Modifier
                        .size(state.tamanoImagenSecundaria.dp.coerceIn(36.dp, 160.dp))
                        .clip(RoundedCornerShape(8.dp))
                )

                "txt1" -> Text(
                    text = state.textoPrincipal,
                    color = Color(state.colorTextoPrincipal),
                    fontSize = state.tamanoTextoPrincipal.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 5
                )

                "txt2" -> Text(
                    text = state.textoSecundario,
                    color = Color(state.colorTextoSecundario),
                    fontSize = state.tamanoTextoSecundario.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 5
                )
            }
        }
    }
}

private fun rememberPreviewItems(state: LienzoUiState, pos: LienzoPosicion): List<String> {
    val items = mutableListOf<String>()
    when (pos) {
        LienzoPosicion.ARRIBA -> {
            if (state.visibilidadImagenSecundaria && state.posicionImagenSecundaria == pos) items += "img2"
            if (state.visibilidadImagenPrincipal && state.posicionImagenPrincipal == pos) items += "img1"
            if (state.posicionTextoPrincipal == pos) items += "txt1"
            if (state.visibilidadTextoSecundario && state.posicionTextoSecundario == pos) items += "txt2"
        }

        LienzoPosicion.IZQUIERDA, LienzoPosicion.DERECHA -> {
            if (state.visibilidadImagenPrincipal && state.posicionImagenPrincipal == pos) items += "img1"
            if (state.visibilidadImagenSecundaria && state.posicionImagenSecundaria == pos) items += "img2"
            if (state.posicionTextoPrincipal == pos) items += "txt1"
            if (state.visibilidadTextoSecundario && state.posicionTextoSecundario == pos) items += "txt2"
        }

        LienzoPosicion.CENTRO -> {
            if (state.posicionTextoPrincipal == pos) items += "txt1"
            if (state.visibilidadTextoSecundario && state.posicionTextoSecundario == pos) items += "txt2"
        }

        LienzoPosicion.ABAJO -> {
            if (state.posicionTextoPrincipal == pos) items += "txt1"
            if (state.visibilidadTextoSecundario && state.posicionTextoSecundario == pos) items += "txt2"
            if (state.visibilidadImagenSecundaria && state.posicionImagenSecundaria == pos) items += "img2"
            if (state.visibilidadImagenPrincipal && state.posicionImagenPrincipal == pos) items += "img1"
        }
    }
    return items
}

@Composable
private fun LienzoTabButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color(0xFFFFA500) else Color(0xFF606060))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text = text, color = if (selected) Color.Black else Color.White)
    }
}

@Composable
private fun PositionSelector(selected: LienzoPosicion, onSelected: (LienzoPosicion) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LienzoPosicion.entries.forEach { pos ->
            val isSelected = selected == pos
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Color(0xFF2FAEF8) else Color(0xFF454545))
                    .clickable { onSelected(pos) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(pos.name.lowercase().replaceFirstChar { it.uppercase() }, color = Color.White)
            }
        }
    }
}

@Composable
private fun ColorPaletteRow(selectedColor: Int, onColorSelected: (Int) -> Unit) {
    val colors = listOf(
        android.graphics.Color.BLACK,
        android.graphics.Color.WHITE,
        android.graphics.Color.RED,
        android.graphics.Color.GREEN,
        android.graphics.Color.BLUE,
        android.graphics.Color.CYAN,
        android.graphics.Color.MAGENTA,
        android.graphics.Color.YELLOW,
        android.graphics.Color.rgb(255, 153, 0),
        android.graphics.Color.rgb(120, 120, 120),
        android.graphics.Color.rgb(25, 153, 242),
        android.graphics.Color.rgb(114, 40, 153)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        colors.forEach { value ->
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(value))
                    .border(
                        width = if (value == selectedColor) 3.dp else 1.dp,
                        color = if (value == selectedColor) Color.White else Color(0x66000000),
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(value) }
            )
        }
    }
}

private fun Uri.toBitmap(context: Context): Bitmap? {
    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = android.graphics.ImageDecoder.createSource(context.contentResolver, this)
            android.graphics.ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, this)
        }
    }.getOrNull()
}

private fun renderLienzoBitmap(
    state: LienzoUiState,
    imagenPrincipal: Bitmap,
    imagenSecundaria: Bitmap,
    imagenFondo: Bitmap?,
    width: Int = 1200,
    height: Int = 900
): Bitmap {
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)

    if (state.usarImagenDeFondo && imagenFondo != null) {
        if (state.expandirImagenFondo) {
            drawBitmapCover(canvas, imagenFondo, RectF(0f, 0f, width.toFloat(), height.toFloat()))
        } else {
            drawBitmapFit(canvas, imagenFondo, RectF(0f, 0f, width.toFloat(), height.toFloat()))
        }
    } else {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                state.colorFondo1,
                state.colorFondo2,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }

    val cellW = width / 3f
    val cellH = height / 3f
    val topRect = RectF(cellW, 0f, cellW * 2f, cellH)
    val bottomRect = RectF(cellW, cellH * 2f, cellW * 2f, cellH * 3f)

    val drawMap = mutableMapOf<LienzoPosicion, MutableList<String>>()
    LienzoPosicion.entries.forEach { drawMap[it] = mutableListOf() }

    fun addByOrder(pos: LienzoPosicion, key: String) {
        drawMap[pos]?.add(key)
    }

    if (state.visibilidadImagenSecundaria) addByOrder(state.posicionImagenSecundaria, "img2_top")
    if (state.visibilidadImagenPrincipal) addByOrder(state.posicionImagenPrincipal, "img1_top")
    addByOrder(state.posicionTextoPrincipal, "txt1")
    if (state.visibilidadTextoSecundario) addByOrder(state.posicionTextoSecundario, "txt2")

    val bottom = drawMap[LienzoPosicion.ABAJO].orEmpty().toMutableList()
    drawMap[LienzoPosicion.ABAJO]?.clear()
    if ("txt1" in bottom) drawMap[LienzoPosicion.ABAJO]?.add("txt1")
    if ("txt2" in bottom) drawMap[LienzoPosicion.ABAJO]?.add("txt2")
    if ("img2_top" in bottom) drawMap[LienzoPosicion.ABAJO]?.add("img2_top")
    if ("img1_top" in bottom) drawMap[LienzoPosicion.ABAJO]?.add("img1_top")

    drawStackInRect(
        canvas = canvas,
        rect = topRect,
        itemKeys = drawMap[LienzoPosicion.ARRIBA].orEmpty(),
        state = state,
        imagenPrincipal = imagenPrincipal,
        imagenSecundaria = imagenSecundaria,
        scaleFactor = width / 340f
    )

    val middleEntries = listOf(
        LienzoPosicion.IZQUIERDA to drawMap[LienzoPosicion.IZQUIERDA].orEmpty(),
        LienzoPosicion.CENTRO to drawMap[LienzoPosicion.CENTRO].orEmpty(),
        LienzoPosicion.DERECHA to drawMap[LienzoPosicion.DERECHA].orEmpty()
    ).filter { it.second.isNotEmpty() }

    if (middleEntries.isNotEmpty()) {
        val gap = width * 0.014f
        val horizontalPadding = width * 0.16f
        val availableW = width - (horizontalPadding * 2f) - (gap * (middleEntries.size - 1))
        val blockW = availableW / middleEntries.size
        val startX = (width - (blockW * middleEntries.size + gap * (middleEntries.size - 1))) / 2f
        middleEntries.forEachIndexed { index, entry ->
            val left = startX + index * (blockW + gap)
            val rect = RectF(left, cellH, left + blockW, cellH * 2f)
            drawStackInRect(
                canvas = canvas,
                rect = rect,
                itemKeys = entry.second,
                state = state,
                imagenPrincipal = imagenPrincipal,
                imagenSecundaria = imagenSecundaria,
                scaleFactor = width / 340f
            )
        }
    }

    drawStackInRect(
        canvas = canvas,
        rect = bottomRect,
        itemKeys = drawMap[LienzoPosicion.ABAJO].orEmpty(),
        state = state,
        imagenPrincipal = imagenPrincipal,
        imagenSecundaria = imagenSecundaria,
        scaleFactor = width / 340f
    )

    return bitmap
}

private fun drawStackInRect(
    canvas: Canvas,
    rect: RectF,
    itemKeys: List<String>,
    state: LienzoUiState,
    imagenPrincipal: Bitmap,
    imagenSecundaria: Bitmap,
    scaleFactor: Float
) {
    if (itemKeys.isEmpty()) return
    val spacing = 6f
    val maxWidth = rect.width() - 16f
    val heights = itemKeys.map { key ->
        when (key) {
            "img1_top" -> state.tamanoImagenPrincipal * scaleFactor
            "img2_top" -> state.tamanoImagenSecundaria * scaleFactor
            "txt1" -> estimateTextHeight(state.textoPrincipal, state.tamanoTextoPrincipal * scaleFactor, maxWidth)
            "txt2" -> estimateTextHeight(state.textoSecundario, state.tamanoTextoSecundario * scaleFactor, maxWidth)
            else -> 0f
        }
    }
    val totalHeight = heights.sum() + spacing * max(0, itemKeys.size - 1)
    var y = rect.top + max(0f, (rect.height() - totalHeight) / 2f)

    itemKeys.forEach { key ->
        when (key) {
            "img1_top" -> {
                val size = min(state.tamanoImagenPrincipal * scaleFactor, rect.width() - 12f)
                val left = rect.centerX() - size / 2f
                drawBitmapFit(canvas, imagenPrincipal, RectF(left, y, left + size, y + size))
                y += size + spacing
            }

            "img2_top" -> {
                val size = min(state.tamanoImagenSecundaria * scaleFactor, rect.width() - 12f)
                val left = rect.centerX() - size / 2f
                drawBitmapFit(canvas, imagenSecundaria, RectF(left, y, left + size, y + size))
                y += size + spacing
            }

            "txt1" -> {
                val h = drawCenteredTextBlock(
                    canvas = canvas,
                    text = state.textoPrincipal,
                    color = state.colorTextoPrincipal,
                    textSizePx = state.tamanoTextoPrincipal * scaleFactor,
                    left = rect.left + 8f,
                    top = y,
                    width = maxWidth
                )
                y += h + spacing
            }

            "txt2" -> {
                val h = drawCenteredTextBlock(
                    canvas = canvas,
                    text = state.textoSecundario,
                    color = state.colorTextoSecundario,
                    textSizePx = state.tamanoTextoSecundario * scaleFactor,
                    left = rect.left + 8f,
                    top = y,
                    width = maxWidth
                )
                y += h + spacing
            }
        }
    }
}

private fun estimateTextHeight(text: String, textSizePx: Float, width: Float): Float {
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = textSizePx }
    val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt())
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setIncludePad(false)
        .build()
    return layout.height.toFloat()
}

private fun drawCenteredTextBlock(
    canvas: Canvas,
    text: String,
    color: Int,
    textSizePx: Float,
    left: Float,
    top: Float,
    width: Float
): Float {
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.textSize = textSizePx
    }
    val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt())
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setIncludePad(false)
        .build()
    canvas.save()
    canvas.translate(left, top)
    layout.draw(canvas)
    canvas.restore()
    return layout.height.toFloat()
}

private fun drawBitmapFit(canvas: Canvas, bitmap: Bitmap, target: RectF) {
    val srcW = bitmap.width.toFloat()
    val srcH = bitmap.height.toFloat()
    val dstW = target.width()
    val dstH = target.height()
    val scale = min(dstW / srcW, dstH / srcH)
    val finalW = srcW * scale
    val finalH = srcH * scale
    val left = target.left + (dstW - finalW) / 2f
    val top = target.top + (dstH - finalH) / 2f
    canvas.drawBitmap(bitmap, null, RectF(left, top, left + finalW, top + finalH), null)
}

private fun drawBitmapCover(canvas: Canvas, bitmap: Bitmap, target: RectF) {
    val srcW = bitmap.width.toFloat()
    val srcH = bitmap.height.toFloat()
    val dstW = target.width()
    val dstH = target.height()
    val scale = max(dstW / srcW, dstH / srcH)
    val finalW = srcW * scale
    val finalH = srcH * scale
    val left = target.left + (dstW - finalW) / 2f
    val top = target.top + (dstH - finalH) / 2f
    canvas.drawBitmap(bitmap, null, RectF(left, top, left + finalW, top + finalH), null)
}

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, baseName: String): Uri? {
    val resolver = context.contentResolver
    val now = System.currentTimeMillis()
    val filename = "${baseName}_$now.png"
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Neville")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    val uri = resolver.insert(collection, contentValues) ?: return null
    return try {
        resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver.update(uri, ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }, null, null)
        }
        uri
    } catch (_: Throwable) {
        resolver.delete(uri, null, null)
        null
    }
}

private fun saveBitmapToCacheAndGetUri(context: Context, bitmap: Bitmap): Uri? {
    return saveBitmapToGallery(context, bitmap, "lienzo_compartir")
}
