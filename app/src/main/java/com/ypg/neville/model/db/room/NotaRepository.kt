package com.ypg.neville.model.db.room

class NotaRepository(private val notaDao: NotaDao) {

    fun insertar(titulo: String, nota: String, isFav: Boolean = false): Long {
        val now = System.currentTimeMillis()
        val item = NotaEntity(
            titulo = titulo,
            nota = nota,
            fechaCreacion = now,
            fechaModificacion = now,
            isFav = isFav
        )
        return notaDao.insert(item)
    }

    fun actualizar(
        id: Long,
        titulo: String,
        nota: String,
        fechaCreacionOriginal: Long,
        isFav: Boolean = false
    ) {
        val item = NotaEntity(
            id = id,
            titulo = titulo,
            nota = nota,
            fechaCreacion = fechaCreacionOriginal,
            fechaModificacion = System.currentTimeMillis(),
            isFav = isFav
        )
        notaDao.update(item)
    }

    fun cambiarFavorito(id: Long, isFav: Boolean) {
        notaDao.updateFavoritoById(id = id, isFav = isFav)
    }

    fun eliminar(nota: NotaEntity) {
        notaDao.delete(nota)
    }

    fun obtenerTodas(): List<NotaEntity> = notaDao.getAll()

    @Suppress("unused")
    fun obtenerPorId(id: Long): NotaEntity? = notaDao.getById(id)
}
