package com.ypg.neville.feature.morningdialog.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "evening_ritual_reviews",
    indices = [Index(value = ["sessionDateEpochDay"], unique = true)]
)
data class EveningReviewEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionDateEpochDay: Long,
    val completedAtEpochMillis: Long,
    val energy: Int,
    val predominantEmotionId: String,
    val whatWentWell: String,
    val learning: String,
    val autopilotMoment: String,
    val gratitude: String,
    val tomorrowPreparation: String,
    val identityAlignment: Int,
    val suggestion: String,
    val agendaCompletedCount: Int,
    val agendaTotalCount: Int,
    val goalUnitsCompletedCount: Int,
    val presenceReturns: Int,
    val automaticPilotEvents: Int,
    val coherenceSessionsCount: Int,
    val dayContextFingerprint: String,
    val journalEntryRequested: Boolean,
    val journalEntryCreated: Boolean,
    val journalEntryId: Long?
)
