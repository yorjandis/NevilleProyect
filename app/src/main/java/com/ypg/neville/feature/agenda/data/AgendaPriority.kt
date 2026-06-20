package com.ypg.neville.feature.agenda.data

enum class AgendaPriority(val title: String) {
    NEUTRAL("Neutral"),
    BAJA("Baja"),
    MEDIA("Media"),
    ALTA("Alta");

    companion object {
        fun fromRaw(raw: String): AgendaPriority {
            return entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: NEUTRAL
        }
    }
}
