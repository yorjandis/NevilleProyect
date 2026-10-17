package com.ypg.neville.feature.premiumpreview

import org.junit.Assert.assertEquals
import org.junit.Test

class PremiumFeaturePreviewTest {

    @Test
    fun hidesMissingSlotsWhenAtLeastOneScreenshotExists() {
        val displayedNames = selectDisplayedScreenshotNames(
            declaredNames = listOf("Agenda_1", "Agenda_2", "Agenda_3", "Agenda_4", "Agenda_5"),
            availableAssetFiles = setOf(".gitkeep", "Agenda_1.jpg", "Agenda_2.jpg", "Agenda_3.jpg")
        )

        assertEquals(listOf("Agenda_1", "Agenda_2", "Agenda_3"), displayedNames)
    }

    @Test
    fun keepsPlaceholdersWhenNoScreenshotExistsYet() {
        val declaredNames = listOf("Agenda_1", "Agenda_2")

        val displayedNames = selectDisplayedScreenshotNames(
            declaredNames = declaredNames,
            availableAssetFiles = setOf(".gitkeep")
        )

        assertEquals(declaredNames, displayedNames)
    }

    @Test
    fun recognizesEverySupportedScreenshotExtension() {
        val displayedNames = selectDisplayedScreenshotNames(
            declaredNames = listOf("One", "Two", "Three", "Four", "Missing"),
            availableAssetFiles = setOf("One.png", "Two.webp", "Three.jpg", "Four.jpeg")
        )

        assertEquals(listOf("One", "Two", "Three", "Four"), displayedNames)
    }
}
