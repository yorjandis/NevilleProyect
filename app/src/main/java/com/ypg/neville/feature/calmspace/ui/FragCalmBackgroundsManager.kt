package com.ypg.neville.feature.calmspace.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ypg.neville.feature.calmspace.data.CalmMediaStorage
import java.io.File

class FragCalmBackgroundsManager : Fragment() {

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (view as ComposeView).setContent {
            com.ypg.neville.ui.theme.NevilleTheme {
                CalmBackgroundsManagerScreen()
            }
        }
    }

    @Composable
    private fun CalmBackgroundsManagerScreen() {
        val context = LocalContext.current
        val items = remember { mutableStateListOf<File>() }
        var reloadTick by remember { mutableIntStateOf(0) }
        var previewFile by remember { mutableStateOf<File?>(null) }
        var deleteTarget by remember { mutableStateOf<File?>(null) }

        fun reload() {
            items.clear()
            items.addAll(CalmMediaStorage.listCustomBackgroundFiles(context))
        }

        fun import(uri: android.net.Uri) {
            val result = CalmMediaStorage.importImage(context, uri)
            result.onSuccess {
                Toast.makeText(context, "Imagen agregada", Toast.LENGTH_SHORT).show()
                reloadTick++
            }.onFailure { error ->
                Toast.makeText(context, error.message ?: "No se pudo importar la imagen", Toast.LENGTH_LONG).show()
            }
        }

        val filePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) import(uri)
        }

        val galleryPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) import(uri)
        }

        LaunchedEffect(reloadTick) {
            reload()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF19313B), Color(0xFF244C5B), Color(0xFF2F5E71))
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
                    text = "Fondos de Espacio Calma",
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
                Button(onClick = { filePickerLauncher.launch(arrayOf("image/*")) }) {
                    Text("Agregar (Archivos)")
                }
                Button(onClick = { galleryPickerLauncher.launch("image/*") }) {
                    Text("Agregar (Galería)")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay imágenes personalizadas", color = Color.White.copy(alpha = 0.85f))
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items, key = { it.absolutePath }) { file ->
                        val thumbBitmap = remember(file.absolutePath) {
                            runCatching { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }.getOrNull()
                        }
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
                                if (thumbBitmap != null) {
                                    Image(
                                        bitmap = thumbBitmap,
                                        contentDescription = file.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clickable { previewFile = file }
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .background(Color.Black.copy(alpha = 0.22f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("IMG", color = Color.White)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = file.name, color = Color.White, maxLines = 2)
                                    Text(
                                        text = "Pulsa miniatura para ver",
                                        color = Color.White.copy(alpha = 0.76f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
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

        previewFile?.let { file ->
            AlertDialog(
                onDismissRequest = { previewFile = null },
                confirmButton = {
                    TextButton(onClick = { previewFile = null }) { Text("Cerrar") }
                },
                title = { Text(file.name) },
                text = {
                    val bitmap = remember(file.absolutePath) {
                        runCatching { BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() }.getOrNull()
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = file.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                        )
                    } else {
                        Text("No se pudo cargar vista previa.")
                    }
                }
            )
        }

        deleteTarget?.let { file ->
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text("Eliminar imagen") },
                text = { Text("¿Eliminar ${file.name}?") },
                confirmButton = {
                    TextButton(onClick = {
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

