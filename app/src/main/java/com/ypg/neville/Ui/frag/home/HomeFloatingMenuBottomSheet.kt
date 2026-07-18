package com.ypg.neville.ui.frag

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.subscription.SubscriptionManager
import com.ypg.neville.ui.theme.ContextMenuShape

class HomeFloatingMenuBottomSheet : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), R.style.Theme_NevilleProyect_CenterDialog)
        val content = ComposeView(requireContext()).apply {
            setContent {
                com.ypg.neville.ui.theme.NevilleTheme {
                    var showSheetContent by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { showSheetContent = true }

                    AnimatedVisibility(
                        visible = showSheetContent,
                        enter = scaleIn(
                            initialScale = 0.90f,
                            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(durationMillis = 220))
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(20.dp),
                            tonalElevation = 6.dp
                        ) {
                            FloatingMenuContent(onNavigate = { destination ->
                                val host = requireActivity() as? MainActivity
                                when (destination) {
                                    "neville" -> host?.openDestinationAsSheet(R.id.frag_neville_goddard)
                                    "joe" -> host?.openDestinationAsSheet(R.id.frag_joe_dispenza)
                                    "gregg" -> host?.openDestinationAsSheet(R.id.frag_gregg)
                                    "bruce" -> host?.openDestinationAsSheet(R.id.frag_bruce_lipton)
                                    "frases" -> host?.openDestinationAsSheet(R.id.frag_listado_frases)
                                    "notas" -> host?.openDestinationAsSheet(R.id.frag_notas)
                                    "enciclopedia" -> {
                                        frag_listado.elementLoaded = "enciclopedia"
                                        host?.openDestinationAsSheet(R.id.frag_listado)
                                    }
                                    "evidencia" -> {
                                        frag_listado.elementLoaded = "evidenciaCientifica"
                                        host?.openDestinationAsSheet(R.id.frag_listado)
                                    }
                                    "reflexiones" -> {
                                        frag_listado.elementLoaded = "reflexiones"
                                        host?.openDestinationAsSheet(R.id.frag_listado)
                                    }
                                    "ayudas" -> {
                                        frag_listado.elementLoaded = "ayudas"
                                        host?.openDestinationAsSheet(R.id.frag_listado)
                                    }
                                    "lienzo" -> host?.openDestinationAsSheet(R.id.frag_lienzo)
                                    "metas" -> host?.openDestinationAsSheet(R.id.frag_metas)
                                    "diario" -> host?.openDestinationAsSheet(R.id.frag_diario)
                                    "recordatorios" -> host?.openDestinationAsSheet(R.id.frag_reminders)
                                    "agenda" -> host?.openDestinationAsSheet(R.id.frag_agenda)
                                    "dialogo_matutino" -> host?.openDestinationAsSheet(R.id.frag_morning_dialog)
                                    "resumen_semanal" -> host?.openDestinationAsSheet(R.id.frag_weekly_summary)
                                    "voces" -> host?.openDestinationAsSheet(R.id.frag_voice_recordings)
                                    "anclas" -> host?.openDestinationAsSheet(R.id.frag_emotional_anchors)
                                    "calma" -> host?.openDestinationAsSheet(R.id.frag_calm_space)
                                    "cardio" -> host?.openDestinationAsSheet(R.id.frag_cardio_coherence)
                                    "presencia" -> host?.openDestinationAsSheet(R.id.frag_presence)
                                    "centro_sanador" -> host?.openDestinationAsSheet(R.id.frag_healing_center)
                                    "ajustes" -> host?.openDestinationAsSheet(R.id.fragSetting)
                                    "premium" -> host?.showSubscriptionPaywall()
                                }
                                dismiss()
                            })
                        }
                    }
                }
            }
        }

        dialog.setContentView(content)
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.apply {
            setGravity(Gravity.CENTER)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setWindowAnimations(R.style.NevilleBottomDialogAnimation)
            val verticalOffsetPx = (MENU_CENTER_OFFSET_DP * resources.displayMetrics.density).toInt()
            attributes = attributes.apply { y = verticalOffsetPx }
        }
        return dialog
    }

    @Composable
    private fun FloatingMenuContent(onNavigate: (String) -> Unit) {
        var showRecursos by remember { mutableStateOf(false) }
        var showProductividad by remember { mutableStateOf(false) }
        val hasPremium = SubscriptionManager.hasActiveSubscriptionNow()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HOME_MENU_CONTENT_PADDING_DP.dp),
            verticalArrangement = Arrangement.spacedBy(HOME_MENU_VERTICAL_SPACING_DP.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HOME_MENU_HORIZONTAL_SPACING_DP.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(HOME_MENU_VERTICAL_SPACING_DP.dp)
                ) {
                    Button(onClick = { onNavigate("neville") }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.neville_goddard))
                    }
                    Button(onClick = { onNavigate("joe") }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.joe_dispenza))
                    }
                    Button(onClick = { onNavigate("gregg") }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.gregg_braden))
                    }
                    Button(onClick = { onNavigate("bruce") }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.bruce_lipton))
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(horizontal = HOME_TOGGLE_DOT_PADDING_DP.dp)
                        .align(Alignment.CenterVertically),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(HOME_TOGGLE_DOT_TOUCH_SIZE_DP.dp)
                            .clip(CircleShape)
                            .clickable {
                                (requireActivity() as? MainActivity)?.toggleHomeAlternativeMode()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(HOME_TOGGLE_DOT_SIZE_DP.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = HOME_TOGGLE_DOT_ALPHA))
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(HOME_MENU_VERTICAL_SPACING_DP.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { showRecursos = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.home_menu_learning_resources))
                        }

                        DropdownMenu(
                            expanded = showRecursos,
                            onDismissRequest = { showRecursos = false },
                            shape = ContextMenuShape
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.home_nav_quotes)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_item),
                                        contentDescription = stringResource(R.string.home_nav_quotes)
                                    )
                                },
                                onClick = { showRecursos = false; onNavigate("frases") }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.home_nav_notes)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_note),
                                        contentDescription = stringResource(R.string.home_nav_notes)
                                    )
                                },
                                onClick = { showRecursos = false; onNavigate("notas") }
                            )
                            DropdownMenuItem(
                                text = {
                                    val title = stringResource(R.string.home_nav_encyclopedia)
                                    Text(if (hasPremium) title else stringResource(R.string.home_preview_label, title))
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_conf),
                                        contentDescription = stringResource(R.string.home_nav_encyclopedia)
                                    )
                                },
                                onClick = {
                                    showRecursos = false
                                    onNavigate("enciclopedia")
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    val title = stringResource(R.string.home_nav_scientific_evidence)
                                    Text(if (hasPremium) title else stringResource(R.string.home_preview_label, title))
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_tips),
                                        contentDescription = stringResource(R.string.home_nav_scientific_evidence)
                                    )
                                },
                                onClick = {
                                    showRecursos = false
                                    onNavigate("evidencia")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.home_nav_reflections)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_show),
                                        contentDescription = stringResource(R.string.home_nav_reflections)
                                    )
                                },
                                onClick = { showRecursos = false; onNavigate("reflexiones") }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.home_nav_help)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_ayuda),
                                        contentDescription = stringResource(R.string.home_nav_help)
                                    )
                                },
                                onClick = { showRecursos = false; onNavigate("ayudas") }
                            )
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { showProductividad = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.home_menu_productivity))
                        }
                        DropdownMenu(
                            expanded = showProductividad,
                            onDismissRequest = { showProductividad = false },
                            shape = ContextMenuShape
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.home_nav_diary)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_open_book),
                                        contentDescription = stringResource(R.string.home_nav_diary)
                                    )
                                },
                                onClick = { showProductividad = false; onNavigate("diario") }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.home_nav_canvas)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_edit_note),
                                        contentDescription = stringResource(R.string.home_nav_canvas)
                                    )
                                },
                                onClick = { showProductividad = false; onNavigate("lienzo") }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.home_nav_goals)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_arriba),
                                        contentDescription = stringResource(R.string.home_nav_goals)
                                    )
                                },
                                onClick = { showProductividad = false; onNavigate("metas") }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.home_nav_agenda)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_calendar_toggle),
                                        contentDescription = stringResource(R.string.home_nav_agenda)
                                    )
                                },
                                onClick = { showProductividad = false; onNavigate("agenda") }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.home_nav_reminders)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_calendar_toggle),
                                        contentDescription = stringResource(R.string.home_nav_reminders)
                                    )
                                },
                                onClick = { showProductividad = false; onNavigate("recordatorios") }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.home_nav_daily_ritual)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_calendar_toggle),
                                        contentDescription = stringResource(R.string.home_nav_daily_ritual)
                                    )
                                },
                                onClick = { showProductividad = false; onNavigate("dialogo_matutino") }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.weekly_summary_title)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_item),
                                        contentDescription = stringResource(R.string.weekly_summary_title)
                                    )
                                },
                                onClick = { showProductividad = false; onNavigate("resumen_semanal") }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.voice_notes_title)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_audio),
                                        contentDescription = stringResource(R.string.voice_notes_title)
                                    )
                                },
                                onClick = { showProductividad = false; onNavigate("voces") }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.anchors_title)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_tips),
                                        contentDescription = stringResource(R.string.anchors_title)
                                    )
                                },
                                onClick = { showProductividad = false; onNavigate("anclas") }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.calm_title)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_show),
                                        contentDescription = stringResource(R.string.calm_title)
                                    )
                                },
                                onClick = { showProductividad = false; onNavigate("calma") }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.coherence_title)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_tips),
                                        contentDescription = stringResource(R.string.coherence_title)
                                    )
                                },
                                onClick = { showProductividad = false; onNavigate("cardio") }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.presence_title)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_show),
                                        contentDescription = stringResource(R.string.presence_title)
                                    )
                                },
                                onClick = { showProductividad = false; onNavigate("presencia") }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.healing_center_title)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_ayuda),
                                        contentDescription = stringResource(R.string.healing_center_title)
                                    )
                                },
                                onClick = { showProductividad = false; onNavigate("centro_sanador") }
                            )
                        }
                    }
                    Button(onClick = { onNavigate("ajustes") }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.home_nav_settings))
                    }
                    Button(onClick = { onNavigate("premium") }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(
                                if (hasPremium) R.string.home_subscription_active
                                else R.string.home_extended_version
                            )
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val TAG = "HomeFloatingMenuBottomSheet"
        private const val MENU_CENTER_OFFSET_DP = 170f
        private const val HOME_MENU_CONTENT_PADDING_DP = 8
        private const val HOME_MENU_HORIZONTAL_SPACING_DP = 2
        private const val HOME_MENU_VERTICAL_SPACING_DP = 8
        private const val HOME_TOGGLE_DOT_PADDING_DP = 0
        private const val HOME_TOGGLE_DOT_TOUCH_SIZE_DP = 16
        private const val HOME_TOGGLE_DOT_SIZE_DP = 12
        private const val HOME_TOGGLE_DOT_ALPHA = 0.42f
    }
}
