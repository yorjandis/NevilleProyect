package com.ypg.neville.feature.morningdialog.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ypg.neville.feature.morningdialog.domain.EveningDaySnapshot
import com.ypg.neville.feature.morningdialog.domain.EveningReview
import com.ypg.neville.feature.morningdialog.domain.EveningReviewDraft
import com.ypg.neville.feature.morningdialog.domain.EveningRitualRepository
import com.ypg.neville.feature.morningdialog.domain.MorningDialogRepository
import com.ypg.neville.feature.morningdialog.domain.MorningDialogSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class RitualDayUiState(
    val epochDay: Long = LocalDate.now().toEpochDay(),
    val snapshot: EveningDaySnapshot = EveningDaySnapshot(),
    val morningSession: MorningDialogSession? = null,
    val review: EveningReview? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val errorMessage: String? = null
)

class RitualCycleViewModel(
    private val repository: EveningRitualRepository,
    private val morningRepository: MorningDialogRepository
) : ViewModel() {
    val reviews: StateFlow<List<EveningReview>> = repository.observeReviews().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val _dayState = MutableStateFlow(RitualDayUiState())
    val dayState: StateFlow<RitualDayUiState> = _dayState.asStateFlow()

    init {
        loadDay(LocalDate.now().toEpochDay())
    }

    fun loadDay(epochDay: Long) {
        viewModelScope.launch {
            _dayState.value = RitualDayUiState(epochDay = epochDay, loading = true)
            runCatching {
                val date = LocalDate.ofEpochDay(epochDay)
                val zone = ZoneId.systemDefault()
                val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
                val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                RitualDayUiState(
                    epochDay = epochDay,
                    snapshot = repository.snapshot(start, end),
                    morningSession = morningRepository.getTodaySession(epochDay),
                    review = repository.reviewForDay(epochDay),
                    loading = false
                )
            }.onSuccess { _dayState.value = it }
                .onFailure {
                    _dayState.value = _dayState.value.copy(
                        loading = false,
                        errorMessage = "No se pudo preparar la información del día."
                    )
                }
        }
    }

    fun saveReview(draft: EveningReviewDraft, onSaved: (EveningReview) -> Unit = {}) {
        val current = _dayState.value
        viewModelScope.launch {
            _dayState.value = current.copy(saving = true, errorMessage = null)
            runCatching {
                val date = LocalDate.ofEpochDay(current.epochDay)
                val dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                repository.saveReview(
                    epochDay = current.epochDay,
                    dayStartMillis = dayStart,
                    draft = draft,
                    snapshot = current.snapshot,
                    morningSession = current.morningSession,
                    recentReviews = repository.reviews().filter {
                        it.sessionDateEpochDay in (current.epochDay - 6)..current.epochDay
                    }
                )
            }.onSuccess { review ->
                _dayState.value = current.copy(review = review, saving = false)
                onSaved(review)
            }.onFailure {
                _dayState.value = current.copy(
                    saving = false,
                    errorMessage = "No se pudo guardar el cierre. Inténtalo de nuevo."
                )
            }
        }
    }

    fun deleteReview(reviewId: Long) {
        viewModelScope.launch {
            repository.deleteReview(reviewId)
            if (_dayState.value.review?.id == reviewId) loadDay(_dayState.value.epochDay)
        }
    }

    class Factory(
        private val repository: EveningRitualRepository,
        private val morningRepository: MorningDialogRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RitualCycleViewModel(repository, morningRepository) as T
    }
}
