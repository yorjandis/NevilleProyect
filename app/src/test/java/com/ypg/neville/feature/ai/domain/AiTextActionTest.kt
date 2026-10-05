package com.ypg.neville.feature.ai.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiTextActionTest {
    @Test
    fun interpretationAndPracticalApplicationRequireAnAuthor() {
        assertTrue(AiTextAction.INTERPRET.requiresAuthor)
        assertTrue(AiTextAction.CONCRETE_PRACTICE.requiresAuthor)
        assertFalse(AiTextAction.KEY_POINTS.requiresAuthor)
        assertFalse(AiTextAction.SUMMARY.requiresAuthor)
        assertFalse(AiTextAction.PRACTICES.requiresAuthor)
    }
}
