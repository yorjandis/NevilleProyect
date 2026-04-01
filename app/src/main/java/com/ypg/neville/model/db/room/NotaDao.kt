package com.ypg.neville.model.db.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface NotaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(nota: NotaEntity): Long

    @Update
    fun update(nota: NotaEntity)

    @Delete
    fun delete(nota: NotaEntity)

    @Query("SELECT * FROM notas ORDER BY fechaModificacion DESC")
    fun getAll(): List<NotaEntity>

    @Query("SELECT * FROM notas WHERE id = :id LIMIT 1")
    fun getById(id: Long): NotaEntity?

    @Query("SELECT * FROM notas WHERE titulo = :titulo LIMIT 1")
    fun getByTitulo(titulo: String): NotaEntity?

    @Query("DELETE FROM notas WHERE titulo = :titulo")
    fun deleteByTitulo(titulo: String)
}
