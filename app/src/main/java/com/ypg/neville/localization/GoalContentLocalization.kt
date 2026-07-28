package com.ypg.neville.localization

import android.content.Context
import com.ypg.neville.model.metas.HabitPreset
import com.ypg.neville.model.metas.ProgramaPreestablecido
import com.ypg.neville.model.metas.UnitInfo
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Applies the translated editorial fields for goal presets while retaining all
 * planning metadata from the canonical Spanish assets.
 */
object GoalContentLocalization {
    private const val ROOT = "localized_content/goals"
    private val habitCatalogs = ConcurrentHashMap<String, JSONObject>()
    private val programCatalogs = ConcurrentHashMap<String, JSONObject>()

    fun localizedHabit(
        context: Context,
        spanishTitle: String,
        fallback: HabitPreset
    ): HabitPreset {
        val languageFolder = languageFolder(context) ?: return fallback
        val habitId = spanishTitle.replace(Regex("\\s+"), "")
        val translation = habitCatalog(context, languageFolder).optJSONObject(habitId)
            ?: return fallback

        return fallback.copy(
            title = translation.optString("title").takeIf(String::isNotBlank) ?: fallback.title,
            description = translation.optString("description")
                .takeIf(String::isNotBlank) ?: fallback.description,
            customUnitLabel = if (translation.has("customUnitLabel")) {
                translation.optString("customUnitLabel")
            } else {
                fallback.customUnitLabel
            }
        )
    }

    fun localizedProgram(
        context: Context,
        fileBaseName: String,
        fallback: ProgramaPreestablecido
    ): ProgramaPreestablecido {
        val languageFolder = languageFolder(context) ?: return fallback
        val translation = programCatalog(context, languageFolder).optJSONObject(fileBaseName)
            ?: return fallback
        val translatedUnits = translation.optJSONArray("unidadesinfo") ?: return fallback
        if (translatedUnits.length() != fallback.unidadesinfo.size) return fallback

        val units = (0 until translatedUnits.length()).map { index ->
            val item = translatedUnits.optJSONObject(index)
            val original = fallback.unidadesinfo[index]
            UnitInfo(
                name = item?.optString("name")?.takeIf(String::isNotBlank) ?: original.name,
                info = item?.optString("info")?.takeIf(String::isNotBlank) ?: original.info
            )
        }

        return fallback.copy(
            title = translation.optString("title").takeIf(String::isNotBlank) ?: fallback.title,
            detalles = translation.optString("detalles")
                .takeIf(String::isNotBlank) ?: fallback.detalles,
            description = translation.optString("description")
                .takeIf(String::isNotBlank) ?: fallback.description,
            unidadesinfo = units,
            customUnitLabel = if (translation.has("customUnitLabel")) {
                translation.optString("customUnitLabel")
            } else {
                fallback.customUnitLabel
            }
        )
    }

    private fun habitCatalog(context: Context, languageFolder: String): JSONObject =
        habitCatalogs.getOrPut(languageFolder) {
            readCatalog(context, languageFolder, "HealthyHabits.json")
        }

    private fun programCatalog(context: Context, languageFolder: String): JSONObject =
        programCatalogs.getOrPut(languageFolder) {
            readCatalog(context, languageFolder, "GoalPrograms.json")
        }

    private fun readCatalog(context: Context, languageFolder: String, fileName: String): JSONObject =
        runCatching {
            val raw = context.assets.open("$ROOT/$languageFolder/$fileName")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            JSONObject(raw)
        }.getOrDefault(JSONObject())

    private fun languageFolder(context: Context): String? =
        when (context.resources.configuration.locales[0].language) {
            "en" -> "en"
            "zh" -> "zh-Hans"
            else -> null
        }
}
