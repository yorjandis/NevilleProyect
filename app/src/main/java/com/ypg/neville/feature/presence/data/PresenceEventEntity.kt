package com.ypg.neville.feature.presence.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "presence_events",
    indices = [
        Index(value = ["createdAtMillis"]),
        Index(value = ["dayStartMillis"]),
        Index(value = ["eventType"]),
        Index(value = ["moodId"])
    ]
)
data class PresenceEventEntity(
    @PrimaryKey
    val id: String,
    val createdAtMillis: Long,
    val dayStartMillis: Long,
    val eventType: String,
    val moodId: String?,
    val note: String,
    val source: String
)
