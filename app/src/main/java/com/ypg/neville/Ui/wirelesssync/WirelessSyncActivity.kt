package com.ypg.neville.ui.wirelesssync

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.ypg.neville.R
import com.ypg.neville.model.migration.NevilleMigrationRoomBridge
import com.ypg.neville.model.wirelesssync.WirelessSessionDescriptor
import com.ypg.neville.model.wirelesssync.WirelessSyncDirection
import com.ypg.neville.model.wirelesssync.WirelessSyncServer
import com.ypg.neville.model.wirelesssync.WirelessSyncServerState

class WirelessSyncActivity : ComponentActivity() {
    private var activeServer by mutableStateOf<WirelessSyncServer?>(null)
    private var startupError by mutableStateOf<String?>(null)
    private var pendingStart: (() -> Unit)? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val allowed = grants.values.all { it }
        if (allowed) {
            pendingStart?.invoke()
        } else {
            startupError = getString(R.string.wireless_sync_permission_denied)
        }
        pendingStart = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WirelessSyncScreen(
                        onStart = ::requestStart,
                        onStop = ::stopServer,
                        onClose = { finish() },
                        serverProvider = { activeServer },
                        startupErrorProvider = { startupError }
                    )
                }
            }
        }
    }

    private fun requestStart(
        direction: WirelessSyncDirection,
        includedTypes: Set<String>?,
        direct: Boolean
    ) {
        startupError = null
        val startAction = {
            stopServer()
            activeServer = WirelessSyncServer(
                context = this,
                direction = direction,
                includedTypes = includedTypes,
                useDirectHotspot = direct
            ).also(WirelessSyncServer::start)
        }
        val missing = requiredPermissions(direct).filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            startAction()
        } else {
            pendingStart = startAction
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun requiredPermissions(direct: Boolean): List<String> {
        if (!direct) return emptyList()
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> listOf(Manifest.permission.NEARBY_WIFI_DEVICES)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> listOf(Manifest.permission.ACCESS_FINE_LOCATION)
            else -> emptyList()
        }
    }

    private fun stopServer() {
        activeServer?.close()
        activeServer = null
    }

    override fun onDestroy() {
        stopServer()
        super.onDestroy()
    }
}

private data class ContentTypeOption(val type: String, val labelRes: Int)

private val contentTypeOptions = listOf(
    ContentTypeOption(NevilleMigrationRoomBridge.TYPE_NOTE, R.string.wireless_sync_notes),
    ContentTypeOption(NevilleMigrationRoomBridge.TYPE_DIARY, R.string.wireless_sync_diary),
    ContentTypeOption(NevilleMigrationRoomBridge.TYPE_AGENDA, R.string.wireless_sync_agenda),
    ContentTypeOption(NevilleMigrationRoomBridge.TYPE_GOAL, R.string.wireless_sync_goals),
    ContentTypeOption(NevilleMigrationRoomBridge.TYPE_ARCHIVED_GOAL, R.string.wireless_sync_archived_goals),
    ContentTypeOption(NevilleMigrationRoomBridge.TYPE_PERSONAL_PHRASE, R.string.wireless_sync_phrases),
    ContentTypeOption(NevilleMigrationRoomBridge.TYPE_PERSONAL_REFLECTION, R.string.wireless_sync_reflections),
    ContentTypeOption(NevilleMigrationRoomBridge.TYPE_DAY_RITUAL_ARCHIVE, R.string.wireless_sync_rituals),
    ContentTypeOption(NevilleMigrationRoomBridge.TYPE_CALM_PERSONAL_PHRASE, R.string.wireless_sync_calm_phrases)
)

