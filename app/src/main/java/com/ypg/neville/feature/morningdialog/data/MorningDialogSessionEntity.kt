package com.ypg.neville.feature.morningdialog.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "morning_dialog_sessions",
    indices = [Index(value = ["sessionDateEpochDay"], unique = true)]
)
data class MorningDialogSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionDateEpochDay: Long,
    val completedAtEpochMillis: Long,
    val goalsJson: String,
    val identity: String,
    val emotionsJson: String,
    val anticipatedSituationsJson: String,
    val consciousResponsesJson: String,
    val noteText: String = "",
    val completed: Boolean
)
