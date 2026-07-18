package com.ypg.neville.ui.frag

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import androidx.lifecycle.lifecycleScope
import com.ypg.neville.model.preferences.DbPreferences
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.backup.CloudBackupManager
import com.ypg.neville.model.migration.ImportPolicy
import com.ypg.neville.model.migration.ImportPreview
import com.ypg.neville.model.migration.MigrationFormat
import com.ypg.neville.model.migration.MyAppMigrationService
import com.ypg.neville.model.reminders.JournalDailyReminderManager
import com.ypg.neville.model.subscription.SubscriptionManager
import com.ypg.neville.model.utils.ColorPickerManager
import com.ypg.neville.model.utils.NewsContent
import com.ypg.neville.model.utils.UiModalWindows
import kotlinx.coroutines.launch

class frag_Setting : Fragment() {

    private lateinit var pickProviderFolderLauncher: ActivityResultLauncher<Uri?>
    private lateinit var createProviderBackupFileLauncher: ActivityResultLauncher<String>
    private lateinit var pickBackupFileLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var createMigrationExportFileLauncher: ActivityResultLauncher<String>
    private lateinit var pickMigrationImportFileLauncher: ActivityResultLauncher<Array<String>>
    private var settingsUiRefreshTick by mutableStateOf(0)
    private var pendingRestorePassphrase: String? = null
    private var pendingMigrationPassphrase: CharArray? = null
    private var migrationImportPreview: ImportPreview? by mutableStateOf(null)
    private var migrationStatusMessage: String? by mutableStateOf(null)
    private var migrationResultDialogMessage: String? by mutableStateOf(null)
    private var migrationResultIsError by mutableStateOf(false)
    private var recoveredPassphraseMessage: String? by mutableStateOf(null)
    private val notesBiometricLockPrefKey = "notes_biometric_lock_enabled"
    private val defaultBackupFileName = "neville_backup_latest.nvbak"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pickProviderFolderLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null) {
                return@registerForActivityResult
            }

            val manager = CloudBackupManager(requireContext().applicationContext)
            val result = manager.connectProvider(uri)
            result.onSuccess { provider ->
                Toast.makeText(
                    requireContext(),
                    getString(R.string.settings_provider_connected_toast, provider.displayName),
                    Toast.LENGTH_SHORT
                ).show()
                settingsUiRefreshTick++
            }.onFailure { error ->
                Toast.makeText(
                    requireContext(),
                    error.message ?: getString(R.string.settings_provider_connect_error),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        createProviderBackupFileLauncher = registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/octet-stream")
        ) { uri ->
            if (uri == null) {
                return@registerForActivityResult
            }

            val manager = CloudBackupManager(requireContext().applicationContext)
            val result = manager.connectProvider(uri)
            result.onSuccess { provider ->
                Toast.makeText(
                    requireContext(),
                    getString(R.string.settings_provider_connected_toast, provider.displayName),
                    Toast.LENGTH_SHORT
                ).show()
                settingsUiRefreshTick++
            }.onFailure { error ->
                Toast.makeText(
                    requireContext(),
                    error.message ?: getString(R.string.settings_provider_connect_error),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        pickBackupFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) {
                pendingRestorePassphrase = null
                return@registerForActivityResult
            }

            lifecycleScope.launch {
                val manager = CloudBackupManager(requireContext().applicationContext)
                val passphrase = pendingRestorePassphrase
                pendingRestorePassphrase = null
                when (val result = manager.restoreFromBackup(uri, passphrase)) {
                    is CloudBackupManager.RestoreResult.Success -> {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.settings_restore_complete),
                            Toast.LENGTH_LONG
                        ).show()
                        settingsUiRefreshTick++
                    }
                    is CloudBackupManager.RestoreResult.Error -> {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.settings_restore_error, result.reason),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        createMigrationExportFileLauncher = registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/octet-stream")
        ) { uri ->
            val passphrase = pendingMigrationPassphrase
            pendingMigrationPassphrase = null
            if (uri == null || passphrase == null) {
                passphrase?.fill('\u0000')
                migrationStatusMessage = null
                migrationResultDialogMessage = null
                migrationImportPreview = null
                return@registerForActivityResult
            }

            lifecycleScope.launch {
                val result = MyAppMigrationService(requireContext().applicationContext).exportToUri(uri, passphrase)
                passphrase.fill('\u0000')
                result.onSuccess { export ->
                    migrationStatusMessage = buildExportSummary(export.countsByType)
                    migrationResultDialogMessage = migrationStatusMessage
                    migrationResultIsError = false
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.settings_migration_file_created, MigrationFormat.FILE_EXTENSION),
                        Toast.LENGTH_LONG
                    ).show()
                }.onFailure { error ->
                    migrationStatusMessage = getString(
                        R.string.settings_migration_export_error,
                        error.message ?: getString(R.string.settings_unknown_error)
                    )
                    migrationResultDialogMessage = migrationStatusMessage
                    migrationResultIsError = true
                    Toast.makeText(requireContext(), migrationStatusMessage, Toast.LENGTH_LONG).show()
                }
            }
        }

        pickMigrationImportFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val passphrase = pendingMigrationPassphrase
            pendingMigrationPassphrase = null
            if (uri == null || passphrase == null) {
                passphrase?.fill('\u0000')
                migrationStatusMessage = null
                migrationResultDialogMessage = null
                migrationImportPreview = null
                return@registerForActivityResult
            }

            lifecycleScope.launch {
                val result = MyAppMigrationService(requireContext().applicationContext).previewFromUri(uri, passphrase)
                passphrase.fill('\u0000')
                result.onSuccess { preview ->
                    migrationImportPreview = preview
                    migrationStatusMessage = buildPreviewSummary(preview)
                }.onFailure { error ->
                    migrationImportPreview = null
                    migrationStatusMessage = getString(
                        R.string.settings_migration_read_error,
                        error.message ?: getString(R.string.settings_invalid_password_or_file)
                    )
                    migrationResultDialogMessage = migrationStatusMessage
                    migrationResultIsError = true
                    Toast.makeText(requireContext(), migrationStatusMessage, Toast.LENGTH_LONG).show()
                }
            }
        }

    }

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
        val backupManager = remember { CloudBackupManager(context.applicationContext) }
        val coroutineScope = rememberCoroutineScope()
        val refreshTick = settingsUiRefreshTick
        val initialJournalConfig = remember { JournalDailyReminderManager.readConfig(context) }

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

        var showFrequencyDialog by remember { mutableStateOf(false) }
        var showProviderDestinationDialog by remember { mutableStateOf(false) }
        var showRestoreDialog by remember { mutableStateOf(false) }
        var showBackupWarningDialog by remember { mutableStateOf(false) }
        var showJournalTimeDialog by remember { mutableStateOf(false) }
        var showJournalMessageDialog by remember { mutableStateOf(false) }
        var showPresencePhraseDialog by remember { mutableStateOf(false) }
        var showCardioPhrasesDialog by remember { mutableStateOf(false) }
        var showPassphraseDialog by remember { mutableStateOf(false) }
        var showDeletePassphraseDialog by remember { mutableStateOf(false) }
        var showRecoveryGuideDialog by remember { mutableStateOf(false) }
        var journalMessageInput by remember { mutableStateOf(journalReminderCustomMessage) }
        var currentPassphraseInput by remember { mutableStateOf("") }
        var deleteCurrentPassphraseInput by remember { mutableStateOf("") }
        var passphraseInput by remember { mutableStateOf("") }
        var passphraseConfirmInput by remember { mutableStateOf("") }
        var restorePassphraseInput by remember { mutableStateOf("") }
        var migrationExportPassphraseInput by remember { mutableStateOf("") }
        var migrationImportPassphraseInput by remember { mutableStateOf("") }
        var showPassphrasePlainText by remember { mutableStateOf(false) }
        var showMigrationExportDialog by remember { mutableStateOf(false) }
        var showMigrationImportDialog by remember { mutableStateOf(false) }

        val providerInfo = remember(refreshTick) { backupManager.getProviderInfo() }
        val backupFrequency = remember(refreshTick) { backupManager.getFrequency() }
        val lastBackupAt = remember(refreshTick) { backupManager.getLastBackupTimestamp() }
        val lastBackupError = remember(refreshTick) { backupManager.getLastBackupError() }
        val hasSavedPassphrase = remember(refreshTick) { backupManager.hasSavedPassphrase() }
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
                            (activity as? MainActivity)?.showSubscriptionPaywall()
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
                    title = stringResource(R.string.settings_backup_title),
                    subtitle = stringResource(R.string.settings_backup_subtitle)
                ) {
                    Text(
                        text = stringResource(R.string.settings_encryption_active),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF9800)
                    )
                    FieldDivider()
                    ActionField(
                        title = if (providerInfo == null) stringResource(R.string.settings_connect_provider) else stringResource(R.string.settings_provider_connected),
                        description = providerInfo?.let {
                            "${it.displayName} (${it.authority}) · ${it.destinationKind.toUiLabel(context)}"
                        } ?: stringResource(R.string.settings_provider_description)
                    ) {
                        showProviderDestinationDialog = true
                    }
                    if (providerInfo != null) {
                        FieldDivider()
                        ActionField(
                            title = stringResource(R.string.settings_disconnect_provider),
                            description = stringResource(R.string.settings_disconnect_provider_description)
                        ) {
                            backupManager.disconnectProvider()
                            settingsUiRefreshTick++
                            Toast.makeText(context, context.getString(R.string.settings_provider_disconnected), Toast.LENGTH_SHORT).show()
                        }
                    }

                    FieldDivider()
                    ActionField(
                        title = if (hasSavedPassphrase) stringResource(R.string.settings_update_encryption_key) else stringResource(R.string.settings_configure_encryption_key),
                        description = if (hasSavedPassphrase) {
                            stringResource(R.string.settings_key_protects_description)
                        } else {
                            stringResource(R.string.settings_key_required_description)
                        }
                    ) {
                        currentPassphraseInput = ""
                        passphraseInput = ""
                        passphraseConfirmInput = ""
                        showPassphrasePlainText = false
                        showPassphraseDialog = true
                    }

                    if (hasSavedPassphrase) {
                        FieldDivider()
                        ActionField(
                            title = stringResource(R.string.settings_delete_saved_key),
                            description = stringResource(R.string.settings_current_key_required)
                        ) {
                            deleteCurrentPassphraseInput = ""
                            showDeletePassphraseDialog = true
                        }
                    }

                    if (hasSavedPassphrase) {
                        FieldDivider()
                        ActionField(
                            title = stringResource(R.string.settings_recover_password_biometrics),
                            description = stringResource(R.string.settings_recover_password_biometrics_description)
                        ) {
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
                                return@ActionField
                            }
                            launchBiometricRecovery()
                        }
                    }

                    FieldDivider()
                    ActionField(
                        title = stringResource(R.string.settings_recovery_guide),
                        description = stringResource(R.string.settings_recovery_guide_description)
                    ) {
                        showRecoveryGuideDialog = true
                    }

                    FieldDivider()
                    ActionField(
                        title = stringResource(R.string.settings_backup_frequency),
                        description = backupFrequency.toUiLabel(context)
                    ) {
                        showFrequencyDialog = true
                    }

                    FieldDivider()
                    ActionField(
                        title = stringResource(R.string.settings_backup_now),
                        description = if (hasSavedPassphrase) {
                            stringResource(R.string.settings_backup_now_description)
                        } else {
                            stringResource(R.string.settings_backup_key_required_action)
                        }
                    ) {
                        if (!hasSavedPassphrase) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.settings_configure_key_first),
                                Toast.LENGTH_LONG
                            ).show()
                            return@ActionField
                        }
                        showBackupWarningDialog = true
                    }

                    FieldDivider()
                    ActionField(
                        title = stringResource(R.string.settings_restore_from_backup),
                        description = stringResource(R.string.settings_restore_from_backup_description)
                    ) {
                        showRestoreDialog = true
                    }

                    FieldDivider(padding = 8.dp)
                    Text(
                        text = stringResource(R.string.settings_last_backup, backupManager.formatTimestamp(lastBackupAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!lastBackupError.isNullOrBlank()) {
                        Text(
                            text = stringResource(R.string.settings_last_error, lastBackupError),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            item {
                SettingSection(
                    title = stringResource(R.string.settings_migration_title),
                    subtitle = stringResource(R.string.settings_migration_subtitle)
                ) {
                    ActionField(
                        title = stringResource(R.string.settings_export_to_ios),
                        description = stringResource(R.string.settings_export_to_ios_description, MigrationFormat.FILE_EXTENSION)
                    ) {
                        authenticateForMigration(
                            title = context.getString(R.string.settings_export_data),
                            subtitle = context.getString(R.string.settings_export_authentication)
                        ) {
                            migrationStatusMessage = null
                            migrationResultDialogMessage = null
                            migrationImportPreview = null
                            migrationImportPassphraseInput = ""
                            migrationExportPassphraseInput = ""
                            showMigrationExportDialog = true
                        }
                    }
                    FieldDivider()
                    ActionField(
                        title = stringResource(R.string.settings_import_from_ios),
                        description = stringResource(R.string.settings_import_from_ios_description, MigrationFormat.FILE_EXTENSION)
                    ) {
                        authenticateForMigration(
                            title = context.getString(R.string.settings_import_data),
                            subtitle = context.getString(R.string.settings_import_authentication)
                        ) {
                            migrationStatusMessage = null
                            migrationResultDialogMessage = null
                            migrationImportPreview = null
                            migrationExportPassphraseInput = ""
                            migrationImportPassphraseInput = ""
                            showMigrationImportDialog = true
                        }
                    }
                    migrationStatusMessage?.takeIf { it.isNotBlank() }?.let { message ->
                        FieldDivider(padding = 8.dp)
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

        if (showMigrationExportDialog) {
            AlertDialog(
                onDismissRequest = {
                    migrationExportPassphraseInput = ""
                    pendingMigrationPassphrase = null
                    showMigrationExportDialog = false
                },
                title = { Text(stringResource(R.string.settings_export_to_ios_dialog)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.settings_migration_password_hint))
                        OutlinedTextField(
                            value = migrationExportPassphraseInput,
                            onValueChange = { migrationExportPassphraseInput = it },
                            label = { Text(stringResource(R.string.settings_export_password)) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val passphrase = migrationExportPassphraseInput.toCharArray()
                        if (passphrase.isEmpty()) {
                            Toast.makeText(context, context.getString(R.string.settings_enter_password), Toast.LENGTH_LONG).show()
                            return@TextButton
                        }
                        pendingMigrationPassphrase = passphrase
                        migrationExportPassphraseInput = ""
                        showMigrationExportDialog = false
                        val fileName = "neville-${System.currentTimeMillis()}${MigrationFormat.FILE_EXTENSION}"
                        createMigrationExportFileLauncher.launch(fileName)
                    }) {
                        Text(stringResource(R.string.settings_create_file))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        migrationExportPassphraseInput = ""
                        pendingMigrationPassphrase = null
                        showMigrationExportDialog = false
                    }) {
                        Text(stringResource(R.string.settings_cancel))
                    }
                }
            )
        }

        if (showMigrationImportDialog) {
            AlertDialog(
                onDismissRequest = {
                    migrationImportPassphraseInput = ""
                    pendingMigrationPassphrase = null
                    showMigrationImportDialog = false
                },
                title = { Text(stringResource(R.string.settings_import_from_ios_dialog)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.settings_import_memory_only_hint))
                        OutlinedTextField(
                            value = migrationImportPassphraseInput,
                            onValueChange = { migrationImportPassphraseInput = it },
                            label = { Text(stringResource(R.string.settings_file_password)) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val passphrase = migrationImportPassphraseInput.toCharArray()
                        if (passphrase.isEmpty()) {
                            Toast.makeText(context, context.getString(R.string.settings_enter_the_password), Toast.LENGTH_LONG).show()
                            return@TextButton
                        }
                        pendingMigrationPassphrase = passphrase
                        migrationImportPassphraseInput = ""
                        showMigrationImportDialog = false
                        pickMigrationImportFileLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                    }) {
                        Text(stringResource(R.string.settings_select_file))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        migrationImportPassphraseInput = ""
                        pendingMigrationPassphrase = null
                        showMigrationImportDialog = false
                    }) {
                        Text(stringResource(R.string.settings_cancel))
                    }
                }
            )
        }

        migrationImportPreview?.let { preview ->
            AlertDialog(
                onDismissRequest = {
                    migrationImportPreview = null
                    migrationStatusMessage = null
                    migrationExportPassphraseInput = ""
                    migrationImportPassphraseInput = ""
                    pendingMigrationPassphrase = null
                },
                title = { Text(stringResource(R.string.settings_import_preview)) },
                text = {
                    Column(
                        modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(stringResource(R.string.settings_import_source, preview.manifest.optString("sourcePlatform"), preview.records.size))
                        Text(stringResource(R.string.settings_items, migrationCountsInline(preview.countsByType)))
                        if (preview.conflicts.isNotEmpty()) {
                            Text(
                                stringResource(R.string.settings_conflicts_skipped, preview.conflicts.size),
                                color = MaterialTheme.colorScheme.error
                            )
                            migrationConflictSummaryLines(preview).forEach { line ->
                                Text(line, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (preview.errors.isNotEmpty()) {
                            Text(stringResource(R.string.settings_errors_count, preview.errors.size), color = MaterialTheme.colorScheme.error)
                            preview.errors.take(5).forEach { error ->
                                Text(error, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = preview.errors.isEmpty(),
                        onClick = {
                            coroutineScope.launch {
                                val result = MyAppMigrationService(context.applicationContext)
                                    .importPreview(preview, ImportPolicy.SkipExisting)
                                result.onSuccess { summary ->
                                    migrationStatusMessage = buildImportSummary(summary)
                                    migrationResultDialogMessage = migrationStatusMessage
                                    migrationResultIsError = false
                                    migrationImportPreview = null
                                    Toast.makeText(context, context.getString(R.string.settings_import_complete), Toast.LENGTH_LONG).show()
                                }.onFailure { error ->
                                    migrationStatusMessage = context.getString(
                                        R.string.settings_import_error,
                                        error.message ?: context.getString(R.string.settings_unknown_error)
                                    )
                                    migrationResultDialogMessage = migrationStatusMessage
                                    migrationResultIsError = true
                                    Toast.makeText(context, migrationStatusMessage, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.settings_import_skipping_conflicts))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        migrationImportPreview = null
                        migrationStatusMessage = null
                        migrationExportPassphraseInput = ""
                        migrationImportPassphraseInput = ""
                        pendingMigrationPassphrase = null
                    }) {
                        Text(stringResource(R.string.settings_cancel))
                    }
                }
            )
        }

        migrationResultDialogMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { migrationResultDialogMessage = null },
                title = {
                    Text(
                        if (migrationResultIsError) {
                            stringResource(R.string.settings_migration_failed)
                        } else {
                            stringResource(R.string.settings_migration_summary)
                        }
                    )
                },
                text = {
                    Text(message)
                },
                confirmButton = {
                    TextButton(onClick = { migrationResultDialogMessage = null }) {
                        Text(stringResource(R.string.settings_close))
                    }
                }
            )
        }

        if (showBackupWarningDialog) {
            AlertDialog(
                onDismissRequest = { showBackupWarningDialog = false },
                title = { Text(stringResource(R.string.settings_backup_recovery_key)) },
                text = {
                    Text(
                        stringResource(R.string.settings_backup_recovery_warning)
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showBackupWarningDialog = false
                        coroutineScope.launch {
                            when (val result = backupManager.backupNow()) {
                                is CloudBackupManager.BackupResult.Success -> {
                                    settingsUiRefreshTick++
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.settings_backup_created, result.fileName),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                is CloudBackupManager.BackupResult.Error -> {
                                    settingsUiRefreshTick++
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.settings_backup_error, result.reason),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }) {
                        Text(stringResource(R.string.settings_create_backup))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBackupWarningDialog = false }) {
                        Text(stringResource(R.string.settings_cancel))
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

        if (showFrequencyDialog) {
            AlertDialog(
                onDismissRequest = { showFrequencyDialog = false },
                title = { Text(stringResource(R.string.settings_backup_frequency_dialog)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FrequencyOption(
                            title = stringResource(R.string.settings_frequency_manual),
                            selected = backupFrequency == CloudBackupManager.BackupFrequency.MANUAL
                        ) {
                            backupManager.setFrequency(CloudBackupManager.BackupFrequency.MANUAL)
                                .onSuccess {
                                    settingsUiRefreshTick++
                                    showFrequencyDialog = false
                                }
                                .onFailure { error ->
                                    Toast.makeText(
                                        context,
                                        error.message ?: context.getString(R.string.settings_frequency_save_error),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                        FrequencyOption(
                            title = stringResource(R.string.settings_frequency_daily),
                            selected = backupFrequency == CloudBackupManager.BackupFrequency.DAILY
                        ) {
                            backupManager.setFrequency(CloudBackupManager.BackupFrequency.DAILY)
                                .onSuccess {
                                    settingsUiRefreshTick++
                                    showFrequencyDialog = false
                                }
                                .onFailure { error ->
                                    Toast.makeText(
                                        context,
                                        error.message ?: context.getString(R.string.settings_frequency_save_error),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                        FrequencyOption(
                            title = stringResource(R.string.settings_frequency_weekly),
                            selected = backupFrequency == CloudBackupManager.BackupFrequency.WEEKLY
                        ) {
                            backupManager.setFrequency(CloudBackupManager.BackupFrequency.WEEKLY)
                                .onSuccess {
                                    settingsUiRefreshTick++
                                    showFrequencyDialog = false
                                }
                                .onFailure { error ->
                                    Toast.makeText(
                                        context,
                                        error.message ?: context.getString(R.string.settings_frequency_save_error),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFrequencyDialog = false }) {
                        Text(stringResource(R.string.settings_close))
                    }
                }
            )
        }

        if (showProviderDestinationDialog) {
            AlertDialog(
                onDismissRequest = { showProviderDestinationDialog = false },
                title = { Text(stringResource(R.string.settings_connect_provider_dialog)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.settings_provider_destination_prompt))
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                showProviderDestinationDialog = false
                                pickProviderFolderLauncher.launch(null)
                            }
                        ) {
                            Text(stringResource(R.string.settings_provider_folder_option))
                        }
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                showProviderDestinationDialog = false
                                createProviderBackupFileLauncher.launch(defaultBackupFileName)
                            }
                        ) {
                            Text(stringResource(R.string.settings_provider_cloud_file_option))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showProviderDestinationDialog = false }) {
                        Text(stringResource(R.string.settings_close))
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

        if (showRestoreDialog) {
            AlertDialog(
                onDismissRequest = { showRestoreDialog = false },
                title = { Text(stringResource(R.string.settings_restore_database_dialog)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.settings_restore_database_warning))
                        OutlinedTextField(
                            value = restorePassphraseInput,
                            onValueChange = { restorePassphraseInput = it },
                            label = { Text(stringResource(R.string.settings_encryption_key)) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showRestoreDialog = false
                        pendingRestorePassphrase = restorePassphraseInput.trim().ifBlank { null }
                        restorePassphraseInput = ""
                        pickBackupFileLauncher.launch(arrayOf("application/octet-stream", "application/zip", "*/*"))
                    }) {
                        Text(stringResource(R.string.settings_continue))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        restorePassphraseInput = ""
                        showRestoreDialog = false
                    }) {
                        Text(stringResource(R.string.settings_cancel))
                    }
                }
            )
        }

        if (showPassphraseDialog) {
            AlertDialog(
                onDismissRequest = { showPassphraseDialog = false },
                title = {
                    Text(
                        if (hasSavedPassphrase) {
                            stringResource(R.string.settings_update_encryption_key_dialog)
                        } else {
                            stringResource(R.string.settings_backup_encryption_key_dialog)
                        }
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.settings_encryption_key_hint))
                        SwitchField(
                            title = stringResource(R.string.settings_show_password),
                            description = stringResource(R.string.settings_show_password_description),
                            checked = showPassphrasePlainText,
                            onCheckedChange = { showPassphrasePlainText = it }
                        )
                        if (hasSavedPassphrase) {
                            OutlinedTextField(
                                value = currentPassphraseInput,
                                onValueChange = { currentPassphraseInput = it },
                                label = { Text(stringResource(R.string.settings_current_key)) },
                                visualTransformation = if (showPassphrasePlainText) {
                                    androidx.compose.ui.text.input.VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true
                            )
                        }
                        OutlinedTextField(
                            value = passphraseInput,
                            onValueChange = { passphraseInput = it },
                            label = { Text(stringResource(R.string.settings_new_key)) },
                            visualTransformation = if (showPassphrasePlainText) {
                                androidx.compose.ui.text.input.VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = passphraseConfirmInput,
                            onValueChange = { passphraseConfirmInput = it },
                            label = { Text(stringResource(R.string.settings_confirm_key)) },
                            visualTransformation = if (showPassphrasePlainText) {
                                androidx.compose.ui.text.input.VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val pass = passphraseInput.trim()
                        val passConfirm = passphraseConfirmInput.trim()
                        if (pass != passConfirm) {
                            Toast.makeText(context, context.getString(R.string.settings_keys_do_not_match), Toast.LENGTH_LONG).show()
                            return@TextButton
                        }
                        val result = if (hasSavedPassphrase) {
                            backupManager.updatePassphrase(
                                currentPassphrase = currentPassphraseInput.trim(),
                                newPassphrase = pass
                            )
                        } else {
                            backupManager.savePassphrase(pass)
                        }
                        result.onSuccess {
                            settingsUiRefreshTick++
                            showPassphraseDialog = false
                            currentPassphraseInput = ""
                            passphraseInput = ""
                            passphraseConfirmInput = ""
                            showPassphrasePlainText = false
                            Toast.makeText(context, context.getString(R.string.settings_key_saved), Toast.LENGTH_SHORT).show()
                        }.onFailure { error ->
                            Toast.makeText(context, error.message ?: context.getString(R.string.settings_key_save_error), Toast.LENGTH_LONG).show()
                        }
                    }) {
                        Text(stringResource(R.string.settings_save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        currentPassphraseInput = ""
                        passphraseInput = ""
                        passphraseConfirmInput = ""
                        showPassphrasePlainText = false
                        showPassphraseDialog = false
                    }) {
                        Text(stringResource(R.string.settings_cancel))
                    }
                }
            )
        }

        if (showDeletePassphraseDialog) {
            AlertDialog(
                onDismissRequest = { showDeletePassphraseDialog = false },
                title = { Text(stringResource(R.string.settings_delete_encryption_key_dialog)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.settings_delete_encryption_key_hint))
                        OutlinedTextField(
                            value = deleteCurrentPassphraseInput,
                            onValueChange = { deleteCurrentPassphraseInput = it },
                            label = { Text(stringResource(R.string.settings_current_key)) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        backupManager.deletePassphrase(deleteCurrentPassphraseInput.trim())
                            .onSuccess {
                                settingsUiRefreshTick++
                                deleteCurrentPassphraseInput = ""
                                showDeletePassphraseDialog = false
                                Toast.makeText(context, context.getString(R.string.settings_key_deleted), Toast.LENGTH_SHORT).show()
                            }
                            .onFailure { error ->
                                Toast.makeText(
                                    context,
                                    error.message ?: context.getString(R.string.settings_key_delete_error),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }) { Text(stringResource(R.string.settings_delete)) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        deleteCurrentPassphraseInput = ""
                        showDeletePassphraseDialog = false
                    }) { Text(stringResource(R.string.settings_cancel)) }
                }
            )
        }

        if (!recoveredPassphraseMessage.isNullOrBlank()) {
            AlertDialog(
                onDismissRequest = { recoveredPassphraseMessage = null },
                title = { Text(stringResource(R.string.settings_encryption_password_dialog)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.settings_biometric_recovery_success))
                        Text(
                            text = recoveredPassphraseMessage.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { recoveredPassphraseMessage = null }) {
                        Text(stringResource(R.string.settings_close))
                    }
                }
            )
        }

        if (showRecoveryGuideDialog) {
            AlertDialog(
                onDismissRequest = { showRecoveryGuideDialog = false },
                title = { Text(stringResource(R.string.settings_recovery_guide_dialog)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.settings_recovery_guide_step_1))
                        Text(stringResource(R.string.settings_recovery_guide_step_2))
                        Text(stringResource(R.string.settings_recovery_guide_step_3))
                        Text(stringResource(R.string.settings_recovery_guide_step_4))
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showRecoveryGuideDialog = false }) {
                        Text(stringResource(R.string.settings_understood))
                    }
                }
            )
        }
    }

    @Composable
    private fun FrequencyOption(
        title: String,
        selected: Boolean,
        onClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (selected) stringResource(R.string.settings_selected) else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    private fun CloudBackupManager.ProviderDestinationKind.toUiLabel(context: android.content.Context): String {
        return when (this) {
            CloudBackupManager.ProviderDestinationKind.TREE -> context.getString(R.string.settings_destination_folder)
            CloudBackupManager.ProviderDestinationKind.DOCUMENT -> context.getString(R.string.settings_destination_file)
        }
    }

    private fun buildExportSummary(countsByType: Map<String, Int>): String {
        val total = countsByType.values.sum()
        return buildString {
            appendLine(getString(R.string.settings_export_complete))
            appendLine(getString(R.string.settings_total_exported, total))
            appendMigrationCounts(countsByType)
        }.trim()
    }

    private fun buildPreviewSummary(preview: ImportPreview): String {
        return buildString {
            appendLine(getString(R.string.settings_preview_ready))
            appendLine(getString(R.string.settings_total_detected, preview.records.size))
            appendMigrationCounts(preview.countsByType)
            if (preview.conflicts.isNotEmpty()) appendLine(getString(R.string.settings_conflicts_count, preview.conflicts.size))
            if (preview.errors.isNotEmpty()) appendLine(getString(R.string.settings_errors_count, preview.errors.size))
        }.trim()
    }

    private fun buildImportSummary(summary: com.ypg.neville.model.migration.MigrationSummary): String {
        return buildString {
            appendLine(getString(R.string.settings_import_complete))
            appendLine(getString(R.string.settings_inserted_count, summary.inserted))
            appendLine(getString(R.string.settings_updated_count, summary.updated))
            appendLine(getString(R.string.settings_skipped_count, summary.skipped))
            appendLine(getString(R.string.settings_conflicts_count, summary.conflicts))
            appendLine(getString(R.string.settings_errors_count, summary.errors))
        }.trim()
    }

    private fun StringBuilder.appendMigrationCounts(countsByType: Map<String, Int>) {
        if (countsByType.isEmpty()) {
            appendLine(getString(R.string.settings_no_items))
            return
        }
        appendLine(getString(R.string.settings_by_type))
        countsByType.toSortedMap().forEach { (type, count) ->
            appendLine("- ${migrationTypeLabel(type)}: $count")
        }
    }

    private fun migrationCountsInline(countsByType: Map<String, Int>): String {
        if (countsByType.isEmpty()) return getString(R.string.settings_no_items_lowercase)
        return countsByType.toSortedMap()
            .map { (type, count) -> "${migrationTypeLabel(type, count)}: $count" }
            .joinToString("; ")
    }

    private fun migrationConflictSummaryLines(preview: ImportPreview): List<String> {
        return preview.conflicts
            .groupingBy { it.type }
            .eachCount()
            .toSortedMap()
            .map { (type, count) ->
                getString(
                    if (count == 1) R.string.settings_existing_item_skipped_singular else R.string.settings_existing_item_skipped_plural,
                    migrationTypeLabel(type, count),
                    count
                )
            }
    }

    private fun migrationTypeLabel(type: String): String {
        return migrationTypeLabel(type, 1)
    }

    private fun migrationTypeLabel(type: String, count: Int): String {
        val singular = count == 1
        return when (type) {
            "note" -> getString(if (singular) R.string.settings_type_note else R.string.settings_type_notes)
            "diary_entry" -> getString(if (singular) R.string.settings_type_diary_entry else R.string.settings_type_diary_entries)
            "agenda_entry" -> getString(if (singular) R.string.settings_type_agenda_entry else R.string.settings_type_agenda_entries)
            "goal" -> getString(if (singular) R.string.settings_type_goal else R.string.settings_type_goals)
            "archived_goal" -> getString(if (singular) R.string.settings_type_archived_goal else R.string.settings_type_archived_goals)
            "personal_phrase" -> getString(if (singular) R.string.settings_type_personal_quote else R.string.settings_type_personal_quotes)
            "personal_reflection" -> getString(if (singular) R.string.settings_type_personal_reflection else R.string.settings_type_personal_reflections)
            "day_ritual_archive" -> getString(if (singular) R.string.settings_type_archived_ritual else R.string.settings_type_archived_rituals)
            "calm_personal_phrase" -> getString(if (singular) R.string.settings_type_calm_quote else R.string.settings_type_calm_quotes)
            else -> type
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

    private fun CloudBackupManager.BackupFrequency.toUiLabel(context: android.content.Context): String {
        return when (this) {
            CloudBackupManager.BackupFrequency.MANUAL -> context.getString(R.string.settings_frequency_manual_description)
            CloudBackupManager.BackupFrequency.DAILY -> context.getString(R.string.settings_frequency_daily)
            CloudBackupManager.BackupFrequency.WEEKLY -> context.getString(R.string.settings_frequency_weekly)
        }
    }

    private fun launchBiometricRecovery() {
        val context = context ?: return
        runCatching {
            val executor = ContextCompat.getMainExecutor(context)
            val prompt = BiometricPrompt(
                this,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        val manager = CloudBackupManager(requireContext().applicationContext)
                        manager.getPassphraseForBiometricRecovery()
                            .onSuccess { passphrase ->
                                recoveredPassphraseMessage = passphrase
                            }
                            .onFailure { error ->
                                Toast.makeText(
                                    requireContext(),
                                    error.message ?: getString(R.string.settings_key_recovery_error),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        Toast.makeText(requireContext(), errString, Toast.LENGTH_SHORT).show()
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.settings_key_recovery_title))
                .setSubtitle(getString(R.string.settings_key_recovery_authentication))
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

    private fun authenticateForMigration(
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
                getString(R.string.settings_migration_authentication_required),
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
