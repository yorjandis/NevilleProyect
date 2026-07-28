package com.ypg.neville.localization

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Single entry point for reading or changing Neville's UI language.
 *
 * An empty locale list means that the app follows the device language. When
 * that language is unsupported, Android falls back to the Spanish resources.
 */
object AppLocaleManager {

    fun selectedLanguage(): AppLanguage? {
        val selectedTag = AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag()
        return AppLanguage.fromLanguageTag(selectedTag)
    }

    fun setLanguage(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(language.languageTag)
        )
    }

    fun followSystemLanguage() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    }
}
