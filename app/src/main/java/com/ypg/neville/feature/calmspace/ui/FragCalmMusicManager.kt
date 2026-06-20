package com.ypg.neville.feature.calmspace.ui

import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ypg.neville.feature.calmspace.data.CalmMediaStorage
import java.io.File

class FragCalmMusicManager : Fragment() {

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (view as ComposeView).setContent {
            com.ypg.neville.ui.theme.NevilleTheme {
                CalmMusicManagerScreen()
            }
        }
    }

    @Composable
    private fun CalmMusicManagerScreen() {
        val context = LocalContext.current
        val items = remember { mutableStateListOf<File>() }
        var reloadTick by remember { mutableIntStateOf(0) }
        var deleteTarget by remember { mutableStateOf<File?>(null) }
        var playingPath by remember { mutableStateOf<String?>(null) }
        var nowPlayingId by remember { mutableLongStateOf(-1L) }

        val playerHolder = remember { mutableStateOf<MediaPlayer?>(null) }
        fun stopPlayback() {
            runCatching {
                playerHolder.value?.stop()
                playerHolder.value?.release()
            }
            playerHolder.value = null
            playingPath = null
            nowPlayingId = -1L
        }

        fun reload() {
            items.clear()
            items.addAll(CalmMediaStorage.listCustomMusicFiles(context))
        }

        fun import(uri: android.net.Uri) {
            val result = CalmMediaStorage.importMusic(context, uri)
            result.onSuccess {
                Toast.makeText(context, "Música agregada", Toast.LENGTH_SHORT).show()
                reloadTick++
            }.onFailure { error ->
                Toast.makeText(context, error.message ?: "No se pudo importar el audio", Toast.LENGTH_LONG).show()
            }
        }

        val filePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) import(uri)
        }

        val contentPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) import(uri)
        }

        LaunchedEffect(reloadTick) {
            reload()
        }

        DisposableEffect(Unit) {
            onDispose { stopPlayback() }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF2B2038), Color(0xFF3E2C5E), Color(0xFF4D3D7B))
                    )
                )
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Música de Espacio Calma",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Text(
                    text = "Cerrar",
                    color = Color.White,
                    modifier = Modifier
                        .clickable { findNavController().popBackStack() }
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { filePickerLauncher.launch(arrayOf("audio/*")) }) {
                    Text("Agregar (Archivos)")
                }
                Button(onClick = { contentPickerLauncher.launch("audio/*") }) {
                    Text("Agregar (Galería)")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay música personalizada", color = Color.White.copy(alpha = 0.85f))
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items, key = { it.absolutePath }) { file ->
                        val isPlaying = playingPath == file.absolutePath
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = file.name, color = Color.White, maxLines = 2)
                                    Text(
                                        text = if (isPlaying) "Reproduciendo..." else "Pista lista para reproducir",
                                        color = Color.White.copy(alpha = 0.76f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        if (isPlaying) {
                                            stopPlayback()
                                        } else {
                                            stopPlayback()
                                            val player = runCatching {
                                                MediaPlayer().apply {
                                                    setDataSource(file.absolutePath)
                                                    isLooping = true
                                                    prepare()
                                                    start()
                                                }
                                            }.getOrNull()
                                            if (player == null) {
                                                Toast.makeText(context, "No se pudo reproducir", Toast.LENGTH_SHORT).show()
                                            } else {
                                                playerHolder.value = player
                                                playingPath = file.absolutePath
                                                nowPlayingId = file.hashCode().toLong()
                                            }
                                        }
                                    }
                                ) {
                                    Text(if (isPlaying) "Detener" else "Reproducir")
                                }
                                TextButton(onClick = { deleteTarget = file }) {
                                    Text("Eliminar")
                                }
                            }
                        }
                    }
                }
            }
        }

        deleteTarget?.let { file ->
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text("Eliminar audio") },
                text = { Text("¿Eliminar ${file.name}?") },
                confirmButton = {
                    TextButton(onClick = {
                        if (playingPath == file.absolutePath) {
                            stopPlayback()
                        }
                        runCatching { file.delete() }
                        deleteTarget = null
                        reloadTick++
                    }) { Text("Eliminar") }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") }
                }
            )
        }
    }
}

