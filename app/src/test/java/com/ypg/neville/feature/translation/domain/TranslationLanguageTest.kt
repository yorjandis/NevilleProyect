package com.ypg.neville.feature.translation.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TranslationLanguageTest {

    @Test
    fun `maps supported regional tags to the translation languages`() {
        assertEquals(TranslationLanguage.SPANISH, TranslationLanguage.fromLanguageTag("es-MX"))
        assertEquals(TranslationLanguage.ENGLISH, TranslationLanguage.fromLanguageTag("en-US"))
        assertEquals(
            TranslationLanguage.SIMPLIFIED_CHINESE,
            TranslationLanguage.fromLanguageTag("zh-Hans-CN")
        )
        assertEquals(
            TranslationLanguage.SIMPLIFIED_CHINESE,
            TranslationLanguage.fromLanguageTag("zh_CN")
        )
    }

    @Test
    fun `rejects undetermined and unsupported languages`() {
        assertNull(TranslationLanguage.fromLanguageTag(null))
        assertNull(TranslationLanguage.fromLanguageTag("und"))
        assertNull(TranslationLanguage.fromLanguageTag("fr"))
    }
}
