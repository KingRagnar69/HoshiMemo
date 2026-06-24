package com.ragdev69.hoshimemo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room table that mirrors [com.ragdev69.hoshimemo.data.model.TodoItem].
 * Used as the offline-first store when the user is not signed in — the
 * same pattern [NoteEntity] uses for notes. Includes [userId] and
 * [isSynced] so a todo created offline can later be pushed up to
 * Firestore and matched/marked without duplicating.
 */
@Entity(tableName = "todo_items")
data class TodoEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val isCompleted: Boolean = false,
    val isFailed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val userId: String = "",
    val priority: Int = 0,
    val timerSeconds: Long = 0L,
    val timerStartedAt: Long = 0L,
    val groupId: String = "",
    val groupName: String = "",
    val orderInGroup: Int = 0,
    val isSynced: Boolean = false
)