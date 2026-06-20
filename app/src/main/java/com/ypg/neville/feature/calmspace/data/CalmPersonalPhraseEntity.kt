package com.ypg.neville.feature.calmspace.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "calm_personal_phrases",
    indices = [Index(value = ["phrase"], unique = true), Index(value = ["updatedAt"])]
)
data class CalmPersonalPhraseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val phrase: String,
    val createdAt: Long,
    val updatedAt: Long
)

