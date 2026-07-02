package com.ypg.neville.ui.frag

import androidx.navigation.NavArgument
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphNavigator
import androidx.navigation.NavType
import androidx.navigation.fragment.FragmentNavigator
import com.ypg.neville.feature.agenda.ui.FragAgenda
import com.ypg.neville.R
import com.ypg.neville.feature.cardiocoherence.ui.FragCardioCoherence
import com.ypg.neville.feature.calmspace.ui.FragCalmBackgroundsManager
import com.ypg.neville.feature.calmspace.ui.FragCalmMusicManager
import com.ypg.neville.feature.calmspace.ui.FragCalmPhraseManager
import com.ypg.neville.feature.calmspace.ui.FragCalmSpace
import com.ypg.neville.feature.emotionalanchors.ui.FragEmotionalAnchorRun
import com.ypg.neville.feature.emotionalanchors.ui.FragEmotionalAnchors
import com.ypg.neville.feature.emotionalanchors.ui.FragEmotionalAnchorsList
import com.ypg.neville.feature.morningdialog.ui.FragMorningDialog
import com.ypg.neville.feature.presence.ui.FragPresence
import com.ypg.neville.feature.voice.ui.FragVoiceRecordings
import com.ypg.neville.feature.weeklysummary.ui.FragWeeklySummary

fun buildNevilleNavGraph(navController: NavController, startDestination: Int): NavGraph {
    val provider = navController.navigatorProvider
    val graph = NavGraph(NavGraphNavigator(provider))
    val fragmentNavigator = provider.getNavigator(FragmentNavigator::class.java)

    fun addFragmentDestination(
        id: Int,
        className: String,
        args: List<Pair<String, NavArgument>> = emptyList()
    ) {
        val destination = fragmentNavigator.createDestination().apply {
            this.id = id
            setClassName(className)
            args.forEach { (name, arg) -> addArgument(name, arg) }
        }
        graph.addDestination(destination)
    }

    addFragmentDestination(R.id.frag_listado, frag_listado::class.java.name)
    addFragmentDestination(R.id.frag_home, FragHome::class.java.name)
    addFragmentDestination(R.id.frag_content_webview, FragContentWebView::class.java.name)
    addFragmentDestination(R.id.frag_author_photo_gallery, FragAuthorPhotoGallery::class.java.name)
    addFragmentDestination(R.id.frag_gregg, FragGregg::class.java.name)
    addFragmentDestination(R.id.frag_neville_goddard, FragNevilleGoddard::class.java.name)
    addFragmentDestination(R.id.frag_joe_dispenza, FragJoeDispenza::class.java.name)
    addFragmentDestination(R.id.frag_bruce_lipton, FragBruceLipton::class.java.name)
    addFragmentDestination(R.id.frag_list_info, FragListInfo::class.java.name)
    addFragmentDestination(R.id.frag_listado_frases, FragListadoFrases::class.java.name)
    addFragmentDestination(R.id.fragSetting, frag_Setting::class.java.name)
    addFragmentDestination(R.id.frag_notas, FragNotas::class.java.name)
    addFragmentDestination(R.id.frag_diario, FragDiario::class.java.name)
    addFragmentDestination(R.id.frag_lienzo, FragLienzo::class.java.name)
    addFragmentDestination(R.id.frag_metas, FragMetas::class.java.name)
    addFragmentDestination(R.id.frag_reminders, FragReminders::class.java.name)
    addFragmentDestination(R.id.frag_agenda, FragAgenda::class.java.name)
    addFragmentDestination(R.id.frag_morning_dialog, FragMorningDialog::class.java.name)
    addFragmentDestination(R.id.frag_voice_recordings, FragVoiceRecordings::class.java.name)
    addFragmentDestination(R.id.frag_emotional_anchors, FragEmotionalAnchorsList::class.java.name)
    addFragmentDestination(
        R.id.frag_emotional_anchor_create,
        FragEmotionalAnchors::class.java.name,
        args = listOf(
            FragEmotionalAnchors.ARG_EDIT_ANCHOR_ID to
                NavArgument.Builder()
                    .setType(NavType.LongType)
                    .setDefaultValue(-1L)
                    .build()
        )
    )
    addFragmentDestination(
        R.id.frag_emotional_anchor_run,
        FragEmotionalAnchorRun::class.java.name,
        args = listOf(
            FragEmotionalAnchorRun.ARG_ANCHOR_ID to
                NavArgument.Builder()
                    .setType(NavType.LongType)
                    .setDefaultValue(-1L)
                    .build()
        )
    )
    addFragmentDestination(R.id.frag_import_shared_text, FragImportSharedText::class.java.name)
    addFragmentDestination(R.id.frag_weekly_summary, FragWeeklySummary::class.java.name)
    addFragmentDestination(R.id.frag_calm_space, FragCalmSpace::class.java.name)
    addFragmentDestination(R.id.frag_calm_phrase_manager, FragCalmPhraseManager::class.java.name)
    addFragmentDestination(R.id.frag_calm_backgrounds_manager, FragCalmBackgroundsManager::class.java.name)
    addFragmentDestination(R.id.frag_calm_music_manager, FragCalmMusicManager::class.java.name)
    addFragmentDestination(R.id.frag_cardio_coherence, FragCardioCoherence::class.java.name)
    addFragmentDestination(R.id.frag_presence, FragPresence::class.java.name)

    graph.setStartDestination(startDestination)
    return graph
}
