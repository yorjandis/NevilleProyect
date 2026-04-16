package com.ypg.neville.model.db.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "preferences")
data class PreferenceEntity(
    @PrimaryKey val prefKey: String,
    val prefValue: String,
    val valueType: String,
    val updatedAt: Long = System.currentTimeMillis()
)
