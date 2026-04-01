package com.ypg.neville.ui.frag

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ypg.neville.R

class FragNevilleGoddard : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.frag_author_placeholder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AuthorPlaceholderUi.bind(
            view = view,
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
