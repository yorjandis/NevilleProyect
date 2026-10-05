package com.ypg.neville.feature.ai.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ypg.neville.R
import com.ypg.neville.feature.ai.domain.AiAuthor
import com.ypg.neville.feature.ai.domain.AiConversation
import com.ypg.neville.feature.ai.domain.AiMessage
import com.ypg.neville.feature.ai.domain.AiMessageStatus
import com.ypg.neville.feature.ai.domain.OpenRouterConfiguration
import com.ypg.neville.model.db.utilsDB
import kotlinx.coroutines.launch

private val ChatBackground = Brush.linearGradient(
    listOf(Color(0xFFE19451), Color(0xFF77452E))
)
private val UserBubble = Color.Black.copy(alpha = 0.72f)
private val ResponseBubble = Color.White.copy(alpha = 0.88f)

@Composable
fun AiChatRoot(
    viewModel: AiChatViewModel,
    prefill: String?,
    onClose: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var acceptedTerms by remember {
        mutableStateOf(OpenRouterConfiguration.hasAcceptedAiTerms(context))
    }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showPrivacy by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(prefill) { viewModel.load(prefill) }

    if (!acceptedTerms) {
        AiTermsScreen(
            onAccept = {
                OpenRouterConfiguration.setAcceptedAiTerms(context, true)
                acceptedTerms = true
            },
            onClose = onClose
        )
    } else {
        AiChatScreen(
            state = state,
            onClose = onClose,
            onInputChanged = viewModel::setInputText,
            onSubmit = {
                when {
                    !state.hasApiKey -> showSettings = true
                    !OpenRouterConfiguration.hasPrivacyConsent(context) -> showPrivacy = true
                    else -> viewModel.submitMessage()
                }
            },
            onSubmitSuggestion = { suggestion ->
                when {
                    !state.hasApiKey -> showSettings = true
                    !OpenRouterConfiguration.hasPrivacyConsent(context) -> showPrivacy = true
                    else -> viewModel.submitMessage(suggestion)
                }
            },
            onStop = viewModel::cancelResponse,
            onRetry = viewModel::retryResponse,
            onNewConversation = viewModel::createConversation,
            onShowSettings = { showSettings = true },
            onShowHistory = { showHistory = true }
        )
    }

    state.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::consumeError,
            title = { Text(stringResource(R.string.ai_chat_title)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::consumeError) {
                    Text(stringResource(R.string.ai_accept))
                }
            }
        )
    }

    if (showSettings) {
        FullScreenDialog(onDismiss = { showSettings = false }) {
            OpenRouterSettingsScreen(
                onClose = { showSettings = false },
                onCredentialsChanged = viewModel::refreshCredentialState
            )
        }
    }

    if (showPrivacy) {
        FullScreenDialog(onDismiss = { showPrivacy = false }) {
            OpenRouterPrivacyScreen(
                onCancel = { showPrivacy = false },
                onAccept = {
                    OpenRouterConfiguration.acceptPrivacyConsent(context)
                    showPrivacy = false
                }
            )
        }
    }

    if (showHistory) {
        FullScreenDialog(onDismiss = { showHistory = false }) {
            ConversationHistoryScreen(
                state = state,
                onClose = { showHistory = false },
                onOpen = {
                    viewModel.selectConversation(it)
                    showHistory = false
                },
                onRename = viewModel::renameConversation,
                onDelete = viewModel::deleteConversation
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiChatScreen(
    state: AiChatUiState,
    onClose: () -> Unit,
    onInputChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onSubmitSuggestion: (String) -> Unit,
    onStop: () -> Unit,
    onRetry: (String) -> Unit,
    onNewConversation: (AiAuthor) -> Unit,
    onShowSettings: () -> Unit,
    onShowHistory: () -> Unit
) {
    var showOptions by remember { mutableStateOf(false) }
    var showAuthors by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val isUserDragging by listState.interactionSource.collectIsDraggedAsState()
    var followLatestMessage by remember(state.activeConversation?.id) {
        mutableStateOf(true)
    }

    LaunchedEffect(isUserDragging) {
        if (isUserDragging) {
            snapshotFlow { listState.canScrollForward }.collect { canScrollForward ->
                followLatestMessage = !canScrollForward
            }
        }
    }

    LaunchedEffect(state.messages.size) {
        followLatestMessage = true
    }

    LaunchedEffect(
        state.messages.size,
        state.messages.lastOrNull()?.text?.length,
        followLatestMessage
    ) {
        if (state.messages.isNotEmpty() && followLatestMessage) {
            listState.animateScrollToItem((state.messages.size - 1).coerceAtLeast(0))
        }
    }

    Box(Modifier.fillMaxSize().background(ChatBackground)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                stringResource(
                                    R.string.ai_ask_author,
                                    state.author.displayName.substringBefore(" Goddard")
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "OpenRouter",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.78f)
                            )
                        }
                    },
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
                        Box {
                            IconButton(onClick = { showAuthors = true }, enabled = !state.isResponding) {
                                Icon(
                                    Icons.Rounded.Person,
                                    stringResource(R.string.ai_change_author),
                                    tint = Color.White
                                )
                            }
                            DropdownMenu(
                                expanded = showAuthors,
                                onDismissRequest = { showAuthors = false }
                            ) {
                                AiAuthor.entries.forEach { author ->
                                    DropdownMenuItem(
                                        text = { Text(author.displayName) },
                                        onClick = {
                                            showAuthors = false
                                            onNewConversation(author)
                                        }
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = { onNewConversation(state.author) },
                            enabled = !state.isResponding
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                stringResource(R.string.ai_new_conversation),
                                tint = Color.White
                            )
                        }
                        Box {
                            IconButton(onClick = { showOptions = true }) {
                                Icon(
                                    Icons.Rounded.MoreVert,
                                    stringResource(R.string.ai_more_options),
                                    tint = Color.White
                                )
                            }
                            DropdownMenu(
                                expanded = showOptions,
                                onDismissRequest = { showOptions = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(stringResource(R.string.ai_conversation_history))
                                    },
                                    leadingIcon = { Icon(Icons.Rounded.History, null) },
                                    onClick = { showOptions = false; onShowHistory() }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(stringResource(R.string.ai_configure_openrouter))
                                    },
                                    leadingIcon = { Icon(Icons.Rounded.Key, null) },
                                    onClick = { showOptions = false; onShowSettings() }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.12f),
                        titleContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                ChatPrompt(
                    text = state.inputText,
                    isResponding = state.isResponding,
                    onTextChanged = onInputChanged,
                    onSubmit = onSubmit,
                    onStop = onStop
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                ProviderBadge(
                    modelIdentifier = state.activeModelIdentifier,
                    dailyLimit = OpenRouterConfiguration.dailyFreeRequestLimit(LocalContext.current)
                )
                if (state.loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                } else if (state.messages.isEmpty()) {
                    if (!state.hasApiKey) {
                        MissingKeyContent(onConfigure = onShowSettings)
                    } else {
                        SuggestionGrid(
                            author = state.author,
                            onSuggestion = onSubmitSuggestion
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.messages, key = { it.id }) { message ->
                            ChatMessageBubble(message, onRetry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderBadge(modelIdentifier: String, dailyLimit: Int) {
    Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.28f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                "OpenRouter · $modelIdentifier",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            stringResource(R.string.ai_free_account_limit, dailyLimit),
            color = Color.White.copy(alpha = 0.76f),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun MissingKeyContent(onConfigure: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.Key, null, tint = Color.White, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(R.string.ai_openrouter_not_configured),
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(R.string.ai_add_personal_key),
            color = Color.White.copy(alpha = 0.82f),
            modifier = Modifier.padding(vertical = 10.dp)
        )
        Button(onClick = onConfigure) {
            Text(stringResource(R.string.ai_configure_openrouter))
        }
    }
}

@Composable
private fun ChatMessageBubble(message: AiMessage, onRetry: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.86f),
            horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (message.isUser) UserBubble else ResponseBubble
            ) {
                if (
                    !message.isUser &&
                    message.status == AiMessageStatus.STREAMING &&
                    message.text.isBlank()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(9.dp))
                        Text(stringResource(R.string.ai_thinking), color = Color.Black)
                    }
                } else {
                    Text(
                        text = message.text,
                        color = if (message.isUser) Color.White else Color.Black,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }
            if (!message.isUser) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, message.text)
                            }
                            context.startActivity(
                                Intent.createChooser(
                                    intent,
                                    context.getString(R.string.ai_share_response)
                                )
                            )
                        },
                        enabled = message.status != AiMessageStatus.STREAMING
                    ) {
                        Icon(
                            Icons.Rounded.Share,
                            stringResource(R.string.ai_share),
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { onRetry(message.id) },
                        enabled = message.status != AiMessageStatus.STREAMING
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            stringResource(R.string.ai_regenerate),
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                val saved = runCatching {
                                    utilsDB.insertNewApunte(
                                        context,
                                        context.getString(R.string.ai_chat_note_title),
                                        message.text
                                    ) >= 0
                                }.getOrDefault(false)
                                Toast.makeText(
                                    context,
                                    if (saved) {
                                        context.getString(R.string.ai_response_saved_notes)
                                    } else {
                                        context.getString(R.string.ai_response_save_failed)
                                    },
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        enabled = message.status != AiMessageStatus.STREAMING
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.NoteAdd,
                            stringResource(R.string.ai_save_notes),
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "OpenRouter · ${message.modelIdentifier}",
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatPrompt(
    text: String,
    isResponding: Boolean,
    onTextChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onStop: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
    ) {
        if (isResponding) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(10.dp))
                OutlinedButton(
                    onClick = onStop,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Rounded.Cancel, null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.ai_stop))
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            stringResource(R.string.ai_write_something),
                            color = Color.White.copy(alpha = 0.64f)
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontSize = 20.sp),
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSubmit() })
                )
                IconButton(
                    onClick = onSubmit,
                    enabled = text.isNotBlank(),
                    modifier = Modifier
                        .padding(start = 6.dp, bottom = 4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF9800))
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Send,
                        stringResource(R.string.ai_send),
                        tint = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionGrid(author: AiAuthor, onSuggestion: (String) -> Unit) {
    val suggestionResource = when (author) {
        AiAuthor.NEVILLE -> R.array.ai_suggestions_neville
        AiAuthor.JOE_DISPENZA -> R.array.ai_suggestions_joe
        AiAuthor.BRUCE_LIPTON -> R.array.ai_suggestions_bruce
        AiAuthor.GREGG_BRADEN -> R.array.ai_suggestions_gregg
    }
    val suggestions = stringArrayResource(suggestionResource).toList()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                stringResource(R.string.ai_suggestions),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(suggestions.chunked(2)) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { suggestion ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSuggestion(suggestion) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.20f)
                    ) {
                        Text(
                            suggestion,
                            color = Color.White,
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun AiTermsScreen(onAccept: () -> Unit, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatBackground)
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Rounded.Close,
                    stringResource(R.string.ai_close),
                    tint = Color.White
                )
            }
        }
        Text(
            stringResource(R.string.ai_terms_title),
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(R.string.ai_terms_body),
            color = Color.White,
            fontSize = 20.sp
        )
        Button(onClick = onAccept) { Text(stringResource(R.string.ai_terms_accept)) }
        Text(
            stringResource(R.string.ai_terms_not_accepted),
            color = Color.White.copy(alpha = 0.74f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationHistoryScreen(
    state: AiChatUiState,
    onClose: () -> Unit,
    onOpen: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    var renameTarget by remember { mutableStateOf<AiConversation?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<AiConversation?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ai_conversation_history)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.ai_close))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.conversations, key = { it.id }) { conversation ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(conversation.id) },
                    tonalElevation = 2.dp,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(conversation.title, fontWeight = FontWeight.Bold)
                            Text(
                                "${conversation.author.displayName} · ${conversation.modelIdentifier}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = {
                            renameTarget = conversation
                            renameText = conversation.title
                        }) {
                            Icon(Icons.Rounded.Edit, stringResource(R.string.ai_rename))
                        }
                        IconButton(onClick = { deleteTarget = conversation }) {
                            Icon(Icons.Rounded.Delete, stringResource(R.string.ai_delete))
                        }
                    }
                }
            }
        }
    }
    renameTarget?.let { conversation ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(stringResource(R.string.ai_rename)) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRename(conversation.id, renameText)
                    renameTarget = null
                }) { Text(stringResource(R.string.ai_save)) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
    deleteTarget?.let { conversation ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.ai_delete_conversation)) },
            text = {
                Text(
                    stringResource(
                        R.string.ai_delete_conversation_message,
                        conversation.title
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(conversation.id)
                    deleteTarget = null
                }) {
                    Text(
                        stringResource(R.string.ai_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun FullScreenDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) { content() }
    }
}
