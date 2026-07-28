package com.ypg.neville.localization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLanguageTest {

    @Test
    fun `recognizes every supported language tag`() {
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromLanguageTag("es-ES"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLanguageTag("en-US"))
        assertEquals(AppLanguage.SIMPLIFIED_CHINESE, AppLanguage.fromLanguageTag("zh-Hans-CN"))
        assertEquals(AppLanguage.SIMPLIFIED_CHINESE, AppLanguage.fromLanguageTag("zh_CN"))
    }

    @Test
    fun `returns null for the system option or an unsupported language`() {
        assertNull(AppLanguage.fromLanguageTag(null))
        assertNull(AppLanguage.fromLanguageTag(""))
        assertNull(AppLanguage.fromLanguageTag("fr"))
    }
}
