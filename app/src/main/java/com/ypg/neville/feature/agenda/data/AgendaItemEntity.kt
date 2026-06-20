package com.ypg.neville.feature.agenda.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "agenda_items",
    indices = [
        Index(value = ["activityDateMillis"]),
        Index(value = ["activityTimeMillis"]),
        Index(value = ["reminderId"])
    ]
)
data class AgendaItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long,

    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long,

    @ColumnInfo(name = "note")
    val note: String,

    @ColumnInfo(name = "activityDateMillis")
    val activityDateMillis: Long,

    @ColumnInfo(name = "activityTimeMillis")
    val activityTimeMillis: Long,

    @ColumnInfo(name = "place")
    val place: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "priority")
    val priority: String,

    @ColumnInfo(name = "colorHex")
    val colorHex: String,

    @ColumnInfo(name = "completed")
    val completed: Boolean?,

    @ColumnInfo(name = "reminderActive")
    val reminderActive: Boolean,

    @ColumnInfo(name = "reminderId")
    val reminderId: String?
)
