package com.ypg.neville.feature.voice.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.ypg.neville.feature.voice.data.RoomVoiceRecordingStore
import com.ypg.neville.feature.voice.data.VoiceRecording
import com.ypg.neville.feature.voice.domain.VoiceNotesController
import com.ypg.neville.feature.voice.media.AndroidVoicePlayerEngine
import com.ypg.neville.feature.voice.media.AndroidVoiceRecorderEngine
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class FragVoiceRecordings : Fragment() {

    private val dbExecutor = Executors.newSingleThreadExecutor()
    private lateinit var controller: VoiceNotesController

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
        controller = VoiceNotesController(
            store = RoomVoiceRecordingStore(db.voiceRecordingDao()),
            recorder = AndroidVoiceRecorderEngine(requireContext()),
            player = AndroidVoicePlayerEngine(),
            audioDirectory = File(requireContext().filesDir, "voice_notes")
        )

        (view as ComposeView).setContent {
            com.ypg.neville.ui.theme.NevilleTheme {
                VoiceRecordingsScreen(controller = controller)
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
    private fun VoiceRecordingsScreen(controller: VoiceNotesController) {
        val context = LocalContext.current
        val recordings = remember { mutableStateListOf<VoiceRecording>() }

        var titleDraft by remember { mutableStateOf("") }
        var isRecording by remember { mutableStateOf(false) }
        var elapsedMs by remember { mutableLongStateOf(0L) }
        var startedAt by remember { mutableLongStateOf(0L) }
        var isStopping by remember { mutableStateOf(false) }
        var playingId by remember { mutableLongStateOf(-1L) }
        var renameTarget by remember { mutableStateOf<VoiceRecording?>(null) }
        var deleteTarget by remember { mutableStateOf<VoiceRecording?>(null) }

        fun reload() {
            dbExecutor.execute {
                val list = controller.list()
                activity?.runOnUiThread {
                    recordings.clear()
                    recordings.addAll(list)
                }
            }
        }

        fun stopAndPersist(isAutomatic: Boolean = false) {
            if (isStopping) return
            isStopping = true
            dbExecutor.execute {
                val result = controller.stopAndSave(titleDraft)
                activity?.runOnUiThread {
                    isStopping = false
                    isRecording = false
                    elapsedMs = 0L
                    startedAt = 0L

                    if (result.isSuccess) {
                        titleDraft = ""
                        reload()
                        if (isAutomatic) {
                            Toast.makeText(context, "Límite de 3 minutos alcanzado", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(
                            context,
                            result.exceptionOrNull()?.message ?: "No se pudo guardar la grabación",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        fun startRecording() {
            dbExecutor.execute {
                val result = controller.startRecording()
                activity?.runOnUiThread {
                    if (result.isSuccess) {
                        isRecording = true
                        startedAt = System.currentTimeMillis()
                        elapsedMs = 0L
                    } else {
                        Toast.makeText(
                            context,
                            result.exceptionOrNull()?.message ?: "No se pudo iniciar la grabación",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        val requestMicPermission = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { granted ->
                if (granted) {
                    startRecording()
                } else {
                    Toast.makeText(context, "Permiso de micrófono denegado", Toast.LENGTH_SHORT).show()
                }
            }
        )

        fun startWithPermission() {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (granted) {
                startRecording()
            } else {
                requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        LaunchedEffect(Unit) {
            reload()
        }

        LaunchedEffect(isRecording, startedAt) {
            if (!isRecording) return@LaunchedEffect
            while (isRecording) {
                elapsedMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)
                if (elapsedMs >= VoiceNotesController.MAX_RECORD_MS) {
                    stopAndPersist(isAutomatic = true)
                    break
                }
                delay(150L)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFD0E8F7),
                            Color(0xFFDDEBFF))
                    )
                )
                .padding(12.dp)
        ) {
            Text(
                text = "Notas de Voz",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "Graba frases de voz de hasta 3 minutos y reprodúcelas cuando quieras.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1B2A41),
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
            )

            Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = titleDraft,
                        onValueChange = { titleDraft = it },
                        singleLine = true,
                        label = { Text("Título (opcional)") },
                        placeholder = { Text("Ej: Mi frase de enfoque") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isRecording
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tiempo: ${formatClock(elapsedMs)} / 03:00",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isRecording) Color(0xFFFF9800) else Color(0xFFB5F24A)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isRecording) {
                            Button(
                                onClick = { startWithPermission() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Iniciar grabación")
                            }
                        } else {
                            Button(
                                onClick = { stopAndPersist() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Detener y guardar")
                            }

                            Button(
                                onClick = {
                                    dbExecutor.execute {
                                        controller.cancelRecording()
                                        activity?.runOnUiThread {
                                            isRecording = false
                                            elapsedMs = 0L
                                            startedAt = 0L
                                            Toast.makeText(context, "Grabación cancelada", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancelar")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (recordings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Todavía no tienes notas de voz guardadas",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF324A5F)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recordings, key = { it.id }) { item ->
                        VoiceRecordingItem(
                            item = item,
                            isPlaying = item.id == playingId,
                            onPlayToggle = {
                                if (controller.isPlaying(item.filePath)) {
                                    controller.stopPlayback()
                                    playingId = -1L
                                } else {
                                    runCatching {
                                        controller.play(item.filePath) {
                                            activity?.runOnUiThread {
                                                playingId = -1L
                                            }
                                        }
                                    }.onSuccess {
                                        playingId = item.id
                                    }.onFailure {
                                        Toast.makeText(
                                            context,
                                            it.message ?: "No se pudo reproducir",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            onRename = { renameTarget = item },
                            onDelete = { deleteTarget = item }
                        )
                    }
                }
            }
        }

        renameTarget?.let { item ->
            RenameVoiceDialog(
                initialTitle = item.title,
                onDismiss = { renameTarget = null },
                onConfirm = { newTitle ->
                    dbExecutor.execute {
                        val updated = controller.rename(item.id, newTitle)
                        activity?.runOnUiThread {
                            if (updated != null) {
                                reload()
                            } else {
                                Toast.makeText(context, "Título no válido", Toast.LENGTH_SHORT).show()
                            }
                            renameTarget = null
                        }
                    }
                }
            )
        }

        deleteTarget?.let { item ->
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text("Eliminar voz") },
                text = { Text("¿Quieres eliminar \"${item.title}\"?") },
                confirmButton = {
                    TextButton(onClick = {
                        dbExecutor.execute {
                            controller.delete(item)
                            activity?.runOnUiThread {
                                if (playingId == item.id) {
                                    playingId = -1L
                                }
                                reload()
                                deleteTarget = null
                            }
                        }
                    }) {
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
    }

    @Composable
    private fun VoiceRecordingItem(
        item: VoiceRecording,
        isPlaying: Boolean,
        onPlayToggle: () -> Unit,
        onRename: () -> Unit,
        onDelete: () -> Unit
    ) {
        Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Duración ${formatClock(item.durationMs)} · ${formatDate(item.createdAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF425466),
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onPlayToggle, modifier = Modifier.weight(1f)) {
                        Text(if (isPlaying) "Detener" else "Reproducir")
                    }
                    Button(onClick = onRename, modifier = Modifier.weight(1f)) {
                        Text("Renombrar")
                    }
                    Button(onClick = onDelete, modifier = Modifier.weight(1f)) {
                        Text("Eliminar")
                    }
                }
            }
        }
    }

    @Composable
    private fun RenameVoiceDialog(
        initialTitle: String,
        onDismiss: () -> Unit,
        onConfirm: (String) -> Unit
    ) {
        var draft by remember(initialTitle) { mutableStateOf(initialTitle) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Renombrar voz") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    label = { Text("Nuevo título") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { onConfirm(draft) }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        )
    }

    private fun formatClock(ms: Long): String {
        val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    private fun formatDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}
