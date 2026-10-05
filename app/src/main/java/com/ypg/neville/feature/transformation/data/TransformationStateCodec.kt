package com.ypg.neville.feature.transformation.data

import com.ypg.neville.feature.transformation.domain.TransformationConfiguration
import com.ypg.neville.feature.transformation.domain.TransformationDayEntry
import com.ypg.neville.feature.transformation.domain.TransformationJournal
import com.ypg.neville.feature.transformation.domain.TransformationParaEvent
import com.ypg.neville.feature.transformation.domain.TransformationScore
import com.ypg.neville.feature.transformation.domain.TransformationState
import org.json.JSONArray
import org.json.JSONObject

internal object TransformationStateCodec {
    fun encode(state: TransformationState): String = JSONObject().apply {
        put("schemaVersion", state.schemaVersion)
        put("configuration", state.configuration?.toJson() ?: JSONObject.NULL)
        put("entries", JSONArray().apply { state.entries.forEach { put(it.toJson()) } })
        put("paraEvents", JSONArray().apply { state.paraEvents.forEach { put(it.toJson()) } })
        put("completedAtMillis", state.completedAtMillis ?: JSONObject.NULL)
    }.toString()

    fun decode(value: String): TransformationState {
        val json = JSONObject(value)
        return TransformationState(
            schemaVersion = json.optInt("schemaVersion", 1),
            configuration = json.optJSONObject("configuration")?.toConfiguration(),
            entries = json.optJSONArray("entries").objects().map { it.toEntry() },
            paraEvents = json.optJSONArray("paraEvents").objects().map { it.toParaEvent() },
            completedAtMillis = json.optNullableLong("completedAtMillis")
        )
    }

    private fun TransformationConfiguration.toJson() = JSONObject().apply {
        put("patternName", patternName); put("trigger", trigger); put("automaticThought", automaticThought)
        put("emotion", emotion); put("oldBehavior", oldBehavior); put("consequence", consequence)
        put("alternativeBehavior", alternativeBehavior); put("toleratedEmotion", toleratedEmotion)
        put("identity", identity); put("startedAtMillis", startedAtMillis)
        put("remindersEnabled", remindersEnabled); put("morningMinuteOfDay", morningMinuteOfDay)
        put("pauseMinuteOfDay", pauseMinuteOfDay); put("eveningMinuteOfDay", eveningMinuteOfDay)
    }

    private fun JSONObject.toConfiguration() = TransformationConfiguration(
        patternName = optString("patternName"), trigger = optString("trigger"),
        automaticThought = optString("automaticThought"), emotion = optString("emotion"),
        oldBehavior = optString("oldBehavior"), consequence = optString("consequence"),
        alternativeBehavior = optString("alternativeBehavior"), toleratedEmotion = optString("toleratedEmotion"),
        identity = optString("identity"), startedAtMillis = optLong("startedAtMillis"),
        remindersEnabled = optBoolean("remindersEnabled"), morningMinuteOfDay = optInt("morningMinuteOfDay", 480),
        pauseMinuteOfDay = optInt("pauseMinuteOfDay", 840), eveningMinuteOfDay = optInt("eveningMinuteOfDay", 1260)
    )

    private fun TransformationDayEntry.toJson() = JSONObject().apply {
        put("day", day); put("updatedAtMillis", updatedAtMillis); put("commitment", commitment)
        put("exerciseNotes", exerciseNotes); put("morningCompleted", morningCompleted)
        put("actionCompleted", actionCompleted); put("eveningCompleted", eveningCompleted)
        put("journal", journal.toJson()); put("score", score.toJson())
        put("evidences", JSONArray(evidences))
    }

    private fun JSONObject.toEntry() = TransformationDayEntry(
        day = optInt("day"), updatedAtMillis = optLong("updatedAtMillis"), commitment = optString("commitment"),
        exerciseNotes = optString("exerciseNotes"), morningCompleted = optBoolean("morningCompleted"),
        actionCompleted = optBoolean("actionCompleted"), eveningCompleted = optBoolean("eveningCompleted"),
        journal = optJSONObject("journal")?.toJournal() ?: TransformationJournal(),
        score = optJSONObject("score")?.toScore() ?: TransformationScore(),
        evidences = optJSONArray("evidences").strings()
    )

    private fun TransformationJournal.toJson() = JSONObject().apply {
        put("situation", situation); put("thought", thought); put("emotionAndBody", emotionAndBody)
        put("impulse", impulse); put("response", response); put("learning", learning)
        put("maximumIntensity", maximumIntensity); put("recoveryMinutes", recoveryMinutes)
    }

    private fun JSONObject.toJournal() = TransformationJournal(
        situation = optString("situation"), thought = optString("thought"),
        emotionAndBody = optString("emotionAndBody"), impulse = optString("impulse"),
        response = optString("response"), learning = optString("learning"),
        maximumIntensity = optInt("maximumIntensity"), recoveryMinutes = optInt("recoveryMinutes")
    )

    private fun TransformationScore.toJson() = JSONObject().apply {
        put("awareness", awareness); put("pause", pause); put("regulation", regulation)
        put("alternativeBehavior", alternativeBehavior); put("recovery", recovery)
    }

    private fun JSONObject.toScore() = TransformationScore(
        awareness = optInt("awareness"), pause = optInt("pause"), regulation = optInt("regulation"),
        alternativeBehavior = optInt("alternativeBehavior"), recovery = optInt("recovery")
    )

    private fun TransformationParaEvent.toJson() = JSONObject().apply {
        put("id", id); put("createdAtMillis", createdAtMillis); put("perceivedSignal", perceivedSignal)
        put("emotion", emotion); put("alternativeAction", alternativeAction); put("pauseSeconds", pauseSeconds)
    }

    private fun JSONObject.toParaEvent() = TransformationParaEvent(
        id = optString("id"), createdAtMillis = optLong("createdAtMillis"),
        perceivedSignal = optString("perceivedSignal"), emotion = optString("emotion"),
        alternativeAction = optString("alternativeAction"), pauseSeconds = optInt("pauseSeconds")
    )

    private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else buildList {
        for (index in 0 until length()) optJSONObject(index)?.let(::add)
    }

    private fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else buildList {
        for (index in 0 until length()) optString(index).takeIf { it.isNotBlank() }?.let(::add)
    }

    private fun JSONObject.optNullableLong(key: String): Long? =
        if (isNull(key) || !has(key)) null else optLong(key)
}
