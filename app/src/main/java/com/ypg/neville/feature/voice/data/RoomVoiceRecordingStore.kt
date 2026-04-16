package com.ypg.neville.feature.voice.data

import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryEventLogger
import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryEventType

class RoomVoiceRecordingStore(
    private val dao: VoiceRecordingDao
) : VoiceRecordingStore {

    override fun list(): List<VoiceRecording> {
        return dao.loadAll().map { it.toDomain() }
    }

    override fun create(title: String, filePath: String, durationMs: Long): VoiceRecording {
        val now = System.currentTimeMillis()
        val id = dao.insert(
            VoiceRecordingEntity(
                title = title,
                filePath = filePath,
                durationMs = durationMs,
                createdAt = now,
                updatedAt = now
            )
        )
        WeeklySummaryEventLogger.log(WeeklySummaryEventType.VOICE_CREATED, targetKey = id.toString())
        return requireNotNull(dao.findById(id)?.toDomain())
    }

    override fun updateTitle(id: Long, title: String): VoiceRecording? {
        val current = dao.findById(id) ?: return null
        val updated = current.copy(title = title, updatedAt = System.currentTimeMillis())
        dao.update(updated)
        return updated.toDomain()
    }

    override fun deleteById(id: Long) {
        dao.deleteById(id)
        WeeklySummaryEventLogger.log(WeeklySummaryEventType.VOICE_DELETED, targetKey = id.toString())
    }

    private fun VoiceRecordingEntity.toDomain(): VoiceRecording {
        return VoiceRecording(
            id = id,
            title = title,
            filePath = filePath,
            durationMs = durationMs,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
