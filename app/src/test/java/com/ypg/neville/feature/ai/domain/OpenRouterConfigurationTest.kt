package com.ypg.neville.feature.ai.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenRouterConfigurationTest {
    @Test
    fun onlyAutomaticOrExplicitFreeModelsAreAllowed() {
        assertTrue(
            OpenRouterConfiguration.isFreeModelIdentifier(
                OpenRouterConfiguration.AUTOMATIC_FREE_MODEL
            )
        )
        assertTrue(OpenRouterConfiguration.isFreeModelIdentifier("vendor/model:free"))
        assertFalse(OpenRouterConfiguration.isFreeModelIdentifier("vendor/model"))
        assertFalse(OpenRouterConfiguration.isFreeModelIdentifier("vendor/free-model"))
    }
}
