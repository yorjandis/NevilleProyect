package com.ypg.neville.feature.emotionalanchors.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ypg.neville.R
import com.ypg.neville.feature.emotionalanchors.data.EmotionalAnchor
import com.ypg.neville.feature.emotionalanchors.data.RoomEmotionalAnchorStore
import com.ypg.neville.feature.emotionalanchors.domain.BreathingTechniques
import com.ypg.neville.feature.emotionalanchors.domain.EmotionalAnchorsController
import com.ypg.neville.feature.emotionalanchors.domain.RecordedAnchorAudio
import com.ypg.neville.feature.voice.media.AndroidVoicePlayerEngine
import com.ypg.neville.feature.voice.media.AndroidVoiceRecorderEngine
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors
import org.json.JSONArray

class FragEmotionalAnchors : Fragment() {

    private val dbExecutor = Executors.newSingleThreadExecutor()
    private lateinit var controller: EmotionalAnchorsController

    private data class PredefinedMandala(
        val displayName: String,
        val assetFileName: String
    )

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
                val editAnchorId = arguments?.getLong(ARG_EDIT_ANCHOR_ID, NO_EDIT_ANCHOR_ID) ?: NO_EDIT_ANCHOR_ID
                EmotionalAnchorCreateScreen(controller = controller, editAnchorId = editAnchorId)
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
    private fun EmotionalAnchorCreateScreen(
        controller: EmotionalAnchorsController,
        editAnchorId: Long
    ) {
        val context = LocalContext.current
        val techniques = remember { BreathingTechniques.quickStressRelief }
        val predefinedPhrases = stringArrayResource(R.array.anchor_suggested_phrases).toList()
        val predefinedMandalas = remember { loadPredefinedMandalas(context) }
        val isEditMode = remember(editAnchorId) { editAnchorId > NO_EDIT_ANCHOR_ID }

        var phraseDraft by remember { mutableStateOf("") }
        var showPhraseMenu by remember { mutableStateOf(false) }
        var selectedTechnique by remember { mutableStateOf(techniques.first()) }
        var showTechniqueMenu by remember { mutableStateOf(false) }
        var showMandalaMenu by remember { mutableStateOf(false) }
        var selectedImagePath by remember { mutableStateOf<String?>(null) }
        var recordedAudio by remember { mutableStateOf<RecordedAnchorAudio?>(null) }
        var editingAnchor by remember { mutableStateOf<EmotionalAnchor?>(null) }
        var preloadDone by remember(editAnchorId) { mutableStateOf(false) }

        var isRecording by remember { mutableStateOf(false) }
        var isStopping by remember { mutableStateOf(false) }
        var elapsedMs by remember { mutableLongStateOf(0L) }
        var startedAt by remember { mutableLongStateOf(0L) }

        fun stopRecording(isAutomatic: Boolean = false) {
            if (isStopping) return
            isStopping = true
            dbExecutor.execute {
                val result = controller.stopRecordingAudio()
                activity?.runOnUiThread {
                    isStopping = false
                    isRecording = false
                    elapsedMs = 0L
                    startedAt = 0L

                    result.onSuccess { audio ->
                        recordedAudio = audio
                        if (isAutomatic) {
                            Toast.makeText(context, context.getString(R.string.anchors_audio_limit), Toast.LENGTH_SHORT).show()
                        }
                    }.onFailure { error ->
                        Toast.makeText(
                            context,
                            context.getString(R.string.anchors_save_audio_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        fun startRecording() {
            dbExecutor.execute {
                val result = controller.startRecordingAudio()
                activity?.runOnUiThread {
                    result.onSuccess {
                        isRecording = true
                        startedAt = System.currentTimeMillis()
                        elapsedMs = 0L
                    }.onFailure { error ->
                        Toast.makeText(
                            context,
                            context.getString(R.string.anchors_start_recording_error),
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
                    Toast.makeText(context, context.getString(R.string.anchors_microphone_denied), Toast.LENGTH_SHORT).show()
                }
            }
        )

        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
            onResult = { uri: Uri? ->
                if (uri == null) return@rememberLauncherForActivityResult
                dbExecutor.execute {
                    val copiedPath = copyImageToInternalStorage(context, uri)
                    activity?.runOnUiThread {
                        if (copiedPath != null) {
                            selectedImagePath?.let { previous ->
                                val isOriginalEditingImage = isEditMode && previous == editingAnchor?.imagePath
                                if (previous != copiedPath && !isOriginalEditingImage) {
                                    File(previous).delete()
                                }
                            }
                            selectedImagePath = copiedPath
                        } else {
                            Toast.makeText(context, context.getString(R.string.anchors_import_image_error), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )

        fun startRecordingWithPermission() {
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

        LaunchedEffect(isRecording, startedAt) {
            if (!isRecording) return@LaunchedEffect
            while (isRecording) {
                elapsedMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)
                if (elapsedMs >= MAX_ANCHOR_AUDIO_MS) {
                    stopRecording(isAutomatic = true)
                    break
                }
                delay(150L)
            }
        }

        LaunchedEffect(editAnchorId, preloadDone) {
            if (!isEditMode || preloadDone) return@LaunchedEffect
            dbExecutor.execute {
                val existing = controller.getById(editAnchorId)
                activity?.runOnUiThread {
                    if (existing == null) {
                        Toast.makeText(context, context.getString(R.string.anchors_not_found_edit), Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                        return@runOnUiThread
                    }
                    editingAnchor = existing
                    phraseDraft = existing.phrase
                    selectedTechnique = techniques.firstOrNull { it.id == existing.breathingTechniqueId } ?: techniques.first()
                    selectedImagePath = existing.imagePath
                    recordedAudio = if (existing.audioPath.isNotBlank()) {
                        RecordedAnchorAudio(existing.audioPath, existing.audioDurationMs)
                    } else {
                        null
                    }
                    preloadDone = true
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFEBE2C2),
                            Color(0xFFECC488)
                        )
                    )
                )
                .padding(12.dp)
        ) {
            Text(
                text = if (isEditMode) stringResource(R.string.anchors_edit_title) else stringResource(R.string.anchors_create_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = stringResource(R.string.anchors_training_reminder),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF131313),
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
            )

            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = phraseDraft,
                            onValueChange = { phraseDraft = it },
                            label = { Text(stringResource(R.string.anchors_phrase_label)) },
                            placeholder = { Text(stringResource(R.string.anchors_phrase_placeholder)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        IconButton(
                            onClick = { showPhraseMenu = true },
                            modifier = Modifier
                                .align(androidx.compose.ui.Alignment.TopEnd)
                                .padding(top = 8.dp, end = 4.dp)
                                .size(26.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_tips),
                                contentDescription = stringResource(R.string.anchors_suggested_phrases),
                                tint = Color(0xFFFFC107)
                            )
                        }

                        DropdownMenu(
                            expanded = showPhraseMenu,
                            onDismissRequest = { showPhraseMenu = false }
                        ) {
                            predefinedPhrases.forEach { phrase ->
                                DropdownMenuItem(
                                    text = { Text(phrase) },
                                    onClick = {
                                        phraseDraft = phrase
                                        showPhraseMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { showTechniqueMenu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.anchors_breathing_selection, localizedBreathingName(selectedTechnique.id, selectedTechnique.name)))
                        }
                        DropdownMenu(
                            expanded = showTechniqueMenu,
                            onDismissRequest = { showTechniqueMenu = false }
                        ) {
                            techniques.forEach { technique ->
                                DropdownMenuItem(
                                    text = { Text("${localizedBreathingName(technique.id, technique.name)} · ${localizedBreathingPattern(technique.id, technique.pattern)}") },
                                    onClick = {
                                        selectedTechnique = technique
                                        showTechniqueMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Text(
                        text = stringResource(
                            R.string.anchors_breathing_details,
                            localizedBreathingPattern(selectedTechnique.id, selectedTechnique.pattern),
                            localizedBreathingEffect(selectedTechnique.id, selectedTechnique.shortEffect)
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFCDDC39)
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (selectedImagePath == null) stringResource(R.string.anchors_select_image) else stringResource(R.string.anchors_change_image))
                        }

                        IconButton(
                            onClick = { showMandalaMenu = true },
                            modifier = Modifier
                                .align(androidx.compose.ui.Alignment.TopEnd)
                                .padding(top = 8.dp, end = 4.dp)
                                .size(26.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_tips),
                                contentDescription = stringResource(R.string.anchors_predefined_mandalas),
                                tint = Color(0xFF0C0E5E)
                            )
                        }

                        DropdownMenu(
                            expanded = showMandalaMenu,
                            onDismissRequest = { showMandalaMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.anchors_choose_own_image)) },
                                onClick = {
                                    showMandalaMenu = false
                                    imagePickerLauncher.launch("image/*")
                                }
                            )
                            predefinedMandalas.forEach { mandala ->
                                DropdownMenuItem(
                                    text = { Text(mandala.displayName) },
                                    onClick = {
                                        showMandalaMenu = false
                                        dbExecutor.execute {
                                            val copiedPath = copyAssetImageToInternalStorage(
                                                context = context,
                                                assetFileName = mandala.assetFileName
                                            )
                                            activity?.runOnUiThread {
                                                if (copiedPath != null) {
                                                    selectedImagePath?.let { previous ->
                                                        val isOriginalEditingImage = isEditMode && previous == editingAnchor?.imagePath
                                                        if (previous != copiedPath && !isOriginalEditingImage) {
                                                            File(previous).delete()
                                                        }
                                                    }
                                                    selectedImagePath = copiedPath
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.anchors_load_mandala_error),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    val selectedBitmap = remember(selectedImagePath) {
                        selectedImagePath
                            ?.let { BitmapFactory.decodeFile(it) }
                            ?.asImageBitmap()
                    }
                    if (selectedBitmap != null) {
                        Image(
                            bitmap = selectedBitmap,
                            contentDescription = stringResource(R.string.anchors_image_content_description),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Text(
                        text = stringResource(
                            R.string.anchors_short_audio,
                            recordedAudio?.let { formatClock(it.durationMs) } ?: stringResource(R.string.anchors_not_recorded)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFF9800)
                    )
                    Text(
                        text = stringResource(R.string.anchors_current_recording, formatClock(elapsedMs)),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isRecording) Color(0xFFBF360C) else Color(0xFFCDDC39)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isRecording) {
                            Button(
                                onClick = { startRecordingWithPermission() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.anchors_record_audio))
                            }
                        } else {
                            Button(
                                onClick = { stopRecording() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.anchors_stop_audio))
                            }
                            Button(
                                onClick = {
                                    dbExecutor.execute {
                                        controller.cancelRecordingAudio()
                                        activity?.runOnUiThread {
                                            isRecording = false
                                            elapsedMs = 0L
                                            startedAt = 0L
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.common_cancel))
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val imagePath = selectedImagePath
                            val audio = recordedAudio
                            val existing = editingAnchor

                            if (phraseDraft.trim().isBlank()) {
                                Toast.makeText(context, context.getString(R.string.anchors_write_phrase), Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (imagePath.isNullOrBlank()) {
                                Toast.makeText(context, context.getString(R.string.anchors_select_image_validation), Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (isEditMode && existing == null) {
                                Toast.makeText(context, context.getString(R.string.anchors_loading_for_edit), Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            dbExecutor.execute {
                                val result = if (existing != null) {
                                    controller.updateAnchor(
                                        existing = existing,
                                        phrase = phraseDraft,
                                        breathingTechnique = selectedTechnique,
                                        imagePath = imagePath,
                                        audio = audio
                                    )
                                } else {
                                    controller.createAnchor(
                                        phrase = phraseDraft,
                                        breathingTechnique = selectedTechnique,
                                        imagePath = imagePath,
                                        audio = audio
                                    )
                                }
                                activity?.runOnUiThread {
                                    result.onSuccess {
                                        Toast.makeText(
                                            context,
                                            context.getString(if (existing != null) R.string.anchors_updated else R.string.anchors_created),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        findNavController().popBackStack()
                                    }.onFailure { error ->
                                        Toast.makeText(
                                            context,
                                            context.getString(if (existing != null) R.string.anchors_update_error else R.string.anchors_create_error),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isEditMode) stringResource(R.string.anchors_update_action) else stringResource(R.string.anchors_save_action))
                    }

                    Button(
                        onClick = { findNavController().popBackStack() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.anchors_back_to_list))
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

    private fun copyImageToInternalStorage(context: Context, uri: Uri): String? {
        return runCatching {
            val imageDirectory = File(context.filesDir, "emotional_anchors/images")
            if (!imageDirectory.exists()) {
                imageDirectory.mkdirs()
            }

            val extension = when (context.contentResolver.getType(uri)?.lowercase(Locale.ROOT)) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }

            val outputFile = File(imageDirectory, "anchor_image_${System.currentTimeMillis()}.$extension")
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "No se pudo abrir la imagen" }
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            outputFile.absolutePath
        }.getOrNull()
    }

    private fun copyAssetImageToInternalStorage(context: Context, assetFileName: String): String? {
        return runCatching {
            val imageDirectory = File(context.filesDir, "emotional_anchors/images")
            if (!imageDirectory.exists()) {
                imageDirectory.mkdirs()
            }

            val extension = assetFileName.substringAfterLast('.', "jpg")
            val outputFile = File(
                imageDirectory,
                "anchor_mandala_${assetFileName.substringBeforeLast('.')}_${System.currentTimeMillis()}.$extension"
            )

            context.assets.open("mandalas/$assetFileName").use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            outputFile.absolutePath
        }.getOrNull()
    }

    private fun loadPredefinedMandalas(context: Context): List<PredefinedMandala> {
        val fromJson = runCatching {
            context.assets.open("mandalas/mandalas.json").bufferedReader().use { it.readText() }
        }.mapCatching { jsonText ->
            val arr = JSONArray(jsonText)
            buildList {
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val name = item.optString("name").trim()
                    val file = item.optString("file").trim()
                    if (name.isNotBlank() && file.isNotBlank()) {
                        add(PredefinedMandala(displayName = name, assetFileName = file))
                    }
                }
            }
        }.getOrDefault(emptyList())

        if (fromJson.isNotEmpty()) return fromJson

        val availableFiles = runCatching {
            context.assets.list("mandalas").orEmpty().toList()
        }.getOrDefault(emptyList())

        return availableFiles
            .filter { file ->
                val lower = file.lowercase(Locale.ROOT)
                (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")) &&
                    lower != "mandalas.json"
            }
            .sorted()
            .map { file ->
                PredefinedMandala(
                    displayName = file
                        .substringBeforeLast('.')
                        .replace('_', ' ')
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                    assetFileName = file
                )
            }
    }

    companion object {
        const val ARG_EDIT_ANCHOR_ID = "arg_edit_anchor_id"
        private const val NO_EDIT_ANCHOR_ID = -1L
        private const val MAX_ANCHOR_AUDIO_MS = 60_000L

    }
}
