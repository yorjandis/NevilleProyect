package com.ypg.neville.model.db.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VideoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(video: VideoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(videos: List<VideoEntity>)

    @Query("DELETE FROM videos")
    fun clearAll()

    @Query("SELECT COUNT(*) FROM videos")
    fun count(): Int

    @Query("SELECT * FROM videos")
    fun getAll(): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE fav = '1'")
    fun getFavoritos(): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE TRIM(nota) != ''")
    fun getConNotas(): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE type = :type")
    fun getByType(type: String): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE title = :title LIMIT 1")
    fun getByTitle(title: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE link = :link LIMIT 1")
    fun getByLink(link: String): VideoEntity?

    @Query("UPDATE videos SET fav = :fav WHERE title = :title")
    fun updateFavByTitle(title: String, fav: String)

    @Query("UPDATE videos SET fav = :fav WHERE link = :link")
    fun updateFavByLink(link: String, fav: String)

    @Query("UPDATE videos SET nota = :nota WHERE title = :title")
    fun updateNotaByTitle(title: String, nota: String)
}
