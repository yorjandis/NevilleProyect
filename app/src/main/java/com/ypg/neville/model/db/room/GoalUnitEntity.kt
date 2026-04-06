package com.ypg.neville.model.db.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "goal_units",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["goalId"]), Index(value = ["status"])]
)
data class GoalUnitEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "goalId")
    val goalId: String,

    @ColumnInfo(name = "unitIndex")
    val unitIndex: Int,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "unitType")
    val unitType: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "info")
    val info: String,

    @ColumnInfo(name = "note")
    val note: String,

    @ColumnInfo(name = "startDate")
    val startDate: Long?,

    @ColumnInfo(name = "endDate")
    val endDate: Long?,

    @ColumnInfo(name = "completedDate")
    val completedDate: Long?
)
