package com.ypg.neville.model.db.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface DiarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(diario: DiarioEntity): Long

    @Update
    fun update(diario: DiarioEntity)

    @Delete
    fun delete(diario: DiarioEntity)

    @Query("SELECT * FROM Diario ORDER BY isFav DESC, fechaM DESC")
    fun getAll(): List<DiarioEntity>

    @Query("SELECT * FROM Diario WHERE id = :id LIMIT 1")
    fun getById(id: Long): DiarioEntity?

    @Query("UPDATE Diario SET isFav = :isFav, fechaM = :fechaModificacion WHERE id = :id")
    fun updateFavoritoById(id: Long, isFav: Boolean, fechaModificacion: Long = System.currentTimeMillis())
}
