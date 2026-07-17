package com.ypg.neville.model.migration

import android.content.Context
import android.net.Uri
import com.ypg.neville.BuildConfig
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.StringReader
import java.util.UUID

class MyAppMigrationService(
    private val context: Context,
    private val crypto: MigrationCrypto = MigrationCrypto()
) {
    suspend fun exportToUri(uri: Uri, password: CharArray): Result<ExportResult> {
        return withContext(Dispatchers.IO) {
            runCatching {
                crypto.validateExportPassword(password)
                val db = NevilleRoomDatabase.getInstance(context.applicationContext)
                val bridge = NevilleMigrationRoomBridge(db)
                val records = bridge.exportRecords()
                writeRecords(uri, password, records)
            }
        }
    }

    suspend fun exportSelectedToUri(
        uri: Uri,
        password: CharArray,
        records: List<CanonicalRecord>
    ): Result<ExportResult> {
        return withContext(Dispatchers.IO) {
            runCatching {
                crypto.validateExportPassword(password)
                require(records.isNotEmpty()) { "Selecciona al menos un elemento para exportar" }
                writeRecords(uri, password, records)
            }
        }
    }

    suspend fun previewFromUri(uri: Uri, password: CharArray): Result<ImportPreview> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val encrypted = context.contentResolver.openInputStream(uri)?.use { it.readMigrationFile() }
                    ?: error("No se pudo abrir el archivo")
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
                val bridge = NevilleMigrationRoomBridge(NevilleRoomDatabase.getInstance(context.applicationContext))
                ImportPreview(
                    manifest = plainPackage.manifest,
                    records = records,
                    countsByType = counts,
                    conflicts = bridge.findConflicts(records),
                    errors = errors
                )
            }
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

    private fun writeRecords(
        uri: Uri,
        password: CharArray,
        records: List<CanonicalRecord>
    ): ExportResult {
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
        context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
            output.write(encrypted)
        } ?: error("No se pudo abrir el destino de exportación")
        return ExportResult(exportId, counts, encrypted.size)
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

    private fun InputStream.readMigrationFile(): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            if (read == 0) continue
            total += read
            require(total <= MigrationFormat.MAXIMUM_FILE_BYTES) {
                "El archivo supera el tamaño máximo permitido"
            }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
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
