package com.ypg.neville.feature.emotionalanchors.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ypg.neville.R
import com.ypg.neville.feature.emotionalanchors.data.EmotionalAnchor
import com.ypg.neville.feature.emotionalanchors.data.RoomEmotionalAnchorStore
import com.ypg.neville.feature.emotionalanchors.domain.EmotionalAnchorsController
import com.ypg.neville.feature.voice.media.AndroidVoicePlayerEngine
import com.ypg.neville.feature.voice.media.AndroidVoiceRecorderEngine
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import com.ypg.neville.ui.theme.ContextMenuShape
import java.io.File
import kotlin.random.Random
import java.util.concurrent.Executors

class FragEmotionalAnchorRun : Fragment() {

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

        val anchorId = arguments?.getLong(ARG_ANCHOR_ID, -1L) ?: -1L

        (view as ComposeView).setContent {
            com.ypg.neville.ui.theme.NevilleTheme {
                EmotionalAnchorRunScreen(anchorId = anchorId)
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
    private fun EmotionalAnchorRunScreen(anchorId: Long) {
        val context = LocalContext.current
        var anchor by remember { mutableStateOf<EmotionalAnchor?>(null) }
        var isPlaying by remember { mutableStateOf(false) }
        var shouldLoop by remember { mutableStateOf(false) }

        fun startPlayback(item: EmotionalAnchor) {
            dbExecutor.execute {
                runCatching {
                    controller.playAnchor(item.audioPath) {
                        activity?.runOnUiThread {
                            if (shouldLoop) {
                                startPlayback(item)
                            } else {
                                isPlaying = false
                            }
                        }
                    }
                }.onSuccess {
                    activity?.runOnUiThread {
                        isPlaying = true
                    }
                }.onFailure {
                    activity?.runOnUiThread {
                        isPlaying = false
                        Toast.makeText(context, "No se pudo reproducir el audio", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        LaunchedEffect(anchorId) {
            dbExecutor.execute {
                val loaded = controller.getById(anchorId)
                activity?.runOnUiThread {
                    if (loaded == null) {
                        Toast.makeText(context, "Ancla no encontrada", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    } else {
                        anchor = loaded
                    }
                }
            }
        }

        val current = anchor
        if (current == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFFFA726), Color(0xFF66BB6A))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("Cargando ancla...", color = Color.White)
            }
            return
        }

        val bitmap = remember(current.imagePath) {
            BitmapFactory.decodeFile(current.imagePath)?.asImageBitmap()
        }
        val hasAudio = current.audioPath.isNotBlank() && File(current.audioPath).exists()
        val helpMessages = remember { HELP_MESSAGES.ifEmpty { listOf("Respira... Estas en control") } }
        var helpMessageIndex by remember { mutableIntStateOf(0) }
        val helpAlpha = remember { Animatable(0f) }
        var showHelpText by remember { mutableStateOf(true) }
        var showHelpMenu by remember { mutableStateOf(false) }

        LaunchedEffect(helpMessages) {
            while (true) {
                helpAlpha.animateTo(
                    targetValue = 0.95f,
                    animationSpec = tween(durationMillis = HELP_FADE_IN_MS)
                )
                helpAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = HELP_FADE_OUT_MS)
                )
                helpMessageIndex = nextRandomHelpIndex(helpMessageIndex, helpMessages.size)
                kotlinx.coroutines.delay(HELP_BETWEEN_MESSAGES_MS)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Imagen del ancla de fondo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFFFA726), Color(0xFF66BB6A))
                            )
                        )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.20f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        //Opacidad del fondo de la frase del ancla
                        containerColor = Color.Black.copy(alpha = 0.48f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = current.phrase,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (showHelpText) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                        ) {
                            Text(
                                text = helpMessages[helpMessageIndex],
                                color = Color.White.copy(alpha = helpAlpha.value),
                                textAlign = TextAlign.Center,
                                style = TextStyle(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 22.sp
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = { showHelpMenu = true }
                                        )
                                    }
                            )

                            DropdownMenu(
                                expanded = showHelpMenu,
                                onDismissRequest = { showHelpMenu = false },
                                modifier = Modifier.align(Alignment.TopCenter),
                                shape = ContextMenuShape
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Ocultar frases de ayuda") },
                                    onClick = {
                                        showHelpMenu = false
                                        showHelpText = false
                                    }
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 22.dp)
                                .size(18.dp)
                                .background(Color.White.copy(alpha = 0.18f), CircleShape)
                                .clickable { showHelpText = true }
                        )
                    }
                }

                if (hasAudio) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.16f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        shouldLoop = !shouldLoop
                                    },
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .size(40.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_anchor_loop),
                                        contentDescription = "Reproducir en bucle",
                                        tint = if (shouldLoop) {
                                            Color(0xFF81817C)
                                        } else {
                                            Color.White.copy(alpha = 0.2f)
                                        }
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (isPlaying) {
                                            controller.stopPlayback()
                                            isPlaying = false
                                        } else {
                                            startPlayback(current)
                                        }
                                    },
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .size(52.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            id = if (isPlaying) R.drawable.ic_anchor_stop else R.drawable.ic_anchor_play
                                        ),
                                        contentDescription = if (isPlaying) "Detener" else "Reproducir",
                                        tint = Color.White.copy(alpha = 0.95f)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        controller.stopPlayback()
                                        isPlaying = false
                                        findNavController().popBackStack()
                                    },
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .size(40.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_anchor_close),
                                        contentDescription = "Cerrar",
                                        tint = Color.White.copy(alpha = 0.88f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val ARG_ANCHOR_ID = "arg_anchor_id"
        // Ajusta aquí la velocidad del texto de ayuda (en milisegundos).
        private const val HELP_FADE_IN_MS = 4000
        private const val HELP_FADE_OUT_MS = 6000
        private const val HELP_BETWEEN_MESSAGES_MS = 220L

        private val HELP_MESSAGES = listOf(
            "Respira... Estas en control",
            "Inhala calma, exhala tensión",
            "Vuelve al presente, todo está bien",
            "Suave y lento... recupera tu centro",
            "Eres importante! Continua así",

            "Respira profundo, aquí y ahora",
            "Todo pasa, esto también",
            "Inhala paz, exhala preocupación",
            "Un respiro a la vez",
            "Tu respiración es tu ancla",
            "Calma… no hay prisa",

            "Este momento es suficiente",
            "Aquí estás a salvo",
            "Vuelve a lo simple",
            "Solo este instante importa",
            "Siente el ahora",
            "Paso a paso, sin prisa",

            "Puedes con esto",
            "Ya has superado mucho",
            "Sigue, lo estás haciendo bien",
            "Eres más fuerte de lo que crees",
            "No te rindas ahora",
            "Confía en ti",

            "Déjalo pasar, no te aferres",
            "No todo merece tu energía",
            "Suelta lo que no controlas",
            "Es solo una emoción, no es permanente",
            "Permítete sentir y soltar",

            "Está bien no estar bien",
            "Haz lo mejor que puedas",
            "Eres suficiente",
            "Trátate con amabilidad",
            "No tienes que ser perfecto",
            "Descansa, lo necesitas"
        )

        private fun nextRandomHelpIndex(currentIndex: Int, total: Int): Int {
            if (total <= 1) return 0
            var next = currentIndex
            while (next == currentIndex) {
                next = Random.nextInt(total)
            }
            return next
        }
    }
}
