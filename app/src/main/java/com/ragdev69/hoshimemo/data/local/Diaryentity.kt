package com.ragdev69.hoshimemo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_entries")
data class DiaryEntity(
    @PrimaryKey
    val dateKey: String,       // "yyyy-MM-dd" e.g. "2025-06-18"
    val diaryText: String = "",
    val codeText: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)