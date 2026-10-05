package com.ypg.neville.feature.transformation.data

import android.content.Context
import androidx.core.content.edit
import com.ypg.neville.feature.transformation.domain.TransformationConfiguration
import com.ypg.neville.feature.transformation.domain.TransformationDayEntry
import com.ypg.neville.feature.transformation.domain.TransformationParaEvent
import com.ypg.neville.feature.transformation.domain.TransformationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TransformationRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<TransformationState> = _state.asStateFlow()

    @Synchronized
    fun start(configuration: TransformationConfiguration) = commit(TransformationState(configuration = configuration))

    @Synchronized
    fun updateConfiguration(configuration: TransformationConfiguration) =
        commit(_state.value.copy(configuration = configuration))

    @Synchronized
    fun updateEntry(entry: TransformationDayEntry) {
        val updated = entry.copy(updatedAtMillis = System.currentTimeMillis())
        val entries = _state.value.entries.filterNot { it.day == entry.day }.plus(updated).sortedBy { it.day }
        val completedAt = if (entries.count { it.isCompleted } == 21) {
            _state.value.completedAtMillis ?: System.currentTimeMillis()
        } else null
        commit(_state.value.copy(entries = entries, completedAtMillis = completedAt))
    }

    fun entry(day: Int): TransformationDayEntry = _state.value.entries.firstOrNull { it.day == day }
        ?: TransformationDayEntry(day = day)

    @Synchronized
    fun recordPara(event: TransformationParaEvent) = commit(
        _state.value.copy(paraEvents = (listOf(event) + _state.value.paraEvents).take(250))
    )

    @Synchronized
    fun reset() = commit(TransformationState())

    private fun load(): TransformationState {
        val primary = preferences.getString(KEY_STATE, null)
        val backup = preferences.getString(KEY_BACKUP, null)
        return sequenceOf(primary, backup).filterNotNull().mapNotNull {
            runCatching { TransformationStateCodec.decode(it) }.getOrNull()
        }.firstOrNull() ?: TransformationState()
    }

    private fun commit(state: TransformationState) {
        val encoded = TransformationStateCodec.encode(state)
        preferences.edit(commit = true) {
            preferences.getString(KEY_STATE, null)?.let { putString(KEY_BACKUP, it) }
            putString(KEY_STATE, encoded)
        }
        _state.value = state
    }

    private companion object {
        const val PREFERENCES = "transformation_protocol"
        const val KEY_STATE = "state_v1"
        const val KEY_BACKUP = "state_backup_v1"
    }
}
