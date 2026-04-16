package com.ypg.neville.model.reminders

import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryEventLogger
import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryEventType
import java.util.UUID

class ReminderRepository(private val dao: ReminderDao) {

    fun load(): List<ReminderEntity> {
        val now = System.currentTimeMillis()
        val current = dao.loadAll()
        var changed = false
        val sanitized = current.map { reminder ->
            val frequency = ReminderFrequency.fromEntity(reminder)
            if (
                reminder.isStarted &&
                frequency is ReminderFrequency.DateOnce &&
                frequency.dateMillis <= now
            ) {
                changed = true
                reminder.copy(isStarted = false, startedAt = null)
            } else {
                reminder
            }
        }

        if (changed) {
            sanitized.forEach { dao.update(it) }
        }
        return sanitized
    }

    fun get(id: String): ReminderEntity? = dao.findById(id)

    fun create(title: String, message: String, frequency: ReminderFrequency): ReminderEntity {
        val empty = ReminderEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            message = message,
            frequencyType = "interval",
            isStarted = true,
            startedAt = System.currentTimeMillis(),
            sortOrder = dao.nextSortOrder()
        )
        val entity = ReminderFrequency.applyToEntity(empty, frequency)
        dao.insert(entity)
        WeeklySummaryEventLogger.log(WeeklySummaryEventType.REMINDERS_CREATED, targetKey = entity.id)
        return entity
    }

    fun update(reminder: ReminderEntity) {
        dao.update(reminder)
        WeeklySummaryEventLogger.log(WeeklySummaryEventType.REMINDERS_MODIFIED, targetKey = reminder.id)
    }

    fun deleteById(id: String) {
        dao.deleteById(id)
        WeeklySummaryEventLogger.log(WeeklySummaryEventType.REMINDERS_DELETED, targetKey = id)
    }
}
