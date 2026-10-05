package com.ypg.neville.feature.transformation.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ypg.neville.feature.transformation.data.TransformationRepository
import com.ypg.neville.feature.transformation.domain.TransformationConfiguration
import com.ypg.neville.feature.transformation.domain.TransformationDayEntry
import com.ypg.neville.feature.transformation.domain.TransformationParaEvent
import com.ypg.neville.feature.transformation.domain.TransformationState
import com.ypg.neville.feature.transformation.notifications.TransformationReminderScheduler
import kotlinx.coroutines.flow.StateFlow

class TransformationViewModel(
    private val applicationContext: Context,
    private val repository: TransformationRepository
) : ViewModel() {
    val state: StateFlow<TransformationState> = repository.state

    fun start(configuration: TransformationConfiguration) {
        repository.start(configuration)
        TransformationReminderScheduler.sync(applicationContext, configuration)
    }

    fun updateConfiguration(configuration: TransformationConfiguration) {
        repository.updateConfiguration(configuration)
        TransformationReminderScheduler.sync(applicationContext, configuration)
    }

    fun entry(day: Int): TransformationDayEntry = repository.entry(day)
    fun updateEntry(entry: TransformationDayEntry) = repository.updateEntry(entry)

    fun completeMorning(day: Int) = repository.updateEntry(repository.entry(day).copy(morningCompleted = true))

    fun recordPara(signal: String, emotion: String, action: String, pauseSeconds: Int) =
        repository.recordPara(
            TransformationParaEvent(
                perceivedSignal = signal.trim(), emotion = emotion.trim(),
                alternativeAction = action.trim(), pauseSeconds = pauseSeconds
            )
        )

    fun reset() {
        repository.reset()
        TransformationReminderScheduler.sync(applicationContext, null)
    }

    class Factory(context: Context) : ViewModelProvider.Factory {
        private val appContext = context.applicationContext
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TransformationViewModel(appContext, TransformationRepository(appContext)) as T
    }
}
