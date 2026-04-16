package com.ypg.neville.feature.emotionalanchors.data

import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryEventLogger
import com.ypg.neville.feature.weeklysummary.domain.WeeklySummaryEventType

class RoomEmotionalAnchorStore(
    private val dao: EmotionalAnchorDao
) : EmotionalAnchorStore {

    override fun list(): List<EmotionalAnchor> {
        return dao.loadAll().map { it.toDomain() }
    }

    override fun getById(id: Long): EmotionalAnchor? {
        return dao.findById(id)?.toDomain()
    }

    override fun create(
        phrase: String,
        breathingTechniqueId: String,
        breathingTechniqueName: String,
        breathingTechniquePattern: String,
        breathingTechniqueGuide: String,
        imagePath: String,
        audioPath: String,
        audioDurationMs: Long
    ): EmotionalAnchor {
        val now = System.currentTimeMillis()
        val id = dao.insert(
            EmotionalAnchorEntity(
                phrase = phrase,
                breathingTechniqueId = breathingTechniqueId,
                breathingTechniqueName = breathingTechniqueName,
                breathingTechniquePattern = breathingTechniquePattern,
                breathingTechniqueGuide = breathingTechniqueGuide,
                imagePath = imagePath,
                audioPath = audioPath,
                audioDurationMs = audioDurationMs,
                createdAt = now,
                updatedAt = now
            )
        )

        WeeklySummaryEventLogger.log(WeeklySummaryEventType.ANCHORS_CREATED, targetKey = id.toString())

        return EmotionalAnchor(
            id = id,
            phrase = phrase,
            breathingTechniqueId = breathingTechniqueId,
            breathingTechniqueName = breathingTechniqueName,
            breathingTechniquePattern = breathingTechniquePattern,
            breathingTechniqueGuide = breathingTechniqueGuide,
            imagePath = imagePath,
            audioPath = audioPath,
            audioDurationMs = audioDurationMs,
            createdAt = now,
            updatedAt = now
        )
    }

    override fun deleteById(id: Long) {
        dao.deleteById(id)
        // Por requerimiento sólo contamos creadas y utilizadas.
    }

    override fun update(
        id: Long,
        phrase: String,
        breathingTechniqueId: String,
        breathingTechniqueName: String,
        breathingTechniquePattern: String,
        breathingTechniqueGuide: String,
        imagePath: String,
        audioPath: String,
        audioDurationMs: Long
    ): EmotionalAnchor {
        val existing = requireNotNull(dao.findById(id)) { "El ancla no existe" }
        val now = System.currentTimeMillis()
        val updated = existing.copy(
            phrase = phrase,
            breathingTechniqueId = breathingTechniqueId,
            breathingTechniqueName = breathingTechniqueName,
            breathingTechniquePattern = breathingTechniquePattern,
            breathingTechniqueGuide = breathingTechniqueGuide,
            imagePath = imagePath,
            audioPath = audioPath,
            audioDurationMs = audioDurationMs,
            updatedAt = now
        )
        dao.update(updated)
        return updated.toDomain()
    }

    private fun EmotionalAnchorEntity.toDomain(): EmotionalAnchor {
        return EmotionalAnchor(
            id = id,
            phrase = phrase,
            breathingTechniqueId = breathingTechniqueId,
            breathingTechniqueName = breathingTechniqueName,
            breathingTechniquePattern = breathingTechniquePattern,
            breathingTechniqueGuide = breathingTechniqueGuide,
            imagePath = imagePath,
            audioPath = audioPath,
            audioDurationMs = audioDurationMs,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
