package com.ypg.neville.model.db.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "frases",
    indices = [
        Index(value = ["asset_key"]),
        Index(value = ["autor"]),
        Index(value = ["categoria"])
    ]
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

    @ColumnInfo(name = "isfav")
    val isfav: String = "0",

    @ColumnInfo(name = "personal")
    val personal: String = "0",

    @ColumnInfo(name = "fav")
    val fav: String = "0",

    @ColumnInfo(name = "nota")
    val nota: String = "",

    @ColumnInfo(name = "inbuild")
    val inbuild: String = "0",

    @ColumnInfo(name = "categoria")
    val categoria: String = "AUTOR",

    @ColumnInfo(name = "asset_key")
    val assetKey: String = "",

    @ColumnInfo(name = "asset_hash")
    val assetHash: String = "",

    @ColumnInfo(name = "shared")
    val shared: String = "0"
) {
    fun favState(): String = if (isfav == "1" || fav == "1") "1" else "0"
    fun personalState(): String = if (personal == "1" || inbuild == "0") "1" else "0"
}
