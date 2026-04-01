package com.ypg.neville.model.db.room

class NotaRepository(private val notaDao: NotaDao) {

    fun insertar(titulo: String, nota: String): Long {
        val now = System.currentTimeMillis()
        val item = NotaEntity(
            titulo = titulo,
            nota = nota,
            fechaCreacion = now,
            fechaModificacion = now
        )
        return notaDao.insert(item)
    }

    fun actualizar(id: Long, titulo: String, nota: String, fechaCreacionOriginal: Long) {
        val item = NotaEntity(
            id = id,
            titulo = titulo,
            nota = nota,
            fechaCreacion = fechaCreacionOriginal,
            fechaModificacion = System.currentTimeMillis()
        )
        notaDao.update(item)
    }

    fun eliminar(nota: NotaEntity) {
        notaDao.delete(nota)
    }

    fun obtenerTodas(): List<NotaEntity> = notaDao.getAll()

    fun obtenerPorId(id: Long): NotaEntity? = notaDao.getById(id)
}
