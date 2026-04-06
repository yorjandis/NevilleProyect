package com.ypg.neville.model.db.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Diario")
data class DiarioEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "emocion")
    val emocion: String = "\uD83D\uDE0C",

    @ColumnInfo(name = "fecha")
    val fecha: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "fechaM")
    val fechaM: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "isFav")
    val isFav: Boolean = false
)
