package com.ypg.neville.model.db.room

import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryEventLogger
import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryEventType

class DiarioRepository(private val diarioDao: DiarioDao) {

    fun insertar(
        title: String,
        content: String,
        emocion: String,
        isFav: Boolean,
        fechaCreacionMillis: Long = System.currentTimeMillis()
    ): Long {
        val now = System.currentTimeMillis()
        val item = SecureRoomText.encryptDiario(DiarioEntity(
            title = title,
            content = content,
            emocion = emocion,
            fecha = fechaCreacionMillis,
            fechaM = now,
            isFav = isFav
        ))
        val id = diarioDao.insert(item)
        WeeklySummaryEventLogger.log(WeeklySummaryEventType.JOURNAL_CREATED, targetKey = id.toString())
        return id
    }

    fun actualizar(id: Long, title: String, content: String, emocion: String, isFav: Boolean, fechaOriginal: Long) {
        val item = SecureRoomText.encryptDiario(DiarioEntity(
            id = id,
            title = title,
            content = content,
            emocion = emocion,
            fecha = fechaOriginal,
            fechaM = System.currentTimeMillis(),
            isFav = isFav
        ))
        diarioDao.update(item)
        WeeklySummaryEventLogger.log(WeeklySummaryEventType.JOURNAL_MODIFIED, targetKey = id.toString())
    }

    fun cambiarFavorito(id: Long, isFav: Boolean) {
        diarioDao.updateFavoritoById(id = id, isFav = isFav)
    }

    fun eliminar(diario: DiarioEntity) {
        diarioDao.delete(diario)
        WeeklySummaryEventLogger.log(WeeklySummaryEventType.JOURNAL_DELETED, targetKey = diario.id.toString())
    }

    fun obtenerTodas(): List<DiarioEntity> = diarioDao.getAll().map(SecureRoomText::decryptDiario)

    fun obtenerPorId(id: Long): DiarioEntity? = diarioDao.getById(id)?.let(SecureRoomText::decryptDiario)

    fun actualizarTituloYContenido(id: Long, title: String, content: String) {
        diarioDao.updateTitleAndContentById(
            id = id,
            title = SecureRoomText.encryptDiarioTitle(title),
            content = SecureRoomText.encryptDiarioContent(content)
        )
    }
}
