package com.ypg.neville.model.db.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conf",
    indices = [
        Index(value = ["title"], unique = true),
        Index(value = ["link"], unique = true)
    ]
)
data class ConfEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "link")
    val link: String,

    @ColumnInfo(name = "fav")
    val fav: String = "0",

    @ColumnInfo(name = "nota")
    val nota: String = "",

    @ColumnInfo(name = "shared")
    val shared: String = "0"
)
