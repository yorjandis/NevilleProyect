package com.ypg.neville.model.migration

import org.json.JSONObject

data class CanonicalRecord(
    val type: String,
    val id: String,
    val createdAt: String,
    val updatedAt: String,
    val schemaVersion: Int = MigrationFormat.SCHEMA_VERSION,
    val payload: JSONObject
) {
    fun toJsonLine(): String {
        return JSONObject()
            .put("type", type)
            .put("id", id)
            .put("createdAt", createdAt)
            .put("updatedAt", updatedAt)
            .put("schemaVersion", schemaVersion)
            .put("payload", payload)
            .toString()
    }

    fun contentFingerprint(): String {
        return MigrationIds.sha256Hex("$type|$createdAt|${payload.toString()}")
    }

    companion object {
        fun fromJsonLine(line: String): CanonicalRecord {
            val json = JSONObject(line)
            val type = json.getString("type").trim()
            val id = json.getString("id").trim()
            val createdAt = json.getString("createdAt").trim()
            val updatedAt = json.getString("updatedAt").trim()
            val schemaVersion = json.optInt("schemaVersion", MigrationFormat.SCHEMA_VERSION)
            val payload = json.getJSONObject("payload")
            require(type.isNotBlank()) { "type vacío" }
            require(id.isNotBlank()) { "id vacío" }
            MigrationFormat.isoToMillis(createdAt)
            MigrationFormat.isoToMillis(updatedAt)
            require(schemaVersion <= MigrationFormat.SCHEMA_VERSION) {
                "schemaVersion no soportado: $schemaVersion"
            }
            return CanonicalRecord(type, id, createdAt, updatedAt, schemaVersion, payload)
        }
    }
}

data class ImportConflict(
    val type: String,
    val id: String,
    val reason: String
)

data class MigrationSummary(
    val inserted: Int = 0,
    val updated: Int = 0,
    val skipped: Int = 0,
    val conflicts: Int = 0,
    val errors: Int = 0
) {
    operator fun plus(other: MigrationSummary): MigrationSummary {
        return MigrationSummary(
            inserted = inserted + other.inserted,
            updated = updated + other.updated,
            skipped = skipped + other.skipped,
            conflicts = conflicts + other.conflicts,
            errors = errors + other.errors
        )
    }
}

data class ExportResult(
    val exportId: String,
    val countsByType: Map<String, Int>,
    val bytesWritten: Int
)

data class ImportPreview(
    val manifest: JSONObject,
    val records: List<CanonicalRecord>,
    val countsByType: Map<String, Int>,
    val conflicts: List<ImportConflict>,
    val errors: List<String>
)

enum class ImportPolicy {
    SkipExisting,
    OverwriteExisting
}
