package com.ypg.neville.feature.emotionalanchors.data

interface EmotionalAnchorStore {
    fun list(): List<EmotionalAnchor>
    fun getById(id: Long): EmotionalAnchor?
    fun create(
        phrase: String,
        breathingTechniqueId: String,
        breathingTechniqueName: String,
        breathingTechniquePattern: String,
        breathingTechniqueGuide: String,
        imagePath: String,
        audioPath: String,
        audioDurationMs: Long
    ): EmotionalAnchor

    fun update(
        id: Long,
        phrase: String,
        breathingTechniqueId: String,
        breathingTechniqueName: String,
        breathingTechniquePattern: String,
        breathingTechniqueGuide: String,
        imagePath: String,
        audioPath: String,
        audioDurationMs: Long
    ): EmotionalAnchor

    fun deleteById(id: Long)
}
