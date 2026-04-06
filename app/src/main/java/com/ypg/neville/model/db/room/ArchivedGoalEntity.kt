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

    @ColumnInfo(name = "completionDate")
    val completionDate: Long
)
