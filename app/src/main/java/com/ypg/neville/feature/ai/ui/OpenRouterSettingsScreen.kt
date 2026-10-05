package com.ypg.neville.feature.ai.ui

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ypg.neville.R
import com.ypg.neville.feature.ai.domain.OpenRouterConfiguration
import com.ypg.neville.feature.ai.domain.OpenRouterModel
import com.ypg.neville.feature.ai.network.OpenRouterClient
import com.ypg.neville.feature.ai.network.OpenRouterErrorKind
import com.ypg.neville.feature.ai.network.OpenRouterException
import com.ypg.neville.feature.ai.security.OpenRouterCredentialStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenRouterSettingsScreen(
    onClose: () -> Unit,
    onCredentialsChanged: () -> Unit
) {
    val context = LocalContext.current
    val credentialStore = remember { OpenRouterCredentialStore(context) }
    val client = remember { OpenRouterClient() }
    val scope = rememberCoroutineScope()
    var apiKey by remember { mutableStateOf("") }
    var hasStoredKey by remember { mutableStateOf(credentialStore.hasApiKey()) }
    var isValidating by remember { mutableStateOf(false) }
    var models by remember { mutableStateOf(OpenRouterConfiguration.fallbackModels) }
    var selectedModel by remember {
        mutableStateOf(OpenRouterConfiguration.selectedModelIdentifier(context))
    }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var tutorialExpanded by remember { mutableStateOf(!hasStoredKey) }
    var modelMenuExpanded by remember { mutableStateOf(false) }
    var usesPersonalVoice by remember {
        mutableStateOf(OpenRouterConfiguration.usesPersonalVoice(context))
    }

    fun validateAndSave() {
        statusMessage = null
        errorMessage = null
        isValidating = true
        scope.launch {
            runCatching {
                val candidate = apiKey.trim()
                val key = candidate.ifBlank {
                    credentialStore.readApiKey()
                        ?: throw OpenRouterException(OpenRouterErrorKind.MISSING_API_KEY)
                }
                val catalog = client.availableModels(key)
                if (candidate.isNotEmpty()) credentialStore.saveApiKey(candidate)
                models = catalog.models
                OpenRouterConfiguration.updateDailyFreeRequestLimit(
                    context,
                    catalog.dailyFreeRequestLimit
                )
                if (
                    !OpenRouterConfiguration.hasSelectedModelIdentifier(context) ||
                    catalog.models.none { it.id == selectedModel }
                ) {
                    selectedModel = catalog.models.firstOrNull {
                        it.isAutomaticFreeSelection
                    }?.id ?: catalog.models.firstOrNull()?.id
                        ?: OpenRouterConfiguration.DEFAULT_MODEL
                }
                OpenRouterConfiguration.selectModel(context, selectedModel)
                apiKey = ""
                hasStoredKey = true
                statusMessage = context.getString(R.string.ai_settings_key_valid)
                onCredentialsChanged()
            }.onFailure {
                errorMessage = it.localizedAiMessage(context)
            }
            isValidating = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OpenRouter") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.ai_close))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SettingsSection(title = stringResource(R.string.ai_settings_create_key)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { tutorialExpanded = !tutorialExpanded }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.ai_settings_quick_guide),
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        if (tutorialExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        null
                    )
                }
                if (tutorialExpanded) {
                    TutorialStep(
                        1,
                        stringResource(R.string.ai_settings_step1_title),
                        stringResource(R.string.ai_settings_step1_detail)
                    )
                    TutorialStep(
                        2,
                        stringResource(R.string.ai_settings_step2_title),
                        stringResource(R.string.ai_settings_step2_detail)
                    )
                    TutorialStep(
                        3,
                        stringResource(R.string.ai_settings_step3_title),
                        stringResource(R.string.ai_settings_step3_detail)
                    )
                    TutorialStep(
                        4,
                        stringResource(R.string.ai_settings_step4_title),
                        stringResource(R.string.ai_settings_step4_detail)
                    )
                    TutorialStep(
                        5,
                        stringResource(R.string.ai_settings_step5_title),
                        stringResource(R.string.ai_settings_step5_detail)
                    )
                    LinkText(
                        stringResource(R.string.ai_settings_open_keys),
                        OPENROUTER_KEYS_URL
                    )
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(
                            stringResource(R.string.ai_settings_key_safety),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            SettingsSection(title = stringResource(R.string.ai_settings_personal_key)) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it.trimStart() },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            stringResource(
                                if (hasStoredKey) R.string.ai_settings_replace_key
                                else R.string.ai_settings_api_key
                            )
                        )
                    },
                    leadingIcon = { Icon(Icons.Rounded.Key, null) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                    singleLine = true
                )
                if (hasStoredKey) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF2E7D32))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.ai_settings_key_stored),
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
                Button(
                    onClick = ::validateAndSave,
                    enabled = !isValidating && (apiKey.isNotBlank() || hasStoredKey),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isValidating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.ai_settings_checking))
                    } else {
                        Text(
                            stringResource(
                                if (apiKey.isBlank() && hasStoredKey) {
                                    R.string.ai_settings_check_stored
                                } else {
                                    R.string.ai_settings_check_save
                                }
                            )
                        )
                    }
                }
                statusMessage?.let { Text(it, color = Color(0xFF2E7D32)) }
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }

            SettingsSection(title = stringResource(R.string.ai_settings_model)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { modelMenuExpanded = true },
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 2.dp
                ) {
                    Text(
                        models.firstOrNull { it.id == selectedModel }
                            ?.localizedDisplayName() ?: selectedModel,
                        modifier = Modifier.padding(14.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    DropdownMenu(
                        expanded = modelMenuExpanded,
                        onDismissRequest = { modelMenuExpanded = false }
                    ) {
                        models.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model.localizedDisplayName()) },
                                onClick = {
                                    selectedModel = model.id
                                    OpenRouterConfiguration.selectModel(context, model.id)
                                    modelMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Text(
                    stringResource(R.string.ai_settings_free_models_only),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(
                        R.string.ai_free_account_limit,
                        OpenRouterConfiguration.dailyFreeRequestLimit(context)
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
                models.firstOrNull { it.id == selectedModel }?.let { selected ->
                    Text(selected.id, style = MaterialTheme.typography.labelSmall)
                    selected.contextLength?.let {
                        Text(
                            stringResource(R.string.ai_settings_context_length, it),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    val description = if (selected.isAutomaticFreeSelection) {
                        stringResource(R.string.ai_settings_auto_model_description)
                    } else {
                        selected.description
                    }
                    description?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 5)
                    }
                }
            }

            SettingsSection(title = stringResource(R.string.ai_settings_privacy_usage)) {
                Text(stringResource(R.string.ai_settings_key_owned))
                Text(stringResource(R.string.ai_settings_data_routing))
                Text(stringResource(R.string.ai_settings_never_paid))
                LinkText(
                    stringResource(R.string.ai_settings_provider_privacy),
                    OPENROUTER_PRIVACY_GUIDE_URL
                )
                LinkText(
                    stringResource(R.string.ai_settings_free_limits),
                    OPENROUTER_FAQ_URL
                )
                if (OpenRouterConfiguration.hasPrivacyConsent(context)) {
                    TextButton(onClick = {
                        OpenRouterConfiguration.revokePrivacyConsent(context)
                        statusMessage = context.getString(R.string.ai_settings_consent_revoked)
                        onCredentialsChanged()
                    }) {
                        Text(
                            stringResource(R.string.ai_settings_revoke_consent),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            SettingsSection(title = stringResource(R.string.ai_settings_role)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(
                                if (usesPersonalVoice) R.string.ai_settings_personal
                                else R.string.ai_settings_impersonal
                            )
                        )
                        Text(
                            if (usesPersonalVoice) {
                                stringResource(R.string.ai_settings_personal_detail)
                            } else {
                                stringResource(R.string.ai_settings_impersonal_detail)
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = usesPersonalVoice,
                        onCheckedChange = {
                            usesPersonalVoice = it
                            OpenRouterConfiguration.setUsesPersonalVoice(context, it)
                        }
                    )
                }
                Text(
                    stringResource(R.string.ai_settings_new_conversations_only),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            if (hasStoredKey) {
                Button(
                    onClick = {
                        credentialStore.deleteApiKey()
                        OpenRouterConfiguration.resetDailyFreeRequestLimit(context)
                        apiKey = ""
                        hasStoredKey = false
                        statusMessage = context.getString(R.string.ai_settings_key_deleted)
                        errorMessage = null
                        onCredentialsChanged()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.ai_settings_delete_key))
                }
            }
            Spacer(Modifier.height(22.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenRouterPrivacyScreen(
    onCancel: () -> Unit,
    onAccept: () -> Unit
) {
    var confirmsSharing by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ai_privacy_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.common_cancel))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.NetworkCheck, null, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(9.dp))
                Text(
                    stringResource(R.string.ai_privacy_online),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(stringResource(R.string.ai_privacy_optional))
            SettingsSection(title = stringResource(R.string.ai_privacy_shared_title)) {
                Text(stringResource(R.string.ai_privacy_shared_detail))
            }
            SettingsSection(title = stringResource(R.string.ai_privacy_with_whom)) {
                Text(stringResource(R.string.ai_privacy_with_whom_detail))
            }
            SettingsSection(title = stringResource(R.string.ai_privacy_not_happen)) {
                Text(stringResource(R.string.ai_privacy_not_happen_detail))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { confirmsSharing = !confirmsSharing }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                androidx.compose.material3.Checkbox(
                    checked = confirmsSharing,
                    onCheckedChange = { confirmsSharing = it }
                )
                Text(
                    stringResource(R.string.ai_privacy_consent),
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            LinkText(
                stringResource(R.string.ai_privacy_terms_link),
                "https://openrouter.ai/terms"
            )
            LinkText(
                stringResource(R.string.ai_privacy_policy_link),
                "https://openrouter.ai/privacy"
            )
            Button(
                onClick = onAccept,
                enabled = confirmsSharing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.ai_privacy_accept_continue))
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun TutorialStep(number: Int, title: String, detail: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            number.toString(),
            color = Color.White,
            modifier = Modifier
                .size(25.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .padding(top = 3.dp),
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LinkText(label: String, url: String) {
    val context = LocalContext.current
    Text(
        text = label,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        }
    )
}

@Composable
private fun OpenRouterModel.localizedDisplayName(): String =
    if (isAutomaticFreeSelection) {
        stringResource(R.string.ai_settings_auto_model)
    } else {
        name
    }

private const val OPENROUTER_KEYS_URL = "https://openrouter.ai/settings/keys"
private const val OPENROUTER_PRIVACY_GUIDE_URL =
    "https://openrouter.ai/docs/guides/privacy/provider-logging"
private const val OPENROUTER_FAQ_URL = "https://openrouter.ai/docs/faq"
