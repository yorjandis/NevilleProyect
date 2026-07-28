package com.ypg.neville.feature.healingcenter.domain

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer

data class HealingCatalog(
    val schemaVersion: Int,
    val contentVersion: String,
    val reviewedAt: String,
    val disclaimer: String,
    val situations: List<HealingSituation>
)

data class HealingSituation(
    val id: String,
    val title: String,
    val subtitle: String,
    val symbol: String,
    val palette: HealingPalette,
    val immediateExplanation: String,
    val biologicalExplanation: String,
    val reassurance: String,
    val searchTerms: List<String>,
    val redFlags: List<String>,
    val whenToSeekHelp: List<String>,
    val protocols: List<HealingProtocol>,
    val practicalTips: List<HealingPracticalTip>,
    val sources: List<HealingEvidenceSource>
) {
    fun matches(query: String): Boolean {
        val normalizedQuery = query.normalizedForSearch()
        if (normalizedQuery.isEmpty()) return true
        return (listOf(title, subtitle) + searchTerms)
            .joinToString(" ")
            .normalizedForSearch()
            .contains(normalizedQuery)
    }
}

data class HealingPracticalTip(
    val id: String,
    val title: String,
    val detail: String
)

enum class HealingPalette { OCEAN, AMBER, VIOLET, FOREST, ROSE, SLATE }

data class HealingProtocol(
    val id: String,
    val title: String,
    val summary: String,
    val symbol: String,
    val kind: HealingInterventionKind,
    val evidence: HealingEvidenceLevel,
    val caution: String?,
    val steps: List<HealingProtocolStep>
) {
    val durationSeconds: Int get() = steps.sumOf { it.durationSeconds }
}

enum class HealingInterventionKind {
    BREATHING,
    GROUNDING,
    MUSCLE_RELEASE,
    MOVEMENT,
    REFLECTION,
    ACUPRESSURE,
    VOCALIZATION,
    CONFLICT_PAUSE
}

enum class HealingEvidenceLevel { SUPPORTED, PROMISING, COMPLEMENTARY, EXPERIMENTAL }

data class HealingProtocolStep(
    val id: String,
    val title: String,
    val instruction: String,
    val durationSeconds: Int,
    val phase: HealingStepPhase,
    val accessibilityCue: String?
)

enum class HealingStepPhase {
    PREPARE,
    INHALE,
    EXHALE,
    OBSERVE,
    ORIENT,
    MOVE,
    PRESS,
    SOUND,
    REFLECT,
    FINISH
}

data class HealingEvidenceSource(
    val id: String,
    val title: String,
    val organization: String,
    val url: String,
    val note: String
)

class BundledHealingCatalogRepository(
    private val context: Context,
    private val assetName: String? = null
) {
    fun load(): HealingCatalog {
        val json = context.assets.open(assetName ?: localizedAssetName()).bufferedReader().use { it.readText() }
        return parse(JSONObject(json)).also(::validate)
    }

    private fun localizedAssetName(): String {
        val language = context.resources.configuration.locales[0].language
        val fileName = when (language) {
            "en" -> "healing_center_en.json"
            "zh" -> "healing_center_zh-Hans.json"
            else -> "healing_center_es.json"
        }
        return "healing_center/$fileName"
    }

    private fun parse(root: JSONObject): HealingCatalog = HealingCatalog(
        schemaVersion = root.getInt("schemaVersion"),
        contentVersion = root.getString("contentVersion"),
        reviewedAt = root.getString("reviewedAt"),
        disclaimer = root.getString("disclaimer"),
        situations = root.getJSONArray("situations").mapObjects(::parseSituation)
    )

    private fun parseSituation(json: JSONObject): HealingSituation = HealingSituation(
        id = json.getString("id"),
        title = json.getString("title"),
        subtitle = json.getString("subtitle"),
        symbol = json.getString("symbol"),
        palette = json.getString("palette").toHealingPalette(),
        immediateExplanation = json.getString("immediateExplanation"),
        biologicalExplanation = json.getString("biologicalExplanation"),
        reassurance = json.getString("reassurance"),
        searchTerms = json.getJSONArray("searchTerms").mapStrings(),
        redFlags = json.getJSONArray("redFlags").mapStrings(),
        whenToSeekHelp = json.getJSONArray("whenToSeekHelp").mapStrings(),
        protocols = json.getJSONArray("protocols").mapObjects(::parseProtocol),
        practicalTips = json.getJSONArray("practicalTips").mapObjects {
            HealingPracticalTip(
                id = it.getString("id"),
                title = it.getString("title"),
                detail = it.getString("detail")
            )
        },
        sources = json.getJSONArray("sources").mapObjects {
            HealingEvidenceSource(
                id = it.getString("id"),
                title = it.getString("title"),
                organization = it.getString("organization"),
                url = it.getString("url"),
                note = it.getString("note")
            )
        }
    )

    private fun parseProtocol(json: JSONObject): HealingProtocol = HealingProtocol(
        id = json.getString("id"),
        title = json.getString("title"),
        summary = json.getString("summary"),
        symbol = json.getString("symbol"),
        kind = json.getString("kind").toInterventionKind(),
        evidence = json.getString("evidence").toEvidenceLevel(),
        caution = json.optString("caution").takeUnless { json.isNull("caution") || it.isBlank() },
        steps = json.getJSONArray("steps").mapObjects {
            HealingProtocolStep(
                id = it.getString("id"),
                title = it.getString("title"),
                instruction = it.getString("instruction"),
                durationSeconds = it.getInt("durationSeconds"),
                phase = it.getString("phase").toStepPhase(),
                accessibilityCue = it.optString("accessibilityCue")
                    .takeUnless { _ -> it.isNull("accessibilityCue") }
                    ?.takeIf(String::isNotBlank)
            )
        }
    )

    private fun validate(catalog: HealingCatalog) {
        require(catalog.schemaVersion == 2) { "Versión de catálogo no compatible" }
        require(catalog.situations.isNotEmpty()) { "El catálogo no contiene situaciones" }
        require(catalog.situations.map { it.id }.isUnique()) { "Hay situaciones duplicadas" }
        catalog.situations.forEach { situation ->
            require(situation.title.isNotBlank()) { "Hay una situación sin título" }
            require(situation.protocols.size == 3) { "${situation.id} debe contener tres técnicas" }
            require(situation.practicalTips.isNotEmpty()) { "${situation.id} no contiene consejos" }
            require(situation.sources.isNotEmpty()) { "${situation.id} no contiene fuentes" }
            require(situation.protocols.map { it.id }.isUnique()) { "Hay técnicas duplicadas en ${situation.id}" }
            require(situation.practicalTips.map { it.id }.isUnique()) { "Hay consejos duplicados en ${situation.id}" }
            situation.protocols.forEach { protocol ->
                require(protocol.steps.isNotEmpty()) { "${protocol.id} no contiene pasos" }
                require(protocol.steps.map { it.id }.isUnique()) { "Hay pasos duplicados en ${protocol.id}" }
                require(protocol.steps.all { it.durationSeconds in 3..120 }) {
                    "${protocol.id} contiene una duración fuera del rango seguro"
                }
            }
        }
    }
}

