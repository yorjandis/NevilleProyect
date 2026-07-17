package com.ypg.neville.model.migration

import java.security.MessageDigest
import java.util.Locale

object MigrationIds {
    fun stableId(type: String, vararg parts: Any?): String {
        val source = buildString {
            append(type.lowercase(Locale.US))
            parts.forEach { part ->
                append('|')
                append(part?.toString()?.trim().orEmpty())
            }
        }
        return "sha256:${sha256Hex(source)}"
    }

    fun stableLongId(type: String, vararg parts: Any?): Long {
        val hex = stableId(type, *parts).removePrefix("sha256:")
        val unsigned = hex.take(15).toLong(16)
        return unsigned.coerceAtLeast(1L)
    }

    fun sha256Hex(value: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
