package com.ypg.neville.feature.cardiocoherence.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ypg.neville.feature.cardiocoherence.data.CardioCoherenceRepository
import com.ypg.neville.feature.cardiocoherence.domain.BreathingRhythmOption
import com.ypg.neville.feature.cardiocoherence.domain.GeneratedCardioCoherenceSession
import com.ypg.neville.feature.cardiocoherence.domain.GuidanceAudioService
import com.ypg.neville.feature.cardiocoherence.domain.InitialEmotionalState
import com.ypg.neville.feature.cardiocoherence.domain.MeditationPhaseKind
import com.ypg.neville.feature.cardiocoherence.domain.MeditationSessionRecord
import com.ypg.neville.feature.cardiocoherence.domain.NoOpGuidanceAudioService
import com.ypg.neville.feature.cardiocoherence.domain.PostSessionEmotion
import com.ypg.neville.feature.cardiocoherence.domain.SessionDurationOption
import com.ypg.neville.feature.cardiocoherence.domain.SessionEngine
import com.ypg.neville.feature.cardiocoherence.domain.UserStateModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class CardioCoherenceStage {
    SETUP,
    SESSION,
    EVALUATION,
    SUMMARY
}

data class CardioCoherenceUiState(
    val stage: CardioCoherenceStage = CardioCoherenceStage.SETUP,
    val selectedState: InitialEmotionalState = InitialEmotionalState.NEUTRAL,
    val durationOption: SessionDurationOption = SessionDurationOption.TEN,
    val breathingRhythm: BreathingRhythmOption = BreathingRhythmOption.FIVE_HALF_FIVE_HALF,
    val intention: String = "",
    val beforeScore: Int = 5,
    val afterScore: Int = 7,
    val mentalClarityScore: Int = 7,
    val heartConnectionScore: Int = 7,
    val predominantEmotion: PostSessionEmotion = PostSessionEmotion.CALM,
    val closingWord: String = "",
    val records: List<MeditationSessionRecord> = emptyList(),
    val session: GeneratedCardioCoherenceSession? = null,
    val elapsedSeconds: Int = 0,
    val preparationRemainingSeconds: Int = 0,
    val currentPhaseIndex: Int = 0,
    val isPaused: Boolean = false,
    val isSaving: Boolean = false,
    val savedRecordId: Long? = null
) {
    val totalSeconds: Int = session?.totalDurationSeconds ?: durationOption.minutes * 60
    val isPreparing: Boolean = preparationRemainingSeconds > 0
    val remainingSeconds: Int = (totalSeconds - elapsedSeconds).coerceAtLeast(0)
    val currentPhaseElapsedSeconds: Int
        get() {
            val phases = session?.phases ?: return 0
            return elapsedSeconds - phases.take(currentPhaseIndex).sumOf { it.durationSeconds }
        }
    val currentPhaseRemainingSeconds: Int
        get() {
            val phase = session?.phases?.getOrNull(currentPhaseIndex) ?: return 0
            return (phase.durationSeconds - currentPhaseElapsedSeconds).coerceAtLeast(0)
        }
}

