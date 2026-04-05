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

    @Query("SELECT * FROM frases WHERE (isfav = '1' OR fav = '1')")
    fun getFavoritas(): List<FraseEntity>

    @Query("SELECT * FROM frases WHERE personal != '1' AND inbuild != '0'")
    fun getInbuilt(): List<FraseEntity>

    @Query("SELECT * FROM frases WHERE (isfav = '1' OR fav = '1') AND personal != '1' AND inbuild != '0'")
    fun getInbuiltFavoritas(): List<FraseEntity>

    @Query("SELECT * FROM frases WHERE TRIM(nota) != '' AND personal != '1' AND inbuild != '0'")
    fun getInbuiltConNotas(): List<FraseEntity>

    @Query("SELECT * FROM frases WHERE personal = '1' OR inbuild = '0'")
    fun getPersonales(): List<FraseEntity>

    @Query("SELECT * FROM frases WHERE (isfav = '1' OR fav = '1') AND (personal = '1' OR inbuild = '0')")
    fun getPersonalesFavoritas(): List<FraseEntity>

    @Query("SELECT * FROM frases WHERE (personal = '1' OR inbuild = '0') AND TRIM(nota) != ''")
    fun getPersonalesConNotas(): List<FraseEntity>

    @Query("SELECT * FROM frases WHERE id = :id LIMIT 1")
    fun getById(id: Long): FraseEntity?

    @Query("SELECT * FROM frases WHERE frase = :frase LIMIT 1")
    fun getByFrase(frase: String): FraseEntity?

    @Query("UPDATE frases SET fav = :fav, isfav = :fav WHERE id = :id")
    fun updateFavById(id: Long, fav: String)

    @Query("UPDATE frases SET fav = :fav, isfav = :fav WHERE frase = :frase")
    fun updateFavByFrase(frase: String, fav: String)

    @Query("UPDATE frases SET nota = :nota WHERE frase = :frase")
    fun updateNotaByFrase(frase: String, nota: String)

    @Query("UPDATE frases SET frase = :newFrase WHERE id = :id")
    fun updateFraseTextById(id: Long, newFrase: String)

    @Query("DELETE FROM frases WHERE frase = :frase")
    fun deleteByFrase(frase: String)

    @Query("SELECT COUNT(*) FROM frases WHERE personal != '1' AND inbuild != '0'")
    fun countManagedAssetFrases(): Int

    @Query("SELECT * FROM frases WHERE personal != '1' AND inbuild != '0'")
    fun getManagedAssetFrases(): List<FraseEntity>

    @Query("DELETE FROM frases WHERE personal != '1' AND inbuild != '0'")
    fun deleteManagedAssetFrases()

    @Query(
        """
        SELECT * FROM frases
        WHERE (:onlyFav = 0 OR isfav = '1' OR fav = '1')
        AND (
            (:includeAutores = 1 AND categoria = 'AUTOR')
            OR (:includeOtros = 1 AND categoria = 'OTROS')
            OR (:includeSalud = 1 AND categoria = 'SALUD')
        )
        """
    )
    fun getForHome(
        onlyFav: Int,
        includeAutores: Int,
        includeOtros: Int,
        includeSalud: Int
    ): List<FraseEntity>

    @Query("SELECT * FROM frases WHERE LOWER(autor) = LOWER(:autor)")
    fun getByAutor(autor: String): List<FraseEntity>
}
