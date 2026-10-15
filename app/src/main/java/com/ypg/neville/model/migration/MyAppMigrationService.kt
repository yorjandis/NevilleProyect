package com.ypg.neville.model.migration

import android.content.Context
import com.ypg.neville.BuildConfig
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.StringReader
import java.util.UUID

class MyAppMigrationService(
    private val context: Context,
    private val crypto: MigrationCrypto = MigrationCrypto()
) {
    suspend fun exportBytes(
        password: CharArray,
        includedTypes: Set<String>? = null,
        allowEmpty: Boolean = false
    ): Result<EncryptedExport> {
        return withContext(Dispatchers.IO) {
            runCatching {
                crypto.validateExportPassword(password)
                val bridge = NevilleMigrationRoomBridge(
                    NevilleRoomDatabase.getInstance(context.applicationContext)
                )
                val records = bridge.exportRecords().let { all ->
                    if (includedTypes.isNullOrEmpty()) all else all.filter {
                        it.type in includedTypes ||
                            (it.type == NevilleMigrationRoomBridge.TYPE_DIARY_ATTACHMENT &&
                                NevilleMigrationRoomBridge.TYPE_DIARY in includedTypes)
                    }
                }
                require(allowEmpty || records.isNotEmpty()) { "No hay elementos para transferir" }
                encryptRecords(password, records)
            }
        }
    }

    suspend fun previewBytes(
        encrypted: ByteArray,
        password: CharArray
    ): Result<ImportPreview> {
        return withContext(Dispatchers.IO) {
            runCatching { buildPreview(encrypted, password) }
        }
    }

    suspend fun importPreview(
        preview: ImportPreview,
        policy: ImportPolicy = ImportPolicy.SkipExisting
    ): Result<MigrationSummary> {
        return withContext(Dispatchers.IO) {
            runCatching {
                require(preview.errors.isEmpty()) { "Hay errores de validación pendientes" }
                val db = NevilleRoomDatabase.getInstance(context.applicationContext)
                NevilleMigrationRoomBridge(db).importRecords(preview.records, policy)
            }
        }
    }

    private fun buildManifest(
        exportId: String,
        countsByType: Map<String, Int>,
        parameters: MigrationCrypto.Parameters
    ): JSONObject {
        val summary = JSONObject()
        countsByType.forEach { (type, count) -> summary.put(type, count) }
        return JSONObject()
            .put("format", MigrationFormat.FORMAT_NAME)
            .put("formatVersion", MigrationFormat.FORMAT_VERSION)
            .put("createdAt", MigrationFormat.utcNow())
            .put("sourcePlatform", "android")
            .put("sourceAppVersion", BuildConfig.VERSION_NAME)
            .put("schemaVersion", MigrationFormat.SCHEMA_VERSION)
            .put("contentSummary", summary)
            .put("encryption", crypto.encryptionJson(parameters))
            .put("exportId", exportId)
    }

    private fun encryptRecords(
        password: CharArray,
        records: List<CanonicalRecord>
    ): EncryptedExport {
        val counts = records.groupingBy { it.type }.eachCount().toSortedMap()
        val exportId = UUID.randomUUID().toString()
        val parameters = crypto.newParameters()
        val manifest = buildManifest(exportId, counts, parameters)
        val ndjson = records.joinToString(separator = "\n", postfix = "\n") { it.toJsonLine() }
        val plaintext = MigrationFormat.packPlaintext(manifest, ndjson)
        val encrypted = try {
            crypto.encrypt(plaintext, password, parameters)
        } finally {
            plaintext.fill(0)
        }
        return EncryptedExport(exportId, counts, encrypted)
    }

    private fun buildPreview(encrypted: ByteArray, password: CharArray): ImportPreview {
        require(encrypted.size <= MigrationFormat.MAXIMUM_FILE_BYTES) {
            "El paquete supera el tamaño máximo permitido"
        }
        val plainPackage = crypto.decrypt(encrypted, password)
        validateManifest(plainPackage.manifest)
        val records = mutableListOf<CanonicalRecord>()
        val errors = mutableListOf<String>()
        BufferedReader(StringReader(plainPackage.ndjson)).useLines { lines ->
            lines.forEachIndexed { index, rawLine ->
                val line = rawLine.trim()
                if (line.isNotEmpty()) {
                    runCatching { CanonicalRecord.fromJsonLine(line) }
                        .onSuccess(records::add)
                        .onFailure { errors += "Línea ${index + 1}: ${it.message ?: "registro inválido"}" }
                }
            }
        }
        val counts = records.groupingBy { it.type }.eachCount().toSortedMap()
        val manifestCounts = plainPackage.manifest.getJSONObject("contentSummary")
        val declaredCounts = buildMap {
            val keys = manifestCounts.keys()
            while (keys.hasNext()) {
                val type = keys.next()
                val count = intField(manifestCounts, type)
                require(count >= 0) { "Recuento negativo para $type" }
                put(type, count)
            }
        }.toSortedMap()
        require(declaredCounts == counts) { "El manifiesto no coincide con data.ndjson" }
        val bridge = NevilleMigrationRoomBridge(
            NevilleRoomDatabase.getInstance(context.applicationContext)
        )
        return ImportPreview(
            manifest = plainPackage.manifest,
            records = records,
            countsByType = counts,
            conflicts = bridge.findConflicts(records),
            errors = errors
        )
    }

    private fun validateManifest(manifest: JSONObject) {
        require(stringField(manifest, "format") == MigrationFormat.FORMAT_NAME) { "Formato no soportado" }
        require(intField(manifest, "formatVersion") == MigrationFormat.FORMAT_VERSION) { "Versión de formato no soportada" }
        require(intField(manifest, "schemaVersion") <= MigrationFormat.SCHEMA_VERSION) { "Schema no soportado" }
        require(stringField(manifest, "sourcePlatform") in setOf("ios", "android")) { "Plataforma de origen inválida" }
        val encryption = manifest.getJSONObject("encryption")
        require(stringField(encryption, "cipher") == MigrationFormat.CIPHER) { "Cifrado no soportado" }
        require(stringField(encryption, "kdf") == MigrationFormat.KDF) { "KDF no soportado" }
        require(intField(encryption, "kdfVersion") == MigrationFormat.KDF_VERSION &&
            intField(encryption, "kdfMemoryKiB") == MigrationFormat.KDF_MEMORY_KIB &&
            intField(encryption, "kdfIterations") == MigrationFormat.KDF_ITERATIONS &&
            intField(encryption, "kdfParallelism") == MigrationFormat.KDF_PARALLELISM
        ) { "Parámetros Argon2id no soportados" }
        require(stringField(encryption, "passwordNormalization") == MigrationFormat.PASSWORD_NORMALIZATION) {
            "Normalización de contraseña no soportada"
        }
        require(intField(encryption, "keyLengthBits") == MigrationFormat.KEY_BITS) {
            "Longitud de clave no soportada"
        }
        require(intField(encryption, "tagLengthBits") == MigrationFormat.TAG_BITS) {
            "Etiqueta GCM no soportada"
        }
        require(manifest.getJSONObject("contentSummary").length() >= 0) { "Resumen inválido" }
        require(stringField(manifest, "exportId").isNotBlank()) { "exportId vacío" }
    }

    private fun stringField(json: JSONObject, key: String): String {
        return json.opt(key) as? String
            ?: throw IllegalArgumentException("$key debe ser una cadena JSON")
    }

    private fun intField(json: JSONObject, key: String): Int {
        return when (val value = json.opt(key)) {
            is Int -> value
            is Long -> {
                require(value in Int.MIN_VALUE..Int.MAX_VALUE) { "$key está fuera de rango" }
                value.toInt()
            }
            else -> throw IllegalArgumentException("$key debe ser un número entero JSON")
        }
    }
}
