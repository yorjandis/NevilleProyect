package com.ypg.neville.ui.frag

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.ypg.neville.R

class FragNevilleGoddard : Fragment() {

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    AuthorPlaceholderScreen(
                        authorName = getString(R.string.neville_goddard),
                        imageRes = R.drawable.neville,
                        cards = listOf(
                            AccessCardPlaceholder("Recurso 1", "Abrir", "Guardar"),
                            AccessCardPlaceholder("Recurso 2", "Abrir", "Compartir"),
                            AccessCardPlaceholder("Recurso 3", "Abrir", null)
                        )
                    )
                }
            }
        }
    }
}
