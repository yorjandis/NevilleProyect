package com.ypg.neville.model.db.room

import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryEventLogger
import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryEventType

class NotaRepository(private val notaDao: NotaDao) {

    fun insertar(titulo: String, nota: String, isFav: Boolean = false): Long {
        val now = System.currentTimeMillis()
        val item = SecureRoomText.encryptNota(NotaEntity(
            titulo = titulo,
            nota = nota,
            fechaCreacion = now,
            fechaModificacion = now,
            isFav = isFav
        ))
        val id = notaDao.insert(item)
        WeeklySummaryEventLogger.log(WeeklySummaryEventType.NOTES_CREATED, targetKey = id.toString())
        return id
    }

    fun actualizar(
        id: Long,
        titulo: String,
        nota: String,
        fechaCreacionOriginal: Long,
        isFav: Boolean = false
    ) {
        val item = SecureRoomText.encryptNota(NotaEntity(
            id = id,
            titulo = titulo,
            nota = nota,
            fechaCreacion = fechaCreacionOriginal,
            fechaModificacion = System.currentTimeMillis(),
            isFav = isFav
        ))
        notaDao.update(item)
        WeeklySummaryEventLogger.log(WeeklySummaryEventType.NOTES_MODIFIED, targetKey = id.toString())
    }

    fun cambiarFavorito(id: Long, isFav: Boolean) {
        notaDao.updateFavoritoById(id = id, isFav = isFav)
    }

    fun eliminar(nota: NotaEntity) {
        notaDao.delete(nota)
        WeeklySummaryEventLogger.log(WeeklySummaryEventType.NOTES_DELETED, targetKey = nota.id.toString())
    }

    fun obtenerTodas(): List<NotaEntity> = notaDao.getAll().map(SecureRoomText::decryptNota)

    @Suppress("unused")
    fun obtenerPorId(id: Long): NotaEntity? = notaDao.getById(id)?.let(SecureRoomText::decryptNota)
}
