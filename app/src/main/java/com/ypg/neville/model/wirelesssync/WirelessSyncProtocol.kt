package com.ypg.neville.model.wirelesssync

import android.util.Base64
import com.ypg.neville.model.migration.MigrationFormat
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

enum class WirelessSyncDirection(val wireValue: String) {
    AndroidToIos("android_to_ios"),
    IosToAndroid("ios_to_android"),
    Bidirectional("bidirectional");

    companion object {
        fun fromWire(value: String): WirelessSyncDirection = entries.first { it.wireValue == value }
    }
}

data class WirelessNetworkCredentials(
    val ssid: String,
    val password: String
)

data class WirelessSessionDescriptor(
    val version: Int,
    val sessionId: String,
    val direction: WirelessSyncDirection,
    val serviceName: String,
    val secret: String,
    val expiresAtEpochSeconds: Long,
    val network: WirelessNetworkCredentials?,
    val includedTypes: Set<String>?
) {
    fun toQrPayload(): String {
        val json = JSONObject()
            .put("v", version)
            .put("sid", sessionId)
            .put("direction", direction.wireValue)
            .put("service", serviceName)
            .put("secret", secret)
            .put("expires", expiresAtEpochSeconds)
        includedTypes?.let { types ->
            json.put("types", org.json.JSONArray(types.sorted()))
        }
        network?.let {
            json.put(
                "network",
                JSONObject()
                    .put("ssid", it.ssid)
                    .put("password", it.password)
                    .put("security", "wpa2")
            )
        }
        return "$URI_PREFIX${encodeUrl(json.toString().toByteArray(Charsets.UTF_8))}"
    }

    fun secretBytes(): ByteArray = decodeUrl(secret)

    companion object {
        const val VERSION = 2
        const val SERVICE_TYPE = "_nevillesync._tcp"
        const val URI_PREFIX = "neville://sync/"

        fun newSecret(random: SecureRandom = SecureRandom()): String {
            return encodeUrl(ByteArray(32).also(random::nextBytes))
        }

        fun parse(payload: String): WirelessSessionDescriptor {
            require(payload.startsWith(URI_PREFIX)) { "Código de enlace no reconocido" }
            val json = JSONObject(String(decodeUrl(payload.removePrefix(URI_PREFIX)), Charsets.UTF_8))
            require(json.getInt("v") == VERSION) { "Versión de enlace no soportada" }
            val networkJson = json.optJSONObject("network")
            return WirelessSessionDescriptor(
                version = json.getInt("v"),
                sessionId = json.getString("sid"),
                direction = WirelessSyncDirection.fromWire(json.getString("direction")),
                serviceName = json.getString("service"),
                secret = json.getString("secret"),
                expiresAtEpochSeconds = json.getLong("expires"),
                network = networkJson?.let {
                    WirelessNetworkCredentials(it.getString("ssid"), it.getString("password"))
                },
                includedTypes = json.optJSONArray("types")?.let { values ->
                    buildSet {
                        repeat(values.length()) { index -> add(values.getString(index)) }
                    }
                }
            ).also {
                require(it.secretBytes().size == 32) { "Clave de sesión inválida" }
            }
        }

        private fun encodeUrl(bytes: ByteArray): String = Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )

        private fun decodeUrl(value: String): ByteArray = Base64.decode(
            value,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }
}

data class WirelessImportResult(
    val inserted: Int,
    val updated: Int,
    val skipped: Int,
    val conflicts: Int,
    val errors: Int,
    val message: String
) {
    fun toJsonBytes(): ByteArray = JSONObject()
        .put("inserted", inserted)
        .put("updated", updated)
        .put("skipped", skipped)
        .put("conflicts", conflicts)
        .put("errors", errors)
        .put("message", message)
        .toString()
        .toByteArray(Charsets.UTF_8)

    companion object {
        val Empty = WirelessImportResult(0, 0, 0, 0, 0, "")

        fun from(summary: com.ypg.neville.model.migration.MigrationSummary, message: String) =
            WirelessImportResult(
                summary.inserted,
                summary.updated,
                summary.skipped,
                summary.conflicts,
                summary.errors,
                message
            )

        fun fromJson(bytes: ByteArray): WirelessImportResult {
            val json = JSONObject(String(bytes, Charsets.UTF_8))
            return WirelessImportResult(
                inserted = json.getInt("inserted"),
                updated = json.getInt("updated"),
                skipped = json.getInt("skipped"),
                conflicts = json.getInt("conflicts"),
                errors = json.getInt("errors"),
                message = json.optString("message")
            )
        }
    }
}

data class WirelessTransferSummary(
    val direction: WirelessSyncDirection,
    val sent: Int,
    val received: Int,
    val localImport: WirelessImportResult,
    val remoteImport: WirelessImportResult
) {
    val totalInserted: Int get() = localImport.inserted + remoteImport.inserted
    val totalUpdated: Int get() = localImport.updated + remoteImport.updated
    val totalSkipped: Int get() = localImport.skipped + remoteImport.skipped
    val totalConflicts: Int get() = localImport.conflicts + remoteImport.conflicts
    val totalErrors: Int get() = localImport.errors + remoteImport.errors
}

internal object WirelessFrameCodec {
    const val HELLO: Int = 1
    const val CHALLENGE: Int = 2
    const val AUTH: Int = 3
    const val AUTH_OK: Int = 4
    const val PACKAGE: Int = 5
    const val RESULT: Int = 6
    const val ERROR: Int = 7

    data class Frame(val type: Int, val payload: ByteArray)

    fun write(output: DataOutputStream, type: Int, payload: ByteArray = byteArrayOf()) {
        require(payload.size <= MigrationFormat.MAXIMUM_FILE_BYTES) { "Mensaje demasiado grande" }
        output.writeByte(type)
        output.writeInt(payload.size)
        output.write(payload)
        output.flush()
    }

    fun read(input: DataInputStream): Frame {
        val type = input.readUnsignedByte()
        val length = input.readInt()
        require(length in 0..MigrationFormat.MAXIMUM_FILE_BYTES) { "Longitud de mensaje inválida" }
        return Frame(type, ByteArray(length).also(input::readFully))
    }
}

internal object WirelessSessionAuthenticator {
    private const val CONTEXT = "neville-sync-v1"

    fun proof(
        secret: ByteArray,
        sessionId: String,
        clientNonce: ByteArray,
        serverNonce: ByteArray
    ): ByteArray {
        val message = buildList {
            add(CONTEXT.toByteArray(Charsets.UTF_8))
            add(sessionId.toByteArray(Charsets.UTF_8))
            add(clientNonce)
            add(serverNonce)
        }.fold(ByteArray(0)) { accumulated, item ->
            accumulated + byteArrayOf(0) + item
        }
        return Mac.getInstance("HmacSHA256").run {
            init(SecretKeySpec(secret, "HmacSHA256"))
            doFinal(message)
        }
    }

    fun matches(expected: ByteArray, actual: ByteArray): Boolean {
        return MessageDigest.isEqual(expected, actual)
    }
}
