package com.ypg.neville.feature.morningdialog.data

import org.json.JSONArray

object MorningDialogJsonCodec {

    fun encodeList(values: List<String>): String {
        val array = JSONArray()
        values.forEach { value ->
            array.put(value)
        }
        return array.toString()
    }

    fun decodeList(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val value = array.optString(i).trim()
                    if (value.isNotEmpty()) add(value)
                }
            }
        }.getOrDefault(emptyList())
    }
}
