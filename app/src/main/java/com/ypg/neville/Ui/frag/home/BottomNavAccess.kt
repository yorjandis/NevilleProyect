package com.ypg.neville.ui.frag

import android.content.SharedPreferences
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.core.content.edit
import com.ypg.neville.R

enum class BottomNavAccessGroup(@param:StringRes val titleRes: Int) {
    PRODUCTIVITY(R.string.home_menu_productivity),
    LEARNING_RESOURCES(R.string.home_menu_learning_resources),
    AUTHORS(R.string.bottom_nav_authors_group)
}

enum class BottomNavAccess(
    val id: String,
    val group: BottomNavAccessGroup,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
    @param:IdRes val destinationId: Int,
    val listContent: String? = null
) {
    Lectures("conf", BottomNavAccessGroup.AUTHORS, R.string.home_nav_lectures, R.drawable.ic_conf, R.id.frag_listado, "autores/neville/conf"),
    Notes("notas", BottomNavAccessGroup.LEARNING_RESOURCES, R.string.home_nav_notes, R.drawable.ic_note, R.id.frag_notas),
    Quotes("frases", BottomNavAccessGroup.LEARNING_RESOURCES, R.string.home_nav_quotes, R.drawable.ic_item, R.id.frag_listado_frases),
    Encyclopedia("enciclopedia", BottomNavAccessGroup.LEARNING_RESOURCES, R.string.home_nav_encyclopedia, R.drawable.ic_conf, R.id.frag_listado, "enciclopedia"),
    Evidence("evidencia", BottomNavAccessGroup.LEARNING_RESOURCES, R.string.home_nav_scientific_evidence, R.drawable.ic_tips, R.id.frag_listado, "evidenciaCientifica"),
    Reflections("reflexiones", BottomNavAccessGroup.LEARNING_RESOURCES, R.string.home_nav_reflections, R.drawable.ic_show, R.id.frag_listado, "reflexiones"),
    Help("ayudas", BottomNavAccessGroup.LEARNING_RESOURCES, R.string.home_nav_help, R.drawable.ic_ayuda, R.id.frag_listado, "ayudas"),

    Diary("diario", BottomNavAccessGroup.PRODUCTIVITY, R.string.home_nav_diary, R.drawable.ic_diario_pen_book, R.id.frag_diario),
    Chat("chat", BottomNavAccessGroup.PRODUCTIVITY, R.string.nav_chat, R.drawable.ic_nav_chat, R.id.frag_ai_chat),
    Canvas("lienzo", BottomNavAccessGroup.PRODUCTIVITY, R.string.home_nav_canvas, R.drawable.ic_edit_note, R.id.frag_lienzo),
    Goals("metas", BottomNavAccessGroup.PRODUCTIVITY, R.string.home_nav_goals, R.drawable.ic_arriba, R.id.frag_metas),
    Agenda("agenda", BottomNavAccessGroup.PRODUCTIVITY, R.string.home_nav_agenda, R.drawable.ic_calendar_toggle, R.id.frag_agenda),
    Reminders("recordatorios", BottomNavAccessGroup.PRODUCTIVITY, R.string.home_nav_reminders, R.drawable.ic_calendar_toggle, R.id.frag_reminders),
    Ritual("morning_dialog", BottomNavAccessGroup.PRODUCTIVITY, R.string.home_nav_daily_ritual, R.drawable.ic_calendar_toggle, R.id.frag_morning_dialog),
    WeeklySummary("weekly_summary", BottomNavAccessGroup.PRODUCTIVITY, R.string.weekly_summary_title, R.drawable.ic_item, R.id.frag_weekly_summary),
    Anchors("anclas", BottomNavAccessGroup.PRODUCTIVITY, R.string.anchors_title, R.drawable.ic_tips, R.id.frag_emotional_anchors),
    Calm("calma", BottomNavAccessGroup.PRODUCTIVITY, R.string.calm_title, R.drawable.ic_show, R.id.frag_calm_space),
    Coherence("cardio", BottomNavAccessGroup.PRODUCTIVITY, R.string.coherence_title, R.drawable.ic_tips, R.id.frag_cardio_coherence),
    Presence("presence", BottomNavAccessGroup.PRODUCTIVITY, R.string.presence_title, R.drawable.ic_show, R.id.frag_presence),
    Transformation("transformacion", BottomNavAccessGroup.PRODUCTIVITY, R.string.transformation_short_title, R.drawable.ic_tips, R.id.frag_transformation_protocol),
    HealingCenter("centro_sanador", BottomNavAccessGroup.PRODUCTIVITY, R.string.healing_center_title, R.drawable.ic_ayuda, R.id.frag_healing_center),

    Neville("autor_neville", BottomNavAccessGroup.AUTHORS, R.string.neville_goddard, R.drawable.ic_item, R.id.frag_neville_goddard),
    Joe("autor_joe", BottomNavAccessGroup.AUTHORS, R.string.joe_dispenza, R.drawable.ic_tips, R.id.frag_joe_dispenza),
    Gregg("autor_gregg", BottomNavAccessGroup.AUTHORS, R.string.gregg_braden, R.drawable.ic_show, R.id.frag_gregg),
    Bruce("autor_bruce", BottomNavAccessGroup.AUTHORS, R.string.bruce_lipton, R.drawable.ic_ayuda, R.id.frag_bruce_lipton);

    companion object {
        val defaults = listOf(Lectures, Notes, Diary, Chat)

        fun fromId(id: String?): BottomNavAccess? = entries.firstOrNull { it.id == id }
    }
}

object BottomNavPreferences {
    private const val KEY_PREFIX = "main_bottom_nav_slot_"
    const val SLOT_COUNT = 4

    fun load(prefs: SharedPreferences): List<BottomNavAccess> {
        val selected = mutableListOf<BottomNavAccess>()
        repeat(SLOT_COUNT) { index ->
            val saved = BottomNavAccess.fromId(prefs.getString("$KEY_PREFIX$index", null))
            val preferred = saved ?: BottomNavAccess.defaults[index]
            val access = preferred.takeUnless { it in selected }
                ?: BottomNavAccess.defaults.firstOrNull { it !in selected }
                ?: BottomNavAccess.entries.first { it !in selected }
            selected += access
        }
        return selected
    }

    fun save(prefs: SharedPreferences, accesses: List<BottomNavAccess>) {
        require(accesses.size == SLOT_COUNT)
        require(accesses.distinct().size == SLOT_COUNT)
        prefs.edit {
            accesses.forEachIndexed { index, access ->
                putString("$KEY_PREFIX$index", access.id)
            }
        }
    }
}
