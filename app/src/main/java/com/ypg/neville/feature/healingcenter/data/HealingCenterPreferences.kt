package com.ypg.neville.feature.healingcenter.data

import android.content.Context
import androidx.core.content.edit
import com.ypg.neville.model.preferences.DbPreferences

class HealingCenterPreferences(context: Context) {
    private val preferences = DbPreferences.default(context.applicationContext)

    fun favoriteSituationIds(): Set<String> =
        preferences.getStringSet(KEY_FAVORITE_SITUATION_IDS, emptySet()).orEmpty()

    fun toggleFavorite(situationId: String): Set<String> {
        val updated = favoriteSituationIds().toMutableSet().apply {
            if (!add(situationId)) remove(situationId)
        }
        preferences.edit { putStringSet(KEY_FAVORITE_SITUATION_IDS, updated) }
        return updated
    }

    fun emergencyRegion(): String = preferences.getString(KEY_EMERGENCY_REGION, "").orEmpty()

    fun setEmergencyRegion(regionCode: String) {
        preferences.edit { putString(KEY_EMERGENCY_REGION, regionCode.uppercase()) }
    }

    companion object {
        private const val KEY_FAVORITE_SITUATION_IDS = "healing_center.favorite_situation_ids"
        private const val KEY_EMERGENCY_REGION = "healing_center.emergency_region"
    }
}
