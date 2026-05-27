package com.ragdev69.hoshimemo.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Note(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val color: String = "#1E1E35",
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null,
    val isDeleted: Boolean = false,
    val userId: String = "",
    val isPinned: Boolean = false
) {
    // Required no-arg constructor for Firestore
    constructor() : this(
        id = "", title = "", content = "", color = "#1E1E35",
        createdAt = null, updatedAt = null,
        isDeleted = false, userId = "", isPinned = false
    )
}