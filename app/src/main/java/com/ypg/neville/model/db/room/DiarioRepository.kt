package com.ypg.neville.model.db.room

class DiarioRepository(private val diarioDao: DiarioDao) {

    fun insertar(
        title: String,
        content: String,
        emocion: String,
        isFav: Boolean,
        fechaCreacionMillis: Long = System.currentTimeMillis()
    ): Long {
        val now = System.currentTimeMillis()
        val item = DiarioEntity(
            title = title,
            content = content,
            emocion = emocion,
            fecha = fechaCreacionMillis,
            fechaM = now,
            isFav = isFav
        )
        return diarioDao.insert(item)
    }

    fun actualizar(id: Long, title: String, content: String, emocion: String, isFav: Boolean, fechaOriginal: Long) {
        val item = DiarioEntity(
            id = id,
            title = title,
            content = content,
            emocion = emocion,
            fecha = fechaOriginal,
            fechaM = System.currentTimeMillis(),
            isFav = isFav
        )
        diarioDao.update(item)
    }

    fun cambiarFavorito(id: Long, isFav: Boolean) {
        diarioDao.updateFavoritoById(id = id, isFav = isFav)
    }

    fun eliminar(diario: DiarioEntity) {
        diarioDao.delete(diario)
    }

    fun obtenerTodas(): List<DiarioEntity> = diarioDao.getAll()

    fun obtenerPorId(id: Long): DiarioEntity? = diarioDao.getById(id)
}
