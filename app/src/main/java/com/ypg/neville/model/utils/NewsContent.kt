package com.ypg.neville.model.utils

import android.content.Context
import com.ypg.neville.R

object NewsContent {

    @JvmStatic
    fun buildNewsText(context: Context): String = listOf(
        context.getString(R.string.main_news_intro),
        context.getString(R.string.main_news_version_title),
        context.getString(R.string.main_news_body),
        context.getString(R.string.main_news_closing)
    ).joinToString(separator = "\n\n")
}
