package com.ypg.neville.model.backup

import android.content.Context
import android.provider.DocumentsContract
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.ypg.neville.model.preferences.DbPreferences
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipException
import java.util.zip.ZipOutputStream
import javax.crypto.AEADBadTagException

class CloudBackupManager(private val context: Context) {

    enum class BackupFrequency {
        MANUAL,
        DAILY,
        WEEKLY;

        companion object {
            fun from(value: String?): BackupFrequency {
                return entries.firstOrNull { it.name == value } ?: MANUAL
            }
        }
    }

    enum class ProviderDestinationKind(val storageValue: String) {
        TREE("tree"),
        DOCUMENT("document");

        companion object {
            fun from(storedValue: String?, uri: Uri): ProviderDestinationKind {
                if (storedValue == TREE.storageValue) return TREE
                if (storedValue == DOCUMENT.storageValue) return DOCUMENT
                return if (DocumentsContract.isTreeUri(uri)) TREE else DOCUMENT
            }
        }
    }

    data class ProviderInfo(
        val uri: String,
        val authority: String,
        val displayName: String,
        val destinationKind: ProviderDestinationKind
    )

    sealed class BackupResult {
        data class Success(val fileName: String, val timestampMs: Long) : BackupResult()
        data class Error(val reason: String) : BackupResult()
    }

    sealed class RestoreResult {
        data class Success(val restoredAtMs: Long) : RestoreResult()
        data class Error(val reason: String) : RestoreResult()
    }

    private val prefs by lazy { DbPreferences.default(context) }
    private val backupCrypto by lazy { BackupCrypto() }
    private val passphraseStore by lazy { BackupPassphraseStore(context.applicationContext) }

    fun getProviderInfo(): ProviderInfo? {
        val uri = prefs.getString(KEY_PROVIDER_URI, null) ?: return null
        val providerUri = uri.toUri()
        val authority = prefs.getString(KEY_PROVIDER_AUTHORITY, null) ?: "Proveedor"
        val displayName = prefs.getString(KEY_PROVIDER_NAME, null) ?: authority
        val destinationKind = ProviderDestinationKind.from(
            prefs.getString(KEY_PROVIDER_KIND, null),
            providerUri
        )
        return ProviderInfo(
            uri = uri,
            authority = authority,
            displayName = displayName,
            destinationKind = destinationKind
        )
    }

