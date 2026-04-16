package com.ypg.neville.model.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.ypg.neville.model.db.room.NevilleRoomDatabase
import com.ypg.neville.model.db.room.PreferenceEntity
import org.json.JSONArray
import java.util.concurrent.CopyOnWriteArraySet

object DbPreferences {

    private const val DEFAULT_STORE = "default"

    @JvmStatic
    fun default(context: Context): SharedPreferences {
        return RoomSharedPreferences(context.applicationContext, DEFAULT_STORE)
    }

    @JvmStatic
    fun named(context: Context, name: String): SharedPreferences {
        return RoomSharedPreferences(context.applicationContext, name)
    }

    private class RoomSharedPreferences(
        private val context: Context,
        private val storeName: String
    ) : SharedPreferences {

        private val listeners = CopyOnWriteArraySet<SharedPreferences.OnSharedPreferenceChangeListener>()
        private val prefix = "$storeName::"
        private val migratedKey = "${prefix}__migrated__"

        init {
            migrateLegacyStoreIfNeeded()
        }

        override fun getAll(): MutableMap<String, *> {
            return dao().getByPrefix("$prefix%").associate { entity ->
                val key = entity.prefKey.removePrefix(prefix)
                key to decodeValue(entity)
            }.toMutableMap()
        }

        override fun getString(key: String?, defValue: String?): String? {
            val safeKey = key ?: return defValue
            val entity = dao().getByKey(fullKey(safeKey)) ?: return defValue
            return when (entity.valueType) {
                TYPE_STRING -> entity.prefValue
                TYPE_INT,
                TYPE_LONG,
                TYPE_FLOAT,
                TYPE_BOOLEAN -> entity.prefValue
                else -> defValue
            }
        }

        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
            val safeKey = key ?: return defValues
            val entity = dao().getByKey(fullKey(safeKey)) ?: return defValues
            if (entity.valueType != TYPE_STRING_SET) return defValues
            return runCatching {
                val arr = JSONArray(entity.prefValue)
                val out = linkedSetOf<String>()
                for (i in 0 until arr.length()) out.add(arr.optString(i))
                out
            }.getOrElse { defValues }
        }

        override fun getInt(key: String?, defValue: Int): Int {
            val safeKey = key ?: return defValue
            val entity = dao().getByKey(fullKey(safeKey)) ?: return defValue
            return entity.prefValue.toIntOrNull() ?: defValue
        }

        override fun getLong(key: String?, defValue: Long): Long {
            val safeKey = key ?: return defValue
            val entity = dao().getByKey(fullKey(safeKey)) ?: return defValue
            return entity.prefValue.toLongOrNull() ?: defValue
        }

