package com.ypg.neville.model.wirelesssync

import android.annotation.SuppressLint
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.ypg.neville.model.migration.ImportPolicy
import com.ypg.neville.model.migration.MigrationSummary
import com.ypg.neville.model.migration.MyAppMigrationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed interface WirelessSyncServerState {
    data object Idle : WirelessSyncServerState
    data object Starting : WirelessSyncServerState
    data class Ready(val descriptor: WirelessSessionDescriptor) : WirelessSyncServerState
    data object Connected : WirelessSyncServerState
    data object Transferring : WirelessSyncServerState
    data class Completed(val message: String) : WirelessSyncServerState
    data class Failed(val message: String) : WirelessSyncServerState
}

class WirelessSyncServer(
    context: Context,
    private val direction: WirelessSyncDirection,
    private val includedTypes: Set<String>?,
    private val useDirectHotspot: Boolean
) : AutoCloseable {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val migrationService = MyAppMigrationService(appContext)
    private val secureRandom = SecureRandom()
    private val nsdManager = appContext.getSystemService(NsdManager::class.java)
    private val wifiManager = appContext.getSystemService(WifiManager::class.java)
    private val mutableState = MutableStateFlow<WirelessSyncServerState>(WirelessSyncServerState.Idle)
    val state: StateFlow<WirelessSyncServerState> = mutableState.asStateFlow()

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var hotspotReservation: WifiManager.LocalOnlyHotspotReservation? = null

    fun start() {
        if (serverJob != null) return
        serverJob = scope.launch {
            runCatching { runServer() }
                .onFailure { error ->
                    if (serverSocket?.isClosed != true) {
                        mutableState.value = WirelessSyncServerState.Failed(
                            error.message ?: "No se pudo iniciar la conexión"
                        )
                    }
                }
        }
    }

    private suspend fun runServer() {
        mutableState.value = WirelessSyncServerState.Starting
        val networkCredentials = if (useDirectHotspot) startLocalOnlyHotspot() else null
        val socket = ServerSocket(0).also {
            it.reuseAddress = true
            it.soTimeout = (SESSION_TTL_SECONDS * 1_000).toInt()
            serverSocket = it
        }
        val sessionId = UUID.randomUUID().toString().lowercase()
        val serviceName = "Neville-${sessionId.take(8)}"
        val secret = WirelessSessionDescriptor.newSecret(secureRandom)
        val descriptor = WirelessSessionDescriptor(
            version = WirelessSessionDescriptor.VERSION,
            sessionId = sessionId,
            direction = direction,
            serviceName = serviceName,
            secret = secret,
            expiresAtEpochSeconds = System.currentTimeMillis() / 1000 + SESSION_TTL_SECONDS,
            network = networkCredentials
        )
        registerService(serviceName, sessionId, socket.localPort)
        mutableState.value = WirelessSyncServerState.Ready(descriptor)
        socket.accept().use { client ->
            client.soTimeout = SOCKET_TIMEOUT_MILLIS
            mutableState.value = WirelessSyncServerState.Connected
            handleClient(client, descriptor)
        }
    }

    private suspend fun handleClient(socket: Socket, descriptor: WirelessSessionDescriptor) {
        val input = DataInputStream(BufferedInputStream(socket.getInputStream()))
        val output = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
        authenticate(input, output, descriptor)
        mutableState.value = WirelessSyncServerState.Transferring
        when (direction) {
            WirelessSyncDirection.AndroidToIos -> sendPackage(input, output, descriptor)
            WirelessSyncDirection.IosToAndroid -> receivePackage(input, output, descriptor)
        }
    }

    private fun authenticate(
        input: DataInputStream,
        output: DataOutputStream,
        descriptor: WirelessSessionDescriptor
    ) {
        val hello = WirelessFrameCodec.read(input)
        require(hello.type == WirelessFrameCodec.HELLO) { "Inicio de sesión inválido" }
        val helloJson = JSONObject(String(hello.payload, Charsets.UTF_8))
        require(helloJson.getString("sid") == descriptor.sessionId) { "Sesión incorrecta" }
        require(System.currentTimeMillis() / 1000 <= descriptor.expiresAtEpochSeconds) { "El enlace ha caducado" }
        val clientNonce = android.util.Base64.decode(helloJson.getString("nonce"), android.util.Base64.DEFAULT)
        require(clientNonce.size == NONCE_BYTES) { "Nonce de cliente inválido" }
        val serverNonce = ByteArray(NONCE_BYTES).also(secureRandom::nextBytes)
        val challenge = JSONObject()
            .put("nonce", android.util.Base64.encodeToString(serverNonce, android.util.Base64.NO_WRAP))
            .toString()
            .toByteArray(Charsets.UTF_8)
        WirelessFrameCodec.write(output, WirelessFrameCodec.CHALLENGE, challenge)
        val auth = WirelessFrameCodec.read(input)
        require(auth.type == WirelessFrameCodec.AUTH) { "Autenticación ausente" }
        val expected = WirelessSessionAuthenticator.proof(
            descriptor.secretBytes(),
            descriptor.sessionId,
            clientNonce,
            serverNonce
        )
        require(WirelessSessionAuthenticator.matches(expected, auth.payload)) { "Autenticación rechazada" }
        WirelessFrameCodec.write(output, WirelessFrameCodec.AUTH_OK)
    }

    private suspend fun sendPackage(
        input: DataInputStream,
        output: DataOutputStream,
        descriptor: WirelessSessionDescriptor
    ) {
        val password = descriptor.secret.toCharArray()
        try {
            val export = migrationService.exportBytes(password, includedTypes).getOrThrow()
            WirelessFrameCodec.write(output, WirelessFrameCodec.PACKAGE, export.bytes)
            val result = WirelessFrameCodec.read(input)
            require(result.type == WirelessFrameCodec.RESULT) { "El iPhone no confirmó la importación" }
            val message = JSONObject(String(result.payload, Charsets.UTF_8)).optString(
                "message",
                "Transferencia completada"
            )
            mutableState.value = WirelessSyncServerState.Completed(message)
        } finally {
            password.fill('\u0000')
        }
    }

    private suspend fun receivePackage(
        input: DataInputStream,
        output: DataOutputStream,
        descriptor: WirelessSessionDescriptor
    ) {
        val frame = WirelessFrameCodec.read(input)
        require(frame.type == WirelessFrameCodec.PACKAGE) { "No se recibió ningún paquete" }
        val password = descriptor.secret.toCharArray()
        try {
            val preview = migrationService.previewBytes(frame.payload, password).getOrThrow()
            require(preview.errors.isEmpty()) { "El paquete contiene registros inválidos" }
            val summary = migrationService.importPreview(preview, ImportPolicy.NewerWins).getOrThrow()
            val message = summary.localizedMessage()
            val result = JSONObject()
                .put("inserted", summary.inserted)
                .put("updated", summary.updated)
                .put("skipped", summary.skipped)
                .put("conflicts", summary.conflicts)
                .put("errors", summary.errors)
                .put("message", message)
                .toString()
                .toByteArray(Charsets.UTF_8)
            WirelessFrameCodec.write(output, WirelessFrameCodec.RESULT, result)
            mutableState.value = WirelessSyncServerState.Completed(message)
        } finally {
            password.fill('\u0000')
            frame.payload.fill(0)
        }
    }

    private fun MigrationSummary.localizedMessage(): String {
        return "Importados: $inserted · Actualizados: $updated · Omitidos: $skipped · Conflictos: $conflicts"
    }

    private suspend fun registerService(name: String, sessionId: String, port: Int) {
        suspendCancellableCoroutine { continuation ->
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = name
                serviceType = WirelessSessionDescriptor.SERVICE_TYPE
                setPort(port)
                setAttribute("sid", sessionId)
                setAttribute("v", WirelessSessionDescriptor.VERSION.toString())
            }
            val listener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(registered: NsdServiceInfo) {
                    if (continuation.isActive) continuation.resume(Unit)
                }

                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(IllegalStateException("No se pudo anunciar el servicio ($errorCode)"))
                    }
                }

                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit
                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
            }
            registrationListener = listener
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
            continuation.invokeOnCancellation { unregisterService() }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun startLocalOnlyHotspot(): WirelessNetworkCredentials {
        require(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            "La conexión directa requiere Android 8 o posterior"
        }
        return suspendCancellableCoroutine { continuation ->
            val callback = object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                    hotspotReservation = reservation
                    val credentials = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val config = reservation.softApConfiguration
                        WirelessNetworkCredentials(config.ssid.orEmpty(), config.passphrase.orEmpty())
                    } else {
                        @Suppress("DEPRECATION")
                        val config = reservation.wifiConfiguration
                        @Suppress("DEPRECATION")
                        WirelessNetworkCredentials(config?.SSID.orEmpty(), config?.preSharedKey.orEmpty())
                    }
                    if (credentials.ssid.isBlank() || credentials.password.isBlank()) {
                        reservation.close()
                        if (continuation.isActive) {
                            continuation.resumeWithException(IllegalStateException("El sistema no proporcionó las credenciales del hotspot"))
                        }
                    } else if (continuation.isActive) {
                        continuation.resume(credentials)
                    }
                }

                override fun onFailed(reason: Int) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(IllegalStateException("No se pudo crear la conexión directa ($reason)"))
                    }
                }

                override fun onStopped() {
                    if (continuation.isActive) {
                        continuation.resumeWithException(IllegalStateException("La conexión directa se detuvo"))
                    }
                }
            }
            wifiManager.startLocalOnlyHotspot(callback, Handler(Looper.getMainLooper()))
            continuation.invokeOnCancellation { hotspotReservation?.close() }
        }
    }

    private fun unregisterService() {
        registrationListener?.let { listener ->
            runCatching { nsdManager.unregisterService(listener) }
        }
        registrationListener = null
    }

    override fun close() {
        unregisterService()
        runCatching { serverSocket?.close() }
        serverSocket = null
        hotspotReservation?.close()
        hotspotReservation = null
        scope.cancel()
    }

    companion object {
        private const val SESSION_TTL_SECONDS = 180L
        private const val SOCKET_TIMEOUT_MILLIS = 180_000
        private const val NONCE_BYTES = 16
    }
}
