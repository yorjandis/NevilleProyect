package com.ypg.neville.ui.frag

import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.ypg.neville.R

data class AccessCardPlaceholder(
    val title: String,
    val primaryButton: String,
    val secondaryButton: String? = null
)

object AuthorPlaceholderUi {

    fun bind(view: View, authorName: String, imageRes: Int, cards: List<AccessCardPlaceholder>) {
        val authorImage = view.findViewById<ImageView>(R.id.author_image)
        val authorNameText = view.findViewById<TextView>(R.id.author_name)
        val biographyButton = view.findViewById<Button>(R.id.button_author_biography)
        val quoteText = view.findViewById<TextView>(R.id.quote_text)

        authorImage.setImageResource(imageRes)
        authorNameText.text = authorName
        biographyButton.text = view.context.getString(R.string.author_biography_button_placeholder)
        quoteText.text = view.context.getString(R.string.author_quote_placeholder, authorName)

        val cardIds = listOf(R.id.author_card_1, R.id.author_card_2, R.id.author_card_3)
        cardIds.forEachIndexed { index, cardId ->
            val cardRoot = view.findViewById<View>(cardId)
            val cardTitle = cardRoot.findViewById<TextView>(R.id.access_card_title)
            val primaryButton = cardRoot.findViewById<Button>(R.id.access_card_button_primary)
            val secondaryButton = cardRoot.findViewById<Button>(R.id.access_card_button_secondary)

            val card = cards.getOrNull(index)
            if (card == null) {
                cardRoot.visibility = View.GONE
                return@forEachIndexed
            }

            cardRoot.visibility = View.VISIBLE
            cardTitle.text = card.title
            primaryButton.text = card.primaryButton

            if (card.secondaryButton.isNullOrBlank()) {
                secondaryButton.visibility = View.GONE
            } else {
                secondaryButton.visibility = View.VISIBLE
                secondaryButton.text = card.secondaryButton
            }
        }
    }
}
