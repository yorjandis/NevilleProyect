package com.ypg.neville.feature.ai.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels

class FragAiChat : Fragment() {
    private val viewModel: AiChatViewModel by viewModels {
        AiChatViewModel.Factory(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            com.ypg.neville.ui.theme.NevilleTheme {
                AiChatRoot(
                    viewModel = viewModel,
                    prefill = arguments?.getString(ARG_PREFILL),
                    onClose = { requireActivity().onBackPressedDispatcher.onBackPressed() }
                )
            }
        }
    }

    companion object {
        const val ARG_PREFILL = "ai_chat_prefill"
    }
}
