package com.ypg.neville.model.db.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reflexiones_personales")
data class ReflexionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "titulo")
    val titulo: String,

    @ColumnInfo(name = "contenido")
    val contenido: String,

    @ColumnInfo(name = "favorito")
    val favorito: Boolean = false,

    @ColumnInfo(name = "nota")
    val nota: String = "",

    @ColumnInfo(name = "fechaCreacion")
    val fechaCreacion: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "fechaModificacion")
    val fechaModificacion: Long = System.currentTimeMillis()
)
