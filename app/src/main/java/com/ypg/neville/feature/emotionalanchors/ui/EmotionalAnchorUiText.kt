package com.ypg.neville.feature.emotionalanchors.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ypg.neville.R

@Composable
internal fun localizedBreathingName(id: String, fallback: String): String =
    localizedBreathingText(id, fallback, BreathingTextKind.NAME)

@Composable
internal fun localizedBreathingPattern(id: String, fallback: String): String =
    localizedBreathingText(id, fallback, BreathingTextKind.PATTERN)

@Composable
internal fun localizedBreathingGuide(id: String, fallback: String): String =
    localizedBreathingText(id, fallback, BreathingTextKind.GUIDE)

@Composable
internal fun localizedBreathingEffect(id: String, fallback: String): String =
    localizedBreathingText(id, fallback, BreathingTextKind.EFFECT)

@Composable
private fun localizedBreathingText(id: String, fallback: String, kind: BreathingTextKind): String {
    val resourceId = breathingTextResource(id, kind)
    return resourceId?.let { stringResource(it) } ?: fallback
}

@StringRes
private fun breathingTextResource(id: String, kind: BreathingTextKind): Int? {
    return when (id) {
        "physiological_sigh" -> when (kind) {
            BreathingTextKind.NAME -> R.string.breathing_physiological_sigh_name
            BreathingTextKind.PATTERN -> R.string.breathing_physiological_sigh_pattern
            BreathingTextKind.GUIDE -> R.string.breathing_physiological_sigh_guide
            BreathingTextKind.EFFECT -> R.string.breathing_physiological_sigh_effect
        }
        "coherent_4_6" -> when (kind) {
            BreathingTextKind.NAME -> R.string.breathing_coherent_name
            BreathingTextKind.PATTERN -> R.string.breathing_coherent_pattern
            BreathingTextKind.GUIDE -> R.string.breathing_coherent_guide
            BreathingTextKind.EFFECT -> R.string.breathing_coherent_effect
        }
        "box_4_4_4_4" -> when (kind) {
            BreathingTextKind.NAME -> R.string.breathing_box_name
            BreathingTextKind.PATTERN -> R.string.breathing_box_pattern
            BreathingTextKind.GUIDE -> R.string.breathing_box_guide
            BreathingTextKind.EFFECT -> R.string.breathing_box_effect
        }
        "relax_4_7_8" -> when (kind) {
            BreathingTextKind.NAME -> R.string.breathing_relax_name
            BreathingTextKind.PATTERN -> R.string.breathing_relax_pattern
            BreathingTextKind.GUIDE -> R.string.breathing_relax_guide
            BreathingTextKind.EFFECT -> R.string.breathing_relax_effect
        }
        "long_exhale_4_8" -> when (kind) {
            BreathingTextKind.NAME -> R.string.breathing_long_exhale_name
            BreathingTextKind.PATTERN -> R.string.breathing_long_exhale_pattern
            BreathingTextKind.GUIDE -> R.string.breathing_long_exhale_guide
            BreathingTextKind.EFFECT -> R.string.breathing_long_exhale_effect
        }
        else -> null
    }
}

private enum class BreathingTextKind { NAME, PATTERN, GUIDE, EFFECT }
