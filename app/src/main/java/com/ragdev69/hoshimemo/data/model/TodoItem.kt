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
    val priority: Int = 0,            // 0 = normal, 1 = medium, 2 = high
    val timerSeconds: Long = 0L,      // total countdown duration in seconds (0 = no timer)
    val timerStartedAt: Long = 0L     // epoch millis when timer was started; 0 = not started
) {
    constructor() : this(
        id = "", title = "", isCompleted = false,
        createdAt = null, userId = "", priority = 0,
        timerSeconds = 0L, timerStartedAt = 0L
    )

    /**
     * How many seconds remain right now, calculated from wall-clock time.
     * This survives screen navigation, tab switches, and app backgrounding
     * because it is based on absolute epoch time, not a composable countdown.
     */
    fun remainingSeconds(): Long {
        if (timerSeconds <= 0L || timerStartedAt <= 0L || isCompleted) return timerSeconds
        val elapsed = (System.currentTimeMillis() - timerStartedAt) / 1000L
        return maxOf(0L, timerSeconds - elapsed)
    }

    fun isTimerExpired(): Boolean =
        timerSeconds > 0L && timerStartedAt > 0L && !isCompleted && remainingSeconds() <= 0L
}