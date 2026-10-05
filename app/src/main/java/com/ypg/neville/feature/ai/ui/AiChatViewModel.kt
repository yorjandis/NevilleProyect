package com.ypg.neville.feature.ai.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ypg.neville.R
import com.ypg.neville.feature.ai.data.AiChatRepository
import com.ypg.neville.feature.ai.domain.AiAuthor
import com.ypg.neville.feature.ai.domain.AiConversation
import com.ypg.neville.feature.ai.domain.AiInstructions
import com.ypg.neville.feature.ai.domain.AiMessage
import com.ypg.neville.feature.ai.domain.AiMessageRole
import com.ypg.neville.feature.ai.domain.AiMessageStatus
import com.ypg.neville.feature.ai.domain.OpenRouterChatRequest
import com.ypg.neville.feature.ai.domain.OpenRouterConfiguration
import com.ypg.neville.feature.ai.domain.OpenRouterContextMessage
import com.ypg.neville.feature.ai.network.OpenRouterClient
import com.ypg.neville.feature.ai.network.OpenRouterErrorKind
import com.ypg.neville.feature.ai.network.OpenRouterException
import com.ypg.neville.feature.ai.security.OpenRouterCredentialStore
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiChatUiState(
    val loading: Boolean = true,
    val conversations: List<AiConversation> = emptyList(),
    val activeConversation: AiConversation? = null,
    val messages: List<AiMessage> = emptyList(),
    val inputText: String = "",
    val isResponding: Boolean = false,
    val hasApiKey: Boolean = false,
    val errorMessage: String? = null
) {
    val author: AiAuthor get() = activeConversation?.author ?: AiAuthor.NEVILLE
    val activeModelIdentifier: String
        get() = activeConversation?.modelIdentifier ?: OpenRouterConfiguration.DEFAULT_MODEL
}