    fun connectProvider(providerUri: Uri): Result<ProviderInfo> {
        return runCatching {
            val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(providerUri, flags)
            } catch (securityException: SecurityException) {
                Log.w(TAG, "No se pudo persistir permiso URI: $providerUri", securityException)
            }

            val destinationKind = ProviderDestinationKind.from(null, providerUri)
            val authority = providerUri.authority.orEmpty().ifBlank { "Proveedor" }
            val displayName = when (destinationKind) {
                ProviderDestinationKind.TREE -> {
                    val root = DocumentFile.fromTreeUri(context, providerUri)
                        ?: throw IllegalStateException("No se pudo abrir la carpeta elegida")
                    if (!root.canWrite()) {
                        throw IllegalStateException("La carpeta elegida no permite escritura")
                    }
                    root.name?.takeIf { it.isNotBlank() } ?: authority
                }
                ProviderDestinationKind.DOCUMENT -> {
                    val document = DocumentFile.fromSingleUri(context, providerUri)
                        ?: throw IllegalStateException("No se pudo abrir el archivo de backup elegido")
                    if (!document.isFile) {
                        throw IllegalStateException("Debes seleccionar un archivo de backup válido")
                    }
                    if (!document.canWrite()) {
                        throw IllegalStateException("El archivo elegido no permite escritura")
                    }
                    document.name?.takeIf { it.isNotBlank() } ?: BACKUP_FILE_NAME
                }
            }

            prefs.edit {
                putString(KEY_PROVIDER_URI, providerUri.toString())
                putString(KEY_PROVIDER_AUTHORITY, authority)
                putString(KEY_PROVIDER_NAME, displayName)
                putString(KEY_PROVIDER_KIND, destinationKind.storageValue)
            }

            CloudBackupScheduler.sync(context.applicationContext)
            ProviderInfo(
                uri = providerUri.toString(),
                authority = authority,
                displayName = displayName,
                destinationKind = destinationKind
            )
        }
    }

    fun disconnectProvider() {
        val oldUri = prefs.getString(KEY_PROVIDER_URI, null)
        if (!oldUri.isNullOrBlank()) {
            try {
                context.contentResolver.releasePersistableUriPermission(
                    oldUri.toUri(),
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Si el permiso ya no existe, seguimos con limpieza de estado.
            }
        }

        prefs.edit {
            remove(KEY_PROVIDER_URI)
            remove(KEY_PROVIDER_AUTHORITY)
            remove(KEY_PROVIDER_NAME)
            remove(KEY_PROVIDER_KIND)
        }
        CloudBackupScheduler.sync(context.applicationContext)
    }

    fun getFrequency(): BackupFrequency {
        return BackupFrequency.from(prefs.getString(KEY_BACKUP_FREQUENCY, BackupFrequency.MANUAL.name))
    }

    fun setFrequency(frequency: BackupFrequency): Result<Unit> {
        return runCatching {
            if (frequency != BackupFrequency.MANUAL && !hasSavedPassphrase()) {
                throw IllegalStateException(
                    "Configura primero la clave de cifrado para habilitar backups automáticos"
                )
            }
            prefs.edit { putString(KEY_BACKUP_FREQUENCY, frequency.name) }
            CloudBackupScheduler.sync(context.applicationContext)
        }
    }

    fun getLastBackupTimestamp(): Long {
        return prefs.getLong(KEY_LAST_BACKUP_AT, 0L)
    }

    fun getLastBackupError(): String? {
        return prefs.getString(KEY_LAST_BACKUP_ERROR, null)
    }

    fun hasSavedPassphrase(): Boolean {
        return passphraseStore.hasPassphrase()
    }

    fun savePassphrase(passphrase: String): Result<Unit> {
        return runCatching {
            require(passphrase.length >= MIN_PASSPHRASE_LENGTH) {
                "La clave debe tener al menos $MIN_PASSPHRASE_LENGTH caracteres"
            }
            passphraseStore.save(passphrase)
        }
    }

    fun updatePassphrase(currentPassphrase: String, newPassphrase: String): Result<Unit> {
        return runCatching {
            require(currentPassphrase.isNotBlank()) { "Debes ingresar la clave actual" }
            require(newPassphrase.length >= MIN_PASSPHRASE_LENGTH) {
                "La nueva clave debe tener al menos $MIN_PASSPHRASE_LENGTH caracteres"
            }
            val currentStored = passphraseStore.get()
                ?: throw IllegalStateException("No hay clave configurada")
            if (currentStored != currentPassphrase) {
                throw IllegalStateException("La clave actual no es correcta")
            }
            passphraseStore.save(newPassphrase)
        }
    }

    fun deletePassphrase(currentPassphrase: String): Result<Unit> {
        return runCatching {
            require(currentPassphrase.isNotBlank()) { "Debes ingresar la clave actual" }
            val currentStored = passphraseStore.get()
                ?: throw IllegalStateException("No hay clave configurada")
            if (currentStored != currentPassphrase) {
                throw IllegalStateException("La clave actual no es correcta")
            }
            passphraseStore.clear()
        }
    }

    @Suppress("unused")
    fun clearSavedPassphrase() {
        passphraseStore.clear()
    }

    fun getPassphraseForBiometricRecovery(): Result<String> {
        return runCatching {
            passphraseStore.get()
                ?: throw IllegalStateException("No hay clave de cifrado registrada")
        }
    }

    suspend fun backupNow(): BackupResult = withContext(Dispatchers.IO) {
        try {
            val passphrase = passphraseStore.get()
                ?: return@withContext BackupResult.Error(
                    "Configura primero la clave de cifrado para habilitar backups"
                )

            val provider = getProviderInfo()
                ?: return@withContext BackupResult.Error("No hay proveedor conectado")

            checkpointRoom()

            val dbFile = getMainDatabaseFile()
            if (!dbFile.exists()) {
                return@withContext BackupResult.Error("No se encontró la base de datos local")
            }

            val now = System.currentTimeMillis()
            val result = when (provider.destinationKind) {
                ProviderDestinationKind.TREE -> {
                    backupToTree(
                        treeUri = provider.uri.toUri(),
                        dbFile = dbFile,
                        passphrase = passphrase,
                        timestampMs = now
                    )
                }
                ProviderDestinationKind.DOCUMENT -> {
                    backupToDocument(
                        documentUri = provider.uri.toUri(),
                        dbFile = dbFile,
                        passphrase = passphrase,
                        timestampMs = now
                    )
                }
            }
            if (result is BackupResult.Error) {
                return@withContext result
            }

            prefs.edit {
                putLong(KEY_LAST_BACKUP_AT, now)
                remove(KEY_LAST_BACKUP_ERROR)
            }
            result
        } catch (t: Throwable) {
            val message = t.message ?: "Error inesperado durante el backup"
            Log.e(TAG, "backupNow failed", t)
            prefs.edit { putString(KEY_LAST_BACKUP_ERROR, message) }
            BackupResult.Error(message)
        }
    }

    suspend fun restoreFromBackup(
        backupUri: Uri,
        passphraseInput: String? = null
    ): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val passphraseForRestore = passphraseInput?.trim().orEmpty().ifBlank {
                passphraseStore.get().orEmpty()
            }

            NevilleRoomDatabase.closeInstance()

            val dbFile = getMainDatabaseFile()
            dbFile.parentFile?.mkdirs()
            val walFile = dbFile.resolveSibling("$DB_FILE_NAME-wal")
            val shmFile = dbFile.resolveSibling("$DB_FILE_NAME-shm")

            val tempDb = File.createTempFile("neville_restore_", ".db", context.cacheDir)
            val tempWal = File.createTempFile("neville_restore_", ".wal", context.cacheDir)
            val tempShm = File.createTempFile("neville_restore_", ".shm", context.cacheDir)

            var hasDb = false
            var hasWal = false
            var hasShm = false

            context.contentResolver.openInputStream(backupUri)?.use { input ->
                backupCrypto
                    .openPayloadInputStream(input, passphraseForRestore.toCharArray())
                    .use { payload ->
                        ZipInputStream(payload).use { zip ->
                            var entry: ZipEntry? = zip.nextEntry
                            while (entry != null) {
                                if (!entry.isDirectory) {
                                    when (entry.name) {
                                        DB_FILE_NAME -> {
                                            writeEntry(zip, tempDb)
                                            hasDb = true
                                        }
                                        "$DB_FILE_NAME-wal" -> {
                                            writeEntry(zip, tempWal)
                                            hasWal = true
                                        }
                                        "$DB_FILE_NAME-shm" -> {
                                            writeEntry(zip, tempShm)
                                            hasShm = true
                                        }
                                    }
                                }
                                zip.closeEntry()
                                entry = zip.nextEntry
                            }
                        }
                    }
            } ?: return@withContext RestoreResult.Error("No se pudo abrir el archivo de backup")

            if (!hasDb) {
                tempDb.delete()
                tempWal.delete()
                tempShm.delete()
                return@withContext RestoreResult.Error("El backup no contiene la base de datos principal")
            }

            safeDelete(dbFile)
            safeDelete(walFile)
            safeDelete(shmFile)

            tempDb.copyTo(dbFile, overwrite = true)
            if (hasWal) {
                tempWal.copyTo(walFile, overwrite = true)
            }
            if (hasShm) {
                tempShm.copyTo(shmFile, overwrite = true)
            }

            tempDb.delete()
            tempWal.delete()
            tempShm.delete()

            // Durante la restauración la UI puede reabrir Room (prefs/listas) y quedarse
            // apuntando al archivo previo. Cerramos otra vez para forzar nueva conexión.
            NevilleRoomDatabase.closeInstance()

            // Valida que Room pueda reabrirse tras restaurar.
            NevilleRoomDatabase.getInstance(context.applicationContext).openHelper.writableDatabase

            val now = System.currentTimeMillis()
            prefs.edit {
                putLong(KEY_LAST_RESTORE_AT, now)
                remove(KEY_LAST_BACKUP_ERROR)
            }
            BackupRestoreSignal.notifyDataRestored()
            RestoreResult.Success(restoredAtMs = now)
        } catch (t: Throwable) {
            Log.e(TAG, "restoreFromBackup failed", t)
            val message = when {
                t.hasCause<AEADBadTagException>() ->
                    if (!passphraseInput.isNullOrBlank()) {
                        "La contraseña de cifrado es incorrecta."
                    } else {
                        "No se pudo descifrar el backup. Verifica la clave de cifrado y que el archivo no esté corrupto."
                    }
                t.hasCause<ZipException>() ->
                    "El archivo de backup no tiene un formato válido o está dañado."
                else -> t.message ?: "Error inesperado durante la restauración"
            }
            RestoreResult.Error(message)
        }
    }

    fun formatTimestamp(timestampMs: Long): String {
        if (timestampMs <= 0L) {
            return "Nunca"
        }
        val formatter = SimpleDateFormat(HUMAN_DATE_PATTERN, Locale.getDefault())
        formatter.timeZone = TimeZone.getDefault()
        return formatter.format(Date(timestampMs))
    }

    private fun checkpointRoom() {
        try {
            val db = NevilleRoomDatabase.getInstance(context.applicationContext).openHelper.writableDatabase
            db.query("PRAGMA wal_checkpoint(FULL)").close()
        } catch (t: Throwable) {
            Log.w(TAG, "checkpointRoom failed", t)
        }
    }

    private fun getMainDatabaseFile(): File {
        return context.getDatabasePath(DB_FILE_NAME)
    }

    private fun addFileToZip(zip: ZipOutputStream, file: File, entryName: String) {
        zip.putNextEntry(ZipEntry(entryName))
        file.inputStream().use { input ->
            input.copyTo(zip, DEFAULT_BUFFER_SIZE)
        }
        zip.closeEntry()
    }

    private fun addOptionalCompanion(zip: ZipOutputStream, file: File, entryName: String) {
        if (!file.exists()) {
            return
        }
        addFileToZip(zip, file, entryName)
    }

    private fun backupToTree(
        treeUri: Uri,
        dbFile: File,
        passphrase: String,
        timestampMs: Long
    ): BackupResult {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: return BackupResult.Error("No se pudo acceder al proveedor")
        if (!root.canWrite()) {
            return BackupResult.Error("No hay permiso de escritura en el proveedor")
        }

        removePreviousBackups(root)
        val backupName = BACKUP_FILE_NAME
        val outFile = root.createFile("application/octet-stream", backupName)
            ?: return BackupResult.Error("No se pudo crear el archivo de backup")

        context.contentResolver.openOutputStream(outFile.uri)?.use { output ->
            writeEncryptedBackup(output, dbFile, passphrase)
        } ?: return BackupResult.Error("No se pudo abrir el destino del backup")

        return BackupResult.Success(fileName = backupName, timestampMs = timestampMs)
    }

    private fun backupToDocument(
        documentUri: Uri,
        dbFile: File,
        passphrase: String,
        timestampMs: Long
    ): BackupResult {
        val document = DocumentFile.fromSingleUri(context, documentUri)
            ?: return BackupResult.Error("No se pudo acceder al archivo de backup")
        if (!document.isFile) {
            return BackupResult.Error("El destino configurado no es un archivo")
        }
        if (!document.canWrite()) {
            return BackupResult.Error("No hay permiso de escritura en el archivo destino")
        }

        val output = openTruncatedDocumentOutputStream(documentUri)
            ?: return BackupResult.Error("No se pudo abrir el archivo de backup")

        output.use {
            writeEncryptedBackup(it, dbFile, passphrase)
        }

        val backupName = document.name?.takeIf { it.isNotBlank() } ?: BACKUP_FILE_NAME
        return BackupResult.Success(fileName = backupName, timestampMs = timestampMs)
    }

    private fun writeEncryptedBackup(output: java.io.OutputStream, dbFile: File, passphrase: String) {
        backupCrypto
            .openEncryptedPayloadOutputStream(output, passphrase.toCharArray())
            .use { encryptedPayload ->
                ZipOutputStream(encryptedPayload).use { zip ->
                    addFileToZip(zip, dbFile, DB_FILE_NAME)
                    addOptionalCompanion(zip, dbFile.resolveSibling("$DB_FILE_NAME-wal"), "$DB_FILE_NAME-wal")
                    addOptionalCompanion(zip, dbFile.resolveSibling("$DB_FILE_NAME-shm"), "$DB_FILE_NAME-shm")
                }
            }
    }

    private fun openTruncatedDocumentOutputStream(documentUri: Uri): OutputStream? {
        // Prioriza FileDescriptor para asegurar truncado real en providers cloud.
        context.contentResolver.openFileDescriptor(documentUri, "rwt")?.let { pfd ->
            return object : OutputStream() {
                private val stream = FileOutputStream(pfd.fileDescriptor)
                override fun write(b: Int) = stream.write(b)
                override fun write(b: ByteArray) = stream.write(b)
                override fun write(b: ByteArray, off: Int, len: Int) = stream.write(b, off, len)
                override fun flush() = stream.flush()
                override fun close() {
                    stream.flush()
                    stream.close()
                    pfd.close()
                }
            }
        }

        context.contentResolver.openFileDescriptor(documentUri, "rw")?.let { pfd ->
            return object : OutputStream() {
                private val stream = FileOutputStream(pfd.fileDescriptor).also { output ->
                    try {
                        output.channel.truncate(0L)
                        output.channel.position(0L)
                    } catch (_: Throwable) {
                        // Si el provider no permite truncate explícito, continuamos con fallback.
                    }
                }
                override fun write(b: Int) = stream.write(b)
                override fun write(b: ByteArray) = stream.write(b)
                override fun write(b: ByteArray, off: Int, len: Int) = stream.write(b, off, len)
                override fun flush() = stream.flush()
                override fun close() {
                    stream.flush()
                    stream.close()
                    pfd.close()
                }
            }
        }

        return context.contentResolver.openOutputStream(documentUri, "wt")
            ?: context.contentResolver.openOutputStream(documentUri, "w")
    }

    private inline fun <reified T : Throwable> Throwable.hasCause(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is T) return true
            current = current.cause
        }
        return false
    }

    private fun writeEntry(zip: ZipInputStream, target: File) {
        FileOutputStream(target).use { output ->
            zip.copyTo(output, DEFAULT_BUFFER_SIZE)
        }
    }

    private fun safeDelete(file: File) {
        if (file.exists() && !file.delete()) {
            throw IOException("No se pudo reemplazar ${file.name}")
        }
    }

    private fun removePreviousBackups(root: DocumentFile) {
        root.listFiles()
            .filter { file ->
                file.isFile && file.name?.lowercase(Locale.ROOT)?.endsWith(BACKUP_FILE_EXTENSION) == true
            }
            .forEach { existing ->
                if (!existing.delete()) {
                    throw IOException("No se pudo eliminar backup anterior: ${existing.name}")
                }
            }
    }

    companion object {
        private const val TAG = "CloudBackupManager"
        private const val DB_FILE_NAME = "neville_room.db"
        private const val MIN_PASSPHRASE_LENGTH = 12
        private const val BACKUP_FILE_NAME = "neville_backup_latest.nvbak"
        private const val BACKUP_FILE_EXTENSION = ".nvbak"

        const val KEY_PROVIDER_URI = "cloud_backup_provider_uri"
        const val KEY_PROVIDER_AUTHORITY = "cloud_backup_provider_authority"
        const val KEY_PROVIDER_NAME = "cloud_backup_provider_name"
        const val KEY_PROVIDER_KIND = "cloud_backup_provider_kind"
        const val KEY_BACKUP_FREQUENCY = "cloud_backup_frequency"
        const val KEY_LAST_BACKUP_AT = "cloud_backup_last_backup_at"
        const val KEY_LAST_RESTORE_AT = "cloud_backup_last_restore_at"
        const val KEY_LAST_BACKUP_ERROR = "cloud_backup_last_error"

        private const val HUMAN_DATE_PATTERN = "yyyy-MM-dd HH:mm"
    }
}
