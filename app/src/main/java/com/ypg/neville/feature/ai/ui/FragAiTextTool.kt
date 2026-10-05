package com.ypg.neville.feature.ai.ui

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.Fragment
import com.ypg.neville.R
import com.ypg.neville.feature.ai.domain.AiAuthor
import com.ypg.neville.feature.ai.domain.AiTextAction
import com.ypg.neville.feature.ai.domain.AiTextProcessor
import com.ypg.neville.feature.ai.domain.OpenRouterConfiguration
import com.ypg.neville.feature.ai.network.OpenRouterClient
import com.ypg.neville.feature.ai.security.OpenRouterCredentialStore
import com.ypg.neville.model.db.utilsDB
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FragAiTextTool : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            com.ypg.neville.ui.theme.NevilleTheme {
                AiTextToolScreen(
                    sourceTitle = arguments?.getString(ARG_TITLE).orEmpty(),
                    sourceText = arguments?.getString(ARG_TEXT).orEmpty(),
                    initialAction = AiTextAction.fromId(arguments?.getString(ARG_ACTION).orEmpty()),
                    initialAuthor = AiAuthor.fromId(arguments?.getString(ARG_AUTHOR).orEmpty()),
                    onClose = { requireActivity().onBackPressedDispatcher.onBackPressed() }
                )
            }
        }
    }

    companion object {
        const val ARG_TITLE = "ai_text_title"
        const val ARG_TEXT = "ai_text_content"
        const val ARG_ACTION = "ai_text_action"
        const val ARG_AUTHOR = "ai_text_author"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiTextToolScreen(
    sourceTitle: String,
    sourceText: String,
    initialAction: AiTextAction,
    initialAuthor: AiAuthor,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val isUserDragging by scrollState.interactionSource.collectIsDraggedAsState()
    val credentialStore = remember { OpenRouterCredentialStore(context) }
    val processor = remember { AiTextProcessor(OpenRouterClient()) }
    var action by remember { mutableStateOf(initialAction) }
    var author by remember { mutableStateOf(initialAuthor) }
    var result by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showActionMenu by remember { mutableStateOf(false) }
    var showAuthorMenu by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var followLatestResult by remember { mutableStateOf(true) }
    var acceptedTerms by remember {
        mutableStateOf(OpenRouterConfiguration.hasAcceptedAiTerms(context))
    }

    LaunchedEffect(isUserDragging) {
        if (isUserDragging) {
            snapshotFlow { scrollState.canScrollForward }.collect { canScrollForward ->
                followLatestResult = !canScrollForward
            }
        }
    }

    LaunchedEffect(result.length, followLatestResult) {
        if (result.isNotBlank() && followLatestResult) {
            delay(16)
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    fun generate() {
        val key = credentialStore.readApiKey()
        when {
            key.isNullOrBlank() -> showSettings = true
            !OpenRouterConfiguration.hasPrivacyConsent(context) -> showPrivacy = true
            sourceText.isBlank() -> error = context.getString(R.string.ai_no_text)
            else -> {
                followLatestResult = true
                loading = true
                result = ""
                error = null
                scope.launch {
                    runCatching {
                        processor.process(
                            action,
                            author,
                            sourceText,
                            OpenRouterConfiguration.selectedModelIdentifier(context),
                            key,
                            onUpdate = { partial ->
                                scope.launch { result = partial }
                            }
                        )
                    }.onSuccess { result = it }
                        .onFailure { error = it.localizedAiMessage(context) }
                    loading = false
                }
            }
        }
    }

    if (!acceptedTerms) {
        AiTermsScreen(
            onAccept = {
                OpenRouterConfiguration.setAcceptedAiTerms(context, true)
                acceptedTerms = true
            },
            onClose = onClose
        )
        return
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFFE19451), Color(0xFF77452E)))
        )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(action.localizedLabel()) },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(
                                Icons.Rounded.Close,
                                stringResource(R.string.ai_close),
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showSettings = true },
                            enabled = !loading
                        ) {
                            Icon(
                                Icons.Rounded.Settings,
                                stringResource(R.string.ai_configure_openrouter),
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.12f),
                        titleContentColor = Color.White
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    sourceTitle.ifBlank { stringResource(R.string.ai_selected_text) },
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "OpenRouter · ${OpenRouterConfiguration.selectedModelIdentifier(context)}",
                    color = Color.White.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.labelMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        SelectorSurface(
                            label = action.localizedLabel(),
                            enabled = !loading
                        ) {
                            showActionMenu = true
                        }
                        DropdownMenu(
                            expanded = !loading && showActionMenu,
                            onDismissRequest = { showActionMenu = false }
                        ) {
                            AiTextAction.entries.forEach {
                                DropdownMenuItem(
                                    text = { Text(it.localizedLabel()) },
                                    onClick = {
                                        action = it
                                        showActionMenu = false
                                        result = ""
                                    }
                                )
                            }
                        }
                    }
                    if (action.requiresAuthor) {
                        Box(Modifier.weight(1f)) {
                            SelectorSurface(
                                label = author.displayName,
                                enabled = !loading
                            ) {
                                showAuthorMenu = true
                            }
                            DropdownMenu(
                                expanded = !loading && showAuthorMenu,
                                onDismissRequest = { showAuthorMenu = false }
                            ) {
                                AiAuthor.entries.forEach {
                                    DropdownMenuItem(
                                        text = { Text(it.displayName) },
                                        onClick = {
                                            author = it
                                            showAuthorMenu = false
                                            result = ""
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                when {
                    result.isNotBlank() -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.88f)
                        ) {
                            Text(result, color = Color.Black, modifier = Modifier.padding(14.dp))
                        }
                        if (loading) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.width(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.ai_processing_text),
                                    color = Color.White
                                )
                            }
                        } else {
                            Row {
                                IconButton(onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, result)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(
                                            intent,
                                            context.getString(R.string.ai_share_response)
                                        )
                                    )
                                }) {
                                    Icon(
                                        Icons.Rounded.Share,
                                        stringResource(R.string.ai_share),
                                        tint = Color.White
                                    )
                                }
                                IconButton(onClick = {
                                    val saved = runCatching {
                                        utilsDB.insertNewApunte(
                                            context,
                                            context.getString(R.string.ai_response_note_title),
                                            result
                                        ) >= 0
                                    }.getOrDefault(false)
                                    Toast.makeText(
                                        context,
                                        if (saved) {
                                            context.getString(R.string.ai_response_saved_short)
                                        } else {
                                            context.getString(R.string.ai_save_failed_short)
                                        },
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.NoteAdd,
                                        stringResource(R.string.ai_save_notes),
                                        tint = Color.White
                                    )
                                }
                                IconButton(onClick = ::generate) {
                                    Icon(
                                        Icons.Rounded.Refresh,
                                        stringResource(R.string.ai_regenerate),
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                    loading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 34.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color.White)
                            Text(stringResource(R.string.ai_processing_text), color = Color.White)
                        }
                    }
                    result.isBlank() -> {
                        Button(onClick = ::generate, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.AutoAwesome, null)
                            Spacer(Modifier.width(7.dp))
                            Text(stringResource(R.string.ai_generate))
                        }
                    }
                }
                error?.let { Text(it, color = Color(0xFFFFCDD2), fontWeight = FontWeight.Bold) }
            }
        }
    }

    if (showSettings) {
        FullScreenToolDialog(onDismiss = { showSettings = false }) {
            OpenRouterSettingsScreen(
                onClose = { showSettings = false },
                onCredentialsChanged = {}
            )
        }
    }
    if (showPrivacy) {
        FullScreenToolDialog(onDismiss = { showPrivacy = false }) {
            OpenRouterPrivacyScreen(
                onCancel = { showPrivacy = false },
                onAccept = {
                    OpenRouterConfiguration.acceptPrivacyConsent(context)
                    showPrivacy = false
                    generate()
                }
            )
        }
    }
}

@Composable
private fun SelectorSurface(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = if (enabled) 0.20f else 0.10f)
    ) {
        Text(
            label,
            color = Color.White.copy(alpha = if (enabled) 1f else 0.55f),
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun FullScreenToolDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(Modifier.fillMaxSize()) { content() }
    }
}