class AiChatViewModel(
    private val appContext: Context,
    private val repository: AiChatRepository,
    private val credentialStore: OpenRouterCredentialStore,
    private val client: OpenRouterClient
) : ViewModel() {
    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    private var responseJob: Job? = null
    private var loaded = false

    fun load(prefill: String? = null) {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            runCatching {
                refreshConversations()
                val latest = _uiState.value.conversations.firstOrNull()
                if (prefill.isNullOrBlank() && latest != null) {
                    selectConversation(latest.id)
                } else {
                    createConversationInternal(AiAuthor.NEVILLE)
                    if (!prefill.isNullOrBlank()) {
                        setInputText(
                            appContext.getString(R.string.ai_prefill_prompt, prefill.trim())
                        )
                    }
                }
            }.onFailure(::showError)
            _uiState.update {
                it.copy(
                    loading = false,
                    hasApiKey = credentialStore.hasApiKey()
                )
            }
        }
    }

    fun refreshCredentialState() {
        _uiState.update { it.copy(hasApiKey = credentialStore.hasApiKey()) }
    }

    fun setInputText(value: String) {
        _uiState.update { it.copy(inputText = value.take(MAX_MESSAGE_CHARACTERS)) }
    }

    fun createConversation(author: AiAuthor = _uiState.value.author) {
        viewModelScope.launch {
            runCatching { createConversationInternal(author) }.onFailure(::showError)
        }
    }

    fun selectConversation(id: String) {
        if (_uiState.value.isResponding) return
        viewModelScope.launch {
            runCatching {
                val conversation = repository.conversation(id)
                    ?: error(appContext.getString(R.string.ai_error_conversation_missing))
                val messages = repository.messages(id)
                _uiState.update {
                    it.copy(
                        activeConversation = conversation,
                        messages = messages,
                        inputText = ""
                    )
                }
            }.onFailure(::showError)
        }
    }

    fun renameConversation(id: String, title: String) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                repository.renameConversation(id, clean)
                refreshConversations()
                if (_uiState.value.activeConversation?.id == id) {
                    _uiState.update { state ->
                        state.copy(activeConversation = state.activeConversation?.copy(title = clean))
                    }
                }
            }.onFailure(::showError)
        }
    }

    fun deleteConversation(id: String) {
        if (_uiState.value.isResponding && _uiState.value.activeConversation?.id == id) return
        viewModelScope.launch {
            runCatching {
                repository.deleteConversation(id)
                refreshConversations()
                val remaining = _uiState.value.conversations.firstOrNull()
                if (_uiState.value.activeConversation?.id == id) {
                    if (remaining != null) selectConversation(remaining.id)
                    else createConversationInternal(AiAuthor.NEVILLE)
                }
            }.onFailure(::showError)
        }
    }

    fun submitMessage(suggestedText: String? = null) {
        if (_uiState.value.isResponding) return
        val state = _uiState.value
        val prompt = (suggestedText ?: state.inputText).trim()
        if (prompt.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = appContext.getString(R.string.ai_error_empty_message))
            }
            return
        }
        if (prompt.length > MAX_MESSAGE_CHARACTERS) {
            _uiState.update {
                it.copy(
                    errorMessage = appContext.getString(
                        R.string.ai_error_message_too_long,
                        MAX_MESSAGE_CHARACTERS
                    )
                )
            }
            return
        }
        val conversation = state.activeConversation ?: return
        val apiKey = credentialStore.readApiKey()
        if (apiKey.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    hasApiKey = false,
                    errorMessage = OpenRouterException(OpenRouterErrorKind.MISSING_API_KEY)
                        .localizedAiMessage(appContext)
                )
            }
            return
        }
        if (!OpenRouterConfiguration.hasPrivacyConsent(appContext)) {
            _uiState.update {
                it.copy(errorMessage = appContext.getString(R.string.ai_error_privacy_required))
            }
            return
        }

        val nextSequence = (state.messages.maxOfOrNull { it.sequence } ?: -1L) + 1L
        val now = System.currentTimeMillis()
        val userMessage = AiMessage(
            id = UUID.randomUUID().toString(),
            conversationId = conversation.id,
            text = prompt,
            role = AiMessageRole.USER,
            createdAt = now,
            sequence = nextSequence,
            status = AiMessageStatus.COMPLETED,
            modelIdentifier = conversation.modelIdentifier
        )
        val assistantMessage = AiMessage(
            id = UUID.randomUUID().toString(),
            conversationId = conversation.id,
            text = "",
            role = AiMessageRole.ASSISTANT,
            createdAt = now + 1,
            sequence = nextSequence + 1,
            status = AiMessageStatus.STREAMING,
            modelIdentifier = conversation.modelIdentifier
        )
        val previousMessages = state.messages.filter { it.status == AiMessageStatus.COMPLETED }
        _uiState.update {
            it.copy(
                inputText = "",
                isResponding = true,
                messages = it.messages + userMessage + assistantMessage,
                errorMessage = null
            )
        }

        responseJob = viewModelScope.launch {
            try {
                repository.appendMessage(userMessage)
                repository.appendMessage(assistantMessage)
                updateAutomaticTitle(conversation, prompt)
                val context = compactAndBuildContext(conversation, previousMessages, apiKey)
                val currentConversation = repository.conversation(conversation.id) ?: conversation
                var accumulated = ""
                var continuationAttempts = 0
                var request = OpenRouterChatRequest(
                    instructions = AiInstructions.chat(
                        currentConversation.author,
                        currentConversation.usesPersonalVoice
                    ),
                    summary = currentConversation.summary,
                    messages = context,
                    prompt = prompt,
                    modelIdentifier = currentConversation.modelIdentifier
                )
                while (true) {
                    try {
                        client.streamChat(request, apiKey) { delta ->
                            accumulated += delta
                            updateAssistantDraft(assistantMessage.id, accumulated)
                        }
                        break
                    } catch (error: OpenRouterException) {
                        if (
                            error.kind == OpenRouterErrorKind.RESPONSE_TRUNCATED &&
                            continuationAttempts == 0 &&
                            accumulated.isNotBlank()
                        ) {
                            continuationAttempts++
                            request = request.copy(
                                messages = context + listOf(
                                    OpenRouterContextMessage(AiMessageRole.USER, prompt),
                                    OpenRouterContextMessage(AiMessageRole.ASSISTANT, accumulated)
                                ),
                                prompt = "Continúa exactamente desde el punto donde se interrumpió. Devuelve únicamente la continuación pendiente, sin repetir lo ya escrito."
                            )
                        } else {
                            throw error
                        }
                    }
                }
                if (accumulated.isBlank()) throw OpenRouterException(OpenRouterErrorKind.EMPTY_RESPONSE)
                val completed = currentAssistant(assistantMessage.id)?.copy(
                    text = accumulated,
                    status = AiMessageStatus.COMPLETED,
                    errorDescription = null
                ) ?: return@launch
                replaceMessage(completed)
                repository.updateMessage(completed)
                refreshConversations()
            } catch (_: CancellationException) {
                // cancelResponse persists the visible partial response.
            } catch (error: Throwable) {
                val failed = currentAssistant(assistantMessage.id)?.copy(
                    text = currentAssistant(assistantMessage.id)?.text
                        ?.takeIf { it.isNotBlank() }
                        ?: appContext.getString(R.string.ai_error_generate),
                    status = AiMessageStatus.FAILED,
                    errorDescription = error.message
                )
                if (failed != null) {
                    replaceMessage(failed)
                    runCatching { repository.updateMessage(failed) }
                }
                showError(error)
            } finally {
                _uiState.update { it.copy(isResponding = false) }
                responseJob = null
            }
        }
    }

    fun retryResponse(assistantMessageId: String) {
        val messages = _uiState.value.messages
        val index = messages.indexOfFirst { it.id == assistantMessageId }
        if (index <= 0) return
        val previousUser = messages.subList(0, index).lastOrNull { it.isUser } ?: return
        submitMessage(previousUser.text)
    }

    fun cancelResponse() {
        val streaming = _uiState.value.messages.lastOrNull {
            it.status == AiMessageStatus.STREAMING
        }
        responseJob?.cancel()
        if (streaming != null) {
            val cancelled = streaming.copy(
                text = streaming.text.ifBlank {
                    appContext.getString(R.string.ai_response_stopped)
                },
                status = AiMessageStatus.CANCELLED
            )
            replaceMessage(cancelled)
            viewModelScope.launch { runCatching { repository.updateMessage(cancelled) } }
        }
        _uiState.update { it.copy(isResponding = false) }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private suspend fun createConversationInternal(author: AiAuthor) {
        cancelResponse()
        val conversation = repository.createConversation(
            author = author,
            usesPersonalVoice = OpenRouterConfiguration.usesPersonalVoice(appContext),
            modelIdentifier = OpenRouterConfiguration.selectedModelIdentifier(appContext)
        )
        refreshConversations()
        _uiState.update {
            it.copy(
                activeConversation = conversation,
                messages = emptyList(),
                inputText = ""
            )
        }
    }

    private suspend fun refreshConversations() {
        _uiState.update { it.copy(conversations = repository.conversations()) }
    }

    private suspend fun updateAutomaticTitle(conversation: AiConversation, prompt: String) {
        val untitled = appContext.getString(R.string.ai_new_conversation)
        if (conversation.title != untitled && conversation.title != "Nueva conversación") return
        val title = prompt.replace("\n", " ").trim().take(52).ifBlank { untitled }
        repository.renameConversation(conversation.id, title)
        _uiState.update { it.copy(activeConversation = conversation.copy(title = title)) }
    }

    private suspend fun compactAndBuildContext(
        conversation: AiConversation,
        completed: List<AiMessage>,
        apiKey: String
    ): List<OpenRouterContextMessage> {
        val paired = completed.filter { it.status == AiMessageStatus.COMPLETED }
        val totalCharacters = paired.sumOf { it.text.length }
        var currentConversation = conversation
        if (paired.size > MAX_CONTEXT_MESSAGES || totalCharacters > MAX_CONTEXT_CHARACTERS) {
            val recent = paired.takeLast(RECENT_MESSAGES_AFTER_SUMMARY)
            val recentIds = recent.mapTo(hashSetOf()) { it.id }
            val older = paired.filterNot { it.id in recentIds }
            if (older.isNotEmpty()) {
                val text = older.joinToString("\n\n") {
                    "${if (it.isUser) "Usuario" else "Asistente"}: ${it.text}"
                }
                val summary = client.summarize(
                    text = text,
                    previousSummary = conversation.summary,
                    modelIdentifier = conversation.modelIdentifier,
                    apiKey = apiKey
                )
                repository.updateSummary(conversation.id, summary)
                currentConversation = conversation.copy(summary = summary)
                _uiState.update { it.copy(activeConversation = currentConversation) }
            }
        }

        var characterCount = 0
        val selected = mutableListOf<AiMessage>()
        for (message in paired.asReversed()) {
            val next = characterCount + message.text.length
            if (selected.size >= MAX_CONTEXT_MESSAGES || next > MAX_CONTEXT_CHARACTERS) break
            selected += message
            characterCount = next
        }
        return selected.asReversed().map {
            OpenRouterContextMessage(it.role, it.text)
        }
    }

    private fun updateAssistantDraft(id: String, text: String) {
        _uiState.update { state ->
            state.copy(messages = state.messages.map {
                if (it.id == id) it.copy(text = text) else it
            })
        }
    }

    private fun currentAssistant(id: String): AiMessage? =
        _uiState.value.messages.firstOrNull { it.id == id }

    private fun replaceMessage(message: AiMessage) {
        _uiState.update { state ->
            state.copy(messages = state.messages.map {
                if (it.id == message.id) message else it
            })
        }
    }

    private fun showError(error: Throwable) {
        _uiState.update {
            it.copy(errorMessage = error.localizedAiMessage(appContext))
        }
    }

    override fun onCleared() {
        responseJob?.cancel()
        super.onCleared()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val appContext = context.applicationContext
            val database = NevilleRoomDatabase.getInstance(appContext)
            return AiChatViewModel(
                appContext = appContext,
                repository = AiChatRepository(appContext, database.aiChatDao()),
                credentialStore = OpenRouterCredentialStore(appContext),
                client = OpenRouterClient()
            ) as T
        }
    }

    companion object {
        const val MAX_MESSAGE_CHARACTERS = 4_000
        private const val MAX_CONTEXT_MESSAGES = 12
        private const val MAX_CONTEXT_CHARACTERS = 24_000
        private const val RECENT_MESSAGES_AFTER_SUMMARY = 8
    }
}