class CardioCoherenceViewModel(
    private val repository: CardioCoherenceRepository,
    initialBreathingRhythm: BreathingRhythmOption = BreathingRhythmOption.FIVE_HALF_FIVE_HALF,
    private val sessionEngine: SessionEngine = SessionEngine(),
    private val guidanceAudioService: GuidanceAudioService = NoOpGuidanceAudioService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CardioCoherenceUiState(breathingRhythm = initialBreathingRhythm)
    )
    val uiState: StateFlow<CardioCoherenceUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.observeRecords().collect { records ->
                _uiState.update { it.copy(records = records) }
            }
        }
    }

    fun refreshRecords() {
        viewModelScope.launch {
            val records = withContext(Dispatchers.IO) {
                repository.loadRecords()
            }
            _uiState.update { it.copy(records = records) }
        }
    }

    fun selectInitialState(value: InitialEmotionalState) {
        _uiState.update { it.copy(selectedState = value) }
    }

    fun selectDuration(value: SessionDurationOption) {
        _uiState.update { it.copy(durationOption = value) }
    }

    fun selectBreathingRhythm(value: BreathingRhythmOption) {
        _uiState.update { it.copy(breathingRhythm = value) }
    }

    fun updateIntention(value: String) {
        _uiState.update { it.copy(intention = value.take(180)) }
    }

    fun updateBeforeScore(value: Int) {
        _uiState.update { it.copy(beforeScore = value.coerceIn(1, 10)) }
    }

    fun updateAfterScore(value: Int) {
        _uiState.update { it.copy(afterScore = value.coerceIn(1, 10)) }
    }

    fun updateMentalClarityScore(value: Int) {
        _uiState.update { it.copy(mentalClarityScore = value.coerceIn(1, 10)) }
    }

    fun updateHeartConnectionScore(value: Int) {
        _uiState.update { it.copy(heartConnectionScore = value.coerceIn(1, 10)) }
    }

    fun updatePredominantEmotion(value: PostSessionEmotion) {
        _uiState.update { it.copy(predominantEmotion = value) }
    }

    fun updateClosingWord(value: String) {
        _uiState.update { it.copy(closingWord = value.take(36)) }
    }

    fun startSession() {
        val state = _uiState.value
        val userState = UserStateModel(
            initialState = state.selectedState,
            durationMinutes = state.durationOption.minutes,
            intention = state.intention.trim(),
            beforeScore = state.beforeScore,
            breathingRhythm = state.breathingRhythm
        )
        val session = sessionEngine.generate(userState)
        tickerJob?.cancel()
        _uiState.update {
            it.copy(
                stage = CardioCoherenceStage.SESSION,
                session = session,
                elapsedSeconds = 0,
                preparationRemainingSeconds = SESSION_PREPARATION_SECONDS,
                currentPhaseIndex = 0,
                isPaused = false,
                savedRecordId = null
            )
        }
        viewModelScope.launch { guidanceAudioService.prepare(session) }
        startTicker()
    }

    fun pause() {
        _uiState.update { it.copy(isPaused = true) }
        viewModelScope.launch { guidanceAudioService.pause() }
    }

    fun resume() {
        _uiState.update { it.copy(isPaused = false) }
        viewModelScope.launch { guidanceAudioService.resume() }
    }

    fun finishSession() {
        tickerJob?.cancel()
        viewModelScope.launch { guidanceAudioService.stop() }
        _uiState.update {
            it.copy(
                stage = CardioCoherenceStage.EVALUATION,
                isPaused = true,
                afterScore = it.session?.userState?.beforeScore ?: it.beforeScore,
                mentalClarityScore = it.mentalClarityScore.coerceIn(1, 10),
                heartConnectionScore = it.heartConnectionScore.coerceIn(1, 10)
            )
        }
    }

    fun resetFlow() {
        tickerJob?.cancel()
        _uiState.update {
            CardioCoherenceUiState(
                selectedState = it.selectedState,
                durationOption = it.durationOption,
                breathingRhythm = it.breathingRhythm,
                beforeScore = it.afterScore.coerceIn(1, 10),
                mentalClarityScore = it.mentalClarityScore,
                heartConnectionScore = it.heartConnectionScore,
                predominantEmotion = it.predominantEmotion
            )
        }
    }

    fun saveEvaluation() {
        val state = _uiState.value
        val session = state.session ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val record = MeditationSessionRecord(
                dateEpochMillis = System.currentTimeMillis(),
                durationMinutes = (state.elapsedSeconds.coerceAtLeast(1) + 59) / 60,
                initialState = session.userState.initialState,
                intention = session.userState.intention,
                beforeScore = session.userState.beforeScore,
                afterScore = state.afterScore,
                mentalClarityScore = state.mentalClarityScore,
                heartConnectionScore = state.heartConnectionScore,
                predominantEmotion = state.predominantEmotion,
                closingWord = state.closingWord.trim(),
                phasesCompleted = completedPhases(state)
            )
            val id = withContext(Dispatchers.IO) { repository.save(record) }
            _uiState.update {
                it.copy(
                    stage = CardioCoherenceStage.SUMMARY,
                    isSaving = false,
                    savedRecordId = id
                )
            }
        }
    }

    private fun startTicker() {
        tickerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val state = _uiState.value
                val session = state.session ?: break
                if (state.stage != CardioCoherenceStage.SESSION) break
                if (state.isPaused) continue

                if (state.preparationRemainingSeconds > 0) {
                    _uiState.update {
                        it.copy(
                            preparationRemainingSeconds = (it.preparationRemainingSeconds - 1).coerceAtLeast(0)
                        )
                    }
                    continue
                }

                val nextElapsed = (state.elapsedSeconds + 1).coerceAtMost(session.totalDurationSeconds)
                val nextPhaseIndex = phaseIndexFor(session, nextElapsed)
                _uiState.update {
                    it.copy(
                        elapsedSeconds = nextElapsed,
                        currentPhaseIndex = nextPhaseIndex
                    )
                }

                if (nextElapsed >= session.totalDurationSeconds) {
                    finishSession()
                    break
                }
            }
        }
    }

    private fun phaseIndexFor(session: GeneratedCardioCoherenceSession, elapsedSeconds: Int): Int {
        var accumulated = 0
        session.phases.forEachIndexed { index, phase ->
            accumulated += phase.durationSeconds
            if (elapsedSeconds < accumulated) return index
        }
        return session.phases.lastIndex.coerceAtLeast(0)
    }

    private fun completedPhases(state: CardioCoherenceUiState): List<MeditationPhaseKind> {
        val session = state.session ?: return emptyList()
        val result = mutableListOf<MeditationPhaseKind>()
        var remaining = state.elapsedSeconds
        session.phases.forEach { phase ->
            if (remaining >= phase.durationSeconds) {
                result.add(phase.kind)
            }
            remaining -= phase.durationSeconds
        }
        return result
    }

    override fun onCleared() {
        tickerJob?.cancel()
        viewModelScope.launch { guidanceAudioService.stop() }
        super.onCleared()
    }

    class Factory(
        private val repository: CardioCoherenceRepository,
        private val initialBreathingRhythm: BreathingRhythmOption = BreathingRhythmOption.FIVE_HALF_FIVE_HALF
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CardioCoherenceViewModel(
                repository = repository,
                initialBreathingRhythm = initialBreathingRhythm
            ) as T
        }
    }

    companion object {
        private const val SESSION_PREPARATION_SECONDS = 5
    }
}
