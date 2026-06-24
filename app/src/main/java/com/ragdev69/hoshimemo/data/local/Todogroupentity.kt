package com.ragdev69.hoshimemo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room table for todo groups, mirroring the users/{uid}/todoGroups
 * Firestore documents. Includes [userId] and [isSynced] for the same
 * offline-then-sync flow as [TodoEntity].
 */
@Entity(tableName = "todo_groups")
data class TodoGroupEntity(
    @PrimaryKey
    val groupId: String = UUID.randomUUID().toString(),
    val groupName: String = "",
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)