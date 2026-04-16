package com.ypg.neville.feature.weeklysummary.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weekly_summaries",
    indices = [Index(value = ["weekStartMillis"], unique = true)]
)
data class WeeklySummaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val weekStartMillis: Long,
    val weekEndMillis: Long,
    val generatedAtMillis: Long = System.currentTimeMillis(),
    val notesCreated: Int,
    val notesModified: Int,
    val notesDeleted: Int,
    val journalCreated: Int,
    val journalModified: Int,
    val journalDeleted: Int,
    val conferencesRead: Int,
    val goalsCreated: Int,
    val goalsCompleted: Int,
    val goalsInProgress: Int,
    val remindersCreated: Int,
    val remindersModified: Int,
    val remindersDeleted: Int,
    val voiceCreated: Int,
    val voiceDeleted: Int,
    val emotionalAnchorsCreated: Int,
    val emotionalAnchorsUsed: Int,
    val morningRitualsCompleted: Int,
    val personalPhrasesCreated: Int,
    val personalPhrasesModified: Int,
    val personalPhrasesDeleted: Int,
    val encyclopediaAccessed: Int
)
