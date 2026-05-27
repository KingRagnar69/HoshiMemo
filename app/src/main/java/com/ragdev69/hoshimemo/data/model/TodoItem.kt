package com.ragdev69.hoshimemo.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class TodoItem(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val isCompleted: Boolean = false,
    @ServerTimestamp
    val createdAt: Date? = null,
    val userId: String = "",
    val priority: Int = 0  // 0 = normal, 1 = medium, 2 = high
) {
    constructor() : this(
        id = "", title = "", isCompleted = false,
        createdAt = null, userId = "", priority = 0
    )
}