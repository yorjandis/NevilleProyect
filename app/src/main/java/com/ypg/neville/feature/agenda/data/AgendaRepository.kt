package com.ypg.neville.feature.agenda.data

import java.util.UUID

class AgendaRepository(private val dao: AgendaItemDao) {

    fun load(): List<AgendaItemEntity> = dao.loadAll()

    fun get(id: String): AgendaItemEntity? = dao.findById(id)

    fun create(activityDateMillis: Long, activityTimeMillis: Long): AgendaItemEntity {
        val now = System.currentTimeMillis()
        return AgendaItemEntity(
            id = UUID.randomUUID().toString(),
            title = "",
            createdAt = now,
            updatedAt = now,
            note = "",
            activityDateMillis = activityDateMillis,
            activityTimeMillis = activityTimeMillis,
            place = "",
            content = "",
            priority = AgendaPriority.NEUTRAL.name.lowercase(),
            colorHex = "#A9D7A4",
            completed = null,
            reminderActive = false,
            reminderId = null
        )
    }

    fun save(item: AgendaItemEntity) {
        if (dao.findById(item.id) == null) {
            dao.insert(item)
        } else {
            dao.update(item)
        }
    }

    fun deleteById(id: String) {
        dao.deleteById(id)
    }
}
