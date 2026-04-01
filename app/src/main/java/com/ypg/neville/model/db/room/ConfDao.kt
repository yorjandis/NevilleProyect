package com.ypg.neville.model.db.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ConfDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: ConfEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(items: List<ConfEntity>)

    @Query("DELETE FROM conf")
    fun clearAll()

    @Query("SELECT COUNT(*) FROM conf")
    fun count(): Int

    @Query("SELECT * FROM conf")
    fun getAll(): List<ConfEntity>

    @Query("SELECT * FROM conf WHERE fav = '1'")
    fun getFavoritas(): List<ConfEntity>

    @Query("SELECT * FROM conf WHERE TRIM(nota) != ''")
    fun getConNotas(): List<ConfEntity>

    @Query("SELECT * FROM conf WHERE title = :title LIMIT 1")
    fun getByTitle(title: String): ConfEntity?

    @Query("UPDATE conf SET fav = :fav WHERE title = :title")
    fun updateFavByTitle(title: String, fav: String)

    @Query("UPDATE conf SET nota = :nota WHERE title = :title")
    fun updateNotaByTitle(title: String, nota: String)
}
