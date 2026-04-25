package com.ypg.neville.feature.emotionalanchors.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.Fragment
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.feature.emotionalanchors.data.EmotionalAnchor
import com.ypg.neville.feature.emotionalanchors.data.RoomEmotionalAnchorStore
import com.ypg.neville.feature.emotionalanchors.domain.EmotionalAnchorsController
import com.ypg.neville.feature.emotionalanchors.ui.FragEmotionalAnchorRun.Companion.ARG_ANCHOR_ID
import com.ypg.neville.feature.voice.media.AndroidVoicePlayerEngine
import com.ypg.neville.feature.voice.media.AndroidVoiceRecorderEngine
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class FragEmotionalAnchorsList : Fragment() {

    private val dbExecutor = Executors.newSingleThreadExecutor()
    private lateinit var controller: EmotionalAnchorsController

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = NevilleRoomDatabase.getInstance(requireContext())
        controller = EmotionalAnchorsController(
            store = RoomEmotionalAnchorStore(db.emotionalAnchorDao()),
            recorder = AndroidVoiceRecorderEngine(requireContext()),
            player = AndroidVoicePlayerEngine(),
            audioDirectory = File(requireContext().filesDir, "emotional_anchors/audio")
        )

        (view as ComposeView).setContent {
            com.ypg.neville.ui.theme.NevilleTheme {
                EmotionalAnchorsListScreen(controller = controller)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        controller.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        dbExecutor.shutdown()
    }

    @Composable
    private fun EmotionalAnchorsListScreen(controller: EmotionalAnchorsController) {
        val anchors = remember { mutableStateListOf<EmotionalAnchor>() }
        var reloadTrigger by remember { mutableStateOf(0) }
        var deleteTarget by remember { mutableStateOf<EmotionalAnchor?>(null) }
        var showHelpDialog by remember { mutableStateOf(false) }

        fun reload() {
            dbExecutor.execute {
                val list = controller.list()
                activity?.runOnUiThread {
                    anchors.clear()
                    anchors.addAll(list)
                }
            }
        }

        LaunchedEffect(reloadTrigger) {
            reload()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF0CD55),
                            Color(0xFF86BD60),
                            Color(0xFFA8AF7A)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Anclas Emocionales",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    TextButton(onClick = { showHelpDialog = true }) {
                        Text("Ayuda", color = Color(0xFF455A64))
                    }
                }

                Text(
                    text = "Entrena tu cerebro y respuesta emocional lanzando repetidamente un ancla en momentos positivos. " +
                            "Luego, en momentos de estres,  utiliza el ancla para afrontar la situación.",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF000000),
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))
                /*
                Text(
                    text = "Mis anclas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                 */


                Spacer(modifier = Modifier.height(6.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp)
                ) {
                    items(items = anchors, key = { it.id }) { anchor ->
                        EmotionalAnchorItem(
                            anchor = anchor,
                            onLaunch = {
                                val args = Bundle().apply { putLong(ARG_ANCHOR_ID, anchor.id) }
                                MainActivity.currentInstance()?.openDestinationAsSheet(R.id.frag_emotional_anchor_run, args)
                            },
                            onDelete = {
                                deleteTarget = anchor
                            },
                            onEdit = {
                                val args = Bundle().apply {
                                    putLong(FragEmotionalAnchors.ARG_EDIT_ANCHOR_ID, anchor.id)
                                }
                                MainActivity.currentInstance()?.openDestinationAsSheet(R.id.frag_emotional_anchor_create, args)
                            }
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = { MainActivity.currentInstance()?.openDestinationAsSheet(R.id.frag_emotional_anchor_create) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp),
                containerColor = Color(0xFF1A4D8F)
            ) {
                Text("+", color = Color.White, style = MaterialTheme.typography.headlineSmall)
            }
        }

        deleteTarget?.let { target ->
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text("Eliminar ancla") },
                text = { Text("¿Seguro que deseas eliminar esta Ancla Emocional? Esta acción no se puede deshacer.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            deleteTarget = null
                            dbExecutor.execute {
                                controller.deleteAnchor(target)
                                activity?.runOnUiThread {
                                    reloadTrigger++
                                }
                            }
                        }
                    ) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTarget = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (showHelpDialog) {
            Dialog(
                onDismissRequest = { showHelpDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .border(1.dp, Color(0xFF90A4AE), RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF7FAFC)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFFE0B2),
                                        Color(0xFFBBDEFB),
                                        Color(0xFFC8E6C9)
                                    )
                                )
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "¿Qué son las Anclas Emocionales?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                "Un ancla emocional es una técnica psicológica que consiste en asociar un estímulo concreto (una palabra, imagen, sonido o gesto) con un estado emocional positivo, para poder activarlo voluntariamente cuando lo necesites, especialmente en situaciones de estrés.\n\n" +
                                    "🔹 ¿Cómo funciona?\n\n" +
                                    "El cerebro aprende por asociación. Si repites varias veces una combinación específica mientras estás en calma o motivado, ese “paquete” se convierte en un acceso rápido a ese estado emocional.\n\n" +
                                    "🔹 Componentes de un ancla emocional\n\n" +
                                    "Una ancla efectiva suele combinar varios elementos:\n" +
                                    "• Frase: corta y potente (ej. “Estoy en control”)\n" +
                                    "• Respiración: patrón consciente (ej. inhalar 4 segundos, exhalar 6)\n" +
                                    "• Imagen mental: recuerdo o visualización positiva\n" +
                                    "• Audio: música o sonido breve que refuerce la emoción\n\n" +
                                    "🔹 Cómo crearla (ejemplo práctico)\n" +
                                    "1. Ponte en un estado emocional positivo (tranquilidad, confianza, foco).\n" +
                                    "2. Repite una frase como: “Todo fluye”.\n" +
                                    "3. Respira de forma lenta y profunda (4–6).\n" +
                                    "4. Visualiza un momento donde te sentías seguro o exitoso.\n" +
                                    "5. Añade un audio breve (puede ser una canción o tono específico).\n" +
                                    "6. Repite este proceso varias veces para reforzar la asociación.\n\n" +
                                    "🔹 Cómo usarla\n\n" +
                                    "Cuando estés en una situación de estrés:\n" +
                                    "• Activa la frase\n" +
                                    "• Respira igual que entrenaste\n" +
                                    "• Evoca la imagen\n" +
                                    "• (Opcional) reproduce el audio\n\n" +
                                    "👉 Esto ayuda a “resetear” tu estado emocional y recuperar control rápidamente.\n\n" +
                                    "🔹 Clave importante\n\n" +
                                    "La efectividad depende de la repetición y consistencia. Cuanto más practiques " +
                                        "el ancla en momentos positivos, más potente será en momentos difíciles.",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showHelpDialog = false }) {
                                Text("Entendido", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun EmotionalAnchorItem(
        anchor: EmotionalAnchor,
        onLaunch: () -> Unit,
        onDelete: () -> Unit,
        onEdit: () -> Unit
    ) {
        var isBreathingGuideVisible by remember(anchor.id) { mutableStateOf(false) }
        val bitmap = remember(anchor.imagePath) {
            BitmapFactory.decodeFile(anchor.imagePath)?.asImageBitmap()
        }

        Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = anchor.phrase,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_anchor_edit),
                            contentDescription = "Editar ancla",
                            tint = Color(0xFF546E7A)
                        )
                    }
                }
                Text(
                    text = "${anchor.breathingTechniqueName} · ${anchor.breathingTechniquePattern}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFFFC107),
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.clickable { isBreathingGuideVisible = !isBreathingGuideVisible }
                )
                if (isBreathingGuideVisible) {
                    Text(
                        text = anchor.breathingTechniqueGuide,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFFEB3B),
                        fontWeight = FontWeight.Bold
                    )
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Imagen ancla",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onLaunch),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(Color(0xFFEEEEEE), RoundedCornerShape(10.dp))
                            .clickable(onClick = onLaunch)
                            .padding(8.dp)
                    ) {
                        Text("Imagen no disponible", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Audio: ${formatClock(anchor.audioDurationMs)} · ${formatDate(anchor.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_anchor_delete),
                            contentDescription = "Eliminar ancla",
                            tint = Color.White.copy(alpha = 0.78f)
                        )
                    }

                    Spacer(modifier = Modifier.width(25.dp))

                    IconButton(
                        onClick = onLaunch,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_anchor_play),
                            contentDescription = "Lanzar ancla",
                            tint = Color(0xFFE7FC31).copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }

    private fun formatClock(ms: Long): String {
        val totalSeconds = (ms / 1000L).toInt().coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    private fun formatDate(timestamp: Long): String {
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return format.format(Date(timestamp))
    }
}
