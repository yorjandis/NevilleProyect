package com.ypg.neville.feature.emotionalanchors.data

data class EmotionalAnchor(
    val id: Long,
    val phrase: String,
    val breathingTechniqueId: String,
    val breathingTechniqueName: String,
    val breathingTechniquePattern: String,
    val breathingTechniqueGuide: String,
    val imagePath: String,
    val audioPath: String,
    val audioDurationMs: Long,
    val createdAt: Long,
    val updatedAt: Long
)