@Composable
private fun WirelessSyncScreen(
    onStart: (WirelessSyncDirection, Set<String>?, Boolean) -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit,
    serverProvider: () -> WirelessSyncServer?,
    startupErrorProvider: () -> String?
) {
    var direction by remember { mutableStateOf(WirelessSyncDirection.AndroidToIos) }
    var direct by remember { mutableStateOf(false) }
    var transferEverything by remember { mutableStateOf(true) }
    var selectedTypes by remember { mutableStateOf(contentTypeOptions.map { it.type }.toSet()) }
    var showInfo by remember { mutableStateOf(false) }
    val server = serverProvider()
    val idleState = remember {
        kotlinx.coroutines.flow.MutableStateFlow<WirelessSyncServerState>(WirelessSyncServerState.Idle)
    }
    val state by (server?.state ?: idleState).collectAsState()
    val startupError = startupErrorProvider()

    DisposableEffect(server) {
        onDispose { }
    }

    if (showInfo) {
        WirelessSyncInfoDialog(onDismiss = { showInfo = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.wireless_sync_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showInfo = true }) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.wireless_sync_info_action)
                )
            }
        }
        Text(stringResource(R.string.wireless_sync_android_explanation), color = MaterialTheme.colorScheme.onSurfaceVariant)
        startupError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        if (state is WirelessSyncServerState.Idle || state is WirelessSyncServerState.Failed) {
            Text(stringResource(R.string.wireless_sync_direction), fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = direction == WirelessSyncDirection.AndroidToIos,
                    onClick = { direction = WirelessSyncDirection.AndroidToIos },
                    label = { Text(stringResource(R.string.wireless_sync_send_to_iphone)) }
                )
                FilterChip(
                    selected = direction == WirelessSyncDirection.IosToAndroid,
                    onClick = { direction = WirelessSyncDirection.IosToAndroid },
                    label = { Text(stringResource(R.string.wireless_sync_receive_from_iphone)) }
                )
            }

            if (direction == WirelessSyncDirection.AndroidToIos) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = transferEverything, onCheckedChange = { transferEverything = it })
                    Text(stringResource(R.string.wireless_sync_everything))
                }
                if (!transferEverything) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        contentTypeOptions.chunked(2).forEach { options ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                options.forEach { option ->
                                    FilterChip(
                                        selected = option.type in selectedTypes,
                                        onClick = {
                                            selectedTypes = if (option.type in selectedTypes) {
                                                selectedTypes - option.type
                                            } else {
                                                selectedTypes + option.type
                                            }
                                        },
                                        label = { Text(stringResource(option.labelRes), maxLines = 2) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (options.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            Text(stringResource(R.string.wireless_sync_connection), fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !direct,
                    onClick = { direct = false },
                    label = { Text(stringResource(R.string.wireless_sync_same_wifi)) }
                )
                FilterChip(
                    selected = direct,
                    onClick = { direct = true },
                    label = { Text(stringResource(R.string.wireless_sync_direct)) }
                )
            }
            Text(
                stringResource(if (direct) R.string.wireless_sync_direct_hint else R.string.wireless_sync_wifi_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = {
                    onStart(
                        direction,
                        if (direction == WirelessSyncDirection.AndroidToIos && !transferEverything) selectedTypes else null,
                        direct
                    )
                },
                enabled = direction == WirelessSyncDirection.IosToAndroid || transferEverything || selectedTypes.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.wireless_sync_create_link))
            }
        }

        when (val current = state) {
            WirelessSyncServerState.Starting -> Text(stringResource(R.string.wireless_sync_preparing))
            is WirelessSyncServerState.Ready -> ReadyQr(current.descriptor)
            WirelessSyncServerState.Connected -> Text(stringResource(R.string.wireless_sync_connected))
            WirelessSyncServerState.Transferring -> Text(stringResource(R.string.wireless_sync_transferring))
            is WirelessSyncServerState.Completed -> {
                Text(stringResource(R.string.wireless_sync_completed), fontWeight = FontWeight.Bold)
                Text(current.message)
                Button(onClick = { onStop(); onClose() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.wireless_sync_close))
                }
            }
            is WirelessSyncServerState.Failed -> {
                Text(current.message, color = MaterialTheme.colorScheme.error)
            }
            WirelessSyncServerState.Idle -> Unit
        }

        if (state !is WirelessSyncServerState.Completed) {
            OutlinedButton(onClick = { onStop(); onClose() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.wireless_sync_cancel))
            }
        }
    }
}

@Composable
private fun WirelessSyncInfoDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .widthIn(max = 640.dp)
                .heightIn(max = 720.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.wireless_sync_info_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.wireless_sync_info_close))
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(top = 12.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    InfoBlock(
                        icon = Icons.Outlined.SwapHoriz,
                        title = stringResource(R.string.wireless_sync_info_utility_title),
                        text = stringResource(R.string.wireless_sync_info_utility)
                    )
                    InfoBlock(
                        icon = Icons.Outlined.Lock,
                        title = stringResource(R.string.wireless_sync_info_security_title),
                        text = stringResource(R.string.wireless_sync_info_security)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.GridView,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                stringResource(R.string.wireless_sync_info_content_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        contentTypeOptions.chunked(2).forEach { options ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                options.forEach { option ->
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 44.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    ) {
                                        Text(
                                            stringResource(option.labelRes),
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                                if (options.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoBlock(icon: ImageVector, title: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReadyQr(descriptor: WirelessSessionDescriptor) {
    val bitmap = remember(descriptor) { qrBitmap(descriptor.toQrPayload(), 900) }
    Text(stringResource(R.string.wireless_sync_scan_on_iphone), fontWeight = FontWeight.Bold)
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = stringResource(R.string.wireless_sync_qr_description),
        modifier = Modifier.fillMaxWidth().size(320.dp)
    )
    Spacer(Modifier.height(4.dp))
    Text(stringResource(R.string.wireless_sync_link_expires), style = MaterialTheme.typography.bodySmall)
}

private fun qrBitmap(content: String, size: Int): Bitmap {
    val matrix: BitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
    }
}
