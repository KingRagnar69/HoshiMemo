package com.ragdev69.hoshimemo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room table that mirrors [com.ragdev69.hoshimemo.data.model.Note].
 * Used as the offline-first store when the user is not signed in.
 * When the user later signs in, [LocalRepository] can hand these rows
 * off to [FirebaseRepository] for cloud sync.
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val content: String,
    val color: String = "#1E1E35",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val userId: String = "",        // empty string = local / not yet synced
    val isPinned: Boolean = false,
    val isSynced: Boolean = false   // true once uploaded to Firestore
)