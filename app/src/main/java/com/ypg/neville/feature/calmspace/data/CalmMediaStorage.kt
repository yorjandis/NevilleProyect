package com.ypg.neville.feature.calmspace.data

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID

object CalmMediaStorage {

    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp")
    private val audioExtensions = setOf("mp3", "ogg", "wav", "m4a", "aac")

    private const val BACKGROUND_TOKEN_PREFIX = "custom_bg:"
    private const val MUSIC_TOKEN_PREFIX = "custom_music:"

    fun customBackgroundsDir(context: Context): File {
        return File(calmDocumentsRoot(context), "backgrounds").apply { mkdirs() }
    }

    fun customMusicDir(context: Context): File {
        return File(calmDocumentsRoot(context), "music").apply { mkdirs() }
    }

    fun listCustomBackgroundFiles(context: Context): List<File> {
        return customBackgroundsDir(context)
            .listFiles()
            .orEmpty()
            .filter { it.isFile && extensionOf(it.name) in imageExtensions }
            .sortedByDescending { it.lastModified() }
    }

    fun listCustomMusicFiles(context: Context): List<File> {
        return customMusicDir(context)
            .listFiles()
            .orEmpty()
            .filter { it.isFile && extensionOf(it.name) in audioExtensions }
            .sortedByDescending { it.lastModified() }
    }

    fun importImage(context: Context, sourceUri: Uri): Result<File> {
        return copyUriToDir(
            context = context,
            sourceUri = sourceUri,
            targetDir = customBackgroundsDir(context),
            defaultBase = "calma_custom_bg",
            allowedExtensions = imageExtensions
        )
    }

    fun importMusic(context: Context, sourceUri: Uri): Result<File> {
        return copyUriToDir(
            context = context,
            sourceUri = sourceUri,
            targetDir = customMusicDir(context),
            defaultBase = "calma_custom_music",
            allowedExtensions = audioExtensions
        )
    }

    fun toBackgroundToken(file: File): String = "$BACKGROUND_TOKEN_PREFIX${file.absolutePath}"
    fun toMusicToken(file: File): String = "$MUSIC_TOKEN_PREFIX${file.absolutePath}"

    fun customBackgroundPathFromToken(token: String): String? {
        if (!token.startsWith(BACKGROUND_TOKEN_PREFIX)) return null
        return token.removePrefix(BACKGROUND_TOKEN_PREFIX)
    }

    fun customMusicPathFromToken(token: String): String? {
        if (!token.startsWith(MUSIC_TOKEN_PREFIX)) return null
        return token.removePrefix(MUSIC_TOKEN_PREFIX)
    }

    fun isCustomBackgroundToken(token: String): Boolean = token.startsWith(BACKGROUND_TOKEN_PREFIX)
    fun isCustomMusicToken(token: String): Boolean = token.startsWith(MUSIC_TOKEN_PREFIX)

    private fun calmDocumentsRoot(context: Context): File {
        val root = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: File(context.filesDir, "documents")
        return File(root, "calm_space").apply { mkdirs() }
    }

    private fun copyUriToDir(
        context: Context,
        sourceUri: Uri,
        targetDir: File,
        defaultBase: String,
        allowedExtensions: Set<String>
    ): Result<File> {
        return runCatching {
            val resolver = context.contentResolver
            val extension = resolveExtension(context, sourceUri, allowedExtensions)
                ?: throw IllegalArgumentException("Formato no soportado")
            val displayName = resolveDisplayName(context, sourceUri)
            val safeBase = displayName
                ?.substringBeforeLast(".")
                ?.ifBlank { defaultBase }
                ?: defaultBase
            val target = File(
                targetDir,
                "${safeBase.take(40)}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$extension"
            )

            resolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("No se pudo abrir el archivo seleccionado")

            target
        }
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String? {
        val resolver = context.contentResolver
        return runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) return@runCatching cursor.getString(index)
                }
                null
            }
        }.getOrNull()
    }

    private fun resolveExtension(
        context: Context,
        uri: Uri,
        allowedExtensions: Set<String>
    ): String? {
        val fromName = resolveDisplayName(context, uri)?.let { extensionOf(it) }
        if (fromName in allowedExtensions) return fromName

        val mime = context.contentResolver.getType(uri).orEmpty().lowercase(Locale.ROOT)
        val fromMime = when {
            mime.contains("jpeg") -> "jpg"
            mime.contains("jpg") -> "jpg"
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            mime.contains("mpeg") -> "mp3"
            mime.contains("mp3") -> "mp3"
            mime.contains("ogg") -> "ogg"
            mime.contains("wav") -> "wav"
            mime.contains("mp4") || mime.contains("m4a") -> "m4a"
            mime.contains("aac") -> "aac"
            else -> null
        }
        return fromMime?.takeIf { it in allowedExtensions }
    }

    private fun extensionOf(name: String): String {
        return name.substringAfterLast('.', "").lowercase(Locale.ROOT)
    }
}

