package com.ypg.neville.ui.frag

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.ypg.neville.MainActivity
import com.ypg.neville.R
import com.ypg.neville.model.backup.CloudBackupManager
import com.ypg.neville.model.db.utilsDB
import com.ypg.neville.model.utils.ColorPickerManager
import com.ypg.neville.model.utils.NewsContent
import com.ypg.neville.model.utils.UiModalWindows
import kotlinx.coroutines.launch

class frag_Setting : Fragment() {

    private lateinit var pickProviderFolderLauncher: ActivityResultLauncher<Uri?>
    private lateinit var pickBackupFileLauncher: ActivityResultLauncher<Array<String>>
    private var settingsUiRefreshTick by mutableStateOf(0)
    private var pendingRestorePassphrase: String? = null
    private var recoveredPassphraseMessage: String? by mutableStateOf(null)
    private val notesBiometricLockPrefKey = "notes_biometric_lock_enabled"

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
                    "Proveedor conectado: ${provider.displayName}",
                    Toast.LENGTH_SHORT
                ).show()
                settingsUiRefreshTick++
            }.onFailure { error ->
                Toast.makeText(
                    requireContext(),
                    error.message ?: "No se pudo conectar el proveedor",
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
                            "Restauración completada. Se recargará la app.",
                            Toast.LENGTH_LONG
                        ).show()
                        settingsUiRefreshTick++
                        activity?.recreate()
                    }
                    is CloudBackupManager.RestoreResult.Error -> {
                        Toast.makeText(
                            requireContext(),
                            "Error al restaurar: ${result.reason}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
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
        val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
        val backupManager = remember { CloudBackupManager(context.applicationContext) }
        val coroutineScope = rememberCoroutineScope()
        val refreshTick = settingsUiRefreshTick

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
        var notesBiometricLockEnabled by remember { mutableStateOf(prefs.getBoolean(notesBiometricLockPrefKey, false)) }

        var showFrequencyDialog by remember { mutableStateOf(false) }
        var showRestoreDialog by remember { mutableStateOf(false) }
        var showPassphraseDialog by remember { mutableStateOf(false) }
        var showDeletePassphraseDialog by remember { mutableStateOf(false) }
        var showRecoveryGuideDialog by remember { mutableStateOf(false) }
        var currentPassphraseInput by remember { mutableStateOf("") }
        var deleteCurrentPassphraseInput by remember { mutableStateOf("") }
        var passphraseInput by remember { mutableStateOf("") }
        var passphraseConfirmInput by remember { mutableStateOf("") }
        var restorePassphraseInput by remember { mutableStateOf("") }
        var showPassphrasePlainText by remember { mutableStateOf(false) }

        val providerInfo = remember(refreshTick) { backupManager.getProviderInfo() }
        val backupFrequency = remember(refreshTick) { backupManager.getFrequency() }
        val lastBackupAt = remember(refreshTick) { backupManager.getLastBackupTimestamp() }
        val lastBackupError = remember(refreshTick) { backupManager.getLastBackupError() }
        val hasSavedPassphrase = remember(refreshTick) { backupManager.hasSavedPassphrase() }

        fun isAnyFilterEnabled(
            nev: Boolean = filterAutorNeville,
            joe: Boolean = filterAutorJoe,
            gregg: Boolean = filterAutorGregg,
            bruce: Boolean = filterAutorBruce,
            otros: Boolean = filterOtros,
            salud: Boolean = filterSalud
        ): Boolean = nev || joe || gregg || bruce || otros || salud

        fun saveAuthorsAggregate() {
            prefs.edit {
                putBoolean(
                    "home_filter_autores",
                    filterAutorNeville || filterAutorJoe || filterAutorGregg || filterAutorBruce
                )
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
                        text = "Ajustes",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Personaliza apariencia, tamaños y contenido de inicio.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                SettingSection(
                    title = "Apariencia",
                    subtitle = "Tema y colores del contenido"
                ) {
                    SwitchField(
                        title = "Tema oscuro",
                        description = "Activa o desactiva el modo oscuro de la aplicación",
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
                        title = "Color de fuente en Inicio",
                        description = "Color del texto de frase en FragHome"
                    ) {
                        ColorPickerManager.showColorPicker(
                            context,
                            prefs.getInt("color_letra_frases_home", prefs.getInt("color_letra_frases", 0xFF1F2A37.toInt())),
                            "color_letra_frases_home",
                            "Color de fuente en Inicio"
                        )
                    }
                    FieldDivider()
                    ActionField(
                        title = "Color de Fondo A",
                        description = "Primer color del degradado en Inicio"
                    ) {
                        ColorPickerManager.showColorPicker(
                            context,
                            prefs.getInt("color_fondo_a", 0xFFF3F5F9.toInt()),
                            "color_fondo_a",
                            "Color de Fondo A"
                        )
                    }
                    FieldDivider()
                    ActionField(
                        title = "Color de Fondo B",
                        description = "Segundo color del degradado en Inicio"
                    ) {
                        ColorPickerManager.showColorPicker(
                            context,
                            prefs.getInt("color_fondo_b", 0xFFE2E7F0.toInt()),
                            "color_fondo_b",
                            "Color de Fondo B"
                        )
                    }
                }
            }

            item {
                SettingSection(
                    title = "Tamaño de Letra",
                    subtitle = "Ajuste independiente por tipo de contenido"
                ) {
                    SliderField(
                        title = "Texto de Frases",
                        description = "Inicio y vistas de autor",
                        value = fuenteFrase,
                        range = 14..40
                    ) {
                        fuenteFrase = it
                        prefs.edit { putString("fuente_frase", it.toString()) }
                    }
                    FieldDivider()
                    SliderField(
                        title = "Listados",
                        description = "Conferencias, Enciclopedia, Evidencia y Reflexiones",
                        value = fuenteListados,
                        range = 12..40
                    ) {
                        fuenteListados = it
                        prefs.edit { putString("fuente_listados", it.toString()) }
                    }
                    FieldDivider()
                    SliderField(
                        title = "Zoom de contenido",
                        description = "Visor interno de texto",
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
                    title = "Colores de lectura",
                    subtitle = "Fondo degradado y color de texto para contenido en texto"
                ) {
                    ActionField(
                        title = "Color de Fondo Lectura A",
                        description = "Primer color del degradado en FragContentWebView"
                    ) {
                        ColorPickerManager.showColorPicker(
                            context,
                            prefs.getInt("color_lectura_fondo_a", 0xFFF8F4EA.toInt()),
                            "color_lectura_fondo_a",
                            "Color de Fondo Lectura A"
                        )
                    }
                    FieldDivider()
                    ActionField(
                        title = "Color de Fondo Lectura B",
                        description = "Segundo color del degradado en FragContentWebView"
                    ) {
                        ColorPickerManager.showColorPicker(
                            context,
                            prefs.getInt("color_lectura_fondo_b", 0xFFECE3D3.toInt()),
                            "color_lectura_fondo_b",
                            "Color de Fondo Lectura B"
                        )
                    }
                    FieldDivider()
                    ActionField(
                        title = "Color de texto Lectura",
                        description = "Color principal del texto en el visor de contenido"
                    ) {
                        ColorPickerManager.showColorPicker(
                            context,
                            prefs.getInt("color_lectura_texto", 0xFF2B2115.toInt()),
                            "color_lectura_texto",
                            "Color de texto Lectura"
                        )
                    }
                }
            }

            item {
                SettingSection(
                    title = "Frases en Inicio",
                    subtitle = "Selecciona exactamente qué categorías y autores mostrar"
                ) {
                    Text(
                        text = "Autores",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    SwitchField(
                        title = "Neville Goddard",
                        description = "Mostrar frases de Neville en Inicio",
                        checked = filterAutorNeville
                    ) { newValue ->
                        if (!isAnyFilterEnabled(nev = newValue)) {
                            Toast.makeText(context, "Debe quedar al menos un filtro activo", Toast.LENGTH_SHORT).show()
                            return@SwitchField
                        }
                        filterAutorNeville = newValue
                        prefs.edit { putBoolean("home_filter_author_neville", newValue) }
                        saveAuthorsAggregate()
                    }
                    FieldDivider()
                    SwitchField(
                        title = "Joe Dispenza",
                        description = "Mostrar frases de Joe en Inicio",
                        checked = filterAutorJoe
                    ) { newValue ->
                        if (!isAnyFilterEnabled(joe = newValue)) {
                            Toast.makeText(context, "Debe quedar al menos un filtro activo", Toast.LENGTH_SHORT).show()
                            return@SwitchField
                        }
                        filterAutorJoe = newValue
                        prefs.edit { putBoolean("home_filter_author_joe", newValue) }
                        saveAuthorsAggregate()
                    }
                    FieldDivider()
                    SwitchField(
                        title = "Gregg Braden",
                        description = "Mostrar frases de Gregg en Inicio",
                        checked = filterAutorGregg
                    ) { newValue ->
                        if (!isAnyFilterEnabled(gregg = newValue)) {
                            Toast.makeText(context, "Debe quedar al menos un filtro activo", Toast.LENGTH_SHORT).show()
                            return@SwitchField
                        }
                        filterAutorGregg = newValue
                        prefs.edit { putBoolean("home_filter_author_gregg", newValue) }
                        saveAuthorsAggregate()
                    }
                    FieldDivider()
                    SwitchField(
                        title = "Bruce Lipton",
                        description = "Mostrar frases de Bruce en Inicio",
                        checked = filterAutorBruce
                    ) { newValue ->
                        if (!isAnyFilterEnabled(bruce = newValue)) {
                            Toast.makeText(context, "Debe quedar al menos un filtro activo", Toast.LENGTH_SHORT).show()
                            return@SwitchField
                        }
                        filterAutorBruce = newValue
                        prefs.edit { putBoolean("home_filter_author_bruce", newValue) }
                        saveAuthorsAggregate()
                    }

                    FieldDivider(padding = 8.dp)
                    Text(
                        text = "Categorías",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    SwitchField(
                        title = "Otros autores",
                        description = "Incluir frases de autores diversos",
                        checked = filterOtros
                    ) { newValue ->
                        if (!isAnyFilterEnabled(otros = newValue)) {
                            Toast.makeText(context, "Debe quedar al menos un filtro activo", Toast.LENGTH_SHORT).show()
                            return@SwitchField
                        }
                        filterOtros = newValue
                        prefs.edit { putBoolean("home_filter_otros", newValue) }
                    }
                    FieldDivider()
                    SwitchField(
                        title = "Salud",
                        description = "Incluir tips y frases de salud",
                        checked = filterSalud
                    ) { newValue ->
                        if (!isAnyFilterEnabled(salud = newValue)) {
                            Toast.makeText(context, "Debe quedar al menos un filtro activo", Toast.LENGTH_SHORT).show()
                            return@SwitchField
                        }
                        filterSalud = newValue
                        prefs.edit { putBoolean("home_filter_salud", newValue) }
                    }
                }
            }

            item {
                SettingSection(
                    title = "Notas",
                    subtitle = "Controla el acceso a la lista de notas"
                ) {
                    SwitchField(
                        title = "Bloqueo biométrico (Suscripción)",
                        description = "Solicita biometría para abrir la lista de Notas",
                        checked = notesBiometricLockEnabled
                    ) { newValue ->
                        if (!newValue) {
                            authenticateForNotesLockChange {
                                notesBiometricLockEnabled = false
                                prefs.edit { putBoolean(notesBiometricLockPrefKey, false) }
                                Toast.makeText(
                                    context,
                                    "Bloqueo biométrico de Notas desactivado",
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
                                "No hay biometría disponible/configurada en este dispositivo",
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
                    title = "Backup de Base de Datos",
                    subtitle = "Conecta Drive/OneDrive/otros y programa respaldos automáticos"
                ) {
                    Text(
                        text = "Cifrado activo: AES-256-GCM (clave derivada con PBKDF2).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FieldDivider()
                    ActionField(
                        title = if (providerInfo == null) "Conectar proveedor personal" else "Proveedor conectado",
                        description = providerInfo?.let {
                            "${it.displayName} (${it.authority})"
                        } ?: "Selecciona una carpeta en Drive, OneDrive u otro proveedor"
                    ) {
                        pickProviderFolderLauncher.launch(null)
                    }
                    if (providerInfo != null) {
                        FieldDivider()
                        ActionField(
                            title = "Desconectar proveedor",
                            description = "Quita el enlace y detiene backups automáticos"
                        ) {
                            backupManager.disconnectProvider()
                            settingsUiRefreshTick++
                            Toast.makeText(context, "Proveedor desconectado", Toast.LENGTH_SHORT).show()
                        }
                    }

                    FieldDivider()
                    ActionField(
                        title = if (hasSavedPassphrase) "Actualizar clave de cifrado" else "Configurar clave de cifrado",
                        description = if (hasSavedPassphrase) {
                            "Clave guardada localmente para backups automáticos"
                        } else {
                            "Requerida para cifrar y restaurar backups"
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
                            title = "Eliminar clave guardada",
                            description = "Requiere la clave actual"
                        ) {
                            deleteCurrentPassphraseInput = ""
                            showDeletePassphraseDialog = true
                        }
                    }

                    if (hasSavedPassphrase) {
                        FieldDivider()
                        ActionField(
                            title = "Recuperar contraseña con biometría",
                            description = "Se mostrará la contraseña tras autenticar con biometría"
                        ) {
                            val canAuth = BiometricManager.from(context).canAuthenticate(
                                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                    BiometricManager.Authenticators.BIOMETRIC_WEAK
                            )
                            if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
                                Toast.makeText(
                                    context,
                                    "No hay biometría disponible/configurada en este dispositivo",
                                    Toast.LENGTH_LONG
                                ).show()
                                return@ActionField
                            }
                            launchBiometricRecovery()
                        }
                    }

                    FieldDivider()
                    ActionField(
                        title = "Guía de recuperación",
                        description = "Cómo recuperar la contraseña con biometría"
                    ) {
                        showRecoveryGuideDialog = true
                    }

                    FieldDivider()
                    ActionField(
                        title = "Frecuencia",
                        description = backupFrequency.toUiLabel()
                    ) {
                        showFrequencyDialog = true
                    }

                    FieldDivider()
                    ActionField(
                        title = "Hacer backup ahora",
                        description = if (hasSavedPassphrase) {
                            "Genera una copia manual inmediatamente"
                        } else {
                            "Configura una clave de cifrado para habilitar esta acción"
                        }
                    ) {
                        if (!hasSavedPassphrase) {
                            Toast.makeText(
                                context,
                                "Configura primero la clave de cifrado",
                                Toast.LENGTH_LONG
                            ).show()
                            return@ActionField
                        }
                        coroutineScope.launch {
                            when (val result = backupManager.backupNow()) {
                                is CloudBackupManager.BackupResult.Success -> {
                                    settingsUiRefreshTick++
                                    Toast.makeText(
                                        context,
                                        "Backup creado: ${result.fileName}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                is CloudBackupManager.BackupResult.Error -> {
                                    settingsUiRefreshTick++
                                    Toast.makeText(
                                        context,
                                        "Error de backup: ${result.reason}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }

                    FieldDivider()
                    ActionField(
                        title = "Restaurar desde backup",
                        description = "Selecciona un archivo .nvbak desde un proveedor"
                    ) {
                        showRestoreDialog = true
                    }

                    FieldDivider(padding = 8.dp)
                    Text(
                        text = "Último backup: ${backupManager.formatTimestamp(lastBackupAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!lastBackupError.isNullOrBlank()) {
                        Text(
                            text = "Último error: $lastBackupError",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            item {
                SettingSection(
                    title = "Contenido",
                    subtitle = "Sincronización y mantenimiento"
                ) {
                    ActionField(
                        title = "Actualizar frases desde archivos",
                        description = "Reimporta las frases desde assets/frases"
                    ) {
                        val updated = utilsDB.forceRefreshFrasesFromAssets(context)
                        Toast.makeText(
                            context,
                            if (updated) "Frases actualizadas desde assets" else "No hubo cambios en frases",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            item {
                SettingSection(
                    title = "Proyecto y Soporte",
                    subtitle = "Acciones de contacto, reseña y novedades"
                ) {
                    ActionField("Ver novedades", "Consultar cambios de versión") {
                        UiModalWindows.showAyudaContectual(
                            context,
                            "Novedades",
                            "Que hay de nuevo?",
                            NewsContent.buildNewsText(),
                            false,
                            AppCompatResources.getDrawable(context, R.drawable.neville)
                        )
                    }
                    FieldDivider()
                    ActionField("Enviar comentario", "Contactar con el desarrollador") {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = "mailto:info@ypgcode.es".toUri()
                            putExtra(Intent.EXTRA_SUBJECT, "Comentario sobre Neville Para Todos")
                        }
                        try {
                            startActivity(emailIntent)
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(context, "No se encontró una app de correo", Toast.LENGTH_LONG).show()
                        }
                    }
                    FieldDivider()
                    ActionField("Sitio web del proyecto", "Abrir web oficial") {
                        startActivity(Intent(Intent.ACTION_VIEW, "https://ypgcode.es/neville_goddard/".toUri()))
                    }
                    FieldDivider()
                    ActionField("Escribir reseña", "Valorar la app en Google Play") {
                        val uri = "market://details?id=${context.packageName}".toUri()
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        try {
                            startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(context, "No se encontró la app de tienda", Toast.LENGTH_LONG).show()
                        }
                    }


                }
            }
        }

        if (showFrequencyDialog) {
            AlertDialog(
                onDismissRequest = { showFrequencyDialog = false },
                title = { Text("Frecuencia de backup") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FrequencyOption(
                            title = "Manual",
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
                                        error.message ?: "No se pudo guardar la frecuencia",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                        FrequencyOption(
                            title = "Diario",
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
                                        error.message ?: "No se pudo guardar la frecuencia",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                        FrequencyOption(
                            title = "Semanal",
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
                                        error.message ?: "No se pudo guardar la frecuencia",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFrequencyDialog = false }) {
                        Text("Cerrar")
                    }
                }
            )
        }

        if (showRestoreDialog) {
            AlertDialog(
                onDismissRequest = { showRestoreDialog = false },
                title = { Text("Restaurar base de datos") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Esta acción reemplaza tus datos actuales. Si continúas, selecciona un archivo de backup .nvbak.")
                        OutlinedTextField(
                            value = restorePassphraseInput,
                            onValueChange = { restorePassphraseInput = it },
                            label = { Text("Clave de cifrado") },
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
                        Text("Continuar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        restorePassphraseInput = ""
                        showRestoreDialog = false
                    }) {
                        Text("Cancelar")
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
                            "Actualizar clave de cifrado"
                        } else {
                            "Clave de cifrado de backups"
                        }
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Usa una clave larga. La necesitarás para restaurar backups en una reinstalación.")
                        SwitchField(
                            title = "Mostrar contraseña",
                            description = "Visualiza u oculta los caracteres de la clave",
                            checked = showPassphrasePlainText,
                            onCheckedChange = { showPassphrasePlainText = it }
                        )
                        if (hasSavedPassphrase) {
                            OutlinedTextField(
                                value = currentPassphraseInput,
                                onValueChange = { currentPassphraseInput = it },
                                label = { Text("Clave actual") },
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
                            label = { Text("Nueva clave") },
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
                            label = { Text("Confirmar clave") },
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
                            Toast.makeText(context, "Las claves no coinciden", Toast.LENGTH_LONG).show()
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
                            Toast.makeText(context, "Clave guardada", Toast.LENGTH_SHORT).show()
                        }.onFailure { error ->
                            Toast.makeText(context, error.message ?: "No se pudo guardar la clave", Toast.LENGTH_LONG).show()
                        }
                    }) {
                        Text("Guardar")
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
                        Text("Cancelar")
                    }
                }
            )
        }

        if (showDeletePassphraseDialog) {
            AlertDialog(
                onDismissRequest = { showDeletePassphraseDialog = false },
                title = { Text("Eliminar clave de cifrado") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Para eliminar la clave guardada, confirma la clave actual.")
                        OutlinedTextField(
                            value = deleteCurrentPassphraseInput,
                            onValueChange = { deleteCurrentPassphraseInput = it },
                            label = { Text("Clave actual") },
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
                                Toast.makeText(context, "Clave eliminada", Toast.LENGTH_SHORT).show()
                            }
                            .onFailure { error ->
                                Toast.makeText(
                                    context,
                                    error.message ?: "No se pudo eliminar la clave",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }) { Text("Eliminar") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        deleteCurrentPassphraseInput = ""
                        showDeletePassphraseDialog = false
                    }) { Text("Cancelar") }
                }
            )
        }

        if (!recoveredPassphraseMessage.isNullOrBlank()) {
            AlertDialog(
                onDismissRequest = { recoveredPassphraseMessage = null },
                title = { Text("Contraseña de cifrado") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Autenticación biométrica exitosa. Esta es tu contraseña:")
                        Text(
                            text = recoveredPassphraseMessage.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { recoveredPassphraseMessage = null }) {
                        Text("Cerrar")
                    }
                }
            )
        }

        if (showRecoveryGuideDialog) {
            AlertDialog(
                onDismissRequest = { showRecoveryGuideDialog = false },
                title = { Text("Guía de recuperación de clave") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("1. Usa 'Recuperar contraseña con biometría'.")
                        Text("2. Autentícate con la biometría configurada en el dispositivo.")
                        Text("3. Si la autenticación es correcta, la app mostrará la contraseña de cifrado.")
                        Text("4. No compartas ni captures la contraseña cuando se muestre en pantalla.")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showRecoveryGuideDialog = false }) {
                        Text("Entendido")
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
                text = if (selected) "Seleccionado" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
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
                    style = MaterialTheme.typography.bodySmall,
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
                    color = MaterialTheme.colorScheme.onSurface
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
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    @Composable
    private fun FieldDivider(padding: androidx.compose.ui.unit.Dp = 6.dp) {
        HorizontalDivider(modifier = Modifier.padding(vertical = padding))
    }

    private fun CloudBackupManager.BackupFrequency.toUiLabel(): String {
        return when (this) {
            CloudBackupManager.BackupFrequency.MANUAL -> "Manual (sin ejecución automática)"
            CloudBackupManager.BackupFrequency.DAILY -> "Diario"
            CloudBackupManager.BackupFrequency.WEEKLY -> "Semanal"
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
                                    error.message ?: "No se pudo recuperar la clave",
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
                .setTitle("Recuperación de clave")
                .setSubtitle("Autentícate para ver tu contraseña de cifrado")
                .setNegativeButtonText("Cancelar")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
                .build()

            prompt.authenticate(promptInfo)
        }.onFailure { error ->
            Toast.makeText(
                requireContext(),
                error.message ?: "No se pudo iniciar autenticación biométrica",
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
                "No hay biometría disponible/configurada en este dispositivo",
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
                .setTitle("Desactivar bloqueo de Notas")
                .setSubtitle("Autentícate para desactivar la protección biométrica")
                .setNegativeButtonText("Cancelar")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
                .build()

            prompt.authenticate(promptInfo)
        }.onFailure { error ->
            Toast.makeText(
                requireContext(),
                error.message ?: "No se pudo iniciar autenticación biométrica",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
