package com.ypg.neville.model.db.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "frases",
    indices = [Index(value = ["frase"], unique = true)]
)
data class FraseEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "frase")
    val frase: String,

    @ColumnInfo(name = "autor")
    val autor: String = "",

    @ColumnInfo(name = "fuente")
    val fuente: String = "",

    @ColumnInfo(name = "fav")
    val fav: String = "0",

    @ColumnInfo(name = "nota")
    val nota: String = "",

    @ColumnInfo(name = "inbuild")
    val inbuild: String = "0",

    @ColumnInfo(name = "shared")
    val shared: String = "0"
)
