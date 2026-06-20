package com.ypg.neville.feature.cardiocoherence.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cardio_coherence_records",
    indices = [
        Index(value = ["dateEpochMillis"]),
        Index(value = ["initialState"])
    ]
)
data class MeditationSessionRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateEpochMillis: Long,
    val durationMinutes: Int,
    val initialState: String,
    val intention: String,
    val beforeScore: Int,
    val afterScore: Int,
    val mentalClarityScore: Int,
    val heartConnectionScore: Int,
    val predominantEmotion: String,
    val closingWord: String,
    val phasesCompleted: String
)
