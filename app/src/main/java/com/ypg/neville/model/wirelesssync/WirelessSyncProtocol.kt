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
    IosToAndroid("ios_to_android");

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
    val network: WirelessNetworkCredentials?
) {
    fun toQrPayload(): String {
        val json = JSONObject()
            .put("v", version)
            .put("sid", sessionId)
            .put("direction", direction.wireValue)
            .put("service", serviceName)
            .put("secret", secret)
            .put("expires", expiresAtEpochSeconds)
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
        const val VERSION = 1
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
