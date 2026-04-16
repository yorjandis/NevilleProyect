package com.ypg.neville.feature.morningdialog.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ypg.neville.feature.morningdialog.domain.MorningDialogRepository
import com.ypg.neville.feature.morningdialog.domain.MorningDialogSession
import com.ypg.neville.feature.morningdialog.notifications.MorningDialogScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

private const val MAX_ITEMS = 3

data class TriggerResponseInput(
    val trigger: String = "",
    val response: String = ""
)

data class MorningDialogFlowUiState(
    val step: Int = 1,
    val goals: List<String> = listOf(""),
    val identities: List<String> = emptyList(),
    val customIdentity: String = "",
    val emotions: List<String> = emptyList(),
    val customEmotion: String = "",
    val triggerResponses: List<TriggerResponseInput> = listOf(TriggerResponseInput()),
    val dayRemindersEnabled: Boolean = true,
    val dayReminderTimes: List<Int> = MorningDialogScheduler.DEFAULT_DAY_REMINDER_TIMES,
    val isSaving: Boolean = false,
    val isCompleted: Boolean = false,
    val validationMessage: String? = null
)

class MorningDialogFlowViewModel(
    private val repository: MorningDialogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MorningDialogFlowUiState())
    val uiState: StateFlow<MorningDialogFlowUiState> = _uiState.asStateFlow()

    fun setStep(step: Int) {
        _uiState.update {
            it.copy(step = step.coerceIn(1, 6), validationMessage = null)
        }
    }

    fun goNext(): Boolean {
        val current = _uiState.value
        val validationError = validateStep(current)
        if (validationError != null) {
            _uiState.update { it.copy(validationMessage = validationError) }
            return false
        }

        _uiState.update {
            it.copy(step = (it.step + 1).coerceAtMost(6), validationMessage = null)
        }
        return true
    }

    fun goBack(): Boolean {
        if (_uiState.value.step <= 1) return false
        _uiState.update {
            it.copy(step = (it.step - 1).coerceAtLeast(1), validationMessage = null)
        }
        return true
    }

    fun updateGoal(index: Int, value: String) {
        _uiState.update { state ->
            state.copy(goals = state.goals.toMutableList().apply { this[index] = value })
        }
    }

    fun addGoal() {
        _uiState.update { state ->
            if (state.goals.size >= MAX_ITEMS) state
            else state.copy(goals = state.goals + "")
        }
    }

    fun removeGoal(index: Int) {
        _uiState.update { state ->
            val updated = state.goals.toMutableList().apply {
                removeAt(index)
                if (isEmpty()) add("")
            }
            state.copy(goals = updated)
        }
    }

    fun toggleIdentity(value: String) {
        _uiState.update { state ->
            val current = state.identities.toMutableList()
            if (current.contains(value)) current.remove(value) else current.add(value)
            state.copy(identities = current)
        }
    }

    fun updateCustomIdentity(value: String) {
        _uiState.update { it.copy(customIdentity = value) }
    }

    fun toggleEmotion(value: String) {
        _uiState.update { state ->
            val current = state.emotions.toMutableList()
            if (current.contains(value)) {
                current.remove(value)
            } else {
                current.add(value)
            }
            state.copy(emotions = current)
        }
    }

    fun updateCustomEmotion(value: String) {
        _uiState.update { it.copy(customEmotion = value) }
    }

    fun setDayRemindersEnabled(enabled: Boolean) {
        _uiState.update { it.copy(dayRemindersEnabled = enabled) }
    }

    fun addDayReminderTime(minutesOfDay: Int) {
        _uiState.update { state ->
            if (state.dayReminderTimes.size >= MorningDialogScheduler.MAX_DAY_REMINDERS) return@update state
            val normalized = minutesOfDay.coerceIn(0, (24 * 60) - 1)
            val updated = (state.dayReminderTimes + normalized).distinct().sorted()
            state.copy(dayReminderTimes = updated)
        }
    }

    fun removeDayReminderTime(minutesOfDay: Int) {
        _uiState.update { state ->
            state.copy(dayReminderTimes = state.dayReminderTimes.filterNot { it == minutesOfDay })
        }
    }

    fun updateTrigger(index: Int, value: String) {
        _uiState.update { state ->
            val current = state.triggerResponses.toMutableList()
            current[index] = current[index].copy(trigger = value)
            state.copy(triggerResponses = current)
        }
    }

    fun updateResponse(index: Int, value: String) {
        _uiState.update { state ->
            val current = state.triggerResponses.toMutableList()
            current[index] = current[index].copy(response = value)
            state.copy(triggerResponses = current)
        }
    }

    fun addTriggerResponse() {
        _uiState.update { state ->
            if (state.triggerResponses.size >= MAX_ITEMS) state
            else state.copy(triggerResponses = state.triggerResponses + TriggerResponseInput())
        }
    }

    fun removeTriggerResponse(index: Int) {
        _uiState.update { state ->
            val current = state.triggerResponses.toMutableList().apply {
                removeAt(index)
                if (isEmpty()) add(TriggerResponseInput())
            }
            state.copy(triggerResponses = current)
        }
    }

    fun complete(onFinished: (sessionId: Long, remindersEnabled: Boolean, reminderTimes: List<Int>) -> Unit) {
        val validationError = validateStep(_uiState.value)
        if (validationError != null) {
            _uiState.update { it.copy(validationMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, validationMessage = null) }

            val now = System.currentTimeMillis()
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone).toEpochDay()
            val state = _uiState.value

            val normalizedGoals = state.goals.map { it.trim() }.filter { it.isNotEmpty() }
            val identities = buildList {
                addAll(state.identities.map { it.trim() }.filter { it.isNotEmpty() })
                val custom = state.customIdentity.trim()
                if (custom.isNotEmpty()) add(custom)
            }.distinct()
            val emotions = buildList {
                addAll(state.emotions)
                val custom = state.customEmotion.trim()
                if (custom.isNotEmpty()) add(custom)
            }.distinct()

            val normalizedPairs = state.triggerResponses
                .map { it.copy(trigger = it.trigger.trim(), response = it.response.trim()) }
                .filter { it.trigger.isNotEmpty() && it.response.isNotEmpty() }

            val session = MorningDialogSession(
                sessionDateEpochDay = today,
                completedAtEpochMillis = now,
                goals = normalizedGoals,
                identity = identities.joinToString(", "),
                emotions = emotions,
                anticipatedSituations = normalizedPairs.map { it.trigger },
                consciousResponses = normalizedPairs.map { it.response },
                noteText = "",
                completed = true
            )
            repository.saveSession(
                session
            )
            val persisted = repository.getTodaySession(today)
            val sessionId = persisted?.id ?: 0L

            _uiState.update { it.copy(isSaving = false, isCompleted = true, step = 6, validationMessage = null) }
            onFinished(
                sessionId,
                state.dayRemindersEnabled,
                state.dayReminderTimes.sorted().take(MorningDialogScheduler.MAX_DAY_REMINDERS)
            )
        }
    }

    fun clearCompletionFlag() {
        _uiState.update { it.copy(isCompleted = false) }
    }

    private fun validateStep(state: MorningDialogFlowUiState): String? {
        return when (state.step) {
            2 -> {
                val validGoals = state.goals.map { it.trim() }.filter { it.isNotEmpty() }
                when {
                    validGoals.isEmpty() -> "Añade al menos 1 meta para hoy."
                    validGoals.size > MAX_ITEMS -> "Solo puedes guardar hasta 3 metas."
                    else -> null
                }
            }

            3 -> {
                val hasCustom = state.customIdentity.trim().isNotEmpty()
                val hasSelected = state.identities.isNotEmpty()
                if (!hasCustom && !hasSelected) {
                    "Elige al menos una identidad o añade una personalizada."
                } else null
            }

            4 -> {
                val hasCustom = state.customEmotion.trim().isNotEmpty()
                val hasSelected = state.emotions.isNotEmpty()
                if (!hasCustom && !hasSelected) {
                    "Elige al menos una emoción o intención para cultivar hoy."
                } else null
            }

            5 -> {
                val validPairs = state.triggerResponses
                    .map { it.copy(trigger = it.trigger.trim(), response = it.response.trim()) }
                    .filter { it.trigger.isNotEmpty() && it.response.isNotEmpty() }
                if (validPairs.isEmpty()) {
                    "Completa al menos una situación y tu respuesta consciente."
                } else null
            }

            6 -> {
                if (state.dayRemindersEnabled && state.dayReminderTimes.isEmpty()) {
                    "Añade al menos una hora o desactiva los recordatorios del día."
                } else null
            }

            else -> null
        }
    }

    class Factory(
        private val repository: MorningDialogRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MorningDialogFlowViewModel(repository) as T
        }
    }
}
