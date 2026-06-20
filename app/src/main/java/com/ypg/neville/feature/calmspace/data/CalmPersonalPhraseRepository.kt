package com.ypg.neville.feature.calmspace.data

class CalmPersonalPhraseRepository(
    private val dao: CalmPersonalPhraseDao
) {
    fun listAll(): List<CalmPersonalPhraseEntity> = dao.getAll()

    fun create(phrase: String): Long {
        val now = System.currentTimeMillis()
        return dao.insert(
            CalmPersonalPhraseEntity(
                phrase = phrase.trim(),
                createdAt = now,
                updatedAt = now
            )
        )
    }

    fun update(id: Long, phrase: String): Boolean {
        val rows = dao.updatePhraseById(
            id = id,
            phrase = phrase.trim(),
            updatedAt = System.currentTimeMillis()
        )
        return rows > 0
    }

    fun delete(id: Long): Boolean = dao.deleteById(id) > 0
}

