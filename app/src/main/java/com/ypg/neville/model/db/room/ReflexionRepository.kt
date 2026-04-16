package com.ypg.neville.model.db.room

class ReflexionRepository(private val reflexionDao: ReflexionDao) {

    fun insertar(titulo: String, contenido: String, favorito: Boolean, nota: String): Long {
        val now = System.currentTimeMillis()
        return reflexionDao.insert(
            ReflexionEntity(
                titulo = titulo,
                contenido = contenido,
                favorito = favorito,
                nota = nota,
                fechaCreacion = now,
                fechaModificacion = now
            )
        )
    }

    fun actualizar(
        id: Long,
        titulo: String,
        contenido: String,
        favorito: Boolean,
        nota: String,
        fechaCreacionOriginal: Long
    ) {
        reflexionDao.update(
            ReflexionEntity(
                id = id,
                titulo = titulo,
                contenido = contenido,
                favorito = favorito,
                nota = nota,
                fechaCreacion = fechaCreacionOriginal,
                fechaModificacion = System.currentTimeMillis()
            )
        )
    }

    fun eliminar(reflexion: ReflexionEntity) {
        reflexionDao.delete(reflexion)
    }

    fun obtenerTodas(): List<ReflexionEntity> = reflexionDao.getAll()

    fun obtenerPorId(id: Long): ReflexionEntity? = reflexionDao.getById(id)
}
