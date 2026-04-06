package com.ypg.neville.model.db.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "goals",
    indices = [Index(value = ["title"]) ]
)
data class GoalEntity(
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

    @ColumnInfo(name = "isStarted")
    val isStarted: Boolean,

    @ColumnInfo(name = "startDate")
    val startDate: Long?
)
