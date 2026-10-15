package com.ypg.neville.ui.frag

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.ypg.neville.feature.presence.data.PresenceSettings
import com.ypg.neville.feature.cardiocoherence.data.CardioCoherencePreferences
import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryRepository
import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryReviewDay
import com.ypg.neville.feature.weeklysummary.domain.WeeklySummarySettings
import com.ypg.neville.model.preferences.DbPreferences
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.reminders.JournalDailyReminderManager
import com.ypg.neville.model.subscription.SubscriptionManager
import com.ypg.neville.feature.premiumpreview.PremiumFeatureId
import com.ypg.neville.model.utils.ColorPickerManager
import com.ypg.neville.model.utils.NewsContent
import com.ypg.neville.model.utils.UiModalWindows
import com.ypg.neville.ui.wirelesssync.WirelessSyncActivity

class frag_Setting : Fragment() {

    private sealed interface WeeklyCleanupResult {
        data class Success(val deletedCount: Int) : WeeklyCleanupResult
        data object Error : WeeklyCleanupResult
    }

    private val notesBiometricLockPrefKey = "notes_biometric_lock_enabled"

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                com.ypg.neville.ui.theme.NevilleTheme {
                    SettingsScreen()
                }
            }
        }
    }

    @Composable
    private fun SettingsScreen() {
        val context = LocalContext.current
        val prefs = remember { DbPreferences.default(context) }
        val initialJournalConfig = remember { JournalDailyReminderManager.readConfig(context) }
        val initialWeeklySummaryConfig = remember { WeeklySummarySettings.readConfig(context) }

        var temaNoche by remember { mutableStateOf(prefs.getBoolean("tema", true)) }
        var fuenteFrase by remember { mutableStateOf((prefs.getString("fuente_frase", "28")?.toIntOrNull() ?: 28).coerceIn(14, 40)) }
        var fuenteListados by remember { mutableStateOf((prefs.getString("fuente_listados", "22")?.toIntOrNull() ?: 22).coerceIn(12, 40)) }
        var fuenteConf by remember { mutableStateOf((prefs.getString("fuente_conf", "170")?.toIntOrNull() ?: 170).coerceIn(100, 250)) }
        var filterAutorNeville by remember { mutableStateOf(prefs.getBoolean("home_filter_author_neville", true)) }
        var filterAutorJoe by remember { mutableStateOf(prefs.getBoolean("home_filter_author_joe", true)) }
        var filterAutorGregg by remember { mutableStateOf(prefs.getBoolean("home_filter_author_gregg", true)) }
        var filterAutorBruce by remember { mutableStateOf(prefs.getBoolean("home_filter_author_bruce", true)) }
        var filterOtros by remember { mutableStateOf(prefs.getBoolean("home_filter_otros", true)) }
        var filterSalud by remember { mutableStateOf(prefs.getBoolean("home_filter_salud", true)) }
        var filterFavoritas by remember { mutableStateOf(prefs.getBoolean("home_filter_favoritas", false)) }
        var filterPersonales by remember { mutableStateOf(prefs.getBoolean("home_filter_personales", false)) }
        var filterConNota by remember { mutableStateOf(prefs.getBoolean("home_filter_con_nota", false)) }
        var showFrasesInicioFilterMenu by remember { mutableStateOf(false) }
        var agendaHomeButtonEnabled by remember {
            mutableStateOf(prefs.getBoolean(FragHome.PREF_KEY_AGENDA_HOME_BUTTON_ENABLED, true))
        }
        var presenceCelebrationPhrase by remember {
            mutableStateOf(
                prefs.getString(
                    PresenceSettings.CUSTOM_CELEBRATION_PHRASE_KEY,
                    PresenceSettings.DEFAULT_CELEBRATION_PHRASE
                )?.takeIf { it.isNotBlank() } ?: PresenceSettings.DEFAULT_CELEBRATION_PHRASE
            )
        }
        var presencePhraseInput by remember { mutableStateOf(presenceCelebrationPhrase) }
        var cardioSessionPhrases by remember {
            mutableStateOf(CardioCoherencePreferences.loadSessionPhrases(context))
        }
        var presenceHomeButtonEnabled by remember {
            mutableStateOf(prefs.getBoolean(FragHome.PREF_KEY_PRESENCE_HOME_BUTTON_ENABLED, true))
        }
        var homeAlternativePresenceTotal by remember {
            mutableStateOf(
                prefs.getInt(
                    FragHome.PREF_KEY_HOME_ALTERNATIVE_PRESENCE_TOTAL,
                    FragHome.HOME_ALTERNATIVE_PRESENCE_TOTAL_DEFAULT
                ).coerceAtLeast(FragHome.HOME_ALTERNATIVE_PRESENCE_TOTAL_DEFAULT)
            )
        }
        var homeAlternativeGoalsTotal by remember {
            mutableStateOf(
                prefs.getInt(
                    FragHome.PREF_KEY_HOME_ALTERNATIVE_GOALS_TOTAL,
                    FragHome.HOME_ALTERNATIVE_GOALS_TOTAL_DEFAULT
                ).coerceAtLeast(FragHome.HOME_ALTERNATIVE_GOALS_TOTAL_DEFAULT)
            )
        }
        var homeAlternativeDiaryTotal by remember {
            mutableStateOf(
                prefs.getInt(
                    FragHome.PREF_KEY_HOME_ALTERNATIVE_DIARY_TOTAL,
                    FragHome.HOME_ALTERNATIVE_DIARY_TOTAL_DEFAULT
                ).coerceAtLeast(FragHome.HOME_ALTERNATIVE_DIARY_TOTAL_DEFAULT)
            )
        }
        var notesBiometricLockEnabled by remember { mutableStateOf(prefs.getBoolean(notesBiometricLockPrefKey, false)) }
        var journalReminderEnabled by remember { mutableStateOf(initialJournalConfig.enabled) }
        var journalReminderHour by remember { mutableStateOf(initialJournalConfig.hour) }
        var journalReminderMinute by remember { mutableStateOf(initialJournalConfig.minute) }
        var journalReminderCustomMessage by remember { mutableStateOf(initialJournalConfig.customMessage) }
        var weeklyReviewDay by remember { mutableStateOf(initialWeeklySummaryConfig.day) }
        var weeklyReviewNotificationsEnabled by remember {
            mutableStateOf(initialWeeklySummaryConfig.notificationsEnabled)
        }
        var weeklyReviewRecordsToKeep by remember {
            mutableStateOf(initialWeeklySummaryConfig.recordsToKeep)
        }
        var bottomNavAccesses by remember { mutableStateOf(BottomNavPreferences.load(prefs)) }
        var bottomNavPickerSlot by remember { mutableStateOf<Int?>(null) }

        var showJournalTimeDialog by remember { mutableStateOf(false) }
        var showJournalMessageDialog by remember { mutableStateOf(false) }
        var showPresencePhraseDialog by remember { mutableStateOf(false) }
        var showCardioPhrasesDialog by remember { mutableStateOf(false) }
        var journalMessageInput by remember { mutableStateOf(journalReminderCustomMessage) }
        var showWeeklyReviewDayMenu by remember { mutableStateOf(false) }
        var showWeeklyReviewCleanupConfirmation by remember { mutableStateOf(false) }
        var weeklyReviewCleanupResult by remember {
            mutableStateOf<WeeklyCleanupResult?>(null)
        }
        val weeklyNotificationPermissionDeniedMessage = stringResource(
            R.string.weekly_settings_notification_permission_denied
        )

        val weeklyNotificationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            val updated = WeeklySummarySettings.setNotificationsEnabled(context, granted)
            weeklyReviewNotificationsEnabled = updated.notificationsEnabled
            if (!granted) {
                Toast.makeText(
                    context,
                    weeklyNotificationPermissionDeniedMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        val hasPremiumSubscription = SubscriptionManager.hasActiveSubscriptionNow()

        fun saveAuthorsAggregate() {
            prefs.edit {
                putBoolean(
                    "home_filter_autores",
                    filterAutorNeville || filterAutorJoe || filterAutorGregg || filterAutorBruce
                )
            }
        }

        fun applyAuthorSelection(
            nev: Boolean = filterAutorNeville,
            joe: Boolean = filterAutorJoe,
            gregg: Boolean = filterAutorGregg,
            bruce: Boolean = filterAutorBruce
        ) {
            var nextNeville = nev
            val nextJoe = joe
            val nextGregg = gregg
            val nextBruce = bruce

            if (!nextNeville && !nextJoe && !nextGregg && !nextBruce) {
                nextNeville = true
            }

            filterAutorNeville = nextNeville
            filterAutorJoe = nextJoe
            filterAutorGregg = nextGregg
            filterAutorBruce = nextBruce

            prefs.edit {
                putBoolean("home_filter_author_neville", nextNeville)
                putBoolean("home_filter_author_joe", nextJoe)
                putBoolean("home_filter_author_gregg", nextGregg)
                putBoolean("home_filter_author_bruce", nextBruce)
            }
            saveAuthorsAggregate()
        }

        fun requestPremiumForJournal() {
            (activity as? MainActivity)?.showSubscriptionPaywall()
        }

        LaunchedEffect(hasPremiumSubscription) {
            if (!hasPremiumSubscription && journalReminderEnabled) {
                val updated = JournalDailyReminderManager.setEnabled(context, false)
                journalReminderEnabled = updated.enabled
                journalReminderHour = updated.hour
                journalReminderMinute = updated.minute
                journalReminderCustomMessage = updated.customMessage
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.settings_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                SettingSection(
                    title = stringResource(R.string.settings_appearance_title),
                    subtitle = stringResource(R.string.settings_appearance_subtitle)
                ) {
                    SwitchField(
                        title = stringResource(R.string.settings_dark_theme),
                        description = stringResource(R.string.settings_dark_theme_description),
                        checked = temaNoche,
                        onCheckedChange = {
                            temaNoche = it
                            prefs.edit { putBoolean("tema", it) }
                            AppCompatDelegate.setDefaultNightMode(
                                if (it) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                            )
                            activity?.recreate()
                        }
                    )
                    FieldDivider()
                    ActionField(
                        title = stringResource(R.string.settings_home_font_color),
                        description = stringResource(R.string.settings_home_font_color_description)
                    ) {
                        ColorPickerManager.showColorPicker(
                            context,
                            prefs.getInt("color_letra_frases_home", prefs.getInt("color_letra_frases", 0xFF1F2A37.toInt())),
                            "color_letra_frases_home",
                            context.getString(R.string.settings_home_font_color_picker_title)
                        )
                    }
                    FieldDivider()
                    ActionField(
                        title = stringResource(R.string.settings_gradient_top_color),
                        description = stringResource(R.string.settings_home_gradient_top_description)
                    ) {
                        ColorPickerManager.showColorPicker(
                            context,
                            prefs.getInt("color_fondo_a", 0xFFC69FF9.toInt()),
                            "color_fondo_a",
                            context.getString(R.string.settings_gradient_top_color_picker_title)
                        )
                    }
                    FieldDivider()
                    ActionField(
                        title = stringResource(R.string.settings_gradient_bottom_color),
                        description = stringResource(R.string.settings_home_gradient_bottom_description)
                    ) {
                        ColorPickerManager.showColorPicker(
                            context,
                            prefs.getInt("color_fondo_b", 0xFFC4AA8E.toInt()),
                            "color_fondo_b",
                            context.getString(R.string.settings_gradient_bottom_color_picker_title)
                        )
                    }
                }
            }

            item {
                SettingSection(
                    title = stringResource(R.string.bottom_nav_settings_title),
                    subtitle = stringResource(R.string.bottom_nav_settings_subtitle)
                ) {
                    val slotLabels = listOf(
                        R.string.bottom_nav_slot_outer_left,
                        R.string.bottom_nav_slot_inner_left,
                        R.string.bottom_nav_slot_inner_right,
                        R.string.bottom_nav_slot_outer_right
                    )
                    bottomNavAccesses.forEachIndexed { index, access ->
                        if (index > 0) FieldDivider()
                        ActionField(
                            title = stringResource(slotLabels[index]),
                            description = stringResource(access.labelRes)
                        ) {
                            bottomNavPickerSlot = index
                        }
                    }
                    FieldDivider()
                    ActionField(
                        title = stringResource(R.string.bottom_nav_restore_defaults),
                        description = stringResource(R.string.bottom_nav_restore_defaults_description)
                    ) {
                        bottomNavAccesses = BottomNavAccess.defaults
                        BottomNavPreferences.save(prefs, bottomNavAccesses)
                        (activity as? MainActivity)?.updateBottomNavAccesses(bottomNavAccesses)
                    }
                }
            }

            item {
                SettingSection(
                    title = stringResource(R.string.settings_font_size_title),
                    subtitle = stringResource(R.string.settings_font_size_subtitle)
                ) {
                    SliderField(
                        title = stringResource(R.string.settings_quote_text),
                        description = stringResource(R.string.settings_quote_text_description),
                        value = fuenteFrase,
                        range = 14..40
                    ) {
                        fuenteFrase = it
                        prefs.edit { putString("fuente_frase", it.toString()) }
                    }
                    FieldDivider()
                    SliderField(
                        title = stringResource(R.string.settings_lists),
                        description = stringResource(R.string.settings_lists_description),
                        value = fuenteListados,
                        range = 12..40
                    ) {
                        fuenteListados = it
                        prefs.edit { putString("fuente_listados", it.toString()) }
                    }
                    FieldDivider()
                    SliderField(
                        title = stringResource(R.string.settings_content_zoom),
                        description = stringResource(R.string.settings_content_zoom_description),
                        value = fuenteConf,
                        range = 100..250
                    ) {
                        fuenteConf = it
                        prefs.edit { putString("fuente_conf", it.toString()) }
                    }
                }
            }

            item {
                SettingSection(
                    title = stringResource(R.string.settings_reading_colors_title),
                    subtitle = stringResource(R.string.settings_reading_colors_subtitle)
                ) {
                    ActionField(
                        title = stringResource(R.string.settings_gradient_top_color),
                        description = ""
                    ) {
                        ColorPickerManager.showColorPicker(
                            context,
                            prefs.getInt("color_lectura_fondo_a", 0xFFF8F4EA.toInt()),
                            "color_lectura_fondo_a",
                            context.getString(R.string.settings_gradient_top_color_picker_title)
                        )
                    }
                    FieldDivider()
                    ActionField(
                        title = stringResource(R.string.settings_gradient_bottom_color),
                        description = ""
                    ) {
                        ColorPickerManager.showColorPicker(
                            context,
                            prefs.getInt("color_lectura_fondo_b", 0xFFECE3D3.toInt()),
                            "color_lectura_fondo_b",
                            context.getString(R.string.settings_gradient_bottom_color_picker_title)
                        )
                    }
                    FieldDivider()
                    ActionField(
                        title = stringResource(R.string.settings_reader_text_color),
                        description = stringResource(R.string.settings_reader_text_color_description)
                    ) {
                        ColorPickerManager.showColorPicker(
                            context,
                            prefs.getInt("color_lectura_texto", 0xFF2B2115.toInt()),
                            "color_lectura_texto",
                            context.getString(R.string.settings_reader_text_color_picker_title)
                        )
                    }
                }
            }

            item {
                SettingSection(
                    title = stringResource(R.string.settings_home_quotes_title),
                    subtitle = stringResource(R.string.settings_home_quotes_subtitle)
                ) {
                    val anyAuthorActive = filterAutorNeville || filterAutorJoe || filterAutorGregg || filterAutorBruce
                    val hasFavOrNoteFilter = filterFavoritas || filterConNota
                    val favNoteDescriptor = when {
                        filterFavoritas && filterConNota -> stringResource(R.string.settings_filter_favorites_with_notes)
                        filterFavoritas -> stringResource(R.string.settings_filter_favorites_lowercase)
                        else -> stringResource(R.string.settings_filter_with_notes_lowercase)
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        ActionField(
                            title = stringResource(R.string.settings_select_filters),
                            description = stringResource(R.string.settings_select_filters_description)
                        ) {
                            showFrasesInicioFilterMenu = true
                        }
                        DropdownMenu(
                            expanded = showFrasesInicioFilterMenu,
                            onDismissRequest = { showFrasesInicioFilterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (filterAutorNeville) "✓ Neville Goddard" else "Neville Goddard") },
                                onClick = {
                                    applyAuthorSelection(nev = !filterAutorNeville)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (filterAutorJoe) "✓ Joe Dispenza" else "Joe Dispenza") },
                                onClick = {
                                    applyAuthorSelection(joe = !filterAutorJoe)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (filterAutorGregg) "✓ Gregg Braden" else "Gregg Braden") },
                                onClick = {
                                    applyAuthorSelection(gregg = !filterAutorGregg)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (filterAutorBruce) "✓ Bruce Lipton" else "Bruce Lipton") },
                                onClick = {
                                    applyAuthorSelection(bruce = !filterAutorBruce)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (filterOtros) stringResource(R.string.settings_checked_other_authors) else stringResource(R.string.settings_other_authors)) },
                                onClick = {
                                    val newValue = !filterOtros
                                    filterOtros = newValue
                                    prefs.edit { putBoolean("home_filter_otros", newValue) }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (filterSalud) stringResource(R.string.settings_checked_health) else stringResource(R.string.settings_health)) },
                                onClick = {
                                    val newValue = !filterSalud
                                    filterSalud = newValue
                                    prefs.edit { putBoolean("home_filter_salud", newValue) }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (filterFavoritas) stringResource(R.string.settings_checked_favorite_quotes) else stringResource(R.string.settings_favorite_quotes)) },
                                onClick = {
                                    val newValue = !filterFavoritas
                                    filterFavoritas = newValue
                                    prefs.edit { putBoolean("home_filter_favoritas", newValue) }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (filterPersonales) stringResource(R.string.settings_checked_personal_quotes) else stringResource(R.string.settings_personal_quotes)) },
                                onClick = {
                                    val newValue = !filterPersonales
                                    filterPersonales = newValue
                                    prefs.edit { putBoolean("home_filter_personales", newValue) }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (filterConNota) stringResource(R.string.settings_checked_quotes_with_note) else stringResource(R.string.settings_quotes_with_note)) },
                                onClick = {
                                    val newValue = !filterConNota
                                    filterConNota = newValue
                                    prefs.edit { putBoolean("home_filter_con_nota", newValue) }
                                }
                            )
                        }
                    }

                    if (hasFavOrNoteFilter) {
                        Text(
                            text = if (anyAuthorActive) {
                                stringResource(R.string.settings_filtered_active_authors, favNoteDescriptor)
                            } else {
                                stringResource(R.string.settings_filtered_all_authors, favNoteDescriptor)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val activeFilters = buildList<Pair<String, () -> Unit>> {
                        if (filterAutorNeville) add("Neville" to {
                            applyAuthorSelection(nev = false)
                        })
                        if (filterAutorJoe) add("Joe" to {
                            applyAuthorSelection(joe = false)
                        })
                        if (filterAutorGregg) add("Gregg" to {
                            applyAuthorSelection(gregg = false)
                        })
                        if (filterAutorBruce) add("Bruce" to {
                            applyAuthorSelection(bruce = false)
                        })
                        if (filterOtros) add(stringResource(R.string.settings_filter_other) to {
                            filterOtros = false
                            prefs.edit { putBoolean("home_filter_otros", false) }
                        })
                        if (filterSalud) add(stringResource(R.string.settings_health) to {
                            filterSalud = false
                            prefs.edit { putBoolean("home_filter_salud", false) }
                        })
                        if (filterFavoritas) add(stringResource(R.string.settings_filter_favorites) to {
                            filterFavoritas = false
                            prefs.edit { putBoolean("home_filter_favoritas", false) }
                        })
                        if (filterPersonales) add(stringResource(R.string.settings_filter_personal) to {
                            filterPersonales = false
                            prefs.edit { putBoolean("home_filter_personales", false) }
                        })
                        if (filterConNota) add(stringResource(R.string.settings_filter_with_note) to {
                            filterConNota = false
                            prefs.edit { putBoolean("home_filter_con_nota", false) }
                        })
                    }

                    if (activeFilters.isEmpty()) {
                        Text(
                            text = stringResource(R.string.settings_no_active_filters),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            activeFilters.forEach { (label, onRemove) ->
                                ActiveFilterChip(label = label, onRemove = onRemove)
                            }
                        }
                    }
                }
            }

            item {
                SettingSection(
                    title = stringResource(R.string.settings_calm_space_title),
                    subtitle = stringResource(R.string.settings_calm_space_subtitle)
                ) {
                    ActionField(
                        title = stringResource(R.string.settings_manage_calm_quotes),
                        description = stringResource(R.string.settings_manage_calm_quotes_description)
                    ) {
                        MainActivity.currentInstance()?.openDestinationAsSheet(R.id.frag_calm_phrase_manager)
                    }
                    FieldDivider()
                    ActionField(
                        title = stringResource(R.string.settings_manage_calm_backgrounds),
                        description = stringResource(R.string.settings_manage_calm_backgrounds_description)
                    ) {
                        MainActivity.currentInstance()?.openDestinationAsSheet(R.id.frag_calm_backgrounds_manager)
                    }
                    FieldDivider()
                    ActionField(
                        title = stringResource(R.string.settings_manage_calm_music),
                        description = stringResource(R.string.settings_manage_calm_music_description)
                    ) {
                        MainActivity.currentInstance()?.openDestinationAsSheet(R.id.frag_calm_music_manager)
                    }
                }
            }

            item {
                SettingSection(
                    title = stringResource(R.string.settings_alternative_home_title),
                    subtitle = stringResource(R.string.settings_alternative_home_subtitle)
                ) {
                    SliderField(
                        title = stringResource(R.string.settings_presence_total),
                        description = stringResource(R.string.settings_presence_total_description),
                        value = homeAlternativePresenceTotal,
                        range = FragHome.HOME_ALTERNATIVE_PRESENCE_TOTAL_DEFAULT..50
                    ) { value ->
                        homeAlternativePresenceTotal = value.coerceAtLeast(FragHome.HOME_ALTERNATIVE_PRESENCE_TOTAL_DEFAULT)
                        FragHome.homeAlternativePresenceTotalState.value = homeAlternativePresenceTotal
                        prefs.edit {
                            putInt(
                                FragHome.PREF_KEY_HOME_ALTERNATIVE_PRESENCE_TOTAL,
                                homeAlternativePresenceTotal
                            )
                        }
                    }
                    FieldDivider()
                    SliderField(
                        title = stringResource(R.string.settings_goals_total),
                        description = stringResource(R.string.settings_goals_total_description),
                        value = homeAlternativeGoalsTotal,
                        range = FragHome.HOME_ALTERNATIVE_GOALS_TOTAL_DEFAULT..20
                    ) { value ->
                        homeAlternativeGoalsTotal = value.coerceAtLeast(FragHome.HOME_ALTERNATIVE_GOALS_TOTAL_DEFAULT)
                        FragHome.homeAlternativeGoalsTotalState.value = homeAlternativeGoalsTotal
                        prefs.edit {
                            putInt(
                                FragHome.PREF_KEY_HOME_ALTERNATIVE_GOALS_TOTAL,
                                homeAlternativeGoalsTotal
                            )
                        }
                    }
                    FieldDivider()
                    SliderField(
                        title = stringResource(R.string.settings_diary_total),
                        description = stringResource(R.string.settings_diary_total_description),
                        value = homeAlternativeDiaryTotal,
                        range = FragHome.HOME_ALTERNATIVE_DIARY_TOTAL_DEFAULT..20
                    ) { value ->
                        homeAlternativeDiaryTotal = value.coerceAtLeast(FragHome.HOME_ALTERNATIVE_DIARY_TOTAL_DEFAULT)
                        FragHome.homeAlternativeDiaryTotalState.value = homeAlternativeDiaryTotal
                        prefs.edit {
                            putInt(
                                FragHome.PREF_KEY_HOME_ALTERNATIVE_DIARY_TOTAL,
                                homeAlternativeDiaryTotal
                            )
                        }
                    }
                }
            }

            item {
                SettingSection(
                    title = stringResource(R.string.settings_coherence_title),
                    subtitle = stringResource(R.string.settings_coherence_subtitle)
                ) {
                    ActionField(
                        title = stringResource(R.string.settings_session_quotes),
                        description = stringResource(R.string.settings_session_quotes_description)
                    ) {
                        cardioSessionPhrases = CardioCoherencePreferences.loadSessionPhrases(context)
                        showCardioPhrasesDialog = true
                    }
                    FieldDivider()
                    ActionField(
                        title = stringResource(R.string.settings_restore_default_quotes),
                        description = stringResource(R.string.settings_restore_default_quotes_description)
                    ) {
                        CardioCoherencePreferences.resetSessionPhrases(context)
                        cardioSessionPhrases = CardioCoherencePreferences.defaultSessionPhrases
                        Toast.makeText(
                            context,
                            context.getString(R.string.settings_coherence_quotes_restored),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            item {
                SettingSection(
                    title = stringResource(R.string.settings_presence_title),
                    subtitle = stringResource(R.string.settings_presence_subtitle)
                ) {
                    ActionField(
                        title = stringResource(R.string.settings_celebration_quote),
                        description = presenceCelebrationPhrase.trim().ifBlank {
                            PresenceSettings.DEFAULT_CELEBRATION_PHRASE
                        }
                    ) {
                        presencePhraseInput = presenceCelebrationPhrase
                        showPresencePhraseDialog = true
                    }
                    FieldDivider()
                    ActionField(
                        title = stringResource(R.string.settings_restore_default_quote),
                        description = PresenceSettings.DEFAULT_CELEBRATION_PHRASE
                    ) {
                        presenceCelebrationPhrase = PresenceSettings.DEFAULT_CELEBRATION_PHRASE
                        presencePhraseInput = PresenceSettings.DEFAULT_CELEBRATION_PHRASE
                        prefs.edit {
                            putString(
                                PresenceSettings.CUSTOM_CELEBRATION_PHRASE_KEY,
                                PresenceSettings.DEFAULT_CELEBRATION_PHRASE
                            )
                        }
                        Toast.makeText(context, context.getString(R.string.settings_presence_quote_restored), Toast.LENGTH_SHORT).show()
                    }
                    FieldDivider()
                    SwitchField(
                        title = if (presenceHomeButtonEnabled) stringResource(R.string.settings_home_button_active) else stringResource(R.string.settings_home_button_hidden),
                        description = stringResource(R.string.settings_presence_home_button_description),
                        checked = presenceHomeButtonEnabled
                    ) { enabled ->
                        presenceHomeButtonEnabled = enabled
                        prefs.edit { putBoolean(FragHome.PREF_KEY_PRESENCE_HOME_BUTTON_ENABLED, enabled) }
                    }
                }
            }

            item {
                SettingSection(
                    title = stringResource(R.string.settings_agenda_title),
                    subtitle = stringResource(R.string.settings_agenda_subtitle)
                ) {
                    SwitchField(
                        title = if (agendaHomeButtonEnabled) stringResource(R.string.settings_home_button_active) else stringResource(R.string.settings_home_button_hidden),
                        description = stringResource(R.string.settings_agenda_home_button_description),
                        checked = agendaHomeButtonEnabled
                    ) { enabled ->
                        agendaHomeButtonEnabled = enabled
                        prefs.edit { putBoolean(FragHome.PREF_KEY_AGENDA_HOME_BUTTON_ENABLED, enabled) }
                    }
                }
            }

            item {
                val locale = LocalLocale.current.platformLocale
                val selectedDayTitle = stringResource(weeklyReviewDay.titleRes)
                SettingSection(
                    title = stringResource(R.string.weekly_settings_title),
                    subtitle = stringResource(R.string.weekly_settings_subtitle)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ActionField(
                            title = stringResource(R.string.weekly_settings_review_day),
                            description = selectedDayTitle
                        ) {
                            showWeeklyReviewDayMenu = true
                        }
                        DropdownMenu(
                            expanded = showWeeklyReviewDayMenu,
                            onDismissRequest = { showWeeklyReviewDayMenu = false }
                        ) {
                            WeeklySummaryReviewDay.entries.forEach { day ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (day == weeklyReviewDay) {
                                                "✓ ${stringResource(day.titleRes)}"
                                            } else {
                                                stringResource(day.titleRes)
                                            }
                                        )
                                    },
                                    onClick = {
                                        val updated = WeeklySummarySettings.setReviewDay(context, day)
                                        weeklyReviewDay = updated.day
                                        showWeeklyReviewDayMenu = false
                                    }
                                )
                            }
                        }
                    }

                    FieldDivider()
                    SwitchField(
                        title = stringResource(R.string.weekly_settings_reminder_at_six),
                        description = stringResource(
                            R.string.weekly_settings_availability,
                            selectedDayTitle.lowercase(locale)
                        ),
                        checked = weeklyReviewNotificationsEnabled
                    ) { enabled ->
                        if (
                            enabled &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            weeklyNotificationPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        } else {
                            val updated = WeeklySummarySettings.setNotificationsEnabled(
                                context,
                                enabled
                            )
                            weeklyReviewNotificationsEnabled = updated.notificationsEnabled
                        }
                    }

                    FieldDivider()
                    StepValueField(
                        title = stringResource(
                            R.string.weekly_settings_keep_records,
                            weeklyReviewRecordsToKeep
                        ),
                        description = stringResource(
                            R.string.weekly_settings_cleanup_description,
                            weeklyReviewRecordsToKeep
                        ),
                        value = weeklyReviewRecordsToKeep,
                        minValue = WeeklySummarySettings.MIN_RECORDS_TO_KEEP,
                        maxValue = WeeklySummarySettings.MAX_RECORDS_TO_KEEP,
                        step = WeeklySummarySettings.RECORD_STEP
                    ) { value ->
                        val updated = WeeklySummarySettings.setRecordsToKeep(context, value)
                        weeklyReviewRecordsToKeep = updated.recordsToKeep
                    }

                    FieldDivider()
                    TextButton(
                        onClick = { showWeeklyReviewCleanupConfirmation = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.weekly_settings_delete_old),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                SettingSection(
                    title = stringResource(R.string.settings_notes_title),
                    subtitle = stringResource(R.string.settings_notes_subtitle)
                ) {
                    SwitchField(
                        title = stringResource(R.string.settings_biometric_lock),
                        description = stringResource(R.string.settings_biometric_lock_description),
                        checked = notesBiometricLockEnabled
                    ) { newValue ->
                        if (!newValue) {
                            authenticateForNotesLockChange {
                                notesBiometricLockEnabled = false
                                prefs.edit { putBoolean(notesBiometricLockPrefKey, false) }
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.settings_notes_biometric_disabled),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            return@SwitchField
                        }

                        if (!com.ypg.neville.model.subscription.SubscriptionManager.hasActiveSubscriptionNow()) {
                            (activity as? MainActivity)?.showPremiumFeaturePreview(
                                PremiumFeatureId.PROTECTED_NOTES
                            )
                            return@SwitchField
                        }

                        val canAuth = BiometricManager.from(context).canAuthenticate(
                            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                BiometricManager.Authenticators.BIOMETRIC_WEAK
                        )
                        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.settings_biometric_unavailable),
                                Toast.LENGTH_LONG
                            ).show()
                            return@SwitchField
                        }

                        notesBiometricLockEnabled = true
                        prefs.edit { putBoolean(notesBiometricLockPrefKey, true) }
                    }
                }
            }

            item {
                SettingSection(
                    title = stringResource(R.string.settings_daily_reminder_title),
                    subtitle = if (hasPremiumSubscription) {
                        stringResource(R.string.settings_daily_reminder_premium_subtitle)
                    } else {
                        stringResource(R.string.settings_daily_reminder_locked_subtitle)
                    }
                ) {
                    SwitchField(
                        title = if (journalReminderEnabled) stringResource(R.string.settings_reminder_active) else stringResource(R.string.settings_reminder_paused),
                        description = if (hasPremiumSubscription) {
                            val locale = LocalLocale.current.platformLocale
                            if (journalReminderEnabled) {
                                stringResource(
                                    R.string.settings_reminder_sends_daily_at,
                                    String.format(locale, "%02d:%02d", journalReminderHour, journalReminderMinute)
                                )
                            } else {
                                stringResource(R.string.settings_reminder_enable_description)
                            }
                        } else {
                            stringResource(R.string.settings_subscription_required)
                        },
                        checked = journalReminderEnabled
                    ) { enabled ->
                        if (!hasPremiumSubscription) {
                            requestPremiumForJournal()
                            return@SwitchField
                        }
                        val updated = JournalDailyReminderManager.setEnabled(context, enabled)
                        journalReminderEnabled = updated.enabled
                        journalReminderHour = updated.hour
                        journalReminderMinute = updated.minute
                        journalReminderCustomMessage = updated.customMessage
                    }

                    FieldDivider()
                    val locale = LocalLocale.current.platformLocale
                    ActionField(
                        title = stringResource(R.string.settings_notification_time),
                        description = stringResource(
                            R.string.settings_scheduled_daily_at,
                            String.format(locale, "%02d:%02d", journalReminderHour, journalReminderMinute)
                        )
                    ) {
                        if (!hasPremiumSubscription) {
                            requestPremiumForJournal()
                            return@ActionField
                        }
                        showJournalTimeDialog = true
                    }

                    FieldDivider()
                    ActionField(
                        title = stringResource(R.string.settings_notification_text),
                        description = journalReminderCustomMessage.trim().ifBlank {
                            stringResource(
                                R.string.settings_default_value,
                                stringResource(R.string.global_journal_default_message)
                            )
                        }
                    ) {
                        if (!hasPremiumSubscription) {
                            requestPremiumForJournal()
                            return@ActionField
                        }
                        journalMessageInput = journalReminderCustomMessage
                        showJournalMessageDialog = true
                    }
                }
            }

            item {
                SettingSection(
                    title = stringResource(R.string.settings_wireless_sync_title),
                    subtitle = stringResource(R.string.settings_wireless_sync_subtitle)
                ) {
                    ActionField(
                        title = stringResource(R.string.wireless_sync_title),
                        description = stringResource(R.string.wireless_sync_settings_description)
                    ) {
                        authenticateForWirelessSync(
                            title = context.getString(R.string.wireless_sync_title),
                            subtitle = context.getString(R.string.wireless_sync_authentication)
                        ) {
                            startActivity(Intent(context, WirelessSyncActivity::class.java))
                        }
                    }
                }
            }

            item {
                SettingSection(
                    title = stringResource(R.string.settings_project_support_title),
                    subtitle = stringResource(R.string.settings_project_support_subtitle)
                ) {
                    ActionField(stringResource(R.string.settings_whats_new), stringResource(R.string.settings_whats_new_description)) {
                        UiModalWindows.showAyudaContectual(
                            context,
                            context.getString(R.string.settings_whats_new),
                            context.getString(R.string.settings_whats_new_question),
                            NewsContent.buildNewsText(context),
                            false,
                            AppCompatResources.getDrawable(context, R.drawable.neville)
                        )
                    }
                    FieldDivider()
                    ActionField(stringResource(R.string.settings_send_feedback), stringResource(R.string.settings_send_feedback_description)) {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = "mailto:info@ypgcode.es".toUri()
                            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.settings_feedback_email_subject))
                        }
                        try {
                            startActivity(emailIntent)
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(context, context.getString(R.string.settings_email_app_not_found), Toast.LENGTH_LONG).show()
                        }
                    }
                    FieldDivider()
                    ActionField(stringResource(R.string.settings_project_website), stringResource(R.string.settings_project_website_description)) {
                        startActivity(Intent(Intent.ACTION_VIEW, "https://ypgcode.es/neville_goddard/".toUri()))
                    }
                    FieldDivider()
                    ActionField(stringResource(R.string.settings_write_review), stringResource(R.string.settings_write_review_description)) {
                        val uri = "market://details?id=${context.packageName}".toUri()
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        try {
                            startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(context, context.getString(R.string.settings_store_app_not_found), Toast.LENGTH_LONG).show()
                        }
                    }


                }
            }
        }

        bottomNavPickerSlot?.let { slotIndex ->
            AlertDialog(
                onDismissRequest = { bottomNavPickerSlot = null },
                title = { Text(stringResource(R.string.bottom_nav_picker_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.bottom_nav_picker_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 440.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            BottomNavAccessGroup.entries.forEach { group ->
                                item(key = "group_${group.name}") {
                                    Text(
                                        text = stringResource(group.titleRes),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                    )
                                }
                                val groupAccesses = BottomNavAccess.entries.filter { it.group == group }
                                items(
                                    count = groupAccesses.size,
                                    key = { groupAccesses[it].id }
                                ) { accessIndex ->
                                    val access = groupAccesses[accessIndex]
                                    val isSelected = bottomNavAccesses[slotIndex] == access
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val updated = bottomNavAccesses.toMutableList()
                                                val occupiedSlot = updated.indexOf(access)
                                                if (occupiedSlot >= 0) {
                                                    updated[occupiedSlot] = updated[slotIndex]
                                                }
                                                updated[slotIndex] = access
                                                bottomNavAccesses = updated
                                                BottomNavPreferences.save(prefs, updated)
                                                (activity as? MainActivity)?.updateBottomNavAccesses(updated)
                                                bottomNavPickerSlot = null
                                            }
                                            .padding(horizontal = 6.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(access.iconRes),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.size(12.dp))
                                        Text(
                                            text = stringResource(access.labelRes),
                                            style = MaterialTheme.typography.bodyLarge,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isSelected) {
                                            Text(
                                                text = "✓",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { bottomNavPickerSlot = null }) {
                        Text(stringResource(R.string.settings_cancel))
                    }
                }
            )
        }

        if (showWeeklyReviewCleanupConfirmation) {
            AlertDialog(
                onDismissRequest = { showWeeklyReviewCleanupConfirmation = false },
                title = {
                    Text(stringResource(R.string.weekly_settings_delete_confirmation_title))
                },
                text = {
                    Text(
                        stringResource(
                            R.string.weekly_settings_delete_confirmation_body,
                            weeklyReviewRecordsToKeep
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showWeeklyReviewCleanupConfirmation = false
                            weeklyReviewCleanupResult = runCatching {
                                WeeklySummaryRepository.createDefault()
                                    .deleteOldSummaries(weeklyReviewRecordsToKeep)
                            }.fold(
                                onSuccess = { deletedCount ->
                                    WeeklyCleanupResult.Success(deletedCount)
                                },
                                onFailure = { WeeklyCleanupResult.Error }
                            )
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.weekly_settings_delete_action),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showWeeklyReviewCleanupConfirmation = false }
                    ) {
                        Text(stringResource(R.string.settings_cancel))
                    }
                }
            )
        }

        weeklyReviewCleanupResult?.let { result ->
            val message = when (result) {
                is WeeklyCleanupResult.Success -> when (result.deletedCount) {
                    0 -> stringResource(
                        R.string.weekly_settings_prune_none,
                        weeklyReviewRecordsToKeep
                    )
                    1 -> stringResource(
                        R.string.weekly_settings_prune_one,
                        weeklyReviewRecordsToKeep
                    )
                    else -> stringResource(
                        R.string.weekly_settings_prune_many,
                        result.deletedCount,
                        weeklyReviewRecordsToKeep
                    )
                }
                WeeklyCleanupResult.Error -> stringResource(R.string.weekly_settings_prune_error)
            }
            AlertDialog(
                onDismissRequest = { weeklyReviewCleanupResult = null },
                title = { Text(stringResource(R.string.weekly_settings_cleanup_result_title)) },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { weeklyReviewCleanupResult = null }) {
                        Text(stringResource(R.string.settings_close))
                    }
                }
            )
        }

        if (showPresencePhraseDialog) {
            AlertDialog(
                onDismissRequest = { showPresencePhraseDialog = false },
                title = { Text(stringResource(R.string.settings_presence_quote_dialog)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.settings_presence_quote_dialog_description))
                        OutlinedTextField(
                            value = presencePhraseInput,
                            onValueChange = { presencePhraseInput = it },
                            label = { Text(stringResource(R.string.settings_custom_quote)) },
                            singleLine = false,
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val next = presencePhraseInput.trim().ifBlank {
                            PresenceSettings.DEFAULT_CELEBRATION_PHRASE
                        }
                        presenceCelebrationPhrase = next
                        presencePhraseInput = next
                        prefs.edit {
                            putString(PresenceSettings.CUSTOM_CELEBRATION_PHRASE_KEY, next)
                        }
                        showPresencePhraseDialog = false
                        Toast.makeText(context, context.getString(R.string.settings_presence_quote_saved), Toast.LENGTH_SHORT).show()
                    }) {
                        Text(stringResource(R.string.settings_save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPresencePhraseDialog = false }) {
                        Text(stringResource(R.string.settings_cancel))
                    }
                }
            )
        }

        if (showCardioPhrasesDialog) {
            AlertDialog(
                onDismissRequest = { showCardioPhrasesDialog = false },
                title = { Text(stringResource(R.string.settings_coherence_quotes_dialog)) },
                text = {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 520.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val phaseTitles = listOf(
                            stringResource(R.string.settings_coherence_phase_regulation),
                            stringResource(R.string.settings_coherence_phase_heart_connection),
                            stringResource(R.string.settings_coherence_phase_elevated_emotion),
                            stringResource(R.string.settings_coherence_phase_integration)
                        )
                        phaseTitles.forEachIndexed { phaseIndex, title ->
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            repeat(2) { phraseOffset ->
                                val phraseIndex = (phaseIndex * 2) + phraseOffset
                                OutlinedTextField(
                                    value = cardioSessionPhrases[phraseIndex],
                                    onValueChange = { value ->
                                        val updated = cardioSessionPhrases.toMutableList()
                                        updated[phraseIndex] = value.take(
                                            CardioCoherencePreferences.MAX_SESSION_PHRASE_LENGTH
                                        )
                                        cardioSessionPhrases = updated
                                    },
                                    label = { Text(stringResource(R.string.settings_quote_number, phraseOffset + 1)) },
                                    supportingText = {
                                        Text(
                                            "${cardioSessionPhrases[phraseIndex].length}/" +
                                                CardioCoherencePreferences.MAX_SESSION_PHRASE_LENGTH
                                        )
                                    },
                                    minLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        CardioCoherencePreferences.saveSessionPhrases(context, cardioSessionPhrases)
                        cardioSessionPhrases = CardioCoherencePreferences.loadSessionPhrases(context)
                        showCardioPhrasesDialog = false
                        Toast.makeText(
                            context,
                            context.getString(R.string.settings_coherence_quotes_saved),
                            Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Text(stringResource(R.string.settings_save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCardioPhrasesDialog = false }) {
                        Text(stringResource(R.string.settings_cancel))
                    }
                }
            )
        }

        if (showJournalTimeDialog) {
            DisposableEffect(Unit) {
                val dialog = TimePickerDialog(
                    context,
                    { _, selectedHour, selectedMinute ->
                        val updated = JournalDailyReminderManager.setTime(context, selectedHour, selectedMinute)
                        journalReminderEnabled = updated.enabled
                        journalReminderHour = updated.hour
                        journalReminderMinute = updated.minute
                        journalReminderCustomMessage = updated.customMessage
                    },
                    journalReminderHour,
                    journalReminderMinute,
                    true
                )
                dialog.setOnDismissListener { showJournalTimeDialog = false }
                dialog.show()
                onDispose { dialog.dismiss() }
            }
        }

        if (showJournalMessageDialog) {
            AlertDialog(
                onDismissRequest = { showJournalMessageDialog = false },
                title = { Text(stringResource(R.string.settings_reminder_text_dialog)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.settings_reminder_text_hint))
                        OutlinedTextField(
                            value = journalMessageInput,
                            onValueChange = { journalMessageInput = it },
                            label = { Text(stringResource(R.string.settings_optional_text)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val updated = JournalDailyReminderManager.setCustomMessage(context, journalMessageInput)
                        journalReminderEnabled = updated.enabled
                        journalReminderHour = updated.hour
                        journalReminderMinute = updated.minute
                        journalReminderCustomMessage = updated.customMessage
                        showJournalMessageDialog = false
                    }) {
                        Text(stringResource(R.string.settings_save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showJournalMessageDialog = false }) {
                        Text(stringResource(R.string.settings_cancel))
                    }
                }
            )
        }

    }

    @Composable
    private fun SettingSection(
        title: String,
        subtitle: String,
        content: @Composable ColumnScope.() -> Unit
    ) {
        val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        val sectionTitleColor = if (isDarkTheme) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurface

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = sectionTitleColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider()
                content()
            }
        }
    }

    @Composable
    private fun SwitchField(
        title: String,
        description: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFCDDC39)
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }

    @Composable
    private fun SliderField(
        title: String,
        description: String,
        value: Int,
        range: IntRange,
        onValueChange: (Int) -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt().coerceIn(range.first, range.last)) },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                steps = (range.last - range.first - 1).coerceAtLeast(0)
            )
        }
    }

    @Composable
    private fun StepValueField(
        title: String,
        description: String,
        value: Int,
        minValue: Int,
        maxValue: Int,
        step: Int,
        onValueChange: (Int) -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    enabled = value > minValue,
                    onClick = { onValueChange((value - step).coerceAtLeast(minValue)) }
                ) {
                    Text("−", fontSize = 24.sp)
                }
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                TextButton(
                    enabled = value < maxValue,
                    onClick = { onValueChange((value + step).coerceAtMost(maxValue)) }
                ) {
                    Text("+", fontSize = 24.sp)
                }
            }
        }
    }

    @Composable
    private fun ActionField(
        title: String,
        description: String,
        onClick: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFD9EA42)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    @Composable
    private fun ActiveFilterChip(
        label: String,
        onRemove: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(999.dp)
                )
                .clickable(onClick = onRemove)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp
            )
            Text(
                text = "x",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    @Composable
    private fun FieldDivider(padding: androidx.compose.ui.unit.Dp = 6.dp) {
        HorizontalDivider(modifier = Modifier.padding(vertical = padding))
    }

    private fun authenticateForNotesLockChange(onSuccess: () -> Unit) {
        val context = context ?: return
        val canAuth = BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(
                context,
                getString(R.string.settings_biometric_unavailable),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        runCatching {
            val executor = ContextCompat.getMainExecutor(context)
            val prompt = BiometricPrompt(
                this,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        Toast.makeText(requireContext(), errString, Toast.LENGTH_SHORT).show()
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.settings_disable_notes_lock_title))
                .setSubtitle(getString(R.string.settings_disable_notes_lock_authentication))
                .setNegativeButtonText(getString(R.string.settings_cancel))
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
                .build()

            prompt.authenticate(promptInfo)
        }.onFailure { error ->
            Toast.makeText(
                requireContext(),
                error.message ?: getString(R.string.settings_biometric_start_error),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun authenticateForWirelessSync(
        title: String,
        subtitle: String,
        onSuccess: () -> Unit
    ) {
        val context = context ?: return
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val canAuth = BiometricManager.from(context).canAuthenticate(authenticators)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(
                context,
                getString(R.string.settings_wireless_sync_authentication_required),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        runCatching {
            val executor = ContextCompat.getMainExecutor(context)
            val prompt = BiometricPrompt(
                this,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        Toast.makeText(requireContext(), errString, Toast.LENGTH_SHORT).show()
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(authenticators)
                .build()

            prompt.authenticate(promptInfo)
        }.onFailure { error ->
            Toast.makeText(
                requireContext(),
                error.message ?: getString(R.string.settings_authentication_start_error),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
