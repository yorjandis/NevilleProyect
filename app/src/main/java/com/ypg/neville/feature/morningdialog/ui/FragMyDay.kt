package com.ypg.neville.feature.morningdialog.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.feature.morningdialog.data.RoomMorningDialogRepository
import com.ypg.neville.feature.morningdialog.data.MorningDialogSettingsDataStore
import com.ypg.neville.feature.morningdialog.domain.EveningRitualRepository
import com.ypg.neville.feature.morningdialog.ui.screens.EveningReviewScreen
import com.ypg.neville.feature.morningdialog.ui.screens.MyDayScreen
import com.ypg.neville.feature.morningdialog.ui.screens.RitualPrivacyUnlockScreen
import com.ypg.neville.feature.morningdialog.ui.viewmodel.RitualCycleViewModel
import com.ypg.neville.model.db.room.NevilleRoomDatabase

class FragMyDay : Fragment() {
    private val database by lazy { NevilleRoomDatabase.getInstance(requireContext().applicationContext) }
    private val morningRepository by lazy { RoomMorningDialogRepository(database.morningDialogDao()) }
    private val settingsStore by lazy { MorningDialogSettingsDataStore(requireContext().applicationContext) }
    private val cycleViewModel: RitualCycleViewModel by viewModels {
        RitualCycleViewModel.Factory(EveningRitualRepository(database), morningRepository)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                com.ypg.neville.ui.theme.NevilleTheme {
                    MyDayRoot(cycleViewModel, settingsStore) { parentFragmentManager.popBackStack() }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyDayRoot(
    viewModel: RitualCycleViewModel,
    settingsStore: MorningDialogSettingsDataStore,
    onClose: () -> Unit
) {
    val state by viewModel.dayState.collectAsState()
    val reviews by viewModel.reviews.collectAsState()
    val settings by settingsStore.settingsFlow.collectAsState(initial = null)
    var showReview by remember { mutableStateOf(false) }
    var unlocked by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (showReview) stringResource(R.string.ritual_evening) else stringResource(R.string.ritual_my_day)) },
                navigationIcon = {
                    IconButton(onClick = { if (showReview) showReview = false else onClose() }) {
                        Text("←", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (settings == null) {
                CircularProgressIndicator(Modifier.align(androidx.compose.ui.Alignment.Center))
            } else if (settings?.protectClosingReflections == true && !unlocked) {
                RitualPrivacyUnlockScreen { unlocked = true }
            } else if (showReview) {
                EveningReviewScreen(
                    state = state,
                    weeklyClosures = reviews.count { it.sessionDateEpochDay in (state.epochDay - 6)..state.epochDay },
                    onDateChange = viewModel::loadDay,
                    onSave = { viewModel.saveReview(it) },
                    onOpenDiary = { MainActivity.currentInstance()?.openDestinationAsSheet(R.id.frag_diario) }
                )
            } else {
                MyDayScreen(
                    state = state,
                    onDateChange = viewModel::loadDay,
                    onOpenMorning = { MainActivity.currentInstance()?.openDestinationAsSheet(R.id.frag_morning_dialog) },
                    onOpenAgenda = { MainActivity.currentInstance()?.openDestinationAsSheet(R.id.frag_agenda) },
                    onOpenGoals = { MainActivity.currentInstance()?.openDestinationAsSheet(R.id.frag_metas) },
                    onOpenPresence = { MainActivity.currentInstance()?.openDestinationAsSheet(R.id.frag_presence) },
                    onOpenCoherence = { MainActivity.currentInstance()?.openDestinationAsSheet(R.id.frag_cardio_coherence) },
                    onOpenReview = { showReview = true }
                )
            }
        }
    }
}
