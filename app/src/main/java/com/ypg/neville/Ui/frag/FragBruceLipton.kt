package com.ypg.neville.ui.frag

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.ypg.neville.R
import com.ypg.neville.model.db.utilsDB

class FragBruceLipton : Fragment() {

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                com.ypg.neville.ui.theme.NevilleTheme {
                    val author = getString(R.string.bruce_lipton)
                    val placeholder = getString(R.string.author_quote_placeholder, author)
                    var quote by remember {
                        mutableStateOf(utilsDB.getRandomFraseByAutor(requireContext(), author)?.frase ?: placeholder)
                    }
                    AuthorPlaceholderScreen(
                        authorName = author,
                        imageRes = R.drawable.bruce,
                        quote = quote,
                        onQuoteClick = {
                            quote = utilsDB.getRandomFraseByAutor(requireContext(), author)?.frase ?: placeholder
                        },
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
