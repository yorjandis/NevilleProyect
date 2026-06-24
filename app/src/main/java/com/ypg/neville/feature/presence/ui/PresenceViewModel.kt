package com.ypg.neville.feature.presence.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ypg.neville.feature.presence.data.PresenceDayStats
import com.ypg.neville.feature.presence.data.PresenceEventPoint
import com.ypg.neville.feature.presence.data.PresenceMood
import com.ypg.neville.feature.presence.data.PresenceMoodStats
import com.ypg.neville.feature.presence.data.PresenceRepository
import com.ypg.neville.feature.presence.data.PresenceStreakStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PresenceUiState(
    val todayPresentCount: Int = 0,
    val dayStats: List<PresenceDayStats> = emptyList(),
    val moodStats: List<PresenceMoodStats> = emptyList(),
    val eventPoints: List<PresenceEventPoint> = emptyList(),
    val futureFeelingCount: Int = 0,
    val streakStats: PresenceStreakStats = PresenceStreakStats(0, 0),
    val showCelebration: Boolean = false,
    val showMilestone: Boolean = false
)

class PresenceViewModel(
    private val repository: PresenceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PresenceUiState())
    val uiState: StateFlow<PresenceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeChanges().collect {
                loadState()
            }
        }
        reload()
    }

    fun recordPresent(mood: PresenceMood? = null) {
        viewModelScope.launch {
            repository.recordPresent(mood)
            loadState()
            _uiState.update {
                it.copy(
                    showCelebration = true,
                    showMilestone = it.todayPresentCount >= 10
                )
            }
        }
    }

    fun recordMood(mood: PresenceMood) {
        viewModelScope.launch {
            repository.recordMood(mood)
            loadState()
        }
    }

    fun dismissCelebration() {
        _uiState.update { it.copy(showCelebration = false) }
    }

    fun dismissMilestone() {
        _uiState.update { it.copy(showMilestone = false) }
    }

    fun resetAll() {
        viewModelScope.launch {
            repository.resetAll()
            loadState()
        }
    }

    private fun reload() {
        viewModelScope.launch {
            loadState()
        }
    }

    private suspend fun loadState() {
        _uiState.update {
            it.copy(
                todayPresentCount = repository.todayPresentCount(),
                dayStats = repository.dayStats(90),
                moodStats = repository.moodStats(90),
                eventPoints = repository.eventPoints(90),
                futureFeelingCount = repository.futureFeelingCount(90),
                streakStats = repository.streakStats(90)
            )
        }
    }

    class Factory(private val repository: PresenceRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PresenceViewModel(repository) as T
        }
    }
}
