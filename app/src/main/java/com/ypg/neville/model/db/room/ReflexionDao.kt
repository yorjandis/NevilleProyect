package com.ypg.neville.model.db.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ReflexionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(reflexion: ReflexionEntity): Long

    @Update
    fun update(reflexion: ReflexionEntity)

    @Delete
    fun delete(reflexion: ReflexionEntity)

    @Query("SELECT * FROM reflexiones_personales ORDER BY favorito DESC, fechaModificacion DESC")
    fun getAll(): List<ReflexionEntity>

    @Query("SELECT * FROM reflexiones_personales WHERE id = :id LIMIT 1")
    fun getById(id: Long): ReflexionEntity?
}
