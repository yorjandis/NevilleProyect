package com.ypg.neville.feature.healingcenter.domain

import android.content.Context
import com.ypg.neville.R

object HealingSafetyPolicy {
    fun urgentSignals(context: Context): List<String> =
        context.resources.getStringArray(R.array.healing_urgent_signals).toList()

    fun combinedSignals(context: Context, situation: HealingSituation): List<String> =
        (urgentSignals(context) + situation.redFlags).distinct()
}
