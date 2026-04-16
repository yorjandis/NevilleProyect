package com.ypg.neville.feature.emotionalanchors.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "emotional_anchors",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["updatedAt"])
    ]
)
data class EmotionalAnchorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
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
