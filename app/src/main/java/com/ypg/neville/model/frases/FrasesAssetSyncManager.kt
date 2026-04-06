package com.ypg.neville.model.frases

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.ypg.neville.model.db.room.FraseEntity
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import java.security.MessageDigest

object FrasesAssetSyncManager {

    private const val PREF_HASH_PREFIX = "frases_asset_hash::"

    fun syncIfNeeded(context: Context, room: NevilleRoomDatabase): Boolean {
        return syncInternal(context, room, force = false)
    }

    fun forceSync(context: Context, room: NevilleRoomDatabase): Boolean {
        return syncInternal(context, room, force = true)
    }

    private fun syncInternal(context: Context, room: NevilleRoomDatabase, force: Boolean): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val dao = room.fraseDao()

        val sourceHashes = FrasesAssetParser.sourceSpecs.associate { spec ->
            val raw = context.assets.open(spec.assetPath).bufferedReader(Charsets.UTF_8).use { it.readText() }
            spec.assetPath to sha256(raw)
        }

        val hasChanged = sourceHashes.any { (assetPath, hash) ->
            prefs.getString("$PREF_HASH_PREFIX$assetPath", null) != hash
        }

        val hasManagedRows = dao.countManagedAssetFrases() > 0
        if (!force && !hasChanged && hasManagedRows) return false

        val parsed = FrasesAssetParser.sourceSpecs.flatMap { spec ->
            val fileHash = sourceHashes[spec.assetPath].orEmpty()
            FrasesAssetParser.parse(context, spec).map { it.copy(assetHash = fileHash) }
        }

        val previousStateByAssetKey = linkedMapOf<String, FraseEntity>()
        val previousStateByAuthorText = linkedMapOf<String, FraseEntity>()
        dao.getManagedAssetFrases().forEach { existing ->
            val key = existing.assetKey.trim()
            if (key.isNotBlank()) {
                previousStateByAssetKey[key] = existing
            }
            previousStateByAuthorText[composeAuthorTextKey(existing.autor, existing.frase)] = existing
        }

        val imported = parsed.map { item ->
            val previous = previousStateByAssetKey[item.assetKey]
                ?: previousStateByAuthorText[composeAuthorTextKey(item.autor, item.frase)]
            val favState = previous?.favState() ?: "0"
            FraseEntity(
                frase = item.frase,
                autor = item.autor,
                fuente = item.fuente,
                isfav = favState,
                personal = "0",
                fav = favState,
                nota = previous?.nota ?: item.nota,
                inbuild = "1",
                categoria = item.categoria,
                assetKey = item.assetKey,
                assetHash = item.assetHash,
                shared = previous?.shared ?: "0"
            )
        }

        room.runInTransaction {
            dao.deleteManagedAssetFrases()
            dao.insertAll(imported)
        }

        prefs.edit {
            sourceHashes.forEach { (assetPath, hash) ->
                putString("$PREF_HASH_PREFIX$assetPath", hash)
            }
        }
        return true
    }

    private fun composeAuthorTextKey(autor: String, frase: String): String {
        return "${autor.trim().lowercase()}::${frase.trim().lowercase()}"
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return buildString(digest.size * 2) {
            digest.forEach { b -> append("%02x".format(b)) }
        }
    }
}
