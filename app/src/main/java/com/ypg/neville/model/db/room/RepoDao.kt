package com.ypg.neville.model.db.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RepoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: RepoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(items: List<RepoEntity>)

    @Query("DELETE FROM repo")
    fun clearAll()

    @Query("SELECT * FROM repo")
    fun getAll(): List<RepoEntity>

    @Query("SELECT * FROM repo WHERE type = :type")
    fun getByType(type: String): List<RepoEntity>

    @Query("SELECT * FROM repo WHERE fav = '1' AND type = :type")
    fun getFavoritosByType(type: String): List<RepoEntity>

    @Query("SELECT * FROM repo WHERE TRIM(nota) != '' AND type = :type")
    fun getConNotasByType(type: String): List<RepoEntity>

    @Query("SELECT * FROM repo WHERE title = :title LIMIT 1")
    fun getByTitle(title: String): RepoEntity?

    @Query("UPDATE repo SET fav = :fav WHERE title = :title")
    fun updateFavByTitle(title: String, fav: String)

    @Query("UPDATE repo SET nota = :nota WHERE title = :title")
    fun updateNotaByTitle(title: String, nota: String)
}
