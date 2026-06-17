package com.ragdev69.hoshimemo.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class TodoItem(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val isCompleted: Boolean = false,
    val isFailed: Boolean = false,         // true when swiped left (failed)
    @ServerTimestamp
    val createdAt: Date? = null,
    val userId: String = "",
    val priority: Int = 0,                 // 0=normal 1=medium 2=high
    val timerSeconds: Long = 0L,           // countdown duration; 0=no timer
    val timerStartedAt: Long = 0L,         // epoch ms when START was tapped; 0=not started
    val groupId: String = "",              // "" = ungrouped
    val groupName: String = "",            // denormalised for display
    val orderInGroup: Int = 0              // position within group for sequential timer
) {
    constructor() : this(
        id = "", title = "", isCompleted = false, isFailed = false,
        createdAt = null, userId = "", priority = 0,
        timerSeconds = 0L, timerStartedAt = 0L,
        groupId = "", groupName = "", orderInGroup = 0
    )

    fun remainingSeconds(): Long {
        if (timerSeconds <= 0L || timerStartedAt <= 0L || isCompleted || isFailed) return timerSeconds
        val elapsed = (System.currentTimeMillis() - timerStartedAt) / 1000L
        return maxOf(0L, timerSeconds - elapsed)
    }

    fun isTimerExpired(): Boolean =
        timerSeconds > 0L && timerStartedAt > 0L && !isCompleted && !isFailed && remainingSeconds() <= 0L

    fun timerRunning(): Boolean =
        timerSeconds > 0L && timerStartedAt > 0L && !isCompleted && !isFailed && remainingSeconds() > 0L
}