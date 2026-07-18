package com.ypg.neville.model.db.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "archived_goals",
    indices = [Index(value = ["completionDate"])]
)
data class ArchivedGoalEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "descriptionText")
    val descriptionText: String,

    @ColumnInfo(name = "totalUnits")
    val totalUnits: Int,

    @ColumnInfo(name = "unitType")
    val unitType: String,

    @ColumnInfo(name = "frequency")
    val frequency: Int,

    @ColumnInfo(name = "scheduleType")
    val scheduleType: String = "interval",

    @ColumnInfo(name = "weeklyDaysPerWeek")
    val weeklyDaysPerWeek: Int = 3,

    @ColumnInfo(name = "dayPeriod")
    val dayPeriod: String = "anytime",

    @ColumnInfo(name = "customUnitLabel")
    val customUnitLabel: String = "",

    @ColumnInfo(name = "executionTargetValue")
    val executionTargetValue: Double = 1.0,

    @ColumnInfo(name = "completionBasis")
    val completionBasis: String = "executions",

    @ColumnInfo(name = "durationValue")
    val durationValue: Int = 0,

    @ColumnInfo(name = "durationUnit")
    val durationUnit: String = "dias",

    @ColumnInfo(name = "completionDate")
    val completionDate: Long
)
