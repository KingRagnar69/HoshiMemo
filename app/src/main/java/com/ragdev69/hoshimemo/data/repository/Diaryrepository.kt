package com.ragdev69.hoshimemo.data.repository

import com.ragdev69.hoshimemo.data.local.DiaryDao
import com.ragdev69.hoshimemo.data.local.DiaryEntity
import kotlinx.coroutines.flow.Flow

class DiaryRepository(private val dao: DiaryDao) {

    fun observeEntry(dateKey: String): Flow<DiaryEntity?> =
        dao.observeEntry(dateKey)

    suspend fun saveEntry(dateKey: String, diaryText: String, codeText: String) {
        dao.upsert(
            DiaryEntity(
                dateKey   = dateKey,
                diaryText = diaryText,
                codeText  = codeText,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getEarliestDateKey(): String? = dao.getEarliestDateKey()

    suspend fun getAllDateKeys(): List<String> = dao.getAllDateKeys()
}