private fun String.normalizedForSearch(): String = Normalizer
    .normalize(lowercase(), Normalizer.Form.NFD)
    .replace("\\p{Mn}+".toRegex(), "")

private fun JSONArray.mapStrings(): List<String> = List(length()) { index -> getString(index) }

private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    List(length()) { index -> transform(getJSONObject(index)) }

private fun <T> List<T>.isUnique(): Boolean = size == toSet().size

private fun String.toHealingPalette(): HealingPalette = when (this) {
    "ocean" -> HealingPalette.OCEAN
    "amber" -> HealingPalette.AMBER
    "violet" -> HealingPalette.VIOLET
    "forest" -> HealingPalette.FOREST
    "rose" -> HealingPalette.ROSE
    "slate" -> HealingPalette.SLATE
    else -> error("Paleta desconocida: $this")
}

private fun String.toInterventionKind(): HealingInterventionKind = when (this) {
    "breathing" -> HealingInterventionKind.BREATHING
    "grounding" -> HealingInterventionKind.GROUNDING
    "muscleRelease" -> HealingInterventionKind.MUSCLE_RELEASE
    "movement" -> HealingInterventionKind.MOVEMENT
    "reflection" -> HealingInterventionKind.REFLECTION
    "acupressure" -> HealingInterventionKind.ACUPRESSURE
    "vocalization" -> HealingInterventionKind.VOCALIZATION
    "conflictPause" -> HealingInterventionKind.CONFLICT_PAUSE
    else -> error("Tipo de intervención desconocido: $this")
}

private fun String.toEvidenceLevel(): HealingEvidenceLevel = when (this) {
    "supported" -> HealingEvidenceLevel.SUPPORTED
    "promising" -> HealingEvidenceLevel.PROMISING
    "complementary" -> HealingEvidenceLevel.COMPLEMENTARY
    "experimental" -> HealingEvidenceLevel.EXPERIMENTAL
    else -> error("Nivel de evidencia desconocido: $this")
}

private fun String.toStepPhase(): HealingStepPhase = when (this) {
    "prepare" -> HealingStepPhase.PREPARE
    "inhale" -> HealingStepPhase.INHALE
    "exhale" -> HealingStepPhase.EXHALE
    "observe" -> HealingStepPhase.OBSERVE
    "orient" -> HealingStepPhase.ORIENT
    "move" -> HealingStepPhase.MOVE
    "press" -> HealingStepPhase.PRESS
    "sound" -> HealingStepPhase.SOUND
    "reflect" -> HealingStepPhase.REFLECT
    "finish" -> HealingStepPhase.FINISH
    else -> error("Fase desconocida: $this")
}
