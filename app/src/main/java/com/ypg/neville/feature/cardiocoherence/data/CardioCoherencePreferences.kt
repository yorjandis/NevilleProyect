package com.ypg.neville.feature.cardiocoherence.data

import android.content.Context
import androidx.core.content.edit
import com.ypg.neville.model.preferences.DbPreferences

object CardioCoherencePreferences {
    const val MAX_SESSION_PHRASE_LENGTH = 90

    val phaseTitles = listOf(
        "Regulación",
        "Conexión corazón",
        "Emoción elevada",
        "Integración"
    )

    val defaultSessionPhrases = listOf(
        "Suelto el esfuerzo y permito que mi cuerpo se calme.",
        "Cada respiración me devuelve a un estado de equilibrio.",
        "Llevo mi atención al espacio sereno de mi corazón.",
        "Mi corazón y mi mente comienzan a respirar juntos.",
        "Elijo sentir gratitud, apertura y confianza.",
        "Dejo que esta emoción elevada impregne todo mi ser.",
        "Esta coherencia se integra suavemente dentro de mí.",
        "Permanezco presente y llevo este estado conmigo."
    )

    fun loadSessionPhrases(context: Context): List<String> {
        val preferences = DbPreferences.default(context.applicationContext)
        return defaultSessionPhrases.indices.map { index ->
            preferences.getString(sessionPhraseKey(index), null)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: defaultSessionPhrases[index]
        }
    }

    fun saveSessionPhrases(context: Context, phrases: List<String>) {
        DbPreferences.default(context.applicationContext).edit {
            defaultSessionPhrases.indices.forEach { index ->
                val value = phrases.getOrNull(index)
                    ?.trim()
                    ?.take(MAX_SESSION_PHRASE_LENGTH)
                    ?.takeIf { it.isNotEmpty() }
                    ?: defaultSessionPhrases[index]
                putString(sessionPhraseKey(index), value)
            }
        }
    }

    fun resetSessionPhrases(context: Context) {
        DbPreferences.default(context.applicationContext).edit {
            defaultSessionPhrases.indices.forEach { index ->
                remove(sessionPhraseKey(index))
            }
        }
    }

    private fun sessionPhraseKey(index: Int): String = "coherencia_session_phrase_$index"
}
