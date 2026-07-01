package com.ragdev69.hoshimemo.ui.secret

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * Schedules the "your letter unlocked" notification. Uses an inexact
 * (setAndAllowWhileIdle) alarm on purpose — letters are locked for weeks,
 * months, or years, so being off by a few minutes on the day it unlocks is
 * completely fine, and inexact alarms don't require the user to grant the
 * special "Alarms & reminders" permission that exact alarms need on
 * Android 12+.
 */
object LetterAlarmScheduler {

    fun schedule(context: Context, letter: FutureLetter) {
        // Nothing to schedule for a letter that's already open-able or opened.
        if (letter.isOpened || letter.unlockAt <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context, letter.id, letter.title)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, letter.unlockAt, pendingIntent)
    }

    fun cancel(context: Context, letterId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildPendingIntent(context, letterId, ""))
    }

    private fun buildPendingIntent(context: Context, letterId: String, title: String): PendingIntent {
        val intent = Intent(context, LetterUnlockReceiver::class.java).apply {
            action = LetterUnlockReceiver.ACTION_LETTER_UNLOCK
            putExtra(LetterUnlockReceiver.EXTRA_LETTER_ID, letterId)
            putExtra(LetterUnlockReceiver.EXTRA_LETTER_TITLE, title)
        }
        return PendingIntent.getBroadcast(
            context,
            letterId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}