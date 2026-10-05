package com.ypg.neville.feature.premiumpreview

import com.ypg.neville.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PremiumFeatureIdTest {

    @Test
    fun destinationMappingMatchesSharedIosPremiumFeatures() {
        val expected = mapOf(
            R.id.frag_agenda to PremiumFeatureId.AGENDA,
            R.id.frag_healing_center to PremiumFeatureId.HEALING_CENTER,
            R.id.frag_cardio_coherence to PremiumFeatureId.CARDIO_COHERENCE,
            R.id.frag_calm_space to PremiumFeatureId.CALM_SPACE,
            R.id.frag_ai_chat to PremiumFeatureId.INTEGRATED_AI,
            R.id.frag_ai_text_tool to PremiumFeatureId.INTEGRATED_AI,
            R.id.frag_lienzo to PremiumFeatureId.CREATIVE_CANVAS,
            R.id.frag_metas to PremiumFeatureId.GOALS,
            R.id.frag_presence to PremiumFeatureId.CONSCIOUS_PRESENCE,
            R.id.frag_transformation_protocol to PremiumFeatureId.TRANSFORMATION_PROTOCOL,
            R.id.frag_reminders to PremiumFeatureId.SMART_REMINDERS,
            R.id.frag_weekly_summary to PremiumFeatureId.WEEKLY_REVIEW,
            R.id.frag_morning_dialog to PremiumFeatureId.CONSCIOUS_DAILY_CYCLE,
            R.id.frag_my_day to PremiumFeatureId.CONSCIOUS_DAILY_CYCLE
        )

        expected.forEach { (destination, feature) ->
            assertEquals(feature, PremiumFeatureId.forDestination(destination))
        }
        assertNull(PremiumFeatureId.forDestination(R.id.frag_voice_recordings))
        assertNull(PremiumFeatureId.forDestination(R.id.frag_emotional_anchors))
    }

    @Test
    fun wireNamesRoundTrip() {
        PremiumFeatureId.entries.forEach { feature ->
            assertEquals(feature, PremiumFeatureId.fromWireName(feature.wireName))
        }
        assertNull(PremiumFeatureId.fromWireName("not-a-feature"))
    }

    @Test
    fun screenshotPathsRemainInsidePremiumAssetRoot() {
        PremiumFeatureId.entries.forEach { feature ->
            assertEquals(
                "${PremiumFeatureId.ASSET_ROOT}/${feature.assetDirectory}",
                feature.assetPath
            )
        }
    }
}
