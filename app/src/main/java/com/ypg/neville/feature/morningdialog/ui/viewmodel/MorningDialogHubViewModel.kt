package com.ypg.neville.feature.morningdialog.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ypg.neville.feature.morningdialog.data.MorningDialogSettings
import com.ypg.neville.feature.morningdialog.data.MorningDialogSettingsDataStore
import com.ypg.neville.feature.morningdialog.domain.MorningDialogRepository
import com.ypg.neville.feature.morningdialog.domain.MorningDialogSession
import com.ypg.neville.feature.morningdialog.notifications.MorningDialogScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId


data class MorningDialogHubUiState(
    val settings: MorningDialogSettings = MorningDialogSettings(),
    val sessions: List<MorningDialogSession> = emptyList(),
    val todayCompleted: Boolean = false,
    val todaySessionId: Long? = null
)

class MorningDialogHubViewModel(
    private val settingsDataStore: MorningDialogSettingsDataStore,
    private val repository: MorningDialogRepository,
    private val scheduler: MorningDialogScheduler
) : ViewModel() {

    private val _selectedSession = MutableStateFlow<MorningDialogSession?>(null)
    val selectedSession: StateFlow<MorningDialogSession?> = _selectedSession.asStateFlow()

    val uiState: StateFlow<MorningDialogHubUiState> = combine(
        settingsDataStore.settingsFlow,
        repository.observeSessions()
    ) { settings, sessions ->
        val todayEpoch = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        val todaySession = sessions.firstOrNull { it.sessionDateEpochDay == todayEpoch && it.completed }
        MorningDialogHubUiState(
            settings = settings,
            sessions = sessions,
            todayCompleted = todaySession != null,
            todaySessionId = todaySession?.id
        )
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
        initialValue = MorningDialogHubUiState()
    )

    fun applySettings(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsDataStore.setTime(hour, minute)
            settingsDataStore.setEnabled(enabled)
            if (enabled) {
                scheduler.scheduleDaily(hour, minute)
            } else {
                scheduler.cancel()
            }
        }
    }

    fun loadSession(sessionId: Long) {
        viewModelScope.launch {
            _selectedSession.value = repository.getSession(sessionId)
        }
    }

    fun saveSessionNote(sessionId: Long, noteText: String, onSaved: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.updateSessionNote(sessionId, noteText)
            _selectedSession.value = repository.getSession(sessionId)
            onSaved?.invoke()
        }
    }

    suspend fun getTodayCompletedSessionId(): Long? {
        val todayEpoch = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
        val session = repository.getTodaySession(todayEpoch)
        return session?.takeIf { it.completed }?.id
    }

    class Factory(
        private val settingsDataStore: MorningDialogSettingsDataStore,
        private val repository: MorningDialogRepository,
        private val scheduler: MorningDialogScheduler
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MorningDialogHubViewModel(settingsDataStore, repository, scheduler) as T
        }
    }
}
