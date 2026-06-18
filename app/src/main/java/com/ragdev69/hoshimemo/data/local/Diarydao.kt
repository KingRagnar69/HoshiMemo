package com.ragdev69.hoshimemo.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {

    @Query("SELECT * FROM diary_entries WHERE dateKey = :dateKey")
    fun observeEntry(dateKey: String): Flow<DiaryEntity?>

    @Query("SELECT dateKey FROM diary_entries ORDER BY dateKey ASC LIMIT 1")
    suspend fun getEarliestDateKey(): String?

    @Query("SELECT dateKey FROM diary_entries")
    suspend fun getAllDateKeys(): List<String>

    @Upsert
    suspend fun upsert(entry: DiaryEntity)

    @Query("DELETE FROM diary_entries WHERE dateKey = :dateKey")
    suspend fun delete(dateKey: String)
}