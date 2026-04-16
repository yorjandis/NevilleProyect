package com.ypg.neville.model.reminders

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    indices = [Index(value = ["sortOrder"])]
)
data class ReminderEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "message")
    val message: String,

    @ColumnInfo(name = "frequencyType")
    val frequencyType: String,

    @ColumnInfo(name = "intervalHours")
    val intervalHours: Int? = null,

    @ColumnInfo(name = "intervalMinutes")
    val intervalMinutes: Int? = null,

    @ColumnInfo(name = "dailyHour")
    val dailyHour: Int? = null,

    @ColumnInfo(name = "dailyMinute")
    val dailyMinute: Int? = null,

    @ColumnInfo(name = "dateTimeMillis")
    val dateTimeMillis: Long? = null,

    @ColumnInfo(name = "monthlyDay")
    val monthlyDay: Int? = null,

    @ColumnInfo(name = "monthlyHour")
    val monthlyHour: Int? = null,

    @ColumnInfo(name = "monthlyMinute")
    val monthlyMinute: Int? = null,

    @ColumnInfo(name = "yearlyMonth")
    val yearlyMonth: Int? = null,

    @ColumnInfo(name = "yearlyDay")
    val yearlyDay: Int? = null,

    @ColumnInfo(name = "yearlyHour")
    val yearlyHour: Int? = null,

    @ColumnInfo(name = "yearlyMinute")
    val yearlyMinute: Int? = null,

    @ColumnInfo(name = "isStarted")
    val isStarted: Boolean,

    @ColumnInfo(name = "startedAt")
    val startedAt: Long?,

    @ColumnInfo(name = "isPinned")
    val isPinned: Boolean = false,

    @ColumnInfo(name = "sortOrder")
    val sortOrder: Int
)
