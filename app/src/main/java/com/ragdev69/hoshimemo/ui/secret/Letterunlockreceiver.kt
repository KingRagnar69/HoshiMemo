package com.ragdev69.hoshimemo.ui.secret

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ragdev69.hoshimemo.MainActivity
import com.ragdev69.hoshimemo.R

class LetterUnlockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val letterId    = intent.getStringExtra(EXTRA_LETTER_ID) ?: return
        val letterTitle = intent.getStringExtra(EXTRA_LETTER_TITLE).orEmpty()

        ensureChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = ACTION_OPEN_LETTER
            putExtra(EXTRA_LETTER_ID, letterId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            letterId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("A letter has unlocked 💌")
            .setContentText(
                if (letterTitle.isNotBlank()) "\"$letterTitle\" is ready to read"
                else "Your letter to future you is ready to read"
            )
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                if (letterTitle.isNotBlank()) "\"$letterTitle\" is ready to read"
                else "Your letter to future you is ready to read"
            ))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .build()

        // Respects the user's notification permission — if it's not granted
        // (Android 13+, denied), this silently no-ops instead of crashing.
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            NotificationManagerCompat.from(context).notify(letterId.hashCode(), notification)
        }
    }

    companion object {
        const val CHANNEL_ID           = "future_letter_unlocks"
        const val ACTION_LETTER_UNLOCK = "com.ragdev69.hoshimemo.ACTION_LETTER_UNLOCK"
        const val ACTION_OPEN_LETTER   = "com.ragdev69.hoshimemo.ACTION_OPEN_LETTER"
        const val EXTRA_LETTER_ID      = "letter_id"
        const val EXTRA_LETTER_TITLE   = "letter_title"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        "Letter unlocks",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Notifies you when a letter to your future self unlocks"
                    }
                    manager.createNotificationChannel(channel)
                }
            }
        }
    }
}