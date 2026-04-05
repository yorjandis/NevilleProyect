package com.ypg.neville.model.db.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notas")
data class NotaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "titulo")
    val titulo: String,

    @ColumnInfo(name = "nota")
    val nota: String,

    @ColumnInfo(name = "fechaCreacion")
    val fechaCreacion: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "fechaModificacion")
    val fechaModificacion: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "isFav")
    val isFav: Boolean = false
)
