package com.ypg.neville.feature.premiumpreview

import androidx.annotation.StringRes
import com.ypg.neville.R

/**
 * Catálogo Android de las presentaciones premium que también existen en iOS.
 *
 * Los nombres de captura no incluyen extensión. El cargador acepta PNG, WebP,
 * JPG y JPEG dentro de la carpeta [assetDirectory], respetando este orden.
 */
enum class PremiumFeatureId(
    val wireName: String,
    val assetDirectory: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    @param:StringRes val practicalValueRes: Int,
    val screenshotNames: List<String>
) {
    AGENDA(
        "agenda",
        "agenda",
        R.string.premium_preview_agenda_title,
        R.string.premium_preview_agenda_description,
        R.string.premium_preview_agenda_value,
        listOf("Agenda_1", "Agenda_2", "Agenda_3", "Agenda_4", "Agenda_5")
    ),
    HEALING_CENTER(
        "healing_center",
        "centro_sanador",
        R.string.premium_preview_healing_center_title,
        R.string.premium_preview_healing_center_description,
        R.string.premium_preview_healing_center_value,
        listOf("CSanador_1", "CSanador_2", "CSanador_3", "CSanador_4", "CSanador_5", "CSanador_6")
    ),
    CARDIO_COHERENCE(
        "cardio_coherence",
        "coherencia_cardio_cerebral",
        R.string.premium_preview_cardio_coherence_title,
        R.string.premium_preview_cardio_coherence_description,
        R.string.premium_preview_cardio_coherence_value,
        listOf("CC_1", "CC_2", "CC_3", "CC_4")
    ),
    CALM_SPACE(
        "calm_space",
        "espacio_calma",
        R.string.premium_preview_calm_space_title,
        R.string.premium_preview_calm_space_description,
        R.string.premium_preview_calm_space_value,
        listOf("EspacioC_1", "EspacioC_2", "EspacioC_3", "EspacioC_4", "EspacioC_5", "EspacioC_6")
    ),
    INTEGRATED_AI(
        "integrated_ai",
        "inteligencia_artificial",
        R.string.premium_preview_ai_title,
        R.string.premium_preview_ai_description,
        R.string.premium_preview_ai_value,
        listOf("IA_1", "IA_2", "IA_3", "IA_4")
    ),
    CREATIVE_CANVAS(
        "creative_canvas",
        "lienzo_creativo",
        R.string.premium_preview_creative_canvas_title,
        R.string.premium_preview_creative_canvas_description,
        R.string.premium_preview_creative_canvas_value,
        listOf("Lienzo_1", "Lienzo_2", "Lienzo_3", "Lienzo_4")
    ),
    GOALS(
        "goals",
        "metas",
        R.string.premium_preview_goals_title,
        R.string.premium_preview_goals_description,
        R.string.premium_preview_goals_value,
        listOf("Metas_1", "Metas_2", "Metas_3", "Metas_4", "Metas_5", "Metas_6", "Metas_7", "Metas_8", "Metas_9")
    ),
    PROTECTED_NOTES(
        "protected_notes",
        "notas_protegidas",
        R.string.premium_preview_protected_notes_title,
        R.string.premium_preview_protected_notes_description,
        R.string.premium_preview_protected_notes_value,
        emptyList()
    ),
    CONSCIOUS_PRESENCE(
        "conscious_presence",
        "presencia_consciente",
        R.string.premium_preview_presence_title,
        R.string.premium_preview_presence_description,
        R.string.premium_preview_presence_value,
        listOf("Presencia_1", "Presencia_2", "Presencia_3")
    ),
    TRANSFORMATION_PROTOCOL(
        "transformation_protocol",
        "protocolo_transformacion",
        R.string.premium_preview_transformation_title,
        R.string.premium_preview_transformation_description,
        R.string.premium_preview_transformation_value,
        listOf("ProtocoloTrans_1", "ProtocoloTrans_2", "ProtocoloTrans_3")
    ),
    SMART_REMINDERS(
        "smart_reminders",
        "recordatorios",
        R.string.premium_preview_reminders_title,
        R.string.premium_preview_reminders_description,
        R.string.premium_preview_reminders_value,
        listOf("Recordatorios_1", "Recordatorios_2", "Recordatorios_3")
    ),
    WEEKLY_REVIEW(
        "weekly_review",
        "revision_semanal",
        R.string.premium_preview_weekly_review_title,
        R.string.premium_preview_weekly_review_description,
        R.string.premium_preview_weekly_review_value,
        listOf("ResumenSemanal_1", "ResumenSemanal_2")
    ),
    CONSCIOUS_DAILY_CYCLE(
        "conscious_daily_cycle",
        "ciclo_consciente_diario",
        R.string.premium_preview_daily_cycle_title,
        R.string.premium_preview_daily_cycle_description,
        R.string.premium_preview_daily_cycle_value,
        listOf("Ritual_premium_1", "Ritual_premium_2", "Ritual_premium_3", "Ritual_premium_4", "Ritual_premium_5")
    ),
    SMART_COPIED_TEXT(
        "smart_copied_text",
        "texto_copiado",
        R.string.premium_preview_copied_text_title,
        R.string.premium_preview_copied_text_description,
        R.string.premium_preview_copied_text_value,
        emptyList()
    ),
    EXTENDED_CONTENT(
        "extended_content",
        "contenido_extendido",
        R.string.premium_preview_extended_content_title,
        R.string.premium_preview_extended_content_description,
        R.string.premium_preview_extended_content_value,
        emptyList()
    );

    val assetPath: String
        get() = "$ASSET_ROOT/$assetDirectory"

    companion object {
        const val ASSET_ROOT = "premium_previews"

        fun fromWireName(value: String?): PremiumFeatureId? =
            entries.firstOrNull { it.wireName == value }

        fun forDestination(destinationId: Int): PremiumFeatureId? = when (destinationId) {
            R.id.frag_agenda -> AGENDA
            R.id.frag_healing_center -> HEALING_CENTER
            R.id.frag_cardio_coherence -> CARDIO_COHERENCE
            R.id.frag_calm_space -> CALM_SPACE
            R.id.frag_ai_chat, R.id.frag_ai_text_tool -> INTEGRATED_AI
            R.id.frag_lienzo -> CREATIVE_CANVAS
            R.id.frag_metas -> GOALS
            R.id.frag_presence -> CONSCIOUS_PRESENCE
            R.id.frag_transformation_protocol -> TRANSFORMATION_PROTOCOL
            R.id.frag_reminders -> SMART_REMINDERS
            R.id.frag_weekly_summary -> WEEKLY_REVIEW
            R.id.frag_morning_dialog, R.id.frag_my_day -> CONSCIOUS_DAILY_CYCLE
            else -> null
        }
    }
}
