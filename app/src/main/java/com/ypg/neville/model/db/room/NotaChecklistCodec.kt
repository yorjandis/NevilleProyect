package com.ypg.neville.model.db.room

import org.json.JSONArray
import org.json.JSONObject

data class NotaChecklistItem(
    val id: String,
    val text: String,
    val checked: Boolean
)

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
                    checked = item.optBoolean("checked", false)
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
                )
            }
        return array.toString()
    }
}
