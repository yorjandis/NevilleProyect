package com.ypg.neville.model.db.room

import org.json.JSONArray
import org.json.JSONObject

data class NotaChecklistItem(
    val id: String,
    val text: String,
    val checked: Boolean,
    val kind: NotaChecklistItemKind = NotaChecklistItemKind.TASK
) {
    val isTask: Boolean
        get() = kind == NotaChecklistItemKind.TASK
}

enum class NotaChecklistItemKind(val storageValue: String) {
    TASK("task"),
    LEADING_NOTE("leadingNote"),
    TRAILING_NOTE("trailingNote");

    companion object {
        fun fromStorageValue(value: String): NotaChecklistItemKind =
            entries.firstOrNull { it.storageValue == value } ?: TASK
    }
}

object NotaChecklistCodec {
    fun decode(json: String): List<NotaChecklistItem> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(json)
            List(array.length()) { index ->
                val item = array.optJSONObject(index) ?: JSONObject()
                NotaChecklistItem(
                    id = item.optString("id").ifBlank { "item-$index" },
                    text = item.optString("text"),
                    checked = if (item.has("checked")) {
                        item.optBoolean("checked", false)
                    } else {
                        item.optBoolean("isChecked", false)
                    },
                    kind = NotaChecklistItemKind.fromStorageValue(item.optString("kind", "task"))
                )
            }.filter { it.text.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    fun encode(items: List<NotaChecklistItem>): String {
        val array = JSONArray()
        items
            .map { it.copy(text = it.text.trim()) }
            .filter { it.text.isNotBlank() }
            .forEach { item ->
                array.put(
                    JSONObject()
                        .put("id", item.id)
                        .put("text", item.text)
                        .put("checked", item.checked)
                        .put("isChecked", item.checked)
                        .put("kind", item.kind.storageValue)
                )
            }
        return array.toString()
    }
}