        override fun getFloat(key: String?, defValue: Float): Float {
            val safeKey = key ?: return defValue
            val entity = dao().getByKey(fullKey(safeKey)) ?: return defValue
            return entity.prefValue.toFloatOrNull() ?: defValue
        }

        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            val safeKey = key ?: return defValue
            val entity = dao().getByKey(fullKey(safeKey)) ?: return defValue
            return when (entity.prefValue.lowercase()) {
                "true", "1" -> true
                "false", "0" -> false
                else -> defValue
            }
        }

        override fun contains(key: String?): Boolean {
            val safeKey = key ?: return false
            return dao().getByKey(fullKey(safeKey)) != null
        }

        override fun edit(): SharedPreferences.Editor = EditorImpl(this)

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            if (listener != null) listeners.add(listener)
        }

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            if (listener != null) listeners.remove(listener)
        }

        private fun fullKey(key: String): String = "$prefix$key"
        private fun dao() = NevilleRoomDatabase.getInstance(context).preferenceDao()

        private fun decodeValue(entity: PreferenceEntity): Any {
            return when (entity.valueType) {
                TYPE_STRING -> entity.prefValue
                TYPE_INT -> entity.prefValue.toIntOrNull() ?: 0
                TYPE_LONG -> entity.prefValue.toLongOrNull() ?: 0L
                TYPE_FLOAT -> entity.prefValue.toFloatOrNull() ?: 0f
                TYPE_BOOLEAN -> entity.prefValue.equals("true", ignoreCase = true) || entity.prefValue == "1"
                TYPE_STRING_SET -> {
                    val arr = runCatching { JSONArray(entity.prefValue) }.getOrNull() ?: JSONArray()
                    val out = linkedSetOf<String>()
                    for (i in 0 until arr.length()) out.add(arr.optString(i))
                    out
                }
                else -> entity.prefValue
            }
        }

        private fun migrateLegacyStoreIfNeeded() {
            if (dao().getByKey(migratedKey) != null) return

            val legacy = if (storeName == DEFAULT_STORE) {
                PreferenceManager.getDefaultSharedPreferences(context)
            } else {
                context.getSharedPreferences(storeName, Context.MODE_PRIVATE)
            }

            val toInsert = mutableListOf<PreferenceEntity>()
            legacy.all.forEach { (key, value) ->
                if (key == "__migrated__") return@forEach
                val entry = encodeEntry(fullKey(key), value)
                if (entry != null) toInsert.add(entry)
            }
            toInsert.add(PreferenceEntity(migratedKey, "true", TYPE_BOOLEAN))
            if (toInsert.isNotEmpty()) dao().upsertAll(toInsert)
        }

        private fun encodeEntry(fullKey: String, value: Any?): PreferenceEntity? {
            return when (value) {
                null -> null
                is String -> PreferenceEntity(fullKey, value, TYPE_STRING)
                is Int -> PreferenceEntity(fullKey, value.toString(), TYPE_INT)
                is Long -> PreferenceEntity(fullKey, value.toString(), TYPE_LONG)
                is Float -> PreferenceEntity(fullKey, value.toString(), TYPE_FLOAT)
                is Boolean -> PreferenceEntity(fullKey, value.toString(), TYPE_BOOLEAN)
                is Set<*> -> {
                    val arr = JSONArray()
                    value.forEach { item -> arr.put(item?.toString() ?: "") }
                    PreferenceEntity(fullKey, arr.toString(), TYPE_STRING_SET)
                }
                else -> PreferenceEntity(fullKey, value.toString(), TYPE_STRING)
            }
        }

        private class EditorImpl(private val prefs: RoomSharedPreferences) : SharedPreferences.Editor {
            private val values = linkedMapOf<String, PreferenceEntity?>()
            private val removals = linkedSetOf<String>()
            private var shouldClear = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null) {
                    if (value == null) {
                        removals.add(key)
                    } else {
                        values[key] = PreferenceEntity(prefs.fullKey(key), value, TYPE_STRING)
                    }
                }
                return this
            }

            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
                if (key != null) {
                    if (values == null) {
                        removals.add(key)
                    } else {
                        val arr = JSONArray()
                        values.forEach { arr.put(it) }
                        this.values[key] = PreferenceEntity(prefs.fullKey(key), arr.toString(), TYPE_STRING_SET)
                    }
                }
                return this
            }

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                if (key != null) values[key] = PreferenceEntity(prefs.fullKey(key), value.toString(), TYPE_INT)
                return this
            }

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                if (key != null) values[key] = PreferenceEntity(prefs.fullKey(key), value.toString(), TYPE_LONG)
                return this
            }

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                if (key != null) values[key] = PreferenceEntity(prefs.fullKey(key), value.toString(), TYPE_FLOAT)
                return this
            }

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                if (key != null) values[key] = PreferenceEntity(prefs.fullKey(key), value.toString(), TYPE_BOOLEAN)
                return this
            }

            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) removals.add(key)
                return this
            }

            override fun clear(): SharedPreferences.Editor {
                shouldClear = true
                values.clear()
                removals.clear()
                return this
            }

            override fun commit(): Boolean {
                applyChanges()
                return true
            }

            override fun apply() {
                applyChanges()
            }

            private fun applyChanges() {
                val dao = prefs.dao()
                if (shouldClear) {
                    dao.deleteByPrefix("${prefs.prefix}%")
                }
                removals.forEach { key -> dao.deleteByKey(prefs.fullKey(key)) }
                values.values.filterNotNull().let { entities ->
                    if (entities.isNotEmpty()) dao.upsertAll(entities)
                }

                val changedKeys = linkedSetOf<String>()
                if (shouldClear) {
                    changedKeys.addAll(prefs.getAll().keys)
                }
                changedKeys.addAll(removals)
                changedKeys.addAll(values.keys)
                changedKeys.forEach { changedKey ->
                    prefs.listeners.forEach { it.onSharedPreferenceChanged(prefs, changedKey) }
                }
            }
        }
    }

    private const val TYPE_STRING = "S"
    private const val TYPE_INT = "I"
    private const val TYPE_LONG = "L"
    private const val TYPE_FLOAT = "F"
    private const val TYPE_BOOLEAN = "B"
    private const val TYPE_STRING_SET = "SS"
}
