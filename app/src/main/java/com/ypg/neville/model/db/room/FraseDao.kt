package com.ypg.neville.model.db.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface FraseDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(frase: FraseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(frases: List<FraseEntity>)

    @Update
    fun update(frase: FraseEntity)

    @Query("DELETE FROM frases")
    fun clearAll()

    @Query("SELECT COUNT(*) FROM frases")
    fun count(): Int

    @Query("SELECT * FROM frases")
    fun getAll(): List<FraseEntity>

    @Query("SELECT * FROM frases WHERE fav = '1'")
    fun getFavoritas(): List<FraseEntity>

    @Query("SELECT * FROM frases WHERE inbuild = '1'")
    fun getInbuilt(): List<FraseEntity>

    @Query("SELECT * FROM frases WHERE fav = '1' AND inbuild = '1'")
    fun getInbuiltFavoritas(): List<FraseEntity>

    @Query("SELECT * FROM frases WHERE TRIM(nota) != '' AND inbuild = '1'")
    fun getInbuiltConNotas(): List<FraseEntity>

    @Query("SELECT * FROM frases WHERE inbuild = '0'")
    fun getPersonales(): List<FraseEntity>

    @Query("SELECT * FROM frases WHERE fav = '1' AND inbuild = '0'")
    fun getPersonalesFavoritas(): List<FraseEntity>

    @Query("SELECT * FROM frases WHERE inbuild = '0' AND TRIM(nota) != ''")
    fun getPersonalesConNotas(): List<FraseEntity>

    @Query("SELECT * FROM frases WHERE id = :id LIMIT 1")
    fun getById(id: Long): FraseEntity?

    @Query("SELECT * FROM frases WHERE frase = :frase LIMIT 1")
    fun getByFrase(frase: String): FraseEntity?

    @Query("UPDATE frases SET fav = :fav WHERE id = :id")
    fun updateFavById(id: Long, fav: String)

    @Query("UPDATE frases SET fav = :fav WHERE frase = :frase")
    fun updateFavByFrase(frase: String, fav: String)

    @Query("UPDATE frases SET nota = :nota WHERE frase = :frase")
    fun updateNotaByFrase(frase: String, nota: String)

    @Query("UPDATE frases SET frase = :newFrase WHERE id = :id")
    fun updateFraseTextById(id: Long, newFrase: String)

    @Query("DELETE FROM frases WHERE frase = :frase")
    fun deleteByFrase(frase: String)
}
